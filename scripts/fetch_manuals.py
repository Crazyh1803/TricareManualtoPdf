#!/usr/bin/env python3
"""
TRICARE Manuals — Content Scraper
==================================
Fetches manual content from manuals.health.mil and writes static files
to docs/data/ for consumption by the GitHub Pages web app.

NOTE: manuals.health.mil uses DoD-signed certificates that are not in the
standard CA bundle.  All requests use verify=False (SSL verification
disabled) and Playwright uses ignore_https_errors=True — identical to the
Android app's lenient OkHttp client.  All traffic is read-only public
government documents.

The ManualToc.aspx page renders its section tree via JavaScript, so we use
Playwright (headless Chromium) to collect the section links, then fall back
to plain requests for fetching the individual static HTML files.

Output layout:
  docs/data/manuals.json           — manual list with latestChange
  docs/data/{CODE}/toc.json        — section index for one manual/change
  docs/data/{CODE}/s/{id}.html     — individual section content (body HTML only)

Run locally:
  pip install -r scripts/requirements.txt
  playwright install chromium
  python scripts/fetch_manuals.py

  # Fetch only a specific manual (faster during development):
  python scripts/fetch_manuals.py --code TOT5
"""

from __future__ import annotations

import argparse
import asyncio
import json
import os
import re
import sys
import time
import warnings
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urljoin, urlparse, parse_qs, urldefrag

import requests
import urllib3
from bs4 import BeautifulSoup
from playwright.async_api import async_playwright

# Suppress the InsecureRequestWarning that comes with verify=False
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

# ── Configuration ───────────────────────────────────────────────────────────
BASE_URL  = "https://manuals.health.mil"
TOC_URL   = BASE_URL + "/pages/ManualToc.aspx?Manual={code}&Change={change}"
REPO_ROOT = Path(__file__).parent.parent.resolve()
DATA_DIR  = REPO_ROOT / "docs" / "data"
MANUALS_JSON = DATA_DIR / "manuals.json"

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/124.0.0.0 Safari/537.36"
    ),
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.5",
}

# Throttle between individual section HTTP requests (seconds).
# manuals.health.mil rate-limits aggressively; 0.5s was too fast.
REQUEST_DELAY = 1.5

# Cooldown between processing different manuals (seconds).
# After scraping 50–80+ sections for one manual the server starts
# resetting connections.  A 60-second pause lets the rate-limit
# window reset before we begin the next manual.
MANUAL_COOLDOWN_SECS = 60

# How many change numbers past the known one to probe when detecting the
# latest version.  The walk breaks at the first candidate that fails, so in
# steady state this costs one extra probe regardless of the ceiling — the
# headroom only matters when catching up after the baseline has gone stale
# (previously hardcoded to 5, which capped TOT5 at exactly known+5 and made
# it impossible to close a multi-month gap in a single run).
#
# NOTE: only safe alongside the Change=-parameter validation in
# _toc_has_sections().  The site returns HTTP 200 and serves current content
# for *any* change number, so a bare "does the page have links?" check
# reports every candidate as valid and a large ceiling would run away.
FORWARD_WALK_LIMIT = 30

# A manual's new content-section count must be at least this fraction of
# what's already on disk, or the scrape is treated as failed (see the
# regression guard in process_manual()). Guards against a rate-limited or
# blocked run silently publishing a near-empty manual — this is what broke
# TOT5 for weeks: every fetch legitimately returned HTTP 200, so nothing
# flagged the collapse from 54 content sections down to 1.
MIN_CONTENT_RATIO = 0.5
MIN_CONTENT_FOR_GUARD = 4  # don't guard tiny/first-ever manuals

# Companion to the count guard: the fraction of section fetches allowed to come
# back empty before the whole scrape is treated as failed. Finding the right
# number of sections but retrieving none of their content would otherwise pass
# the count guard and publish a manual full of error placeholders.
MAX_FAILED_SECTION_RATIO = 0.5

# A section whose extracted content is shorter than this counts as failed.
# Run #42 published 199 sections averaging 1 byte each: the server answered
# HTTP 200 but extract_content_html() reduced the body to an empty string, so
# nothing registered as a failure and the empty files went live. Real sections
# are kilobytes, so anything this small is a failure however it was produced.
MIN_SECTION_CONTENT_CHARS = 200

# If this many sections are attempted and every one comes back empty, stop
# immediately rather than working through hundreds of doomed requests against
# a government server before the ratio check fires at the end.
EARLY_ABORT_SAMPLE = 10

# CSS selectors for site chrome to strip from section HTML
STRIP_SELECTORS = [
    "header", "footer", "nav", "#navigation", "#header", "#footer",
    ".navbar", ".breadcrumb", ".breadcrumbs",
    "[role='banner']", "[role='navigation']",
    "script", "style", "noscript",
    "#dnn_ContentPane > .dnnFormItem",
]

