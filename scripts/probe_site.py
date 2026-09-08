#!/usr/bin/env python3
"""Verify the mirror endpoints the Android app now depends on.

MirrorClient was written against a GitHub Pages URL that could not be reached
from the dev container, so it is so far an assumption. If the base URL or any
path shape is wrong the app is completely broken — no manual list, no content —
so check each call it makes, in the same order, against the live site.

Keep this one: it is a cheap end-to-end check of the contract between the
scraper's output and the app's expectations, worth re-running whenever either
side changes.
"""
import json, sys
import requests

MIRROR_BASE = "https://crazyh1803.github.io/TricareManualtoPdf/data"
SOURCE_BASE = "https://manuals.dha.mil"

fails = []


def check(name, cond, detail=""):
    print(("PASS " if cond else "FAIL ") + name + (f"  -> {detail}" if not cond else ""))
    if not cond:
        fails.append(name)


def get(url):
    try:
        r = requests.get(url, timeout=45)
        return r
    except Exception as e:
        print(f"    ERROR {type(e).__name__}: {e}")
        return None


print(f"=== MirrorClient.fetchManuals() -> {MIRROR_BASE}/manuals.json ===")
r = get(f"{MIRROR_BASE}/manuals.json")
check("manifest reachable", r is not None and r.status_code == 200,
      f"HTTP {r.status_code if r else 'n/a'}")
manuals = []
if r is not None and r.status_code == 200:
    try:
        data = json.loads(r.text)
        manuals = data["manuals"]
        print(f"    lastUpdated={data.get('lastUpdated')}  manuals={len(manuals)}")
        for m in manuals:
            print(f"      {m['code']:6} change={m['latestChange']:<3} "
                  f"hasContent={m['hasContent']}  {m['name']}")
        # The fields MirrorClient reads by name.
        check("manifest has the fields the app reads",
              all(k in manuals[0] for k in ("code", "name", "latestChange", "hasContent")),
              list(manuals[0]))
        check("FR16 present (missing from the app's old hardcoded list)",
              any(m["code"] == "FR16" for m in manuals))
    except Exception as e:
        check("manifest parses", False, e)

if manuals:
    code = manuals[0]["code"]
    print(f"\n=== MirrorClient.fetchToc({code}) -> {MIRROR_BASE}/{code}/toc.json ===")
    r = get(f"{MIRROR_BASE}/{code}/toc.json")
    check("toc reachable", r is not None and r.status_code == 200,
          f"HTTP {r.status_code if r else 'n/a'}")
    if r is not None and r.status_code == 200:
        toc = json.loads(r.text)
        sections = toc["sections"]
        content = [s for s in sections if not s.get("isChapterToc")]
        print(f"    change={toc['change']}  sections={len(sections)}  content={len(content)}")
        check("toc has the fields the app reads",
              all(k in sections[0] for k in
                  ("id", "name", "title", "chapter", "section", "isChapterToc")),
              list(sections[0]))
        check("toc change matches the manifest",
              toc["change"] == manuals[0]["latestChange"],
              f"{toc['change']} vs {manuals[0]['latestChange']}")

        first = content[0]
        print(f"\n=== MirrorClient.fetchSectionHtml({code}, {first['id']}) ===")
        r = get(f"{MIRROR_BASE}/{code}/s/{first['id']}.html")
        check("section html reachable", r is not None and r.status_code == 200,
              f"HTTP {r.status_code if r else 'n/a'}")
        if r is not None and r.status_code == 200:
            print(f"    {len(r.content):,}B  title={first['title']!r}")
            check("section html is real content", len(r.content) > 500, len(r.content))

        # The link the reader's "View official page" builds.
        src = (f"{SOURCE_BASE}/View-Publication/{code}/Revision/{toc['change']}"
               f"/FileName/{first['name']}")
        print(f"\n=== MirrorClient.sourceUrl(...) -> {src} ===")
        print("    (not fetched: the site refuses browserless clients — that is why")
        print("     the app reads the mirror. Shape is checked instead.)")
        check("source url uses the site's own identifier",
              first["name"] and "/FileName/" in src and str(toc["change"]) in src, src)

print(f"\n{len(fails)} failure(s)")
sys.exit(1 if fails else 0)
