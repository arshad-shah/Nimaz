#!/usr/bin/env python3
"""
Nimaz - 16-line IndoPak Mushaf data acquisition (issue #265, sub-task 1/7 of #263)

Source: Quranic Universal Library (QUL / Tarteel.ai), resource
"Indopak 16 lines layout (Taj company)" — https://qul.tarteel.ai/resources/mushaf-layout/11
QUL publishes this layout as open, structured line-break data for building Quran apps (it is the
data behind Quran for Android/iOS and Tarteel). We consume the public per-page preview, which
renders, for every page, each line and every word with its `surah:ayah:word` location and the
IndoPak glyph text. This is QUL's own structured line-break determination — NOT a scan or a copy
of a commercial edition's typeset artwork. See nimaz-pro-data/json/LICENSES_INDOPAK.md.

It produces and validates two files against the acceptance criteria in issue #265:
  - nimaz-pro-data/json/ayahs_indopak.json           (6,236 rows, keyed by existing ayah_id)
  - nimaz-pro-data/json/mushaf_layout_indopak16.json (548 pages, each with <=16 lines)

Usage:
  python3 download_indopak_mushaf_data.py            # fetch all 548 pages, build + validate + write
  python3 download_indopak_mushaf_data.py 1 3        # fetch pages 1..3 only (dev/inspection)
"""

import json
import os
import re
import sys
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from typing import Any, Dict, List, Optional, Set, Tuple

BASE_DIR = Path(__file__).parent.parent
JSON_DIR = BASE_DIR / "json"
CACHE_DIR = Path(os.environ.get("INDOPAK_CACHE_DIR", "/tmp/indopak_pages"))

MUSHAF_RESOURCE_URL = "https://qul.tarteel.ai/resources/mushaf-layout/11"

EXPECTED_SURAHS = 114
EXPECTED_AYAHS = 6236
EXPECTED_PAGES = 548
MAX_LINES_PER_PAGE = 16

VALID_LINE_TYPES = {"ayah", "surah_header", "basmalah"}

# ---------------------------------------------------------------------------
# HTTP
# ---------------------------------------------------------------------------


def download_page_html(page: int, retries: int = 4) -> str:
    cache = CACHE_DIR / f"page_{page:03d}.html"
    if cache.exists():
        return cache.read_text(encoding="utf-8")
    url = f"{MUSHAF_RESOURCE_URL}?page={page}"
    last_err: Optional[Exception] = None
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=90) as response:
                html = response.read().decode("utf-8")
            CACHE_DIR.mkdir(parents=True, exist_ok=True)
            cache.write_text(html, encoding="utf-8")
            return html
        except Exception as e:  # noqa: BLE001 - best-effort acquisition script
            last_err = e
            time.sleep(2 ** attempt)
    raise RuntimeError(f"failed to fetch page {page}: {last_err}")


# ---------------------------------------------------------------------------
# Parsing
# ---------------------------------------------------------------------------

# the inner <div class="line ..."> opening tag (to read modifier classes)
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
    # Narrow to the page body to avoid matching unrelated markup.
    start = page_html.find('class="theme-light page')
    body = page_html[start:] if start >= 0 else page_html
    out = []
    for m in re.finditer(r'<div class="line-container" data-line="(\d+)">', body):
        line_no = int(m.group(1))
        # inner html = from end of this opening tag to the start of the next line-container
        nxt = body.find('<div class="line-container"', m.end())
        inner = body[m.end(): nxt if nxt >= 0 else len(body)]
        out.append((line_no, inner))
    return out


# ---------------------------------------------------------------------------
# Build
# ---------------------------------------------------------------------------


def load_ayah_id_map() -> Dict[Tuple[int, int], int]:
    """(surah_id, number_in_surah) -> global ayah id, from the existing ayahs.json id space."""
    with open(JSON_DIR / "ayahs.json", encoding="utf-8") as f:
        ayahs = json.load(f)
    return {(a["surah_id"], a["number_in_surah"]): a["id"] for a in ayahs}


def _norm_word(text: str) -> str:
    """One word-position -> one whitespace-free token, so `words` stays index-aligned with the
    layout's word positions and `" ".join(words).split(" ")` round-trips exactly. Detached waqf
    marks that the source renders with an internal space are re-attached to their glyph."""
    return re.sub(r"\s+", "", text)


