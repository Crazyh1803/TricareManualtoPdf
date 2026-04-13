#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
TRICARE Manual -> Single PDF (Playwright-only, no httpx/certs)

What it does
------------
1) Open the manual TOC: ManualToc.aspx?Manual=...&Change=...
2) Click "Expand All" (if present) and wait for the tree to load
3) Collect all chapter TOCs (C1TOC.html...) + any top-level content (FOREWORD.html, INTRO.html)
4) For each chapter TOC, collect every real section page (C1S1.html, C1S1_1.html, etc.)
5) Visit each section page, grab HTML, clean it, bundle to one HTML, print to PDF.

Prereqs (in venv):
  pip install playwright beautifulsoup4 lxml
  playwright install chromium

Usage (single manual):
  python tricare_manual_to_pdf.py \\
    "https://manuals.health.mil/pages/ManualToc.aspx?Manual=TPT5&Change=48" \\
    "TRICARE_Policy_Manual.pdf"

Usage (batch — process all 4 manuals):
  python tricare_manual_to_pdf.py --batch manuals.txt

  Where manuals.txt contains one  url<TAB>output.pdf  line per manual.

Diagnose why nothing is found:
  python tricare_manual_to_pdf.py --diagnose \\
    "https://manuals.health.mil/pages/ManualToc.aspx?Manual=TPT5&Change=48" \\
    dummy.pdf
"""

import argparse
import asyncio
import html
import random
import re
import sys
from pathlib import Path
from urllib.parse import urljoin, urlparse

from bs4 import BeautifulSoup, Comment
from playwright.async_api import async_playwright, TimeoutError as PWTimeout

# ── Defaults ─────────────────────────────────────────────────────────────────
PDF_FORMAT  = "A4"
PDF_MARGIN  = {"top": "12mm", "right": "12mm", "bottom": "12mm", "left": "12mm"}
DELAY_MS          = 500    # wait after networkidle before grabbing content
TOC_WAIT_MS       = 3_000  # extra wait on TOC pages for JS tree to render
MAX_RETRIES       = 3
FETCH_DELAY_RANGE = (3.0, 7.0)   # random pause (seconds) between page fetches
MANUAL_COOLDOWN   = 60           # seconds to wait between manuals
TMP_DIR     = Path("manual_tmp")
BUNDLE_PATH = TMP_DIR / "bundle.html"

_JUNK_TAGS         = {"script", "noscript", "style", "link", "meta",
                      "iframe", "nav", "header", "footer"}
_CONTENT_SELECTORS = ["main", "#content", ".Content", ".container",
                      "#main", "#page", "article", "body"]
_FRONT_MATTER      = {"FOREWORD", "INTRO", "PREFACE", "SUMMARY"}


# ── URL helpers ───────────────────────────────────────────────────────────────

def strip_hash(u: str) -> str:
    return u.rstrip("#")


def is_display_html(u: str) -> bool:
    """Match any .html file served under the DisplayManualHtmlFile path."""
    return bool(u and re.search(r"/pages/DisplayManualHtmlFile/.+\.html", u, re.I))


def fname(u: str) -> str:
    return Path(urlparse(u).path).name


def is_chapter_toc_name(name: str) -> bool:
    return bool(re.match(r"^C\d+TOC\.HTML$", name.upper()))


def file_url(path: Path) -> str:
    """Return a well-formed file:// URL (handles Windows C:\\ paths correctly)."""
    return path.resolve().as_uri()


# ── Sorting ───────────────────────────────────────────────────────────────────

def natural_key(name: str):
    """Sort filenames in logical manual order: front matter → chapters → addenda."""
    base = name.split(".", 1)[0].upper()
    if base in _FRONT_MATTER:
        return (-1, 0, 0, 0, base)
    m = re.match(r"^C(\d+)S(\d+)(?:_(\d+))?$", base)
    if m:
        return (0, int(m.group(1)), int(m.group(2)), int(m.group(3) or 0), "")
    m = re.match(r"^C(\d+)AD(.+)$", base)
    if m:
        return (1, int(m.group(1)), 9999, 9999, m.group(2).upper())
    m = re.match(r"^C(\d+)", base)
    c = int(m.group(1)) if m else 999_999
    return (9, c, 999_999, 999_999, base)


