#!/usr/bin/env python3
"""
Nimaz - 16-line IndoPak Mushaf data acquisition scaffold (issue #265, sub-task 1/7 of #263)

STATUS: scaffold only. The endpoints/auth below are NOT verified against live docs — this was
written without outbound network access. Before running this for real:

  1. Re-read https://qul.tarteel.ai/docs/mushaf-layout and/or
     https://api-docs.quran.foundation/docs/tutorials/fonts/page-layout/ for the CURRENT export
     format, auth flow, and exact field names (they may have changed).
  2. Re-confirm the license explicitly permits bundling a full bulk extract into a shipped,
     offline, open-source Android app (not just live per-request API use) — see
     nimaz-pro-data/json/LICENSES_INDOPAK.md and fill it in with the real source/version/license
     once acquired.
  3. Do NOT source the line-layout from a scanned/typeset commercial edition (e.g. Taj Company) -
     that typesetting is copyrighted. Use a source's own structured line-break data.

This script normalises whatever source is used into the two target files and then validates them
against the acceptance criteria in issue #265:
  - nimaz-pro-data/json/ayahs_indopak.json           (6,236 rows, keyed by existing ayah_id)
  - nimaz-pro-data/json/mushaf_layout_indopak16.json (548 pages, each with <=16 lines)
"""

import json
import urllib.request
from pathlib import Path
from typing import Any, Dict, List, Optional, Set

BASE_DIR = Path(__file__).parent.parent
JSON_DIR = BASE_DIR / "json"

EXPECTED_SURAHS = 114
EXPECTED_AYAHS = 6236
EXPECTED_PAGES = 548
MAX_LINES_PER_PAGE = 16

VALID_LINE_TYPES = {"ayah", "surah_header", "basmalah"}


def download_json(url: str, headers: Optional[Dict[str, str]] = None, retries: int = 3) -> Any:
    """Download and parse JSON from URL with retries. Mirrors download_full_data.py conventions."""
    req_headers = {"User-Agent": "Mozilla/5.0"}
    if headers:
        req_headers.update(headers)
    for attempt in range(retries):
        try:
            print(f"    Downloading: {url}")
            req = urllib.request.Request(url, headers=req_headers)
            with urllib.request.urlopen(req, timeout=120) as response:
                return json.loads(response.read().decode("utf-8"))
        except Exception as e:  # noqa: BLE001 - best-effort acquisition script
            print(f"    Attempt {attempt + 1} failed: {e}")
    return None


def save_json(data: Any, filename: str) -> None:
    filepath = JSON_DIR / filename
    with open(filepath, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"    Saved: {filename}")


def load_existing_ayah_ids() -> Set[int]:
    """Existing 6,236 ayah ids from ayahs.json — ayahs_indopak.json must reconcile against these."""
    with open(JSON_DIR / "ayahs.json", encoding="utf-8") as f:
        ayahs = json.load(f)
    return {a["id"] for a in ayahs}


def fetch_indopak_ayah_text() -> List[Dict[str, Any]]:
    """
    TODO: replace with a real call against the chosen source (QUL export or Quran Foundation
    Content API, e.g. a verses-by-page or verses-by-chapter endpoint with `text_indopak` /
    equivalent script field). Must return exactly EXPECTED_AYAHS rows, one per existing ayah_id.
    """
    raise NotImplementedError(
        "Wire this up to the chosen, license-verified source before running. "
        "See nimaz-pro-data/json/LICENSES_INDOPAK.md."
    )


def fetch_mushaf_layout_16line() -> List[Dict[str, Any]]:
    """
    TODO: replace with a real call against the chosen source's 16-line ("INDOPAK_16_LINES")
    mushaf-layout export, normalised into the schema documented in
    docs/nimaz-pro-data-guide.md (page_number, line_number, line_type, surah_id, ayah_id,
    first_word_position, last_word_position). Must cover all EXPECTED_PAGES pages.
    """
    raise NotImplementedError(
        "Wire this up to the chosen, license-verified source before running. "
        "See nimaz-pro-data/json/LICENSES_INDOPAK.md."
    )


def validate_ayahs_indopak(rows: List[Dict[str, Any]], existing_ayah_ids: Set[int]) -> List[str]:
    errors = []
    row_ids = {r["ayah_id"] for r in rows}
    if len(rows) != EXPECTED_AYAHS:
        errors.append(f"expected {EXPECTED_AYAHS} rows, got {len(rows)}")
    missing = existing_ayah_ids - row_ids
    if missing:
        errors.append(f"{len(missing)} existing ayah_ids missing (e.g. {sorted(missing)[:5]})")
    extra = row_ids - existing_ayah_ids
    if extra:
        errors.append(f"{len(extra)} ayah_ids not in existing ayahs.json (e.g. {sorted(extra)[:5]})")
    empty_text = [r["ayah_id"] for r in rows if not r.get("text_indopak", "").strip()]
    if empty_text:
        errors.append(f"{len(empty_text)} rows have empty text_indopak (e.g. {empty_text[:5]})")
    return errors


def validate_mushaf_layout(rows: List[Dict[str, Any]]) -> List[str]:
    errors = []
    pages: Dict[int, Set[int]] = {}
    surah_ids: Set[int] = set()
    for r in rows:
        if r["line_type"] not in VALID_LINE_TYPES:
            errors.append(f"invalid line_type {r['line_type']!r} on page {r['page_number']}")
        pages.setdefault(r["page_number"], set()).add(r["line_number"])
        surah_ids.add(r["surah_id"])

    if len(pages) != EXPECTED_PAGES:
        errors.append(f"expected {EXPECTED_PAGES} pages, got {len(pages)}")
    missing_pages = set(range(1, EXPECTED_PAGES + 1)) - set(pages)
    if missing_pages:
        errors.append(f"{len(missing_pages)} pages missing (e.g. {sorted(missing_pages)[:5]})")
    over_limit = {p: len(lines) for p, lines in pages.items() if len(lines) > MAX_LINES_PER_PAGE}
    if over_limit:
        errors.append(f"{len(over_limit)} pages exceed {MAX_LINES_PER_PAGE} lines: {dict(list(over_limit.items())[:5])}")
    if len(surah_ids) != EXPECTED_SURAHS:
        errors.append(f"expected {EXPECTED_SURAHS} distinct surah_ids referenced, got {len(surah_ids)}")
    return errors


def main() -> None:
    print("=" * 60)
    print("16-line IndoPak Mushaf data acquisition (issue #265)")
    print("=" * 60)

    existing_ayah_ids = load_existing_ayah_ids()
    print(f"  Existing ayah id space: {len(existing_ayah_ids)} ayahs (expected {EXPECTED_AYAHS})")

    ayah_rows = fetch_indopak_ayah_text()
    ayah_errors = validate_ayahs_indopak(ayah_rows, existing_ayah_ids)
    if ayah_errors:
        print("\n[FAIL] ayahs_indopak.json validation:")
        for e in ayah_errors:
            print(f"  - {e}")
    else:
        save_json(ayah_rows, "ayahs_indopak.json")

    layout_rows = fetch_mushaf_layout_16line()
    layout_errors = validate_mushaf_layout(layout_rows)
    if layout_errors:
        print("\n[FAIL] mushaf_layout_indopak16.json validation:")
        for e in layout_errors:
            print(f"  - {e}")
    else:
        save_json(layout_rows, "mushaf_layout_indopak16.json")

    if ayah_errors or layout_errors:
        raise SystemExit(1)

    print("\n[OK] Both files generated and validated. Now fill in LICENSES_INDOPAK.md with the "
          "real source/version/license before committing the data.")


if __name__ == "__main__":
    main()
