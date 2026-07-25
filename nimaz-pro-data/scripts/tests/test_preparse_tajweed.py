#!/usr/bin/env python3
"""
Unit tests for the tajweed pre-parser (``preparse_tajweed.py``).

Covers the sub-issue #288 acceptance cases: nested tags, malformed source,
unknown classes, empty content and leading/trailing whitespace, plus the
character-level round-trip invariant over the full shipped source.

Run with either::

    python3 -m pytest nimaz-pro-data/scripts/tests/test_preparse_tajweed.py
    python3 nimaz-pro-data/scripts/tests/test_preparse_tajweed.py   # unittest fallback
"""

import json
import sys
import unittest
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
JSON_DIR = SCRIPTS_DIR.parent / "json"
sys.path.insert(0, str(SCRIPTS_DIR))

from preparse_tajweed import (  # noqa: E402
    RULE_CODES,
    TAG_CLOSE,
    TAG_OPEN_PATTERN,
    END_SPAN_PATTERN,
    align_segments_to_canonical,
    apply_derived_rules,
    apply_source_fixups,
    cpfair_madd_sequence,
    normalise_uthmani,
    preparse_tajweed,
    reclassify_madd_obligatory,
    split_qalqalah,
)


def _strip_reference(key, html):
    """Independent 'ground truth': raw HTML with fix-ups applied and every
    tag / verse-marker removed. Used for the round-trip invariant."""
    html = apply_source_fixups(key, html)
    html = END_SPAN_PATTERN.sub("", html)
    html = TAG_OPEN_PATTERN.sub("", html).replace(TAG_CLOSE, "")
    return html.strip()


class TestBasicSegments(unittest.TestCase):
    def test_empty_and_none(self):
        self.assertEqual(preparse_tajweed(""), [])
        self.assertEqual(preparse_tajweed(None), [])

    def test_plain_text_only(self):
        self.assertEqual(
            preparse_tajweed("بِسْمِ"),
            [{"t": "بِسْمِ", "r": None}],
        )

    def test_single_rule(self):
        self.assertEqual(
            preparse_tajweed("<tajweed class=ghunnah>نّ</tajweed>"),
            [{"t": "نّ", "r": "g"}],
        )

    def test_plain_then_rule_then_plain(self):
        self.assertEqual(
            preparse_tajweed("ab<tajweed class=ghunnah>x</tajweed>cd"),
            [
                {"t": "ab", "r": None},
                {"t": "x", "r": "g"},
                {"t": "cd", "r": None},
            ],
        )

    def test_end_span_stripped(self):
        # Verse-number markers are removed entirely.
        self.assertEqual(
            preparse_tajweed("مٱ<span class=end>١٩٠</span>"),
            [{"t": "مٱ", "r": None}],
        )

    def test_verse_marker_letters_are_plain(self):
        # 'end' maps to None → its characters are emitted as plain text.
        self.assertIsNone(RULE_CODES["end"])


class TestWhitespace(unittest.TestCase):
    def test_leading_and_trailing_whitespace_stripped_symmetrically(self):
        self.assertEqual(
            preparse_tajweed("   <tajweed class=ghunnah>x</tajweed>   "),
            [{"t": "x", "r": "g"}],
        )

    def test_internal_word_spacing_preserved(self):
        # Spaces between words must survive (segments are concatenated on render).
        segs = preparse_tajweed("فِى سَبِيلِ")
        self.assertEqual("".join(s["t"] for s in segs), "فِى سَبِيلِ")


