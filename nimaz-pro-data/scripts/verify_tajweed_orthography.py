#!/usr/bin/env python3
"""
Verify the tajweed orthography invariant (issue #290).

For every ayah, the coloured tajweed segments must round-trip to the canonical
``text_arabic`` byte-for-byte, and neither text may carry a BOM or stray
zero-width mark. This is the data-level gate that keeps "Show Tajweed Colors"
from changing the glyphs on screen (only their colour) and lets one string back
search, bookmarks and audio highlighting.

Runs against the JSON pipeline (``tajweed.json`` + ``ayahs.json``) — the source
of truth for the next DB regeneration — so it catches drift before the binary
DB is rebuilt. Exits non-zero on any failure (CI-friendly; folded into the full
harness in #292).

    python3 nimaz-pro-data/scripts/verify_tajweed_orthography.py
"""

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from preparse_tajweed import (
    align_segments_to_canonical,
    normalise_uthmani,
    preparse_tajweed,
)

JSON_DIR = Path(__file__).resolve().parent.parent / "json"

# Marks that must never survive normalisation into a stored string.
FORBIDDEN = {
    "﻿": "BOM / ZERO WIDTH NO-BREAK SPACE",
    "​": "ZERO WIDTH SPACE",
    "‌": "ZERO WIDTH NON-JOINER",
    "‍": "ZERO WIDTH JOINER",
}


def main():
    with open(JSON_DIR / "tajweed.json", encoding="utf-8") as f:
        tajweed = json.load(f)
    with open(JSON_DIR / "ayahs.json", encoding="utf-8") as f:
        ayahs = json.load(f)

    mismatches = []
    forbidden_hits = []
    missing_tajweed = []

    for a in ayahs:
        key = f"{a['surah_id']}:{a['number_in_surah']}"
        canonical = normalise_uthmani(a["text_arabic"])

        for ch, label in FORBIDDEN.items():
            if ch in canonical:
                forbidden_hits.append((key, "text_arabic", label))

        raw = tajweed.get(key)
        if not raw:
            missing_tajweed.append(key)
            continue

        segments = align_segments_to_canonical(
            preparse_tajweed(raw, key=key), canonical
        )
        stripped = "".join(s["t"] for s in segments)
        if stripped != canonical:
            mismatches.append(key)
        for ch, label in FORBIDDEN.items():
            if ch in stripped:
                forbidden_hits.append((key, "text_tajweed", label))

    total = len(ayahs)
    ok = total - len(mismatches)
    print(f"Round-trip strip(text_tajweed) == text_arabic: {ok}/{total}")
    if missing_tajweed:
        print(f"Ayahs with no tajweed source: {len(missing_tajweed)} "
              f"(e.g. {missing_tajweed[:5]})")

    failed = False
    if mismatches:
        failed = True
        print(f"\nFAIL: {len(mismatches)} ayah(s) do not round-trip:")
        for key in mismatches[:20]:
            print(f"  - {key}")
        if len(mismatches) > 20:
            print(f"  … and {len(mismatches) - 20} more")

    if forbidden_hits:
        failed = True
        print(f"\nFAIL: {len(forbidden_hits)} forbidden mark(s) found:")
        for key, col, label in forbidden_hits[:20]:
            print(f"  - {key} [{col}]: {label}")

    if failed:
        return 1

    print("OK: tajweed orthography invariant holds for all ayahs.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