# ── TOC expansion ─────────────────────────────────────────────────────────────

async def expand_toc_if_possible(page) -> None:
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
                    break
            except Exception:
                pass


# ── Link collection ───────────────────────────────────────────────────────────

async def _all_hrefs_in_page(page) -> list[str]:
    """Collect every href from every frame (for diagnostics)."""
    out = []
    for frame in page.frames:
        try:
            hrefs = await frame.eval_on_selector_all(
                "a",
                "els => els.map(a => a.href || a.getAttribute('href') || '')",
            )
            out.extend(h for h in (hrefs or []) if h)
        except Exception:
            pass
    return out


async def _collect_display_links(page, base_url: str) -> list[str]:
    """Return all unique DisplayManualHtmlFile links visible across all frames."""
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


async def diagnose_toc(toc_url: str, browser) -> None:
    """Dump page structure to help debug link-collection failures."""
    dump_path = TMP_DIR / "diagnose_page.html"
    shot_path = TMP_DIR / "diagnose_screenshot.png"
    print("\n=== DIAGNOSE MODE ===")
    ctx  = await make_context(browser)
    page = await ctx.new_page()
    try:
        print(f"Loading: {toc_url}")
        await page.goto(toc_url, wait_until="networkidle")
        # Try progressively longer waits to catch slow JS rendering
        for wait_s in (2, 3, 5):
            await page.wait_for_timeout(wait_s * 1000)
            all_hrefs = await _all_hrefs_in_page(page)
            display_hrefs = [h for h in all_hrefs if is_display_html(h)]
            print(f"  After {wait_s}s: {len(all_hrefs)} total hrefs, "
                  f"{len(display_hrefs)} DisplayManualHtmlFile hrefs")
            if display_hrefs:
                break

        await expand_toc_if_possible(page)
        await page.wait_for_timeout(3_000)
        all_hrefs   = await _all_hrefs_in_page(page)
        display_hrefs = [h for h in all_hrefs if is_display_html(h)]
        print(f"  After expand attempt: {len(all_hrefs)} total hrefs, "
              f"{len(display_hrefs)} DisplayManualHtmlFile hrefs")

        print(f"\nFrames on page: {len(page.frames)}")
        for i, frame in enumerate(page.frames):
            print(f"  Frame {i}: {frame.url}")

        if display_hrefs:
            print("\nSample DisplayManualHtmlFile URLs (first 20):")
            for h in display_hrefs[:20]:
                print(f"  {h}")
        else:
            print("\nNO DisplayManualHtmlFile links found.")
            print("Sample of ALL hrefs (first 30):")
            for h in list(dict.fromkeys(all_hrefs))[:30]:
                print(f"  {h}")

        # Save full rendered HTML and a screenshot for manual inspection
        html_content = await page.content()
        dump_path.write_text(html_content, encoding="utf-8")
        await page.screenshot(path=str(shot_path), full_page=True)
        print(f"\nRendered HTML saved : {dump_path}")
        print(f"Screenshot saved    : {shot_path}")
        print("Open these files to inspect the page visually and structurally.")
    finally:
        await ctx.close()
    print("=== END DIAGNOSE ===\n")


async def collect_from_toc(toc_url: str, browser):
    """Stage 1: open main TOC, expand it, split links into chapter TOCs vs top-level."""
    ctx  = await make_context(browser)
    page = await ctx.new_page()
    try:
        try:
            await page.goto(toc_url, wait_until="networkidle", timeout=30_000)
        except Exception as e:
            print(f"  ! goto failed ({e}), continuing anyway…")

        # Log actual URL to catch silent redirects
        actual_url = page.url
        if actual_url != toc_url:
            print(f"  ! Redirected to: {actual_url}")

        # Progressive wait — same strategy that works in --diagnose mode
        links = []
        for wait_s in (2, 3, 5, 8):
            await page.wait_for_timeout(wait_s * 1_000)
            links = await _collect_display_links(page, toc_url)
            if links:
                print(f"  (found {len(links)} link(s) after {wait_s}s)")
                break

        if not links:
            await expand_toc_if_possible(page)
            await page.wait_for_timeout(5_000)
            links = await _collect_display_links(page, toc_url)

        if not links:
            # Last resort: dump page title to help diagnose
            title = await page.title()
            print(f"  ! Still 0 links. Page title: {title!r}")
    finally:
        await ctx.close()

    chapter_tocs, top_level = [], []
    for u in links:
        (chapter_tocs if is_chapter_toc_name(fname(u)) else top_level).append(u)
    return chapter_tocs, top_level