# Selectors for the main content area (tried in order)
CONTENT_SELECTORS = [
    "#dnn_ContentPane",
    "[role='main']",
    "main",
    ".content",
    "#content",
    "body",
]

# Front-matter filenames (sorted before chapters)
_FRONT_MATTER = {"FOREWORD", "INTRO", "PREFACE", "SUMMARY"}


# ── HTTP session ────────────────────────────────────────────────────────────
session = requests.Session()
session.headers.update(HEADERS)
# Disable SSL verification globally on the session —
# health.mil uses DoD intermediate CAs not in the standard bundle.
session.verify = False


def get(url: str) -> requests.Response | None:
    """GET with retries and polite throttling."""
    time.sleep(REQUEST_DELAY)
    for attempt in range(3):
        try:
            r = session.get(url, timeout=30)
            if r.status_code == 200:
                return r
            print(f"  HTTP {r.status_code}: {url}", file=sys.stderr)
            return None
        except requests.RequestException as e:
            if attempt == 2:
                print(f"  Failed ({type(e).__name__}: {e}): {url}", file=sys.stderr)
                return None
            time.sleep(2 ** attempt)
    return None


_CHANGE_PARAM_RE = re.compile(r"[?&]Change=(\d+)", re.IGNORECASE)


# ── URL helpers ──────────────────────────────────────────────────────────────

def is_display_html(u: str) -> bool:
    """Match any .html file served under the DisplayManualHtmlFile path."""
    return bool(u and re.search(r"/pages/DisplayManualHtmlFile/.+\.html", u, re.I))


def is_chapter_toc_name(name: str) -> bool:
    return bool(re.match(r"^C\d+TOC\.HTML$", name.upper()))


def natural_sort_key(name: str):
    """Sort filenames in logical manual order: front matter → chapters (TOC heading first, then sections, then addenda)."""
    base = name.split(".", 1)[0].upper()
    # Strip common manual-code prefix (e.g. "TST5_", "TPT5_")
    base = re.sub(r"^[A-Z]{3,5}\d*_", "", base)

    for keyword in _FRONT_MATTER:
        if base == keyword or base.endswith(keyword):
            return (-1, 0, 0, 0, base)

    # Chapter TOC page (C1TOC.html) — sorts before all sections in that chapter
    m = re.match(r"^C(\d+)TOC$", base)
    if m:
        return (0, int(m.group(1)), 0, 0, "")

    # Chapter section: C1S1, C1S1_1, C01S02_3, etc.
    m = re.match(r"^C(\d+)S(\d+)(?:_(\d+))?$", base)
    if m:
        return (0, int(m.group(1)), int(m.group(2)), int(m.group(3) or 0), "")

    # Addendum: C1AD_A, C1ADA, C2ADDENDUM, etc.
    m = re.match(r"^C(\d+)AD(.*)$", base)
    if m:
        return (1, int(m.group(1)), 9999, 9999, m.group(2).upper())

    # Any other file that at least starts with a chapter number
    m = re.match(r"^C(\d+)", base)
    c = int(m.group(1)) if m else 999_999
    return (9, c, 999_999, 999_999, base)


def parse_chapter_section(name: str) -> tuple[int, str]:
    """Return (chapter_number, section_string) from a DisplayManualHtmlFile filename."""
    base = name.split(".", 1)[0].upper()
    base = re.sub(r"^[A-Z]{3,5}\d*_", "", base)

    m = re.match(r"^C(\d+)S(\d+)(?:_(\d+))?$", base)
    if m:
        chapter = int(m.group(1))
        section = m.group(2) + ("." + m.group(3) if m.group(3) else "")
        return chapter, section

    m = re.match(r"^C(\d+)", base)
    if m:
        return int(m.group(1)), ""

    return 0, ""


# ── TOC collection via Playwright ────────────────────────────────────────────

async def _expand_toc(page) -> None:
    """Click 'Expand All' if present and wait for the tree to render.

    Uses only very specific selectors — the generic 'a:has-text(Expand)'
    is intentionally omitted because it can match unrelated navigation links
    (e.g. on TRT5) and navigate the page away from the TOC entirely.
    """
    selectors = [
        "a:has-text('Expand All')",
        "button:has-text('Expand All')",
        "input[value*='Expand All']",
        "a[title='Expand All']",
    ]
    for frame in page.frames:
        for sel in selectors:
            try:
                el = await frame.query_selector(sel)
                if el:
                    await el.click()
                    await page.wait_for_load_state("networkidle", timeout=15_000)
                    return
            except Exception:
                pass


async def _collect_display_hrefs(page, base_url: str) -> list[str]:
    """Return all unique DisplayManualHtmlFile hrefs visible across all frames."""
    host = urlparse(base_url).netloc
    seen: set[str] = set()
    out:  list[str] = []
    for frame in page.frames:
        try:
            hrefs: list[str] = await frame.eval_on_selector_all(
                "a",
                "els => els.map(a => a.href || a.getAttribute('href') || '')",
            )
        except Exception:
            continue
        for href in hrefs or []:
            if not href:
                continue
            full = urljoin(frame.url, href)
            if urlparse(full).netloc != host:
                continue
            if not is_display_html(full):
                continue
            if full not in seen:
                seen.add(full)
                out.append(full)
    return out


