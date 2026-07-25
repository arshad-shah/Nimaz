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
    apply_source_fixups,
    preparse_tajweed,
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
        result = preparse_tajweed(
            "a<tajweed class=madda_obligatory>b"
            "<tajweed class=slnt>c</tajweed>d</tajweed>e"
        )
        self.assertEqual(
            result,
            [
                {"t": "a", "r": None},
                {"t": "b", "r": "mo"},   # outer, before inner
                {"t": "c", "r": "sl"},   # inner wins
                {"t": "d", "r": "mo"},   # outer, after inner
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


if __name__ == "__main__":
    unittest.main(verbosity=2)
