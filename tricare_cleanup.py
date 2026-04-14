#!/usr/bin/env python3
"""
tricare_cleanup.py — Post-process scraped TRICARE markdown files

Applies all 9 cleanup steps identified by Claude's document review:

  1. Remove duplicate TOC sections
  2. Strip per-section footer (tricare.mil disclaimer → DHA Address)
  3. Strip '- END -' marker and DHA Logo
  4. Strip per-section navigation header (Military Health System nav block)
  5. Normalize section headers  (Issue 5 + 6)
  6. Insert line breaks in wall-of-text content
  7. Update master TOC entries
  8. Remove excess dividers

Usage:
    python tricare_cleanup.py <input.md> [<input2.md> ...]

    Or process all four known manuals in the current directory:
    python tricare_cleanup.py --all

Output files get a _clean suffix:
    TOT5_change54.md  →  TOT5_change54_clean.md
"""

import re
import sys
from pathlib import Path

# ── Manual metadata ───────────────────────────────────────────────────────────

ABBREVS = {
    "TOT5": "TOM",
    "TPT5": "TPM",
    "TRT5": "TRM",
    "TST5": "TSM",
}


def abbrev_for_file(path: Path) -> str:
    stem = path.stem.upper()
    for code, abbr in ABBREVS.items():
        if stem.startswith(code):
            return abbr
    return "TOM"


# ── Step 1: Remove duplicate consecutive TOC sections ────────────────────────

_TOC_HEADER_RE = re.compile(
    r"^## TRICARE Manuals - Display Chap \d+ TOC.*$",
    re.MULTILINE | re.IGNORECASE,
)


def _section_blocks(text: str) -> list[tuple[int, int, str]]:
    """Return list of (start, end, header_text) for every ## section."""
    positions = [(m.start(), m.group()) for m in re.finditer(r"^## .+$", text, re.MULTILINE)]
    blocks = []
    for i, (start, header) in enumerate(positions):
        end = positions[i + 1][0] if i + 1 < len(positions) else len(text)
        blocks.append((start, end, header))
    return blocks


def remove_duplicate_toc_sections(text: str) -> str:
    blocks = _section_blocks(text)
    to_remove: list[tuple[int, int]] = []
    seen_toc_headers: set[str] = set()

    for start, end, header in blocks:
        # Only deduplicate chapter TOC headers
        if not _TOC_HEADER_RE.match(header):
            continue
        norm = re.sub(r"\s+", " ", header.strip())
        if norm in seen_toc_headers:
            to_remove.append((start, end))
        else:
            seen_toc_headers.add(norm)

    if not to_remove:
        return text

    # Remove from the end to avoid offsetting earlier positions
    result = text
    for start, end in sorted(to_remove, reverse=True):
        result = result[:start] + result[end:]
    return result


# ── Step 2: Strip per-section footer ─────────────────────────────────────────

_FOOTER_RE = re.compile(
    r"- tricare\.mil is the official website.*?DHA Address:.*?---\s*",
    re.DOTALL | re.IGNORECASE,
)
# Shorter variant without DHA Address line
_FOOTER_SHORT_RE = re.compile(
    r"- tricare\.mil is the official website.*?(?=\n## |\Z)",
    re.DOTALL | re.IGNORECASE,
)


def strip_section_footer(text: str) -> str:
    text = _FOOTER_RE.sub("", text)
    text = _FOOTER_SHORT_RE.sub("", text)
    return text


# ── Step 3: Strip '- END -' and DHA Logo ─────────────────────────────────────

_END_MARKER_RE = re.compile(
    r"- END -!\[DHA Logo\][^\n]*",
    re.IGNORECASE,
)
_DHA_LOGO_STANDALONE_RE = re.compile(
    r"^!\[DHA Logo\][^\n]*\n?",
    re.MULTILINE | re.IGNORECASE,
)


def strip_end_marker(text: str) -> str:
    text = _END_MARKER_RE.sub("", text)
    text = _DHA_LOGO_STANDALONE_RE.sub("", text)
    return text


# ── Step 4: Strip per-section navigation header ───────────────────────────────

