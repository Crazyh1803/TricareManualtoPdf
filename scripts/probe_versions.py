#!/usr/bin/env python3
"""Does /api/publication/<code>/<revision>/<file> actually serve that revision?

The first pass was inconclusive: several revisions of TPT5/C1S1_1 all came back
about the same size with the same internal marker. That is equally consistent
with "the server ignores the revision" and with "this section genuinely has not
changed". The distinction decides whether a version picker is possible at all,
so this tests it properly:

  A. Hash the extracted text of several sections at the current revision and at
     one ten changes older. Across ten published changes some sampled section
     must differ — if every hash matches, the revision in the URL is ignored.
  B. Ask the publication page itself for an old revision and read the revision
     it reports back.
  C. Read the real titles of the TO15/TP15/TR15/TS15 manuals, to confirm which
     generation they are before any of them is added.

Delete once the version work is done.
"""
import asyncio, hashlib, re, sys, time
import requests
from bs4 import BeautifulSoup
from playwright.async_api import async_playwright

BASE = "https://manuals.dha.mil"
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")
CHALLENGE = "Please enable JavaScript to view the page content"
REV_RE = re.compile(r"Revision\s+(\d+)\s*\(Published\s*([^)]*)\)", re.I)
SAMPLE = ["C1S1_1", "C1S1_2", "C2S1_1", "C4S1_1", "C7S1_1", "C8S1_1"]

session = requests.Session()
session.headers.update({"User-Agent": UA})


def body_hash(html):
    """Hash only the document text, ignoring any per-request noise."""
    soup = BeautifulSoup(html, "html.parser")
    doc = soup.select_one("#publication-document") or soup
    for nav in doc.select("nav"):
        nav.decompose()
    text = " ".join(doc.get_text(" ", strip=True).split())
    return hashlib.sha256(text.encode()).hexdigest()[:16], len(text)


async def main():
    async with async_playwright() as pw:
        browser = await pw.chromium.launch(args=["--disable-blink-features=AutomationControlled"])
        ctx = await browser.new_context(user_agent=UA, viewport={"width": 1440, "height": 900})

        # ── C. What the older-generation manuals actually are ───────────────
        print("=== C. Titles of the manuals users are asking for ===")
        for code in ("TO15", "TP15", "TR15", "TS15", "TOT5"):
            page = await ctx.new_page()
            try:
                await page.goto(f"{BASE}/View-Publication/{code}",
                                wait_until="networkidle", timeout=60_000)
                await page.wait_for_timeout(3_500)
                body = await page.inner_text("body")
                m = REV_RE.search(body)
                # The manual's formal title is the .Header line in the document.
                header = await page.evaluate(
                    "() => { const e = document.querySelector('.Header'); "
                    "return e ? e.innerText.trim() : ''; }")
                links = await page.eval_on_selector_all(
                    "a[href*='/FileName/']", "els => els.length")
                print(f"  {code}: rev={m.group(1) if m else '??'} "
                      f"published={m.group(2).strip() if m else '??'} "
                      f"sections={links}")
                print(f"        title={header!r}")
            except Exception as e:
                print(f"  {code}: ERROR {type(e).__name__}: {e}")
            finally:
                await page.close()

        # ── Session for the content API ─────────────────────────────────────
        page = await ctx.new_page()
        await page.goto(f"{BASE}/View-Publication/TPT5", wait_until="networkidle", timeout=60_000)
        await page.wait_for_timeout(3_000)
        body = await page.inner_text("body")
        cur = int(REV_RE.search(body).group(1))
        cookies = await ctx.cookies()

        # ── B. Ask the publication page for an old revision ─────────────────
        print(f"\n=== B. Publication page asked for an old revision ===")
        for rev in (cur, cur - 10):
            p2 = await ctx.new_page()
            try:
                await p2.goto(f"{BASE}/View-Publication/TPT5/Revision/{rev}",
                              wait_until="networkidle", timeout=60_000)
                await p2.wait_for_timeout(3_000)
                t = await p2.inner_text("body")
                m = REV_RE.search(t)
                hrefs = await p2.eval_on_selector_all(
                    "a[href*='/FileName/']", "els => els.map(e => e.getAttribute('href'))")
                revs_in_links = sorted({int(x) for h in hrefs
                                        for x in re.findall(r"/Revision/(\d+)/", h or "")})
                print(f"  asked rev {rev:<3} -> page says {m.group(0) if m else '??'}; "
                      f"{len(hrefs)} links referencing revisions {revs_in_links}")
            except Exception as e:
                print(f"  asked rev {rev}: ERROR {type(e).__name__}")
            finally:
                await p2.close()
        await page.close()
        await browser.close()

    for c in cookies:
        if c.get("name") and c.get("value") is not None:
            session.cookies.set(c["name"], c["value"],
                                domain=c.get("domain") or "", path=c.get("path") or "/")

    # ── A. Same sections, current vs ten changes back ───────────────────────
    old = cur - 10
    print(f"\n=== A. Section text: revision {cur} vs {old} ===")
    hx = {"HX-Request": "true", "HX-Target": "publication-document",
          "Referer": f"{BASE}/View-Publication/TPT5"}
    same = diff = errors = 0
    for fn in SAMPLE:
        got = {}
        for rev in (cur, old):
            try:
                r = session.get(f"{BASE}/api/publication/TPT5/{rev}/{fn}",
                                headers=hx, timeout=45)
                r.encoding = "utf-8"
                if r.status_code == 200 and CHALLENGE not in r.text:
                    got[rev] = body_hash(r.text)
            except Exception:
                pass
            time.sleep(2.0)
        if len(got) < 2:
            print(f"  {fn:10} could not fetch both")
            errors += 1
            continue
        (h_new, n_new), (h_old, n_old) = got[cur], got[old]
        match = h_new == h_old
        same += match
        diff += (not match)
        print(f"  {fn:10} r{cur}={h_new} ({n_new:>6} chars)   "
              f"r{old}={h_old} ({n_old:>6} chars)   "
              f"{'IDENTICAL' if match else 'DIFFERENT'}")

    print(f"\n  identical={same}  different={diff}  errors={errors}")
    if diff == 0 and same > 0:
        print("  => every sampled section is byte-identical ten changes apart.")
        print("     The revision in the URL is not selecting a historical version:")
        print("     the server serves current content whatever number is asked for.")
    elif diff:
        print("  => content genuinely varies by revision; historical versions are real.")


asyncio.run(main())