class TestNestedTags(unittest.TestCase):
    def test_innermost_wins_outer_keeps_rest(self):
        # The 2:190 shape: madda_obligatory wrapping a nested slnt.
        # Without a cpfair entry, madda_obligatory maps to "mt" (v3 fallback).
        result = preparse_tajweed(
            "a<tajweed class=madda_obligatory>b"
            "<tajweed class=slnt>c</tajweed>d</tajweed>e"
        )
        self.assertEqual(
            result,
            [
                {"t": "a", "r": None},
                {"t": "b", "r": "mt"},   # outer, before inner
                {"t": "c", "r": "sl"},   # inner wins
                {"t": "d", "r": "mt"},   # outer, after inner
                {"t": "e", "r": None},
            ],
        )

    def test_outer_fully_covered_by_inner_contributes_no_segment(self):
        result = preparse_tajweed(
            "<tajweed class=madda_normal>"
            "<tajweed class=slnt>x</tajweed></tajweed>"
        )
        self.assertEqual(result, [{"t": "x", "r": "sl"}])

    def test_real_2_190_has_no_leaked_markup(self):
        with open(JSON_DIR / "tajweed.json", encoding="utf-8") as fh:
            taj = json.load(fh)
        segs = preparse_tajweed(taj["2:190"], key="2:190")
        self.assertFalse(any("<" in s["t"] or ">" in s["t"] for s in segs))
        # the previously-dropped inner slnt span is recovered
        self.assertIn("sl", {s["r"] for s in segs})


class TestFailLoud(unittest.TestCase):
    def test_unknown_class_raises(self):
        with self.assertRaises(ValueError) as ctx:
            preparse_tajweed("<tajweed class=bogus_rule>x</tajweed>", key="1:1")
        self.assertIn("bogus_rule", str(ctx.exception))
        self.assertIn("1:1", str(ctx.exception))

    def test_unclosed_tag_raises(self):
        with self.assertRaises(ValueError):
            preparse_tajweed("<tajweed class=ghunnah>x")

    def test_stray_closing_tag_raises(self):
        with self.assertRaises(ValueError):
            preparse_tajweed("x</tajweed>")

    def test_malformed_source_before_fixup_raises(self):
        # The raw 32:3 fragment (broken opening tag) must not parse silently.
        with self.assertRaises(ValueError):
            preparse_tajweed("أَمْ فْتَرَ>ٮٰ</tajweed>هُ")


class TestSourceFixups(unittest.TestCase):
    def test_32_3_fixup_applied(self):
        with open(JSON_DIR / "tajweed.json", encoding="utf-8") as fh:
            taj = json.load(fh)
        segs = preparse_tajweed(taj["32:3"], key="32:3")
        self.assertFalse(any("<" in s["t"] or ">" in s["t"] for s in segs))

    def test_fixup_is_idempotent_when_source_already_correct(self):
        # A corrected upstream re-fetch (no broken fragment) must no-op.
        already_fixed = "فْتَرَ<tajweed class=madda_normal>ٮٰ</tajweed>هُ"
        self.assertEqual(apply_source_fixups("32:3", already_fixed), already_fixed)


class TestFullSourceInvariants(unittest.TestCase):
    """Whole-corpus guarantees over the shipped tajweed.json."""

    @classmethod
    def setUpClass(cls):
        with open(JSON_DIR / "tajweed.json", encoding="utf-8") as fh:
            cls.taj = json.load(fh)

    def test_all_ayahs_parse(self):
        for key, html in self.taj.items():
            preparse_tajweed(html, key=key)  # must not raise

    def test_no_leaked_markup_anywhere(self):
        offenders = []
        for key, html in self.taj.items():
            for seg in preparse_tajweed(html, key=key):
                if "<" in seg["t"] or ">" in seg["t"]:
                    offenders.append(key)
                    break
        self.assertEqual(offenders, [])

    def test_character_roundtrip(self):
        # Concatenated segment text must reconstruct the source exactly:
        # no character is ever dropped.
        mismatches = []
        for key, html in self.taj.items():
            joined = "".join(s["t"] for s in preparse_tajweed(html, key=key))
            if joined != _strip_reference(key, html):
                mismatches.append(key)
        self.assertEqual(mismatches, [])

    def test_recovered_inner_span_counts_match_source(self):
        # The inner spans that the old regex dropped (ghunnah, slnt) now
        # exactly equal their raw tag counts in the source.
        import collections
        raw = collections.Counter()
        for html in self.taj.values():
            for m in TAG_OPEN_PATTERN.finditer(html):
                raw[m.group(2).lower()] += 1
        seg = collections.Counter()
        for key, html in self.taj.items():
            for s in preparse_tajweed(html, key=key):
                if s["r"]:
                    seg[s["r"]] += 1
        self.assertEqual(seg["g"], raw["ghunnah"])
        self.assertEqual(seg["sl"], raw["slnt"])


