#!/usr/bin/env python3
"""
Nimaz - line-accurate Mushaf layout acquisition (generalised).

This is the engine behind every line-accurate Mushaf edition the app ships. It was
generalised out of `download_indopak_mushaf_data.py` (issue #265, which acquired the
16-line IndoPak layout); that script is now a thin wrapper that calls in here, so the
16-line acquisition is byte-for-byte the same process it always was.

Source: Quranic Universal Library (QUL / Tarteel.ai) — https://qul.tarteel.ai
QUL publishes these layouts as open, structured line-break data for building Quran apps.
We consume the public per-page preview, which renders, for every page, each line and every
word with its `surah:ayah:word` location and the glyph text. This is QUL's own structured
line-break determination — NOT a scan or a copy of a commercial edition's typeset artwork.
See nimaz-pro-data/json/LICENSES_MUSHAF_LAYOUTS.md.

For each script key it produces and validates two files, in `nimaz-pro-data/json/mushaf/`
and mirrored into `app/src/main/assets/quran/mushaf/`:

  <script>_text.json    6,236 rows: {"ayah_id", "text", "words"[]}
  <script>_layout.json  one row per line-segment: page/line/type/surah/ayah/word range

The two are cross-validated: every ayah's word positions must be exactly covered by its
layout segments, and `" ".join(words) == text` must round-trip (no intra-word spaces), which
is what lets the app store the glyph text once per ayah and slice it per printed line.

Usage:
  python3 download_mushaf_layout.py INDOPAK_15                 # fetch, validate, write
  python3 download_mushaf_layout.py INDOPAK_15 --pages 1 3     # dev: fetch a few pages only
  python3 download_mushaf_layout.py INDOPAK_15 --compare INDOPAK_16
                                                              # report text-sharing with another
  python3 download_mushaf_layout.py --list
"""

import argparse
import json
import os
import re
import sys
import time
import urllib.request
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Any, Dict, List, Optional, Set, Tuple

BASE_DIR = Path(__file__).parent.parent
JSON_DIR = BASE_DIR / "json"
MUSHAF_JSON_DIR = JSON_DIR / "mushaf"
ASSET_DIR = BASE_DIR.parent / "app" / "src" / "main" / "assets" / "quran" / "mushaf"

EXPECTED_SURAHS = 114
EXPECTED_AYAHS = 6236
VALID_LINE_TYPES = {"ayah", "surah_header", "basmalah"}

# Surah 1 counts the basmalah as ayah 1; surah 9 (At-Tawbah) has no basmalah.
NO_BASMALAH_SURAHS = {1, 9}

_TYPE_ORDER = {"surah_header": 0, "basmalah": 1, "ayah": 2}


# ---------------------------------------------------------------------------
# Registry — must stay in sync with the Kotlin MushafScript catalogue
# ---------------------------------------------------------------------------

class LayoutSpec:
    def __init__(self, key: str, qul_id: int, pages: int, lines: int, label: str,
                 text_source: str):
        self.key = key            # matches MushafScript enum name in Kotlin
        self.qul_id = qul_id      # QUL mushaf-layout resource id
        self.pages = pages
        self.lines = lines
        self.label = label
        # Editions that print the same glyph text and segment it into the same words differ
        # only in where the lines break, so they share ONE text asset and ship only their own
        # layout. Verified, not assumed: `write_outputs` re-compares against an existing text
        # file for this source and refuses to write if it has drifted.
        self.text_source = text_source

    @property
    def url(self) -> str:
        return f"https://qul.tarteel.ai/resources/mushaf-layout/{self.qul_id}"


LAYOUTS: Dict[str, LayoutSpec] = {
    "INDOPAK_16": LayoutSpec("INDOPAK_16", 11, 548, 16,
                             "Indopak 16 lines (Taj Company)", text_source="INDOPAK"),
    "INDOPAK_15": LayoutSpec("INDOPAK_15", 12, 610, 15,
                             "Indopak 15 lines (Qudratullah)", text_source="INDOPAK"),
    # Shares the IndoPak script but NOT its glyphs: this print differs from the 16-line in
    # the vowel marks of 28 ayahs (fatha vs shadda, superscript alef vs fatha, ...), so it
    # carries its own text source. Verified by write_outputs, which refused to let it share.
    "INDOPAK_13": LayoutSpec("INDOPAK_13", 313, 847, 13,
                             "Indopak 13 lines (Taj Company)", text_source="INDOPAK_13"),
}


