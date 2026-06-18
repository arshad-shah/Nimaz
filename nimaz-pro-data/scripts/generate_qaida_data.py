#!/usr/bin/env python3
"""
Nimaz Pro — Noorani Qaida data generator (Issue #172, sub-issue A of #171).

Deterministically authors the four Qaida JSON files consumed later by the
DB pipeline (sub-issue C):

    json/qaida_letters.json   — the 29-letter reference table
    json/qaida_lessons.json   — the 17 lesson definitions
    json/qaida_lines.json     — printed rows inside each lesson
    json/qaida_cells.json     — individual tappable tokens within each line

Design goals
------------
* Reproducible: re-running this script regenerates byte-identical JSON, so the
  content is derived rather than hand-maintained.
* Positional letter forms are generated programmatically using the Arabic
  joining model (the tatweel/kashida U+0640 visually marks where a glyph
  connects), rather than hand-typing presentation forms.
* Transliteration scaffolding and audio_key assignment are derived from a small
  per-letter table so every cell ends up with a unique, stable audio_key
  (the actual audio clips are produced in sub-issue B).

No app code is touched here — this is pure data authoring + this script.
"""

import json
import re
import unicodedata
from pathlib import Path

# ── Paths ─────────────────────────────────────────────────────────────
SCRIPT_DIR = Path(__file__).resolve().parent
JSON_DIR = SCRIPT_DIR.parent / "json"

# ── Arabic combining marks ────────────────────────────────────────────
TATWEEL = "ـ"        # ـ  kashida — marks a connection point
FATHA = "َ"          # ◌َ
KASRA = "ِ"          # ◌ِ
DAMMA = "ُ"          # ◌ُ
FATHATAYN = "ً"      # ◌ً
KASRATAYN = "ٍ"      # ◌ٍ
DAMMATAYN = "ٌ"      # ◌ٌ
SUKOON = "ْ"         # ◌ْ
SHADDA = "ّ"         # ◌ّ
DAGGER_ALIF = "ٰ"    # ◌ٰ  standing fatha (alif khanjariyya)
SUBSCRIPT_ALEF = "ٖ"  # ◌ٖ standing kasra
INVERTED_DAMMA = "ٗ"  # ◌ٗ inverted damma
ALIF = "ا"           # ا
WAW = "و"            # و
YAA = "ي"            # ي

