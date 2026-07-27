#!/usr/bin/env python3
"""
Nimaz - Quran translation acquisition.

Downloads the bundled Quran translation catalogue from the Al Quran Cloud API
(https://alquran.cloud/api), which republishes the Tanzil.net translation corpus, and
writes one compact JSON asset per translation:

    nimaz-pro-data/json/translations/<translation_id>.json      (kept in-repo, reviewable)
    app/src/main/assets/quran/translations/<translation_id>.json (shipped in the APK)

Asset format (deliberately compact — a positional array, not 6,236 objects):

    {
      "translationId": "en_pickthall",
      "contentVersion": 1,
      "source": "...",
      "texts": ["<ayah 1>", "<ayah 2>", ...]      # exactly 6,236 entries, index i == ayah_id i+1
    }

The positional array is safe because the app's global ayah id space is the canonical
1..6,236 mushaf order — the same order this API returns and the same order
`nimaz-pro-data/json/ayahs.json` is built in. `verify()` asserts that alignment against
ayahs.json (surah/ayah counts per surah) before anything is written.

The catalogue below must stay in sync with the Kotlin catalogue in
`domain/model/QuranTranslation.kt` — `verify_catalogue_parity()` checks that and is run
by `python3 download_translations.py --check`.

Usage:
  python3 download_translations.py             # download + verify + write every translation
  python3 download_translations.py en_pickthall ur_maududi   # only these
  python3 download_translations.py --check     # no network; verify existing assets + parity
"""

import json
import re
import sys
import time
import urllib.request
from pathlib import Path
from typing import Any, Dict, List, Optional

BASE_DIR = Path(__file__).parent.parent
JSON_DIR = BASE_DIR / "json"
OUT_DIR = JSON_DIR / "translations"
ASSET_DIR = BASE_DIR.parent / "app" / "src" / "main" / "assets" / "quran" / "translations"
KOTLIN_CATALOGUE = (
    BASE_DIR.parent / "app" / "src" / "main" / "java" / "com" / "arshadshah" / "nimaz"
    / "domain" / "model" / "QuranTranslation.kt"
)

API = "https://api.alquran.cloud/v1/quran/{edition}"
EXPECTED_AYAHS = 6236

# Bump when the *shape* of the emitted asset changes, or when a translation's text is
# re-fetched and corrected. The app re-seeds a translation when this exceeds the version
# it last stored for that translation id.
CONTENT_VERSION = 1

# translation_id -> upstream alquran.cloud edition identifier.
#
# `translation_id` is the value stored in `translations.translator_id` and in the user's
# DataStore preference, so it is a STABLE KEY — never rename one. `sahih_international`
# predates this script (it was seeded by download_and_generate.py) and keeps its
# unprefixed legacy id for exactly that reason; everything added since is `<lang>_<name>`.
CATALOGUE: Dict[str, str] = {
    # English
    "sahih_international": "en.sahih",
    "en_yusuf_ali": "en.yusufali",
    "en_pickthall": "en.pickthall",
    "en_clear_quran": "en.itani",
    # Urdu
    "ur_maududi": "ur.maududi",
    "ur_jalandhry": "ur.jalandhry",
    # Other languages
    "id_indonesian": "id.indonesian",
    "tr_diyanet": "tr.diyanet",
    "fr_hamidullah": "fr.hamidullah",
    "bn_bengali": "bn.bengali",
    "hi_hindi": "hi.hindi",
    "es_garcia": "es.garcia",
    "ru_kuliev": "ru.kuliev",
    "ms_basmeih": "ms.basmeih",
    "de_bubenheim": "de.bubenheim",
}


# ---------------------------------------------------------------------------
# HTTP
# ---------------------------------------------------------------------------


def download_json(url: str, retries: int = 4) -> Any:
    last: Optional[Exception] = None
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=120) as response:
                return json.loads(response.read().decode("utf-8"))
        except Exception as e:  # noqa: BLE001 - best-effort acquisition script
            last = e
            if attempt < retries - 1:
                time.sleep(2 ** attempt)
    raise RuntimeError(f"failed to fetch {url}: {last}")


# ---------------------------------------------------------------------------
# Reference ayah order
# ---------------------------------------------------------------------------


def load_reference() -> List[Dict[str, int]]:
    """The app's canonical ayah id space, from the already-shipped ayahs.json."""
    with open(JSON_DIR / "ayahs.json", encoding="utf-8") as f:
        ayahs = json.load(f)
    if len(ayahs) != EXPECTED_AYAHS:
        raise SystemExit(f"ayahs.json has {len(ayahs)} rows, expected {EXPECTED_AYAHS}")
    ordered = sorted(ayahs, key=lambda a: a["id"])
    for i, a in enumerate(ordered, start=1):
        if a["id"] != i:
            raise SystemExit(f"ayahs.json id space is not contiguous at {i} (found {a['id']})")
    return [{"surah": a["surah_id"], "ayah": a["number_in_surah"]} for a in ordered]


def flatten(edition_payload: Any) -> List[Dict[str, Any]]:
    """API surahs -> flat [{surah, ayah, text}] in mushaf order."""
    out = []
    for surah in edition_payload["data"]["surahs"]:
        for verse in surah["ayahs"]:
            out.append({
                "surah": surah["number"],
                "ayah": verse["numberInSurah"],
                "text": verse["text"].strip(),
            })
    return out