class TestNormaliseUthmani(unittest.TestCase):
    def test_strips_bom(self):
        self.assertEqual(normalise_uthmani("﻿بِسْمِ"), "بِسْمِ")

    def test_strips_zero_width_marks(self):
        self.assertEqual(normalise_uthmani("a‌b‍b​c"), "abbc")

    def test_trims_outer_whitespace_only(self):
        self.assertEqual(normalise_uthmani("  فِى سَبِيلِ  "), "فِى سَبِيلِ")

    def test_empty_and_none(self):
        self.assertEqual(normalise_uthmani(""), "")
        self.assertIsNone(normalise_uthmani(None))


class TestAlignSegmentsToCanonical(unittest.TestCase):
    def _strip(self, segs):
        return "".join(s["t"] for s in segs)

    def test_roundtrips_to_canonical_exactly(self):
        # tajweed uses tatweel+superscript-alef; canonical uses bare superscript.
        segs = [{"t": "رَحْمَ", "r": None}, {"t": "ـٰ", "r": "mn"}, {"t": "ن", "r": None}]
        canonical = "رَحْمَٰن"  # dagger alef as a single U+0670, no tatweel
        out = align_segments_to_canonical(segs, canonical)
        self.assertEqual(self._strip(out), canonical)

    def test_rule_transfers_onto_canonical_mark(self):
        segs = [{"t": "مَ", "r": None}, {"t": "ـٰ", "r": "mn"}]
        out = align_segments_to_canonical(segs, "مَٰ")
        # the surviving dagger alef keeps the madd rule
        self.assertEqual(out, [{"t": "مَ", "r": None}, {"t": "ٰ", "r": "mn"}])

    def test_replace_region_takes_last_non_null_rule(self):
        # source region 'AB' (A plain, B ruled) replaced by canonical 'x'
        segs = [{"t": "A", "r": None}, {"t": "B", "r": "mn"}]
        out = align_segments_to_canonical(segs, "x")
        self.assertEqual(out, [{"t": "x", "r": "mn"}])

    def test_inserted_mark_inherits_previous_rule(self):
        # canonical has an extra combining mark the source lacked.
        segs = [{"t": "ن", "r": "g"}]
        out = align_segments_to_canonical(segs, "نۨ")  # extra small high noon
        self.assertEqual(self._strip(out), "نۨ")
        self.assertTrue(all(s["r"] == "g" for s in out))

    def test_zwnj_removed_between_words_preserves_rules(self):
        segs = [{"t": "a‌ b", "r": "dg"}]
        out = align_segments_to_canonical(segs, "a b")
        self.assertEqual(self._strip(out), "a b")
        self.assertEqual({s["r"] for s in out}, {"dg"})