async def collect_section_links_from_chapter(
    chapter_url: str,
    browser,
    sem: asyncio.Semaphore,
) -> list[str]:
    """Load one chapter TOC and collect all inner section links."""
    async with sem:
        ctx  = await make_context(browser)
        page = await ctx.new_page()
        try:
            await page.goto(chapter_url, wait_until="networkidle")
            try:
                await page.wait_for_selector("body", timeout=10_000)
            except PWTimeout:
                pass
            await page.wait_for_timeout(DELAY_MS)
            links = await _collect_display_links(page, chapter_url)
        finally:
            await ctx.close()

    return [u for u in links if not is_chapter_toc_name(fname(u))]


# ── HTML cleaning & conversion ────────────────────────────────────────────────

def _extract_main_node(soup: BeautifulSoup):
    """Return the largest meaningful content node from a parsed page."""
    best, best_len = None, 0
    for sel in _CONTENT_SELECTORS:
        for node in soup.select(sel):
            length = len(node.get_text(" ", strip=True))
            if length > best_len:
                best_len, best = length, node
    return best


def _extract_title(soup: BeautifulSoup, fallback: str) -> str:
    """Pull a human-readable title from the page (h1 → title tag → fallback)."""
    h1 = soup.find("h1")
    if h1:
        t = h1.get_text(" ", strip=True)
        if t:
            return t
    title_tag = soup.find("title")
    if title_tag:
        t = title_tag.get_text(" ", strip=True)
        # Strip boilerplate suffix like " | TRICARE Manuals"
        t = re.sub(r"\s*[|–—-].*$", "", t).strip()
        if t:
            return t
    return fallback


# Heading text that belongs to site navigation, not section content
_NAV_HEADINGS = {"military health system", "tricare", "dha", "defense health agency"}


def clean_html_keep_main(html_text: str) -> tuple[str, str]:
    """Return (page_title, cleaned_inner_html)."""
    soup = BeautifulSoup(html_text, "lxml")

    for tag in soup(_JUNK_TAGS):
        tag.decompose()
    for c in soup.find_all(string=lambda t: isinstance(t, Comment)):
        c.extract()

    best = _extract_main_node(soup)
    if best is None:
        return _extract_title(soup, ""), str(soup)

    # Extract title from within the content block (skip generic nav headings)
    title = ""
    for h in best.find_all(["h1", "h2", "h3"], limit=5):
        t = h.get_text(" ", strip=True)
        if t and t.lower() not in _NAV_HEADINGS:
            title = t
            break
    if not title:
        title = _extract_title(soup, "")

    for tag in best.find_all(True):
        for attr in list(tag.attrs):
            if attr.startswith("on") or attr in ("style", "background"):
                del tag.attrs[attr]

    return title, ("".join(str(c) for c in best.contents) or str(best))


def html_to_markdown(html_str: str) -> str:
    """Convert a cleaned HTML fragment to plain Markdown text."""
    soup = BeautifulSoup(html_str, "lxml")

    lines = []

    def walk(node):
        if isinstance(node, str):
            text = node.strip()
            if text:
                lines.append(text)
            return
        tag = node.name
        if tag is None:
            return
        if tag in ("h1", "h2"):
            lines.append("\n### " + node.get_text(" ", strip=True))
        elif tag in ("h3", "h4", "h5", "h6"):
            lines.append("\n#### " + node.get_text(" ", strip=True))
        elif tag == "p":
            t = node.get_text(" ", strip=True)
            if t:
                lines.append("\n" + t)
        elif tag in ("ul", "ol"):
            for li in node.find_all("li", recursive=False):
                lines.append("- " + li.get_text(" ", strip=True))
        elif tag == "table":
            rows = node.find_all("tr")
            for r_idx, row in enumerate(rows):
                cells = [c.get_text(" ", strip=True) for c in row.find_all(["th", "td"])]
                lines.append("| " + " | ".join(cells) + " |")
                if r_idx == 0:
                    lines.append("|" + "|".join("---" for _ in cells) + "|")
        elif tag in ("br",):
            lines.append("")
        elif tag in ("div", "section", "article", "body", "main",
                     "span", "td", "th", "li", "blockquote"):
            for child in node.children:
                walk(child)
        else:
            for child in node.children:
                walk(child)

    for child in soup.children:
        walk(child)

    # Collapse excessive blank lines
    result = re.sub(r"\n{3,}", "\n\n", "\n".join(lines))
    return result.strip()


