#!/usr/bin/env python3
"""Throwaway diagnostic #3: everything needed to write the new scraper.

Probe #2 settled that plain HTTP gets only a ~5.8 KB SPA shell — no links, no
title, no revision — so Playwright stays. This is the last probe: it collects
the concrete details the rewrite needs.

  A. Per manual: revision number, published date, manual name, link count.
  B. One section page: which DOM node holds the content, and a sample of its
     HTML, so the extractor can be written against something real.
  C. Every network request the page makes, so a JSON API (if one exists) is
     not missed — probe #1 only logged json content-types and caught a CSS
     file on an /api/ path, which hints there is more under /api/.

Delete once the retarget is done.
"""
import asyncio, re, sys
from playwright.async_api import async_playwright

BASE = "https://manuals.dha.mil"
CODES = ["TOT5", "TPT5", "TRT5", "TST5", "FR16"]
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")
REV_RE = re.compile(r"Revision\s+(\d+)\s*\(Published\s*([^)]*)\)", re.I)


async def main():
    async with async_playwright() as pw:
        browser = await pw.chromium.launch(args=["--disable-blink-features=AutomationControlled"])
        ctx = await browser.new_context(user_agent=UA, viewport={"width": 1440, "height": 900})

        requests_seen = []
        first_section = None

        print("\n=== A. Revision of each tracked manual ===")
        for code in CODES:
            page = await ctx.new_page()
            page.on("request", lambda r: requests_seen.append((r.method, r.url,
                                                              r.resource_type)))
            url = f"{BASE}/View-Publication/{code}"
            try:
                await page.goto(url, wait_until="networkidle", timeout=60_000)
            except Exception as e:
                print(f"  {code}: goto failed: {e}")
                await page.close()
                continue
            await page.wait_for_timeout(4_000)

            title = await page.title()
            body = await page.inner_text("body")
            m = REV_RE.search(body)
            hrefs = await page.eval_on_selector_all(
                "a[href]", "els => els.map(e => e.getAttribute('href'))")
            fn = [h for h in hrefs if h and "/FileName/" in h]
            tocs = [h for h in fn if h.rstrip("/").upper().endswith("TOC")]

            print(f"  {code}: revision={m.group(1) if m else '??'} "
                  f"published={m.group(2).strip() if m else '??'} "
                  f"| {len(fn)} section link(s), {len(tocs)} TOC-ish "
                  f"| title={title!r}")
            if code == "TPT5" and fn:
                first_section = fn[0] if fn[0].startswith("http") else BASE + fn[0]
                print(f"    sample links: {fn[:3]}")
                print(f"    sample TOCs : {tocs[:3]}")
            await page.close()

        print("\n=== B. Structure of one section page ===")
        if not first_section:
            print("  no section link captured — cannot continue")
        else:
            page = await ctx.new_page()
            print(f"  {first_section}")
            try:
                await page.goto(first_section, wait_until="networkidle", timeout=60_000)
            except Exception as e:
                print(f"  goto failed: {e}")
            await page.wait_for_timeout(4_000)
            print(f"  final url: {page.url}")
            print(f"  title    : {await page.title()}")

            info = await page.evaluate("""() => {
                const out = [];
                const walk = (el, depth) => {
                    if (depth > 4) return;
                    for (const c of el.children) {
                        const txt = (c.innerText || '').trim();
                        out.push({
                            depth,
                            tag: c.tagName.toLowerCase(),
                            id: c.id || '',
                            cls: (c.className && c.className.baseVal !== undefined
                                  ? c.className.baseVal : c.className) || '',
                            chars: txt.length,
                        });
                        walk(c, depth + 1);
                    }
                };
                walk(document.body, 0);
                return out;
            }""")
            print("\n  --- nodes with >200 chars of text (content candidates) ---")
            for n in info:
                if n["chars"] > 200:
                    pad = "  " * n["depth"]
                    print(f"    {pad}<{n['tag']} id={n['id']!r} class={str(n['cls'])[:60]!r}> "
                          f"{n['chars']} chars")

            for sel in ("main", "article", "#content", ".content", "#main-content",
                        "[class*=publication]", "[class*=document]", "[class*=viewer]",
                        "[class*=manual]", "iframe"):
                try:
                    c = await page.eval_on_selector_all(sel, "els => els.length")
                except Exception:
                    c = 0
                if c:
                    print(f"    selector {sel!r} matches {c}")

            print("\n  --- first 1500 chars of visible text ---")
            print("   ", (await page.inner_text("body"))[:1500].replace("\n", "\n    "))
            await page.close()

        print("\n=== C. Network requests (non-image), deduped ===")
        seen = set()
        for method, url, rtype in requests_seen:
            if rtype in ("image", "font", "media"):
                continue
            key = (method, url.split("?")[0])
            if key in seen:
                continue
            seen.add(key)
            print(f"  {method:4} {rtype:12} {url[:160]}")

        await browser.close()
    print("\nProbe complete.")


if __name__ == "__main__":
    asyncio.run(main())