def build_from_pages(pages: Dict[int, str]) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
    ayah_id_map = load_ayah_id_map()

    # (surah, ayah_in_surah) -> {word_position: token}
    ayah_words: Dict[Tuple[int, int], Dict[int, str]] = {}
    layout_rows: List[Dict[str, Any]] = []
    header_line: Dict[int, Tuple[int, int]] = {}          # surah -> (page, line) of its name banner
    bismillah_lines: List[Tuple[int, int]] = []           # (page, line) of every bismillah band

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

            # group consecutive words on this line by ayah, in source order
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

    # assemble ayahs_indopak.json — words[] indexed by position, text_indopak = join
    ayah_rows: List[Dict[str, Any]] = []
    for (surah, ayah_in_surah), wmap in ayah_words.items():
        gid = ayah_id_map.get((surah, ayah_in_surah))
        if gid is None:
            continue
        words = [wmap[p] for p in sorted(wmap)]
        ayah_rows.append({
            "ayah_id": gid,
            "text_indopak": " ".join(words),
            "words": words,
        })
    ayah_rows.sort(key=lambda r: r["ayah_id"])

    return ayah_rows, layout_rows


_TYPE_ORDER = {"surah_header": 0, "basmalah": 1, "ayah": 2}
# Surah 1 counts the basmalah as ayah 1; surah 9 (At-Tawbah) has no basmalah.
NO_BASMALAH_SURAHS = {1, 9}


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
        by_surah.setdefault(surah, pos)  # keep earliest band per surah

    for surah, (page, line) in header_line.items():
        if surah in NO_BASMALAH_SURAHS:
            continue
        by_surah.setdefault(surah, (page, line))  # synthesise onto banner line if none marked

    return [_meta_row(pg, ln, "basmalah", surah) for surah, (pg, ln) in by_surah.items()]


def _meta_row(page: int, line_no: int, line_type: str, surah_id: int) -> Dict[str, Any]:
    return {
        "page_number": page,
        "line_number": line_no,
        "line_type": line_type,
        "surah_id": surah_id,
        "ayah_id": None,
        "first_word_position": None,
        "last_word_position": None,
    }


def _finalize_seg(seg: Dict[str, Any], ayah_id_map: Dict[Tuple[int, int], int]) -> Dict[str, Any]:
    gid = ayah_id_map.get((seg["_surah"], seg["_ayah"]))
    return {
        "page_number": seg["page_number"],
        "line_number": seg["line_number"],
        "line_type": "ayah",
        "surah_id": seg["_surah"],
        "ayah_id": gid,
        "first_word_position": seg["first_word_position"],
        "last_word_position": seg["last_word_position"],
    }


# ---------------------------------------------------------------------------
# Validation
# ---------------------------------------------------------------------------


def validate_ayahs_indopak(rows: List[Dict[str, Any]], existing_ayah_ids: Set[int]) -> List[str]:
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
    empty_text = [r["ayah_id"] for r in rows if not r.get("text_indopak", "").strip()]
    if empty_text:
        errors.append(f"{len(empty_text)} rows have empty text_indopak (e.g. {empty_text[:5]})")
    no_words = [r["ayah_id"] for r in rows if not r.get("words")]
    if no_words:
        errors.append(f"{len(no_words)} rows have no words[] (e.g. {no_words[:5]})")
    bad_join = [r["ayah_id"] for r in rows
                if r.get("words") and r["text_indopak"].split(" ") != r["words"]]
    if bad_join:
        errors.append(f"{len(bad_join)} rows: text_indopak split != words[] (e.g. {bad_join[:5]})")
    empty_word = [r["ayah_id"] for r in rows if any(not w for w in r.get("words", []))]
    if empty_word:
        errors.append(f"{len(empty_word)} rows contain an empty word token (e.g. {empty_word[:5]})")
    return errors


def validate_alignment(ayah_rows: List[Dict[str, Any]], layout_rows: List[Dict[str, Any]]) -> List[str]:
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
            errors.append(
                f"ayah {aid}: words={n} but layout covers {sorted(got)[:3]}..{sorted(got)[-3:] if got else []}"
            )
    if errors:
        return [f"{len(errors)} ayahs: layout word-positions != words[] positions"] + errors[:5]
    return errors


