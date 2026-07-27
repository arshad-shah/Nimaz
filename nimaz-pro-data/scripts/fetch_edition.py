#!/usr/bin/env python3
"""
Nimaz — manifest-driven Quran edition fetcher.

Reads `nimaz-pro-data/manifest.json` (the pipeline mirror of the app's content registry) and
fetches, validates and writes one edition's data files by id:

    python3 fetch_edition.py indopak16                  # fetch, validate, write
    python3 fetch_edition.py indopak16 --validate-only  # fetch + validate, write nothing
    python3 fetch_edition.py indopak16 --pages 1 3      # dev: fetch a few pages, no write
    python3 fetch_edition.py --list                     # what the manifest declares

## Why this exists
`download_indopak_mushaf_data.py` hardcoded resource 11, 548 pages, 16 lines and two output
filenames, so a second line-accurate edition meant a forked copy of the script — and with it a
forked copy of the validators, which are the part that actually matters. This driver keeps
**one** copy of every validator and parameterises the edition, so the same fidelity checks run
against whatever layout is fetched.

It deliberately *imports* the original module rather than reimplementing it: the losslessness
check (`' '.join(words) == text_<source>`), the page/line count assertions and the 114-header /
112-basmalah invariants are the accumulated result of the #265 acceptance work, and rewriting
them is exactly how a subtle regression gets in. Edition-specific values are rebound on the
module before its builders run.

## --validate-only
The check that gates a new layout: the stored word positions index into an ayah text column, so
a layout is only usable if it tokenises against the *same* text the app already ships. Run this
against a candidate resource and read the alignment section before committing to the edition.
If alignment fails, that layout needs its own ayah text column — a schema migration, and the
one legitimate exception to "a new layout is data only". See docs/quran/content-registry.md.
"""

import argparse
import json
import sys
from pathlib import Path
from typing import Any, Dict

BASE_DIR = Path(__file__).parent.parent
MANIFEST_PATH = BASE_DIR / "manifest.json"

sys.path.insert(0, str(Path(__file__).parent))


def load_manifest() -> Dict[str, Any]:
    with open(MANIFEST_PATH, encoding="utf-8") as f:
        return json.load(f)


def find_edition(manifest: Dict[str, Any], edition_id: str):
    """Returns (axis, entry) for `edition_id`, or (None, None)."""
    for axis in ("mushafLayouts", "translations", "tafseers"):
        for entry in manifest.get(axis, []):
            if entry.get("id") == edition_id:
                return axis, entry
    return None, None


def list_editions(manifest: Dict[str, Any]) -> None:
    for axis in ("mushafLayouts", "translations", "tafseers"):
        entries = manifest.get(axis, [])
        if not entries:
            continue
        print(f"\n{axis}:")
        for e in entries:
            fetchable = " (fetchable)" if axis == "mushafLayouts" else " (not fetchable here)"
            print(f"  {e['id']:<24} {e.get('displayName', '')}{fetchable}")
    print()


def fetch_mushaf_layout(entry: Dict[str, Any], args: argparse.Namespace) -> int:
    """Fetch + validate (+ optionally write) one line-accurate layout."""
    import download_indopak_mushaf_data as pipeline

    # Rebind the module's edition-specific constants. The builders and validators read these
    # at call time, so every check below runs against this edition's expectations rather than
    # the 16-line IndoPak ones the module was written for.
    pipeline.MUSHAF_RESOURCE_URL = entry["resourceUrl"]
    pipeline.EXPECTED_PAGES = entry["expectedPages"]
    pipeline.MAX_LINES_PER_PAGE = entry["maxLines"]

    page_start, page_end = 1, entry["expectedPages"]
    dev = False
    if args.pages:
        page_start, page_end = args.pages
        dev = True

    print("=" * 68)
    print(f"Fetching mushaf layout: {entry['id']} — {entry.get('displayName', '')}")
    print(f"  source: {entry['resourceUrl']}")
    print(f"  expect: {entry['expectedPages']} pages, <= {entry['maxLines']} lines/page, "
          f"text source {entry['textSource']}")
    print(f"  pages : {page_start}..{page_end}")
    print("=" * 68)

    pages = pipeline.fetch_pages(page_start, page_end)
    ayah_rows, layout_rows = pipeline.build_from_pages(pages)
    print(f"  parsed: {len(ayah_rows)} ayahs, {len(layout_rows)} layout rows")

    if dev:
        print("  [dev mode] partial fetch — skipping validation and write. Sample:")
        print("   ", json.dumps(ayah_rows[:2], ensure_ascii=False))
        print("   ", json.dumps(layout_rows[:6], ensure_ascii=False))
        return 0

    existing_ayah_ids = pipeline.load_existing_ayah_ids()
    checks = [
        ("ayah text", pipeline.validate_ayahs_indopak(ayah_rows, existing_ayah_ids)),
        ("layout structure", pipeline.validate_mushaf_layout(layout_rows)),
        # The gate described in this module's docstring: word positions must exactly cover
        # each ayah's tokenisation, or the reader reconstructs the wrong glyphs.
        ("text/layout alignment", pipeline.validate_alignment(ayah_rows, layout_rows)),
    ]

    failed = False
    for name, errors in checks:
        if errors:
            failed = True
            print(f"\n[FAIL] {name}:")
            for e in errors[:10]:
                print(f"  - {e}")
        else:
            print(f"[OK]   {name}")

    if failed:
        print("\nNot writing any file. If alignment failed, this layout does not tokenise")
        print("against the shipped ayah text and needs its own text column — report the")
        print("result rather than silently adding one (docs/quran/content-registry.md).")
        return 1

    if args.validate_only:
        print("\n[OK] All checks passed. --validate-only: nothing written.")
        return 0

    pipeline.save_json(layout_rows, entry["output"]["layout"])
    if entry["output"].get("ayahText"):
        pipeline.save_json(ayah_rows, entry["output"]["ayahText"])

    print("\n[OK] Generated and validated.")
    print(f"     Next: copy the output(s) into app/src/main/assets/quran/, add the entry to")
    print(f"     QuranEditions.mushafLayouts + QuranContentAssets, and record the licence in")
    print(f"     nimaz-pro-data/json/{entry['license']}.")
    if entry.get("fidelitySheet"):
        print(f"     Fidelity sheet: {entry['fidelitySheet']}")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("edition_id", nargs="?", help="manifest id, e.g. indopak16")
    parser.add_argument("--list", action="store_true", help="list manifest entries and exit")
    parser.add_argument("--validate-only", action="store_true",
                        help="fetch and validate but write nothing")
    parser.add_argument("--pages", nargs=2, type=int, metavar=("START", "END"),
                        help="dev: fetch a page range only; implies no write")
    args = parser.parse_args()

    manifest = load_manifest()

    if args.list or not args.edition_id:
        list_editions(manifest)
        return 0 if args.list else 2

    axis, entry = find_edition(manifest, args.edition_id)
    if entry is None:
        print(f"error: no manifest entry with id {args.edition_id!r}", file=sys.stderr)
        list_editions(manifest)
        return 2

    if axis == "mushafLayouts":
        return fetch_mushaf_layout(entry, args)

    # Translations and tafseers currently ship inside the prepopulated DB rather than as
    # seeded assets, and are produced by download_and_generate.py. Say so plainly rather than
    # pretending to fetch — a silent no-op here would look like success.
    print(f"error: {args.edition_id!r} is a {axis[:-1]}, which this script does not fetch.",
          file=sys.stderr)
    print(f"       It is produced by scripts/download_and_generate.py "
          f"(output: {entry.get('output')}).", file=sys.stderr)
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