# ---------------------------------------------------------------------------
# HTTP
# ---------------------------------------------------------------------------


def cache_dir(spec: LayoutSpec) -> Path:
    root = Path(os.environ.get("MUSHAF_CACHE_DIR", "/tmp/mushaf_pages"))
    return root / spec.key


def download_page_html(spec: LayoutSpec, page: int, retries: int = 4) -> str:
    cache = cache_dir(spec) / f"page_{page:04d}.html"
    if cache.exists():
        return cache.read_text(encoding="utf-8")
    url = f"{spec.url}?page={page}"
    last_err: Optional[Exception] = None
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=90) as response:
                html = response.read().decode("utf-8")
            cache.parent.mkdir(parents=True, exist_ok=True)
            cache.write_text(html, encoding="utf-8")
            return html
        except Exception as e:  # noqa: BLE001 - best-effort acquisition script
            last_err = e
            time.sleep(2 ** attempt)
    raise RuntimeError(f"failed to fetch {spec.key} page {page}: {last_err}")


def fetch_pages(spec: LayoutSpec, page_start: int, page_end: int, workers: int = 8) -> Dict[int, str]:
    pages: Dict[int, str] = {}
    todo = list(range(page_start, page_end + 1))
    with ThreadPoolExecutor(max_workers=workers) as ex:
        futures = {ex.submit(download_page_html, spec, p): p for p in todo}
        done = 0
        for fut in as_completed(futures):
            pages[futures[fut]] = fut.result()
            done += 1
            if done % 50 == 0 or done == len(todo):
                print(f"    fetched {done}/{len(todo)} pages")
    return pages


# ---------------------------------------------------------------------------
# Parsing
# ---------------------------------------------------------------------------

LINE_CLASS_RE = re.compile(r'<div class="(line[^"]*)"\s+id="line-\d+"')
SURAH_ICON_RE = re.compile(r"surah(\d{3})")
WORD_SPAN_RE = re.compile(
    r'<span class="char[^"]*"\s+id="word-\d+"[^>]*?'
    r'data-location="(\d+):(\d+):(\d+)"[^>]*?'
    r'data-position="(\d+)"[^>]*?>\s*'
    r'<a[^>]*>\s*(.*?)\s*</a>',
    re.DOTALL,
)


def _split_lines(page_html: str) -> List[Tuple[int, str]]:
    """Return [(line_number, inner_html)] for every line-container on the page, line order."""
    start = page_html.find('class="theme-light page')
    body = page_html[start:] if start >= 0 else page_html
    out = []
    for m in re.finditer(r'<div class="line-container" data-line="(\d+)">', body):
        line_no = int(m.group(1))
        nxt = body.find('<div class="line-container"', m.end())
        inner = body[m.end(): nxt if nxt >= 0 else len(body)]
        out.append((line_no, inner))
    return out


def _norm_word(text: str) -> str:
    """One word-position -> one whitespace-free token, so `words` stays index-aligned with the
    layout's word positions and `" ".join(words).split(" ")` round-trips exactly. Detached waqf
    marks that the source renders with an internal space are re-attached to their glyph."""
    return re.sub(r"\s+", "", text)


def load_ayah_id_map() -> Dict[Tuple[int, int], int]:
    """(surah_id, number_in_surah) -> global ayah id, from the existing ayahs.json id space."""
    with open(JSON_DIR / "ayahs.json", encoding="utf-8") as f:
        ayahs = json.load(f)
    return {(a["surah_id"], a["number_in_surah"]): a["id"] for a in ayahs}


def load_existing_ayah_ids() -> Set[int]:
    with open(JSON_DIR / "ayahs.json", encoding="utf-8") as f:
        return {a["id"] for a in json.load(f)}


# ---------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------


