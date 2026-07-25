#!/usr/bin/env python3
"""
Pre-parse Tajweed HTML to simplified JSON format for efficient rendering.

Converts: <tajweed class="ghunnah">text</tajweed>
To: [{"t": "text", "r": "g"}]

This eliminates runtime regex parsing in the Android app.

V3 rule codes — each tajweed sub-type gets its own code so the app can
assign distinct colors matching a standard colour-coded mushaf. V3 (issue
#289) corrects the madd taxonomy: `madda_obligatory` is split into Madd Jaiz
Munfasil (`mf`) and Madd Wajib Muttasil (`mt`), `madda_permissible` is
re-labelled Madd 'Aarid lis-Sukun (`ma`), and qalqalah is split into
sughra/kubra (`qs`/`qk`). See the RULE_CODES map below.

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
  three segments — ``ُوٓ`` = ``mt``, ``اْ`` = ``sl``, ``‌ۚ`` = ``mt`` (the
  ``madda_obligatory`` code; see the V3 taxonomy below) — rather than one
  mislabelled span with leaked markup.

Because of this, per-*segment* (or per-*code*) span counts are **not** a
conservation invariant — an outer tag split by an inner produces 2+ segments,
an outer tag fully covered by an inner produces 0, and the later alignment
stage (issue #290) merges adjacent same-rule segments. The correct invariant
is **character coverage**, at two levels:

1. concatenating every segment's text reconstructs the source text exactly
   (tags and verse markers removed), so no character is ever dropped; and
2. every character that carried a rule keeps a rule through the whole pipeline
   (tokenize → taxonomy split → canonical alignment) and no character gains
   one — see ``align_segments_to_canonical`` (rules are conserved on the
   delete path too). Assert this at the *end* of the pipeline, not after the
   tokenizer alone.
"""

import difflib
import json
import re
import unicodedata
from pathlib import Path

# ── V3 Rule Code Map ──────────────────────────────────────────────────
# Every class from the Quran.com uthmani_tajweed API is mapped to a short
# code.  This lets the Android renderer show a different colour for each
# tajweed sub-rule, matching printed tajweed mushafs.
#
# V3 (issue #289) corrects the madd taxonomy. The quran.com source merges two
# distinct rules under `madda_obligatory` and mis-labels `madda_permissible`;
# cross-validated against the independent `tajweed_cpfair.json` dataset the
# real split is:
#
#   quran.com `madda_obligatory` (5166)  =  Madd Jaiz Munfasil   (`mf`, 2/4/5)
#                                         +  Madd Wajib Muttasil  (`mt`, 4/5)
#   quran.com `madda_permissible`(4543)  =  Madd 'Aarid lis-Sukun (`ma`, 2/4/6)
#
# The munfasil/muttasil split is applied at the *source-tag* level in
# reclassify_madd_obligatory() using cpfair's per-ayah ordering (see below);
# `madd_munfasil` / `madd_muttasil` are therefore synthetic classes injected
# before tokenizing.  Qalqalah is split into sughra/kubra positionally by
# split_qalqalah().  Beat counts follow the Hafs 'an 'Asim reading (ref:
# Kareema Czerepinski, *Tajweed Rules of the Qur'an*, Parts I–III).
#
# Base classes present in tajweed.json (18): end, ghunnah, ham_wasl,
#   idgham_ghunnah, idgham_mutajanisayn, idgham_mutaqaribayn, idgham_shafawi,
#   idgham_wo_ghunnah, ikhafa, ikhafa_shafawi, iqlab, laam_shamsiyah,
#   madda_necessary, madda_normal, madda_obligatory, madda_permissible,
#   qalaqah, slnt
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

    # ── Qalqalah (echoing / bouncing) ── split into sughra/kubra by
    #    split_qalqalah(); 'q' is the intermediate/legacy code.
    'qalqalah': 'q',
    'qalaqah': 'q',

    # ── Madd Tabee'i / Normal (natural elongation, 2 beats) ──
    'madd': 'mn',
    'madd_normal': 'mn',
    'madda_normal': 'mn',

    # ── Madd 'Aarid lis-Sukun (2/4/6 beats) — what `madda_permissible`
    #    actually marks (cpfair `madd_246`), NOT munfasil. ──
    'madd_permissible': 'ma',
    'madda_permissible': 'ma',
    'madd_246': 'ma',

    # ── Madd Jaiz Munfasil (permissible, 2/4/5 beats) — synthetic class,
    #    split out of `madda_obligatory` by reclassify_madd_obligatory(). ──
    'madd_munfasil': 'mf',

    # ── Madd Wajib Muttasil (obligatory, 4/5 beats) — the true meaning of
    #    `madda_obligatory`; unsplit/fallback `madda_obligatory` also maps
    #    here (conservative obligatory default). ──
    'madd_muttasil': 'mt',
    'madd_obligatory': 'mt',
    'madda_obligatory': 'mt',

    # ── Madd Lazim / Necessary (6 beats) ──
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

