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


NO_CHANGE_URL   = BASE_URL + "/pages/ManualToc.aspx?Manual={code}"
CHANGE_PARAM_RE = re.compile(r"[?&]Change=(\d+)", re.IGNORECASE)


# ── Version discovery (faithful port of Android VersionChecker) ─────────────
#
# The TRICARE site has three behaviours we have to navigate:
#   - It returns HTTP 200 for every URL (soft 404).
#   - It encodes ampersands as "&amp;" in attribute values — so regex on raw
#     HTML fails; we must parse with BeautifulSoup so .get("href") decodes them.
#   - Section links on a TOC page for an *invalid* Change= echo back whichever
#     change the server fell back to serving, NOT what we requested.  So
#     comparing "requested vs rendered" Change= values reveals validity.

def _page_serves_change(html: str, n: int) -> bool:
    """
    True iff the page genuinely corresponds to change N (not a soft-404
    fallback to a different change).
    Strategy 1 — look for decoded hrefs containing ?Change=N or &Change=N
    (anchored so Change=54 doesn't match Change=540).
    Strategy 2 — fall back to scanning visible text for "Change N".
    """
    try:
        soup = BeautifulSoup(html, "lxml")
    except Exception:
        return False

    link_re = re.compile(rf"[?&]Change={n}(?:[^0-9]|$)", re.IGNORECASE)
    for a in soup.find_all("a", href=True):
        # BeautifulSoup decodes &amp; → & when reading the attribute value.
        if link_re.search(a["href"]):
            return True

    text = ((soup.title.string or "") if soup.title else "") + " " + soup.get_text()
    text = text.replace("\u00A0", " ")
    return bool(re.search(rf"change\W{{0,4}}{n}(?:\D|$)", text, re.IGNORECASE))


def _max_change_in_page(html: str) -> int | None:
    """
    Scan all decoded <a href> attributes and <select name=Change> <option>
    values for the maximum Change=N.
    """
    try:
        soup = BeautifulSoup(html, "lxml")
    except Exception:
        return None

    candidates: list[int] = []
    for a in soup.find_all("a", href=True):
        for m in CHANGE_PARAM_RE.finditer(a["href"]):
            try:
                candidates.append(int(m.group(1)))
            except ValueError:
                pass

    for opt in soup.select('[name="Change"] option, [name="change"] option'):
        try:
            candidates.append(int(opt.get("value", "")))
        except ValueError:
            pass

    return max(candidates) if candidates else None


def find_latest_change(code: str) -> int | None:
    """
    Three-stage discovery matching the Android app:

      1. Fetch ManualToc.aspx?Manual=CODE with NO Change param.
         If the server redirects to ?Change=N, return N immediately.
         Otherwise scan the returned HTML for the highest Change=N in any
         decoded href or <option value="N">.

      2. If Step 1 yielded nothing usable, binary-search [1..200] by requesting
         ?Change=mid and asking _page_serves_change() whether the response
         genuinely reflects that change.  The search hinges on the fact that
         invalid requests fall back to a valid change whose links give that
         change away.

      3. Returns None only if Change=1 itself is unreachable.
    """
    # ── Step 1: no-Change fetch ──────────────────────────────────────────────
    url = NO_CHANGE_URL.format(code=code)
    time.sleep(REQUEST_DELAY)
    try:
        r = session.get(url, timeout=30, allow_redirects=True)
    except requests.RequestException as e:
        print(f"  [{code}] Step 1 failed: {type(e).__name__}", file=sys.stderr)
        r = None

    if r is not None and r.status_code == 200:
        # (a) Did the server redirect to ?Change=N?
        m = CHANGE_PARAM_RE.search(r.url)
        if m:
            found = int(m.group(1))
            if found > 0:
                print(f"  [{code}] Server redirected to Change={found}")
                return found
        # (b) Scan decoded hrefs / options for max Change=N
        m_max = _max_change_in_page(r.text)
        if m_max and m_max > 1:
            print(f"  [{code}] Page-scan max change: {m_max}")
            return m_max

    # ── Step 2: binary search with rendered-change verification ──────────────
    # Verify Change=1 is reachable first — if not, the server is down.
    probe = get(TOC_URL.format(code=code, change=1))
    if probe is None:
        print(f"  [{code}] Change=1 unreachable", file=sys.stderr)
        return None

    lo, hi = 1, 200
    while lo < hi - 1:
        mid = (lo + hi) // 2
        r = get(TOC_URL.format(code=code, change=mid))
        if r is None:
            return lo  # network hiccup — best known
        if _page_serves_change(r.text, mid):
            lo = mid
        else:
            hi = mid
    print(f"  [{code}] Binary search result: {lo}")
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

    known = entry.get("latestChange", 0)
    if known <= 0:
        print("  No change number in manuals.json — skipping.", file=sys.stderr)
        return entry

    # Check if newer changes have been published since last run
    latest = known
    for candidate in range(known + 1, known + 20):
        if change_exists(code, candidate):
            print(f"  Newer change found: {candidate}")
            latest = candidate
        else:
            break  # changes are sequential; first miss = done

    print(f"  Using change: {latest}")

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
