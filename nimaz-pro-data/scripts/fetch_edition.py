#!/usr/bin/env python3
"""
Nimaz — manifest-driven Quran edition fetcher.

Reads `nimaz-pro-data/manifest.json` (the pipeline mirror of the app's content registry) and
fetches, validates and writes one edition's data files by id:

    python3 fetch_edition.py indopak16                  # a mushaf layout (from QUL)
    python3 fetch_edition.py pickthall                  # a translation (from Tanzil)
    python3 fetch_edition.py <id> --validate-only       # fetch + validate, write nothing
    python3 fetch_edition.py indopak16 --pages 1 3      # dev: fetch a few pages, no write
    python3 fetch_edition.py --all-translations         # every manifest translation
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
import re
import sys
import urllib.request
from pathlib import Path
from typing import Any, Dict, List, Tuple

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
            can_fetch = axis == "mushafLayouts" or e.get("source") == "tanzil"
            mark = " (fetchable)" if can_fetch else " (not fetchable here)"
            lang = f"[{e.get('languageTag', '--')}] " if axis == "translations" else ""
            print(f"  {e['id']:<24} {lang}{e.get('displayName', '')}{mark}")
    print()



# ---------------------------------------------------------------------------
# Translations (Tanzil)
# ---------------------------------------------------------------------------

TANZIL_TRANS_URL = "https://tanzil.net/trans/{edition}"

EXPECTED_AYAHS = 6236
EXPECTED_SURAHS = 114

# `surah|ayah|text`, one per line. Tanzil appends a `#`-prefixed metadata footer.
TANZIL_ROW = re.compile(r"^(\d+)\|(\d+)\|(.*)$")


def load_ayah_key_map() -> Dict[Tuple[int, int], int]:
    """(surah, ayah_in_surah) -> global ayah id, from the canonical ayahs.json."""
    with open(BASE_DIR / "json" / "ayahs.json", encoding="utf-8") as f:
        ayahs = json.load(f)
    return {(a["surah_id"], a["number_in_surah"]): a["id"] for a in ayahs}


def download_tanzil(edition: str) -> str:
    url = TANZIL_TRANS_URL.format(edition=edition)
    print(f"  GET {url}")
    req = urllib.request.Request(url, headers={"User-Agent": "nimaz-data-pipeline"})
    with urllib.request.urlopen(req, timeout=120) as resp:
        return resp.read().decode("utf-8")


def parse_tanzil_metadata(raw: str) -> Dict[str, str]:
    """The `#`-prefixed footer carries Name/Translator/Language/Last Update/Source.

    Captured because it is exactly what a licence block needs, and reading it off the file
    beats retyping it from a web page into LICENSES_TRANSLATIONS.md.
    """
    meta: Dict[str, str] = {}
    for line in raw.splitlines():
        line = line.strip()
        if not line.startswith("#"):
            continue
        body = line.lstrip("#").strip()
        if ":" in body:
            key, _, value = body.partition(":")
            key, value = key.strip(), value.strip()
            if key and value:
                meta[key] = value
    return meta


def parse_tanzil(raw: str, key_map: Dict[Tuple[int, int], int]) -> List[Tuple[int, str]]:
    """Returns [(global_ayah_id, text)], skipping the metadata footer."""
    rows: List[Tuple[int, str]] = []
    for line in raw.splitlines():
        m = TANZIL_ROW.match(line.strip())
        if not m:
            continue  # blank line or `#` footer
        surah, ayah, text = int(m.group(1)), int(m.group(2)), m.group(3).strip()
        key = (surah, ayah)
        if key not in key_map:
            raise ValueError(f"{surah}:{ayah} is not a valid ayah reference")
        rows.append((key_map[key], text))
    return rows


def validate_translation(rows: List[Tuple[int, str]]) -> List[str]:
    """Every invariant that would otherwise surface as a blank or shifted verse in the app."""
    errors: List[str] = []
    ids = [i for i, _ in rows]

    if len(rows) != EXPECTED_AYAHS:
        errors.append(f"expected {EXPECTED_AYAHS} rows, got {len(rows)}")

    unique = set(ids)
    if len(unique) != len(ids):
        dupes = sorted({i for i in ids if ids.count(i) > 1})[:5]
        errors.append(f"duplicate ayah ids (e.g. {dupes})")

    missing = set(range(1, EXPECTED_AYAHS + 1)) - unique
    if missing:
        errors.append(f"{len(missing)} ayah ids missing (e.g. {sorted(missing)[:5]})")

    extra = unique - set(range(1, EXPECTED_AYAHS + 1))
    if extra:
        errors.append(f"ayah ids outside 1..{EXPECTED_AYAHS}: {sorted(extra)[:5]}")

    # A blank verse renders as an empty translation card — silent, and easy to miss in review.
    blank = [i for i, t in rows if not t.strip()]
    if blank:
        errors.append(f"{len(blank)} blank translations (e.g. ayah ids {blank[:5]})")

    # The file must be in mushaf order: the seeder writes texts positionally by ayah id.
    if ids != sorted(ids):
        errors.append("rows are not in ascending ayah-id order")

    return errors


def fetch_translation(entry: Dict[str, Any], args: argparse.Namespace) -> int:
    source = entry.get("source")
    if source != "tanzil":
        print(f"error: translation {entry['id']!r} has source {source!r}; only 'tanzil' is "
              f"fetchable here.", file=sys.stderr)
        if source == "alquran.cloud":
            print("       It predates this pipeline and ships inside the prepopulated DB "
                  "(scripts/download_and_generate.py).", file=sys.stderr)
        return 2

    print("=" * 68)
    print(f"Fetching translation: {entry['id']} — {entry.get('displayName', '')}")
    print(f"  tanzil edition: {entry['edition']}  language: {entry.get('languageTag')}"
          f"{'  (RTL)' if entry.get('isRightToLeft') else ''}")
    print("=" * 68)

    raw = download_tanzil(entry["edition"])
    meta = parse_tanzil_metadata(raw)
    if meta:
        print("  source metadata: " + ", ".join(f"{k}={v}" for k, v in meta.items()
                                                if k in ("Name", "Translator", "Language",
                                                         "Last Update")))

    rows = parse_tanzil(raw, load_ayah_key_map())
    print(f"  parsed: {len(rows)} verses")

    errors = validate_translation(rows)
    if errors:
        print("\n[FAIL] translation validation:")
        for e in errors[:10]:
            print(f"  - {e}")
        print("\nNot writing. A translation with a shifted or blank verse is worse than none.")
        return 1
    print("[OK]   translation validation")

    if args.validate_only:
        print("\n[OK] All checks passed. --validate-only: nothing written.")
        return 0

    # Compact positional form: index i holds ayah id i+1. Storing the ids explicitly would
    # roughly double the asset for no information, and the validators above already guarantee
    # the rows are complete, unique and in order.
    payload = {
        "contentVersion": entry["contentVersion"],
        "translatorId": entry["id"],
        "tanzilEdition": entry["edition"],
        "sourceLastUpdate": meta.get("Last Update", ""),
        "texts": [t for _, t in rows],
    }

    out_dir = BASE_DIR / "json" / "translations"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / entry["output"]
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, separators=(",", ":"))
    size_mb = out_path.stat().st_size / 1_000_000
    print(f"    Saved: translations/{entry['output']} ({len(rows)} verses, {size_mb:.2f} MB)")

    print("\n[OK] Generated and validated.")
    print(f"     Next: copy into app/src/main/assets/quran/translations/, add the entry to")
    print(f"     QuranEditions.translations + QuranContentAssets.translations, and record the")
    print(f"     licence in nimaz-pro-data/json/{entry['license'].split('#')[0]}.")
    return 0


# ---------------------------------------------------------------------------
# Mushaf layouts (QUL)
# ---------------------------------------------------------------------------

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
    parser.add_argument("--all-translations", action="store_true",
                        help="fetch every fetchable translation in the manifest")
    args = parser.parse_args()

    manifest = load_manifest()

    if args.list:
        list_editions(manifest)
        return 0

    if args.all_translations:
        fetchable = [e for e in manifest.get("translations", []) if e.get("source") == "tanzil"]
        if not fetchable:
            print("error: no fetchable translations in the manifest", file=sys.stderr)
            return 2
        failures = []
        for entry in fetchable:
            if fetch_translation(entry, args) != 0:
                failures.append(entry["id"])
            print()
        if failures:
            print(f"[FAIL] {len(failures)} translation(s) failed: {', '.join(failures)}",
                  file=sys.stderr)
            return 1
        print(f"[OK] {len(fetchable)} translations fetched and validated.")
        return 0

    if not args.edition_id:
        list_editions(manifest)
        return 2

    axis, entry = find_edition(manifest, args.edition_id)
    if entry is None:
        print(f"error: no manifest entry with id {args.edition_id!r}", file=sys.stderr)
        list_editions(manifest)
        return 2

    if axis == "mushafLayouts":
        return fetch_mushaf_layout(entry, args)
    if axis == "translations":
        return fetch_translation(entry, args)

    # Tafseers ship inside the prepopulated DB rather than as seeded assets, and are produced
    # by download_and_generate.py. Say so plainly rather than pretending to fetch — a silent
    # no-op here would look like success.
    print(f"error: {args.edition_id!r} is a {axis[:-1]}, which this script does not fetch.",
          file=sys.stderr)
    print(f"       It is produced by scripts/download_and_generate.py "
          f"(output: {entry.get('output')}).", file=sys.stderr)
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
