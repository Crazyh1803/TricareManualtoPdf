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

The register is paginated — 12 entries per page over roughly 29 pages — and
nothing lazy-loads, so every page has to be visited. Reading only what renders
first would publish six weeks of history as though it were the whole six
years.

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
from datetime import datetime, timezone
from pathlib import Path

from playwright.async_api import async_playwright

REPO_ROOT = Path(__file__).parent.parent.resolve()
DATA_DIR = REPO_ROOT / "docs" / "data"
MANUALS_JSON = DATA_DIR / "manuals.json"

BASE_URL = "https://manuals.dha.mil"
CHANGES_URL = BASE_URL + "/Explore/Changes"

# Largest page size the register offers, to keep the number of page loads down.
PAGE_SIZE = 48
# Hard ceiling on pages followed, so a pagination bug cannot loop forever.
MAX_PAGES = 60
PAGE_SETTLE_MS = 3_000
PAGE_DELAY_SECS = 1.5

_PUBLISHED_RE = re.compile(r"Published on:\s*([0-9]{1,2}/[0-9]{1,2}/[0-9]{4})")
_CODE_CHANGE_RE = re.compile(r"^\s*([A-Z][A-Z0-9]{3})\s+Change\s+(\d+)\s*$", re.I)
_CONREQ_RE = re.compile(r"Conreq:\s*(\S+)")


def iso_date(american: str) -> str:
    """'9/18/2026' -> '2026-09-18'; returns '' if it cannot be parsed."""
    try:
        return datetime.strptime(american, "%m/%d/%Y").strftime("%Y-%m-%d")
    except ValueError:
        return ""


async def _launch(pw):
    browser = await pw.chromium.launch(
        headless=True, args=["--disable-blink-features=AutomationControlled"]
    )
    ctx = await browser.new_context(
        user_agent=(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/124.0.0.0 Safari/537.36"
        ),
        viewport={"width": 1440, "height": 1000},
        extra_http_headers={"Accept-Language": "en-US,en;q=0.9"},
    )
    await ctx.add_init_script(
        "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
    )
    return browser, ctx


async def _entries_on_page(page) -> list[dict]:
    """Parse the change-package cards rendered on the current page."""
    raw = await page.evaluate("""() => {
        const out = [];
        for (const card of document.querySelectorAll('.changepackage-card')) {
            const link = card.querySelector('a[href*="/View-Change/"]');
            out.push({
                title: link ? (link.innerText || '').trim() : '',
                href: link ? link.getAttribute('href') : '',
                // Whole-card text carries the "Published on:" and "Conreq:" lines.
                text: (card.innerText || '').replace(/\\u00a0/g, ' '),
                items: Array.from(card.querySelectorAll('li'))
                            .map(li => (li.innerText || '').trim()),
            });
        }
        return out;
    }""")

    entries = []
    for r in raw:
        m = _PUBLISHED_RE.search(r["text"])
        published = iso_date(m.group(1)) if m else ""
        conreq = ""
        mc = _CONREQ_RE.search(r["text"])
        if mc:
            conreq = mc.group(1)
        pkg = ""
        if r["href"]:
            mp = re.search(r"/View-Change/(\d+)", r["href"])
            if mp:
                pkg = mp.group(1)

        bumps = []
        for item in r["items"]:
            mi = _CODE_CHANGE_RE.match(item)
            if mi:
                bumps.append((mi.group(1).upper(), int(mi.group(2))))
        if bumps:
            entries.append({
                "title": " ".join(r["title"].split()),
                "published": published,
                "conreq": conreq,
                "package": pkg,
                "bumps": bumps,
            })
    return entries


async def fetch_register() -> list[dict]:
    """Walk every page of the change register and return all entries."""
    all_entries: list[dict] = []
    seen_pkgs: set[str] = set()

    async with async_playwright() as pw:
        browser, ctx = await _launch(pw)
        page = await ctx.new_page()
        try:
            url = f"{CHANGES_URL}?PageSize={PAGE_SIZE}"
            await page.goto(url, wait_until="networkidle", timeout=60_000)
            await page.wait_for_timeout(PAGE_SETTLE_MS)

            for page_no in range(1, MAX_PAGES + 1):
                batch = await _entries_on_page(page)
                fresh = [e for e in batch
                         if not e["package"] or e["package"] not in seen_pkgs]
                for e in fresh:
                    if e["package"]:
                        seen_pkgs.add(e["package"])
                all_entries.extend(fresh)
                print(f"  page {page_no}: {len(batch)} card(s), {len(fresh)} new "
                      f"({len(all_entries)} total)")

                # Follow the pagination link whose text is the next page number.
                # Reading the control rather than guessing a query parameter
                # keeps this working if the site renames it.
                nxt = await page.evaluate("""(n) => {
                    const links = Array.from(
                        document.querySelectorAll('.pagination a.page-link'));
                    const want = String(n + 1);
                    const a = links.find(x => (x.innerText || '').trim() === want);
                    return a ? a.getAttribute('href') : null;
                }""", page_no)
                if not nxt:
                    print(f"  no link to page {page_no + 1}; register ends here")
                    break

                await asyncio.sleep(PAGE_DELAY_SECS)
                target = nxt if nxt.startswith("http") else BASE_URL + nxt
                await page.goto(target, wait_until="networkidle", timeout=60_000)
                await page.wait_for_timeout(PAGE_SETTLE_MS)
            else:
                print(f"  stopped at the {MAX_PAGES}-page ceiling", file=sys.stderr)
        finally:
            try:
                await page.close()
            except Exception:
                pass
            await ctx.close()
            await browser.close()

    return all_entries


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
    entries = asyncio.run(fetch_register())
    print(f"\n{len(entries)} change package(s) read.")

    if not entries:
        # An empty register means the page moved or we were blocked. Writing
        # empty revision lists would replace a real history with nothing.
        print("No entries parsed — leaving existing revisions.json files alone.",
              file=sys.stderr)
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