_NAV_HEADER_RE = re.compile(
    r"## Military Health System\n.*?(?=\nTRICARE (?:Operations|Policy|Reimbursement|Systems) Manual)",
    re.DOTALL | re.IGNORECASE,
)
# Alternative: strip from the duplicated nav list pattern
_NAV_LIST_RE = re.compile(
    r"\[TRICARE Manuals Home.*?### Main Navigation.*?(?=\n[A-Z]|\n##)",
    re.DOTALL,
)
# The "- DHA Home … - TRICARE" duplicate nav list that appears in every section
_NAV_DUP_RE = re.compile(
    r"(?:- DHA Home\n- Manuals.*?- TRICARE\n){1,}",
    re.DOTALL,
)


def strip_nav_header(text: str) -> str:
    text = _NAV_HEADER_RE.sub("", text)
    text = _NAV_LIST_RE.sub("", text)
    text = _NAV_DUP_RE.sub("", text)
    return text


# ── Steps 5 + 6: Normalize section headers and parse content first-lines ──────

# Maps the verbose web header to a clean format
# Input:  ## TRICARE Manuals - Display Chap 1 Sect 2 (Change 54, Mar 27, 2026)
# Output: ## TOM Ch.1 §2 — <title from content>

_SECTION_HEADER_RE = re.compile(
    r"^## TRICARE Manuals - Display (.+?)$",
    re.MULTILINE,
)
_DISPLAY_CHAP_SECT_RE = re.compile(
    r"Chap (\d+) Sect ([\d.]+)\s*\((.+?)\)",
    re.IGNORECASE,
)
_DISPLAY_CHAP_TOC_RE = re.compile(
    r"Chap (\d+) TOC\s*\((.+?)\)",
    re.IGNORECASE,
)
_DISPLAY_ADDENDUM_RE = re.compile(
    r"Chap (\d+) Add(?:endum)? ?([A-Z0-9]+)\s*\((.+?)\)",
    re.IGNORECASE,
)
_DISPLAY_FOREWORD_RE = re.compile(
    r"(Foreword|Preface|Introduction|Intro)\s*\((.+?)\)",
    re.IGNORECASE,
)

# First content line patterns for extracting the section title and metadata
_CONTENT_FIRST_LINE_RE = re.compile(
    r"^TRICARE (?:Operations|Policy|Reimbursement|Systems) Manual [^,]+,\s*\w+ \d{4}"
    r"(?:[^\n]*?Chapter \d+)?(?:[^\n]*?Section ([\d.]+))?([^\n]*?)(?:Issue Date:|Revision:|Authority:|$)",
    re.IGNORECASE,
)


def _extract_title_from_content(content_after_header: str) -> str:
    """Pull the section title from the first content paragraph."""
    lines = content_after_header.strip().splitlines()
    for line in lines[:5]:
        line = line.strip()
        if not line:
            continue
        # Skip master TOC entries like "1. Something"
        if re.match(r"^\d+\.", line):
            continue
        # Strip leading manual boilerplate
        clean = re.sub(
            r"^TRICARE (?:Operations|Policy|Reimbursement|Systems) Manual[^,]+,\s*\w+ \d{4}",
            "", line, flags=re.IGNORECASE
        ).strip()
        clean = re.sub(r"^(?:Administration|Medicine|Claims|Finance|Systems)[^C]*", "", clean).strip()
        clean = re.sub(r"^Chapter \d+", "", clean, flags=re.IGNORECASE).strip()
        clean = re.sub(r"^Section [\d.]+", "", clean, flags=re.IGNORECASE).strip()
        # Extract title up to metadata keywords
        m = re.match(r"^(.+?)(?:\s+Issue Date:|\s+Revision:|\s+Authority:|\s*$)", clean)
        if m:
            title = m.group(1).strip()
            if len(title) > 3:
                return title
    return ""