async def _toc_has_sections(ctx, code: str, change: int) -> bool:
    """Return True if the TOC page for this change *genuinely* has sections.

    The TRICARE site returns HTTP 200 for ANY change number (even non-existent
    ones), serving the current content instead.  The only reliable test is to
    check whether the section links that appear on the page actually reference
    the requested Change number.  If all links carry a *different* Change value
    the server redirected us to a different version, which means this change
    does not exist yet.
    """
    toc_url = TOC_URL.format(code=code, change=change)
    page = await ctx.new_page()
    try:
        try:
            await page.goto(toc_url, wait_until="networkidle", timeout=20_000)
        except Exception:
            pass
        await page.wait_for_timeout(2_000)
        urls = await _collect_display_hrefs(page, toc_url)
        if not urls:
            return False

        # Inspect Change= parameters in the returned URLs.
        change_vals: set[int] = set()
        has_paramless = False
        for url in urls:
            m = _CHANGE_PARAM_RE.search(url)
            if m:
                change_vals.add(int(m.group(1)))
            else:
                has_paramless = True  # URL has no Change param — can't validate

        if has_paramless or not change_vals:
            # Cannot validate via Change= param → fall back to "has links" check
            return True

        # If *any* returned link matches the requested change, the page is valid.
        # If all links carry a different change number the site silently served a
        # different version, so this change does not actually exist.
        return change in change_vals
    finally:
        await page.close()


async def _launch_context(pw):
    """Browser + context configured to get past the site's JS bot challenge.

    manuals.health.mil answers plain HTTP clients with a constant 6572-byte
    challenge interstitial (no <title>, no-cache metas, data: favicon) for
    every URL, which is why requests-based fetching silently produced empty
    content. A real browser runs the challenge and is then served the actual
    document, so every fetch of manual content has to come through here.
    """
    browser = await pw.chromium.launch(
        headless=True,
        args=["--disable-blink-features=AutomationControlled"],
    )
    ctx = await browser.new_context(
        ignore_https_errors=True,
        user_agent=(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/124.0.0.0 Safari/537.36"
        ),
        viewport={"width": 1280, "height": 900},
        extra_http_headers={"Accept-Language": "en-US,en;q=0.9"},
    )
    # Hide the navigator.webdriver property that sites use for bot detection
    await ctx.add_init_script(
        "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
    )
    return browser, ctx


async def _collect_chapter_sections(ctx, chapter_toc_url: str) -> list[str]:
    """Render one chapter TOC page in the browser and return its section URLs.

    Restricted to the chapter's own directory: chapters cross-reference other
    manuals and publications ("../tpt5/...", "../fr16/...") which resolve to
    valid DisplayManualHtmlFile URLs but belong elsewhere.
    """
    # One bad chapter must not abort the whole manual, so every browser
    # interaction here is contained — including new_page() itself.
    page = None
    try:
        page = await ctx.new_page()
        try:
            await page.goto(chapter_toc_url, wait_until="networkidle", timeout=25_000)
        except Exception:
            pass  # partial render is still worth scraping
        await page.wait_for_timeout(1_500)
        found = await _collect_display_hrefs(page, chapter_toc_url)
    except Exception as e:
        print(f"    (browser fetch failed for {chapter_toc_url}: {e})", file=sys.stderr)
        return []
    finally:
        if page is not None:
            try:
                await page.close()
            except Exception:
                pass

    base_dir = chapter_toc_url.rsplit("/", 1)[0] + "/"
    seen: set[str] = set()
    out: list[str] = []
    for u in found:
        u = urldefrag(u)[0]
        if not u.lower().startswith(base_dir.lower()):
            continue
        if is_chapter_toc_name(Path(urlparse(u).path).name):
            continue  # self-reference / sibling chapter TOC
        if u not in seen:
            seen.add(u)
            out.append(u)
    return out


