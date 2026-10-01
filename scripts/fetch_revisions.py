#!/usr/bin/env python3
"""
TRICARE Manuals — revision history
==================================
Publishes, for every manual, the list of published changes with the date and
title of each, as docs/data/{CODE}/revisions.json.

Source is the site's own change register at /Explore/Changes. Each entry is a
"change package": one dated publication that bumps one or more manuals, e.g.

    Establish East Region Virtual Value Network Pilot + CDRL
    Published on: 9/18/2026
    AD25 Change 2 · TST5 Change 40 · TOT5 Change 66
    Conreq: 23891

so the register is the only place the date and reason behind a change number
can be found. The manual's own publication page states its current revision
and nothing about the ones before it.

The register is paginated — 12 cards a page over roughly 29 pages — and its
controls are htmx, not links: each page number carries

    hx-get="/api/changepackages/cards?page=N&pageSize=12&filter=<json>"

with href="#". A first attempt followed the href and so re-loaded page one,
publishing six weeks of history as though it were the whole six years. That
endpoint is read directly instead: one request per page of any size, with the
filter the page itself builds (it names all fourteen publications), so no
query parameter is guessed and nothing depends on the UI's own paging.

The request is made by the register page, through its own fetch(). Lifting
the browser's cookies into a requests session worked at first and then
started being reset outright by the bot defence, which went on to reset the
navigation too. A same-origin fetch from the open page is the request htmx
itself would make, with the session, headers and TLS fingerprint the site has
already accepted.

Two honesty requirements shape the output:

  * The register begins in December 2020. Manuals older than that have change
    numbers predating it (TO15 and TP15 are on 166), so a revision list is
    necessarily partial. Each file records the window the register covers and
    how many changes are unaccounted for, rather than implying the list is
    complete.
  * A change number is only ever reported with the date the site gives it. No
    interpolating, no guessing dates for changes the register does not mention.
"""
import asyncio
import json
import re
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

from bs4 import BeautifulSoup
from playwright.async_api import async_playwright

REPO_ROOT = Path(__file__).parent.parent.resolve()
DATA_DIR = REPO_ROOT / "docs" / "data"
MANUALS_JSON = DATA_DIR / "manuals.json"

BASE_URL = "https://manuals.dha.mil"
CHANGES_URL = BASE_URL + "/Explore/Changes"

# Cards per API request. The UI offers 12/24/48; the endpoint takes whatever
# it is given, so ask for a size that pulls the whole register in a few calls
# and let the loop handle a server-side cap by reading what actually comes back.
PAGE_SIZE = 100
# Hard ceiling on requests, so a paging bug cannot loop forever.
MAX_PAGES = 40
PAGE_DELAY_SECS = 1.5
# The site's bot defence resets a request outright when it dislikes it —
# "Connection reset by peer", not an HTTP status — and a single one of those
# on page 0 used to end the whole read. Each page is retried, and from the
# second failure the register is reloaded first, which also refreshes the
# filter. Backoff grows with each attempt and the whole step is capped at ten
# minutes in CI, so a site that is resetting everything costs one run's
# refresh, not a stuck job: the files already published simply stay.
PAGE_ATTEMPTS = 4
RENEW_AFTER = 2
PAGE_RETRY_BACKOFF_SECS = 20.0

CARDS_API = BASE_URL + "/api/changepackages/cards"
HX_TARGET = "change-packages-wrapper"

_PUBLISHED_RE = re.compile(r"Published on:\s*([0-9]{1,2}/[0-9]{1,2}/[0-9]{4})")
_CODE_CHANGE_RE = re.compile(r"^\s*([A-Z][A-Z0-9]{3})\s+Change\s+(\d+)\s*$", re.I)
_CONREQ_RE = re.compile(r"Conreq:\s*(\S+)")
_HXGET_RE = re.compile(r"/api/changepackages/cards\?([^\"\']+)")

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/124.0.0.0 Safari/537.36"
)


def iso_date(american: str) -> str:
    """'9/18/2026' -> '2026-09-18'; returns '' if it cannot be parsed."""
    try:
        return datetime.strptime(american, "%m/%d/%Y").strftime("%Y-%m-%d")
    except ValueError:
        return ""


def parse_cards(html: str) -> list[dict]:
    """Parse change-package cards out of a register page or API fragment."""
    soup = BeautifulSoup(html, "lxml")
    entries = []
    for card in soup.select(".changepackage-card"):
        text = card.get_text("\n", strip=True).replace("\u00a0", " ")

        link = card.select_one('a[href*="/View-Change/"]')
        title = " ".join(link.get_text(" ", strip=True).split()) if link else ""
        pkg = ""
        if link and link.get("href"):
            mp = re.search(r"/View-Change/(\d+)", link["href"])
            if mp:
                pkg = mp.group(1)

        m = _PUBLISHED_RE.search(text)
        published = iso_date(m.group(1)) if m else ""
        mc = _CONREQ_RE.search(text)
        conreq = mc.group(1) if mc else ""

        bumps = []
        for li in card.select("li"):
            mi = _CODE_CHANGE_RE.match(li.get_text(" ", strip=True))
            if mi:
                bumps.append((mi.group(1).upper(), int(mi.group(2))))

        if bumps:
            entries.append({
                "title": title,
                "published": published,
                "conreq": conreq,
                "package": pkg,
                "bumps": bumps,
            })
    return entries