def normalize_section_headers(text: str, abbr: str) -> str:
    """Rewrite verbose TRICARE web display headers to short, clean format."""

    def replace_header(m: Match) -> str:
        descriptor = m.group(1).strip()

        # Foreword / Preface
        fw = _DISPLAY_FOREWORD_RE.match(descriptor)
        if fw:
            return f"## {abbr} Foreword"

        # Addendum
        ad = _DISPLAY_ADDENDUM_RE.match(descriptor)
        if ad:
            ch, add_letter, change_info = ad.group(1), ad.group(2), ad.group(3)
            # Look for title in following content
            pos = m.end()
            snippet = text[pos:pos + 400]
            title = _extract_title_from_content(snippet)
            title_part = f" — {title}" if title else ""
            return f"## {abbr} Ch.{ch} Addendum {add_letter}{title_part}"

        # Chapter TOC
        toc = _DISPLAY_CHAP_TOC_RE.match(descriptor)
        if toc:
            ch = toc.group(1)
            return f"## {abbr} Ch.{ch} — Table of Contents"

        # Chapter + Section
        cs = _DISPLAY_CHAP_SECT_RE.match(descriptor)
        if cs:
            ch, sect, change_info = cs.group(1), cs.group(2), cs.group(3)
            pos = m.end()
            snippet = text[pos:pos + 400]
            title = _extract_title_from_content(snippet)
            title_part = f" — {title}" if title else ""
            return f"## {abbr} Ch.{ch} §{sect}{title_part}"

        # Fallback: keep the descriptor but add abbr prefix
        return f"## {abbr} — {descriptor}"

    # Can't use re.sub with back-references to surrounding text, so do it manually
    result_parts = []
    last_end = 0
    for m in _SECTION_HEADER_RE.finditer(text):
        result_parts.append(text[last_end:m.start()])
        result_parts.append(replace_header(m))
        last_end = m.end()
    result_parts.append(text[last_end:])
    return "".join(result_parts)


# Fix type hint for the inner function
from typing import Match


# ── Step 6: Insert line breaks in wall-of-text content ───────────────────────

# Match numbered subsection starts: "2.1 Title" or "3.4.2 Title" preceded by text
_SUBSECTION_RE = re.compile(r"(?<=\S)(\s)(\d+\.\d+(?:\.\d+)*\s+[A-Z])")
# Note: / Example: blocks
_NOTE_RE = re.compile(r"(?<=\S)\s+(Note:|Example:|NOTE:|EXAMPLE:)")
# Bullet • preceded by non-whitespace
_BULLET_RE = re.compile(r"(?<=\S)(•)")


def insert_line_breaks(text: str) -> str:
    lines = []
    for line in text.splitlines():
        if line.strip().startswith("|"):
            lines.append(line)
            continue
        line = _SUBSECTION_RE.sub(r"\n\n\2", line)
        line = _NOTE_RE.sub(r"\n\n\1", line)
        line = _BULLET_RE.sub(r"\n•", line)
        lines.append(line)
    return "\n".join(lines)


# ── Step 7: Update master TOC ─────────────────────────────────────────────────

def update_master_toc(text: str) -> str:
    """
    The master TOC is the numbered list at the very top of the file.
    After step 5 renames the section headers, rebuild the TOC from
    the new ## headers so it stays in sync.
    """
    # Find the master TOC block: lines 1 → first "---"
    lines = text.splitlines()
    toc_end = 0
    content_start = 0
    in_toc = False
    for i, line in enumerate(lines):
        if line.startswith("## Table of Contents"):
            in_toc = True
            toc_end = i
            continue
        if in_toc and line == "---":
            content_start = i + 1
            break

    if not in_toc:
        return text  # No master TOC found

    # Collect all ## headers after the master TOC
    section_headers = [
        line[3:].strip()
        for line in lines[content_start:]
        if line.startswith("## ")
    ]

    # Rebuild TOC
    new_toc_lines = ["## Table of Contents", ""]
    for i, h in enumerate(section_headers, 1):
        new_toc_lines.append(f"{i}. {h}")
    new_toc_lines.append("")
    new_toc_lines.append("---")

    result_lines = lines[:toc_end] + new_toc_lines + lines[content_start:]
    return "\n".join(result_lines)


# ── Step 8: Remove excess dividers ───────────────────────────────────────────

_MULTI_HR_RE = re.compile(r"(\n---\s*){2,}")
_HR_BEFORE_HEADER_RE = re.compile(r"---\s*\n(## )")
_EXCESS_BLANK_RE = re.compile(r"\n{4,}")


