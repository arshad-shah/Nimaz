#!/usr/bin/env python3
"""
Derived tajweed rule engine (issue #291, Track A).

Computes the tajweed rules that *neither* shipped dataset (quran.com
`tajweed.json`, cpfair `tajweed_cpfair.json`) marks, over the normalised Uthmani
text (see `preparse_tajweed.normalise_uthmani`). All rules here are the fixed,
deterministic rules of the **Hafs 'an 'Asim** reading.

⚠️ IMPORTANT — this engine is intentionally **not** wired into
`generate_database.py`. The derived recitation rules encode fiqh-of-recitation
decisions; per #291 the shipped output must be **reviewed against a printed
tajweed mushaf by a qualified reviewer** before it colours the Qur'an in the
app, and *which* rules to display (e.g. isti'la tafkhim would colour a large
fraction of all letters) is a product decision. This module provides the tested,
citable engine so that review/wiring is a follow-up, not a rewrite.

References for the rule conditions:
- Kareema Czerepinski, *Tajweed Rules of the Qur'an*, Parts I–III.
- al-Jazari, *al-Muqaddimah al-Jazariyyah*.
- cpfair/quran-tajweed (decision-tree implementation of the overlapping rules).

Codes introduced here (proposed; colours/legend are the Kotlin follow-up):
- ``tk`` Tafkhim (heavy) / ``tq`` Tarqiq (light) — for raa and the lam of the Name
- ``il`` Isti'la letter (always heavy)
- ``ml`` Madd Lin (already defined in the v3 set; populated by split_madd_lin)
- ``dm`` Idgham Mutamathilayn
- ``wq`` Waqf/stop sign (classified by classify_waqf)
"""

# ── Character constants (Uthmani diacritics) ──────────────────────────
FATHA = 'َ'
DAMMA = 'ُ'
KASRA = 'ِ'
FATHATAN = 'ً'
DAMMATAN = 'ٌ'
KASRATAN = 'ٍ'
SUKUN = 'ْ'
SHADDA = 'ّ'
SUPERSCRIPT_ALEF = 'ٰ'
ALEF_WASLA = 'ٱ'   # ٱ — hamzat al-wasl (temporary/no original kasra)
RAA = 'ر'
LAM = 'ل'
WAW = 'و'
YA = 'ي'
ALEF = 'ا'

FATHAS = {FATHA, FATHATAN}
DAMMAS = {DAMMA, DAMMATAN}
KASRAS = {KASRA, KASRATAN}
SHORT_VOWELS = FATHAS | DAMMAS | KASRAS | {SUKUN}

# Isti'la (elevated) letters — always heavy (tafkhim). خ ص ض غ ط ق ظ
ISTILA_LETTERS = set('خصضغطقظ')

# Layn letters when sakin after a fatha.
LAYN_LETTERS = {WAW, YA}

# Waqf / stop signs and their meaning (see tajweed_special_rules.json).
WAQF_SIGNS = {
    'ۘ': 'waqf_lazim',        # ۘ mim — obligatory stop
    'ۙ': 'waqf_mamnu',        # ۙ la — do not stop
    'ۚ': 'waqf_jaiz',         # ۚ jim — permissible stop
    'ۛ': 'waqf_muanaqah',     # ۛ three dots — embracing (stop at one of a pair)
    'ۖ': 'wasl_awla',         # ۖ continuing preferable
    'ۗ': 'waqf_awla',         # ۗ stopping preferable
    'ۜ': 'sakt',              # ۜ small seen — sakt (see the exceptions table)
}


def is_istila(ch):
    """True if ``ch`` is an isti'la letter (always pronounced heavy)."""
    return ch in ISTILA_LETTERS


def classify_waqf(ch):
    """Return the waqf-sign class for ``ch`` (see WAQF_SIGNS), or None."""
    return WAQF_SIGNS.get(ch)


def _prev_base(text, i):
    """Index of the base (non-mark) letter before position ``i``, or -1."""
    j = i - 1
    while j >= 0 and (text[j] in SHORT_VOWELS or text[j] in (SHADDA, SUPERSCRIPT_ALEF)):
        j -= 1
    return j


def _vowel_on(text, i):
    """The short vowel/sukun governing the letter at ``i`` (skipping a shadda or
    superscript alef that may precede it), or None if the letter is bare."""
    j = i + 1
    while j < len(text) and text[j] in (SHADDA, SUPERSCRIPT_ALEF):
        j += 1
    if j < len(text) and text[j] in SHORT_VOWELS:
        return text[j]
    return None


