#!/usr/bin/env python3
"""
Nimaz Pro - Full Hisnul Muslim Importer
=======================================

Imports the complete *Hisnul Muslim* (Fortress of the Muslim) by Sa'id bin
Ali al-Qahtani — 132 chapters / 267 supplications — into the app's existing
dua schema, WITHOUT any database schema change.

Source: nimaz-pro-data/sources/hisnulmuslim_en.json (the canonical
hisnmuslim.com export). Al-Qahtani restricted his compilation to authentic
(sahih / hasan) supplications, removing weak narrations.

Pipeline position: runs AFTER expand_duas.py. The curated, individually
referenced entries (the original 64 + expand_duas.py additions) are the
quality core; this importer only APPENDS Hisnul Muslim supplications that
are not already present (deduplicated by diacritic-stripped Arabic), so we
get full coverage without losing the polished, source-graded entries.

For imported entries:
  * Arabic is cleaned of wrapper/ornament markers.
  * Translation is taken from the source and lightly cleaned.
  * Transliteration is generated rule-based from the voweled Arabic
    (the source's own transliteration is too rough to ship).
  * `source` records the Hisnul Muslim chapter for provenance.
  * `repeat_count` comes from the source REPEAT field.

The 132 chapters are mapped onto a clean category taxonomy (existing
categories are reused; a small number of new ones are created).
"""

import json
import re
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JSON_DIR = ROOT / "json"
SOURCE = ROOT / "sources" / "hisnulmuslim_en.json"
DUAS_PATH = JSON_DIR / "duas.json"
CATEGORIES_PATH = JSON_DIR / "dua_categories.json"


# ---------------------------------------------------------------------------
# Arabic helpers
# ---------------------------------------------------------------------------
ARABIC_DIACRITICS = "".join(
    chr(c) for c in list(range(0x064B, 0x0653)) + [0x0670, 0x0640]
)
_DIAC_RE = re.compile("[" + ARABIC_DIACRITICS + "]")
_ARABIC_LETTER_RE = re.compile(r"[ء-ي]")


def strip_diacritics(text: str) -> str:
    return _DIAC_RE.sub("", text)


def normalize_for_dedup(text: str) -> str:
    """Diacritic-free, letters-only signature for duplicate detection."""
    t = strip_diacritics(text)
    # normalize alef/ya/hamza variants
    t = (t.replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").replace("ٱ", "ا")
           .replace("ى", "ي").replace("ة", "ه")
           .replace("ؤ", "و").replace("ئ", "ي").replace("ء", ""))
    letters = "".join(_ARABIC_LETTER_RE.findall(t))
    return letters


def repair_arabic(text: str) -> str:
    """Fix common spurious mid-word breaks present in the source export."""
    # "اللَّهُ مَّ" / "اللّهُ مَّ" -> "اللَّهُمَّ" (Allahumma split by a stray space)
    text = re.sub(r"هُ\s+مَّ", "هُمَّ", text)
    text = re.sub(r"هُ\s+مّ", "هُمّ", text)
    return text


_NARRATION_AR = ("صلى الله عليه وسلم", "رضي الله عن", "قال رسول", "عن النبي",
                 "عن رسول", "قال النبي", "حدثنا", "وقال ", "أن النبي", "أن رسول")


def is_narration(clean_ar: str, raw_translation: str) -> bool:
    """True for hadith-narration / virtue entries that aren't pure supplications."""
    nd = strip_diacritics(clean_ar)
    if any(m in nd for m in _NARRATION_AR):
        return True
    low = raw_translation.lower()
    if "narrated" in low or "reported that" in low or "AA" in raw_translation:
        return True
    return False


def count_broken_tokens(clean_ar: str) -> int:
    """Number of lone single-letter Arabic tokens (a fragmentation signal)."""
    toks = [t for t in clean_ar.replace("،", " ").split() if _ARABIC_LETTER_RE.search(t)]
    return sum(1 for t in toks if len(strip_diacritics(t)) == 1
               and strip_diacritics(t) != "و")


def clean_arabic(text: str) -> str:
    """Remove wrapper/ornament markers, keep Arabic letters/harakat + commas."""
    text = repair_arabic(text)
    # Drop Quran ornament brackets and grouping/quote characters.
    text = re.sub(r"[()\[\]{}«»\"'*ـ﴾﴿•◦↑\d]", " ", text)
    # Keep Arabic range, spaces and Arabic comma/semicolon/period.
    kept = []
    for ch in text:
        if "؀" <= ch <= "ۿ" or ch in " ،؛":
            kept.append(ch)
    out = "".join(kept)
    out = re.sub(r"\s+", " ", out).strip()
    out = re.sub(r"\s+،", "،", out)
    return out