class TestFullSourceOrthography(unittest.TestCase):
    """Whole-corpus #290 guarantee: re-derived tajweed round-trips to text_arabic."""

    @classmethod
    def setUpClass(cls):
        with open(JSON_DIR / "tajweed.json", encoding="utf-8") as fh:
            cls.taj = json.load(fh)
        with open(JSON_DIR / "ayahs.json", encoding="utf-8") as fh:
            cls.ayahs = json.load(fh)

    def test_all_ayahs_roundtrip_to_canonical(self):
        mismatches = []
        for a in self.ayahs:
            key = f"{a['surah_id']}:{a['number_in_surah']}"
            canonical = normalise_uthmani(a["text_arabic"])
            segs = align_segments_to_canonical(
                preparse_tajweed(self.taj[key], key=key), canonical
            )
            if "".join(s["t"] for s in segs) != canonical:
                mismatches.append(key)
        self.assertEqual(mismatches, [])

    def test_no_forbidden_marks_in_canonical(self):
        offenders = []
        for a in self.ayahs:
            canonical = normalise_uthmani(a["text_arabic"])
            if any(ch in canonical for ch in ("﻿", "​", "‌", "‍")):
                offenders.append(f"{a['surah_id']}:{a['number_in_surah']}")
        self.assertEqual(offenders, [])


class TestMaddTaxonomy(unittest.TestCase):
    """Munfasil / muttasil split via cpfair (issue #289)."""

    def _entry(self, *rules):
        # build a cpfair-like entry with annotations in the given order
        return {"annotations": [
            {"start": i, "end": i + 1, "rule": r} for i, r in enumerate(rules)
        ]}

    def test_cpfair_sequence_sorted_and_filtered(self):
        entry = {"annotations": [
            {"start": 5, "end": 6, "rule": "madd_muttasil"},
            {"start": 1, "end": 2, "rule": "madd_2"},          # ignored
            {"start": 3, "end": 4, "rule": "madd_munfasil"},
        ]}
        self.assertEqual(
            cpfair_madd_sequence(entry),
            ["madd_munfasil", "madd_muttasil"],  # by start offset
        )

    def test_reclassify_splits_in_order(self):
        html = ("a<tajweed class=madda_obligatory>x</tajweed>"
                "b<tajweed class=madda_obligatory>y</tajweed>")
        out = reclassify_madd_obligatory(html, self._entry("madd_munfasil", "madd_muttasil"))
        self.assertIn("class=madd_munfasil>x", out)
        self.assertIn("class=madd_muttasil>y", out)

    def test_reclassify_count_mismatch_left_untouched(self):
        html = "<tajweed class=madda_obligatory>x</tajweed>"
        out = reclassify_madd_obligatory(html, self._entry("madd_munfasil", "madd_muttasil"))
        self.assertEqual(out, html)  # 1 tag vs 2 annotations → no change

    def test_madd_codes_map_to_v3(self):
        self.assertEqual(RULE_CODES["madd_munfasil"], "mf")
        self.assertEqual(RULE_CODES["madd_muttasil"], "mt")
        self.assertEqual(RULE_CODES["madda_obligatory"], "mt")  # fallback → obligatory
        self.assertEqual(RULE_CODES["madda_permissible"], "ma")  # 'Aarid, not munfasil

    def test_end_to_end_split(self):
        html = ("وَتَعْتَد<tajweed class=madda_obligatory>ُوٓاْ</tajweed> "
                "<tajweed class=madda_obligatory>َآ</tajweed>ءَ")
        segs = preparse_tajweed(html, key="x", cpfair_entry=self._entry("madd_munfasil", "madd_muttasil"))
        codes = {s["r"] for s in segs}
        self.assertIn("mf", codes)
        self.assertIn("mt", codes)
        self.assertNotIn("mo", codes)


class TestQalqalahSplit(unittest.TestCase):
    def test_word_final_is_kubra(self):
        segs = [{"t": "قَ", "r": None}, {"t": "دْ", "r": "q"}]  # ends the string
        split_qalqalah(segs)
        self.assertEqual(segs[-1]["r"], "qk")

    def test_medial_is_sughra(self):
        segs = [{"t": "يَ", "r": None}, {"t": "قْ", "r": "q"}, {"t": "طَعُ", "r": None}]
        split_qalqalah(segs)
        self.assertEqual(segs[1]["r"], "qs")

    def test_before_space_is_kubra(self):
        segs = [{"t": "قَ", "r": None}, {"t": "دْ", "r": "q"}, {"t": " غَيْر", "r": None}]
        split_qalqalah(segs)
        self.assertEqual(segs[1]["r"], "qk")


