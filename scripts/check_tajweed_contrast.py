#!/usr/bin/env python3
"""
Tajweed colour contrast validator (issue #294).

The tajweed feature paints ~20 rule colours onto the Quran text. This checks
every rule's light and dark colour against the reader background it actually
renders on, using the WCAG 2.x relative-luminance contrast formula, and fails
if any is below the 4.5:1 body-text threshold. It also reports the pairwise
separation within each same-hue rule family (the madd family, the idgham
family), where near-neighbours are hardest to tell apart.

Reads the hex values straight from the theme sources, so the committed contrast
table in docs/ARCHITECTURE.md can be regenerated and can never silently drift
from Color.kt / Palette.kt.

    python3 scripts/check_tajweed_contrast.py            # verify, exit non-zero on failure
    python3 scripts/check_tajweed_contrast.py --markdown  # print the docs table
"""

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

# The theme moved from :app to :core:ui in PR 10 of #551, and will not be the last thing to move
# while the module split is under way. Resolve it rather than hardcode it, newest home first, and
# fail with the list of places looked in — a wrong path here is a crash, not a silent pass, but
# only if the error says where it looked.
THEME_CANDIDATES = [
    ROOT / "core/ui/src/main/kotlin/com/arshadshah/nimaz/presentation/theme",
    ROOT / "app/src/main/java/com/arshadshah/nimaz/presentation/theme",
]


def _find_theme():
    for candidate in THEME_CANDIDATES:
        if (candidate / "Palette.kt").is_file():
            return candidate
    looked = "\n  ".join(str(c) for c in THEME_CANDIDATES)
    raise SystemExit(
        f"Palette.kt not found. Looked in:\n  {looked}\n"
        "If the theme moved again, add its new home to THEME_CANDIDATES."
    )


THEME = _find_theme()
PALETTE = THEME / "Palette.kt"
COLOR = THEME / "Color.kt"

# The reader surfaces the coloured ayah text sits on. The lighter of each pair
# is the stricter target for that theme (less contrast for the coloured text).
LIGHT_BG = "FAFAFA"  # NimazColors.BackgroundLight (P.GrayBg)
DARK_BG = "1C1917"   # NimazColors.SurfaceDark (P.Stone900) — lighter than Stone950
THRESHOLD = 4.5

# Rules in legend order, paired with the TajweedColors val prefix.
RULES = [
    ("Ghunnah", "Ghunnah"),
    ("Ikhfa", "Ikhfa"),
    ("Ikhfa Shafawi", "IkhfaShafawi"),
    ("Idgham w/ Ghunnah", "IdghamGhunnah"),
    ("Idgham w/o Ghunnah", "IdghamNoGhunnah"),
    ("Idgham Shafawi", "IdghamShafawi"),
    ("Idgham Mutajanisayn", "IdghamMutajanisayn"),
    ("Idgham Mutaqaribayn", "IdghamMutaqaribay"),
    ("Idgham Mutamathilayn", "IdghamMutamathilayn"),
    ("Qalqalah Sughra", "QalqalahSughra"),
    ("Qalqalah Kubra", "QalqalahKubra"),
    ("Madd Tabee'i", "MaddNormal"),
    ("Madd Munfasil", "MaddMunfasil"),
    ("Madd Muttasil", "MaddMuttasil"),
    ("Madd 'Aarid", "MaddAarid"),
    ("Madd Lin", "MaddLin"),
    ("Madd Lazim", "MaddNecessary"),
    ("Iqlab", "Iqlab"),
    ("Lam Shamsiyyah", "LamShamsiyyah"),
    ("Silent", "Silent"),
    ("Hamza al-Wasl", "HamzaWasl"),
    ("Waqf sign", "Waqf"),
    ("Tafkhim", "Tafkhim"),
    ("Tarqiq", "Tarqiq"),
]