def clean_translation(text: str) -> str:
    text = text.replace("\r", " ").replace("\n", " ")
    text = re.sub(r"\(\s*\d+\s*\)", " ", text)   # verse markers ( 2 )
    text = re.sub(r"\s+", " ", text).strip()
    # Strip a single wrapping pair of parentheses and surrounding quotes.
    if text.startswith("(") and text.endswith(")") and text.count("(") == 1:
        text = text[1:-1].strip()
    text = text.strip("‘’“”\"'").strip()
    return text


# ---------------------------------------------------------------------------
# Rule-based transliteration (voweled Arabic -> readable Latin)
# ---------------------------------------------------------------------------
_CONS = {
    "ب": "b", "ت": "t", "ث": "th", "ج": "j", "ح": "h", "خ": "kh", "د": "d",
    "ذ": "dh", "ر": "r", "ز": "z", "س": "s", "ش": "sh", "ص": "s", "ض": "d",
    "ط": "t", "ظ": "dh", "ع": "'", "غ": "gh", "ف": "f", "ق": "q", "ك": "k",
    "ل": "l", "م": "m", "ن": "n", "ه": "h", "ة": "h",
    "أ": "'", "إ": "'", "ء": "'", "ؤ": "'", "ئ": "'",
}
_SHORT = {"َ": "a", "ِ": "i", "ُ": "u"}
_TANWIN = {"ً": "an", "ٍ": "in", "ٌ": "un"}
_LONG = {"ا": "a", "آ": "a", "ى": "a", "و": "w", "ي": "y", "ٰ": "a"}
_SHADDA = "ّ"
_SUKUN = "ْ"
_HARAKA = set(_SHORT) | set(_TANWIN) | {_SHADDA, _SUKUN, "ٓ"}


def transliterate(arabic: str) -> str:
    words = arabic.split(" ")
    result = []
    for w in words:
        if not _ARABIC_LETTER_RE.search(w):
            continue
        out = []
        last_cons = None  # index in `out` of the last consonant (for shadda)
        i, n = 0, len(w)
        while i < n:
            ch = w[i]
            nxt = w[i + 1] if i + 1 < n else ""
            if ch in _CONS:
                out.append(_CONS[ch]); last_cons = len(out) - 1
            elif ch in _LONG:
                prev = out[-1] if out else ""
                lengthen = (
                    (ch in ("ا", "آ", "ى", "ٰ") and prev == "a")
                    or (ch == "و" and prev == "u")
                    or (ch == "ي" and prev == "i")
                ) and (nxt not in _SHORT and nxt != _SHADDA)
                if lengthen:
                    pass  # elongation already represented by the short vowel
                elif ch in ("ا", "آ", "ى", "ٰ"):
                    out.append("a"); last_cons = None
                else:
                    out.append("w" if ch == "و" else "y"); last_cons = len(out) - 1
            elif ch in _SHORT:
                out.append(_SHORT[ch])
            elif ch in _TANWIN:
                out.append(_TANWIN[ch])
            elif ch == _SHADDA:
                if last_cons is not None:
                    out[last_cons] = out[last_cons] * 2
            # sukun / madda / anything else -> ignore
            i += 1
        word = "".join(out)
        word = re.sub(r"(.)\1{2,}", r"\1\1", word)       # 3+ same char -> 2
        word = re.sub(r"([aiu])\1+", r"\1", word)         # long vowel -> single
        word = re.sub(r"^([bcdfghjklmnpqrstvwxyz])\1", r"\1", word)  # leading double
        # Definite article assimilation before sun letters (al-rr.. -> arr..).
        word = re.sub(r"^al(tt|thth|dd|dhdh|rr|zz|ss|shsh|nn|ll)", r"a\1", word)
        word = word.strip("'")
        if word:
            result.append(word)
    text = " ".join(result)
    # Common fixups for readability/consistency.
    text = text.replace("allh", "allah").replace("biallah", "billah")
    return text


# ---------------------------------------------------------------------------
# Chapter -> category taxonomy
# ---------------------------------------------------------------------------
NEW_CATEGORIES = [
    ("During Prayer (Salah)", "في الصلاة", "🧎"),
    ("Witr & Night Prayer", "الوتر وقيام الليل", "🌃"),
    ("Funeral & Death", "الجنازة والموت", "🪦"),
    ("Fasting & Ramadan", "الصيام ورمضان", "🌛"),
    ("Enemy & Fear", "لقاء العدو والخوف", "⚔️"),
    ("Nature & Weather", "الطبيعة والطقس", "🌬️"),
    ("Gratitude & Praise", "الحمد والشكر", "🤍"),
    ("Dhikr & Remembrance", "الذكر والتسبيح", "📿"),
    ("Social & Daily Life", "آداب وحياة يومية", "🤝"),
]

