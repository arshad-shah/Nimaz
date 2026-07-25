#!/usr/bin/env python3
"""
Tests for the tajweed validation harness (issue #292).

Two kinds: the real pipeline must pass, and planted defects must be caught
(a harness that never fails is worthless).
"""

import sys
import unittest
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(SCRIPTS_DIR))

import verify_tajweed as V  # noqa: E402


class TestHarnessPassesOnRealData(unittest.TestCase):
    def test_pipeline_is_clean(self):
        report = V.verify_pipeline()
        self.assertTrue(report.ok(), f"unexpected failures: {report.failures[:10]}")


class TestHarnessCatchesDefects(unittest.TestCase):
    """Feed _check_segments planted defects and assert the right check fires."""

    def _run(self, key, segments, text_arabic, empty_allow=frozenset()):
        report = V.Report()
        V._check_segments(report, key, segments, text_arabic, empty_allow)
        return {c for c, _ in report.failures}

    def test_leaked_markup_caught(self):
        checks = self._run("x", [{"t": "a<tajweed>", "r": None}], "a<tajweed>")
        self.assertIn("well-formed", checks)

    def test_legacy_code_caught(self):
        checks = self._run("x", [{"t": "a", "r": "mo"}], "a")
        self.assertIn("code-whitelist", checks)

    def test_unknown_code_caught(self):
        checks = self._run("x", [{"t": "a", "r": "zz"}], "a")
        self.assertIn("code-whitelist", checks)

    def test_roundtrip_mismatch_caught(self):
        checks = self._run("x", [{"t": "a", "r": None}], "different")
        self.assertIn("round-trip", checks)

    def test_empty_spans_not_in_allowlist_caught(self):
        checks = self._run("x", [{"t": "a", "r": None}], "a")
        self.assertIn("coverage", checks)

    def test_empty_spans_in_allowlist_ok(self):
        checks = self._run("x", [{"t": "a", "r": None}], "a", empty_allow={"x"})
        self.assertNotIn("coverage", checks)

    def test_cross_source_drift_caught(self):
        report = V.Report()
        # a tampered source with the wrong ghunnah count vs the allow-list
        tajweed = {"1:1": "<tajweed class=ghunnah>x</tajweed>"}
        cpfair = []  # no cpfair annotations → delta won't match allow-list
        V._check_cross_source(report, tajweed, cpfair)
        self.assertTrue(any(c == "cross-source" for c, _ in report.failures))

    def test_golden_mismatch_caught(self):
        report = V.Report()
        V._check_golden(report, {"1:1": [{"t": "wrong", "r": None}]})
        self.assertTrue(any(c == "golden" for c, _ in report.failures))


if __name__ == "__main__":
    unittest.main(verbosity=2)