def raa_rule(text, i):
    """
    Classify the raa (ر) at index ``i`` as 'tk' (tafkhim/heavy) or 'tq'
    (tarqiq/light) per the Hafs rules. Returns None if ``text[i]`` is not a raa.

    Rules (Czerepinski, Part II):
      * raa with fatha / damma → heavy;
      * raa with kasra → light;
      * raa sakinah: heavy if the preceding letter carries fatha or damma;
        light if preceded by an *original* kasra — UNLESS an isti'la letter
        follows in the same word (→ heavy, e.g. قِرْطَاس, فِرْقَة). A kasra from
        hamzat al-wasl is not original, so such a raa is heavy (e.g. ٱرْجِعِى).
    """
    if text[i] != RAA:
        return None
    vowel = _vowel_on(text, i)
    if vowel in FATHAS or vowel in DAMMAS:
        return 'tk'
    if vowel in KASRAS:
        return 'tq'
    if vowel == SUKUN or vowel is None:  # raa sakinah (incl. word-final at waqf)
        p = _prev_base(text, i)
        if p < 0:
            return 'tk'
        prev_vowel = _vowel_on(text, p)
        # hamzat al-wasl before → temporary kasra → heavy
        if text[p] == ALEF_WASLA:
            return 'tk'
        if prev_vowel in FATHAS or prev_vowel in DAMMAS:
            return 'tk'
        if prev_vowel in KASRAS:
            # light, unless a following isti'la letter (same word) pulls it heavy
            nxt = i + 1
            while nxt < len(text) and text[nxt] in SHORT_VOWELS | {SHADDA, SUPERSCRIPT_ALEF}:
                nxt += 1
            if nxt < len(text) and is_istila(text[nxt]) and _vowel_on(text, nxt) != KASRA:
                return 'tk'
            return 'tq'
        # preceded by a sakin letter: look one further back
        pp = _prev_base(text, p)
        pp_vowel = _vowel_on(text, pp) if pp >= 0 else None
        if pp_vowel in KASRAS and not is_istila(text[p]):
            return 'tq'
        return 'tk'
    return 'tk'


def lam_of_name_rule(text, i):
    """
    Classify the lam of the Name (لفظ الجلالة, ٱللَّه) at index ``i`` as 'tk'
    (heavy) or 'tq' (light). Returns None if the lam at ``i`` is not the lam of
    the Name (heuristic: lam+shadda inside ...ٱللَّه...).

    Heavy after fatha or damma; light after kasra (Czerepinski, Part I).
    """
    if text[i] != LAM:
        return None
    # must be the doubled lam of Allah: preceded by alef-wasla+lam, shadda on it
    if i + 1 < len(text) and text[i + 1] == SHADDA:
        p = _prev_base(text, i)
        if p >= 0 and text[p] == LAM:
            q = _prev_base(text, p)
            if q >= 0 and text[q] == ALEF_WASLA:
                # The vowel that governs tafkhim/tarqiq is the one *before* the
                # Name. hamzat al-wasl elides in continuation, so look across any
                # word boundary (space) to the previous letter's vowel.
                r = q - 1
                while r >= 0 and text[r] == ' ':
                    r -= 1
                while r >= 0 and text[r] not in SHORT_VOWELS and not text[r].isalpha():
                    r -= 1
                # r now on a vowel mark or a letter; find the governing vowel
                prev_vowel = None
                k = r
                while k >= 0:
                    if text[k] in SHORT_VOWELS:
                        prev_vowel = text[k]
                        break
                    if text[k].isalpha():
                        # letter with no following mark we passed → sukun-like; stop
                        break
                    k -= 1
                if prev_vowel in KASRAS:
                    return 'tq'
                return 'tk'
    return None


def is_madd_lin(text, i):
    """
    True if the layn letter (و/ي sakin after fatha) at ``i`` is a Madd Lin — i.e.
    it is followed by one consonant that is stopped upon (word-final at a waqf).
    This is the subset of 'Aarid (`ma`) spans whose elongated letter is a layn
    letter, so it should render as `ml` rather than `ma`.
    """
    if text[i] not in LAYN_LETTERS:
        return False
    if _vowel_on(text, i) != SUKUN:
        return False
    p = _prev_base(text, i)
    if p < 0 or _vowel_on(text, p) not in FATHAS:
        return False
    # a following base consonant that ends the word (next is space / end)
    nxt = i + 1
    while nxt < len(text) and text[nxt] in SHORT_VOWELS | {SHADDA, SUPERSCRIPT_ALEF}:
        nxt += 1
    if nxt >= len(text):
        return True
    # one consonant then a word boundary
    after = nxt + 1
    while after < len(text) and text[after] in SHORT_VOWELS | {SHADDA, SUPERSCRIPT_ALEF}:
        after += 1
    return after >= len(text) or text[after] == ' '


def idgham_mutamathilayn(text):
    """
    Yield (index, length) spans of Idgham Mutamathilayn: two identical adjacent
    letters where the first is sakin (across a word boundary), so the first
    merges into the second. Excludes the ghunnah/madd letters already covered by
    other rules (noon, meem — those are their own idgham categories; و/ي/ا as
    madd letters are excluded).
    """
    spans = []
    EXCLUDED = {'ن', 'م', WAW, YA, ALEF}  # noon, meem, madd letters
    i = 0
    n = len(text)
    while i < n:
        ch = text[i]
        if ch.isalpha() and ch not in EXCLUDED:
            vowel = _vowel_on(text, i)
            # first letter must be sakin: an explicit sukun, or word-final (the
            # next base is across a space, so it carries no vowel of its own).
            nxt = i + 1
            while nxt < n and text[nxt] in SHORT_VOWELS | {SHADDA, SUPERSCRIPT_ALEF}:
                nxt += 1
            is_sakin = vowel == SUKUN or (vowel is None and nxt < n and text[nxt] == ' ')
            if is_sakin:
                j = nxt
                while j < n and text[j] == ' ':
                    j += 1
                if j < n and text[j] == ch and _vowel_on(text, j) != SUKUN:
                    spans.append((i, 1))
        i += 1
    return spans