# ── Page fetcher with retries ─────────────────────────────────────────────────

async def fetch_page(
    url: str,
    browser,
    sem: asyncio.Semaphore,
    retries: int = MAX_RETRIES,
) -> tuple[str, str] | None:
    """Return (page_title, cleaned_html) or None on failure."""
    async with sem:
        for attempt in range(1, retries + 1):
            ctx = await make_context(browser)
            pg  = await ctx.new_page()
            try:
                await pg.goto(url, wait_until="networkidle", timeout=30_000)
                try:
                    await pg.wait_for_selector("body", timeout=10_000)
                except PWTimeout:
                    pass
                if DELAY_MS:
                    await pg.wait_for_timeout(DELAY_MS)
                raw = await pg.content()
                result = clean_html_keep_main(raw)
                # Random courtesy pause to avoid WAF rate-limiting
                await asyncio.sleep(random.uniform(*FETCH_DELAY_RANGE))
                return result
            except Exception as exc:
                if attempt == retries:
                    print(f"    ! Failed after {retries} attempts: {fname(url)} — {exc}")
                    return None
                await asyncio.sleep(attempt * 2)
            finally:
                await ctx.close()
    return None


# ── Output builders ──────────────────────────────────────────────────────────

def build_markdown_doc(pages: list[tuple[str, str]], title: str = "TRICARE Manual") -> str:
    """Build a single Markdown document from (section_title, html_content) pairs."""
    parts = [f"# {title}\n", f"*{len(pages)} sections*\n", "---\n"]
    parts.append("## Table of Contents\n")
    for i, (label, _) in enumerate(pages, 1):
        parts.append(f"{i}. {label}")
    parts.append("\n---\n")
    for label, html_content in pages:
        parts.append(f"\n## {label}\n")
        parts.append(html_to_markdown(html_content))
        parts.append("\n\n---\n")
    return "\n".join(parts)


# ── HTML bundle builder ───────────────────────────────────────────────────────

def build_bundle_html(pages: list[tuple[str, str]], title: str = "TRICARE Manual") -> str:
    css = f"""
@page {{
  size: {PDF_FORMAT};
  margin: {PDF_MARGIN['top']} {PDF_MARGIN['right']} {PDF_MARGIN['bottom']} {PDF_MARGIN['left']};
}}
body {{
  font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif;
  font-size: 10pt; line-height: 1.4; color: #111;
}}
h1, h2, h3, h4 {{ break-after: avoid; color: #003366; }}
.section {{ page-break-after: always; padding-bottom: 8px; }}
.section:last-child {{ page-break-after: auto; }}
.section-title {{
  font-size: 13pt; font-weight: bold; margin-bottom: 6px;
  border-bottom: 2px solid #003366; padding-bottom: 4px;
}}
hr.sep {{ border: 0; border-top: 1px solid #ccc; margin: 10px 0; }}
.toc-list {{ columns: 2; column-gap: 2em; }}
@media print {{ .toc-list {{ columns: 1; }} }}
table {{ border-collapse: collapse; width: 100%; margin: 6px 0; }}
table, th, td {{ border: 1px solid #999; }}
th {{ background: #e8eef5; }}
th, td {{ padding: 4px 8px; vertical-align: top; font-size: 9pt; }}
a {{ color: #003399; }}
.small {{ color: #666; font-size: 9pt; }}
"""
    toc_items, body_parts = [], []
    for i, (label, html_part) in enumerate(pages, start=1):
        anchor = f"sec{i:04d}"
        toc_items.append(f'<li><a href="#{anchor}">{html.escape(label)}</a></li>')
        body_parts.append(
            f'<div class="section" id="{anchor}">'
            f'<p class="section-title">{html.escape(label)}</p>'
            f'{html_part}'
            f'</div>'
        )

    return (
        f'<!doctype html>\n<html lang="en">\n<head>\n'
        f'<meta charset="utf-8">\n'
        f'<title>{html.escape(title)}</title>\n'
        f'<style>{css}</style>\n</head>\n<body>\n'
        f'<h1>{html.escape(title)}</h1>\n'
        f'<p class="small">Generated automatically — {len(pages)} sections.</p>\n'
        f'<hr class="sep">\n'
        f'<h2>Table of Contents</h2>\n'
        f'<ul class="toc-list">{"".join(toc_items)}</ul>\n'
        f'<hr class="sep">\n'
        f'{"".join(body_parts)}\n'
        f'</body>\n</html>'
    )