# ──────────────────────────────────────────────────────────────────────
# Letter reference table.
#
# Order is the canonical Qaida order: 28 letters + hamzah (29 total).
# `slug`  — unique ASCII id used to build stable audio keys.
# `sound` — natural transliteration of the bare consonant.
# `connecting` — False for the 6 non-connectors (ا د ذ ر ز و); hamzah is
#                handled as a special non-joining glyph below.
# makhraj_area ∈ {JAWF, HALQ, LISAN, SHAFATAIN, KHAYSHUM}
# ──────────────────────────────────────────────────────────────────────
LETTERS = [
    # ar, name_ar, name_translit, slug, sound, area, detail, hint, connecting
    ("ا", "أَلِف", "alif", "alif", "a", "JAWF",
     "Jawf (the empty space of the mouth and throat); a carrier/elongation with no fixed contact point.",
     "like the long 'a' in 'father' (when used for elongation)", False),
    ("ب", "بَاء", "baa", "ba", "b", "SHAFATAIN",
     "The inner part of both lips pressed together.",
     "like 'b' in 'book'", True),
    ("ت", "تَاء", "taa", "ta", "t", "LISAN",
     "Tip of the tongue against the roots (gums) of the upper front teeth.",
     "like 't' in 'table'", True),
    ("ث", "ثَاء", "thaa", "tha", "th", "LISAN",
     "Tip of the tongue lightly touching the edges of the upper front teeth.",
     "like 'th' in 'think'", True),
    ("ج", "جِيم", "jeem", "jim", "j", "LISAN",
     "Middle of the tongue against the roof of the mouth (hard palate).",
     "like 'j' in 'jam'", True),
    ("ح", "حَاء", "Haa", "hha", "h", "HALQ",
     "Middle of the throat (heavy, whispered).",
     "a deep, breathy 'h' from the middle of the throat (no English equivalent)", True),
    ("خ", "خَاء", "khaa", "kha", "kh", "HALQ",
     "Nearest (upper) part of the throat.",
     "like 'ch' in the Scottish 'loch'", True),
    ("د", "دَال", "daal", "dal", "d", "LISAN",
     "Tip of the tongue against the roots of the upper front teeth.",
     "like 'd' in 'door'", False),
    ("ذ", "ذَال", "dhaal", "dhal", "dh", "LISAN",
     "Tip of the tongue lightly touching the edges of the upper front teeth.",
     "like 'th' in 'this'", False),
    ("ر", "رَاء", "raa", "ra", "r", "LISAN",
     "Tip of the tongue (slightly back) against the gum, with a light trill.",
     "a lightly rolled 'r'", False),
    ("ز", "زَاي", "zaay", "za", "z", "LISAN",
     "Tip of the tongue near the back of the lower front teeth.",
     "like 'z' in 'zebra'", False),
    ("س", "سِين", "seen", "sin", "s", "LISAN",
     "Tip of the tongue near the back of the lower front teeth.",
     "like 's' in 'sun'", True),
    ("ش", "شِين", "sheen", "shin", "sh", "LISAN",
     "Middle of the tongue against the hard palate, with air spread.",
     "like 'sh' in 'ship'", True),
    ("ص", "صَاد", "Saad", "sad", "s", "LISAN",
     "Tip of the tongue near the back of the lower front teeth (heavy/emphatic).",
     "a heavy, emphatic 's'", True),
    ("ض", "ضَاد", "Daad", "dad", "d", "LISAN",
     "One or both sides of the tongue against the upper molars (unique to Arabic).",
     "a heavy, emphatic 'd'", True),
    ("ط", "طَاء", "Taa", "tta", "t", "LISAN",
     "Tip of the tongue against the roots of the upper front teeth (heavy/emphatic).",
     "a heavy, emphatic 't'", True),
    ("ظ", "ظَاء", "Zaa", "zha", "z", "LISAN",
     "Tip of the tongue lightly touching the edges of the upper front teeth (heavy/emphatic).",
     "a heavy, emphatic 'th'/'dh'", True),
    ("ع", "عَيْن", "ayn", "ain", "'", "HALQ",
     "Middle of the throat.",
     "a deep throat sound made by tightening the middle of the throat (no English equivalent)", True),
    ("غ", "غَيْن", "ghayn", "ghain", "gh", "HALQ",
     "Nearest (upper) part of the throat.",
     "like a gargled French 'r'", True),
    ("ف", "فَاء", "faa", "fa", "f", "SHAFATAIN",
     "Inner edge of the upper front teeth against the inside of the lower lip.",
     "like 'f' in 'fish'", True),
    ("ق", "قَاف", "qaaf", "qaf", "q", "LISAN",
     "Deepest part of the tongue (its root) against the soft palate.",
     "a deep 'k' produced from the back of the throat", True),
    ("ك", "كَاف", "kaaf", "kaf", "k", "LISAN",
     "Back of the tongue against the palate, just below the qaaf point.",
     "like 'k' in 'kite'", True),
    ("ل", "لَام", "laam", "lam", "l", "LISAN",
     "Tip and sides of the tongue against the gums of the upper front teeth.",
     "like 'l' in 'lamp'", True),
    ("م", "مِيم", "meem", "mim", "m", "SHAFATAIN",
     "Both lips pressed together (with nasal ghunnah from the khayshum).",
     "like 'm' in 'moon'", True),
    ("ن", "نُون", "noon", "nun", "n", "LISAN",
     "Tip of the tongue against the gum of the upper front teeth (with nasal ghunnah from the khayshum).",
     "like 'n' in 'noon'", True),
    ("ه", "هَاء", "haa", "ha", "h", "HALQ",
     "Deepest (lowest) part of the throat.",
     "like 'h' in 'house'", True),
    ("و", "وَاو", "waw", "waw", "w", "SHAFATAIN",
     "Rounding of both lips.",
     "like 'w' in 'water' (or long 'oo' when used for elongation)", False),
    ("ي", "يَاء", "yaa", "ya", "y", "LISAN",
     "Middle of the tongue against the hard palate.",
     "like 'y' in 'yes' (or long 'ee' when used for elongation)", True),
    ("ء", "هَمْزَة", "hamzah", "hamza", "'", "HALQ",
     "Deepest (lowest) part of the throat (a glottal stop).",
     "a glottal catch, like the break in 'uh-oh'", False),
]

# Letters that genuinely never join (no initial/medial forms).
NON_CONNECTING = {"ا", "د", "ذ", "ر", "ز", "و"}

