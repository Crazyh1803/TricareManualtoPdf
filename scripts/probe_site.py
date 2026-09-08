#!/usr/bin/env python3
"""Throwaway diagnostic #2: can we scrape manuals.dha.mil without a browser?

Probe #1 established the new URL scheme and that the revision number is
printed on the publication page. This answers the two questions that decide
how big the scraper rewrite is:

  1. Does a FileName/... URL serve real content to a plain HTTP request, or
     does it still need Playwright (the old site served plain clients a JS
     challenge)?
  2. Is manuals.health.mil dead or redirecting? That decides whether existing
     app installs degrade gracefully or just fail.

Also dumps the shape of a section page so the content extractor can be
written, and the revision number of every manual we track.
Delete once the retarget is done.
"""
import re, sys
import requests
from bs4 import BeautifulSoup

UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")
S = requests.Session()
S.headers.update({"User-Agent": UA})

NEW = "https://manuals.dha.mil"
CODES = ["TOT5", "TPT5", "TRT5", "TST5", "FR16"]
REV_RE = re.compile(r"Revision\s+(\d+)\s*\(Published([^)]*)\)", re.I)


def get(url, **kw):
    try:
        return S.get(url, timeout=45, **kw)
    except Exception as e:
        print(f"    ERROR {type(e).__name__}: {e}")
        return None


def q1_old_domain():
    print("\n=== Q1. Is manuals.health.mil dead or redirecting? ===")
    for url in ("https://manuals.health.mil/",
                "https://manuals.health.mil/pages/ManualToc.aspx?Manual=TPT5&Change=55"):
        r = get(url, allow_redirects=True)
        if r is None:
            continue
        print(f"  {url}")
        for h in r.history:
            print(f"    hop {h.status_code} -> {h.headers.get('location','')}")
        print(f"    final {r.status_code} {r.url} ({len(r.content)} bytes)")


def q2_plain_http():
    print("\n=== Q2. Does plain HTTP (no browser) get real content? ===")
    pub = get(f"{NEW}/View-Publication/TPT5")
    if pub is None:
        return None
    print(f"  publication page: {pub.status_code}, {len(pub.content)} bytes")
    soup = BeautifulSoup(pub.text, "html.parser")
    text = soup.get_text(" ", strip=True)
    m = REV_RE.search(text)
    print(f"  revision string found in plain HTML: {m.group(0) if m else 'NO'}")

    links = [a["href"] for a in soup.find_all("a", href=True) if "/FileName/" in a["href"]]
    print(f"  /FileName/ links in plain HTML: {len(links)}")
    if not links:
        print("  -> plain HTTP does NOT render the link list; a browser is still required.")
        return None
    for h in links[:3]:
        print(f"    e.g. {h}")

    target = links[0]
    if target.startswith("/"):
        target = NEW + target
    print(f"\n  --- fetching one section: {target}")
    sec = get(target)
    if sec is None:
        return None
    print(f"  {sec.status_code}, {len(sec.content)} bytes, {sec.headers.get('content-type','')}")
    ssoup = BeautifulSoup(sec.text, "html.parser")
    stext = ssoup.get_text(" ", strip=True)
    print(f"  visible text length: {len(stext)}")
    print(f"  first 400 chars: {stext[:400]!r}")

    print("\n  --- candidate content containers ---")
    for sel in ("main", "article", "#content", ".content", "#main-content",
                ".publication-content", ".manual-content", "table"):
        found = ssoup.select(sel)
        if found:
            print(f"    {sel!r}: {len(found)} node(s), first has "
                  f"{len(found[0].get_text(strip=True))} chars of text")
    print("\n  --- top-level structure under <body> ---")
    body = ssoup.body
    if body:
        for child in list(body.children)[:25]:
            if getattr(child, "name", None):
                cls = " ".join(child.get("class", []))
                cid = child.get("id", "")
                print(f"    <{child.name} id={cid!r} class={cls!r}> "
                      f"{len(child.get_text(strip=True))} chars")
    return target


def q3_revisions():
    print("\n=== Q3. Current revision of each manual we track ===")
    for code in CODES:
        r = get(f"{NEW}/View-Publication/{code}")
        if r is None:
            continue
        soup = BeautifulSoup(r.text, "html.parser")
        text = soup.get_text(" ", strip=True)
        m = REV_RE.search(text)
        n = len([a for a in soup.find_all("a", href=True) if "/FileName/" in a["href"]])
        title = (soup.title.string or "").strip() if soup.title else ""
        print(f"  {code}: {m.group(0) if m else 'revision NOT FOUND'} | "
              f"{n} FileName link(s) | title={title!r}")


if __name__ == "__main__":
    q1_old_domain()
    q2_plain_http()
    q3_revisions()
    print("\nProbe complete.")
