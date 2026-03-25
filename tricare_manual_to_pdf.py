#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
TRICARE Manual -> Single PDF (Playwright-only, no httpx/certs)

What it does
------------
1) Open the manual TOC: ManualToc.aspx?Manual=...&Change=...
2) Click "Expand All" (if present)
3) Collect all chapter TOCs (C1TOC.html...) + any top-level content (FOREWORD.html, INTRO.html)
4) For each chapter TOC, collect every real section page (e.g., C1S1.html, C1S1_1.html, C11ADA.html, etc.)
5) Visit each section page with Playwright, grab the page HTML, clean it, bundle to one HTML, then print to a PDF.

Prereqs (in venv):
  pip install playwright beautifulsoup4 lxml
  python -m playwright install chromium

Usage:
  source venv/bin/activate
  python tricare_full_manual_playwright_only.py \
    "https://manuals.health.mil/pages/ManualToc.aspx?Manual=TPT5&Change=41" \
    "TRICARE_Policy_Manual_Change41.pdf"
"""

import asyncio
import html
import re
import sys
from pathlib import Path
from urllib.parse import urljoin, urlparse

from bs4 import BeautifulSoup, Comment
from playwright.async_api import async_playwright, TimeoutError as PWTimeout

PDF_FORMAT = "A4"  # or "Letter"
PDF_MARGIN = {"top": "12mm", "right": "12mm", "bottom": "12mm", "left": "12mm"}
DELAY_MS_AFTER_LOAD = 150
TMP_DIR = Path("manual_tmp")
BUNDLE_HTML_PATH = TMP_DIR / "bundle.html"


def strip_hash(u: str) -> str:
    return u[:-1] if u.endswith("#") else u


def is_display_html(u: str) -> bool:
    # Only actual policy HTML pages
    return bool(u and re.search(r"/pages/DisplayManualHtmlFile/.+\.html$", u, re.I))


def fname(u: str) -> str:
    return Path(urlparse(u).path).name


def is_chapter_toc_name(name: str) -> bool:
    # C1TOC.html, C2TOC.html, etc.
    return bool(re.match(r"^C\d+TOC\.HTML$", name.upper()))


def natural_key(name: str):
    """
    Sort in logical manual order:
      C{chap}S{sec}_{part}.html
      C{chap}AD{LETTER}.html (after normal sections)
    Fallback: chapter then name.
    """
    base = name.split(".", 1)[0]

    m = re.match(r"^C(?P<c>\d+)S(?P<s>\d+)(?:_(?P<p>\d+))?$", base, re.I)
    if m:
        c = int(m.group("c"))
        s = int(m.group("s"))
        p = int(m.group("p") or 0)
        return (0, c, s, p, "")

    m = re.match(r"^C(?P<c>\d+)AD(?P<add>[A-Z]+)$", base, re.I)
    if m:
        c = int(m.group("c"))
        add = (m.group("add") or "").upper()
        return (1, c, 9999, 9999, add)

    m = re.match(r"^C(?P<c>\d+)", base, re.I)
    c = int(m.group("c")) if m else 999999
    return (9, c, 999999, 999999, base.upper())


async def expand_toc_if_possible(page) -> None:
    for frame in page.frames:
        for sel in ["a:has-text('Expand All')",
                    "button:has-text('Expand All')",
                    "a[title*='Expand']",
                    "a:has-text('Expand')"]:
            try:
                el = await frame.query_selector(sel)
                if el:
                    await el.click()
                    await page.wait_for_load_state("networkidle")
            except Exception:
                pass


async def collect_from_toc(toc_url: str):
    """
    Stage 1:
      - Open main TOC
      - Expand
      - Collect all DisplayManualHtmlFile links
      - Split into chapter TOCs vs top-level content (e.g., FOREWORD.html)
    """
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        ctx = await browser.new_context()
        page = await ctx.new_page()
        await page.goto(toc_url, wait_until="networkidle")
        try:
            await page.wait_for_selector("body", timeout=10000)
        except PWTimeout:
            pass

        await expand_toc_if_possible(page)

        host = urlparse(toc_url).netloc
        seen = set()
        chapter_tocs = []
        top_level = []

        for frame in page.frames:
            try:
                anchors = await frame.eval_on_selector_all(
                    "a",
                    "els => els.map(a => ({href: a.href || a.getAttribute('href') || ''}))",
                )
            except Exception:
                continue

            for a in anchors or []:
                href = a.get("href") or ""
                if not href:
                    continue
                full = urljoin(frame.url, href)
                if urlparse(full).netloc != host:
                    continue
                if not is_display_html(full):
                    continue
                if full in seen:
                    continue
                seen.add(full)

                name = fname(full)
                if is_chapter_toc_name(name):
                    chapter_tocs.append(full)
                else:
                    top_level.append(full)

        await browser.close()

    return chapter_tocs, top_level


async def collect_section_links_from_chapter(chapter_url: str) -> list[str]:
    """
    Load a chapter TOC page and collect all inner section links (Playwright-only).
    """
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        ctx = await browser.new_context()
        page = await ctx.new_page()
        await page.goto(chapter_url, wait_until="networkidle")
        try:
            await page.wait_for_selector("body", timeout=10000)
        except PWTimeout:
            pass

        host = urlparse(chapter_url).netloc
        seen, out = set(), []

        # Some chapter TOCs have frames; scrape all frames
        for frame in page.frames:
            try:
                anchors = await frame.eval_on_selector_all(
                    "a", "els => els.map(a => a.href || a.getAttribute('href') || '')"
                )
            except Exception:
                continue

            for href in anchors or []:
                if not href:
                    continue
                full = urljoin(frame.url, href)
                if urlparse(full).netloc != host:
                    continue
                if not is_display_html(full):
                    continue
                n = fname(full)
                if is_chapter_toc_name(n):
                    continue  # skip chapter TOCs; we want actual sections
                if full not in seen:
                    seen.add(full)
                    out.append(full)

        await browser.close()
    return out


def clean_html_keep_main(html_text: str) -> str:
    soup = BeautifulSoup(html_text, "lxml")

    # Drop scripts/styles/links/comments to avoid external fetches
    for tag in soup(["script", "noscript", "style", "link"]):
        tag.decompose()
    for c in soup.find_all(string=lambda t: isinstance(t, Comment)):
        c.extract()

    # Heuristic: choose the largest meaningful block
    candidates = []
    for sel in ["main", "div#content", "div.Content", "div.container", "div#main", "div#page", "body"]:
        for node in soup.select(sel):
            txt_len = len(node.get_text(" ", strip=True))
            candidates.append((txt_len, node))

    if not candidates:
        return str(soup)

    candidates.sort(reverse=True, key=lambda x: x[0])
    _, best = candidates[0]
    return "".join(str(c) for c in best.contents) or str(best)


def build_bundle_html(pages: list[tuple[str, str]]) -> str:
    head = f"""<!doctype html>