def build_from_pages(pages: Dict[int, str]) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
    ayah_id_map = load_ayah_id_map()

    ayah_words: Dict[Tuple[int, int], Dict[int, str]] = {}
    layout_rows: List[Dict[str, Any]] = []
    header_line: Dict[int, Tuple[int, int]] = {}
    bismillah_lines: List[Tuple[int, int]] = []

    for page in sorted(pages):
        html = pages[page]
        for line_no, inner in _split_lines(html):
            class_m = LINE_CLASS_RE.search(inner)
            classes = class_m.group(1) if class_m else ""

            # A line can be a surah-name banner, a bismillah band, or (commonly) both at once.
            # QUL's markup is inconsistent (bismillah sometimes shares the banner line, sometimes
            # sits on its own line, occasionally before the banner), so bismillah rows are
            # reconciled in a dedicated pass below rather than trusting per-line surah context.
            if "line--surah-name" in classes:
                icon = SURAH_ICON_RE.search(inner)
                if icon:
                    header_line[int(icon.group(1))] = (page, line_no)
                    layout_rows.append(_meta_row(page, line_no, "surah_header", int(icon.group(1))))
            if "line--bismillah" in classes:
                bismillah_lines.append((page, line_no))
            if "line--surah-name" in classes or "line--bismillah" in classes:
                continue

            words = WORD_SPAN_RE.findall(inner)
            if not words:
                continue  # empty spacer line

            seg: Optional[Dict[str, Any]] = None
            for surah_s, ayah_s, wpos_s, pos_s, text in words:
                surah, ayah_in_surah, wpos = int(surah_s), int(ayah_s), int(pos_s)
                ayah_words.setdefault((surah, ayah_in_surah), {})[wpos] = _norm_word(text)

                if seg is None or seg["_surah"] != surah or seg["_ayah"] != ayah_in_surah:
                    if seg is not None:
                        layout_rows.append(_finalize_seg(seg, ayah_id_map))
                    seg = {
                        "page_number": page, "line_number": line_no, "line_type": "ayah",
                        "_surah": surah, "_ayah": ayah_in_surah,
                        "first_word_position": wpos, "last_word_position": wpos,
                    }
                else:
                    seg["first_word_position"] = min(seg["first_word_position"], wpos)
                    seg["last_word_position"] = max(seg["last_word_position"], wpos)
            if seg is not None:
                layout_rows.append(_finalize_seg(seg, ayah_id_map))

    layout_rows += _basmalah_rows(bismillah_lines, header_line, layout_rows)
    layout_rows.sort(key=lambda r: (r["page_number"], r["line_number"], _TYPE_ORDER[r["line_type"]]))

    ayah_rows: List[Dict[str, Any]] = []
    for (surah, ayah_in_surah), wmap in ayah_words.items():
        gid = ayah_id_map.get((surah, ayah_in_surah))
        if gid is None:
            continue
        words = [wmap[p] for p in sorted(wmap)]
        ayah_rows.append({"ayah_id": gid, "text": " ".join(words), "words": words})
    ayah_rows.sort(key=lambda r: r["ayah_id"])

    return ayah_rows, layout_rows


def _basmalah_rows(
    bismillah_lines: List[Tuple[int, int]],
    header_line: Dict[int, Tuple[int, int]],
    layout_rows: List[Dict[str, Any]],
) -> List[Dict[str, Any]]:
    """Reconcile QUL's bismillah bands to exactly one basmalah row per surah (except 1 & 9).

    A bismillah always introduces the surah whose first ayah follows it, so its owner is the
    surah of the next ayah line in reading order — robust even when QUL renders the band before
    the name banner. Duplicate bands for one surah are collapsed. The rare surah whose bismillah
    QUL folds into the name banner (no separate band) gets one synthesised onto its header line,
    so every non-exempt surah opens with a basmalah for line-accurate rendering.
    """
    ayah_starts = sorted(
        ((r["page_number"], r["line_number"], r["surah_id"])
         for r in layout_rows if r["line_type"] == "ayah"),
        key=lambda t: (t[0], t[1]),
    )

    def owning_surah(pos: Tuple[int, int]) -> Optional[int]:
        for pg, ln, surah in ayah_starts:
            if (pg, ln) >= pos:
                return surah
        return None

    by_surah: Dict[int, Tuple[int, int]] = {}
    for pos in sorted(bismillah_lines):
        surah = owning_surah(pos)
        if surah is None or surah in NO_BASMALAH_SURAHS:
            continue
        by_surah.setdefault(surah, pos)

    for surah, (page, line) in header_line.items():
        if surah in NO_BASMALAH_SURAHS:
            continue
        by_surah.setdefault(surah, (page, line))

    return [_meta_row(pg, ln, "basmalah", surah) for surah, (pg, ln) in by_surah.items()]