# ──────────────────────────────────────────────────────────────────────
# 17 lesson definitions (see #171 curriculum table).
# ──────────────────────────────────────────────────────────────────────
LESSONS = [
    (1, "The Letters", "الحُرُوفُ المُفْرَدَة", "Al-Huroof Al-Mufradah",
     "Learn the 29 Arabic letters and their names in their isolated form.",
     ["letters", "isolated"], "🔤"),
    (2, "Joined Letters", "الحُرُوفُ المُرَكَّبَة", "Al-Huroof Al-Murakkabah",
     "See how letters change shape when joined in initial, medial and final forms.",
     ["letters", "joining", "forms"], "🔗"),
    (3, "Disjoined Letters", "الحُرُوفُ المُقَطَّعَة", "Al-Huroof Al-Muqatta'ah",
     "The disjoined letters that open certain surahs, read by their letter names with elongation.",
     ["muqattaat", "elongation"], "✨"),
    (4, "Harakat (Short Vowels)", "الحَرَكَات", "Al-Harakat",
     "Fatha (a), Kasra (i) and Damma (u) — the three short vowels placed on letters.",
     ["harakat", "fatha", "kasra", "damma"], "◌"),
    (5, "Tanween", "التَّنْوِين", "At-Tanween",
     "Fathatayn (an), Kasratayn (in) and Dammatayn (un) — the doubled-vowel endings.",
     ["tanween"], "◌ً"),
    (6, "Harakat & Tanween Drills", "تَمَارِين الحَرَكَات وَالتَّنْوِين", "Tamareen Al-Harakat wat-Tanween",
     "Mixed drills that contrast the short vowels with their tanween counterparts.",
     ["harakat", "tanween", "exercise"], "🔀"),
    (7, "Standing Harakat", "الحَرَكَات القَائِمَة", "Al-Harakat Al-Qa'imah",
     "Standing fatha (dagger alif), standing kasra and inverted damma — superscript vowels read long.",
     ["harakat", "standing", "elongation"], "↕️"),
    (8, "Madd & Leen Letters", "حُرُوفُ المَدِّ وَاللِّين", "Huroof Al-Madd wal-Leen",
     "Alif, Waw and Yaa as elongation (madd) letters, and Waw/Yaa as soft (leen) letters.",
     ["madd", "leen", "elongation"], "➰"),
    (9, "Madd, Leen & Tanween Drills", "تَمَارِين المَدِّ وَاللِّين", "Tamareen Al-Madd wal-Leen",
     "Mixed drills combining madd, leen and tanween in short words.",
     ["madd", "leen", "tanween", "exercise"], "🔁"),
    (10, "Sukoon (Jazm)", "السُّكُون", "As-Sukoon",
     "Vowel-less (saakin) letters and how they close a syllable.",
     ["sukoon", "jazm"], "◌ْ"),
    (11, "Sukoon Drills", "تَمَارِين السُّكُون", "Tamareen As-Sukoon",
     "Drills reading saakin letters inside real words.",
     ["sukoon", "exercise"], "📝"),
    (12, "Shadda (Tashdeed)", "الشَّدَّة", "Ash-Shaddah",
     "Doubled letters marked with shadda — e.g. نَزَّلَ versus نَزَلَ.",
     ["shadda", "tashdeed"], "◌ّ"),
    (13, "Shadda Drills", "تَمَارِين الشَّدَّة", "Tamareen Ash-Shaddah",
     "Drills reading shadda inside real words.",
     ["shadda", "exercise"], "🪄"),
    (14, "Shadda with Sukoon", "الشَّدَّة مَعَ السُّكُون", "Ash-Shaddah ma'a As-Sukoon",
     "Words that combine a shadda and a sukoon.",
     ["shadda", "sukoon"], "🧩"),
    (15, "Words with Two Shaddas", "كَلِمَاتٌ بِشَدَّتَيْن", "Kalimat bi-Shaddatayn",
     "Words that carry two shaddas, building reading stamina.",
     ["shadda", "advanced"], "🧱"),
    (16, "Madd, Shadda & Sukoon Together", "المَدُّ وَالشَّدَّةُ وَالسُّكُون", "Al-Madd wash-Shaddah was-Sukoon",
     "Integrated words combining madd, shadda and sukoon, introducing tajweed timing.",
     ["madd", "shadda", "sukoon", "tajweed"], "🎯"),
    (17, "Comprehensive Revision", "المُرَاجَعَة الشَّامِلَة", "Al-Muraja'ah Ash-Shamilah",
     "Final practice reading real words and short surahs (Al-Fatihah and An-Nas).",
     ["revision", "words", "surah"], "🏆"),
]

# ──────────────────────────────────────────────────────────────────────
# Generation helpers
# ──────────────────────────────────────────────────────────────────────


