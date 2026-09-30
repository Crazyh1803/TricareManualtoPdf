#!/usr/bin/env python3
"""Does the site offer a manual (or chapter) as a downloadable file?

The ask is: current version always available, older versions fetched on
demand. Scraping on demand needs a backend and takes 5-15 minutes per manual,
which cannot feel like a download button. But if the site publishes a whole
manual or chapter as a PDF at a given revision, on-demand becomes fetching one
file — no scraping, no storage, and it works for any revision.

So: enumerate every download / print / export affordance on a publication page
and on a section page, and see whether any of them carries a revision.
"""
import asyncio, json, re
from playwright.async_api import async_playwright

BASE = "https://manuals.dha.mil"
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")
WORDS = ("download", "pdf", "print", "export", "save", "zip", "docx", "file")


async def dump(ctx, label, url):
    print(f"\n=== {label}: {url} ===")
    page = await ctx.new_page()
    seen = []
    page.on("request", lambda r: seen.append((r.method, r.url, r.resource_type)))
    try:
        await page.goto(url, wait_until="networkidle", timeout=60_000)
        await page.wait_for_timeout(4_000)

        hits = await page.evaluate("""(words) => {
            const out = [];
            const els = document.querySelectorAll(
                'a, button, [role=button], [download], [onclick], form, select, option');
            for (const e of els) {
                const hay = [
                    e.getAttribute('href') || '', e.getAttribute('download') || '',
                    e.getAttribute('onclick') || '', e.getAttribute('title') || '',
                    e.getAttribute('aria-label') || '', e.getAttribute('hx-get') || '',
                    e.getAttribute('name') || '', e.id || '',
                    (e.innerText || '').trim()
                ].join(' ').toLowerCase();
                if (words.some(w => hay.includes(w))) {
                    out.push({
                        tag: e.tagName.toLowerCase(),
                        text: (e.innerText || '').trim().slice(0, 60),
                        href: e.getAttribute('href') || '',
                        hxget: e.getAttribute('hx-get') || '',
                        id: e.id || '',
                        cls: (typeof e.className === 'string' ? e.className : '').slice(0, 50),
                    });
                }
            }
            return out;
        }""", list(WORDS))
        if hits:
            for h in hits[:25]:
                print(f"  <{h['tag']}> {h['text']!r}")
                if h['href']:  print(f"        href={h['href']}")
                if h['hxget']: print(f"        hx-get={h['hxget']}")
                if h['id'] or h['cls']:
                    print(f"        id={h['id']!r} class={h['cls']!r}")
        else:
            print("  no download/print/export affordance found")

        # Any <select> is a candidate revision picker — the site may already
        # have one, which would be the cleanest signal of what is offered.
        sels = await page.evaluate("""() => {
            const out = [];
            for (const s of document.querySelectorAll('select')) {
                out.push({
                    name: s.getAttribute('name') || s.id || '',
                    options: Array.from(s.options).slice(0, 12).map(o =>
                        ({value: o.value, text: (o.text || '').trim().slice(0, 50)}))
                });
            }
            return out;
        }""")
        if sels:
            print("  --- <select> elements ---")
            for s in sels:
                print(f"    name={s['name']!r} ({len(s['options'])} shown)")
                for o in s["options"][:8]:
                    print(f"      {o['value']!r} = {o['text']!r}")
    finally:
        await page.close()

    inter = [u for (m, u, t) in seen
             if any(w in u.lower() for w in ("pdf", "download", "export", "print"))]
    if inter:
        print("  --- requests mentioning a file format ---")
        for u in sorted(set(inter))[:10]:
            print(f"    {u[:150]}")


async def main():
    async with async_playwright() as pw:
        browser = await pw.chromium.launch(args=["--disable-blink-features=AutomationControlled"])
        ctx = await browser.new_context(user_agent=UA, viewport={"width": 1440, "height": 900})
        await dump(ctx, "publication page", f"{BASE}/View-Publication/TPT5")
        await dump(ctx, "section page",
                   f"{BASE}/View-Publication/TPT5/Revision/57/FileName/C1S1_1")
        await dump(ctx, "archives", f"{BASE}/Explore/Archives")
        await browser.close()
    print("\nProbe complete.")


asyncio.run(main())