async def _open_register_page(page) -> str:
    """Navigate to the register and lift the filter its own paging links carry.

    The filter is a JSON blob naming every publication. It is taken verbatim
    from a pagination link rather than reconstructed, so the request matches
    what the site asks for and no query parameter is invented.
    """
    await page.goto(CHANGES_URL, wait_until="networkidle", timeout=60_000)
    await page.wait_for_timeout(4_000)
    hx = await page.evaluate("""() => {
        const a = document.querySelector('.pagination a.page-link[hx-get]');
        return a ? a.getAttribute('hx-get') : '';
    }""")
    m = _HXGET_RE.search(hx or "")
    return m.group(1) if m else ""


_FETCH_JS = """async ([url, target]) => {
    const r = await fetch(url, {
        headers: {'HX-Request': 'true', 'HX-Target': target},
        credentials: 'same-origin',
    });
    return [r.status, await r.text()];
}"""


async def _fetch_cards(page, url: str) -> tuple[int, str]:
    """Request a page of cards from inside the register page itself.

    Carrying the browser's cookies into a requests session stopped working:
    the bot defence resets those calls outright ("Connection reset by peer"),
    and after enough of them it resets the navigation too. Asking the page to
    fetch its own API is the same-origin request htmx would make, with the
    session, headers and TLS fingerprint the site already accepted.
    """
    status, text = await page.evaluate(_FETCH_JS, [url, HX_TARGET])
    return int(status), text


def _api_url(query: str, page: int) -> str:
    """Rewrite the captured query for a given page and our page size."""
    parts = []
    for kv in query.split("&"):
        if not kv:
            continue
        k = kv.split("=", 1)[0]
        if k in ("page", "pageSize"):
            continue
        parts.append(kv)
    parts.insert(0, f"page={page}")
    parts.insert(1, f"pageSize={PAGE_SIZE}")
    return CARDS_API + "?" + "&".join(parts)


async def collect_pages(fetch_cards, renew) -> tuple[list[dict], bool]:
    """Page the register. Returns (entries, truncated).

    fetch_cards(query, page_no) -> (status, html), and may raise;
    renew() -> a fresh filter query, or '' if the session could not be renewed.

    truncated means the read stopped somewhere other than the end of the
    register, so what came back is its newest slice rather than all of it.
    """
    query = await renew()
    if not query:
        # Without the page's own filter the endpoint returns nothing useful,
        # and guessing one risks silently narrowing the register.
        print("  could not read the register's filter from a pagination link",
              file=sys.stderr)
        return [], True
    print(f"  filter captured ({len(query)} chars)")

    all_entries: list[dict] = []
    seen: set[str] = set()
    truncated = True

    for page_no in range(0, MAX_PAGES):
        html = None
        for attempt in range(1, PAGE_ATTEMPTS + 1):
            try:
                status, body = await fetch_cards(query, page_no)
                if status == 200:
                    html = body
                    break
                reason = f"HTTP {status}"
            except Exception as e:
                reason = f"{type(e).__name__}: {e}"
            print(f"  page {page_no}: {reason} "
                  f"(attempt {attempt}/{PAGE_ATTEMPTS})", file=sys.stderr)
            if attempt == PAGE_ATTEMPTS:
                break
            await asyncio.sleep(PAGE_RETRY_BACKOFF_SECS * attempt)
            if attempt >= RENEW_AFTER:
                # Reload the register so the session, and the filter it
                # carries, are the site's current ones.
                print("  reloading the register…", file=sys.stderr)
                fresh = await renew()
                if fresh:
                    query = fresh
        if html is None:
            print(f"  page {page_no}: gave up after {PAGE_ATTEMPTS} attempts",
                  file=sys.stderr)
            break

        batch = parse_cards(html)
        fresh_cards = [e for e in batch
                       if not e["package"] or e["package"] not in seen]
        for e in fresh_cards:
            if e["package"]:
                seen.add(e["package"])
        all_entries.extend(fresh_cards)
        print(f"  page {page_no}: {len(batch)} card(s), {len(fresh_cards)} new "
              f"({len(all_entries)} total)")

        if not batch:
            truncated = False          # ran off the end of the register
            break
        if len(batch) < PAGE_SIZE:
            truncated = False          # a short page is the last page
            break
        if not fresh_cards:
            # A full page that adds nothing new means the endpoint is ignoring
            # our page number. Stop, and do not call the result complete.
            print(f"  page {page_no}: all duplicates — the endpoint is not paging",
                  file=sys.stderr)
            break
        await asyncio.sleep(PAGE_DELAY_SECS)
    else:
        print(f"  stopped at the {MAX_PAGES}-request ceiling", file=sys.stderr)

    return all_entries, truncated