async def fetch_change_and_toc_urls(code: str, known_change: int) -> tuple[int | None, list[str], list[str]]:
    """
    Single Playwright session that detects the latest change number AND
    collects the top-level TOC URLs (chapter TOCs + front matter).

    Version detection:
      Start from known_change (from manuals.json).  Check up to
      FORWARD_WALK_LIMIT increments ahead, stopping at the first candidate
      that fails — a change is "valid" only if its TOC page actually renders
      DisplayManualHtmlFile section links.  This avoids the false-positive
      caused by the site always returning HTTP 200, even for invalid changes.

    Returns (latest_change, top_level_urls, chapter_section_urls).
    """
    async with async_playwright() as pw:
        browser, ctx = await _launch_context(pw)
        try:
            # ── Detect latest change ──────────────────────────────────────────
            # Walk forward from known_change until we find one with no links.
            latest = known_change
            print(f"  [{code}] Checking change {latest} (known)…")
            if not await _toc_has_sections(ctx, code, latest):
                # The known change failed URL-param validation — the site may be
                # serving a newer version than we have recorded.  Try to recover
                # by reading the actual Change= value from the served links.
                toc_url_probe = TOC_URL.format(code=code, change=latest)
                probe_page = await ctx.new_page()
                try:
                    try:
                        await probe_page.goto(toc_url_probe, wait_until="networkidle", timeout=20_000)
                    except Exception:
                        pass
                    await probe_page.wait_for_timeout(2_000)
                    probe_urls = await _collect_display_hrefs(probe_page, toc_url_probe)
                finally:
                    await probe_page.close()

                served_change: int | None = None
                for u in probe_urls:
                    m = _CHANGE_PARAM_RE.search(u)
                    if m:
                        served_change = int(m.group(1))
                        break

                if served_change and served_change != latest:
                    print(
                        f"  [{code}] Site served Change={served_change} when asked for "
                        f"Change={latest}. Advancing known to {served_change}.",
                        file=sys.stderr,
                    )
                    latest = served_change
                    # Walk forward from the discovered change
                    for candidate in range(served_change + 1, served_change + 1 + FORWARD_WALK_LIMIT):
                        print(f"  [{code}] Checking change {candidate}…")
                        if await _toc_has_sections(ctx, code, candidate):
                            latest = candidate
                        else:
                            break
                else:
                    print(f"  [{code}] Known change {latest} has no sections (server issue?)", file=sys.stderr)
            elif known_change == 0:
                # Change=0 is a special "always-current" sentinel used by manuals
                # whose TOC links are date-based (no ?Change= parameter).  The
                # server accepts any numeric change value and returns the same
                # content, so a forward walk would just keep incrementing the
                # number every CI run forever.  Pin to 0 and skip it entirely.
                print(f"  [{code}] Change=0 sentinel — skipping forward walk.")
            else:
                for candidate in range(known_change + 1, known_change + 1 + FORWARD_WALK_LIMIT):
                    print(f"  [{code}] Checking change {candidate}…")
                    if await _toc_has_sections(ctx, code, candidate):
                        latest = candidate
                    else:
                        break

            print(f"  [{code}] Latest change: {latest}")

            # ── Collect top-level TOC URLs for the detected change ─────────────
            toc_url = TOC_URL.format(code=code, change=latest)
            page = await ctx.new_page()
            try:
                try:
                    await page.goto(toc_url, wait_until="networkidle", timeout=30_000)
                except Exception as e:
                    print(f"  [{code}] goto failed ({e}), continuing…", file=sys.stderr)

                # ── Pass 1: progressive waits, collect without expanding ───────
                # eval_on_selector_all sees all DOM nodes including hidden ones
                # so collapsed trees are still found.  Collecting before any
                # expand avoids the risk that clicking a nav link navigates
                # the page away (observed on TRT5 with the old broad selectors).
                # Mirror the PDF tool's progressive strategy: 2s → 5s → 8s.
                urls: list[str] = []
                for wait_ms in (2_000, 3_000, 5_000):
                    await page.wait_for_timeout(wait_ms)
                    urls = await _collect_display_hrefs(page, toc_url)
                    print(f"  [{code}] Pass-1 after {wait_ms//1000}s: {len(urls)} URLs")
                    if urls:
                        break

                # ── Pass 2: expand and re-collect only if Pass 1 found nothing ─
                if not urls:
                    await _expand_toc(page)
                    await page.wait_for_timeout(5_000)
                    for wait_s in (0, 3, 5):
                        if wait_s:
                            await page.wait_for_timeout(wait_s * 1_000)
                        urls = await _collect_display_hrefs(page, toc_url)
                        print(f"  [{code}] Pass-2 after expand+{wait_s}s: {len(urls)} URLs")
                        if urls:
                            break

                if not urls:
                    # ── Diagnostics: print everything visible to help debug ────
                    page_title = await page.title()
                    print(f"  [{code}] Page title: {page_title!r}")
                    print(f"  [{code}] Frames on page:")
                    all_hrefs: list[str] = []
                    for frame in page.frames:
                        print(f"    frame url: {frame.url}")
                        try:
                            hs = await frame.eval_on_selector_all(
                                "a", "els => els.map(a => a.href || '')"
                            )
                            all_hrefs.extend(h for h in (hs or []) if h)
                        except Exception:
                            pass
                    unique_hrefs = list(dict.fromkeys(all_hrefs))
                    print(f"  [{code}] All hrefs on page ({len(unique_hrefs)} unique):")
                    for h in unique_hrefs[:30]:
                        print(f"    {h}")
            finally:
                await page.close()

            # ── Stage 2: section links from each chapter TOC, same browser ──
            # Done inside this context on purpose: a plain requests.Session()
            # returns these pages with no section links at all (every chapter
            # reported "0 section(s)"), while Playwright reads the same pages
            # fine — so the browser's rendering/session is what makes them
            # readable. Reusing the open context also keeps cookies warm.
            chapter_toc_urls = [
                u for u in urls
                if is_chapter_toc_name(Path(urlparse(u).path).name)
            ]
            section_urls: list[str] = []
            if chapter_toc_urls:
                print(f"  [{code}] Stage 2: {len(chapter_toc_urls)} chapter TOC(s) via browser…")
            for i, chap_url in enumerate(chapter_toc_urls, 1):
                secs = await _collect_chapter_sections(ctx, chap_url)
                note = ""
                if not secs:
                    # Last resort: the old plain-requests path.
                    secs = fetch_chapter_section_urls(chap_url)
                    note = " [requests fallback]"
                    if not secs:
                        note = f" [EMPTY] {chap_url}"
                print(f"    Chapter TOC {i}/{len(chapter_toc_urls)}: {len(secs)} section(s){note}")
                section_urls.extend(secs)

            return latest, urls, section_urls
        finally:
            await ctx.close()
            await browser.close()


