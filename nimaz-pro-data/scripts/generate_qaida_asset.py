#!/usr/bin/env python3
"""Generate the app's bundled Qaida content asset (assets/qaida/qaida_content.json).

Why this exists
---------------
Qaida content (lessons / letters / lines / cells) historically lived only inside
the prepopulated DB asset, which Room copies (via `createFromAsset`) *only on a
fresh install*. That meant the content never reached users who upgraded from an
earlier release: their on-device database already existed, so the prepopulated
DB was never re-copied, and `MIGRATION_14_15` only creates the Qaida tables
(empty) — it does not seed them. Those users saw an empty Qaida.

The app now seeds the Qaida content tables at runtime from this JSON asset (see
QaidaContentSeeder), exactly like the Dua and Help content, so both fresh
installs and upgrades converge on the bundled content.

Whenever the Qaida data changes, regenerate the prepopulated DB AND run this
script, then BUMP `CONTENT_VERSION` below so existing installs re-seed.

Usage
-----
    python scripts/generate_qaida_asset.py
"""
import json
from pathlib import Path

# Bump this whenever the Qaida content changes so existing installs re-seed.
# Must stay >= the highest value ever shipped.
CONTENT_VERSION = 1

ROOT = Path(__file__).parent.parent
JSON_DIR = ROOT / "json"
OUTPUT = ROOT.parent / "app" / "src" / "main" / "assets" / "qaida" / "qaida_content.json"


def main() -> None:
    lessons = json.loads((JSON_DIR / "qaida_lessons.json").read_text("utf-8"))
    letters = json.loads((JSON_DIR / "qaida_letters.json").read_text("utf-8"))
    lines = json.loads((JSON_DIR / "qaida_lines.json").read_text("utf-8"))
    cells = json.loads((JSON_DIR / "qaida_cells.json").read_text("utf-8"))

    # Deterministic ordering so the asset diff is stable across regenerations.
    lessons.sort(key=lambda x: (x["display_order"], x["id"]))
    letters.sort(key=lambda x: (x["display_order"], x["id"]))
    lines.sort(key=lambda x: (x["lesson_id"], x["display_order"], x["id"]))
    cells.sort(key=lambda x: (x["lesson_id"], x["line_id"], x["position"], x["id"]))

    root = {
        "contentVersion": CONTENT_VERSION,
        "lessons": lessons,
        "letters": letters,
        "lines": lines,
        "cells": cells,
    }

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT.open("w", encoding="utf-8") as f:
        json.dump(root, f, ensure_ascii=False, indent=2)
        f.write("\n")

    print(f"Wrote {OUTPUT}")
    print(f"  contentVersion={CONTENT_VERSION}, lessons={len(lessons)}, "
          f"letters={len(letters)}, lines={len(lines)}, cells={len(cells)}")


if __name__ == "__main__":
    main()
