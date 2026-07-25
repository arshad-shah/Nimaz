#!/usr/bin/env python3
"""
Pre-parse Tajweed HTML to simplified JSON format for efficient rendering.

Converts: <tajweed class="ghunnah">text</tajweed>
To: [{"t": "text", "r": "g"}]

This eliminates runtime regex parsing in the Android app.

V2 rule codes — each tajweed sub-type gets its own code so the app can
assign distinct colors matching a standard colour-coded mushaf.

Nested tags
-----------
The quran.com source (``json/tajweed.json``) contains 33 *nested* tags —
one tajweed span wrapped inside another — e.g. ``2:190``::

    ...تَعْتَد<tajweed class=madda_obligatory>ُوٓ<tajweed class=slnt>اْ</tajweed>‌ۚ</tajweed> إِ...

Here ``slnt`` (silent) is nested inside ``madda_obligatory``. A single-level
non-greedy regex cannot represent this: it stops at the first ``</tajweed>``,
so the inner tag leaks into the text payload (literal ``<tajweed …>`` markup
rendered in the Mushaf) and the inner rule span is silently dropped.

This module uses a **stack-based tokenizer** instead. Semantics:

* the **innermost** rule wins for the characters it covers, and the outer
  rule is kept for the remaining characters. For ``2:190`` above that yields
  three segments — ``ُوٓ`` = ``mo``, ``اْ`` = ``sl``, ``‌ۚ`` = ``mo`` — rather
  than one mislabelled ``ُوٓاْ‌ۚ`` = ``mo`` span with leaked markup.

Because of this, per-*segment* code counts do not equal raw *tag* counts for
overlapping tags (an outer tag split by an inner produces 2+ segments; an
outer tag fully covered by an inner produces 0). The guaranteed invariant is
character-level: concatenating every segment's text reconstructs the source
text exactly (tags and verse markers removed), so no character is ever lost.
"""

import json
import re
from pathlib import Path

# ── V2 Rule Code Map ──────────────────────────────────────────────────
# Every class from the Quran.com uthmani_tajweed API is mapped to a
# unique short code.  This lets the Android renderer show a different
# colour for each tajweed sub-rule, matching printed tajweed mushafs.
#
# Classes present in tajweed.json (18):
#   end, ghunnah, ham_wasl, idgham_ghunnah, idgham_mutajanisayn,
#   idgham_mutaqaribayn, idgham_shafawi, idgham_wo_ghunnah, ikhafa,
#   ikhafa_shafawi, iqlab, laam_shamsiyah, madda_necessary, madda_normal,
#   madda_obligatory, madda_permissible, qalaqah, slnt
RULE_CODES = {
    # ── Ghunnah (nasalisation, 2 beats) ──
    'ghunnah': 'g',
    'ghn': 'g',

    # ── Ikhfa (concealment / hiding of noon sakinah) ──
    'ikhfa': 'if',
    'ikhafa': 'if',

    # ── Ikhfa Shafawi (labial hiding of meem sakinah) ──
    'ikhfa_shafawi': 'is',
    'ikhafa_shafawi': 'is',

    # ── Idgham with Ghunnah (merging with nasalisation) ──
    'idgham': 'dg',
    'idgham_ghunnah': 'dg',

    # ── Idgham without Ghunnah (merging without nasalisation) ──
    'idgham_no_ghunnah': 'dn',
    'idgham_wo_ghunnah': 'dn',

    # ── Idgham Shafawi (labial merging of meem sakinah) ──
    'idgham_shafawi': 'ds',

    # ── Idgham Mutajanisayn (merging of homorganic letters) ──
    'idgham_mutajanisayn': 'dj',

    # ── Idgham Mutaqaribayn (merging of close-articulation letters) ──
    'idgham_mutaqaribayn': 'dk',

    # ── Qalqalah (echoing / bouncing) ──
    'qalqalah': 'q',
    'qalaqah': 'q',

    # ── Madd Normal / Tabee'i (natural elongation, 2 beats) ──
    'madd': 'mn',
    'madd_normal': 'mn',
    'madda_normal': 'mn',

    # ── Madd Jaiz Munfasil (permissible elongation, 2-4-5 beats) ──
    'madd_permissible': 'mp',
    'madda_permissible': 'mp',

    # ── Madd Wajib Muttasil (obligatory elongation, 4-5 beats) ──
    'madd_obligatory': 'mo',
    'madda_obligatory': 'mo',

    # ── Madd Lazim (necessary elongation, 6 beats) ──
    'madd_necessary': 'my',
    'madda_necessary': 'my',

    # ── Iqlab (conversion of noon sakinah to meem) ──
    'iqlab': 'l',

    # ── Lam Shamsiyyah (assimilation of lam into sun letters) ──
    'lam_shamsiyah': 'ls',
    'laam_shamsiyah': 'ls',

    # ── Silent letters ──
    'silent': 'sl',
    'slnt': 'sl',

    # ── Hamza Al-Wasl (connecting hamza, not pronounced mid-sentence) ──
    'ham_wasl': 'hw',
    'hamza_wasl': 'hw',

    # ── End marker (verse number indicator — stripped) ──
    'end': None,
}

# Pattern to match a tajweed opening tag (handles quoted and unquoted class values)
TAG_OPEN_PATTERN = re.compile(r'<tajweed\s+class=(["\']?)([^"\'>\s]+)\1\s*>')
TAG_CLOSE = '</tajweed>'