# Map each source chapter index (0-131) to a target category name.
CHAPTER_CATEGORY = {
    0: "Morning Adhkar", 1: "Before Sleep", 2: "Waking Up", 3: "Toilet",
    4: "Toilet", 5: "Ablution (Wudu)", 6: "Ablution (Wudu)", 7: "Leaving Home",
    8: "Entering Home", 9: "Entering Mosque", 10: "Entering Mosque",
    11: "Leaving Mosque", 12: "Adhan", 13: "Clothing", 14: "Clothing",
    15: "Clothing", 16: "Clothing", 17: "During Prayer (Salah)",
    18: "During Prayer (Salah)", 19: "During Prayer (Salah)",
    20: "During Prayer (Salah)", 21: "During Prayer (Salah)",
    22: "During Prayer (Salah)", 23: "During Prayer (Salah)",
    24: "During Prayer (Salah)", 25: "During Prayer (Salah)", 26: "After Prayer",
    27: "Istikharah", 28: "Before Sleep", 29: "Before Sleep", 30: "Before Sleep",
    31: "Witr & Night Prayer", 32: "Witr & Night Prayer", 33: "Distress & Anxiety",
    34: "Distress & Anxiety", 35: "Enemy & Fear", 36: "Enemy & Fear",
    37: "Enemy & Fear", 38: "Enemy & Fear", 39: "Protection & Refuge",
    40: "Debt", 41: "During Prayer (Salah)", 42: "Hardship & Calamity",
    43: "Forgiveness", 44: "Protection & Refuge", 45: "Hardship & Calamity",
    46: "Marriage & Children", 47: "Marriage & Children", 48: "Sickness & Healing",
    49: "Sickness & Healing", 50: "Sickness & Healing", 51: "Funeral & Death",
    52: "Hardship & Calamity", 53: "Funeral & Death", 54: "Funeral & Death",
    55: "Funeral & Death", 56: "Funeral & Death", 57: "Funeral & Death",
    58: "Funeral & Death", 59: "Funeral & Death", 60: "Nature & Weather",
    61: "Rain", 62: "Rain", 63: "Rain", 64: "Rain", 65: "Rain",
    66: "Nature & Weather", 67: "Fasting & Ramadan", 68: "Before Eating",
    69: "After Eating", 70: "After Eating", 71: "After Eating",
    72: "Fasting & Ramadan", 73: "Fasting & Ramadan", 74: "Fasting & Ramadan",
    75: "Nature & Weather", 76: "Sneezing", 77: "Sneezing",
    78: "Marriage & Children", 79: "Marriage & Children", 80: "Marriage & Children",
    81: "Anger", 82: "Gratitude & Praise", 83: "Dhikr & Remembrance",
    84: "Forgiveness", 85: "Social & Daily Life", 86: "Social & Daily Life",
    87: "Protection & Refuge", 88: "Social & Daily Life", 89: "Social & Daily Life",
    90: "Debt", 91: "Protection & Refuge", 92: "Social & Daily Life",
    93: "Protection & Refuge", 94: "Traveling", 95: "Traveling", 96: "Traveling",
    97: "Social & Daily Life", 98: "Traveling", 99: "Traveling", 100: "Traveling",
    101: "Traveling", 102: "Traveling", 103: "Traveling", 104: "Traveling",
    105: "Gratitude & Praise", 106: "Salawat on the Prophet ﷺ",
    107: "Social & Daily Life", 108: "Social & Daily Life", 109: "Protection & Refuge",
    110: "Protection & Refuge", 111: "Social & Daily Life", 112: "Social & Daily Life",
    113: "Social & Daily Life", 114: "Hajj & Umrah", 115: "Hajj & Umrah",
    116: "Hajj & Umrah", 117: "Hajj & Umrah", 118: "Hajj & Umrah",
    119: "Hajj & Umrah", 120: "Hajj & Umrah", 121: "Gratitude & Praise",
    122: "Gratitude & Praise", 123: "Sickness & Healing", 124: "Protection & Refuge",
    125: "Distress & Anxiety", 126: "Social & Daily Life", 127: "Protection & Refuge",
    128: "Forgiveness", 129: "Dhikr & Remembrance", 130: "Dhikr & Remembrance",
    131: "Social & Daily Life",
}

_TITLE_PREFIXES = [
    "What to say and do if you ", "What to say upon ", "What to say when ",
    "What to say if ", "What to say while ", "What to say to ", "What to say ",
    "What to do if ", "What to encourage ", "Invocations for when ",
    "Invocations for ", "Invocation for when ", "Invocation for ",
    "Invocations against ", "Invocation against ", "Invocations during ",
    "Invocations in ", "Invocations of ", "Invocation to be recited ",
    "Invocation upon ", "Invocation when ", "Invocation ", "Supplications for ",
    "Supplication to be recited ", "Supplication ", "Some invocations for ",
    "How to recite ", "How to ", "How the ", "How a ", "The excellence of ",
    "The reward for ", "Types of ", "spreading ", "Glorifying and magnifying ",
]


