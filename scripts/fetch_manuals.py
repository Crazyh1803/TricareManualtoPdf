#!/usr/bin/env python3
"""
TRICARE Manuals — Content Scraper
==================================
Fetches manual content from manuals.dha.mil and writes static files
to docs/data/ for consumption by the GitHub Pages web app.

The old site, manuals.health.mil, was retired: every URL now redirects to the
new site's home page, so the previous ASP.NET endpoints returned a valid but
wrong page, which the scraper read as "no sections, already up to date".

Each manual has one publication page listing its revision number and every
section, so discovery is a single page load. Content is a Blazor app rendered
client-side — a plain HTTP request gets back only a ~6 KB shell with no links,
no title and no revision — so all fetching goes through Playwright (headless
Chromium). The site sits behind bot defence; requests are paced and retried
rather than parallelised, and the guards below refuse to publish a scrape that
comes back empty instead of overwriting good data with it.

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
# manuals.health.mil was retired: it now redirects every URL — deep links
# included — to this site's home page, so the old ASP.NET endpoints returned a
# valid but wrong page and the scraper read that as "no sections, already up to
# date". Nothing has been genuinely fetched since.
BASE_URL = "https://manuals.dha.mil"

# One page per manual lists every section in the publication, so there is no
# per-chapter drilling any more. The revision number is printed on it.
PUBLICATION_URL = BASE_URL + "/View-Publication/{code}"

# Section content comes from the endpoint the site's own front end uses. Each
# link on the publication page carries hx-get="/api/publication/<code>/<rev>/
# <file>" and htmx swaps that fragment into #publication-document, so the
# endpoint returns the document itself — no page chrome, no Alpine wrapper, no
# loading overlay. Fetching it directly is both cleaner and far cheaper than
# driving a browser to every section.
SECTION_API_URL = BASE_URL + "/api/publication/{code}/{revision}/{name}"

# The endpoint answers an unadorned request with the bot-defence interstitial
# ("Please enable JavaScript to view the page content. Your support ID is …",
# ~6 KB). Sending the same headers htmx does gets the real document, verified
# byte-for-byte against a browser fetch of the same URL.
HX_HEADERS = {
    "HX-Request": "true",
    "HX-Target": "publication-document",
}
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
# The site rate-limits aggressively; 0.5s was too fast.
REQUEST_DELAY = 1.5

# Cooldown between processing different manuals (seconds).
# After scraping 50–80+ sections for one manual the server starts
# resetting connections.  A 60-second pause lets the rate-limit
# window reset before we begin the next manual.
MANUAL_COOLDOWN_SECS = 60

# The workflow job is capped at 2 hours and its commit step runs *after* this
# script, so a run that overruns publishes nothing at all — two hours of work
# thrown away and the site left stale (exactly what happened on 2026-09-07).
#
# Two budgets keep the run inside that cap. MANUAL_TIME_BUDGET_SECS caps any
# single manual so one pathological manual cannot starve the rest; a manual is
# only *started* while RUN_TIME_BUDGET_SECS still has a full manual budget
# left, so the worst case is RUN_TIME_BUDGET_SECS of wall clock. That leaves
# roughly 20 minutes of the job's 2 hours for the setup steps before this
# script and the commit step after it.
RUN_TIME_BUDGET_SECS = 100 * 60
MANUAL_TIME_BUDGET_SECS = 30 * 60

# Pause between browser navigations when fetching section content. The site
# starts resetting connections (net::ERR_CONNECTION_RESET) when pages are
# requested back to back — the same behaviour MANUAL_COOLDOWN_SECS above was
# added for. The browser path needs its own throttle; it does not inherit
# REQUEST_DELAY, which only applies to the requests session.
SECTION_NAV_DELAY = 2.0

# A reset connection is transient, so retry rather than writing the section
# off. Backoff is per attempt (8s, then 16s), which also lets a rate-limit
# window drain before trying again.
SECTION_FETCH_ATTEMPTS = 3
SECTION_RETRY_BACKOFF = 8.0

# Cool-off after a section exhausts its retries. Retrying hammers the site with
# extra navigations, and that burst can trip throttling that then takes out the
# *following* sections: re-scraping TPT5 retried its canvas section 001 and
# then lost 003, 004 and 005 — all of which had real content the run before —
# to consecutive failures. Backing off after a failure lets that window drain
# instead of carrying the throttling into healthy pages.
FAILED_SECTION_COOLDOWN = 20.0

# Version-detection probes need the same treatment. They were unthrottled and
# single-shot, and since the forward walk stops at the first failure, one
# throttled probe silently caps the detected version (TPT5 came back as 55 on
# one run and 51 on another with identical code).
# Retained for the publication-page load, which is the only probe left.
PROBE_DELAY = 1.5

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

# The bot-defence interstitial the site serves instead of a document. It is
# returned with HTTP 200 and a plausible body, so it has to be recognised by
# its text. Retrying it is pointless — it is a definitive refusal, not a
# transient error — and backing off three times per section is what turns a
# blocked run into ten minutes of waiting before the abort guard fires.
CHALLENGE_MARKERS = (
    "Please enable JavaScript to view the page content",
    "Your support ID is",
)

# If this many sections are attempted and every one comes back empty, stop
# immediately rather than working through hundreds of doomed requests against
# a government server before the ratio check fires at the end.
EARLY_ABORT_SAMPLE = 10

# How many interstitials in a row before giving up on the manual. More than
# one, so a single odd response does not abandon a run; far fewer than
# EARLY_ABORT_SAMPLE, because a refused session will not recover on its own.
CHALLENGE_ABORT_AFTER = 3

# How long a browser-established session lasts before the site stops honouring
# it. Measured, not guessed: TPT5 fetched 253 sections cleanly and was refused
# on the 254th exactly 10m09s in, while TOT5 finished in 10m04s and TRT5 in
# 6m47s and neither saw a single challenge. The session is renewed before that
# ceiling rather than after, since a refusal costs a wasted request.
SESSION_REFRESH_SECS = 8 * 60

# How long to let a section page settle after navigation. The retry pass uses
# the slower value together with wait_until="networkidle", to give genuinely
# slow JS-rendered pages a second chance before they are written off.
SECTION_SETTLE_MS = 700
SECTION_SETTLE_SLOW_MS = 3_000

# CSS selectors for site chrome to strip from section HTML
STRIP_SELECTORS = [
    "header", "footer", "nav", "#navigation", "#header", "#footer",
    ".navbar", ".breadcrumb", ".breadcrumbs",
    "[role='banner']", "[role='navigation']",
    "script", "style", "noscript",
    # New-site chrome. #leftNav is the in-page chapter tree that sits as a
    # sibling of the content column, and .no-print marks the controls the site
    # itself excludes from print — both are navigation, not manual text.
    "#leftNav", ".no-print", "#gov-banner", ".usa-banner",
]

# Selectors for the main content area (tried in order). The section body is a
# single stable node on the new site; the rest are fallbacks so a markup change
# degrades to "too much chrome" rather than to nothing at all.
CONTENT_SELECTORS = [
    "#content-body",
    "main#main-content",
    "[role='main']",
    "main",
    "body",
]

# Front-matter filenames (sorted before chapters)
_FRONT_MATTER = {"FOREWORD", "INTRO", "PREFACE", "SUMMARY"}


# ── HTTP session ────────────────────────────────────────────────────────────
session = requests.Session()
session.headers.update(HEADERS)
# manuals.dha.mil presents a chain that validates against the standard bundle,
# so verification stays on. (The retired health.mil host needed it disabled;
# that is no longer a reason to send unverified requests.)


def get(url: str, headers: dict | None = None) -> requests.Response | None:
    """GET with retries and polite throttling."""
    time.sleep(REQUEST_DELAY)
    for attempt in range(3):
        try:
            r = session.get(url, timeout=45, headers=headers)
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


# ── URL helpers ──────────────────────────────────────────────────────────────

def is_chapter_toc_name(name: str) -> bool:
    """True for navigation-only pages: per-chapter TOCs and the master TOC.

    These get a title in our own TOC but no stored file — the web app builds
    its own navigation, so mirroring the site's would just duplicate it.
    Accepts an optional .html suffix so pre-migration names still match.
    """
    base = name.upper().split(".", 1)[0]
    return bool(re.match(r"^C\d+TOC$", base)) or base.endswith("TOC")


def natural_sort_key(name: str):
    """Sort filenames in logical manual order: front matter → chapters (TOC heading first, then sections, then addenda)."""
    base = name.split(".", 1)[0].upper()
    # Strip common manual-code prefix (e.g. "TST5_", "TPT5_")
    base = re.sub(r"^[A-Z]{3,5}\d*_", "", base)

    # Master TOC ("TPT5TOC", "MASTERTOC") sorts ahead of everything.
    if base.endswith("TOC") and not re.match(r"^C\d+TOC$", base):
        return (-2, 0, 0, 0, base)

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

_REVISION_RE = re.compile(r"Revision\s+(\d+)\s*\(Published\s*([^)]*)\)", re.I)
_FILENAME_RE = re.compile(
    r"/View-Publication/([^/]+)/Revision/(\d+)/FileName/([^/?#]+)", re.I
)


async def _launch_context(pw):
    """Browser + context configured to get past the site's JS bot challenge.

    The site is a Blazor app behind bot defence: a plain HTTP request returns
    only a ~6 KB shell with no links, no title and no revision, which is why
    requests-based fetching silently produced empty content. A real browser
    renders the page, so every fetch has to come through here.
    """
    browser = await pw.chromium.launch(
        headless=True,
        args=["--disable-blink-features=AutomationControlled"],
    )
    ctx = await browser.new_context(
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


async def _open_publication(code: str):
    """Load a manual's publication page in Chromium; return (body, links, cookies).

    Shared by discovery and session renewal: both need a real browser to visit
    the page and run the bot-defence challenge, and renewal gets the revision
    and link list for free.
    """
    url = PUBLICATION_URL.format(code=code)
    async with async_playwright() as pw:
        browser, ctx = await _launch_context(pw)
        page = await ctx.new_page()
        try:
            try:
                await page.goto(url, wait_until="networkidle", timeout=60_000)
            except Exception as e:
                print(f"  [{code}] goto failed ({e}); continuing with whatever rendered",
                      file=sys.stderr)
            await page.wait_for_timeout(4_000)
            body = ""
            try:
                body = await page.inner_text("body")
            except Exception:
                pass
            pairs = await page.eval_on_selector_all(
                "a[href]",
                "els => els.map(e => [e.getAttribute('href'), (e.innerText || '').trim()])",
            )
            cookies = await ctx.cookies()
        finally:
            try:
                await page.close()
            except Exception:
                pass
            await ctx.close()
            await browser.close()
    return body, pairs, cookies


def renew_session(code: str) -> float:
    """Re-establish the HTTP session's cookies through the browser.

    The site expires a session about ten minutes after it is created, and then
    answers every request with the interstitial instead of content. Visiting
    the publication page again in Chromium re-runs the challenge exactly as a
    reader leaving a tab open would, so long manuals continue rather than
    stopping two thirds of the way through.
    """
    print(f"  [{code}] Renewing the browser session…")
    _, _, cookies = asyncio.run(_open_publication(code))
    adopt_browser_cookies(cookies)
    return time.monotonic()


async def fetch_publication(code: str) -> tuple[int | None, str, list[tuple[str, str]]]:
    """Read a manual's revision number and every section link from one page.

    Everything the old two-stage crawl existed for is on this single page: the
    revision is printed as "Revision 56 (Published Sep 4, 2026)", and every
    section is linked as /View-Publication/<code>/Revision/<n>/FileName/<name>.
    That removes both the forward-walk probe — which inferred the change number
    by testing whether successive TOC pages rendered, and so silently answered
    "no newer change" once the old site went away — and the per-chapter TOC
    pass, which was what provoked the connection resets.

    Returns (revision, published_date, [(url, link_text)]). The link text is
    carried out because every section page shares one generic <title>; the
    publication page is the only place a real title is available.
    """
    body, pairs, cookies = await _open_publication(code)

    # Hand the browser's session to the HTTP client. The content endpoint is
    # behind bot defence that decides per session, not per request: a client
    # that has not executed the challenge script gets the interstitial, and
    # our own pre-flight GET was enough to earn a cookie marking us as one.
    # Chromium runs the challenge as any reader's browser does, so continuing
    # with its cookies is both the honest thing to send and the thing that
    # works — the first FR16 run was refused on all 10 sections it tried.
    adopt_browser_cookies(cookies)

    links: list[tuple[str, str]] = []
    seen: set[str] = set()
    revisions: set[int] = set()
    for href, text in pairs:
        if not href:
            continue
        m = _FILENAME_RE.search(href)
        if not m or m.group(1).upper() != code.upper():
            continue
        revisions.add(int(m.group(2)))
        full = urljoin(BASE_URL, href.split("?", 1)[0])
        if full in seen:
            continue
        seen.add(full)
        links.append((full, text))

    published = ""
    revision: int | None = None
    m = _REVISION_RE.search(body)
    if m:
        revision, published = int(m.group(1)), m.group(2).strip()
    elif revisions:
        # The printed string is the intended source; fall back to the revision
        # the links themselves were built with rather than giving up, but say
        # so, because a missing string usually means the markup moved.
        revision = max(revisions)
        print(f"  [{code}] revision string not found on the page; using "
              f"Revision={revision} from the section links.", file=sys.stderr)

    return revision, published, links


# Wall-clock deadline for the manual currently being processed. Set by
# process_manual(); consulted by the section-fetch loop, the only place a
# manual can now spend unbounded time. None means "no limit" (tests).
_manual_deadline: float | None = None


def manual_time_exhausted() -> bool:
    return _manual_deadline is not None and time.monotonic() > _manual_deadline


def build_toc_sections(links: list[tuple[str, str]]) -> list[dict]:
    """Convert (url, link_text) pairs from the publication page into sections.

    The id is the site's own FileName token ("C7S18_2", "FOREWORD") rather than
    a position in the list. Positional ids shifted whenever a section was added
    upstream, which silently re-pointed every later section's stored file at
    the wrong heading and made content preservation unsafe; a name-based id is
    stable across scrapes for as long as the source file keeps its name.
    """
    seen: set[str] = set()
    sections: list[dict] = []

    for url, text in links:
        name = Path(urlparse(url).path).name
        if not name:
            continue
        key = name.upper()
        if key in seen:
            continue
        seen.add(key)

        chapter, section = parse_chapter_section(name)
        sections.append({
            "url":          url,
            "name":         name,
            # Ids become filenames, so keep them to characters that are safe
            # everywhere. The site's tokens are already alphanumeric, but a
            # future one need not be.
            "id":           re.sub(r"[^A-Za-z0-9_.-]", "_", name),
            "chapter":      chapter,
            "section":      section,
            "title":        clean_title(text) or name,
            "isChapterToc": is_chapter_toc_name(name),
        })

    sections.sort(key=lambda s: natural_sort_key(s["name"]))
    return sections


# ── Title cleaning ───────────────────────────────────────────────────────────
_TITLE_PREFIXES = [
    "TRICARE Manuals - Display ",
    "TRICARE Manuals - ",
]
# The site writes chapter labels as "Chap 1 -- Administration".
_DASH_SEP = re.compile(r"\s+--\s+")
_CHANGE_SUFFIX = re.compile(r",?\s*\(?Change\s+\d+.*$", re.IGNORECASE)
_CHAP_SECT     = re.compile(r"Chap\s+(\d+)\s+Sect\s+([\d.]+)", re.IGNORECASE)
_CHAP_TOC      = re.compile(r"Chap\s+(\d+)\s+TOC", re.IGNORECASE)
_CHAP_ONLY     = re.compile(r"Chap\s+(\d+)", re.IGNORECASE)


def clean_title(raw: str) -> str:
    title = " ".join((raw or "").split())
    title = _DASH_SEP.sub(" — ", title)
    for prefix in _TITLE_PREFIXES:
        if title.startswith(prefix):
            title = title[len(prefix):]
    title = _CHANGE_SUFFIX.sub("", title)
    title = _CHAP_SECT.sub(r"Chapter \1, Section \2", title)
    title = _CHAP_TOC.sub(r"Chapter \1 – Contents", title)
    title = _CHAP_ONLY.sub(r"Chapter \1", title)
    return title.strip() or raw.strip()


def extract_title_from_html(soup: BeautifulSoup) -> str:
    """Fallback title read from the document itself.

    Only used when the publication page gave no link text for a section: that
    link text ("Chap 199.1 -- General Provisions", "Sect 1.1 -- General Policy
    And Responsibilities") is the best title the site offers, and this must
    never override it.

    Scoped to #publication-document deliberately. An earlier version searched
    #content-body for its first heading, which is the site's own "Publication
    Information" panel sitting above the document — so every section in the
    first FR16 run came back titled "Publication Information". The <title> tag
    is no use either; every page shares "View Publication | TRICARE Manuals".

    Manuals label their documents differently — FR16 (CFR) uses .Chapter plus
    .CFRSubject, TPT5 uses .MPChapterTitle — so several are tried and the
    number and subject are joined when both exist.
    """
    doc = soup.select_one("#publication-document") or soup

    # Try in priority order, one selector at a time: a comma-separated
    # select_one returns whichever match comes first in the *document*, not
    # first in the list, which picked .MPChapterTitle ("Civilian Health And
    # Medical Program…") over the section's own .CFRSubject.
    def first(*selectors):
        for sel in selectors:
            el = doc.select_one(sel)
            if el and el.get_text(" ", strip=True):
                return el
        return None

    number = first(".Chapter", ".Section")
    subject = first(".CFRSubject", ".Subject", ".SectionTitle", ".MPChapterTitle")
    parts = [el.get_text(" ", strip=True) for el in (number, subject) if el]
    joined = clean_title(" ".join(p for p in parts if p))
    if joined:
        return joined

    for tag in ("h1", "h2", "h3"):
        el = doc.find(tag)
        if el:
            text = clean_title(el.get_text(" ", strip=True))
            if text:
                return text
    return ""


# ── Section content extraction ───────────────────────────────────────────────
def extract_content_html(soup: BeautifulSoup, page_url: str = BASE_URL) -> str:
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
        # Resolve against the page, not the site root: cross-references
        # between sections are written relative to the document, so joining
        # them onto BASE_URL produced dead links.
        a["href"] = urljoin(page_url, href)
        a["target"] = "_blank"
        a["rel"]    = "noopener"

    for img in content.find_all("img"):
        src = img.get("src", "")
        if not src.startswith("http"):
            img.decompose()

    return content.decode_contents()


RETRIEVAL_FAILED_HTML = "<p>Content could not be retrieved.</p>"


def unavailable_html(url: str) -> str:
    """Stand-in for a section whose text could not be extracted.

    Some pages draw their content into a <canvas> rather than emitting HTML —
    every manual's front matter does this, and so does FR16 chapter 199.1.
    There is no text in the DOM to recover at any wait duration, and the
    43-byte canvas element that extraction returns reads as missing content in
    both the reader and the Markdown export. A link to the official page is
    honest and usable; a blank section is neither.
    """
    return (
        "<p><em>This section could not be extracted as text — the official "
        "page renders it as an image.</em></p>\n"
        f'<p><a href="{url}" target="_blank" rel="noopener">'
        "Read this section on manuals.dha.mil</a></p>"
    )


def is_challenge_page(html: str) -> bool:
    """True if the server returned its bot-defence interstitial, not content."""
    return any(marker in html for marker in CHALLENGE_MARKERS)


def is_failed_section(html: str) -> bool:
    """True if this section's content is missing rather than merely short.

    Covers both failure shapes: get() returning nothing, and the server
    answering 200 with a body that extraction reduces to (almost) nothing.
    """
    return html == RETRIEVAL_FAILED_HTML or len(html.strip()) < MIN_SECTION_CONTENT_CHARS


def adopt_browser_cookies(cookies: list[dict]) -> None:
    """Copy Playwright context cookies into the module's requests session."""
    added = 0
    for c in cookies:
        name, value = c.get("name"), c.get("value")
        if not name or value is None:
            continue
        session.cookies.set(name, value,
                            domain=c.get("domain") or "",
                            path=c.get("path") or "/")
        added += 1
    if added:
        print(f"  Carried {added} browser cookie(s) into the HTTP session.")


def section_api_url(section_url: str) -> str | None:
    """Map a /View-Publication/.../FileName/X link to its /api/publication URL."""
    m = _FILENAME_RE.search(section_url)
    if not m:
        return None
    return SECTION_API_URL.format(code=m.group(1), revision=m.group(2), name=m.group(3))


def fetch_section_html(url: str) -> str:
    """GET one section fragment, or "" if it could not be retrieved.

    Forces UTF-8: the endpoint does not declare a charset, so requests guesses
    latin-1 and the manuals' typography comes through as mojibake ("Â«" for
    the « in the section navigation).
    """
    api = section_api_url(url)
    if api is None:
        print(f"    not a section URL: {url}", file=sys.stderr)
        return ""
    # Referer is per-manual, so it is built here rather than in HX_HEADERS.
    headers = {**HX_HEADERS, "Referer": url}
    r = get(api, headers=headers)
    if r is None:
        return ""
    r.encoding = "utf-8"
    return r.text


def fetch_sections(code: str, sections: list[dict]) -> tuple[list[tuple[str, str]], int, int]:
    """Fetch every section's document and extract its content.

    Uses the site's own content endpoint over plain HTTP. Content previously
    came through Playwright because the rendered page was the only way to see
    a document at all; the endpoint returns the same document directly, so a
    browser is now needed only once per manual, for the publication page.

    Titles are written back onto the section dicts in place, chapter TOC
    entries included. Returns ([(section_id, content_html)], failed_count,
    preserved_count) for the content sections only — chapter TOC entries
    contribute a title and no file.

    A section whose fetch fails keeps whatever content the manual already had
    under that source name, rather than being overwritten with a placeholder.
    It still counts as failed, so the abort guards see the true failure rate.
    """
    results: list[tuple[str, str]] = []
    failed = 0
    preserved = 0
    content_total = sum(1 for s in sections if not s["isChapterToc"])
    done = 0
    diagnosed = 0
    consecutive_challenges = 0

    # Read before any writes — the loop below only buffers, so the files on
    # disk still hold the previous run's content while we are fetching.
    previous = load_previous_sections(code)
    # The publication page was fetched moments ago by process_manual, so the
    # session starts its clock here.
    session_started = time.monotonic()

    for s in sections:
        # Renew before the site stops honouring the session, rather than
        # discovering it has lapsed by being refused.
        if time.monotonic() - session_started > SESSION_REFRESH_SECS:
            session_started = renew_session(code)

        # Give up on this manual rather than running until the job is killed.
        if manual_time_exhausted():
            raise RuntimeError(
                f"{code}: exceeded the {MANUAL_TIME_BUDGET_SECS // 60}-minute "
                f"budget after {done}/{content_total} sections — abandoning it so "
                f"the run can finish and publish. Existing data left untouched."
            )

        raw = ""
        content = RETRIEVAL_FAILED_HTML
        ok = False
        challenge = False
        renewed_here = False
        for attempt in range(1, SECTION_FETCH_ATTEMPTS + 1):
            # get() already paces requests and retries transport errors; this
            # outer loop exists for the case that matters more here — a 200
            # whose body extracts to nothing, which the site returns when it
            # serves the bot-defence interstitial instead of the document.
            raw = fetch_section_html(s["url"])
            if is_challenge_page(raw):
                if not renewed_here:
                    # Most likely the session simply lapsed. Re-establish it
                    # through the browser and try this section once more
                    # before treating the refusal as real.
                    renewed_here = True
                    session_started = renew_session(code)
                    continue
                # A refusal that survives a fresh session: retrying the same
                # request earns another interstitial, not content.
                challenge = True
                break
            if raw.strip():
                soup = BeautifulSoup(raw, "lxml")
                # Only fill in a title the publication page could not supply;
                # its link text is the better source.
                if not s.get("title") or s["title"] == s["name"]:
                    heading = extract_title_from_html(soup)
                    if heading:
                        s["title"] = heading
                content = extract_content_html(soup, s["url"])
                if not is_failed_section(content):
                    ok = True
                    break
            if attempt < SECTION_FETCH_ATTEMPTS:
                time.sleep(SECTION_RETRY_BACKOFF * attempt)

        if s["isChapterToc"]:
            continue  # title only; chapter TOCs are not stored as files

        consecutive_challenges = consecutive_challenges + 1 if challenge else 0

        done += 1
        print(f"  [{done}/{content_total}] {s['name']}"
              + ("  (refused: bot-defence interstitial)" if challenge else ""))

        if not ok:
            failed += 1
            if diagnosed < 3:
                diagnosed += 1
                snippet = " ".join(raw[:300].split())
                print(f"    EMPTY EXTRACTION: {s['url']}", file=sys.stderr)
                print(f"      raw={len(raw)}B extracted={len(content.strip())}ch "
                      f"title={s['title']!r} starts: {snippet}", file=sys.stderr)
            kept = previous.get(s["name"])
            if kept:
                # We already hold real text for this section; a failed fetch is
                # no reason to throw it away.
                content = kept["html"]
                s["title"] = kept["title"] or s["title"]
                preserved += 1
                print(f"    kept previous content for {s['name']} ({len(content):,}B)")
            else:
                content = unavailable_html(s["url"])
            # Back off before the next section: a failure usually means we are
            # being throttled, and the retries just added to the burst.
            time.sleep(FAILED_SECTION_COOLDOWN)

        results.append((s["id"], content))

        # Consecutive interstitials mean the session is being refused rather
        # than throttled, so there is nothing to gain from the rest of the
        # manual. Counted consecutively, and reset by any success, so an
        # occasional challenge mid-run does not abandon a scrape that is
        # otherwise working.
        if consecutive_challenges >= CHALLENGE_ABORT_AFTER:
            raise RuntimeError(
                f"{code}: the server returned its bot-defence interstitial for "
                f"{consecutive_challenges} sections in a row instead of content "
                f"— aborting. Leaving existing data untouched."
            )

        # Bail as soon as the run is clearly failing rather than putting
        # hundreds more requests through a government server.
        if done >= EARLY_ABORT_SAMPLE and failed == done:
            raise RuntimeError(
                f"{code}: first {done} sections all came back empty — aborting "
                f"before issuing {content_total - done} more requests. "
                f"Leaving existing data untouched."
            )

    return results, failed, preserved


# ── File writing ─────────────────────────────────────────────────────────────
def write_toc(code: str, change: int, sections: list[dict]) -> None:
    out_dir = DATA_DIR / code
    out_dir.mkdir(parents=True, exist_ok=True)

    toc_sections = [
        {
            "id":           s["id"],
            # Source filename (e.g. "C1S3.html"). Ids are positional and shift
            # whenever the section count changes, so this is the only stable
            # identity a section has across scrapes — it is what lets the next
            # run match a failed fetch to the content it already holds. The web
            # app ignores the field.
            "name":         s["name"],
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


def load_previous_sections(code: str) -> dict[str, dict]:
    """Map source filename -> {title, html} for the content already on disk.

    Used to keep a section's existing content when a fetch fails, instead of
    replacing real text with a placeholder. Keyed on the source filename
    rather than the id, because ids are positional and shift whenever the
    section count changes.

    Returns an empty map for a manual whose toc.json predates the "name"
    field, so the first run after that change simply has nothing to preserve
    from — matching on id instead would risk restoring one section's content
    under another section's heading.
    """
    path = DATA_DIR / code / "toc.json"
    if not path.exists():
        return {}
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return {}

    out: dict[str, dict] = {}
    for s in data.get("sections", []):
        name = s.get("name")
        if not name or s.get("isChapterToc"):
            continue
        html_path = DATA_DIR / code / "s" / f"{s.get('id')}.html"
        if not html_path.exists():
            continue
        try:
            html = html_path.read_text(encoding="utf-8")
        except Exception:
            continue
        # Only worth keeping if it is real content — never carry a previous
        # placeholder or an empty file forward as though it were a rescue.
        if is_failed_section(html) or "could not be extracted" in html:
            continue
        out[name] = {"title": s.get("title", ""), "html": html}
    return out


def prune_stale_sections(code: str, keep_ids: set[str]) -> int:
    """Delete section files the new TOC no longer references.

    Nothing has ever cleaned these up, so files linger whenever a manual's
    section count shrinks or its numbering shifts. They are invisible to the
    web app (it only follows toc.json) but they accumulate in the repo and
    make it hard to tell healthy data from leftovers when inspecting it.
    Only called once the guards have accepted the scrape.
    """
    out_dir = DATA_DIR / code / "s"
    if not out_dir.is_dir():
        return 0
    removed = 0
    for path in out_dir.glob("*.html"):
        if path.stem not in keep_ids:
            path.unlink()
            removed += 1
    return removed


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
    global _manual_deadline

    code = entry["code"]
    name = entry["name"]
    print(f"\n{'='*60}")
    print(f"  Manual: {name} ({code})")

    # Arm the wall-clock ceiling for this manual (see MANUAL_TIME_BUDGET_SECS).
    _manual_deadline = time.monotonic() + MANUAL_TIME_BUDGET_SECS

    _raw = entry.get("latestChange")
    known = _raw if _raw is not None else 1  # preserve 0 (special sentinel for TRT5)
    print("  Reading revision + section list from the publication page…")
    latest, published, links = asyncio.run(fetch_publication(code))
    if latest is None:
        print("  Skipping — the publication page did not yield a revision number.")
        return entry
    print(f"  Revision {latest}"
          + (f" (published {published})" if published else "")
          + f"; {len(links)} section link(s).")

    if not links:
        # A publication page that renders no section links is the signature of
        # a blocked or half-rendered load, not of an empty manual. Refuse it
        # rather than recording a revision we never actually read content for.
        print("  No section links found — leaving existing data untouched.", file=sys.stderr)
        return entry

    if not force and latest == known and entry.get("hasContent"):
        print("  Already up-to-date. Skipping content fetch.")
        return {**entry, "latestChange": latest, "hasContent": True}

    sections = build_toc_sections(links)
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
    fetched, failed, preserved = fetch_sections(code, sections)

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
        lost = failed - preserved
        print(f"  Note: {failed}/{len(content_sections)} section(s) could not be "
              f"retrieved — {preserved} kept their previous content, "
              f"{lost} fell back to a source link.")

    for section_id, html in fetched:
        write_section(code, section_id, html)

    stale = prune_stale_sections(code, {section_id for section_id, _ in fetched})
    if stale:
        print(f"  Removed {stale} stale section file(s) no longer in the TOC.")

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
    skipped_for_time = []
    processed_count = 0
    run_started = time.monotonic()
    for entry in data["manuals"]:
        if args.code and entry["code"] != args.code:
            updated_manuals.append(entry)
            continue

        # Only start a manual if the run budget can still absorb a full manual
        # budget, so the run cannot overshoot and get killed with nothing
        # committed. The rest keep their existing data and are picked up next
        # run.
        elapsed = time.monotonic() - run_started
        if elapsed + MANUAL_TIME_BUDGET_SECS > RUN_TIME_BUDGET_SECS:
            print(f"\nTime budget reached after {elapsed/60:.0f} min — leaving "
                  f"{entry['code']} for the next run so this one can publish.",
                  file=sys.stderr)
            skipped_for_time.append(entry["code"])
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

    if skipped_for_time:
        print(f"\nNOTE: ran out of time budget; not attempted this run: "
              f"{', '.join(skipped_for_time)}. They keep their existing data and "
              f"will be picked up next run.", file=sys.stderr)

    if errors:
        print(f"\nWARNING: {len(errors)} manual(s) failed to update: {', '.join(errors)}", file=sys.stderr)
        print("Done (with errors).")
        sys.exit(1)

    print(f"Done in {(time.monotonic() - run_started)/60:.0f} min.")


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