# ── PDF renderer ──────────────────────────────────────────────────────────────

async def bundle_html_to_pdf(bundle_path: Path, out_pdf: str, browser) -> None:
    ctx  = await make_context(browser)
    page = await ctx.new_page()
    try:
        await page.goto(file_url(bundle_path), wait_until="networkidle")
        if DELAY_MS:
            await page.wait_for_timeout(DELAY_MS)
        await page.pdf(
            path=out_pdf,
            format=PDF_FORMAT,
            margin=PDF_MARGIN,
            print_background=True,
        )
    finally:
        await ctx.close()


# ── Core per-manual logic ─────────────────────────────────────────────────────

async def process_manual(
    toc_url: str,
    out_file: str,
    browser,
    sem: asyncio.Semaphore,
    fmt: str = "md",
    diagnose: bool = False,
) -> None:
    toc_url = strip_hash(toc_url)
    manual_id = re.search(r"Manual=([^&]+)", toc_url)
    title = f"TRICARE Manual {manual_id.group(1)}" if manual_id else "TRICARE Manual"

    print(f"\n{'='*60}")
    print(f"Manual : {title}")
    print(f"Output : {out_file}  [{fmt.upper()}]")
    print(f"{'='*60}")

    if diagnose:
        await diagnose_toc(toc_url, browser)
        return

    # Stage 1
    print("Stage 1: harvesting chapter TOCs & top-level content…")
    chapter_tocs, top_level = await collect_from_toc(toc_url, browser)
    print(f"  Chapter TOCs : {len(chapter_tocs)}")
    print(f"  Top-level    : {len(top_level)}")

    if not chapter_tocs and not top_level:
        print("  ! Nothing found on TOC page.")
        print("  ! Re-run with --diagnose to inspect what the page contains.")
        return

    # Stage 2
    print("Stage 2: collecting section links from each chapter TOC…")
    seen:          set[str]  = set()
    section_links: list[str] = []

    for u in top_level:
        if u not in seen:
            seen.add(u)
            section_links.append(u)

    chap_names = [fname(c) for c in chapter_tocs]
    results = await asyncio.gather(
        *[collect_section_links_from_chapter(c, browser, sem) for c in chapter_tocs],
        return_exceptions=True,
    )
    for chap_name, result in zip(chap_names, results):
        if isinstance(result, Exception):
            print(f"  ! Error scraping {chap_name}: {result}")
            continue
        added = 0
        for u in result:
            if u not in seen:
                seen.add(u)
                section_links.append(u)
                added += 1
        print(f"  {chap_name}: {added} section(s)")

    if not section_links:
        print("  No section links found; skipping this manual.")
        return

    section_links.sort(key=lambda u: natural_key(fname(u)))
    total = len(section_links)
    print(f"Total pages to fetch: {total}")

    # Stage 3
    print("Stage 3: fetching pages…")
    counter = 0

    async def fetch_indexed(u: str) -> tuple[str, str | None]:
        nonlocal counter
        result = await fetch_page(u, browser, sem)
        counter += 1
        # Use real page title if available, fall back to filename
        if result is not None:
            page_title, html_content = result
            label = page_title or fname(u)
            status = ""
        else:
            label, html_content, status = fname(u), None, " [FAILED]"
        print(f"  [{counter}/{total}] {label}{status}")
        return label, html_content

    raw   = await asyncio.gather(*[fetch_indexed(u) for u in section_links])
    pages = [(label, content) for label, content in raw if content is not None]

    failed = total - len(pages)
    if failed:
        print(f"  Warning: {failed} page(s) failed and will be omitted.")

    # Bundle + write output
    if fmt == "md":
        print("Building Markdown document…")
        md = build_markdown_doc(pages, title=title)
        Path(out_file).write_text(md, encoding="utf-8")
        size_kb = Path(out_file).stat().st_size // 1024
        print(f"Done → {out_file}  ({size_kb:,} KB, {len(pages)} sections)")
    else:
        print("Building bundle HTML…")
        bundle_html = build_bundle_html(pages, title=title)
        BUNDLE_PATH.write_text(bundle_html, encoding="utf-8")
        print("Rendering PDF…")
        await bundle_html_to_pdf(BUNDLE_PATH, out_file, browser)
        print(f"Done → {out_file}")