def fetch_chapter_section_urls(chapter_toc_url: str) -> list[str]:
    """
    Stage 2: Fetch a chapter TOC page (static HTML) with requests and extract
    all DisplayManualHtmlFile section links.  Chapter TOC self-references are
    excluded.
    """
    r = get(chapter_toc_url)
    if r is None:
        return []

    host = urlparse(chapter_toc_url).netloc

    # Chapters link their sections with document-relative hrefs — "c1s3.html",
    # "./c3toc.html", "../tpt5/c10s2_1.html" — so they must be resolved against
    # the chapter TOC's own URL.  Resolving against BASE_URL (which has no path)
    # turned "c1s3.html" into https://manuals.health.mil/c1s3.html, which fails
    # is_display_html() and was silently discarded, yielding "0 section(s)" for
    # every chapter of every manual.
    base_dir = chapter_toc_url.rsplit("/", 1)[0] + "/"

    seen: set[str] = set()
    results: list[str] = []

    soup = BeautifulSoup(r.text, "lxml")
    for a in soup.find_all("a", href=True):
        href = a["href"].strip()
        if not href or href.startswith("#"):
            continue
        # Drop the fragment so "c1s3.html#FM63551" and "c1s3.html#FM99999"
        # dedupe to one section rather than being counted several times.
        full = urldefrag(urljoin(chapter_toc_url, href))[0]
        if urlparse(full).netloc != host:
            continue
        if not is_display_html(full):
            continue
        # Keep only this manual's own sections.  Chapters cross-reference other
        # manuals and publications ("../tpt5/...", "../fr16/...") which resolve
        # to valid DisplayManualHtmlFile URLs but belong to a different manual.
        if not full.lower().startswith(base_dir.lower()):
            continue
        name = Path(urlparse(full).path).name
        if is_chapter_toc_name(name):
            continue  # skip self-references to the chapter TOC itself
        if full not in seen:
            seen.add(full)
            results.append(full)

    return results


# ── TOC parsing ─────────────────────────────────────────────────────────────

def build_toc_sections(urls: list[str]) -> list[dict]:
    """
    Convert a list of DisplayManualHtmlFile URLs into sorted section dicts.
    Titles are derived from the filename; actual page titles are filled in
    later when the section HTML is fetched.
    """
    seen: set[str] = set()
    sections: list[dict] = []

    for url in urls:
        name = Path(urlparse(url).path).name
        if not name:
            continue
        key = name.upper()
        if key in seen:
            continue
        seen.add(key)

        is_chap_toc = is_chapter_toc_name(name)
        chapter, section = parse_chapter_section(name)

        # Placeholder title — replaced when we fetch the page
        title = name

        sections.append({
            "url":          url,
            "name":         name,
            "chapter":      chapter,
            "section":      section,
            "title":        title,
            "isChapterToc": is_chap_toc,
        })

    sections.sort(key=lambda s: natural_sort_key(s["name"]))

    pad = max(3, len(str(len(sections))))
    for i, s in enumerate(sections):
        s["id"] = str(i + 1).zfill(pad)

    return sections


# ── Title cleaning ───────────────────────────────────────────────────────────
_TITLE_PREFIXES = [
    "TRICARE Manuals - Display ",
    "TRICARE Manuals - ",
]
_CHANGE_SUFFIX = re.compile(r",?\s*\(?Change\s+\d+.*$", re.IGNORECASE)
_CHAP_SECT     = re.compile(r"Chap\s+(\d+)\s+Sect\s+([\d.]+)", re.IGNORECASE)
_CHAP_TOC      = re.compile(r"Chap\s+(\d+)\s+TOC", re.IGNORECASE)
_CHAP_ONLY     = re.compile(r"Chap\s+(\d+)", re.IGNORECASE)