class TestPipelineConservation(unittest.TestCase):
    """End-to-end character-coverage conservation (issue #290 follow-up).

    This is the test that would have caught the alignment span-drop: it runs the
    FULL pipeline (tokenize → taxonomy split → canonical alignment) and asserts
    that every rule code present in the source segments still has a home after
    alignment, for every ayah — not just `g`/`sl`, and not before alignment.
    """

    @classmethod
    def setUpClass(cls):
        with open(JSON_DIR / "tajweed.json", encoding="utf-8") as fh:
            cls.taj = json.load(fh)
        with open(JSON_DIR / "ayahs.json", encoding="utf-8") as fh:
            cls.canon = {f"{a['surah_id']}:{a['number_in_surah']}":
                         a["text_arabic"] for a in json.load(fh)}
        with open(JSON_DIR / "tajweed_cpfair.json", encoding="utf-8") as fh:
            cls.cpfair = {f"{e['surah']}:{e['ayah']}": e for e in json.load(fh)}

    def test_every_source_rule_code_survives_alignment(self):
        offenders = []
        for key, html in self.taj.items():
            pre = preparse_tajweed(html, key=key, cpfair_entry=self.cpfair.get(key))
            post = align_segments_to_canonical(
                pre, normalise_uthmani(self.canon[key])
            )
            pre_codes = {s["r"] for s in pre if s["r"]}
            post_codes = {s["r"] for s in post if s["r"]}
            if pre_codes - post_codes:
                offenders.append((key, sorted(pre_codes - post_codes)))
        self.assertEqual(offenders, [], f"rule codes lost in alignment: {offenders[:10]}")

    def test_small_waw_madd_is_conserved(self):
        # 34:14 has a small-waw (U+06E5) madd absent from text_arabic; its rule
        # must transfer to a surviving neighbour, not vanish.
        post = align_segments_to_canonical(
            preparse_tajweed(self.taj["34:14"], key="34:14",
                             cpfair_entry=self.cpfair.get("34:14")),
            normalise_uthmani(self.canon["34:14"]),
        )
        self.assertIn("mn", {s["r"] for s in post})

    def test_delete_path_transfers_rule_to_neighbour(self):
        # a ruled source char with no canonical counterpart transfers its rule
        segs = [{"t": "a", "r": None}, {"t": "b", "r": "mn"}]
        out = align_segments_to_canonical(segs, "a")  # 'b' deleted
        self.assertEqual({s["r"] for s in out}, {"mn"})  # 'a' inherits mn


class TestUnannotatedAyahsFixture(unittest.TestCase):
    """Guard the unannotated-ayah gap (sub-issue A of #287): the set of ayahs
    that render with NO colour after the *full* pipeline (incl. the #291 derived
    overlay) must only shrink and stay within the allow-list."""

    def test_gap_count_does_not_grow(self):
        fixture = json.load(
            open(SCRIPTS_DIR / "tests" / "fixtures" / "unannotated_ayahs.json",
                 encoding="utf-8"))
        allow = set(fixture["ayahs"])
        with open(JSON_DIR / "tajweed.json", encoding="utf-8") as fh:
            taj = json.load(fh)
        with open(JSON_DIR / "ayahs.json", encoding="utf-8") as fh:
            canon = {f"{a['surah_id']}:{a['number_in_surah']}": a["text_arabic"]
                     for a in json.load(fh)}
        with open(JSON_DIR / "tajweed_cpfair.json", encoding="utf-8") as fh:
            cpfair = {f"{e['surah']}:{e['ayah']}": e for e in json.load(fh)}

        current = set()
        for key, html in taj.items():
            norm = normalise_uthmani(canon[key])
            segs = apply_derived_rules(
                align_segments_to_canonical(
                    preparse_tajweed(html, key=key, cpfair_entry=cpfair.get(key)), norm),
                norm)
            if not any(s["r"] for s in segs):
                current.add(key)
        # every still-uncoloured ayah must be in the allow-list (no new gaps)
        self.assertEqual(current - allow, set(),
                         f"new uncoloured ayahs not in the fixture: {sorted(current - allow)}")
        self.assertEqual(len(current), fixture["count"])