class Builder:
    """Accumulates lines + cells and hands out unique ids and audio keys."""

    def __init__(self):
        self.lines = []
        self.cells = []
        self._line_id = 100      # first line -> 101
        self._cell_id = 1000     # first cell -> 1001
        self._used_keys = set()
        self._cur_lesson = None
        self._cur_line = None
        self._cur_pos = 0

    def lesson(self, lesson_id):
        self._cur_lesson = lesson_id

    def line(self, line_type, display_order,
             instruction_english=None, instruction_arabic=None):
        self._line_id += 1
        line_number = sum(1 for l in self.lines if l["lesson_id"] == self._cur_lesson) + 1
        line = {
            "id": self._line_id,
            "lesson_id": self._cur_lesson,
            "line_number": line_number,
            "line_type": line_type,
            "instruction_english": instruction_english,
            "instruction_arabic": instruction_arabic,
            "display_order": display_order,
        }
        self.lines.append(line)
        self._cur_line = self._line_id
        self._cur_pos = 0
        return self._line_id

    def _key(self, base):
        base = re.sub(r"[^a-z0-9_]", "", base.lower())
        key = base
        i = 2
        while key in self._used_keys:
            key = f"{base}_{i}"
            i += 1
        self._used_keys.add(key)
        return key

    def cell(self, text_arabic, transliteration, token_type, audio_base,
             highlight_group=None, letter_id=None, notes=None):
        self._cell_id += 1
        self._cur_pos += 1
        cell = {
            "id": self._cell_id,
            "line_id": self._cur_line,
            "lesson_id": self._cur_lesson,
            "position": self._cur_pos,
            "text_arabic": text_arabic,
            "transliteration": transliteration,
            "token_type": token_type,
            "audio_key": self._key(audio_base),
            "highlight_group": highlight_group,
            "letter_id": letter_id,
            "notes": notes,
        }
        self.cells.append(cell)
        return cell


def positional_forms(ar, connecting):
    """Derive the four positional forms from the joining model.

    The tatweel (U+0640) marks a connection point, so it visually stands in
    for the neighbouring glyph the letter would join to.
    """
    isolated = ar
    if ar == "ء":  # hamzah never joins in any direction
        return isolated, None, None, None
    if not connecting:  # joins only to a preceding letter -> final form only
        return isolated, None, None, TATWEEL + ar
    initial = ar + TATWEEL
    medial = TATWEEL + ar + TATWEEL
    final = TATWEEL + ar
    return isolated, initial, medial, final


def build_letters():
    out = []
    for i, (ar, name_ar, name_tr, slug, sound, area, detail, hint, connecting) in enumerate(LETTERS, start=1):
        iso, ini, med, fin = positional_forms(ar, connecting)
        out.append({
            "id": i,
            "letter_arabic": ar,
            "name_arabic": name_ar,
            "name_transliteration": name_tr,
            "isolated_form": iso,
            "initial_form": ini,
            "medial_form": med,
            "final_form": fin,
            "is_connecting": connecting,
            "makhraj_area": area,
            "makhraj_detail": detail,
            "phonetic_hint": hint,
            "audio_key": f"letter_{slug}",
            "display_order": i,
        })
    return out


def build_lessons():
    out = []
    for (num, en, ar, tr, desc, tags, icon) in LESSONS:
        out.append({
            "id": num,
            "lesson_number": num,
            "title_english": en,
            "title_arabic": ar,
            "title_transliteration": tr,
            "description": desc,
            "concept_tags": tags,
            "icon": icon,
            "display_order": num,
        })
    return out


# Convenience lookups keyed by letter index (1-based) and arabic glyph.
LBY_AR = {ar: (idx + 1, ar, name_ar, name_tr, slug, sound, area, detail, hint, conn)
          for idx, (ar, name_ar, name_tr, slug, sound, area, detail, hint, conn) in enumerate(LETTERS)}
LBY_ID = {idx + 1: tup for idx, tup in enumerate(
    [(ar, name_ar, name_tr, slug, sound, area, detail, hint, conn)
     for (ar, name_ar, name_tr, slug, sound, area, detail, hint, conn) in LETTERS])}


def lid(ar):
    return LBY_AR[ar][0]


def slug_of(ar):
    return LBY_AR[ar][4]


def sound_of(ar):
    return LBY_AR[ar][5]


def chunk(seq, n):
    for i in range(0, len(seq), n):
        yield seq[i:i + n]


# ──────────────────────────────────────────────────────────────────────
# Lesson content builders
# ──────────────────────────────────────────────────────────────────────
ALL_AR = [t[0] for t in LETTERS]
# A representative drill subset (covers the common Qaida practice letters).
DRILL = ["ب", "ت", "ث", "ج", "ح", "د", "ر", "س", "ش", "ع", "ف", "ق", "ك", "ل", "م", "ن"]


def lesson1(b):
    """Isolated letters, grouped into rows."""
    b.lesson(1)
    order = 0
    for row in chunk(ALL_AR, 6):
        order += 1
        b.line("PRACTICE", order)
        for ar in row:
            b.cell(ar, LBY_AR[ar][3], "LETTER", f"letter_{slug_of(ar)}",
                   highlight_group=None, letter_id=lid(ar))


