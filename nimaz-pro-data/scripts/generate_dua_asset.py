#!/usr/bin/env python3
"""Generate the app's bundled dua content asset (assets/duas/duas.json).

Why this exists
---------------
Dua content historically lived only inside the prepopulated DB asset, which
Room copies (via `createFromAsset`) *only on a fresh install*. Expanding the
dataset therefore never reached existing users on update. The app now seeds
the dua tables at runtime from this JSON asset (see DuaContentSeeder), exactly
like the Help content, so both fresh installs and upgrades converge on the
bundled content.

Whenever the dua data changes, regenerate the prepopulated DB AND run this
script, then BUMP `CONTENT_VERSION` below so existing installs re-seed.

Usage
-----
    python scripts/generate_dua_asset.py
"""
import json
from pathlib import Path

# Bump this whenever the dua content changes so existing installs re-seed.
# Must stay >= the highest value ever shipped.
CONTENT_VERSION = 1

ROOT = Path(__file__).parent.parent
JSON_DIR = ROOT / "json"
OUTPUT = ROOT.parent / "app" / "src" / "main" / "assets" / "duas" / "duas.json"


def main() -> None:
    categories = json.loads((JSON_DIR / "dua_categories.json").read_text("utf-8"))
    duas = json.loads((JSON_DIR / "duas.json").read_text("utf-8"))

    # Deterministic ordering so the asset diff is stable across regenerations.
    categories.sort(key=lambda c: (c["display_order"], c["id"]))
    duas.sort(key=lambda d: (d["category_id"], d["display_order"], d["id"]))

    root = {
        "contentVersion": CONTENT_VERSION,
        "categories": categories,
        "duas": duas,
    }

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT.open("w", encoding="utf-8") as f:
        json.dump(root, f, ensure_ascii=False, indent=2)
        f.write("\n")

    print(f"Wrote {OUTPUT}")
    print(f"  contentVersion={CONTENT_VERSION}, "
          f"categories={len(categories)}, duas={len(duas)}")


if __name__ == "__main__":
    main()
