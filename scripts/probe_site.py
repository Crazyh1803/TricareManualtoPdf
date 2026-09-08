#!/usr/bin/env python3
"""Throwaway diagnostic: map the new manuals.dha.mil site structure.

The 2026-09-08 TPT5 run landed on 'Home | TRICARE Manuals' at manuals.dha.mil
with zero DisplayManualHtmlFile links, which means manuals.health.mil (the
ASP.NET site the scraper targets) has been replaced. This dumps enough of the
new site — redirects, rendered links, and any JSON/XHR the page calls — to
retarget the scraper. Delete once that work is done.
"""
import asyncio, json, sys
from urllib.parse import urlparse

import requests
from playwright.async_api import async_playwright

OLD = "https://manuals.health.mil/pages/ManualToc.aspx?Manual=TPT5&Change=55"
NEW = "https://manuals.dha.mil/View-Publication/TPT5"
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")


def probe_redirects():
    print("\n=== 1. Where does the OLD url go? ===")
    for url in (OLD, "https://manuals.health.mil/"):
        try:
            r = requests.get(url, headers={"User-Agent": UA}, timeout=30, allow_redirects=True)
            print(f"  {url}")
            print(f"    -> {r.status_code} {r.url}  ({len(r.content)} bytes)")
            for h in r.history:
                print(f"       hop: {h.status_code} {h.url}")
        except Exception as e:
            print(f"  {url} -> ERROR {type(e).__name__}: {e}")


async def probe_new():
    print("\n=== 2. Rendered NEW publication page ===")
    xhr = []
    async with async_playwright() as pw:
        browser = await pw.chromium.launch(args=["--disable-blink-features=AutomationControlled"])
        ctx = await browser.new_context(user_agent=UA, viewport={"width": 1440, "height": 900})
        page = await ctx.new_page()

        async def on_response(resp):
            ct = (resp.headers or {}).get("content-type", "")
            if "json" in ct or "/api/" in resp.url.lower():
                xhr.append((resp.status, resp.url, ct))

        page.on("response", lambda r: asyncio.ensure_future(on_response(r)))

        try:
            await page.goto(NEW, wait_until="networkidle", timeout=60_000)
        except Exception as e:
            print(f"  goto: {e}")
        await page.wait_for_timeout(6_000)

        print(f"  final url : {page.url}")
        print(f"  title     : {await page.title()}")

        hrefs = await page.eval_on_selector_all("a[href]", "els => els.map(e => e.href)")
        uniq = sorted(set(hrefs))
        print(f"\n  --- {len(uniq)} unique href(s) ---")
        for h in uniq:
            print(f"    {h}")

        body = await page.inner_text("body")
        print("\n  --- lines mentioning Change/Version/Chapter ---")
        seen = set()
        for line in body.splitlines():
            s = line.strip()
            low = s.lower()
            if s and s not in seen and any(k in low for k in ("change", "version", "chapter", "revision")):
                seen.add(s)
                print(f"    {s[:200]}")

        print(f"\n  --- {len(xhr)} JSON/API response(s) the page fetched ---")
        for status, url, ct in xhr:
            print(f"    {status} {ct.split(';')[0]}  {url}")

        # Dump the first JSON payload that looks like manual data
        for status, url, _ in xhr:
            if status == 200:
                try:
                    r = await ctx.request.get(url)
                    txt = (await r.text())[:3000]
                    print(f"\n  --- body of {url} (first 3000 chars) ---")
                    print(txt)
                    break
                except Exception as e:
                    print(f"    could not re-fetch {url}: {e}")

        await browser.close()


if __name__ == "__main__":
    probe_redirects()
    asyncio.run(probe_new())
    print("\nProbe complete.")