def lesson2(b):
    """Joining: each connecting letter doubled shows initial+final shapes."""
    b.lesson(2)
    b.line("HEADING", 1, "Letters change shape when they join together.",
           "تَتَغَيَّرُ صُوَرُ الحُرُوفِ عِنْدَ الوَصْل")
    connecting = [ar for ar in ALL_AR if LBY_AR[ar][9] and ar != "ء"]
    order = 1
    for row in chunk(connecting, 6):
        order += 1
        b.line("EXAMPLE", order)
        for ar in row:
            s = slug_of(ar)
            b.cell(ar + ar, f"{sound_of(ar)}{sound_of(ar)}", "SYLLABLE",
                   f"l2_{s}{s}", highlight_group=None, letter_id=lid(ar))


# Muqatta'at that open surahs, with spelled-out names.
MUQATTAAT = [
    ("الٓمٓ", "alif-laam-meem"),
    ("الٓمٓصٓ", "alif-laam-meem-saad"),
    ("الٓرٰ", "alif-laam-raa"),
    ("الٓمٓرٰ", "alif-laam-meem-raa"),
    ("كٓهٰيٰعٓصٓ", "kaaf-haa-yaa-ayn-saad"),
    ("طٰهٰ", "taa-haa"),
    ("طٰسٓمٓ", "taa-seen-meem"),
    ("طٰسٓ", "taa-seen"),
    ("يٰسٓ", "yaa-seen"),
    ("صٓ", "saad"),
    ("حٰمٓ", "haa-meem"),
    ("عٓسٓقٓ", "ayn-seen-qaaf"),
    ("قٓ", "qaaf"),
    ("نٓ", "noon"),
]


def lesson3(b):
    b.lesson(3)
    b.line("HEADING", 1, "Disjoined letters are read by their names, with elongation.",
           "الحُرُوفُ المُقَطَّعَةُ تُقْرَأُ بِأَسْمَائِهَا مَعَ المَدّ")
    order = 1
    for row in chunk(MUQATTAAT, 4):
        order += 1
        b.line("PRACTICE", order)
        for ar, tr in row:
            b.cell(ar, tr, "MUQATTAAT", f"l3_{tr.replace('-', '_')}",
                   highlight_group="muqattaat")


HARAKAT = [(FATHA, "a", "fatha"), (KASRA, "i", "kasra"), (DAMMA, "u", "damma")]
TANWEEN = [(FATHATAYN, "an", "tanween_fath"),
           (KASRATAYN, "in", "tanween_kasr"),
           (DAMMATAYN, "un", "tanween_damm")]


def lesson4(b):
    """Every letter x {fatha, kasra, damma}, one line per letter."""
    b.lesson(4)
    order = 0
    for ar in ALL_AR:
        order += 1
        b.line("PRACTICE", order)
        s, snd = slug_of(ar), sound_of(ar)
        for mark, v, grp in HARAKAT:
            b.cell(ar + mark, f"{snd}{v}", "HARAKAH", f"l4_{s}_{v}",
                   highlight_group=grp, letter_id=lid(ar))


def lesson5(b):
    """Every letter x {fathatayn, kasratayn, dammatayn}."""
    b.lesson(5)
    order = 0
    for ar in ALL_AR:
        order += 1
        b.line("PRACTICE", order)
        s, snd = slug_of(ar), sound_of(ar)
        for mark, v, grp in TANWEEN:
            b.cell(ar + mark, f"{snd}{v}", "TANWEEN", f"l5_{s}_{v}",
                   highlight_group=grp, letter_id=lid(ar))


def lesson6(b):
    """Mixed harakat + tanween two-letter drills."""
    b.lesson(6)
    order = 0
    pairs = list(zip(DRILL, DRILL[1:] + DRILL[:1]))
    for row in chunk(pairs, 4):
        order += 1
        b.line("EXERCISE", order)
        for k, (a1, a2) in enumerate(row):
            (m1, v1, _), (m2, v2, _) = HARAKAT[k % 3], TANWEEN[(k + 1) % 3]
            text = a1 + m1 + a2 + m2
            tr = f"{sound_of(a1)}{v1}{sound_of(a2)}{v2}"
            b.cell(text, tr, "SYLLABLE", f"l6_{slug_of(a1)}{v1}_{slug_of(a2)}{v2}")


def lesson7(b):
    """Standing/superscript harakat + classic example words."""
    b.lesson(7)
    subset = ["ب", "ج", "د", "ر", "س", "ك"]
    standing = [(DAGGER_ALIF, "aa", "standing_fatha"),
                (SUBSCRIPT_ALEF, "ee", "standing_kasra"),
                (INVERTED_DAMMA, "oo", "inverted_damma")]
    order = 0
    for ar in subset:
        order += 1
        b.line("PRACTICE", order)
        s, snd = slug_of(ar), sound_of(ar)
        for mark, v, grp in standing:
            b.cell(ar + mark, f"{snd}{v}", "HARAKAH", f"l7_{s}_{v}",
                   highlight_group=grp, letter_id=lid(ar))
    order += 1
    b.line("EXAMPLE", order, "Real words that use the standing fatha (dagger alif).",
           "كَلِمَاتٌ فِيهَا أَلِفٌ خَنْجَرِيَّة")
    for ar_word, tr in [("هَٰذَا", "haadhaa"), ("ذَٰلِكَ", "dhaalika"),
                        ("الرَّحْمَٰن", "ar-rahmaan")]:
        b.cell(ar_word, tr, "WORD", f"l7_{tr}", highlight_group="standing_fatha")