<html>
<head>
<meta charset="utf-8">
<title>TRICARE Manual (Bundled)</title>
<style>
@page {{
  size: {PDF_FORMAT};
  margin: {PDF_MARGIN['top']} {PDF_MARGIN['right']} {PDF_MARGIN['bottom']} {PDF_MARGIN['left']};
}}
body {{ font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial, sans-serif; line-height: 1.35; }}
h1,h2,h3 {{ break-after: avoid; }}
.section {{ page-break-after: always; }}
.section:last-child {{ page-break-after: auto; }}
hr.toc {{ border: 0; border-top: 1px solid #ccc; margin: 12px 0; }}
.small {{ color:#666; font-size:12px; }}
table {{ border-collapse: collapse; }}
table, th, td {{ border: 1px solid #999; }}
th, td {{ padding: 4px 6px; vertical-align: top; }}
</style>
</head>
<body>
<h1>TRICARE Manual (Bundled)</h1>
<p class="small">Generated by Playwright-only bundler.</p>
<hr class="toc">
<ol>
"""
    toc, body = [], []
    for i, (label, html_part) in enumerate(pages, start=1):
        anchor = f"sec{i:04d}"
        toc.append(f'<li><a href="#{anchor}">{html.escape(label)}</a></li>')
        body.append(f'<div class="section" id="{anchor}"><h2>{html.escape(label)}</h2>{html_part}</div>')
    tail = f"</ol><hr class='toc'>{''.join(body)}</body></html>"
    return head + "".join(toc) + tail


async def bundle_html_to_pdf(bundle_path: Path, out_pdf: str):
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        ctx = await browser.new_context()
        page = await ctx.new_page()
        url = "file://" + str(bundle_path.resolve())
        await page.goto(url, wait_until="networkidle")
        if DELAY_MS_AFTER_LOAD:
            await page.wait_for_timeout(DELAY_MS_AFTER_LOAD)
        await page.pdf(
            path=out_pdf,
            format=PDF_FORMAT,
            margin=PDF_MARGIN,
            print_background=True
        )
        await browser.close()


async def main(toc_url: str, out_pdf: str):
    toc_url = strip_hash(toc_url)
    TMP_DIR.mkdir(exist_ok=True)

    print("Stage 1: harvesting chapter TOCs & top-level content…")
    chapter_tocs, top_level = await collect_from_toc(toc_url)
    print(f"  Chapter TOCs: {len(chapter_tocs)}")
    print(f"  Top-level content: {len(top_level)}")

    print("Stage 2: collecting ALL section links from each chapter TOC…")
    section_links, seen = [], set()
    # Seed with top-level content first
    for u in top_level:
        if u not in seen:
            seen.add(u)
            section_links.append(u)

    for idx, chap in enumerate(chapter_tocs, start=1):
        print(f"  [{idx}/{len(chapter_tocs)}] {fname(chap)}")
        inner = await collect_section_links_from_chapter(chap)
        for u in inner:
            if u not in seen:
                seen.add(u)
                section_links.append(u)

    if not section_links:
        print("No section links found; exiting.")
        sys.exit(2)

    section_links.sort(key=lambda u: natural_key(fname(u)))
    print(f"Total section pages to fetch: {len(section_links)}")

    # Stage 3: fetch page HTML with Playwright and clean it
    pages = []
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        ctx = await browser.new_context()

        total = len(section_links)
        for i, u in enumerate(section_links, start=1):
            label = fname(u)
            print(f"  [{i}/{total}] {label}")
            pg = await ctx.new_page()
            try:
                await pg.goto(u, wait_until="networkidle")
                try:
                    await pg.wait_for_selector("body", timeout=10000)
                except PWTimeout:
                    pass
                if DELAY_MS_AFTER_LOAD:
                    await pg.wait_for_timeout(DELAY_MS_AFTER_LOAD)
                raw_html = await pg.content()  # entire DOM
                cleaned = clean_html_keep_main(raw_html)
                pages.append((label, cleaned))
            except Exception as e:
                print(f"    - Failed to load {u}: {e}")
            finally:
                await pg.close()

        await browser.close()

    print("Building bundle HTML…")
    bundle_html = build_bundle_html(pages)
    BUNDLE_HTML_PATH.write_text(bundle_html, encoding="utf-8")

    print("Rendering final PDF…")
    await bundle_html_to_pdf(BUNDLE_HTML_PATH, out_pdf)
    print(f"Done. Wrote: {out_pdf}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python tricare_full_manual_playwright_only.py <TOC_URL> <OUTPUT_PDF>")
        sys.exit(1)
    asyncio.run(main(sys.argv[1], sys.argv[2]))