# ── Main ──────────────────────────────────────────────────────────────────────

async def make_browser(p):
    """Launch Chromium configured to avoid bot-detection."""
    browser = await p.chromium.launch(
        headless=True,
        args=["--disable-blink-features=AutomationControlled"],
    )
    return browser


async def make_context(browser):
    """Create a browser context that looks like a normal Chrome user."""
    ctx = await browser.new_context(
        user_agent=(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/124.0.0.0 Safari/537.36"
        ),
        extra_http_headers={"Accept-Language": "en-US,en;q=0.9"},
        viewport={"width": 1280, "height": 900},
    )
    # Remove the navigator.webdriver flag that sites use to detect bots
    await ctx.add_init_script(
        "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
    )
    return ctx


async def main(jobs: list[tuple[str, str]], concurrency: int, fmt: str, diagnose: bool) -> None:
    TMP_DIR.mkdir(exist_ok=True)
    sem = asyncio.Semaphore(concurrency)

    async with async_playwright() as p:
        browser = await make_browser(p)
        for i, (toc_url, out_file) in enumerate(jobs):
            if i > 0:
                print(f"\nCooling down {MANUAL_COOLDOWN}s before next manual…")
                await asyncio.sleep(MANUAL_COOLDOWN)
            await process_manual(toc_url, out_file, browser, sem, fmt=fmt, diagnose=diagnose)
        await browser.close()

    print("\nAll done.")


# ── Entry point ───────────────────────────────────────────────────────────────

def parse_args() -> argparse.Namespace:
    ap = argparse.ArgumentParser(
        description="Download TRICARE manual(s) and export each as Markdown or PDF.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    # Single-manual mode
    ap.add_argument("toc_url", nargs="?", help="ManualToc.aspx URL (single-manual mode)")
    ap.add_argument("out_file", nargs="?", help="Output file path (single-manual mode)")
    # Batch mode
    ap.add_argument(
        "--batch", metavar="FILE",
        help="Text file with one 'url<TAB>output_file' line per manual",
    )
    ap.add_argument(
        "--format", "-f", dest="fmt", default="md", choices=["md", "pdf"],
        help="Output format: md (Markdown, best for Claude) or pdf",
    )
    ap.add_argument(
        "--concurrency", "-j",
        type=int, default=1, metavar="N",
        help="Max parallel page fetches (keep at 1 to avoid rate-limiting)",
    )
    ap.add_argument(
        "--diagnose", action="store_true",
        help="Dump all hrefs found on the TOC page (debug)",
    )
    return ap.parse_args()


def build_jobs(args: argparse.Namespace) -> list[tuple[str, str]]:
    if args.batch:
        jobs = []
        for line in Path(args.batch).read_text().splitlines():
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split("\t", 1)
            if len(parts) != 2:
                print(f"Skipping malformed batch line: {line!r}")
                continue
            jobs.append((parts[0].strip(), parts[1].strip()))
        return jobs

    if args.toc_url and args.out_file:
        return [(args.toc_url, args.out_file)]

    print("Error: provide either  toc_url out_file  or  --batch FILE")
    sys.exit(1)


if __name__ == "__main__":
    args = parse_args()
    jobs = build_jobs(args)
    asyncio.run(main(jobs, args.concurrency, args.fmt, args.diagnose))