def short_title(chapter_title: str) -> str:
    t = chapter_title.strip().lstrip(" ")
    for p in _TITLE_PREFIXES:
        if t.lower().startswith(p.lower()):
            t = t[len(p):]
            break
    t = t.strip(" .")
    if not t:
        t = chapter_title.strip()
    return t[0].upper() + t[1:] if t else chapter_title


def arabic_title(clean_ar: str) -> str:
    words = clean_ar.replace("،", " ").split()
    return " ".join(words[:5]) if words else clean_ar


def main():
    src = json.loads(SOURCE.read_text(encoding="utf-8-sig"))["English"]
    duas = json.loads(DUAS_PATH.read_text(encoding="utf-8"))
    categories = json.loads(CATEGORIES_PATH.read_text(encoding="utf-8"))

    cat_by_name = {c["name_english"]: c for c in categories}
    next_cat_id = max(c["id"] for c in categories) + 1
    next_cat_order = max(c["display_order"] for c in categories) + 1
    next_dua_id = max(d["id"] for d in duas) + 1

    # Create any new categories referenced by the mapping (preserve order).
    for name_en, name_ar, icon in NEW_CATEGORIES:
        if name_en not in cat_by_name:
            cat = {
                "id": next_cat_id, "name_english": name_en, "name_arabic": name_ar,
                "icon": icon, "display_order": next_cat_order, "dua_count": 0,
            }
            categories.append(cat); cat_by_name[name_en] = cat
            next_cat_id += 1; next_cat_order += 1

    # Existing Arabic signatures (curated + already-imported) for dedup.
    seen = {normalize_for_dedup(d["text_arabic"]) for d in duas}
    max_order = {}
    for d in duas:
        max_order[d["category_id"]] = max(max_order.get(d["category_id"], 0),
                                          d["display_order"])

    added = skipped_dup = skipped_empty = 0
    for idx, chapter in enumerate(src):
        cat_name = CHAPTER_CATEGORY.get(idx)
        if cat_name is None or cat_name not in cat_by_name:
            raise SystemExit(f"Unmapped chapter {idx}: {chapter['TITLE']!r}")
        cid = cat_by_name[cat_name]["id"]
        base_title = short_title(chapter["TITLE"])
        entries = chapter.get("TEXT", [])
        local_n = 0
        for entry in entries:
            ar = clean_arabic(entry.get("ARABIC_TEXT", ""))
            raw_tr = entry.get("TRANSLATED_TEXT", "")
            translation = clean_translation(raw_tr)
            sig = normalize_for_dedup(ar)
            if (len(sig) < 4 or count_broken_tokens(ar) >= 3
                    or not translation or is_narration(ar, raw_tr)):
                skipped_empty += 1
                continue
            if sig in seen:
                skipped_dup += 1
                continue
            seen.add(sig)
            local_n += 1
            translit = transliterate(ar)
            try:
                repeat = int(str(entry.get("REPEAT", "1")).strip() or "1")
            except ValueError:
                repeat = 1
            multi = sum(
                1 for e in entries
                if len(normalize_for_dedup(clean_arabic(e.get("ARABIC_TEXT", "")))) >= 4
            ) > 1
            title = f"{base_title} ({local_n})" if multi else base_title
            order = max_order.get(cid, 0) + 1
            max_order[cid] = order
            duas.append({
                "id": next_dua_id,
                "category_id": cid,
                "title_english": title[:120],
                "title_arabic": arabic_title(ar),
                "text_arabic": ar,
                "transliteration": translit,
                "translation": translation,
                "source": f"Hisnul Muslim — {chapter['TITLE'].strip()}",
                "virtue": None,
                "repeat_count": repeat if repeat > 0 else 1,
                "audio_file": None,
                "display_order": order,
            })
            next_dua_id += 1
            added += 1

    # Recompute dua_count for every category.
    counts = {}
    for d in duas:
        counts[d["category_id"]] = counts.get(d["category_id"], 0) + 1
    for c in categories:
        c["dua_count"] = counts.get(c["id"], 0)

    DUAS_PATH.write_text(json.dumps(duas, ensure_ascii=False, indent=2) + "\n",
                         encoding="utf-8")
    CATEGORIES_PATH.write_text(
        json.dumps(categories, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8")

    print(f"Imported {added} new duas "
          f"(skipped {skipped_dup} duplicates, {skipped_empty} empty).")
    print(f"Totals: {len(duas)} duas across {len(categories)} categories.")


if __name__ == "__main__":
    main()
