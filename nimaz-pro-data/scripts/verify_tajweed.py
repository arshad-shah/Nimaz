#!/usr/bin/env python3
"""
Tajweed data validation harness (issue #292).

Nothing used to check the tajweed data, so every defect in the parent issue
(#287) shipped silently. This asserts the data is correct at two levels:

* **pipeline mode** (default) — runs the pre-parser + taxonomy split + canonical
  alignment over the JSON sources (``tajweed.json`` + ``ayahs.json`` +
  ``tajweed_cpfair.json``) in memory. This is the source of truth for the next
  DB regeneration and needs no Git-LFS checkout, so it is the CI-friendly path.
* **db mode** (``--db PATH``) — verifies a generated ``nimaz_prepopulated.db``'s
  ``ayahs.text_tajweed`` column. This is the only way to check what LFS actually
  ships; run it as a post-step of ``generate_database.py`` and on device.

Exits non-zero on any violation. Run:

    python3 nimaz-pro-data/scripts/verify_tajweed.py           # pipeline mode
    python3 nimaz-pro-data/scripts/verify_tajweed.py --db out.db

Checks
------
1. Coverage        — every ayah has a non-empty span list (allow-list: the
                     known-unannotated ayahs, #298, which must only shrink).
2. Well-formedness — valid JSON; every segment has ``t``; no segment text
                     contains ``<``, ``>`` or ``tajweed``.
3. Round-trip      — ``''.join(seg.t) == normalise_uthmani(text_arabic)`` (#290).
4. Code whitelist  — every non-null ``r`` is a v3 code; no legacy v1/v2 codes.
5. Conservation    — every source rule code survives the whole pipeline
                     (character coverage; pipeline mode only) (#288/#290).
6. Cross-source    — source-tag vs cpfair per-category signed deltas match the
                     reviewed drift allow-list. This is **drift detection, not
                     independent validation**: the two datasets share provenance
                     (identical 63-ayah gap), so agreement only proves agreement.
7. Golden ayahs    — a pinned fixture of hand-verified expected output.

A per-code span summary is always printed so regressions are visible in the log
even when the run passes.
"""

import argparse
import collections
import json
import re
import sys
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent
JSON_DIR = SCRIPTS_DIR.parent / "json"
FIXTURES = SCRIPTS_DIR / "tests" / "fixtures"
sys.path.insert(0, str(SCRIPTS_DIR))

from preparse_tajweed import (  # noqa: E402
    align_segments_to_canonical,
    load_cpfair,
    normalise_uthmani,
    preparse_tajweed,
)

# Final v3 codes that may appear in shipped output.
V3_CODES = {
    "g", "if", "is", "dg", "dn", "ds", "dj", "dk", "qs", "qk",
    "mn", "mf", "mt", "ma", "ml", "my", "l", "ls", "sl", "hw",
}
# Deprecated codes that must NOT appear in freshly generated data.
LEGACY_CODES = {"mo", "mp", "q", "i", "d", "m", "s"}

# Source tag class → cpfair rule(s), for the cross-source drift check.
CROSS_SOURCE_CATEGORIES = {
    "ghunnah": (["ghunnah"], ["ghunnah"]),
    "ikhfa": (["ikhafa"], ["ikhfa"]),
    "ikhfa_shafawi": (["ikhafa_shafawi"], ["ikhfa_shafawi"]),
    "idgham_ghunnah": (["idgham_ghunnah"], ["idghaam_ghunnah"]),
    "idgham_no_ghunnah": (["idgham_wo_ghunnah"], ["idghaam_no_ghunnah"]),
    "idgham_shafawi": (["idgham_shafawi"], ["idghaam_shafawi"]),
    "idgham_mutajanisayn": (["idgham_mutajanisayn"], ["idghaam_mutajanisayn"]),
    "idgham_mutaqaribayn": (["idgham_mutaqaribayn"], ["idghaam_mutaqaribayn"]),
    "iqlab": (["iqlab"], ["iqlab"]),
    "lam_shamsiyyah": (["laam_shamsiyah"], ["lam_shamsiyyah"]),
    "silent": (["slnt"], ["silent"]),
    "hamzat_wasl": (["ham_wasl"], ["hamzat_wasl"]),
    "madd_2": (["madda_normal"], ["madd_2"]),
    "madd_246": (["madda_permissible"], ["madd_246"]),
    "madd_6": (["madda_necessary"], ["madd_6"]),
    "madd_obligatory_split": (["madda_obligatory"], ["madd_munfasil", "madd_muttasil"]),
    "qalqalah": (["qalaqah"], ["qalqalah"]),
}

