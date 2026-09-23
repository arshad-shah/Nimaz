"""Verify lesson citation addresses against the checked-out content corpus."""
import json
import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LESSON = ROOT / "feature/content/src/main/kotlin/com/arshadshah/nimaz/presentation/screens/learnpray"


class PrayerReferenceCorpusTest(unittest.TestCase):
    def test_all_cited_hadiths_match_local_records(self):
        mapping = dict(re.findall(r'"((?:bukhari|muslim|abudawud):\w+)" to "(\d+)"',
                                  (LESSON / "PrayerReference.kt").read_text()))
        citations = set(re.findall(r'https://sunnah.com/([\w:]+)',
                                  (LESSON / "PrayerLesson.kt").read_text() +
                                  (LESSON / "LearnToPrayScreen.kt").read_text()))
        self.assertEqual(citations, set(mapping))
        muslim_crosswalk = {
            "402a": (897, "Peace be upon Allah"),
            "580b": (1310, "fifty-three"),
            "588a": (1324, "trial of life and death"),
            "772": (1814, "Glory be to my Lord most High"),
            "582": (1315, "whiteness of his cheek"),
        }
        for book in ("bukhari", "muslim", "abudawud"):
            records = {str(r["id"]): r for r in json.loads(
                (ROOT / f"nimaz-pro-data/json/hadith_{book}.json").read_text())}
            for citation, record_id in mapping.items():
                if not citation.startswith(book + ":"):
                    continue
                record = records[record_id]
                if book == "muslim":
                    number, excerpt = muslim_crosswalk[citation.split(":")[1]]
                    self.assertEqual(record["reference"], f"muslim:{number}")
                    self.assertIn(excerpt, record["text_english"])
                else:
                    self.assertEqual(record["reference"], citation)


if __name__ == "__main__":
    unittest.main()