def clean_dividers(text: str) -> str:
    text = _MULTI_HR_RE.sub("\n---\n", text)
    text = _HR_BEFORE_HEADER_RE.sub(r"\1", text)
    text = _EXCESS_BLANK_RE.sub("\n\n\n", text)
    return text


# ── Strip content first-line boilerplate ─────────────────────────────────────

_CONTENT_BOILERPLATE_RE = re.compile(
    r"^TRICARE (?:Operations|Policy|Reimbursement|Systems) Manual [^\n]+\n?",
    re.MULTILINE | re.IGNORECASE,
)


def strip_content_first_line_boilerplate(text: str) -> str:
    return _CONTENT_BOILERPLATE_RE.sub("", text)


# ── Orchestration ─────────────────────────────────────────────────────────────

def clean_file(input_path: Path, output_path: Path) -> None:
    print(f"\n{'='*60}")
    print(f"Input  : {input_path}")
    print(f"Output : {output_path}")
    print(f"{'='*60}")

    text = input_path.read_text(encoding="utf-8")
    original_lines = text.count("\n")
    print(f"Lines before: {original_lines:,}")

    abbr = abbrev_for_file(input_path)

    print("  Step 1: Removing duplicate TOC sections…")
    text = remove_duplicate_toc_sections(text)

    print("  Step 2: Stripping per-section footers…")
    text = strip_section_footer(text)

    print("  Step 3: Stripping '- END -' and DHA Logo…")
    text = strip_end_marker(text)

    print("  Step 4: Stripping navigation headers…")
    text = strip_nav_header(text)

    print("  Step 5+6: Normalizing section headers…")
    text = normalize_section_headers(text, abbr)

    print("  Step 6: Stripping content first-line boilerplate…")
    text = strip_content_first_line_boilerplate(text)

    print("  Step 6: Inserting line breaks in wall-of-text…")
    text = insert_line_breaks(text)

    print("  Step 7: Updating master TOC…")
    text = update_master_toc(text)

    print("  Step 8: Cleaning up excess dividers…")
    text = clean_dividers(text)

    output_path.write_text(text.strip() + "\n", encoding="utf-8")

    final_lines = text.count("\n")
    reduction = (1 - final_lines / original_lines) * 100 if original_lines > 0 else 0
    size_kb = output_path.stat().st_size // 1024
    print(f"Lines after : {final_lines:,}  ({reduction:.0f}% reduction)")
    print(f"Output size : {size_kb:,} KB")


def _validate(output_path: Path) -> None:
    """Quick sanity checks on the cleaned file."""
    text = output_path.read_text(encoding="utf-8")
    checks = [
        ("Main Navigation",              "Navigation header remnants"),
        ("tricare.mil is the official",  "Footer remnants"),
        ("DHA Address:",                 "DHA Address remnants"),
        ("Privacy Policy",               "Privacy Policy remnants"),
        ("DHA Logo",                     "DHA Logo remnants"),
        ("- END -",                      "'- END -' remnants"),
        ("Military Health System",       "MHS nav header remnants"),
    ]
    all_ok = True
    for pattern, description in checks:
        count = text.count(pattern)
        if count > 0:
            print(f"  ⚠  {description}: {count} occurrence(s) of '{pattern}'")
            all_ok = False
    if all_ok:
        print("  ✅ All validation checks passed")


# ── Entry point ───────────────────────────────────────────────────────────────

def main() -> None:
    args = sys.argv[1:]

    if not args or args == ["--help"]:
        print(__doc__)
        return

    if args == ["--all"]:
        # Find all four manuals in the current directory
        files = []
        for f in Path(".").glob("*.md"):
            for code in ABBREVS:
                if f.stem.upper().startswith(code) and "_clean" not in f.stem:
                    files.append(f)
                    break
        if not files:
            print("No TRICARE manual .md files found in the current directory.")
            print("Run from the Downloads folder or pass file paths explicitly.")
            return
    else:
        files = [Path(a) for a in args]

    for f in files:
        if not f.exists():
            print(f"File not found: {f}")
            continue
        output = f.parent / (f.stem + "_clean" + f.suffix)
        clean_file(f, output)
        print("  Validating…")
        _validate(output)

    print("\nDone.")


if __name__ == "__main__":
    main()