def lesson8(b):
    """Madd letters (alif/waw/yaa) then leen (soft waw/yaa)."""
    b.lesson(8)
    subset = ["ب", "ت", "ج", "د", "س", "ف", "ك", "ل", "م", "ن"]
    order = 0
    for ar in subset:
        order += 1
        b.line("PRACTICE", order)
        s, snd = slug_of(ar), sound_of(ar)
        b.cell(ar + FATHA + ALIF, f"{snd}aa", "MADD", f"l8_{s}_aa",
               highlight_group="madd", letter_id=lid(ar))
        b.cell(ar + DAMMA + WAW, f"{snd}oo", "MADD", f"l8_{s}_oo",
               highlight_group="madd", letter_id=lid(ar))
        b.cell(ar + KASRA + YAA, f"{snd}ee", "MADD", f"l8_{s}_ee",
               highlight_group="madd", letter_id=lid(ar))
    order += 1
    b.line("EXAMPLE", order, "Soft (leen) letters: waw or yaa with sukoon after a fatha.",
           "حَرْفَا اللِّين: الوَاوُ وَاليَاءُ السَّاكِنَتَانِ بَعْدَ فَتْحَة")
    for ar in ["ب", "ت", "ج", "س", "ك", "م"]:
        s, snd = slug_of(ar), sound_of(ar)
        b.cell(ar + FATHA + WAW + SUKOON, f"{snd}aw", "LEEN", f"l8_{s}_aw",
               highlight_group="leen", letter_id=lid(ar))
        b.cell(ar + FATHA + YAA + SUKOON, f"{snd}ay", "LEEN", f"l8_{s}_ay",
               highlight_group="leen", letter_id=lid(ar))


def words_lesson(b, lesson_id, header_en, header_ar, groups, token_type="WORD",
                 highlight_group=None, line_type="PRACTICE"):
    """Generic builder: rows of pre-authored (arabic, translit) word tokens."""
    b.lesson(lesson_id)
    order = 0
    if header_en:
        order += 1
        b.line("HEADING", order, header_en, header_ar)
    for row in groups:
        order += 1
        b.line(line_type, order)
        for ar, tr in row:
            b.cell(ar, tr, token_type, f"l{lesson_id}_{tr}",
                   highlight_group=highlight_group)


def lesson9(b):
    words_lesson(
        b, 9,
        "Mixed words combining madd, leen and tanween.",
        "كَلِمَاتٌ تَجْمَعُ المَدَّ وَاللِّينَ وَالتَّنْوِين",
        [
            [("كِتَابٌ", "kitaabun"), ("نُورٌ", "noorun"), ("سَمِيعٌ", "samee'un")],
            [("قَوْلٌ", "qawlun"), ("بَيْتٌ", "baytun"), ("خَيْرٌ", "khayrun")],
            [("دُعَاءً", "du'aa'an"), ("هُدًى", "hudan"), ("مَاءً", "maa'an")],
        ])


def lesson10(b):
    """Sukoon: saakin pattern drills, then real short words."""
    b.lesson(10)
    order = 0
    for row in chunk(DRILL, 6):
        order += 1
        b.line("PRACTICE", order)
        for ar in row:
            s, snd = slug_of(ar), sound_of(ar)
            text = ALIF + FATHA + ar + SUKOON  # اَبْ pattern
            b.cell(text, f"a{snd}", "SUKOON", f"l10_a{s}",
                   highlight_group="sukoon", letter_id=lid(ar))
    order += 1
    b.line("EXAMPLE", order, "Real words with a saakin (vowel-less) letter.",
           "كَلِمَاتٌ فِيهَا حَرْفٌ سَاكِن")
    for ar_word, tr in [("مِنْ", "min"), ("قُلْ", "qul"), ("هَلْ", "hal"),
                        ("كَمْ", "kam"), ("أَنْتَ", "anta")]:
        b.cell(ar_word, tr, "WORD", f"l10_{tr}", highlight_group="sukoon")


