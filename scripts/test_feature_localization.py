"""Every onboarding/lesson string must exist in every shipped language, without exemptions."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "core/ui/src/main/res"
LOCALES = ("values", "values-de", "values-fr", "values-id", "values-ms", "values-tr")
PREFIXES = ("learn_pray_", "onboarding_")


def strings(directory):
    result = {}
    for path in directory.glob("*.xml"):
        for element in ET.parse(path).getroot().findall("string"):
            name = element.get("name", "")
            if name.startswith(PREFIXES):
                if name in result:
                    raise AssertionError(f"Duplicate {name} in {path}")
                result[name] = (element, "".join(element.itertext()))
    return result


class FeatureLocalizationTest(unittest.TestCase):
    def test_complete_keys_nonempty_and_no_language_exemptions(self):
        base = strings(RES / "values")
        self.assertTrue(base)
        for locale in LOCALES:
            localized = strings(RES / locale)
            self.assertEqual(set(base), set(localized), locale)
            for name, (element, value) in localized.items():
                self.assertTrue(value.strip(), (locale, name))
                self.assertNotEqual(element.get("translatable"), "false", (locale, name))
                self.assertFalse(any("ignore" in attr.lower() for attr in element.attrib), (locale, name))
                placeholders = lambda text: sorted(re.findall(r"%\d+\$[dsf]", text))
                self.assertEqual(placeholders(base[name][1]), placeholders(value), (locale, name))

    def test_canonical_recitations_unchanged_in_all_locales(self):
        base = strings(RES / "values")
        for locale in LOCALES[1:]:
            localized = strings(RES / locale)
            for name, (_, value) in base.items():
                if name.startswith("learn_pray_") and name.endswith(("_ar", "_tr")):
                    self.assertEqual(value, localized[name][1], (locale, name))

    def test_no_english_only_banner(self):
        base = strings(RES / "values")
        for name, (_, value) in base.items():
            self.assertNotIn("English guide", value, name)
            self.assertNotIn("lesson text is in English", value, name)



if __name__ == "__main__":
    unittest.main()
