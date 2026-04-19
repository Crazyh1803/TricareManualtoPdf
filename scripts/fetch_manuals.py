#!/usr/bin/env python3
"""
TRICARE Manuals — Content Scraper
==================================
Fetches manual content from manuals.health.mil and writes static files
to docs/data/ for consumption by the GitHub Pages web app.

NOTE: manuals.health.mil uses DoD-signed certificates that are not in the
standard CA bundle.  All requests use verify=False (SSL verification
disabled) to work around this — identical to the Android app's lenient
OkHttp client.  All traffic is read-only public government documents.

Output layout:
  docs/data/manuals.json           — manual list with latestChange
  docs/data/{CODE}/toc.json        — section index for one manual/change
  docs/data/{CODE}/s/{id}.html     — individual section content (body HTML only)

Run locally:
  pip install requests beautifulsoup4 lxml
  python scripts/fetch_manuals.py

  # Fetch only a specific manual (faster during development):
  python scripts/fetch_manuals.py --code TOT5
"""

import argparse
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

# Binary-search ceiling for change numbers
CHANGE_MAX = 300

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


def toc_has_sections(code: str, change: int) -> bool:
    """
    Return True if the TOC page for this change number contains real section
    links.  manuals.health.mil returns HTTP 200 for ALL URLs — including
    invalid change numbers — so we must check the page body rather than
    the status code.  A valid TOC page contains links to DisplayContent.aspx;
    an invalid/out-of-range page does not.
    """
    url = TOC_URL.format(code=code, change=change)
    r = get(url)
    if r is None:
        return False
    return bool(_DISPLAY_RE.search(r.text))


# ── Version discovery ───────────────────────────────────────────────────────
def find_latest_change(code: str) -> int | None:
    """
    Binary-search to find the highest change number that has real content.
    Uses content detection (not HTTP status) because the site soft-404s with
    HTTP 200 for every URL.  ~9 page fetches per manual.
    Returns None only if Change=1 itself has no section links.
    """
    if not toc_has_sections(code, 1):
        print(f"  [{code}] Change=1 has no section links — skipping", file=sys.stderr)
        return None

    lo, hi = 1, CHANGE_MAX
    while lo < hi - 1:
        mid = (lo + hi) // 2
        if toc_has_sections(code, mid):
            lo = mid
        else:
            hi = mid
    return lo


# ── TOC parsing ─────────────────────────────────────────────────────────────
_DISPLAY_RE = re.compile(r"DisplayContent", re.IGNORECASE)


def fetch_toc(code: str, change: int) -> list[dict]:
    """
    Fetch and parse the table-of-contents page.
    Returns a sorted list of section dicts.
    """
    url = TOC_URL.format(code=code, change=change)
    r = get(url)
    if r is None:
        return []

    soup = BeautifulSoup(r.text, "lxml")
    links = soup.find_all("a", href=_DISPLAY_RE)
    if not links:
        print(f"  [{code}] No section links found on TOC page", file=sys.stderr)
        return []

    sections = []
    seen_qs: set[str] = set()

    for link in links:
        href = link.get("href", "").strip()
        full_url = urljoin(BASE_URL, href)
        qs = urlparse(full_url).query.lower()
        if qs in seen_qs:
            continue
        seen_qs.add(qs)

        params = parse_qs(urlparse(full_url).query, keep_blank_values=True)
        chapter_raw = params.get("chapter", params.get("Chapter", ["0"]))[0]
        section_raw = params.get("section", params.get("Section", [""]))[0]

        try:
            chapter = int(chapter_raw)
        except ValueError:
            chapter = 0

        title = link.get_text(strip=True) or f"Chapter {chapter}"
        title = clean_title(title)
        is_chapter_toc = (section_raw == "" or section_raw == "0")

        sections.append({
            "url":          full_url,
            "chapter":      chapter,
            "section":      section_raw,
            "title":        title,
            "isChapterToc": is_chapter_toc,
        })

    def sort_key(s):
        ch  = s["chapter"]
        sec = s["section"]
        try:
            sec_f = float(sec) if sec else 0.0
        except ValueError:
            sec_f = 0.0
        return (ch, 0 if s["isChapterToc"] else 1, sec_f)

    sections.sort(key=sort_key)

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
    print(f"\n{'═'*60}")
    print(f"  Manual: {name} ({code})")

    print("  Discovering latest change…")
    latest = find_latest_change(code)
    if latest is None:
        print("  Skipping — server unreachable.")
        return entry

    print(f"  Latest change: {latest}")
    known = entry.get("latestChange", 0)

    if not force and latest == known and entry.get("hasContent"):
        print("  Already up-to-date. Skipping content fetch.")
        return {**entry, "latestChange": latest, "hasContent": True}

    print("  Fetching table of contents…")
    sections = fetch_toc(code, latest)
    if not sections:
        print("  No sections found — skipping.")
        return entry

    print(f"  Found {len(sections)} sections.")
    write_toc(code, latest, sections)

    content_sections = [s for s in sections if not s["isChapterToc"]]
    print(f"  Fetching {len(content_sections)} content sections…")

    for i, s in enumerate(content_sections, 1):
        print(f"  [{i}/{len(content_sections)}] {s['title'][:70]}")
        _, html = fetch_section_html(s["url"])
        write_section(code, s["id"], html)

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