def lesson11(b):
    words_lesson(
        b, 11,
        "Read these words, watching the saakin letters carefully.",
        "اِقْرَأْ هٰذِهِ الكَلِمَاتِ مُرَاعِيًا الحُرُوفَ السَّاكِنَة",
        [
            [("اِقْرَأْ", "iqra'"), ("اُدْخُلْ", "udkhul"), ("اِجْلِسْ", "ijlis")],
            [("يَوْمْ", "yawm"), ("أَكْبَرْ", "akbar"), ("اِسْمْ", "ism")],
            [("صَبْرْ", "sabr"), ("فَجْرْ", "fajr"), ("عَصْرْ", "asr")],
        ],
        highlight_group="sukoon", line_type="EXERCISE")


def lesson12(b):
    """Shadda: the classic نَزَّلَ vs نَزَلَ contrast, then doubled-letter words."""
    b.lesson(12)
    b.line("EXAMPLE", 1, "A shadda doubles a letter — compare these two words.",
           "الشَّدَّةُ تُضَعِّفُ الحَرْف — قَارِنْ بَيْنَ الكَلِمَتَيْن")
    b.cell("نَزَلَ", "nazala", "WORD", "l12_nazala", highlight_group=None,
           notes="No shadda: 'he came down' (single zaay).")
    b.cell("نَزَّلَ", "nazzala", "WORD", "l12_nazzala", highlight_group="shadda",
           notes="With shadda: 'he sent down' (doubled zaay).")
    order = 1
    for row in chunk([("رَبَّ", "rabba"), ("مَدَّ", "madda"), ("حَجَّ", "hajja"),
                      ("أُمَّ", "umma"), ("شَدَّ", "shadda"), ("سَرَّ", "sarra")], 3):
        order += 1
        b.line("PRACTICE", order)
        for ar_word, tr in row:
            b.cell(ar_word, tr, "SHADDA", f"l12_{tr}", highlight_group="shadda")


def lesson13(b):
    words_lesson(
        b, 13,
        "Read these words, giving each shadda its full doubling.",
        "اِقْرَأْ هٰذِهِ الكَلِمَاتِ مُشَدِّدًا الحُرُوف",
        [
            [("اللَّهُ", "allaahu"), ("الَّذِي", "alladhee"), ("رَبِّ", "rabbi")],
            [("إِيَّاكَ", "iyyaaka"), ("حَقًّا", "haqqan"), ("مُحَمَّدٌ", "muhammadun")],
        ],
        token_type="SHADDA", highlight_group="shadda", line_type="EXERCISE")


def lesson14(b):
    words_lesson(
        b, 14,
        "Words that combine a shadda and a sukoon.",
        "كَلِمَاتٌ تَجْمَعُ الشَّدَّةَ وَالسُّكُون",
        [
            [("الْحَقُّ", "al-haqqu"), ("الْجَنَّةْ", "al-jannah"), ("رَبَّكْ", "rabbak")],
            [("أَشَدّْ", "ashadd"), ("الْبِرّْ", "al-birr"), ("مُسْتَقِرّْ", "mustaqirr")],
        ],
        highlight_group="shadda", line_type="EXERCISE")


def lesson15(b):
    words_lesson(
        b, 15,
        "Words that carry two shaddas.",
        "كَلِمَاتٌ فِيهَا شَدَّتَان",
        [
            [("الضَّالِّينَ", "ad-daalleen"), ("دُرِّيٌّ", "durriyyun")],
            [("حُيِّيتُمْ", "huyyeetum"), ("مُّزَّمِّلُ", "muzzammil")],
        ],
        token_type="WORD", highlight_group="shadda", line_type="EXERCISE")


def lesson16(b):
    words_lesson(
        b, 16,
        "Integrated words: madd, shadda and sukoon together (mind the timing).",
        "كَلِمَاتٌ مُتَكَامِلَة: المَدُّ وَالشَّدَّةُ وَالسُّكُونُ مَعًا",
        [
            [("دَآبَّةٍ", "daabbatin"), ("جَآنٌّ", "jaannun")],
            [("الضَّآلِّينَ", "ad-daaalleen"), ("الطَّآمَّةُ", "at-taaammatu")],
        ],
        token_type="WORD", highlight_group="madd", line_type="EXERCISE")


def strip_bom(s):
    return s.lstrip("﻿￾").strip()


def transliterate_token(tok):
    """Very light transliteration fallback for revision word tokens."""
    plain = "".join(c for c in unicodedata.normalize("NFKD", tok)
                    if unicodedata.category(c) != "Mn")
    table = {a: LBY_AR[a][5] for a in LBY_AR}
    table.update({"آ": "aa", "أ": "'", "إ": "'", "ئ": "'",
                  "ؤ": "'", "ة": "h", "ى": "a", "ٱ": "a"})
    return "".join(table.get(c, "") for c in plain) or "word"


