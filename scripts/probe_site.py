#!/usr/bin/env python3
"""Throwaway diagnostic #5: is /api/publication/... fetchable without a browser?

Probe #4 found that every section anchor carries
    hx-get="/api/publication/{CODE}/{REVISION}/{FILENAME}"
    hx-target="#publication-document" hx-swap="innerHTML"
so htmx pulls the document fragment from that endpoint. If plain HTTP can
reach it, content fetching drops Playwright entirely and the fragment needs no
extraction — it is already the document.

Tests, in order of how much we would have to keep:
  1. plain GET, no special headers
  2. plain GET with htmx's own HX-Request headers
  3. the same via a browser context, as the control

Also checks whether the fragment carries the manual's own title elements, so
titles can come from the content when link text is unavailable.

Delete once the retarget is done.
"""
import asyncio, sys
import requests
from bs4 import BeautifulSoup
from playwright.async_api import async_playwright

BASE = "https://manuals.dha.mil"
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")
TARGETS = [("FR16", 20, "C1"), ("TPT5", 56, "C1S1_1")]


def describe(label, text):
    soup = BeautifulSoup(text, "html.parser")
    body = soup.get_text(" ", strip=True)
    print(f"    {label}: {len(text)}B raw, {len(body)} chars text")
    print(f"      starts: {body[:160]!r}")
    for sel in ("#publication-document", "article", ".Header", ".MPChapterTitle",
                ".Chapter", ".CFRSubject", ".Section", "nav", "#leftNav"):
        found = soup.select(sel)
        if found:
            print(f"      {sel!r}: {len(found)}  first={found[0].get_text(' ', strip=True)[:60]!r}")


def plain(url, headers=None):
    try:
        r = requests.get(url, headers={"User-Agent": UA, **(headers or {})}, timeout=45)
        return r
    except Exception as e:
        print(f"    ERROR {type(e).__name__}: {e}")
        return None


async def via_browser(url):
    async with async_playwright() as pw:
        b = await pw.chromium.launch(args=["--disable-blink-features=AutomationControlled"])
        ctx = await b.new_context(user_agent=UA)
        try:
            r = await ctx.request.get(url, headers={"HX-Request": "true"})
            return r.status, await r.text()
        finally:
            await ctx.close(); await b.close()


for code, rev, fn in TARGETS:
    url = f"{BASE}/api/publication/{code}/{rev}/{fn}"
    print(f"\n=== {url} ===")

    print("  1. plain GET")
    r = plain(url)
    if r is not None:
        print(f"    HTTP {r.status_code}  {r.headers.get('content-type','')}")
        if r.status_code == 200:
            describe("body", r.text)

    print("  2. plain GET with htmx headers")
    r2 = plain(url, {"HX-Request": "true", "HX-Target": "publication-document",
                     "Referer": f"{BASE}/View-Publication/{code}"})
    if r2 is not None:
        print(f"    HTTP {r2.status_code}  {r2.headers.get('content-type','')}")
        if r2.status_code == 200:
            describe("body", r2.text)

    print("  3. browser context (control)")
    try:
        status, text = asyncio.run(via_browser(url))
        print(f"    HTTP {status}")
        if status == 200:
            describe("body", text)
    except Exception as e:
        print(f"    ERROR {type(e).__name__}: {e}")

print("\nProbe complete.")