def build(translation_id: str, edition: str, reference: List[Dict[str, int]]) -> Dict[str, Any]:
    payload = download_json(API.format(edition=edition))
    verses = flatten(payload)

    if len(verses) != EXPECTED_AYAHS:
        raise SystemExit(f"{translation_id}: got {len(verses)} verses, expected {EXPECTED_AYAHS}")

    # Hard alignment check: the API's order must match our id space verse for verse,
    # otherwise the positional array would silently mis-attribute every translation.
    for i, (got, want) in enumerate(zip(verses, reference), start=1):
        if got["surah"] != want["surah"] or got["ayah"] != want["ayah"]:
            raise SystemExit(
                f"{translation_id}: ayah id {i} misaligned — "
                f"API has {got['surah']}:{got['ayah']}, ayahs.json has {want['surah']}:{want['ayah']}"
            )

    empty = [i for i, v in enumerate(verses, start=1) if not v["text"]]
    if empty:
        raise SystemExit(f"{translation_id}: {len(empty)} empty verses (first: ayah id {empty[0]})")

    meta = payload["data"]
    return {
        "translationId": translation_id,
        "contentVersion": CONTENT_VERSION,
        "source": f"alquran.cloud edition '{edition}' ({meta.get('englishName', '')})".strip(),
        "texts": [v["text"] for v in verses],
    }


def write(doc: Dict[str, Any]) -> None:
    for directory in (OUT_DIR, ASSET_DIR):
        directory.mkdir(parents=True, exist_ok=True)
        path = directory / f"{doc['translationId']}.json"
        with open(path, "w", encoding="utf-8") as f:
            # separators: no gratuitous whitespace — these ship in the APK.
            json.dump(doc, f, ensure_ascii=False, separators=(",", ":"))
        size = path.stat().st_size
        print(f"    wrote {path.relative_to(BASE_DIR.parent)} ({size / 1024:.0f} KB)")


# ---------------------------------------------------------------------------
# Verification
# ---------------------------------------------------------------------------


def verify_assets() -> bool:
    """Re-validate every emitted asset without touching the network."""
    ok = True
    if not ASSET_DIR.exists():
        print(f"  no asset dir at {ASSET_DIR}")
        return False
    for translation_id in CATALOGUE:
        path = ASSET_DIR / f"{translation_id}.json"
        if not path.exists():
            print(f"  MISSING {path.name}")
            ok = False
            continue
        doc = json.loads(path.read_text(encoding="utf-8"))
        problems = []
        if doc.get("translationId") != translation_id:
            problems.append(f"translationId={doc.get('translationId')!r}")
        if len(doc.get("texts", [])) != EXPECTED_AYAHS:
            problems.append(f"texts={len(doc.get('texts', []))}")
        if any(not t.strip() for t in doc.get("texts", [])):
            problems.append("has empty verses")
        if problems:
            print(f"  INVALID {path.name}: {', '.join(problems)}")
            ok = False
        else:
            print(f"  ok {path.name} ({path.stat().st_size / 1024:.0f} KB)")
    return ok


def verify_catalogue_parity() -> bool:
    """The Kotlin catalogue is the app-side source of truth; ids must match this script."""
    if not KOTLIN_CATALOGUE.exists():
        print(f"  no Kotlin catalogue at {KOTLIN_CATALOGUE}")
        return False
    src = KOTLIN_CATALOGUE.read_text(encoding="utf-8")
    kotlin_ids = set(re.findall(r'id\s*=\s*"([a-z0-9_]+)"', src))
    script_ids = set(CATALOGUE)
    ok = True
    for missing in sorted(script_ids - kotlin_ids):
        print(f"  in script but not in QuranTranslation.kt: {missing}")
        ok = False
    for extra in sorted(kotlin_ids - script_ids):
        print(f"  in QuranTranslation.kt but not in script: {extra}")
        ok = False
    if ok:
        print(f"  catalogue parity ok ({len(script_ids)} translations)")
    return ok


# ---------------------------------------------------------------------------


def main() -> int:
    args = [a for a in sys.argv[1:]]

    if "--check" in args:
        print("Verifying emitted assets:")
        assets_ok = verify_assets()
        print("Verifying catalogue parity:")
        parity_ok = verify_catalogue_parity()
        return 0 if (assets_ok and parity_ok) else 1

    wanted = args or list(CATALOGUE)
    unknown = [w for w in wanted if w not in CATALOGUE]
    if unknown:
        raise SystemExit(f"unknown translation id(s): {', '.join(unknown)}")

    reference = load_reference()
    print(f"Reference ayah space: {len(reference)} ayahs")

    total = 0
    for translation_id in wanted:
        edition = CATALOGUE[translation_id]
        print(f"\n[{translation_id}] <- {edition}")
        doc = build(translation_id, edition, reference)
        write(doc)
        total += (ASSET_DIR / f"{translation_id}.json").stat().st_size

    print(f"\nDone: {len(wanted)} translations, {total / 1024 / 1024:.1f} MB uncompressed")
    print("Now run: python3 download_translations.py --check")
    return 0


if __name__ == "__main__":
    sys.exit(main())