# Matches a `madda_obligatory` opening tag, capturing the surrounding markup so
# only the class name is rewritten (issue #289 munfasil/muttasil split).
MADD_OBLIGATORY_TAG = re.compile(
    r'(<tajweed\s+class=(["\']?))madda_obligatory(\2\s*>)'
)

# Qalqalah letters (qutb jadd — ق ط ب ج د). Used to split qalqalah into
# sughra (medial) vs kubra (word-final) positionally.
QALQALAH_LETTERS = set('قطبجد')

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


def cpfair_madd_sequence(cpfair_entry):
    """Ordered munfasil/muttasil rules for an ayah's cpfair entry, in text order.

    ``cpfair_entry`` is one record of ``tajweed_cpfair.json``
    (``{"surah", "ayah", "annotations": [{"start", "end", "rule"}]}``). Returns
    the ``madd_munfasil`` / ``madd_muttasil`` rules sorted by start offset — the
    reading-order sequence the ``madda_obligatory`` tags are matched against.
    """
    if not cpfair_entry:
        return []
    return [
        a['rule']
        for a in sorted(cpfair_entry['annotations'], key=lambda a: a['start'])
        if a['rule'] in ('madd_munfasil', 'madd_muttasil')
    ]


def reclassify_madd_obligatory(html_text, cpfair_entry):
    """
    Split the quran.com ``madda_obligatory`` tags into Madd Jaiz Munfasil and
    Madd Wajib Muttasil (issue #289), rewriting the class name *at source* so
    the tokenizer handles nesting naturally.

    quran.com merges both rules under one class; the independent cpfair dataset
    distinguishes them. Per-ayah the two sources agree on the total count for
    6 227/6 236 ayahs; where the i-th ``madda_obligatory`` tag (in text order)
    lines up with cpfair's i-th munfasil/muttasil annotation, the class is
    rewritten to the synthetic ``madd_munfasil`` / ``madd_muttasil`` (→ ``mf`` /
    ``mt``). When the counts disagree (9 ayahs) the tags are left as
    ``madda_obligatory`` — which maps to ``mt`` (the conservative obligatory
    default), so nothing regresses.
    """
    seq = cpfair_madd_sequence(cpfair_entry)
    if not seq:
        return html_text
    if len(MADD_OBLIGATORY_TAG.findall(html_text)) != len(seq):
        return html_text  # count mismatch → leave as madda_obligatory (→ mt)

    rules = iter(seq)

    def _rewrite(match):
        rule = next(rules)
        synthetic = 'madd_munfasil' if rule == 'madd_munfasil' else 'madd_muttasil'
        return match.group(1) + synthetic + match.group(3)

    return MADD_OBLIGATORY_TAG.sub(_rewrite, html_text)


def _is_combining(ch):
    """True for a non-spacing / combining mark (Unicode category starting 'M')."""
    return unicodedata.category(ch).startswith('M')


def split_qalqalah(segments):
    """
    Split qalqalah (``q``) segments into Sughra (``qs``) and Kubra (``qk``).

    Qalqalah Kubra ("greater") applies when the qalqalah letter is **word-final**
    (stopped on); Sughra ("lesser") applies mid-word. This is derived positionally
    from the surrounding text: a ``q`` segment is Kubra when the next base letter
    (skipping combining marks) is a word boundary or the ayah end, else Sughra.

    This is the standard simplified pedagogy; a fuller treatment keyed to actual
    waqf/stop marks is left to the extended-rules pass (#291).
    """
    full = ''.join(seg['t'] for seg in segments)
    starts = []
    pos = 0
    for seg in segments:
        starts.append(pos)
        pos += len(seg['t'])

    for i, seg in enumerate(segments):
        if seg['r'] != 'q':
            continue
        j = starts[i] + len(seg['t'])
        next_base = None
        while j < len(full):
            ch = full[j]
            if ch == ' ' or ch == '‌':  # space or ZWNJ → word boundary
                next_base = ' '
                break
            if not _is_combining(ch):
                next_base = ch
                break
            j += 1
        seg['r'] = 'qk' if next_base in (None, ' ') else 'qs'
    return segments


