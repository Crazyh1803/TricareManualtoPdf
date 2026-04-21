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
from urllib.parse import urljoin, urlparse, parse_qs

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

# Throttle between requests (seconds) — be a polite scraper
REQUEST_DELAY = 0.5

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
    """Click 'Expand All' if present and wait for the tree to render."""
    selectors = [
        "a:has-text('Expand All')",
        "button:has-text('Expand All')",
        "a[title*='Expand']",
        "a:has-text('Expand')",
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


async def fetch_change_and_toc_urls(code: str, known_change: int) -> tuple[int | None, list[str]]:
    """
    Single Playwright session that detects the latest change number AND
    collects the top-level TOC URLs (chapter TOCs + front matter).

    Version detection:
      Start from known_change (from manuals.json).  Check up to 5 increments
      ahead — a change is "valid" only if its TOC page actually renders
      DisplayManualHtmlFile section links.  This avoids the false-positive
      caused by the site always returning HTTP 200, even for invalid changes.

    Returns (latest_change, top_level_urls).
    """
    async with async_playwright() as pw:
        browser = await pw.chromium.launch(headless=True)
        ctx = await browser.new_context(ignore_https_errors=True)
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
                    for candidate in range(served_change + 1, served_change + 6):
                        print(f"  [{code}] Checking change {candidate}…")
                        if await _toc_has_sections(ctx, code, candidate):
                            latest = candidate
                        else:
                            break
                else:
                    print(f"  [{code}] Known change {latest} has no sections (server issue?)", file=sys.stderr)
            else:
                for candidate in range(known_change + 1, known_change + 6):
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

                await page.wait_for_timeout(2_000)
                await _expand_toc(page)
                await page.wait_for_timeout(3_000)

                urls: list[str] = []
                for wait_s in (0, 2, 5):
                    if wait_s:
                        await page.wait_for_timeout(wait_s * 1_000)
                    urls = await _collect_display_hrefs(page, toc_url)
                    print(f"  [{code}] After expand+{wait_s}s: {len(urls)} URLs found")
                    if urls:
                        break

                if not urls:
                    all_hrefs: list[str] = []
                    for frame in page.frames:
                        try:
                            hs = await frame.eval_on_selector_all(
                                "a", "els => els.map(a => a.href || '')"
                            )
                            all_hrefs.extend(h for h in (hs or []) if h)
                        except Exception:
                            pass
                    print(f"  [{code}] Sample hrefs on page (first 10):", file=sys.stderr)
                    for h in list(dict.fromkeys(all_hrefs))[:10]:
                        print(f"    {h}", file=sys.stderr)

                return latest, urls
            finally:
                await page.close()
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
    seen: set[str] = set()
    results: list[str] = []

    soup = BeautifulSoup(r.text, "lxml")
    for a in soup.find_all("a", href=True):
        href = a["href"].strip()
        if not href:
            continue
        full = urljoin(BASE_URL, href)
        if urlparse(full).netloc != host:
            continue
        if not is_display_html(full):
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


def fetch_section_html(url: str) -> tuple[str, str]:
    r = get(url)
    if r is None:
        return "Untitled Section", "<p>Content could not be retrieved.</p>"
    soup    = BeautifulSoup(r.text, "lxml")
    title   = extract_title_from_html(soup)
    content = extract_content_html(soup)
    return title, content


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


# ── Main ─────────────────────────────────────────────────────────────────────
def process_manual(entry: dict, force: bool = False) -> dict:
    code = entry["code"]
    name = entry["name"]
    print(f"\n{'='*60}")
    print(f"  Manual: {name} ({code})")

    known = entry.get("latestChange", 1) or 1
    print("  Discovering latest change + collecting TOC URLs via Playwright…")
    latest, top_urls = asyncio.run(fetch_change_and_toc_urls(code, known))
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

    # ── Stage 2: get individual section URLs from each chapter TOC page ────────
    print("  Stage 2: collecting section URLs from chapter TOC pages…")
    all_section_urls: list[str] = list(front_matter_urls)
    all_section_urls.extend(chapter_toc_urls)  # keep chapter TOCs for isChapterToc metadata
    for i, chap_url in enumerate(chapter_toc_urls, 1):
        sec_urls = fetch_chapter_section_urls(chap_url)
        print(f"    Chapter TOC {i}/{len(chapter_toc_urls)}: {len(sec_urls)} section(s)")
        all_section_urls.extend(sec_urls)

    sections = build_toc_sections(all_section_urls)
    print(f"  Built {len(sections)} total section entries.")

    # Fetch titles and content for each non-chapter-TOC section
    content_sections = [s for s in sections if not s["isChapterToc"]]
    print(f"  Fetching titles + content for {len(content_sections)} content sections…")

    for i, s in enumerate(content_sections, 1):
        print(f"  [{i}/{len(content_sections)}] {s['name']}")
        title, html = fetch_section_html(s["url"])
        s["title"] = title
        write_section(code, s["id"], html)

    # Fetch titles for chapter TOC entries (not stored as content, title only)
    for s in sections:
        if s["isChapterToc"]:
            r = get(s["url"])
            if r:
                soup = BeautifulSoup(r.text, "lxml")
                s["title"] = extract_title_from_html(soup)

    write_toc(code, latest, sections)

    return {**entry, "latestChange": latest, "hasContent": True}


def main():
    parser = argparse.ArgumentParser(description="Fetch TRICARE manual content")
    parser.add_argument("--code",  help="Only process this manual code (e.g. TOT5)")
    parser.add_argument("--force", action="store_true",
                        help="Re-fetch even if latestChange hasn't increased")
    args = parser.parse_args()

    with MANUALS_JSON.open(encoding="utf-8") as f:
        data = json.load(f)

    updated_manuals = []
    for entry in data["manuals"]:
        if args.code and entry["code"] != args.code:
            updated_manuals.append(entry)
            continue
        updated_manuals.append(process_manual(entry, force=args.force))

    data["manuals"]     = updated_manuals
    data["lastUpdated"] = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    MANUALS_JSON.write_text(
        json.dumps(data, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8"
    )
    print(f"\nUpdated {MANUALS_JSON.relative_to(REPO_ROOT)}")
    print("Done.")


if __name__ == "__main__":
    main()
