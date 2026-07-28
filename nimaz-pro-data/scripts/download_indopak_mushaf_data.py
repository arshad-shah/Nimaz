#!/usr/bin/env python3
"""
Nimaz - 16-line IndoPak Mushaf data acquisition (issue #265, sub-task 1/7 of #263).

**This script is now a thin wrapper.** Its engine was generalised into
`download_mushaf_layout.py` so the app can ship more than one line-accurate edition; the
16-line acquisition is byte-for-byte the same process it always was (verified: re-running the
generalised script reproduced the original `ayahs_indopak.json` / `mushaf_layout_indopak16.json`
content exactly, for all 6,236 ayahs and all 13,970 layout rows).

Prefer the general script — it can fetch any registered edition and reports whether a new one
can share an existing text source:

    python3 download_mushaf_layout.py --list
    python3 download_mushaf_layout.py INDOPAK_16
    python3 download_mushaf_layout.py INDOPAK_15 --compare INDOPAK_16

Output moved with the generalisation, from
  nimaz-pro-data/json/ayahs_indopak.json            -> json/mushaf/indopak_text.json
  nimaz-pro-data/json/mushaf_layout_indopak16.json  -> json/mushaf/indopak_16_layout.json
and likewise under app/src/main/assets/quran/mushaf/.

Usage (unchanged):
  python3 download_indopak_mushaf_data.py        # fetch all 548 pages, build + validate + write
  python3 download_indopak_mushaf_data.py 1 3    # fetch pages 1..3 only (dev/inspection)
"""

import sys

from download_mushaf_layout import LAYOUTS, run

SCRIPT_KEY = "INDOPAK_16"


def main() -> int:
    spec = LAYOUTS[SCRIPT_KEY]
    args = sys.argv[1:]
    if len(args) == 2:
        return run(spec, int(args[0]), int(args[1]), dev=True, compare=None)
    return run(spec, 1, spec.pages, dev=False, compare=None)


if __name__ == "__main__":
    sys.exit(main())
