#!/usr/bin/env python3
"""
Per-rule unit tests for the derived tajweed rule engine (issue #291).

The engine is deliberately not wired into the shipped DB (it needs scholarly
review — see tajweed_rules.py); these tests pin its behaviour so a future
reviewed wiring builds on a verified base.
"""

import json
import sys
import unittest
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
JSON_DIR = SCRIPTS_DIR.parent / "json"
sys.path.insert(0, str(SCRIPTS_DIR))

import tajweed_rules as R  # noqa: E402
from preparse_tajweed import normalise_uthmani  # noqa: E402


def _raa(word):
    return R.raa_rule(word, word.index("ر"))


class TestIstilaAndWaqf(unittest.TestCase):
    def test_istila_letters(self):
        for ch in "خصضغطقظ":
            self.assertTrue(R.is_istila(ch), ch)
        for ch in "بتنمر":
            self.assertFalse(R.is_istila(ch), ch)

    def test_waqf_classification(self):
        self.assertEqual(R.classify_waqf("ۚ"), "waqf_jaiz")
        self.assertEqual(R.classify_waqf("ۘ"), "waqf_lazim")
        self.assertEqual(R.classify_waqf("ۙ"), "waqf_mamnu")
        self.assertEqual(R.classify_waqf("ۜ"), "sakt")
        self.assertIsNone(R.classify_waqf("ب"))


class TestRaa(unittest.TestCase):
    def test_raa_with_short_vowels(self):
        self.assertEqual(_raa("رَب"), "tk")   # fatha → heavy
        self.assertEqual(_raa("رُو"), "tk")   # damma → heavy
        self.assertEqual(_raa("رِز"), "tq")   # kasra → light

    def test_raa_sakinah_after_fatha_is_heavy(self):
        self.assertEqual(_raa("مَرْيَم"), "tk")

    def test_raa_sakinah_after_kasra_is_light(self):
        self.assertEqual(_raa("فِرْعَ"), "tq")

    def test_raa_sakinah_after_kasra_but_istila_follows_is_heavy(self):
        self.assertEqual(_raa("قِرْطَا"), "tk")  # قِرْطَاس

    def test_raa_sakinah_after_hamzat_wasl_is_heavy(self):
        self.assertEqual(_raa("ٱرْجِ"), "tk")   # temporary kasra

    def test_not_a_raa(self):
        self.assertIsNone(R.raa_rule("بِسْم", 0))


class TestLamOfTheName(unittest.TestCase):
    """Uses the real normalised text so the شّ/vowel ordering matches the data."""

    @classmethod
    def setUpClass(cls):
        with open(JSON_DIR / "ayahs.json", encoding="utf-8") as f:
            cls.ay = {f"{a['surah_id']}:{a['number_in_surah']}": a["text_arabic"]
                      for a in json.load(f)}

    def _lam(self, key):
        t = normalise_uthmani(self.ay[key])
        for i, ch in enumerate(t):
            r = R.lam_of_name_rule(t, i)
            if r:
                return r
        return None

    def test_light_after_kasra(self):
        self.assertEqual(self._lam("1:1"), "tq")   # بِسْمِ ٱللَّهِ

    def test_heavy_after_fatha(self):
        self.assertEqual(self._lam("112:1"), "tk")  # هُوَ ٱللَّهُ

    def test_heavy_at_ayah_start(self):
        self.assertEqual(self._lam("2:255"), "tk")  # ٱللَّهُ …


class TestMaddLin(unittest.TestCase):
    def _lin(self, word):
        t = normalise_uthmani(word)
        for i, ch in enumerate(t):
            if ch in ("و", "ي"):
                return R.is_madd_lin(t, i)
        return None

    def test_layn_at_stop_is_madd_lin(self):
        self.assertTrue(self._lin("خَوْف"))
        self.assertTrue(self._lin("رَيْب"))

    def test_kasra_ya_is_not_lin(self):
        # يٓ / regular madd, not a layn letter
        t = normalise_uthmani("ٱلرَّحِيم")
        i = t.index("ي")
        self.assertFalse(R.is_madd_lin(t, i))


class TestIdghamMutamathilayn(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        with open(JSON_DIR / "ayahs.json", encoding="utf-8") as f:
            cls.ay = {f"{a['surah_id']}:{a['number_in_surah']}": a["text_arabic"]
                      for a in json.load(f)}

    def test_finds_qul_lahum(self):
        # قُل لَّهُم — lam sakin into lam
        t = normalise_uthmani(self.ay["3:12"])
        self.assertTrue(len(R.idgham_mutamathilayn(t)) >= 1)

    def test_no_false_positive_on_shadda_tanwin_word(self):
        # حِلٌّ (shadda + tanwin) is not sakin → no self-idgham
        t = normalise_uthmani("حِلٌّ")
        self.assertEqual(R.idgham_mutamathilayn(t), [])

    def test_whole_corpus_runs(self):
        for key, txt in self.ay.items():
            R.idgham_mutamathilayn(normalise_uthmani(txt))  # must not raise


if __name__ == "__main__":
    unittest.main(verbosity=2)