class TestFullSourceTaxonomy(unittest.TestCase):
    """Whole-corpus #289 guarantees over the shipped data."""

    @classmethod
    def setUpClass(cls):
        with open(JSON_DIR / "tajweed.json", encoding="utf-8") as fh:
            cls.taj = json.load(fh)
        with open(JSON_DIR / "tajweed_cpfair.json", encoding="utf-8") as fh:
            cls.cpfair = {f"{e['surah']}:{e['ayah']}": e for e in json.load(fh)}

    def _all_segments(self):
        for key, html in self.taj.items():
            yield key, preparse_tajweed(html, key=key, cpfair_entry=self.cpfair.get(key))

    def test_no_legacy_madd_or_qalqalah_codes_remain(self):
        import collections
        counts = collections.Counter()
        for _, segs in self._all_segments():
            for s in segs:
                if s["r"]:
                    counts[s["r"]] += 1
        for legacy in ("mo", "mp", "q"):
            self.assertEqual(counts[legacy], 0, f"{legacy} should be fully split in v3")
        # the split actually produced the new codes
        self.assertGreater(counts["mf"], 3000)   # munfasil
        self.assertGreater(counts["mt"], 1900)    # muttasil
        self.assertGreater(counts["ma"], 4000)    # 'aarid
        self.assertGreater(counts["qk"], 0)       # qalqalah kubra
        self.assertGreater(counts["qs"], 0)       # qalqalah sughra


class TestGraphemeBoundaryMetric(unittest.TestCase):
    """Guard the grapheme-cluster boundary metric (issue #299): it must not grow.

    A rule span may legitimately begin on a combining mark that phonetically
    belongs to the rule (the decided semantics — see
    ``align_segments_to_canonical``). What must not happen is the alignment
    silently splitting *more* clusters over time, so the counts are pinned.
    """

    @classmethod
    def setUpClass(cls):
        import unicodedata
        cls.unicodedata = unicodedata
        with open(JSON_DIR / "tajweed.json", encoding="utf-8") as f:
            cls.taj = json.load(f)
        with open(JSON_DIR / "ayahs.json", encoding="utf-8") as f:
            cls.canon = {f"{a['surah_id']}:{a['number_in_surah']}":
                         a["text_arabic"] for a in json.load(f)}
        with open(JSON_DIR / "tajweed_cpfair.json", encoding="utf-8") as f:
            cls.cpfair = {f"{e['surah']}:{e['ayah']}": e for e in json.load(f)}
        with open(SCRIPTS_DIR / "tests" / "fixtures"
                  / "grapheme_boundary_baseline.json", encoding="utf-8") as f:
            cls.baseline = json.load(f)

    def test_cluster_split_metric_does_not_grow(self):
        combining = self.unicodedata.combining
        starts = splits = 0
        for key, html in self.taj.items():
            segs = align_segments_to_canonical(
                preparse_tajweed(html, key=key, cpfair_entry=self.cpfair.get(key)),
                normalise_uthmani(self.canon[key]),
            )
            for i, s in enumerate(segs):
                if s["t"] and combining(s["t"][0]) and s["r"]:
                    starts += 1
                nxt = segs[i + 1] if i + 1 < len(segs) else None
                if nxt and nxt["t"] and combining(nxt["t"][0]) and s["r"] != nxt["r"]:
                    splits += 1
        self.assertLessEqual(starts, self.baseline["combining_start_spans"])
        self.assertLessEqual(splits, self.baseline["cluster_split_boundaries"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
