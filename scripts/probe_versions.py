#!/usr/bin/env python3
"""Why did paging the change register not work?

The revision fetch read only page 1 — 12 cards, one month of history — even
though it asked for ?PageSize=48 and then looked for a link to page 2. Both
were ignored, which says the controls are not plain links or query parameters.
Dump their actual attributes, and try clicking one to see what happens.
"""
import asyncio, json, re
from playwright.async_api import async_playwright

BASE = "https://manuals.dha.mil"
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")


async def main():
    async with async_playwright() as pw:
        browser = await pw.chromium.launch(args=["--disable-blink-features=AutomationControlled"])
        ctx = await browser.new_context(user_agent=UA, viewport={"width": 1440, "height": 1000})
        page = await ctx.new_page()

        reqs = []
        page.on("request", lambda r: reqs.append((r.method, r.url)))

        # Does the PageSize query parameter do anything at all?
        await page.goto(f"{BASE}/Explore/Changes?PageSize=48",
                        wait_until="networkidle", timeout=60_000)
        await page.wait_for_timeout(4_000)
        n = await page.eval_on_selector_all(".changepackage-card", "els => els.length")
        sel = await page.evaluate(
            "() => { const s = document.querySelector('#PageSize'); return s ? s.value : null; }")
        print(f"=== ?PageSize=48 -> {n} cards on page, select shows {sel!r} ===")

        print("\n=== every attribute on the paging controls ===")
        attrs = await page.evaluate("""() => {
            const out = [];
            for (const a of document.querySelectorAll('.pagination a, .pagination button')) {
                const o = {text: (a.innerText || '').trim(), tag: a.tagName.toLowerCase(), attrs: {}};
                for (const at of a.attributes) o.attrs[at.name] = at.value.slice(0, 160);
                out.push(o);
            }
            return out;
        }""")
        for a in attrs[:8]:
            print(f"  <{a['tag']}> {a['text']!r}")
            for k, v in a["attrs"].items():
                print(f"       {k}={v!r}")

        print("\n=== the PageSize select's own attributes ===")
        sattrs = await page.evaluate("""() => {
            const s = document.querySelector('#PageSize');
            if (!s) return null;
            const o = {};
            for (const at of s.attributes) o[at.name] = at.value.slice(0, 200);
            return o;
        }""")
        print(f"  {json.dumps(sattrs, indent=2) if sattrs else 'not found'}")

        # Is there a form wrapping them?
        form = await page.evaluate("""() => {
            const f = document.querySelector('#PageSize') &&
                      document.querySelector('#PageSize').closest('form');
            if (!f) return null;
            return {action: f.getAttribute('action'), method: f.getAttribute('method'),
                    id: f.id, hx: f.getAttribute('hx-get') || f.getAttribute('hx-post') || ''};
        }""")
        print(f"\n=== wrapping form ===\n  {form}")

        # Click page 2 and watch what the page requests.
        print("\n=== clicking page 2 ===")
        reqs.clear()
        try:
            link = page.locator(".pagination a.page-link", has_text=re.compile(r"^\s*2\s*$")).first
            await link.click(timeout=10_000)
            await page.wait_for_timeout(5_000)
            n2 = await page.eval_on_selector_all(".changepackage-card", "els => els.length")
            txt = await page.inner_text("body")
            dates = re.findall(r"Published on:\s*([0-9/]+)", txt)
            print(f"  after click: url={page.url}")
            print(f"  {n2} cards; newest={dates[0] if dates else '?'} oldest={dates[-1] if dates else '?'}")
            for m, u in reqs:
                if "manuals.dha.mil" in u and "/css/" not in u and "/js/" not in u and "/lib/" not in u:
                    print(f"    {m} {u[:150]}")
        except Exception as e:
            print(f"  click failed: {type(e).__name__}: {e}")

        await browser.close()
    print("\nProbe complete.")


asyncio.run(main())