# Same-hue families whose members must stay tellable apart.
FAMILIES = {
    "madd": ["MaddNormal", "MaddMunfasil", "MaddMuttasil", "MaddAarid",
             "MaddLin", "MaddNecessary"],
    "idgham": ["IdghamGhunnah", "IdghamNoGhunnah", "IdghamShafawi",
               "IdghamMutajanisayn", "IdghamMutaqaribay", "IdghamMutamathilayn"],
}


def _lum(hex6):
    r, g, b = (int(hex6[i:i + 2], 16) / 255 for i in (0, 2, 4))
    f = lambda c: c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4
    return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b)


def contrast(a, b):
    l1, l2 = sorted((_lum(a), _lum(b)), reverse=True)
    return (l1 + 0.05) / (l2 + 0.05)


def load_colors():
    palette = PALETTE.read_text(encoding="utf-8")
    hexmap = dict(re.findall(r'val (\w+)\s*=\s*Color\(0xFF([0-9A-Fa-f]{6})\)', palette))
    color = COLOR.read_text(encoding="utf-8")
    block = color[color.index("object TajweedColors"):]
    block = block[:block.index("\n    }")]
    tokens = dict(re.findall(r'val (\w+)\s*=\s*P\.(\w+)', block))

    def resolve(val_name):
        tok = tokens[val_name]
        if tok not in hexmap:
            raise KeyError(f"palette token {tok} (for {val_name}) not found")
        return hexmap[tok]

    return {prefix: (resolve(prefix + "Light"), resolve(prefix + "Dark"))
            for _, prefix in RULES}


def evaluate():
    colors = load_colors()
    rows = []
    failures = []
    for name, prefix in RULES:
        light, dark = colors[prefix]
        cl = contrast(light, LIGHT_BG)
        cd = contrast(dark, DARK_BG)
        rows.append((name, light, cl, dark, cd))
        if cl < THRESHOLD:
            failures.append(f"{name} light #{light}: {cl:.2f}:1 < {THRESHOLD}")
        if cd < THRESHOLD:
            failures.append(f"{name} dark #{dark}: {cd:.2f}:1 < {THRESHOLD}")
    return colors, rows, failures


def family_separations(colors):
    out = {}
    for fam, members in FAMILIES.items():
        pairs = []
        for i in range(len(members)):
            for j in range(i + 1, len(members)):
                a = colors[members[i]][0]
                b = colors[members[j]][0]
                pairs.append((members[i], members[j], contrast(a, b)))
        out[fam] = pairs
    return out


def print_markdown(rows):
    print("| Rule | Light | vs #FAFAFA | Dark | vs #1C1917 |")
    print("|---|---|---|---|---|")
    for name, light, cl, dark, cd in rows:
        print(f"| {name} | `#{light}` | {cl:.2f}:1 | `#{dark}` | {cd:.2f}:1 |")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--markdown", action="store_true", help="print the docs table")
    args = ap.parse_args()

    colors, rows, failures = evaluate()

    if args.markdown:
        print_markdown(rows)
        return 0

    for name, light, cl, dark, cd in rows:
        flag_l = "" if cl >= THRESHOLD else "  FAIL"
        flag_d = "" if cd >= THRESHOLD else "  FAIL"
        print(f"{name:22s} light #{light} {cl:5.2f}{flag_l:6s} "
              f"dark #{dark} {cd:5.2f}{flag_d}")

    print("\nSame-family light-theme separation (>=3:1 desired, else rely on the "
          "legend / a decoration channel):")
    for fam, pairs in family_separations(colors).items():
        worst = min(pairs, key=lambda p: p[2])
        print(f"  {fam}: worst pair {worst[0]} vs {worst[1]} = {worst[2]:.2f}:1")

    if failures:
        print(f"\n{len(failures)} CONTRAST FAILURE(S) (< {THRESHOLD}:1):")
        for f in failures:
            print(f"  - {f}")
        return 1
    print(f"\nOK: all tajweed colours pass {THRESHOLD}:1 against the reader background.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
