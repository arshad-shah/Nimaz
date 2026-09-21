"""Structural guard for the five-page onboarding; Compose tests cover runtime behavior."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "feature/onboarding/src/main/kotlin/com/arshadshah/nimaz/presentation/screens/onboarding/OnboardingScreen.kt"


class OnboardingContractTest(unittest.TestCase):
    def test_five_pages_with_dedicated_permissions(self):
        source = SOURCE.read_text()
        self.assertIn("ONBOARDING_PAGE_COUNT = 5", source)
        self.assertEqual(len(re.findall(r"IntroPage\(R.string", source)), 4)
        self.assertNotIn("NimazBottomSheet(", source)
        self.assertIn("PermissionContent(insets, permissionCards)", source)
        self.assertIn("pager.settledPage", source)

    def test_motion_and_native_icons(self):
        source = SOURCE.read_text()
        self.assertIn("FastOutSlowInEasing", source)
        self.assertIn("Crossfade(", source)
        self.assertIn("NimazIcons.Previous", source)
        self.assertIn("NimazIcons.Next", source)
        self.assertFalse(re.search(r'Text\("[✦✓✕→←★☆✅❌🔔]', source))


if __name__ == "__main__":
    unittest.main()
