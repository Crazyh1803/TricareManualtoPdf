#!/usr/bin/env python3
"""Settle whether the revision parameter retrieves historical content.

Sampled sections differ between revision 57 and 47 only in the footer's
revision number and date; the body text is identical. That is consistent with
the parameter being cosmetic, but also with those particular sections simply
not having been edited — a real possibility when a change touches only a few
sections out of ~300.

A test that does not depend on picking a lucky section: revision 57 lists 325
sections and revision 47 lists 323, so some sections exist now that did not
exist then. Requesting one of those AT revision 47 is decisive. If the archive
is real it cannot return a document. If it returns today's text, the revision
is a label and nothing more.

Also dumps /Explore/Changes, which is the site's own record of what changed
when — the thing a version feature would ultimately want anyway.
"""
import asyncio, re, time
import requests
from bs4 import BeautifulSoup
from playwright.async_api import async_playwright

BASE = "https://manuals.dha.mil"
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")
CHALLENGE = "Please enable JavaScript to view the page content"
REV_RE = re.compile(r"Revision\s+(\d+)\s*\(Published\s*([^)]*)\)", re.I)
FN_RE = re.compile(r"/FileName/([^/?#]+)", re.I)
session = requests.Session()
session.headers.update({"User-Agent": UA})


async def main():
    async with async_playwright() as pw:
        browser = await pw.chromium.launch(args=["--disable-blink-features=AutomationControlled"])
        ctx = await browser.new_context(user_agent=UA, viewport={"width": 1440, "height": 900})

        async def toc_at(rev=None):
            url = f"{BASE}/View-Publication/TPT5" + (f"/Revision/{rev}" if rev else "")
            page = await ctx.new_page()
            try:
                await page.goto(url, wait_until="networkidle", timeout=60_000)
                await page.wait_for_timeout(3_500)
                body = await page.inner_text("body")
                hrefs = await page.eval_on_selector_all(
                    "a[href*='/FileName/']", "els => els.map(e => e.getAttribute('href'))")
                m = REV_RE.search(body)
                names = {FN_RE.search(h).group(1) for h in hrefs if h and FN_RE.search(h)}
                return (int(m.group(1)) if m else None), names
            finally:
                await page.close()

        cur, names_cur = await toc_at()
        old = cur - 10
        rev_old, names_old = await toc_at(old)
        print(f"=== TPT5 section lists: revision {cur} vs {rev_old} ===")
        print(f"  rev {cur}: {len(names_cur)} sections")
        print(f"  rev {rev_old}: {len(names_old)} sections")
        added = sorted(names_cur - names_old)
        removed = sorted(names_old - names_cur)
        print(f"  present only at {cur}: {added}")
        print(f"  present only at {rev_old}: {removed}")

        # Changes register — the site's own record of what changed when.
        print(f"\n=== /Explore/Changes ===")
        page = await ctx.new_page()
        try:
            await page.goto(f"{BASE}/Explore/Changes", wait_until="networkidle", timeout=60_000)
            await page.wait_for_timeout(4_000)
            txt = await page.inner_text("body")
            lines = [l.strip() for l in txt.splitlines() if l.strip()]
            start = next((i for i, l in enumerate(lines) if "change" in l.lower()), 0)
            for l in lines[start:start + 28]:
                print(f"    {l[:120]}")
        except Exception as e:
            print(f"    ERROR {type(e).__name__}: {e}")
        finally:
            await page.close()

        cookies = await ctx.cookies()
        await browser.close()

    for c in cookies:
        if c.get("name") and c.get("value") is not None:
            session.cookies.set(c["name"], c["value"],
                                domain=c.get("domain") or "", path=c.get("path") or "/")

    hx = {"HX-Request": "true", "HX-Target": "publication-document",
          "Referer": f"{BASE}/View-Publication/TPT5"}

    print(f"\n=== Decisive: request a section at a revision that predates it ===")
    if not added:
        print("  no section was added between the two revisions; cannot run this test")
    for fn in added[:3]:
        for rev in (cur, old):
            url = f"{BASE}/api/publication/TPT5/{rev}/{fn}"
            try:
                r = session.get(url, headers=hx, timeout=45)
                r.encoding = "utf-8"
            except Exception as e:
                print(f"  {fn} @ rev {rev}: ERROR {type(e).__name__}")
                time.sleep(2.0)
                continue
            if r.status_code != 200:
                verdict = f"HTTP {r.status_code}"
            elif CHALLENGE in r.text:
                verdict = "challenged"
            else:
                soup = BeautifulSoup(r.text, "html.parser")
                doc = soup.select_one("#publication-document") or soup
                for nav in doc.select("nav"):
                    nav.decompose()
                body = " ".join(doc.get_text(" ", strip=True).split())
                verdict = f"{len(body)} chars of content"
            print(f"  {fn} @ rev {rev}: {verdict}")
            time.sleep(2.0)
        print(f"    -> {fn} did not exist at rev {old}. Content there means the")
        print(f"       revision selects a label, not an archived version.")

    print("\nProbe complete.")


asyncio.run(main())
