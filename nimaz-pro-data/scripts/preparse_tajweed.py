#!/usr/bin/env python3
"""
Pre-parse Tajweed HTML to simplified JSON format for efficient rendering.

Converts: <tajweed class="ghunnah">text</tajweed>
To: [{"t": "text", "r": "g"}]

This eliminates runtime regex parsing in the Android app.
"""

import json
import re
from pathlib import Path

# Map tajweed class names to single-letter codes
# Based on actual classes found in tajweed.json:
# end, ghunnah, ham_wasl, idgham_ghunnah, idgham_mutajanisayn, idgham_mutaqaribayn,
# idgham_shafawi, idgham_wo_ghunnah, ikhafa, ikhafa_shafawi, iqlab, laam_shamsiyah,
# madda_necessary, madda_normal, madda_obligatory, madda_permissible, qalaqah, slnt
RULE_CODES = {
    # Ghunnah (nasalization) - Green
    'ghunnah': 'g',
    'ghn': 'g',

    # Ikhfa (hiding) - Purple
    'ikhfa': 'i',
    'ikhafa': 'i',
    'ikhfa_shafawi': 'i',
    'ikhafa_shafawi': 'i',

    # Idgham (merging) - Amber/Orange
    'idgham': 'd',
    'idgham_ghunnah': 'd',
    'idgham_no_ghunnah': 'd',
    'idgham_wo_ghunnah': 'd',
    'idgham_shafawi': 'd',
    'idgham_mutajanisayn': 'd',
    'idgham_mutaqaribayn': 'd',

    # Qalqalah (echoing) - Blue
    'qalqalah': 'q',
    'qalaqah': 'q',

    # Madd (elongation) - Red
    'madd': 'm',
    'madd_normal': 'm',
    'madd_permissible': 'm',
    'madd_necessary': 'm',
    'madd_obligatory': 'm',
    'madda_normal': 'm',
    'madda_permissible': 'm',
    'madda_necessary': 'm',
    'madda_obligatory': 'm',

    # Iqlab (conversion) - Purple
    'iqlab': 'l',

    # Silent letters - Gray/Slate
    'silent': 's',
    'slnt': 's',

    # Lam Shamsiyyah (sun letters - use idgham color)
    'lam_shamsiyah': 'd',
    'laam_shamsiyah': 'd',

    # Hamza Wasl (connecting hamza - silent)
    'ham_wasl': 's',
    'hamza_wasl': 's',

    # End marker (verse number indicator - ignore)
    'end': None,
}

# Pattern to match tajweed tags (handles both quoted and unquoted class values)
TAG_PATTERN = re.compile(r'<tajweed\s+class=(["\']?)([^"\'>\s]+)\1>(.*?)</tajweed>')

# Pattern to match span end markers (verse numbers)
END_SPAN_PATTERN = re.compile(r'<span\s+class=end>.*?</span>')


def preparse_tajweed(html_text):
    """
    Convert HTML tajweed text to simplified JSON segments.

    Args:
        html_text: Text with <tajweed class="rule">text</tajweed> markup

    Returns:
        List of segments: [{"t": "text", "r": "code"}, ...]
        where r is None for plain text, or a single-letter rule code
    """
    if not html_text:
        return []

    # Remove end span markers (verse numbers) - they're not needed in the parsed format
    html_text = END_SPAN_PATTERN.sub('', html_text).strip()

    segments = []
    last_end = 0

    for match in TAG_PATTERN.finditer(html_text):
        # Add plain text before this match
        if match.start() > last_end:
            plain = html_text[last_end:match.start()]
            if plain:
                segments.append({"t": plain, "r": None})

        # Get rule class and text content
        rule_class = match.group(2).lower()
        text_content = match.group(3)

        # Map to single-letter code
        rule_code = RULE_CODES.get(rule_class)
        if rule_code is None and rule_class not in RULE_CODES:
            # Unknown rule - try first letter as fallback
            print(f"Warning: Unknown tajweed class '{rule_class}', using first letter")
            rule_code = rule_class[0] if rule_class else None

        if text_content:
            # Only add segment with rule if there's a valid rule code
            # For 'end' class (verse markers), rule_code will be None
            segments.append({"t": text_content, "r": rule_code})

        last_end = match.end()

    # Add remaining plain text
    if last_end < len(html_text):
        remaining = html_text[last_end:].strip()
        if remaining:
            segments.append({"t": remaining, "r": None})

    return segments


def preparse_tajweed_file(input_path, output_path=None):
    """
    Pre-parse an entire tajweed.json file.

    Args:
        input_path: Path to tajweed.json (dict with "surah:ayah" keys)
        output_path: Optional output path (defaults to tajweed_parsed.json)
    """
    input_path = Path(input_path)
    if output_path is None:
        output_path = input_path.parent / "tajweed_parsed.json"
    else:
        output_path = Path(output_path)

    print(f"Loading tajweed data from {input_path}...")
    with open(input_path, 'r', encoding='utf-8') as f:
        tajweed_data = json.load(f)

    print(f"Pre-parsing {len(tajweed_data)} entries...")
    parsed_data = {}
    for key, html_text in tajweed_data.items():
        segments = preparse_tajweed(html_text)
        # Store as JSON string for direct insertion into database
        parsed_data[key] = json.dumps(segments, ensure_ascii=False)

    print(f"Writing parsed data to {output_path}...")
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(parsed_data, f, ensure_ascii=False, indent=2)

    print(f"Done! Parsed {len(parsed_data)} ayahs")
    return parsed_data


def preparse_single(html_text):
    """
    Pre-parse a single HTML tajweed string and return JSON string.
    Used by generate_database.py for inline conversion.
    """
    segments = preparse_tajweed(html_text)
    return json.dumps(segments, ensure_ascii=False)


if __name__ == "__main__":
    # Run standalone to pre-parse the entire tajweed.json file
    json_dir = Path(__file__).parent.parent / "json"
    tajweed_path = json_dir / "tajweed.json"

    if tajweed_path.exists():
        preparse_tajweed_file(tajweed_path)
    else:
        print(f"Error: {tajweed_path} not found")
