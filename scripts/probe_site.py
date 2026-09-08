#!/usr/bin/env python3
"""Throwaway diagnostic #4: fix titles and pick the right content node.

The FR16 test run fetched real content but titled every section "Publication
Information", and stored the Alpine wrapper around the document instead of the
document. Two questions, both needing the live DOM:

  A. On a publication page, what text does each /FileName/ anchor actually
     carry? innerText gave the same string for all 28 links, so this dumps
     every plausible source (innerText, textContent, title, aria-label) plus
     the anchor's own markup.
  B. Inside #publication-document, which elements hold the section title? FR16
     uses .Chapter + .CFRSubject; TPT5 is a different manual family and may
     not, so both are sampled before the extractor is written.

Delete once the retarget is done.
"""
import asyncio, json, sys
from playwright.async_api import async_playwright

BASE = "https://manuals.dha.mil"
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")


async def main():
    async with async_playwright() as pw:
        browser = await pw.chromium.launch(args=["--disable-blink-features=AutomationControlled"])
        ctx = await browser.new_context(user_agent=UA, viewport={"width": 1440, "height": 900})

        section_urls = {}
        for code in ("FR16", "TPT5"):
            print(f"\n=== A. {code}: text carried by each /FileName/ anchor ===")
            page = await ctx.new_page()
            await page.goto(f"{BASE}/View-Publication/{code}", wait_until="networkidle", timeout=60_000)
            await page.wait_for_timeout(4_000)
            rows = await page.evaluate("""() => {
                const out = [];
                for (const a of document.querySelectorAll('a[href*="/FileName/"]')) {
                    out.push({
                        href: a.getAttribute('href'),
                        innerText: (a.innerText || '').trim().slice(0, 90),
                        textContent: (a.textContent || '').trim().replace(/\\s+/g, ' ').slice(0, 90),
                        title: a.getAttribute('title') || '',
                        aria: a.getAttribute('aria-label') || '',
                        cls: a.className || '',
                        parentCls: a.parentElement ? (a.parentElement.className || '') : '',
                        html: a.outerHTML.replace(/\\s+/g, ' ').slice(0, 170),
                    });
                }
                return out;
            }""")
            print(f"  {len(rows)} anchor(s); showing 6")
            for r in rows[:6]:
                print(f"    href        : {r['href']}")
                print(f"      innerText : {r['innerText']!r}")
                print(f"      textContent:{r['textContent']!r}")
                print(f"      title/aria: {r['title']!r} / {r['aria']!r}")
                print(f"      class     : {r['cls']!r} parent={r['parentCls']!r}")
                print(f"      html      : {r['html']!r}")
            # keep a real (non-TOC) section to inspect in part B
            for r in rows:
                tok = r["href"].rstrip("/").rsplit("/", 1)[-1].upper()
                if not tok.endswith("TOC"):
                    section_urls[code] = BASE + r["href"].split("?")[0]
                    break
            await page.close()

        for code, url in section_urls.items():
            print(f"\n=== B. {code}: title-bearing elements in #publication-document ===")
            print(f"  {url}")
            page = await ctx.new_page()
            await page.goto(url, wait_until="networkidle", timeout=60_000)
            await page.wait_for_timeout(4_000)
            rows = await page.evaluate("""() => {
                const doc = document.querySelector('#publication-document');
                if (!doc) return null;
                const out = [];
                let n = 0;
                for (const el of doc.querySelectorAll('*')) {
                    if (n++ > 22) break;
                    const t = (el.innerText || '').trim().replace(/\\s+/g, ' ');
                    out.push({tag: el.tagName.toLowerCase(),
                              cls: el.className || '', txt: t.slice(0, 80)});
                }
                return out;
            }""")
            if rows is None:
                print("  #publication-document NOT FOUND")
            else:
                for r in rows:
                    print(f"    <{r['tag']} class={str(r['cls'])[:38]!r}> {r['txt']!r}")
            await page.close()

        await browser.close()
    print("\nProbe complete.")


asyncio.run(main())