# Pattern to match span end markers (verse numbers)
END_SPAN_PATTERN = re.compile(r'<span\s+class=end>.*?</span>')

# ── Source fix-ups ────────────────────────────────────────────────────
# The upstream quran.com source has a small number of ayahs that are
# malformed at source (broken/absent markup). Each entry is a list of
# (broken_fragment, corrected_fragment) replacements applied to that ayah's
# raw HTML *before* tokenizing. Keying by fragment (not the whole string)
# means a corrected upstream re-fetch simply no-ops instead of silently
# re-introducing the bug or clobbering unrelated upstream edits.
#
# 32:3 — `ٱفْتَرَ>ٮٰهُ`: the opening `<tajweed class=madda_normal>` for the
# dagger-alif elongation (`ٮٰ`) was corrupted to a stray `>`, leaving an
# unbalanced `</tajweed>` and a literal `>` in the rendered text. The rule
# is confirmed as a natural madd (`madd_2`) by the independent
# `tajweed_cpfair.json` dataset at the same position.
SOURCE_FIXUPS = {
    '32:3': [
        ('فْتَرَ>ٮٰ', 'فْتَرَ<tajweed class=madda_normal>ٮٰ'),
    ],
}


def apply_source_fixups(key, html_text):
    """Apply any registered SOURCE_FIXUPS for ``key`` (``"surah:ayah"``)."""
    for broken, fixed in SOURCE_FIXUPS.get(key, []):
        html_text = html_text.replace(broken, fixed)
    return html_text


def preparse_tajweed(html_text, key=None):
    """
    Convert HTML tajweed text to simplified JSON segments.

    Uses a stack-based tokenizer so arbitrarily nested ``<tajweed>`` tags are
    handled correctly (see the module docstring). The innermost rule wins for
    the characters it covers; the enclosing rule is kept for the rest.

    Args:
        html_text: Text with <tajweed class="rule">text</tajweed> markup.
        key: Optional "surah:ayah" key used to (a) apply SOURCE_FIXUPS and
             (b) produce actionable error messages.

    Returns:
        List of segments: [{"t": "text", "r": "code"}, ...]
        where r is None for plain text, or a rule code string.

    Raises:
        ValueError: on an unknown tajweed class, a stray/unbalanced tag, or an
            unclosed tag — malformed input fails loud rather than silently
            corrupting the output.
    """
    if not html_text:
        return []

    where = f" in {key}" if key else ""

    if key is not None:
        html_text = apply_source_fixups(key, html_text)

    # Remove end span markers (verse numbers) - they're not needed in the parsed format.
    # Strip once at the boundary; internal whitespace (word spacing) is preserved.
    html_text = END_SPAN_PATTERN.sub('', html_text).strip()

    segments = []
    stack = []  # active rule codes, innermost last
    pos = 0
    length = len(html_text)

    while pos < length:
        # Closing tag → pop the innermost active rule.
        if html_text.startswith(TAG_CLOSE, pos):
            if not stack:
                raise ValueError(
                    f"Unbalanced </tajweed>{where} at position {pos}: "
                    f"{html_text[pos:pos + 30]!r}"
                )
            stack.pop()
            pos += len(TAG_CLOSE)
            continue

        # Opening tag → validate the class and push its rule code.
        match = TAG_OPEN_PATTERN.match(html_text, pos)
        if match:
            rule_class = match.group(2).lower()
            if rule_class not in RULE_CODES:
                raise ValueError(
                    f"Unknown tajweed class {rule_class!r}{where}. "
                    f"Add it to RULE_CODES or fix the source."
                )
            stack.append(RULE_CODES[rule_class])
            pos = match.end()
            continue

        # Plain text run up to the next '<'.
        nxt = html_text.find('<', pos)
        if nxt == -1:
            nxt = length
        if nxt == pos:
            # A '<' that is neither a valid open nor close tag → malformed source.
            raise ValueError(
                f"Stray '<' / malformed tag{where} at position {pos}: "
                f"{html_text[pos:pos + 30]!r}"
            )
        text_content = html_text[pos:nxt]
        # rule is the innermost active code, or None for plain text.
        # 'end' maps to None, so those characters are emitted as plain text.
        segments.append({"t": text_content, "r": stack[-1] if stack else None})
        pos = nxt

    if stack:
        raise ValueError(f"Unclosed tajweed tag(s) {stack}{where}: {html_text!r}")

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
        segments = preparse_tajweed(html_text, key=key)
        # Store as JSON string for direct insertion into database
        parsed_data[key] = json.dumps(segments, ensure_ascii=False)

    print(f"Writing parsed data to {output_path}...")
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(parsed_data, f, ensure_ascii=False, indent=2)

    print(f"Done! Parsed {len(parsed_data)} ayahs")
    return parsed_data


def preparse_single(html_text, key=None):
    """
    Pre-parse a single HTML tajweed string and return JSON string.
    Used by generate_database.py for inline conversion.

    Pass ``key`` ("surah:ayah") so SOURCE_FIXUPS are applied on the actual
    shipped-DB generation path.
    """
    segments = preparse_tajweed(html_text, key=key)
    return json.dumps(segments, ensure_ascii=False)


if __name__ == "__main__":
    # Run standalone to pre-parse the entire tajweed.json file
    json_dir = Path(__file__).parent.parent / "json"
    tajweed_path = json_dir / "tajweed.json"

    if tajweed_path.exists():
        preparse_tajweed_file(tajweed_path)
    else:
        print(f"Error: {tajweed_path} not found")