def preparse_tajweed(html_text, key=None, cpfair_entry=None):
    """
    Convert HTML tajweed text to simplified JSON segments.

    Uses a stack-based tokenizer so arbitrarily nested ``<tajweed>`` tags are
    handled correctly (see the module docstring). The innermost rule wins for
    the characters it covers; the enclosing rule is kept for the rest.

    Args:
        html_text: Text with <tajweed class="rule">text</tajweed> markup.
        key: Optional "surah:ayah" key used to (a) apply SOURCE_FIXUPS and
             (b) produce actionable error messages.
        cpfair_entry: Optional cpfair record for this ayah. When given, the
             ``madda_obligatory`` tags are split into munfasil/muttasil before
             tokenizing (issue #289).

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

    if cpfair_entry is not None:
        html_text = reclassify_madd_obligatory(html_text, cpfair_entry)

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

    # Split qalqalah into sughra/kubra positionally (issue #289).
    split_qalqalah(segments)

    return segments


# ── Orthography normalisation (issue #290) ────────────────────────────
# The quran.com tajweed markup and the app's canonical `ayahs.text_arabic`
# are two different Uthmani transcriptions of the same verse — they disagree
# on 87% of ayahs (tatweel carriers, ZWNJ, pause-mark encodings, tanween
# forms, alef variants, …). If the two disagree, toggling "Show Tajweed
# Colors" changes the glyphs on screen (word widths, line breaks), not just
# their colour, and no single string can back search / bookmarks / audio
# highlighting.
#
# Rather than hand-reconcile 60+ context-dependent substitutions (fragile,
# and it would risk silently mis-colouring letters), we keep `text_arabic`
# as the single canonical text and *re-derive* the coloured segments by
# aligning the stripped tajweed text onto it and transferring rule labels.
# This guarantees ``strip(text_tajweed) == normalise_uthmani(text_arabic)``
# byte-for-byte, by construction.

# Non-semantic marks removed from the canonical text. Only the BOM appears in
# the shipped ayahs.json (on 1:1), but strip the common zero-width marks too
# so a stray one can never leak into a string comparison / share action.
_NON_SEMANTIC = str.maketrans({
    '﻿': None,  # ZERO WIDTH NO-BREAK SPACE / BOM
    '​': None,  # ZERO WIDTH SPACE
    '‌': None,  # ZERO WIDTH NON-JOINER
    '‍': None,  # ZERO WIDTH JOINER
})


def normalise_uthmani(text):
    """
    Normalise a canonical Uthmani ayah string.

    Strips the BOM and stray zero-width marks (non-semantic here) and trims
    the outer whitespace. ``text_arabic`` is treated as canonical, so this is
    intentionally light — it must not alter the glyphs the app already renders
    for search, bookmarks and non-tajweed display.
    """
    if not text:
        return text
    return text.translate(_NON_SEMANTIC).strip()


def align_segments_to_canonical(segments, canonical_text):
    """
    Re-derive coloured segments over ``canonical_text`` from ``segments``.

    **Boundary semantics (issue #299).** A rule span covers exactly the
    characters the *source* tagged, transferred 1:1 onto the canonical text — the
    **phonetic unit**, not "base letter + all its marks". So a span may begin on
    a combining mark that phonetically belongs to the rule: e.g. in ``1:1`` the
    madd span is ``ِي`` (the kasra + yā that together form the madd), which starts
    on the kasra positioned under the previous letter. This is deliberate: the
    rule genuinely applies to that vowel→letter pair. The cluster-split count is
    tracked as a metric (``tests/fixtures/grapheme_boundary_baseline.json``) so it
    cannot silently grow; on-device mark-placement verification with the shipping
    Arabic font is the remaining part of #299.

    The returned segments are built out of ``canonical_text``'s own characters
    (so their concatenation equals ``canonical_text`` exactly) while carrying
    the tajweed rule of the aligned source character. A character-level diff
    aligns the stripped source text to the canonical text:

    * **equal** run → copy each character's rule across;
    * **replace** run → the canonical characters inherit the last non-null
      rule in the replaced source region (the rule usually sits on a mark that
      differs in encoding, e.g. a small-yeh madd), else null;
    * **insert** run (canonical has extra characters, e.g. a pause mark absent
      from the tajweed source) → inherit the preceding canonical rule, since
      such characters are combining marks attached to the base before them;
    * **delete** run (source-only characters — tatweel, ZWNJ, or a small-waw
      madd with no canonical glyph) → the run carries no glyph in the canonical
      text, but if it carried a *rule* that would otherwise vanish, that rule is
      transferred to a surviving neighbour (issue #290 follow-up: **rules are
      conserved** — every character that carried a rule in the source keeps a
      home). Purely non-semantic deletes (tatweel/ZWNJ, no rule) place nothing.

    Rules are conserved character-for-character: no source character's rule is
    silently dropped. Adjacent characters with the same rule are then merged
    into one segment (so per-*segment* counts may shrink vs the source, but the
    coloured character coverage is identical).
    """
    source_text = ''.join(seg['t'] for seg in segments)
    source_rules = []
    for seg in segments:
        source_rules.extend([seg['r']] * len(seg['t']))

    canon_rules = [None] * len(canonical_text)
    deleted_rules = []  # (boundary_index, rule) for the conservation pass
    matcher = difflib.SequenceMatcher(None, source_text, canonical_text, autojunk=False)
    for op, i1, i2, j1, j2 in matcher.get_opcodes():
        if op == 'equal':
            for offset in range(j2 - j1):
                canon_rules[j1 + offset] = source_rules[i1 + offset]
        elif op == 'replace':
            region = [r for r in source_rules[i1:i2] if r is not None]
            chosen = region[-1] if region else None
            for k in range(j1, j2):
                canon_rules[k] = chosen
        elif op == 'insert':
            for k in range(j1, j2):
                canon_rules[k] = canon_rules[k - 1] if k > 0 else None
        elif op == 'delete':
            region = [r for r in source_rules[i1:i2] if r is not None]
            if region:
                deleted_rules.append((j1, region[-1]))

    # Conservation pass for deletes. A source-only character carries no canonical
    # glyph, but if it carried a rule that would otherwise vanish, transfer that
    # rule to a surviving neighbour — UNLESS the rule already survives on an
    # adjacent character (e.g. a deleted tatweel inside a madd whose superscript
    # alef keeps the rule; no transfer needed). Runs after the main pass so both
    # neighbours are known. Prefer the preceding character (the base/vowel the
    # elongation attaches to); fall back to the following one.
    for j1, rule in deleted_rules:
        before = canon_rules[j1 - 1] if j1 - 1 >= 0 else None
        after = canon_rules[j1] if j1 < len(canon_rules) else None
        if rule in (before, after):
            continue  # rule already has a home on a neighbour
        if before is None and j1 - 1 >= 0:
            canon_rules[j1 - 1] = rule
        elif after is None and j1 < len(canon_rules):
            canon_rules[j1] = rule

    merged = []
    for ch, rule in zip(canonical_text, canon_rules):
        if merged and merged[-1]['r'] == rule:
            merged[-1]['t'] += ch
        else:
            merged.append({'t': ch, 'r': rule})
    return merged


# Sun letters (for deriving Lam Shamsiyyah in the backfill).
_SUN_LETTERS = set('تثدذرزسشصضطظلن')
_ALEF_WASLA = 'ٱ'


def apply_derived_rules(segments, canonical_text, tafkhim=True):
    """
    Overlay the derived rules (issue #291) that neither shipped source marks.
    Operates on aligned segments (whose text equals ``canonical_text``); only
    sets a rule on a character that is currently plain (``r is None``), or splits
    an existing ``ma`` ('Aarid) run into ``ml`` (Lin) — it never overrides a
    source rule, and never changes the text, so the #290 round-trip is preserved.

    Always applied (deterministic, validated in test_tajweed_rules.py):

    * **Madd Lin** (``ml``) — an 'Aarid run whose elongated letter is a layn
      letter (و/ي sakin after fatha at a stop) is Lin, not 'Aarid.
    * **Waqf signs** (``wq``) — the 7 stop signs, previously unstyled.
    * **Idgham Mutamathilayn** (``dm``) — identical adjacent letters, first sakin.
    * **Hamzat al-Wasl** (``hw``) — derived where the source left it unmarked
      (also backfills 5 of the 63 fully-unannotated ayahs, #298).

    When ``tafkhim`` is true (default), also the **heavy/light** rules, which are
    deterministic (position + vowel — no pause dependence, unlike madd) and
    validated against authoritative examples (see test_tajweed_rules.py):

    * **Tafkhim** (``tk``) / **Tarqiq** (``tq``) — for Raa (``raa_rule``) and the
      Lam of the Name (``lam_of_name_rule``).

    NB: Madd Tabee'i is NOT derived for the unannotated ayahs — the aarid/tabee'i
    distinction needs pause-point awareness that only cpfair's trained model (or
    a scholar) has, and cpfair itself has the same 63-ayah gap (verified by
    running its engine). Isti'la (always-heavy) is available as
    ``tajweed_rules.is_istila`` but not auto-applied (it would colour the 7
    isti'la letters everywhere).
    """
    import tajweed_rules as TR  # lazy import (tajweed_rules imports this module)

    chars = []
    rules = []
    for seg in segments:
        for ch in seg['t']:
            chars.append(ch)
            rules.append(seg['r'])
    n = len(chars)

    # Madd Lin: relabel a 'ma' run whose layn letter qualifies.
    i = 0
    while i < n:
        if rules[i] == 'ma':
            j = i
            while j < n and rules[j] == 'ma':
                j += 1
            if any(TR.is_madd_lin(canonical_text, k) for k in range(i, j)):
                for k in range(i, j):
                    rules[k] = 'ml'
            i = j
        else:
            i += 1

    # Waqf signs (only where plain).
    for k, ch in enumerate(chars):
        if rules[k] is None and TR.classify_waqf(ch):
            rules[k] = 'wq'

    # Idgham Mutamathilayn (first letter of the pair, only where plain).
    for start, length in TR.idgham_mutamathilayn(canonical_text):
        for k in range(start, start + length):
            if rules[k] is None:
                rules[k] = 'dm'

    # Hamzat al-Wasl (only where plain) — the ٱ character IS the connecting hamza.
    for k, ch in enumerate(chars):
        if rules[k] is None and ch == _ALEF_WASLA:
            rules[k] = 'hw'

    # NB: Lam Shamsiyyah is intentionally NOT derived here. The naive
    # "ٱ + ل + sun letter" rule can't tell the definite article from a root lam
    # (e.g. the verb ٱلْتَقَى), so it over-fires badly. The source already marks
    # Lam Shamsiyyah for annotated ayahs; deriving it for the unannotated ones
    # (#298) needs the reviewed engine.

    # Tafkhim (tk) / Tarqiq (tq): heavy vs light — deterministic (position +
    # vowel). Applied to plain Raa and the plain Lam of the Name.
    if tafkhim:
        for k, ch in enumerate(chars):
            if rules[k] is not None:
                continue
            if ch == 'ر':
                rules[k] = TR.raa_rule(canonical_text, k)  # 'tk' or 'tq'
            elif ch == 'ل':
                lam = TR.lam_of_name_rule(canonical_text, k)
                if lam:
                    rules[k] = lam

    merged = []
    for ch, r in zip(chars, rules):
        if merged and merged[-1]['r'] == r:
            merged[-1]['t'] += ch
        else:
            merged.append({'t': ch, 'r': r})
    return merged


def load_cpfair(cpfair_path):
    """Load ``tajweed_cpfair.json`` into a ``{"surah:ayah": entry}`` lookup."""
    with open(cpfair_path, 'r', encoding='utf-8') as f:
        return {f"{e['surah']}:{e['ayah']}": e for e in json.load(f)}


def preparse_tajweed_file(input_path, output_path=None, ayahs_path=None,
                          cpfair_path=None):
    """
    Pre-parse an entire tajweed.json file.

    Args:
        input_path: Path to tajweed.json (dict with "surah:ayah" keys)
        output_path: Optional output path (defaults to tajweed_parsed.json)
        ayahs_path: Optional path to ayahs.json. When given, each ayah's
            segments are re-derived over the normalised canonical
            ``text_arabic`` (issue #290) so the reference artifact matches the
            shipped DB (``strip(text_tajweed) == normalise_uthmani(text_arabic)``).
        cpfair_path: Optional path to tajweed_cpfair.json. When given, the madd
            taxonomy is split into munfasil/muttasil (issue #289).
    """
    input_path = Path(input_path)
    if output_path is None:
        output_path = input_path.parent / "tajweed_parsed.json"
    else:
        output_path = Path(output_path)

    print(f"Loading tajweed data from {input_path}...")
    with open(input_path, 'r', encoding='utf-8') as f:
        tajweed_data = json.load(f)

    canonical_by_key = {}
    if ayahs_path is not None:
        with open(ayahs_path, 'r', encoding='utf-8') as f:
            for a in json.load(f):
                canonical_by_key[f"{a['surah_id']}:{a['number_in_surah']}"] = a['text_arabic']
        print(f"Loaded {len(canonical_by_key)} canonical ayah texts from {ayahs_path}")

    cpfair_by_key = {}
    if cpfair_path is not None:
        cpfair_by_key = load_cpfair(cpfair_path)
        print(f"Loaded {len(cpfair_by_key)} cpfair entries from {cpfair_path}")

    print(f"Pre-parsing {len(tajweed_data)} entries...")
    parsed_data = {}
    for key, html_text in tajweed_data.items():
        segments = preparse_tajweed(html_text, key=key,
                                    cpfair_entry=cpfair_by_key.get(key))
        canonical = canonical_by_key.get(key)
        if canonical is not None:
            norm = normalise_uthmani(canonical)
            segments = align_segments_to_canonical(segments, norm)
            segments = apply_derived_rules(segments, norm)
        # Store as JSON string for direct insertion into database
        parsed_data[key] = json.dumps(segments, ensure_ascii=False)

    print(f"Writing parsed data to {output_path}...")
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(parsed_data, f, ensure_ascii=False, indent=2)

    print(f"Done! Parsed {len(parsed_data)} ayahs")
    return parsed_data


def preparse_single(html_text, key=None, canonical_text=None, cpfair_entry=None):
    """
    Pre-parse a single HTML tajweed string and return JSON string.
    Used by generate_database.py for inline conversion.

    Pass ``key`` ("surah:ayah") so SOURCE_FIXUPS are applied on the actual
    shipped-DB generation path.

    Pass ``cpfair_entry`` (the ayah's ``tajweed_cpfair.json`` record) to split
    the madd taxonomy into munfasil/muttasil (issue #289).

    Pass ``canonical_text`` (the ayah's ``text_arabic``) to re-derive the
    coloured segments over the normalised canonical text (issue #290), so the
    stored segments round-trip to ``normalise_uthmani(text_arabic)`` exactly.
    When omitted, the raw tag-parsed segments are returned unchanged.
    """
    segments = preparse_tajweed(html_text, key=key, cpfair_entry=cpfair_entry)
    if canonical_text is not None:
        norm = normalise_uthmani(canonical_text)
        segments = align_segments_to_canonical(segments, norm)
        # Overlay the unambiguous derived rules (Madd Lin, waqf, idgham
        # mutamathilayn, hamzat wasl) — issue #291.
        segments = apply_derived_rules(segments, norm)
    return json.dumps(segments, ensure_ascii=False)


if __name__ == "__main__":
    # Run standalone to pre-parse the entire tajweed.json file, re-deriving
    # against the canonical ayah text when ayahs.json is available.
    json_dir = Path(__file__).parent.parent / "json"
    tajweed_path = json_dir / "tajweed.json"
    ayahs_path = json_dir / "ayahs.json"
    cpfair_path = json_dir / "tajweed_cpfair.json"

    if tajweed_path.exists():
        preparse_tajweed_file(
            tajweed_path,
            ayahs_path=ayahs_path if ayahs_path.exists() else None,
            cpfair_path=cpfair_path if cpfair_path.exists() else None,
        )
    else:
        print(f"Error: {tajweed_path} not found")
