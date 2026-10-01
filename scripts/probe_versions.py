#!/usr/bin/env python3
"""Is an older revision's text reachable, or only its change number?

The register gives every published change with its date. The question the
version picker turns on is whether the site will still serve the text of a
superseded change:

  1. /View-Publication/{CODE}/Revision/{N} — does it load, and do its section
     links carry revision N rather than the current one?
  2. /api/publication/{CODE}/{N}/{FILE} — does it return that revision's text,
     and does the text actually differ from the current revision's?

A yes to 1 means the picker can deep-link honestly. A yes to 2 means an
on-demand fetch of an older revision is possible at all.
"""
import asyncio, re, sys

import requests
from playwright.async_api import async_playwright

BASE = "https://manuals.dha.mil"
CODE = "TOT5"
OLD = 64          # listed in the register, two changes behind
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")
HX = {"HX-Request": "true", "HX-Target": "publication-document"}

ANCHORS_JS = """() => {
    const out = [];
    for (const a of document.querySelectorAll('a[hx-get]')) {
        out.push({text: (a.innerText || '').trim().slice(0, 70),
                  hx: a.getAttribute('hx-get')});
    }
    return out;
}"""

PAGE_JS = """() => ({
    title: document.title,
    h1: (document.querySelector('h1') ? document.querySelector('h1').innerText : '').trim(),
    url: location.href,
    revisionText: (document.body.innerText.match(/Change\\s+\\d+/g) || []).slice(0, 6),
    selects: Array.from(document.querySelectorAll('select')).map(s => ({
        id: s.id, name: s.getAttribute('name'),
        options: Array.from(s.options).slice(0, 8).map(o => o.text.trim())
    })),
})"""


async def open_page(ctx, url, label):
    page = await ctx.new_page()
    try:
        print(f"\n=== {label} ===\n{url}")
        resp = await page.goto(url, wait_until="networkidle", timeout=60_000)
        await page.wait_for_timeout(4_000)
        print(f"  HTTP {resp.status if resp else '?'}")
        info = await page.evaluate(PAGE_JS)
        print(f"  landed on : {info['url']}")
        print(f"  title     : {info['title']}")
        print(f"  h1        : {info['h1']}")
        print(f"  'Change N' on page: {info['revisionText']}")
        for s in info["selects"]:
            print(f"  <select {s['id'] or s['name']}>: {s['options']}")
        anchors = await page.evaluate(ANCHORS_JS)
        print(f"  {len(anchors)} section link(s)")
        for a in anchors[:4]:
            print(f"    {a['hx']}\n      {a['text']}")
        return anchors, await ctx.cookies()
    finally:
        await page.close()


async def main():
    async with async_playwright() as pw:
        browser = await pw.chromium.launch(
            args=["--disable-blink-features=AutomationControlled"])
        ctx = await browser.new_context(user_agent=UA,
                                        viewport={"width": 1440, "height": 1000})
        cur_anchors, _ = await open_page(
            ctx, f"{BASE}/View-Publication/{CODE}", "current revision")
        old_anchors, cookies = await open_page(
            ctx, f"{BASE}/View-Publication/{CODE}/Revision/{OLD}",
            f"revision {OLD} (two changes behind)")
        await browser.close()

    revs_cur = sorted({m for a in cur_anchors
                       for m in re.findall(rf"/{CODE}/(\d+)/", a["hx"] or "")})
    revs_old = sorted({m for a in old_anchors
                       for m in re.findall(rf"/{CODE}/(\d+)/", a["hx"] or "")})
    print(f"\nrevision numbers in the current page's links : {revs_cur}")
    print(f"revision numbers in the /Revision/{OLD} links  : {revs_old}")

    if not cur_anchors:
        print("no section links at all — the probe cannot answer the question",
              file=sys.stderr)
        return

    if not revs_cur:
        print("the current page's links carry no revision number — "
              "the API path assumption is wrong", file=sys.stderr)
        return

    name = (re.search(rf"/api/publication/{CODE}/\d+/([^/?]+)", cur_anchors[0]["hx"] or "")
            or [None, None])
    name = name[1] if name else None
    if not name:
        print(f"could not read a file name out of {cur_anchors[0]['hx']!r}", file=sys.stderr)
        return

    s = requests.Session()
    s.headers.update({"User-Agent": UA, "Accept-Language": "en-US,en;q=0.9"})
    for c in cookies:
        s.cookies.set(c["name"], c["value"],
                      domain=c.get("domain") or "", path=c.get("path") or "/")

    print(f"\n=== the same file at two revisions: {name} ===")
    bodies = {}
    for rev in (None, OLD):
        url = f"{BASE}/api/publication/{CODE}/{rev if rev else revs_cur[-1]}/{name}"
        try:
            r = s.get(url, headers={**HX, "Referer": f"{BASE}/View-Publication/{CODE}"},
                      timeout=45)
            r.encoding = "utf-8"
            body = r.text
            bodies[rev] = body
            print(f"\n  {url}\n    HTTP {r.status_code}, {len(body)} chars")
            print("    " + re.sub(r"\s+", " ", body[:240]))
        except Exception as e:
            print(f"\n  {url}\n    {type(e).__name__}: {e}")

    a, b = bodies.get(None), bodies.get(OLD)
    if a and b:
        print(f"\nidentical: {a == b}")

if __name__ == "__main__":
    asyncio.run(main())
