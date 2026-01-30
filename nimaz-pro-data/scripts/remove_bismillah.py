#!/usr/bin/env python3
"""
Remove Bismillah from the first ayah of each surah in ayahs.json.

The Bismillah is prepended to verse 1 of most surahs, but it should be
displayed separately (handled by UI), not embedded in the ayah text.

Exceptions:
- Surah 1 (Al-Fatiha): Bismillah IS verse 1, so keep it
- Surah 9 (At-Tawbah): Has no Bismillah at all
"""

import json
import unicodedata
from pathlib import Path

# The exact Bismillah text as it appears in the ayahs.json data (38 characters)
# Extracted directly from Surah 2, Ayah 1
BISMILLAH = 'بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ'

# Base Bismillah to check (first word)
BISMILLAH_BASE = "بِسْمِ"


def remove_bismillah_from_ayahs(ayahs_path=None):
    """
    Remove Bismillah from the first ayah of surahs 2-8 and 10-114.

    Args:
        ayahs_path: Path to ayahs.json (optional, defaults to ../json/ayahs.json)
    """
    if ayahs_path is None:
        ayahs_path = Path(__file__).parent.parent / "json" / "ayahs.json"
    else:
        ayahs_path = Path(ayahs_path)

    print(f"Loading ayahs from {ayahs_path}...")
    with open(ayahs_path, 'r', encoding='utf-8') as f:
        ayahs = json.load(f)

    modified_count = 0
    skipped_surahs = []

    for ayah in ayahs:
        surah_id = ayah['surah_id']
        number_in_surah = ayah['number_in_surah']

        # Skip Al-Fatiha (1) - Bismillah is verse 1
        # Skip At-Tawbah (9) - Has no Bismillah
        if surah_id == 1 or surah_id == 9:
            continue

        # Only process first ayah of each surah
        if number_in_surah != 1:
            continue

        # Check and remove Bismillah from text fields
        for field in ['text_arabic', 'text_uthmani']:
            if field not in ayah:
                continue

            text = ayah[field]
            original_text = text

            # Normalize both strings for comparison (NFC normalization)
            text_normalized = unicodedata.normalize('NFC', text)
            bismillah_normalized = unicodedata.normalize('NFC', BISMILLAH)

            # Check if text starts with Bismillah (using normalized comparison)
            if text_normalized.startswith(bismillah_normalized):
                # Remove Bismillah and any leading whitespace
                # Use the actual length of the normalized bismillah in the original text
                text = text[len(BISMILLAH):].lstrip()
                ayah[field] = text
                modified_count += 1
            elif text.startswith(BISMILLAH_BASE):
                # Text starts with "بِسْمِ" - likely Bismillah in different unicode form
                # Try to find where Bismillah ends by looking for a space after position 30+
                space_pos = text.find(' ', 30)
                if space_pos > 0 and space_pos < 50:
                    # Found end of Bismillah
                    text = text[space_pos:].lstrip()
                    ayah[field] = text
                    modified_count += 1
                else:
                    skipped_surahs.append((surah_id, field, text[:60]))

    # Save modified ayahs
    print(f"\nSaving modified ayahs to {ayahs_path}...")
    with open(ayahs_path, 'w', encoding='utf-8') as f:
        json.dump(ayahs, f, ensure_ascii=False, indent=2)

    print(f"\nDone! Modified {modified_count} fields")

    if skipped_surahs:
        print(f"\nWarning: {len(skipped_surahs)} fields may have unhandled Bismillah variants:")
        for surah_id, field, text in skipped_surahs[:5]:
            print(f"  Surah {surah_id} ({field})")

    return modified_count


def verify_bismillah_removal(ayahs_path=None):
    """
    Verify that Bismillah has been removed from first ayahs.
    """
    if ayahs_path is None:
        ayahs_path = Path(__file__).parent.parent / "json" / "ayahs.json"
    else:
        ayahs_path = Path(ayahs_path)

    with open(ayahs_path, 'r', encoding='utf-8') as f:
        ayahs = json.load(f)

    print("Verifying first ayah of each surah:")
    issues = []

    for ayah in ayahs:
        if ayah['number_in_surah'] != 1:
            continue

        surah_id = ayah['surah_id']
        text = ayah.get('text_arabic', '')

        # Check if it still contains Bismillah (except Surah 1)
        has_bismillah = any(text.startswith(b) for b in BISMILLAH_VARIANTS)

        if surah_id == 1:
            if not has_bismillah:
                issues.append(f"Surah 1 should have Bismillah but doesn't")
            else:
                print(f"  Surah 1: OK (has Bismillah - correct)")
        elif surah_id == 9:
            print(f"  Surah 9: OK (no Bismillah - correct)")
        else:
            if has_bismillah:
                issues.append(f"Surah {surah_id} still has Bismillah: {text[:40]}...")
            else:
                print(f"  Surah {surah_id}: OK (no Bismillah)")

    if issues:
        print(f"\nIssues found:")
        for issue in issues:
            print(f"  - {issue}")
    else:
        print(f"\nAll surahs verified successfully!")


if __name__ == "__main__":
    import sys

    if len(sys.argv) > 1 and sys.argv[1] == "--verify":
        verify_bismillah_removal()
    else:
        remove_bismillah_from_ayahs()
        print("\nRun with --verify to check the results")