async def read_register() -> tuple[list[dict], bool]:
    """Open the register in a browser and page its API from inside the page."""
    async with async_playwright() as pw:
        browser = await pw.chromium.launch(
            headless=True, args=["--disable-blink-features=AutomationControlled"]
        )
        ctx = await browser.new_context(
            user_agent=USER_AGENT,
            viewport={"width": 1440, "height": 1000},
            extra_http_headers={"Accept-Language": "en-US,en;q=0.9"},
        )
        await ctx.add_init_script(
            "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
        )
        page = await ctx.new_page()

        async def renew() -> str:
            # A reset navigation must not end the run: the retry loop above
            # treats an empty filter as one more failed attempt.
            try:
                return await _open_register_page(page)
            except Exception as e:
                print(f"  could not reload the register: {type(e).__name__}: {e}",
                      file=sys.stderr)
                return ""

        async def fetch_cards(query: str, page_no: int):
            return await _fetch_cards(page, _api_url(query, page_no))

        try:
            return await collect_pages(fetch_cards, renew)
        finally:
            try:
                await page.close()
            except Exception:
                pass
            await ctx.close()
            await browser.close()


def fetch_register() -> tuple[list[dict], bool]:
    return asyncio.run(read_register())


def build_per_manual(entries: list[dict]) -> tuple[dict[str, list[dict]], str, str]:
    """Group register entries by manual code, newest change first."""
    by_code: dict[str, dict[int, dict]] = {}
    dates = [e["published"] for e in entries if e["published"]]

    for e in entries:
        for code, change in e["bumps"]:
            # A change number should appear once; if the register lists it
            # twice, keep the earlier publication date.
            existing = by_code.setdefault(code, {}).get(change)
            if existing and existing["published"] and e["published"]:
                if existing["published"] <= e["published"]:
                    continue
            by_code[code][change] = {
                "change": change,
                "published": e["published"],
                "title": e["title"],
                "conreq": e["conreq"],
                "package": e["package"],
            }

    out = {code: [revs[c] for c in sorted(revs, reverse=True)]
           for code, revs in by_code.items()}
    return out, (min(dates) if dates else ""), (max(dates) if dates else "")


def write_revisions(code: str, revisions: list[dict],
                    current: int | None, covered_from: str, covered_to: str) -> None:
    out_dir = DATA_DIR / code
    out_dir.mkdir(parents=True, exist_ok=True)

    listed = [r["change"] for r in revisions]
    # How much of the manual's history the register actually accounts for. The
    # register starts in Dec 2020, so a manual on change 166 has most of its
    # history outside it — say so rather than implying the list is complete.
    highest = max(listed) if listed else (current or 0)
    missing = max(0, (current or highest) - len(listed)) if (current or highest) else 0

    payload = {
        "code": code,
        "fetchedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "current": current,
        "register": {
            "source": CHANGES_URL,
            "coversFrom": covered_from,
            "coversTo": covered_to,
            "listed": len(listed),
            "unlisted": missing,
            "complete": missing == 0,
        },
        "revisions": revisions,
    }
    path = out_dir / "revisions.json"
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n",
                    encoding="utf-8")
    print(f"  {code}: {len(listed)} change(s) listed"
          + (f", {missing} before the register begins" if missing else "")
          + f" -> {path.relative_to(REPO_ROOT)}")


def main() -> None:
    if not MANUALS_JSON.exists():
        print(f"{MANUALS_JSON} not found", file=sys.stderr)
        sys.exit(1)
    manuals = json.loads(MANUALS_JSON.read_text(encoding="utf-8"))["manuals"]
    current_by_code = {m["code"]: m.get("latestChange") for m in manuals}

    print("Reading the change register…")
    entries, truncated = fetch_register()
    print(f"\n{len(entries)} change package(s) read.")

    if not entries or truncated:
        # Either the register could not be read at all, or only its newest
        # pages came back. Both would publish a slice of the history as though
        # it were the whole of it, which is the specific way this feature
        # misleads people — it already published six weeks as six years once.
        # The files already on disk stay exactly as they are.
        print("Register read " + ("returned nothing" if not entries
                                  else f"stopped early with {len(entries)} entries")
              + " — leaving existing revisions.json files alone.", file=sys.stderr)
        sys.exit(1)

    per_manual, covered_from, covered_to = build_per_manual(entries)
    print(f"Register covers {covered_from} to {covered_to}.\n")

    for code in current_by_code:
        write_revisions(code, per_manual.get(code, []),
                        current_by_code[code], covered_from, covered_to)

    extra = sorted(set(per_manual) - set(current_by_code))
    if extra:
        print(f"\nAlso present in the register but not tracked here: "
              f"{', '.join(extra)}")

    print("\nDone.")


if __name__ == "__main__":
    main()