def clean_title(raw: str) -> str:
    title = raw.strip()
    for prefix in _TITLE_PREFIXES:
        if title.startswith(prefix):
            title = title[len(prefix):]
    title = _CHANGE_SUFFIX.sub("", title)
    title = _CHAP_SECT.sub(r"Chapter \1, Section \2", title)
    title = _CHAP_TOC.sub(r"Chapter \1 – Contents", title)
    title = _CHAP_ONLY.sub(r"Chapter \1", title)
    return title.strip() or raw.strip()


def extract_title_from_html(soup: BeautifulSoup) -> str:
    raw = soup.title.string.strip() if soup.title else ""
    return clean_title(raw) if raw else "Untitled Section"


# ── Section content extraction ───────────────────────────────────────────────
def extract_content_html(soup: BeautifulSoup) -> str:
    for sel in STRIP_SELECTORS:
        for el in soup.select(sel):
            el.decompose()

    content = None
    for sel in CONTENT_SELECTORS:
        content = soup.select_one(sel)
        if content:
            break
    if content is None:
        content = soup.body or soup

    for a in content.find_all("a", href=True):
        href = a["href"]
        if href.startswith("/") or href.startswith("http"):
            a["href"] = urljoin(BASE_URL, href)
        a["target"] = "_blank"
        a["rel"]    = "noopener"

    for img in content.find_all("img"):
        src = img.get("src", "")
        if not src.startswith("http"):
            img.decompose()

    return content.decode_contents()


RETRIEVAL_FAILED_HTML = "<p>Content could not be retrieved.</p>"


def is_failed_section(html: str) -> bool:
    """True if this section's content is missing rather than merely short.

    Covers both failure shapes: get() returning nothing, and the server
    answering 200 with a body that extraction reduces to (almost) nothing.
    """
    return html == RETRIEVAL_FAILED_HTML or len(html.strip()) < MIN_SECTION_CONTENT_CHARS


async def fetch_sections_via_browser(
    code: str, sections: list[dict]
) -> tuple[list[tuple[str, str]], int]:
    """Fetch every section's page in a real browser and extract its content.

    Plain HTTP cannot be used here: the site answers requests-based clients
    with a JS challenge page, so extraction yielded empty content for every
    section (run #42 published 199 one-byte files before this was understood).

    Titles are written back onto the section dicts in place, for chapter TOC
    entries too. Returns ([(section_id, content_html)], failed_count) for the
    content sections only — chapter TOC entries contribute a title and no file.
    """
    results: list[tuple[str, str]] = []
    failed = 0
    content_total = sum(1 for s in sections if not s["isChapterToc"])
    done = 0
    diagnosed = 0

    async with async_playwright() as pw:
        browser, ctx = await _launch_context(pw)
        page = await ctx.new_page()
        try:
            for s in sections:
                raw = ""
                try:
                    await page.goto(s["url"], wait_until="networkidle", timeout=30_000)
                    await page.wait_for_timeout(400)
                    raw = await page.content()
                except Exception as e:
                    print(f"    (browser fetch failed for {s['url']}: {e})", file=sys.stderr)
                    # The page may be wedged — replace it before continuing.
                    try:
                        await page.close()
                    except Exception:
                        pass
                    page = await ctx.new_page()

                if raw:
                    soup = BeautifulSoup(raw, "lxml")
                    s["title"] = extract_title_from_html(soup)
                    content = extract_content_html(soup)
                else:
                    s["title"] = "Untitled Section"
                    content = RETRIEVAL_FAILED_HTML

                if s["isChapterToc"]:
                    continue  # title only; chapter TOCs are not stored as files

                done += 1
                print(f"  [{done}/{content_total}] {s['name']}")

                if is_failed_section(content):
                    failed += 1
                    if diagnosed < 3:
                        diagnosed += 1
                        snippet = " ".join(raw[:300].split())
                        print(f"    EMPTY EXTRACTION: {s['url']}", file=sys.stderr)
                        print(f"      raw={len(raw)}B extracted={len(content.strip())}ch "
                              f"title={s['title']!r} starts: {snippet}", file=sys.stderr)

                results.append((s["id"], content))

                # Bail as soon as the run is clearly failing rather than
                # putting hundreds more requests through a government server.
                if done >= EARLY_ABORT_SAMPLE and failed == done:
                    raise RuntimeError(
                        f"{code}: first {done} sections all came back empty — aborting "
                        f"before issuing {content_total - done} more requests. "
                        f"Leaving existing data untouched."
                    )
        finally:
            try:
                await page.close()
            except Exception:
                pass
            await ctx.close()
            await browser.close()

    return results, failed