TAG_CLASS = re.compile(r'<tajweed\s+class=(["\']?)([^"\'>\s]+)\1\s*>')


class Report:
    def __init__(self):
        self.failures = []
        self.code_counts = collections.Counter()

    def fail(self, check, message):
        self.failures.append((check, message))

    def ok(self):
        return not self.failures

    def print_summary(self):
        print("\nSpan counts per rule code:")
        for code in sorted(self.code_counts):
            print(f"  {code:4s} {self.code_counts[code]}")
        if self.failures:
            print(f"\n{len(self.failures)} FAILURE(S):")
            for check, msg in self.failures[:60]:
                print(f"  [{check}] {msg}")
            if len(self.failures) > 60:
                print(f"  … and {len(self.failures) - 60} more")
        else:
            print("\nOK: all tajweed checks passed.")


def _load_json(name):
    with open(JSON_DIR / name, encoding="utf-8") as f:
        return json.load(f)


def _load_fixture(name):
    with open(FIXTURES / name, encoding="utf-8") as f:
        return json.load(f)


def _check_segments(report, key, segments, text_arabic, empty_allow):
    """Checks 1–4 on one ayah's segments (works in both pipeline and DB mode)."""
    # 2. well-formedness
    for seg in segments:
        if "t" not in seg:
            report.fail("well-formed", f"{key}: segment missing 't': {seg}")
            return
        text = seg["t"]
        if "<" in text or ">" in text or "tajweed" in text:
            report.fail("well-formed", f"{key}: leaked markup in segment {text!r}")

    # 1. coverage
    has_rule = any(s.get("r") for s in segments)
    if not has_rule and key not in empty_allow:
        report.fail("coverage", f"{key}: no rule spans (not in the #298 allow-list)")

    # 3. round-trip
    joined = "".join(s["t"] for s in segments)
    canonical = normalise_uthmani(text_arabic)
    if joined != canonical:
        report.fail("round-trip", f"{key}: strip(tajweed) != text_arabic")

    # 4. code whitelist
    for seg in segments:
        r = seg.get("r")
        if r is None:
            continue
        report.code_counts[r] += 1
        if r in LEGACY_CODES:
            report.fail("code-whitelist", f"{key}: legacy code {r!r} in fresh output")
        elif r not in V3_CODES:
            report.fail("code-whitelist", f"{key}: unknown code {r!r}")


def _check_cross_source(report, tajweed, cpfair):
    """Check 6: source-tag vs cpfair signed deltas match the drift allow-list."""
    try:
        allow = _load_fixture("cpfair_drift_allowlist.json")["deltas"]
    except FileNotFoundError:
        report.fail("cross-source", "cpfair_drift_allowlist.json missing")
        return
    qcount = collections.Counter()
    for v in tajweed.values():
        for m in TAG_CLASS.finditer(v):
            qcount[m.group(2)] += 1
    ccount = collections.Counter()
    for e in cpfair:
        for a in e["annotations"]:
            ccount[a["rule"]] += 1
    for name, (q_classes, c_rules) in CROSS_SOURCE_CATEGORIES.items():
        q = sum(qcount[x] for x in q_classes)
        c = sum(ccount[x] for x in c_rules)
        delta = q - c
        expected = allow.get(name, {}).get("delta")
        if expected is None:
            report.fail("cross-source", f"{name}: no allow-list entry")
        elif delta != expected:
            report.fail(
                "cross-source",
                f"{name}: delta {delta:+d} (quran {q} vs cpfair {c}), "
                f"allow-list expects {expected:+d} — cross-source drift",
            )


