"""Offline structural checks; not a substitute for scholarly review or Android tests."""
import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FEATURE = ROOT / "feature/content/src/main"
SOURCE = FEATURE / "kotlin/com/arshadshah/nimaz/presentation/screens/learnpray/PrayerLesson.kt"
STRINGS = ROOT / "core/ui/src/main/res/values/learn_pray_strings.xml"


class LearnPrayContentTest(unittest.TestCase):
    def setUp(self):
        self.source = SOURCE.read_text()
        self.entries = [entry for path in STRINGS.parent.glob("learn_pray*strings.xml")
                        for entry in ET.parse(path).getroot().findall("string")]
        self.strings = {entry.attrib["name"]: entry.text for entry in self.entries}

    def test_all_native_text_references_resolve(self):
        refs = set(re.findall(r"R.string.(learn_pray_\w+)", self.source))
        for path in SOURCE.parent.glob("*.kt"):
            refs.update(re.findall(r"R.string.(learn_pray_\w+)", path.read_text()))
        self.assertTrue(refs)
        self.assertFalse(refs - self.strings.keys())
        self.assertEqual(len(self.entries), len(self.strings))
        self.assertTrue(all(self.strings.values()))

    def test_each_recitation_has_three_nonempty_fields(self):
        for name in ("opening", "refuge", "fatihah", "amin", "ikhlas", "takbir",
                     "ruku", "rise", "sujud", "sit", "tashahhud", "salawat", "dua", "salam"):
            for suffix in ("ar", "tr", "en"):
                self.assertTrue(self.strings[f"learn_pray_{name}_{suffix}"].strip())

    def test_both_rakahs_and_both_salams_are_explicit(self):
        ids = re.findall(r'PrayerLessonStep\(\s*"([^"]+)"', self.source)
        self.assertEqual(ids, [
            "opening_takbir", "recitation_1", "bow_1", "rise_1", "prostrate_1a",
            "sit_1", "prostrate_1b", "recitation_2", "bow_2", "rise_2",
            "prostrate_2a", "sit_2", "prostrate_2b", "tashahhud", "salawat",
            "dua", "salam_right", "salam_left",
        ])

    def test_artwork_is_bundled_and_not_a_remote_dependency(self):
        names = set(re.findall(r"ContentR.drawable.(learn_pray_\w+)", self.source))
        self.assertEqual(len(names), 18)
        for name in names:
            asset = FEATURE / "res/drawable-nodpi" / f"{name}.webp"
            self.assertTrue(asset.is_file(), name)
            self.assertGreater(asset.stat().st_size, 1000)

    def test_sources_are_https_and_preview_scope_is_explicit(self):
        urls = re.findall(r'"(https?://[^"]+)"', self.source)
        self.assertTrue(urls)
        self.assertTrue(all(url.startswith(("https://sunnah.com/", "https://quran.com/",
                                           "https://seekersguidance.org/", "https://islamqa.org/")) for url in urls))
        self.assertIn("two rak‘ahs", self.strings["learn_pray_prepare_body"])
        self.assertIn("review", self.strings["learn_pray_review"])
        self.assertIn("does not record", self.strings["learn_pray_complete_body"])

    def test_pose_copy_is_complete_in_every_supported_locale(self):
        base = {e.attrib["name"] for e in ET.parse(STRINGS.parent / "learn_pray_pose_strings.xml").getroot()}
        for locale in ("de", "fr", "id", "ms", "tr"):
            entries = ET.parse(STRINGS.parent.parent / f"values-{locale}" / "learn_pray_pose_strings.xml").getroot()
            self.assertEqual(base, {e.attrib["name"] for e in entries}, locale)
            self.assertTrue(all(e.text and e.text.strip() for e in entries), locale)


if __name__ == "__main__":
    unittest.main()