# ── File writing ─────────────────────────────────────────────────────────────
def write_toc(code: str, change: int, sections: list[dict]) -> None:
    out_dir = DATA_DIR / code
    out_dir.mkdir(parents=True, exist_ok=True)

    toc_sections = [
        {
            "id":           s["id"],
            "title":        s["title"],
            "chapter":      s["chapter"],
            "section":      s["section"],
            "isChapterToc": s["isChapterToc"],
        }
        for s in sections
    ]

    payload = {
        "code":      code,
        "change":    change,
        "fetchedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "sections":  toc_sections,
    }
    path = out_dir / "toc.json"
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"  Wrote {path.relative_to(REPO_ROOT)}")


def write_section(code: str, section_id: str, html: str) -> None:
    out_dir = DATA_DIR / code / "s"
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / f"{section_id}.html"
    path.write_text(html, encoding="utf-8")


def existing_content_count(code: str) -> int:
    """Return the content-section count in the currently committed toc.json
    for this manual, or 0 if there isn't one / it can't be read. Used by the
    regression guard in process_manual() to detect a collapsed scrape."""
    path = DATA_DIR / code / "toc.json"
    if not path.exists():
        return 0
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
        return sum(1 for s in data.get("sections", []) if not s.get("isChapterToc"))
    except Exception:
        return 0


# ── Main ─────────────────────────────────────────────────────────────────────
def process_manual(entry: dict, force: bool = False) -> dict:
    code = entry["code"]
    name = entry["name"]
    print(f"\n{'='*60}")
    print(f"  Manual: {name} ({code})")

    _raw = entry.get("latestChange")
    known = _raw if _raw is not None else 1  # preserve 0 (special sentinel for TRT5)
    print("  Discovering latest change + collecting TOC URLs via Playwright…")
    latest, top_urls, chapter_section_urls = asyncio.run(
        fetch_change_and_toc_urls(code, known)
    )
    if latest is None:
        print("  Skipping — could not detect latest change number.")
        return entry

    if not force and latest == known and entry.get("hasContent"):
        print("  Already up-to-date. Skipping content fetch.")
        return {**entry, "latestChange": latest, "hasContent": True}

    if not top_urls:
        print("  No URLs found on TOC page — skipping content fetch.")
        return {**entry, "latestChange": latest}

    chapter_toc_urls = [u for u in top_urls if is_chapter_toc_name(Path(urlparse(u).path).name)]
    front_matter_urls = [u for u in top_urls if not is_chapter_toc_name(Path(urlparse(u).path).name)]
    print(f"  Found {len(chapter_toc_urls)} chapter TOC(s) + {len(front_matter_urls)} front-matter page(s).")

    # Section URLs were gathered during the Playwright pass above (Stage 2).
    all_section_urls: list[str] = list(front_matter_urls)
    all_section_urls.extend(chapter_toc_urls)  # keep chapter TOCs for isChapterToc metadata
    all_section_urls.extend(chapter_section_urls)
    print(f"  Stage 2 returned {len(chapter_section_urls)} section URL(s).")

    sections = build_toc_sections(all_section_urls)
    print(f"  Built {len(sections)} total section entries.")

    # Fetch titles and content for each non-chapter-TOC section
    content_sections = [s for s in sections if not s["isChapterToc"]]
    print(f"  Fetching titles + content for {len(content_sections)} content sections…")

    # ── Regression guard ────────────────────────────────────────────────────
    # A rate-limited or blocked run can still return HTTP 200 for every
    # request while Stage 2 finds zero real section links per chapter — that
    # is exactly what silently broke TOT5 for weeks (82/54 sections collapsed
    # to 29/1, and nothing flagged it because there was no HTTP error to
    # catch). Refuse to overwrite good data with a scrape that collapsed.
    prev_count = existing_content_count(code)
    if prev_count >= MIN_CONTENT_FOR_GUARD and len(content_sections) < prev_count * MIN_CONTENT_RATIO:
        raise RuntimeError(
            f"{code}: new scrape found only {len(content_sections)} content section(s), "
            f"down from {prev_count} currently on disk — looks like a blocked/rate-limited "
            f"scrape rather than real content loss. Leaving existing data untouched."
        )

    # Content is fetched through a browser (the site serves plain HTTP clients
    # a JS challenge instead of the document) and buffered in memory: the
    # quality check below must be able to abandon a bad scrape without having
    # left half-written files on disk for the publish step to pick up.
    # Chapter TOC titles are resolved in the same pass.
    fetched, failed = asyncio.run(fetch_sections_via_browser(code, sections))

    # The count guard above only proves we found the right *number* of
    # sections. If the fetches themselves are being served a challenge page,
    # every one extracts to nothing — right count, no content — and publishing
    # that is worse than keeping the current data.
    if content_sections and failed > len(content_sections) * MAX_FAILED_SECTION_RATIO:
        raise RuntimeError(
            f"{code}: {failed} of {len(content_sections)} section fetches returned no "
            f"content — the server is refusing content requests. Leaving existing data "
            f"untouched."
        )
    if failed:
        print(f"  Note: {failed}/{len(content_sections)} section(s) could not be retrieved.")

    for section_id, html in fetched:
        write_section(code, section_id, html)

    write_toc(code, latest, sections)

    return {**entry, "latestChange": latest, "hasContent": True}