def lesson17(b):
    """Comprehensive revision: real ayahs pulled from ayahs.json."""
    b.lesson(17)
    ayahs = json.loads((JSON_DIR / "ayahs.json").read_text(encoding="utf-8"))
    selections = [("Al-Fatihah", 1), ("An-Nas", 114)]
    order = 0
    for name, surah_id in selections:
        rows = sorted((a for a in ayahs if a["surah_id"] == surah_id),
                      key=lambda a: a["number_in_surah"])
        order += 1
        b.line("HEADING", order, f"Surah {name}", None)
        for a in rows:
            order += 1
            b.line("PRACTICE", order)
            tokens = [t for t in strip_bom(a["text_arabic"]).split(" ") if t]
            for w, tok in enumerate(tokens, start=1):
                tr = transliterate_token(tok)
                b.cell(tok, tr, "WORD",
                       f"l17_s{surah_id}_a{a['number_in_surah']}_w{w}",
                       highlight_group="word")


BUILDERS = [lesson1, lesson2, lesson3, lesson4, lesson5, lesson6, lesson7,
            lesson8, lesson9, lesson10, lesson11, lesson12, lesson13,
            lesson14, lesson15, lesson16, lesson17]


# ──────────────────────────────────────────────────────────────────────
# Validation
# ──────────────────────────────────────────────────────────────────────
def validate(letters, lessons, lines, cells):
    errors = []

    def seq(rows, label):
        ids = [r["id"] for r in rows]
        if len(ids) != len(set(ids)):
            errors.append(f"{label}: duplicate ids")
        if ids != sorted(ids):
            errors.append(f"{label}: ids not sorted/sequential")

    seq(letters, "letters")
    seq(lessons, "lessons")
    seq(lines, "lines")
    seq(cells, "cells")

    if len(letters) != 29:
        errors.append(f"letters: expected 29, got {len(letters)}")
    if len(lessons) != 17:
        errors.append(f"lessons: expected 17, got {len(lessons)}")

    letter_ids = {l["id"] for l in letters}
    lesson_ids = {l["id"] for l in lessons}
    line_ids = {l["id"] for l in lines}

    for L in lessons:
        if not any(ln["lesson_id"] == L["id"] for ln in lines):
            errors.append(f"lesson {L['id']} has no lines")
    for ln in lines:
        if ln["lesson_id"] not in lesson_ids:
            errors.append(f"line {ln['id']} -> missing lesson {ln['lesson_id']}")
        # HEADING lines are labels; all other line types must carry cells.
        if ln["line_type"] != "HEADING" and not any(c["line_id"] == ln["id"] for c in cells):
            errors.append(f"line {ln['id']} ({ln['line_type']}) has no cells")

    for c in cells:
        if c["line_id"] not in line_ids:
            errors.append(f"cell {c['id']} -> missing line {c['line_id']}")
        if c["lesson_id"] not in lesson_ids:
            errors.append(f"cell {c['id']} -> missing lesson {c['lesson_id']}")
        if c["letter_id"] is not None and c["letter_id"] not in letter_ids:
            errors.append(f"cell {c['id']} -> missing letter {c['letter_id']}")
        if not c["audio_key"]:
            errors.append(f"cell {c['id']} has empty audio_key")

    keys = [c["audio_key"] for c in cells]
    if len(keys) != len(set(keys)):
        errors.append("cells: duplicate audio_key values")

    # 6 non-connectors must have null initial/medial forms.
    for l in letters:
        if l["letter_arabic"] in NON_CONNECTING:
            if l["initial_form"] is not None or l["medial_form"] is not None:
                errors.append(f"letter {l['id']} non-connector should have null forms")
            if l["is_connecting"]:
                errors.append(f"letter {l['id']} non-connector marked connecting")
        if l["makhraj_area"] not in {"JAWF", "HALQ", "LISAN", "SHAFATAIN", "KHAYSHUM"}:
            errors.append(f"letter {l['id']} invalid makhraj_area")

    return errors


def write_json(name, data):
    path = JSON_DIR / name
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n",
                    encoding="utf-8")
    return path


def main():
    letters = build_letters()
    lessons = build_lessons()

    b = Builder()
    for fn in BUILDERS:
        fn(b)
    lines, cells = b.lines, b.cells

    errors = validate(letters, lessons, lines, cells)
    if errors:
        print("VALIDATION FAILED:")
        for e in errors:
            print("  -", e)
        raise SystemExit(1)

    write_json("qaida_letters.json", letters)
    write_json("qaida_lessons.json", lessons)
    write_json("qaida_lines.json", lines)
    write_json("qaida_cells.json", cells)

    print("Qaida data generated successfully:")
    print(f"  letters : {len(letters)}")
    print(f"  lessons : {len(lessons)}")
    print(f"  lines   : {len(lines)}")
    print(f"  cells   : {len(cells)}")
    per = {L['id']: sum(1 for c in cells if c['lesson_id'] == L['id']) for L in lessons}
    print("  cells per lesson:", per)


if __name__ == "__main__":
    main()