def _check_golden(report, segments_by_key):
    """Check 7: golden ayahs match the pinned fixture."""
    try:
        golden = _load_fixture("golden_ayahs.json")
    except FileNotFoundError:
        report.fail("golden", "golden_ayahs.json missing")
        return
    for key, expected in golden.items():
        actual = segments_by_key.get(key)
        if actual is None:
            report.fail("golden", f"{key}: absent from data")
        elif actual != expected:
            report.fail("golden", f"{key}: segments differ from golden fixture")


def verify_pipeline():
    """Run all checks against the in-memory pipeline over the JSON sources."""
    report = Report()
    tajweed = _load_json("tajweed.json")
    ayahs = _load_json("ayahs.json")
    canon_by_key = {f"{a['surah_id']}:{a['number_in_surah']}": a["text_arabic"]
                    for a in ayahs}
    cpfair_list = _load_json("tajweed_cpfair.json")
    cpfair_by_key = {f"{e['surah']}:{e['ayah']}": e for e in cpfair_list}
    empty_allow = set(_load_fixture("unannotated_ayahs.json")["ayahs"])

    segments_by_key = {}
    for a in ayahs:
        key = f"{a['surah_id']}:{a['number_in_surah']}"
        raw = tajweed.get(key)
        if raw is None:
            report.fail("coverage", f"{key}: no tajweed source row")
            continue
        pre = preparse_tajweed(raw, key=key, cpfair_entry=cpfair_by_key.get(key))
        post = align_segments_to_canonical(pre, normalise_uthmani(canon_by_key[key]))
        segments_by_key[key] = post
        _check_segments(report, key, post, canon_by_key[key], empty_allow)

        # 5. conservation — every source rule code survives the pipeline.
        pre_codes = {s["r"] for s in pre if s["r"]}
        post_codes = {s["r"] for s in post if s["r"]}
        if pre_codes - post_codes:
            report.fail("conservation",
                        f"{key}: rule code(s) lost in alignment: "
                        f"{sorted(pre_codes - post_codes)}")

    # coverage allow-list must only shrink (no ayah outside it may be empty)
    current_empty = {k for k, segs in segments_by_key.items()
                     if not any(s.get("r") for s in segs)}
    for extra in sorted(current_empty - empty_allow):
        report.fail("coverage", f"{extra}: newly unannotated, not in allow-list")

    _check_cross_source(report, tajweed, cpfair_list)
    _check_golden(report, segments_by_key)
    return report


def verify_db(db_path):
    """Run the segment checks against a generated sqlite DB's ayahs table."""
    import sqlite3
    report = Report()
    ayahs = _load_json("ayahs.json")
    canon_by_key = {f"{a['surah_id']}:{a['number_in_surah']}": a["text_arabic"]
                    for a in ayahs}
    empty_allow = set(_load_fixture("unannotated_ayahs.json")["ayahs"])
    segments_by_key = {}
    conn = sqlite3.connect(db_path)
    try:
        rows = conn.execute(
            "SELECT surah_id, number_in_surah, text_tajweed FROM ayahs"
        ).fetchall()
    finally:
        conn.close()
    for surah, num, text_tajweed in rows:
        key = f"{surah}:{num}"
        if not text_tajweed:
            if key not in empty_allow:
                report.fail("coverage", f"{key}: null/empty text_tajweed")
            continue
        try:
            segments = json.loads(text_tajweed)
        except json.JSONDecodeError as e:
            report.fail("well-formed", f"{key}: invalid JSON ({e})")
            continue
        segments_by_key[key] = segments
        _check_segments(report, key, segments, canon_by_key.get(key, ""), empty_allow)
    _check_golden(report, segments_by_key)
    return report


def main():
    parser = argparse.ArgumentParser(description="Verify tajweed data (#292).")
    parser.add_argument("--db", help="path to a generated sqlite DB to verify")
    args = parser.parse_args()

    report = verify_db(args.db) if args.db else verify_pipeline()
    report.print_summary()
    return 0 if report.ok() else 1


if __name__ == "__main__":
    sys.exit(main())