def validate_mushaf_layout(rows: List[Dict[str, Any]]) -> List[str]:
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

    if len(pages) != EXPECTED_PAGES:
        errors.append(f"expected {EXPECTED_PAGES} pages, got {len(pages)}")
    missing_pages = set(range(1, EXPECTED_PAGES + 1)) - set(pages)
    if missing_pages:
        errors.append(f"{len(missing_pages)} pages missing (e.g. {sorted(missing_pages)[:5]})")
    over_limit = {p: max(lines) for p, lines in pages.items() if max(lines) > MAX_LINES_PER_PAGE}
    if over_limit:
        errors.append(f"{len(over_limit)} pages exceed {MAX_LINES_PER_PAGE} lines: {dict(list(over_limit.items())[:5])}")
    if len(surah_ids) != EXPECTED_SURAHS:
        errors.append(f"expected {EXPECTED_SURAHS} distinct surah_ids referenced, got {len(surah_ids)}")

    headers = [r["surah_id"] for r in rows if r["line_type"] == "surah_header"]
    if sorted(headers) != list(range(1, EXPECTED_SURAHS + 1)):
        errors.append(f"expected one surah_header per surah 1..114, got {len(headers)}")
    from collections import Counter
    basmalah = Counter(r["surah_id"] for r in rows if r["line_type"] == "basmalah")
    expected_basmalah = {s for s in range(1, EXPECTED_SURAHS + 1)} - NO_BASMALAH_SURAHS
    if set(basmalah) != expected_basmalah:
        errors.append(f"basmalah surahs mismatch: missing {sorted(expected_basmalah - set(basmalah))}, "
                      f"extra {sorted(set(basmalah) - expected_basmalah)}")
    dup = [s for s, c in basmalah.items() if c > 1]
    if dup:
        errors.append(f"duplicate basmalah rows for surahs {dup}")
    return errors


def load_existing_ayah_ids() -> Set[int]:
    with open(JSON_DIR / "ayahs.json", encoding="utf-8") as f:
        ayahs = json.load(f)
    return {a["id"] for a in ayahs}


def save_json(data: Any, filename: str) -> None:
    with open(JSON_DIR / filename, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"    Saved: {filename} ({len(data)} rows)")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------


def fetch_pages(page_start: int, page_end: int, workers: int = 8) -> Dict[int, str]:
    pages: Dict[int, str] = {}
    todo = list(range(page_start, page_end + 1))
    with ThreadPoolExecutor(max_workers=workers) as ex:
        futures = {ex.submit(download_page_html, p): p for p in todo}
        done = 0
        for fut in as_completed(futures):
            p = futures[fut]
            pages[p] = fut.result()
            done += 1
            if done % 25 == 0 or done == len(todo):
                print(f"    fetched {done}/{len(todo)} pages")
    return pages


def main() -> None:
    args = sys.argv[1:]
    if len(args) == 2:
        page_start, page_end = int(args[0]), int(args[1])
        dev = True
    else:
        page_start, page_end = 1, EXPECTED_PAGES
        dev = False

    print("=" * 64)
    print("16-line IndoPak Mushaf data acquisition (issue #265)")
    print(f"  source: {MUSHAF_RESOURCE_URL}  pages {page_start}..{page_end}")
    print("=" * 64)

    pages = fetch_pages(page_start, page_end)
    ayah_rows, layout_rows = build_from_pages(pages)
    print(f"  parsed: {len(ayah_rows)} ayahs, {len(layout_rows)} layout rows")

    if dev:
        print("  [dev mode] skipping full validation + write. Sample:")
        print("   ", json.dumps(ayah_rows[:2], ensure_ascii=False))
        print("   ", json.dumps(layout_rows[:6], ensure_ascii=False))
        return

    existing_ayah_ids = load_existing_ayah_ids()
    ayah_errors = validate_ayahs_indopak(ayah_rows, existing_ayah_ids)
    layout_errors = validate_mushaf_layout(layout_rows)
    align_errors = validate_alignment(ayah_rows, layout_rows)

    if ayah_errors:
        print("\n[FAIL] ayahs_indopak.json validation:")
        for e in ayah_errors:
            print(f"  - {e}")
    if layout_errors:
        print("\n[FAIL] mushaf_layout_indopak16.json validation:")
        for e in layout_errors:
            print(f"  - {e}")
    if align_errors:
        print("\n[FAIL] text/layout alignment validation:")
        for e in align_errors:
            print(f"  - {e}")
    if ayah_errors or layout_errors or align_errors:
        raise SystemExit(1)

    save_json(ayah_rows, "ayahs_indopak.json")
    save_json(layout_rows, "mushaf_layout_indopak16.json")
    print("\n[OK] Both files generated and validated against the #265 acceptance criteria.")
    print("     Confirm nimaz-pro-data/json/LICENSES_INDOPAK.md reflects the source used.")


if __name__ == "__main__":
    main()
