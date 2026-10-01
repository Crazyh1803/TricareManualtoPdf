#!/usr/bin/env python3
"""Map /Explore/Changes so a reliable parser can be written for it.

Phase 1 of the version picker publishes the official revision history: for each
manual, every change number with its publication date and title. That register
is the only place this exists. The earlier dump showed the shape

    Establish East Region Virtual Value Network Pilot + CDRL
    Published on: 9/18/2026
    AD25 Change 2
    TST5 Change 40
    TOT5 Change 66
    Conreq: 23891

but not whether the page shows everything at once. It claims to span Dec 9 2020
to Sep 18 2026; if that is lazy-loaded or paginated, a parser that reads the
first screenful would silently publish a truncated history.

So: count entries, look for paging or filter controls, scroll to see whether
more appear, and dump the markup of a few entries so the parser matches real
structure rather than the rendered text.
"""
import asyncio, re
from playwright.async_api import async_playwright

BASE = "https://manuals.dha.mil"
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")
CODE_CHANGE = re.compile(r"\b([A-Z]{2}[A-Z0-9]{2})\s+Change\s+(\d+)\b")


async def main():
    async with async_playwright() as pw:
        browser = await pw.chromium.launch(args=["--disable-blink-features=AutomationControlled"])
        ctx = await browser.new_context(user_agent=UA, viewport={"width": 1440, "height": 1000})
        page = await ctx.new_page()
        await page.goto(f"{BASE}/Explore/Changes", wait_until="networkidle", timeout=60_000)
        await page.wait_for_timeout(5_000)

        async def census(label):
            text = await page.inner_text("body")
            pairs = CODE_CHANGE.findall(text)
            dates = re.findall(r"Published on:\s*([0-9/]+)", text)
            codes = sorted({c for c, _ in pairs})
            print(f"  {label}: {len(pairs)} code/change pairs, {len(dates)} dated entries")
            if dates:
                print(f"    newest={dates[0]}  oldest={dates[-1]}")
            print(f"    codes seen: {codes}")
            return len(pairs)

        print("=== initial render ===")
        before = await census("on load")

        # Paging or filtering controls?
        print("\n=== controls ===")
        ctrls = await page.evaluate("""() => {
            const out = [];
            const sel = 'button, a[role=button], select, input, [class*=pag], [class*=page], [aria-label*=ext], [aria-label*=rev]';
            for (const e of document.querySelectorAll(sel)) {
                const t = (e.innerText || e.getAttribute('aria-label') || e.getAttribute('placeholder') || '').trim();
                if (!t && !e.name) continue;
                out.push({tag: e.tagName.toLowerCase(), text: t.slice(0, 50),
                          name: e.getAttribute('name') || '', id: e.id || '',
                          cls: (typeof e.className === 'string' ? e.className : '').slice(0, 45)});
            }
            return out;
        }""")
        for c in ctrls[:20]:
            print(f"    <{c['tag']}> {c['text']!r} name={c['name']!r} id={c['id']!r} class={c['cls']!r}")
        if not ctrls:
            print("    none found")

        # Does scrolling load more?
        print("\n=== after scrolling to the bottom ===")
        for _ in range(6):
            await page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
            await page.wait_for_timeout(1_500)
        after = await census("after scroll")
        print(f"    {'MORE LOADED — the list is lazy' if after > before else 'no change — fully rendered on load'}")

        # Entry markup, so the parser targets elements not text.
        print("\n=== markup of the first entries ===")
        html = await page.evaluate("""() => {
            const t = [...document.querySelectorAll('*')]
                .find(e => /Published on:/.test(e.innerText || '') &&
                           e.children.length && (e.innerText || '').length < 400);
            if (!t) return null;
            const box = t.closest('div, li, article, tr') || t;
            const parent = box.parentElement;
            return {
                container: parent ? parent.tagName.toLowerCase() + '.' +
                           ((typeof parent.className === 'string' ? parent.className : '') || '') : '',
                siblings: parent ? parent.children.length : 0,
                sample: box.outerHTML.replace(/\\s+/g, ' ').slice(0, 900),
            };
        }""")
        if html:
            print(f"    container: {html['container']!r} with {html['siblings']} children")
            print(f"    entry markup:\n      {html['sample']}")
        else:
            print("    could not locate an entry element")

        await browser.close()
    print("\nProbe complete.")


asyncio.run(main())
