#!/usr/bin/env python3
"""Throwaway diagnostic #6: can the Android app reach the new site directly?

The app fetches with plain OkHttp — no browser, and OkHttp keeps no cookie jar
by default. Two questions decide whether a URL swap is even possible, or
whether the app has to read from our own published mirror instead:

  A. Section content. Probe #5 showed a *fresh, cookie-less* request with
     htmx's headers gets the real document, while our scraper's session — which
     had picked up a cookie from visiting the site root — was refused. If
     cookie-less access holds up over a realistic run of requests, the app can
     fetch content directly. This makes 25 sequential requests and counts how
     many return content rather than the interstitial.

  B. Section discovery. The publication page is a Blazor shell over plain HTTP
     (~6 KB, no links). Without a browser the app cannot learn which sections
     exist. This confirms that, and checks whether the master TOC document —
     which IS reachable through the content API — lists them instead.

Delete once the Android retarget is done.
"""
import re, sys, time
import requests
from bs4 import BeautifulSoup

BASE = "https://manuals.dha.mil"
UA = ("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
CHALLENGE = "Please enable JavaScript to view the page content"

SECTIONS = [f"C{c}S{s}_1" for c in (1, 2, 3) for s in (1, 2, 3, 4, 5)] + \
           [f"C4S{s}_1" for s in range(1, 11)]


def hx_headers(code):
    return {
        "User-Agent": UA,
        "HX-Request": "true",
        "HX-Target": "publication-document",
        "Referer": f"{BASE}/View-Publication/{code}",
    }


print("=== A. 25 cookie-less content requests (no session, htmx headers) ===")
ok = challenged = other = 0
first_fail = None
t0 = time.monotonic()
for i, fn in enumerate(SECTIONS[:25], 1):
    url = f"{BASE}/api/publication/TPT5/56/{fn}"
    try:
        # A brand-new connection each time, exactly like OkHttp with no cookie jar.
        r = requests.get(url, headers=hx_headers("TPT5"), timeout=45)
    except Exception as e:
        other += 1
        print(f"  {i:2}. {fn:10} ERROR {type(e).__name__}")
        continue
    body = r.text
    if r.status_code != 200:
        other += 1
        state = f"HTTP {r.status_code}"
    elif CHALLENGE in body:
        challenged += 1
        state = "CHALLENGED"
        if first_fail is None:
            first_fail = (i, round(time.monotonic() - t0))
    else:
        ok += 1
        state = f"ok {len(body):,}B"
    print(f"  {i:2}. {fn:10} {state}")
    time.sleep(1.5)

print(f"\n  content={ok}  challenged={challenged}  other={other}  "
      f"elapsed={round(time.monotonic()-t0)}s")
if first_fail:
    print(f"  first refusal at request {first_fail[0]} after {first_fail[1]}s")
else:
    print("  no refusals — cookie-less access held for the whole run")

print("\n=== B. Can a browserless client discover sections? ===")
r = requests.get(f"{BASE}/View-Publication/TPT5", headers={"User-Agent": UA}, timeout=45)
soup = BeautifulSoup(r.text, "html.parser")
links = [a["href"] for a in soup.find_all("a", href=True) if "/FileName/" in a["href"]]
print(f"  publication page: HTTP {r.status_code}, {len(r.content)}B, "
      f"{len(links)} section link(s)")

r2 = requests.get(f"{BASE}/api/publication/TPT5/56/TPT5TOC",
                  headers=hx_headers("TPT5"), timeout=45)
print(f"  master TOC via content API: HTTP {r2.status_code}, {len(r2.content)}B")
if r2.status_code == 200 and CHALLENGE not in r2.text:
    s2 = BeautifulSoup(r2.text, "html.parser")
    hrefs = [a.get("href", "") for a in s2.find_all("a", href=True)]
    fn = [h for h in hrefs if "/FileName/" in h]
    hx = [a.get("hx-get", "") for a in s2.find_all(attrs={"hx-get": True})]
    print(f"    {len(hrefs)} href(s), {len(fn)} with /FileName/, {len(hx)} hx-get")
    for h in (fn or hx)[:5]:
        print(f"      {h}")
    if not fn and not hx:
        print(f"    text sample: {s2.get_text(' ', strip=True)[:300]!r}")

print("\nProbe complete.")