def _meta_row(page: int, line_no: int, line_type: str, surah_id: int) -> Dict[str, Any]:
    return {
        "page_number": page, "line_number": line_no, "line_type": line_type,
        "surah_id": surah_id, "ayah_id": None,
        "first_word_position": None, "last_word_position": None,
    }


def _finalize_seg(seg: Dict[str, Any], ayah_id_map: Dict[Tuple[int, int], int]) -> Dict[str, Any]:
    return {
        "page_number": seg["page_number"], "line_number": seg["line_number"],
        "line_type": "ayah", "surah_id": seg["_surah"],
        "ayah_id": ayah_id_map.get((seg["_surah"], seg["_ayah"])),
        "first_word_position": seg["first_word_position"],
        "last_word_position": seg["last_word_position"],
    }


# ---------------------------------------------------------------------------
# Validation
# ---------------------------------------------------------------------------


def validate_text(rows: List[Dict[str, Any]], existing_ayah_ids: Set[int]) -> List[str]:
    errors = []
    row_ids = {r["ayah_id"] for r in rows}
    if len(rows) != EXPECTED_AYAHS:
        errors.append(f"expected {EXPECTED_AYAHS} rows, got {len(rows)}")
    if len(row_ids) != len(rows):
        errors.append("duplicate ayah_id rows present")
    missing = existing_ayah_ids - row_ids
    if missing:
        errors.append(f"{len(missing)} existing ayah_ids missing (e.g. {sorted(missing)[:5]})")
    extra = row_ids - existing_ayah_ids
    if extra:
        errors.append(f"{len(extra)} ayah_ids not in existing ayahs.json (e.g. {sorted(extra)[:5]})")
    empty_text = [r["ayah_id"] for r in rows if not r.get("text", "").strip()]
    if empty_text:
        errors.append(f"{len(empty_text)} rows have empty text (e.g. {empty_text[:5]})")
    no_words = [r["ayah_id"] for r in rows if not r.get("words")]
    if no_words:
        errors.append(f"{len(no_words)} rows have no words[] (e.g. {no_words[:5]})")
    bad_join = [r["ayah_id"] for r in rows
                if r.get("words") and r["text"].split(" ") != r["words"]]
    if bad_join:
        errors.append(f"{len(bad_join)} rows: text split != words[] (e.g. {bad_join[:5]})")
    empty_word = [r["ayah_id"] for r in rows if any(not w for w in r.get("words", []))]
    if empty_word:
        errors.append(f"{len(empty_word)} rows contain an empty word token (e.g. {empty_word[:5]})")
    return errors


def validate_alignment(ayah_rows, layout_rows) -> List[str]:
    """Every ayah's word positions must be exactly covered by its layout line-segments."""
    errors = []
    word_count = {r["ayah_id"]: len(r["words"]) for r in ayah_rows}
    covered: Dict[int, Set[int]] = {}
    for r in layout_rows:
        if r["line_type"] != "ayah":
            continue
        covered.setdefault(r["ayah_id"], set()).update(
            range(r["first_word_position"], r["last_word_position"] + 1)
        )
    for aid, n in word_count.items():
        want = set(range(1, n + 1))
        got = covered.get(aid, set())
        if got != want:
            errors.append(f"ayah {aid}: words={n} but layout covers {len(got)} positions")
    if errors:
        return [f"{len(errors)} ayahs: layout word-positions != words[] positions"] + errors[:5]
    return errors


