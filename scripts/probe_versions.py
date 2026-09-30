#!/usr/bin/env python3
"""Show exactly what differs between two revisions of the same section.

The hash test said five of five sections differ between revision 57 and 47 —
but every pair had an identical character count, which is the signature of a
short same-length substitution (the revision number echoed into the markup),
not of edited text. If that is all that differs, a version picker would hand
people current content wearing an old label.

This prints the actual differing runs so the question is settled by looking at
them rather than by inference.
"""
import difflib, re, sys, time
import requests
from bs4 import BeautifulSoup
from playwright.async_api import async_playwright
import asyncio

BASE = "https://manuals.dha.mil"
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")
REV_RE = re.compile(r"Revision\s+(\d+)\s*\(Published\s*([^)]*)\)", re.I)
session = requests.Session()
session.headers.update({"User-Agent": UA})


def text_of(html):
    soup = BeautifulSoup(html, "html.parser")
    doc = soup.select_one("#publication-document") or soup
    for nav in doc.select("nav"):
        nav.decompose()
    return " ".join(doc.get_text(" ", strip=True).split())


async def cookies_for(code):
    async with async_playwright() as pw:
        b = await pw.chromium.launch(args=["--disable-blink-features=AutomationControlled"])
        ctx = await b.new_context(user_agent=UA, viewport={"width": 1440, "height": 900})
        page = await ctx.new_page()
        await page.goto(f"{BASE}/View-Publication/{code}", wait_until="networkidle", timeout=60_000)
        await page.wait_for_timeout(3_000)
        body = await page.inner_text("body")
        cur = int(REV_RE.search(body).group(1))
        cks = await ctx.cookies()
        await ctx.close(); await b.close()
    return cur, cks


cur, cks = asyncio.run(cookies_for("TPT5"))
for c in cks:
    if c.get("name") and c.get("value") is not None:
        session.cookies.set(c["name"], c["value"],
                            domain=c.get("domain") or "", path=c.get("path") or "/")

old = cur - 10
hx = {"HX-Request": "true", "HX-Target": "publication-document",
      "Referer": f"{BASE}/View-Publication/TPT5"}

for fn in ("C1S1_2", "C8S1_1"):
    print(f"\n=== TPT5/{fn}: revision {cur} vs {old} ===")
    texts = {}
    for rev in (cur, old):
        r = session.get(f"{BASE}/api/publication/TPT5/{rev}/{fn}", headers=hx, timeout=45)
        r.encoding = "utf-8"
        texts[rev] = text_of(r.text)
        time.sleep(2.0)
    a, b = texts[cur], texts[old]
    print(f"  lengths: r{cur}={len(a)}  r{old}={len(b)}")
    sm = difflib.SequenceMatcher(None, a, b, autojunk=False)
    runs = [op for op in sm.get_opcodes() if op[0] != "equal"]
    print(f"  {len(runs)} differing run(s)")
    for tag, i1, i2, j1, j2 in runs[:12]:
        print(f"    {tag:8} r{cur}[{i1}:{i2}]={a[i1:i2]!r}")
        print(f"             r{old}[{j1}:{j2}]={b[j1:j2]!r}")
        # A little context makes it obvious whether this is prose or a number.
        print(f"             context: …{a[max(0,i1-60):i1]}[HERE]{a[i2:i2+60]}…")
    if not runs:
        print("    identical")

print("\nProbe complete.")
