#!/usr/bin/env python3
"""Throwaway diagnostic: T-2017 manuals, and whether older revisions are fetchable.

Users want the T-2017 generation of manuals and a version picker. Two facts
decide whether that is possible and how much work it is:

  A. Which publication codes exist, and which are the T-2017 ones. The
     publications page lists codes we do not track (TO15, TP15, TR15, TS15,
     AD25, CM25, DC25, US25, WC25) but not what they are.

  B. Whether /api/publication/<code>/<revision>/<file> serves a superseded
     revision, or only the current one. The URL carries a revision number, but
     nothing so far has tested a number other than the current one. If old
     revisions answer, the scraper can archive them and the picker is real; if
     not, the feature cannot be built from this source at all.

Delete once the version work is done.
"""
import asyncio, re, sys, time
import requests
from bs4 import BeautifulSoup
from playwright.async_api import async_playwright

BASE = "https://manuals.dha.mil"
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")
CHALLENGE = "Please enable JavaScript to view the page content"
REV_RE = re.compile(r"Revision\s+(\d+)\s*\(Published\s*([^)]*)\)", re.I)

session = requests.Session()
session.headers.update({"User-Agent": UA})


async def main():
    async with async_playwright() as pw:
        browser = await pw.chromium.launch(args=["--disable-blink-features=AutomationControlled"])
        ctx = await browser.new_context(user_agent=UA, viewport={"width": 1440, "height": 900})

        # ── A. What publications exist ──────────────────────────────────────
        print("=== A. Publications listed on the site ===")
        page = await ctx.new_page()
        await page.goto(f"{BASE}/Explore/Publications", wait_until="networkidle", timeout=60_000)
        await page.wait_for_timeout(4_000)
        rows = await page.evaluate("""() => {
            const out = [];
            for (const a of document.querySelectorAll('a[href*="/View-Publication/"]')) {
                const href = a.getAttribute('href') || '';
                const m = href.match(/\\/View-Publication\\/([A-Za-z0-9]+)\\s*$/);
                if (!m) continue;
                // Climb to the row/card so the human-readable name comes too.
                let label = (a.innerText || '').trim();
                let p = a.parentElement;
                for (let i = 0; i < 3 && p && label.length < 12; i++) {
                    label = (p.innerText || '').trim(); p = p.parentElement;
                }
                out.push({code: m[1], label: label.replace(/\\s+/g, ' ').slice(0, 110)});
            }
            return out;
        }""")
        seen = set()
        for r in rows:
            if r["code"] in seen:
                continue
            seen.add(r["code"])
            print(f"  {r['code']:6} {r['label']}")
        if not rows:
            body = await page.inner_text("body")
            print("  no publication links found; page text follows")
            print("   ", body[:1200].replace("\n", "\n    "))
        await page.close()

        # ── Establish a session for the content API ─────────────────────────
        page = await ctx.new_page()
        await page.goto(f"{BASE}/View-Publication/TPT5", wait_until="networkidle", timeout=60_000)
        await page.wait_for_timeout(3_000)
        body = await page.inner_text("body")
        m = REV_RE.search(body)
        current = int(m.group(1)) if m else 56
        cookies = await ctx.cookies()
        await page.close()
        await browser.close()

    for c in cookies:
        if c.get("name") and c.get("value") is not None:
            session.cookies.set(c["name"], c["value"],
                                domain=c.get("domain") or "", path=c.get("path") or "/")
    print(f"\n  (carried {len(cookies)} cookies; TPT5 current revision = {current})")

    # ── B. Are superseded revisions served? ─────────────────────────────────
    print(f"\n=== B. Fetching TPT5/C1S1_1 at several revisions ===")
    hx = {"HX-Request": "true", "HX-Target": "publication-document",
          "Referer": f"{BASE}/View-Publication/TPT5"}
    for rev in (current, current - 1, current - 2, current - 5, current - 10, 1):
        if rev < 1:
            continue
        url = f"{BASE}/api/publication/TPT5/{rev}/C1S1_1"
        try:
            r = session.get(url, headers=hx, timeout=45)
            r.encoding = "utf-8"
        except Exception as e:
            print(f"  rev {rev:<3} ERROR {type(e).__name__}")
            time.sleep(2.0)
            continue
        if r.status_code != 200:
            print(f"  rev {rev:<3} HTTP {r.status_code}")
        elif CHALLENGE in r.text:
            print(f"  rev {rev:<3} CHALLENGED")
        else:
            soup = BeautifulSoup(r.text, "html.parser")
            txt = soup.get_text(" ", strip=True)
            # Does the served document state which revision it is?
            marker = ""
            for sel in (".Revision", ".Header", ".IssueDate"):
                el = soup.select_one(sel)
                if el:
                    marker = el.get_text(" ", strip=True)[:60]
                    break
            print(f"  rev {rev:<3} ok {len(r.content):>7,}B  {len(txt):>6} chars  marker={marker!r}")
        time.sleep(2.0)

    print("\nProbe complete.")


asyncio.run(main())