def validate_layout(spec: LayoutSpec, rows: List[Dict[str, Any]]) -> List[str]:
    errors = []
    pages: Dict[int, Set[int]] = {}
    surah_ids: Set[int] = set()
    for r in rows:
        if r["line_type"] not in VALID_LINE_TYPES:
            errors.append(f"invalid line_type {r['line_type']!r} on page {r['page_number']}")
        pages.setdefault(r["page_number"], set()).add(r["line_number"])
        if r["surah_id"]:
            surah_ids.add(r["surah_id"])
        if r["line_type"] == "ayah" and r["ayah_id"] is None:
            errors.append(f"ayah line without ayah_id on page {r['page_number']} line {r['line_number']}")

    if len(pages) != spec.pages:
        errors.append(f"expected {spec.pages} pages, got {len(pages)}")
    missing_pages = set(range(1, spec.pages + 1)) - set(pages)
    if missing_pages:
        errors.append(f"{len(missing_pages)} pages missing (e.g. {sorted(missing_pages)[:5]})")
    over_limit = {p: max(lines) for p, lines in pages.items() if max(lines) > spec.lines}
    if over_limit:
        errors.append(f"{len(over_limit)} pages exceed {spec.lines} lines: {dict(list(over_limit.items())[:5])}")
    if len(surah_ids) != EXPECTED_SURAHS:
        errors.append(f"expected {EXPECTED_SURAHS} distinct surah_ids referenced, got {len(surah_ids)}")

    headers = [r["surah_id"] for r in rows if r["line_type"] == "surah_header"]
    if sorted(headers) != list(range(1, EXPECTED_SURAHS + 1)):
        errors.append(f"expected one surah_header per surah 1..114, got {len(headers)}")
    basmalah = Counter(r["surah_id"] for r in rows if r["line_type"] == "basmalah")
    expected_basmalah = set(range(1, EXPECTED_SURAHS + 1)) - NO_BASMALAH_SURAHS
    if set(basmalah) != expected_basmalah:
        errors.append(f"basmalah surahs mismatch: missing {sorted(expected_basmalah - set(basmalah))}, "
                      f"extra {sorted(set(basmalah) - expected_basmalah)}")
    dup = [s for s, c in basmalah.items() if c > 1]
    if dup:
        errors.append(f"duplicate basmalah rows for surahs {dup}")
    return errors


# ---------------------------------------------------------------------------
# Output
# ---------------------------------------------------------------------------


def text_path(text_source: str, asset: bool = False) -> Path:
    return (ASSET_DIR if asset else MUSHAF_JSON_DIR) / f"{text_source.lower()}_text.json"


def layout_path(key: str, asset: bool = False) -> Path:
    return (ASSET_DIR if asset else MUSHAF_JSON_DIR) / f"{key.lower()}_layout.json"


def _dump(path: Path, data, asset: bool) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        # Assets ship in the APK, so no indentation there; the in-repo copy stays
        # readable for review.
        if asset:
            json.dump(data, f, ensure_ascii=False, separators=(",", ":"))
        else:
            json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"    wrote {path.relative_to(BASE_DIR.parent)} "
          f"({len(data)} rows, {path.stat().st_size / 1024 / 1024:.1f} MB)")


def write_outputs(spec: LayoutSpec, ayah_rows, layout_rows) -> None:
    """Write this layout, plus its text source if that isn't already on disk.

    The glyph text is shared by every edition with the same `text_source`, so it is written
    once. If a text file is already there it must match exactly — a mismatch means the
    sharing assumption has broken for this edition, so we stop rather than silently
    overwrite another edition's glyphs with subtly different ones.
    """
    existing = text_path(spec.text_source)
    if existing.exists():
        prev = {r["ayah_id"]: r["text"] for r in json.loads(existing.read_text(encoding="utf-8"))}
        ours = {r["ayah_id"]: r["text"] for r in ayah_rows}
        if prev != ours:
            differing = [a for a in sorted(set(prev) & set(ours)) if prev[a] != ours[a]]
            raise SystemExit(
                f"{spec.key}: glyph text differs from the existing '{spec.text_source}' text "
                f"source in {len(differing)} ayahs (e.g. {differing[:5]}). Give this edition "
                f"its own text_source instead of sharing one."
            )
        print(f"    text source '{spec.text_source}' already written and identical — reusing")
    else:
        for asset in (False, True):
            _dump(text_path(spec.text_source, asset), ayah_rows, asset)

    for asset in (False, True):
        _dump(layout_path(spec.key, asset), layout_rows, asset)