def preflight_check() -> bool:
    """Verify basic network reachability and Playwright availability."""
    import traceback as _tb

    print("── Pre-flight checks ──────────────────────────────────────────────")

    # 1. Python version
    print(f"  Python {sys.version}")

    # 2. Key package versions (use importlib.metadata — not all packages set __version__)
    from importlib.metadata import version as _pkg_version, PackageNotFoundError

    for pkg, import_name in [("requests", "requests"), ("playwright", None), ("beautifulsoup4", "bs4")]:
        try:
            ver = _pkg_version(pkg)
            print(f"  {pkg} {ver}")
        except PackageNotFoundError:
            print(f"  {pkg} not installed!", file=sys.stderr)
            return False
        except Exception as e:
            # Version lookup failed but package may still work — don't abort
            print(f"  {pkg} (version unknown: {e})")

    # 3. Network connectivity to base site
    print(f"  Checking connectivity to {BASE_URL} …")
    try:
        r = session.get(BASE_URL, timeout=20)
        print(f"  {BASE_URL} → HTTP {r.status_code}")
        if r.status_code not in (200, 301, 302, 403):
            print(f"  WARNING: unexpected status {r.status_code}", file=sys.stderr)
    except Exception as e:
        print(f"  NETWORK ERROR reaching {BASE_URL}: {type(e).__name__}: {e}", file=sys.stderr)
        print("  The DoD site may be blocking GitHub Actions IPs, or is currently down.", file=sys.stderr)
        # Don't abort — individual manuals may still partially succeed

    # 4. Playwright browser binary
    print("  Checking Playwright Chromium executable…")
    try:
        from playwright.sync_api import sync_playwright
        with sync_playwright() as pw:
            info = pw.chromium.executable_path
            print(f"  Chromium binary: {info}")
            exists = Path(info).exists()
            print(f"  Exists on disk: {exists}")
            if not exists:
                print("  ERROR: Chromium binary missing — run 'playwright install chromium'", file=sys.stderr)
                return False
    except Exception as e:
        print(f"  Playwright check failed: {type(e).__name__}: {e}", file=sys.stderr)
        _tb.print_exc()
        return False

    print("── Pre-flight OK ──────────────────────────────────────────────────")
    return True


def main():
    parser = argparse.ArgumentParser(description="Fetch TRICARE manual content")
    parser.add_argument("--code",  help="Only process this manual code (e.g. TOT5)")
    parser.add_argument("--force", action="store_true",
                        help="Re-fetch even if latestChange hasn't increased")
    parser.add_argument("--skip-preflight", action="store_true",
                        help="Skip the pre-flight environment check")
    args = parser.parse_args()

    if not args.skip_preflight:
        ok = preflight_check()
        if not ok:
            sys.exit(1)

    if not MANUALS_JSON.exists():
        print(f"ERROR: {MANUALS_JSON} not found. "
              f"DATA_DIR resolved to: {DATA_DIR}", file=sys.stderr)
        sys.exit(1)

    with MANUALS_JSON.open(encoding="utf-8") as f:
        data = json.load(f)

    updated_manuals = []
    errors = []
    processed_count = 0
    for entry in data["manuals"]:
        if args.code and entry["code"] != args.code:
            updated_manuals.append(entry)
            continue
        if processed_count > 0:
            print(f"\nCooling down {MANUAL_COOLDOWN_SECS}s before next manual to avoid rate-limiting…")
            time.sleep(MANUAL_COOLDOWN_SECS)
        try:
            updated_manuals.append(process_manual(entry, force=args.force))
        except Exception as exc:
            import traceback
            print(f"\nERROR processing {entry['code']}: {type(exc).__name__}: {exc}", file=sys.stderr)
            traceback.print_exc()
            errors.append(entry["code"])
            updated_manuals.append(entry)  # keep existing entry unchanged
        processed_count += 1

    data["manuals"]     = updated_manuals
    data["lastUpdated"] = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    MANUALS_JSON.write_text(
        json.dumps(data, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8"
    )
    print(f"\nUpdated {MANUALS_JSON.relative_to(REPO_ROOT)}")

    if errors:
        print(f"\nWARNING: {len(errors)} manual(s) failed to update: {', '.join(errors)}", file=sys.stderr)
        print("Done (with errors).")
        sys.exit(1)

    print("Done.")


if __name__ == "__main__":
    import traceback as _top_tb
    try:
        main()
    except SystemExit:
        raise
    except Exception as _top_exc:
        print(f"\nFATAL UNHANDLED EXCEPTION: {type(_top_exc).__name__}: {_top_exc}", file=sys.stderr)
        _top_tb.print_exc()
        sys.exit(1)