def compare_text(spec: LayoutSpec, ayah_rows, other_key: str) -> None:
    """Report whether this layout's glyph text is identical to another script's.

    If it is, the two editions can share one text asset and only the layout differs — which
    roughly halves what a new edition costs in the APK.
    """
    other_source = LAYOUTS[other_key].text_source if other_key in LAYOUTS else other_key
    other = text_path(other_source)
    legacy = JSON_DIR / "ayahs_indopak.json"  # pre-generalisation name for INDOPAK_16
    src = other if other.exists() else (legacy if other_source == "INDOPAK" else None)
    if src is None or not src.exists():
        print(f"  [compare] no text file for {other_key}; skipping")
        return
    rows = json.loads(src.read_text(encoding="utf-8"))
    theirs = {r["ayah_id"]: r.get("text", r.get("text_indopak")) for r in rows}
    ours = {r["ayah_id"]: r["text"] for r in ayah_rows}
    common = set(theirs) & set(ours)
    diff = [a for a in sorted(common) if theirs[a] != ours[a]]
    wordcount_diff = [a for a in sorted(common)
                      if len(theirs[a].split(" ")) != len(ours[a].split(" "))]
    print(f"  [compare vs {other_key}] {len(common)} ayahs compared: "
          f"{len(diff)} differ in text, {len(wordcount_diff)} differ in word count")
    if diff:
        a = diff[0]
        print(f"      first differing ayah {a}:")
        print(f"        {other_key}: {theirs[a][:70]}")
        print(f"        {spec.key}: {ours[a][:70]}")
    if not diff:
        print(f"      -> identical: {spec.key} can SHARE {other_key}'s text asset")
    elif not wordcount_diff:
        print(f"      -> same segmentation, different glyphs: needs its own text asset")
    else:
        print(f"      -> different segmentation: needs its own text asset")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------


def run(spec: LayoutSpec, page_start: int, page_end: int, dev: bool,
        compare: Optional[str]) -> int:
    print("=" * 64)
    print(f"{spec.key} — {spec.label}")
    print(f"  source: {spec.url}  pages {page_start}..{page_end} ({spec.lines} lines/page)")
    print("=" * 64)

    pages = fetch_pages(spec, page_start, page_end)
    ayah_rows, layout_rows = build_from_pages(pages)
    print(f"  parsed: {len(ayah_rows)} ayahs, {len(layout_rows)} layout rows")

    if compare:
        compare_text(spec, ayah_rows, compare)

    if dev:
        print("  [dev mode] skipping full validation + write. Sample:")
        print("   ", json.dumps(ayah_rows[:1], ensure_ascii=False)[:300])
        print("   ", json.dumps(layout_rows[:4], ensure_ascii=False)[:400])
        return 0

    existing = load_existing_ayah_ids()
    errs = {
        "text": validate_text(ayah_rows, existing),
        "layout": validate_layout(spec, layout_rows),
        "alignment": validate_alignment(ayah_rows, layout_rows),
    }
    failed = False
    for name, e in errs.items():
        if e:
            failed = True
            print(f"\n[FAIL] {name} validation:")
            for line in e:
                print(f"  - {line}")
    if failed:
        return 1

    write_outputs(spec, ayah_rows, layout_rows)
    print(f"\n[OK] {spec.key} generated and validated.")
    print("     Confirm nimaz-pro-data/json/LICENSES_MUSHAF_LAYOUTS.md covers this source.")
    return 0


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__,
                                formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("script", nargs="?", help="script key, e.g. INDOPAK_15")
    p.add_argument("--pages", nargs=2, type=int, metavar=("START", "END"),
                   help="dev mode: fetch only this page range, skip validation/write")
    p.add_argument("--compare", metavar="SCRIPT_KEY",
                   help="report whether this layout's glyph text matches another script's")
    p.add_argument("--list", action="store_true", help="list known layouts and exit")
    args = p.parse_args()

    if args.list or not args.script:
        print("Known layouts:")
        for spec in LAYOUTS.values():
            print(f"  {spec.key:12s} QUL#{spec.qul_id:<4d} {spec.pages:4d} pages, "
                  f"{spec.lines:2d} lines — {spec.label}")
        return 0

    key = args.script.upper()
    if key not in LAYOUTS:
        raise SystemExit(f"unknown script {key!r}; known: {', '.join(LAYOUTS)}")
    spec = LAYOUTS[key]

    if args.pages:
        return run(spec, args.pages[0], args.pages[1], dev=True, compare=args.compare)
    return run(spec, 1, spec.pages, dev=False, compare=args.compare)


if __name__ == "__main__":
    sys.exit(main())
