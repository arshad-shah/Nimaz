#!/usr/bin/env python3
"""
Nimaz Pro - Complete Data Download and Generation Script
Downloads Quran, Hadith, Duas data and generates SQLite database
"""

import json
import os
import sqlite3
import urllib.request
import urllib.error
import ssl
from pathlib import Path
from typing import Dict, List, Any

# Disable SSL verification for GitHub raw content (if needed)
ssl._create_default_https_context = ssl._create_unverified_context

BASE_DIR = Path(__file__).parent.parent
JSON_DIR = BASE_DIR / "json"
OUTPUT_DIR = BASE_DIR / "output"
JSON_DIR.mkdir(exist_ok=True)
OUTPUT_DIR.mkdir(exist_ok=True)

# Revelation order mapping (standard Islamic scholarship)
REVELATION_ORDER = {
    1: 5, 2: 87, 3: 89, 4: 92, 5: 112, 6: 55, 7: 39, 8: 88, 9: 113, 10: 51,
    11: 52, 12: 53, 13: 96, 14: 72, 15: 54, 16: 70, 17: 50, 18: 69, 19: 44, 20: 45,
    21: 73, 22: 103, 23: 74, 24: 102, 25: 42, 26: 47, 27: 48, 28: 49, 29: 85, 30: 84,
    31: 57, 32: 75, 33: 90, 34: 58, 35: 43, 36: 41, 37: 56, 38: 38, 39: 59, 40: 60,
    41: 61, 42: 62, 43: 63, 44: 64, 45: 65, 46: 66, 47: 95, 48: 111, 49: 106, 50: 34,
    51: 67, 52: 76, 53: 23, 54: 37, 55: 97, 56: 46, 57: 94, 58: 105, 59: 101, 60: 91,
    61: 109, 62: 110, 63: 104, 64: 108, 65: 99, 66: 107, 67: 77, 68: 2, 69: 78, 70: 79,
    71: 71, 72: 40, 73: 3, 74: 4, 75: 31, 76: 98, 77: 33, 78: 80, 79: 81, 80: 24,
    81: 7, 82: 82, 83: 86, 84: 83, 85: 27, 86: 36, 87: 8, 88: 68, 89: 10, 90: 35,
    91: 26, 92: 9, 93: 11, 94: 12, 95: 28, 96: 1, 97: 25, 98: 100, 99: 93, 100: 14,
    101: 30, 102: 16, 103: 13, 104: 32, 105: 19, 106: 29, 107: 17, 108: 15, 109: 18, 110: 114,
    111: 6, 112: 22, 113: 20, 114: 21
}

# Sajda verses (surah:ayah)
SAJDA_VERSES = {
    (7, 206): "obligatory", (13, 15): "recommended", (16, 50): "recommended",
    (17, 109): "recommended", (19, 58): "recommended", (22, 18): "recommended",
    (22, 77): "recommended", (25, 60): "recommended", (27, 26): "recommended",
    (32, 15): "obligatory", (38, 24): "recommended", (41, 38): "recommended",
    (53, 62): "obligatory", (84, 21): "obligatory", (96, 19): "obligatory"
}

def download_json(url: str) -> Any:
    """Download and parse JSON from URL"""
    print(f"  Downloading: {url}")
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req, timeout=60) as response:
            return json.loads(response.read().decode('utf-8'))
    except Exception as e:
        print(f"  Error downloading {url}: {e}")
        return None

def save_json(data: Any, filename: str):
    """Save data to JSON file"""
    filepath = JSON_DIR / filename
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"  Saved: {filename}")

def download_quran_data():
    """Download Quran surahs, ayahs, and translations"""
    print("\n" + "="*60)
    print("DOWNLOADING QURAN DATA")
    print("="*60)

    # Download surah metadata
    print("\n[1/3] Downloading surah metadata...")
    surah_data = download_json("https://raw.githubusercontent.com/semarketir/quranjson/master/source/surah.json")

    # Download Sahih International translation (has ayah text + metadata)
    print("\n[2/3] Downloading Arabic text and translation...")
    arabic_data = download_json("https://api.alquran.cloud/v1/quran/ar.alafasy")
    translation_data = download_json("https://api.alquran.cloud/v1/quran/en.sahih")

    if not surah_data or not translation_data:
        print("ERROR: Failed to download Quran data")
        return False

    # Process surahs
    print("\n[3/3] Processing data...")
    surahs = []
    for s in surah_data:
        surah_num = int(s['index'])
        surahs.append({
            "id": surah_num,
            "number": surah_num,
            "name_arabic": s['titleAr'],
            "name_english": s['title'].replace("'", "'"),
            "name_transliteration": s['title'],
            "revelation_type": "MECCAN" if s['place'] == "Mecca" else "MEDINAN",
            "verses_count": s['count'],
            "order_revealed": REVELATION_ORDER.get(surah_num, surah_num),
            "start_page": int(s['pages'])
        })
    save_json(surahs, "surahs.json")

    # Process ayahs
    ayahs = []
    translations = []
    ayah_id = 1

    arabic_surahs = arabic_data['data']['surahs'] if arabic_data else []
    trans_surahs = translation_data['data']['surahs']

    for surah_idx, trans_surah in enumerate(trans_surahs):
        surah_num = trans_surah['number']
        arabic_surah = arabic_surahs[surah_idx] if surah_idx < len(arabic_surahs) else None

        for verse_idx, verse in enumerate(trans_surah['ayahs']):
            arabic_text = arabic_surah['ayahs'][verse_idx]['text'] if arabic_surah else ""

            sajda_key = (surah_num, verse['numberInSurah'])
            is_sajda = sajda_key in SAJDA_VERSES
            sajda_type = SAJDA_VERSES.get(sajda_key)

            ayahs.append({
                "id": ayah_id,
                "surah_id": surah_num,
                "number_in_surah": verse['numberInSurah'],
                "number_global": verse['number'],
                "text_arabic": arabic_text,
                "text_uthmani": arabic_text,
                "juz": verse['juz'],
                "hizb": verse['hizbQuarter'],
                "page": verse['page'],
                "sajda": is_sajda,
                "sajda_type": sajda_type
            })

            translations.append({
                "id": ayah_id,
                "ayah_id": ayah_id,
                "translator_id": "sahih_international",
                "text": verse['text']
            })

            ayah_id += 1

    save_json(ayahs, "ayahs.json")
    save_json(translations, "translations.json")

    print(f"\n  Total: {len(surahs)} surahs, {len(ayahs)} ayahs, {len(translations)} translations")
    return True

def generate_hadith_books():
    """Generate hadith book metadata"""
    print("\n" + "="*60)
    print("GENERATING HADITH BOOKS METADATA")
    print("="*60)

    books = [
        {
            "id": 1,
            "name_english": "Sahih al-Bukhari",
            "name_arabic": "صحيح البخاري",
            "author": "Imam Muhammad al-Bukhari",
            "hadith_count": 7563,
            "description": "The most authentic collection of Hadith, compiled by Imam Bukhari.",
            "icon": "📗"
        },
        {
            "id": 2,
            "name_english": "Sahih Muslim",
            "name_arabic": "صحيح مسلم",
            "author": "Imam Muslim ibn al-Hajjaj",
            "hadith_count": 7500,
            "description": "The second most authentic hadith collection after Bukhari.",
            "icon": "📘"
        },
        {
            "id": 3,
            "name_english": "Sunan Abu Dawud",
            "name_arabic": "سنن أبي داود",
            "author": "Imam Abu Dawud al-Sijistani",
            "hadith_count": 5274,
            "description": "A collection focusing on legal hadiths.",
            "icon": "📙"
        },
        {
            "id": 4,
            "name_english": "Jami at-Tirmidhi",
            "name_arabic": "جامع الترمذي",
            "author": "Imam at-Tirmidhi",
            "hadith_count": 3956,
            "description": "Known for categorizing hadiths by their authenticity.",
            "icon": "📕"
        },
        {
            "id": 5,
            "name_english": "Sunan an-Nasai",
            "name_arabic": "سنن النسائي",
            "author": "Imam an-Nasai",
            "hadith_count": 5758,
            "description": "A collection with strict authentication criteria.",
            "icon": "📓"
        },
        {
            "id": 6,
            "name_english": "Sunan Ibn Majah",
            "name_arabic": "سنن ابن ماجه",
            "author": "Imam Ibn Majah",
            "hadith_count": 4341,
            "description": "The sixth of the six major hadith collections.",
            "icon": "📔"
        }
    ]
    save_json(books, "hadith_books.json")
    return books

def download_hadith_data():
    """Download hadith data from available sources"""
    print("\n" + "="*60)
    print("DOWNLOADING HADITH DATA")
    print("="*60)

    # Try to download from hadith-api.com or similar
    # Since external APIs may require keys, we'll generate sample data with key hadiths

    books_config = [
        ("bukhari", 1, "Sahih al-Bukhari"),
        ("muslim", 2, "Sahih Muslim"),
        ("abudawud", 3, "Sunan Abu Dawud"),
        ("tirmidhi", 4, "Jami at-Tirmidhi"),
        ("nasai", 5, "Sunan an-Nasai"),
        ("ibnmajah", 6, "Sunan Ibn Majah"),
    ]

    # Key authentic hadiths to include
    key_hadiths = {
        1: [  # Bukhari
            {
                "chapter_id": 1, "number_in_book": 1, "number_in_chapter": 1,
                "text_arabic": "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى، فَمَنْ كَانَتْ هِجْرَتُهُ إِلَى اللَّهِ وَرَسُولِهِ فَهِجْرَتُهُ إِلَى اللَّهِ وَرَسُولِهِ، وَمَنْ كَانَتْ هِجْرَتُهُ لِدُنْيَا يُصِيبُهَا أَوِ امْرَأَةٍ يَنْكِحُهَا فَهِجْرَتُهُ إِلَى مَا هَاجَرَ إِلَيْهِ",
                "text_english": "Actions are judged by intentions, so each man will have what he intended. Thus, he whose migration was to Allah and His Messenger, his migration is to Allah and His Messenger; but he whose migration was for some worldly thing he might gain, or for a wife he might marry, his migration is to that for which he migrated.",
                "narrator": "Narrated 'Umar bin Al-Khattab",
                "grade": "Sahih",
                "reference": "bukhari:1"
            },
            {
                "chapter_id": 1, "number_in_book": 2, "number_in_chapter": 2,
                "text_arabic": "بُنِيَ الإِسْلاَمُ عَلَى خَمْسٍ: شَهَادَةِ أَنْ لاَ إِلَهَ إِلاَّ اللَّهُ وَأَنَّ مُحَمَّدًا رَسُولُ اللَّهِ، وَإِقَامِ الصَّلاَةِ، وَإِيتَاءِ الزَّكَاةِ، وَالْحَجِّ، وَصَوْمِ رَمَضَانَ",
                "text_english": "Islam is built on five pillars: testifying that there is no god but Allah and that Muhammad is the Messenger of Allah, establishing prayer, paying Zakat, performing Hajj, and fasting Ramadan.",
                "narrator": "Narrated Ibn 'Umar",
                "grade": "Sahih",
                "reference": "bukhari:8"
            },
            {
                "chapter_id": 2, "number_in_book": 3, "number_in_chapter": 1,
                "text_arabic": "الْمُسْلِمُ مَنْ سَلِمَ الْمُسْلِمُونَ مِنْ لِسَانِهِ وَيَدِهِ، وَالْمُهَاجِرُ مَنْ هَجَرَ مَا نَهَى اللَّهُ عَنْهُ",
                "text_english": "A Muslim is one from whose tongue and hand other Muslims are safe, and a Muhajir (emigrant) is one who abandons what Allah has forbidden.",
                "narrator": "Narrated 'Abdullah bin 'Amr",
                "grade": "Sahih",
                "reference": "bukhari:10"
            },
            {
                "chapter_id": 2, "number_in_book": 4, "number_in_chapter": 2,
                "text_arabic": "لاَ يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ",
                "text_english": "None of you truly believes until he loves for his brother what he loves for himself.",
                "narrator": "Narrated Anas",
                "grade": "Sahih",
                "reference": "bukhari:13"
            },
            {
                "chapter_id": 3, "number_in_book": 5, "number_in_chapter": 1,
                "text_arabic": "مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ",
                "text_english": "Whoever believes in Allah and the Last Day should speak good or remain silent.",
                "narrator": "Narrated Abu Hurairah",
                "grade": "Sahih",
                "reference": "bukhari:6018"
            },
            {
                "chapter_id": 3, "number_in_book": 6, "number_in_chapter": 2,
                "text_arabic": "الدِّينُ النَّصِيحَةُ",
                "text_english": "The religion is sincerity (good counsel).",
                "narrator": "Narrated Tamim ad-Dari",
                "grade": "Sahih",
                "reference": "bukhari:57"
            },
            {
                "chapter_id": 4, "number_in_book": 7, "number_in_chapter": 1,
                "text_arabic": "إِنَّ اللَّهَ لاَ يَنْظُرُ إِلَى صُوَرِكُمْ وَأَمْوَالِكُمْ وَلَكِنْ يَنْظُرُ إِلَى قُلُوبِكُمْ وَأَعْمَالِكُمْ",
                "text_english": "Verily Allah does not look at your appearances or your wealth, but He looks at your hearts and your deeds.",
                "narrator": "Narrated Abu Hurairah",
                "grade": "Sahih",
                "reference": "bukhari:6499"
            },
            {
                "chapter_id": 4, "number_in_book": 8, "number_in_chapter": 2,
                "text_arabic": "مَا مِنْ مُسْلِمٍ يَغْرِسُ غَرْسًا أَوْ يَزْرَعُ زَرْعًا فَيَأْكُلُ مِنْهُ طَيْرٌ أَوْ إِنْسَانٌ أَوْ بَهِيمَةٌ إِلاَّ كَانَ لَهُ بِهِ صَدَقَةٌ",
                "text_english": "There is no Muslim who plants a tree or sows seeds, and then a bird, or a person or an animal eats from it, except that it is charity for him.",
                "narrator": "Narrated Anas bin Malik",
                "grade": "Sahih",
                "reference": "bukhari:2320"
            },
            {
                "chapter_id": 5, "number_in_book": 9, "number_in_chapter": 1,
                "text_arabic": "الطُّهُورُ شَطْرُ الإِيمَانِ",
                "text_english": "Cleanliness is half of faith.",
                "narrator": "Narrated Abu Malik al-Ash'ari",
                "grade": "Sahih",
                "reference": "muslim:223"
            },
            {
                "chapter_id": 5, "number_in_book": 10, "number_in_chapter": 2,
                "text_arabic": "كُلُّكُمْ رَاعٍ وَكُلُّكُمْ مَسْئُولٌ عَنْ رَعِيَّتِهِ",
                "text_english": "Each of you is a shepherd and each of you is responsible for his flock.",
                "narrator": "Narrated 'Abdullah bin 'Umar",
                "grade": "Sahih",
                "reference": "bukhari:893"
            },
        ],
        2: [  # Muslim
            {
                "chapter_id": 1, "number_in_book": 1, "number_in_chapter": 1,
                "text_arabic": "الإِسْلاَمُ أَنْ تَشْهَدَ أَنْ لاَ إِلَهَ إِلاَّ اللَّهُ وَأَنَّ مُحَمَّدًا رَسُولُ اللَّهِ وَتُقِيمَ الصَّلاَةَ وَتُؤْتِيَ الزَّكَاةَ وَتَصُومَ رَمَضَانَ وَتَحُجَّ الْبَيْتَ إِنِ اسْتَطَعْتَ إِلَيْهِ سَبِيلاً",
                "text_english": "Islam is to testify that there is no god but Allah and that Muhammad is the Messenger of Allah, to establish prayer, to pay Zakat, to fast Ramadan, and to make pilgrimage to the House if you are able to do so.",
                "narrator": "Narrated 'Umar bin Al-Khattab",
                "grade": "Sahih",
                "reference": "muslim:8"
            },
            {
                "chapter_id": 1, "number_in_book": 2, "number_in_chapter": 2,
                "text_arabic": "الإِيمَانُ بِضْعٌ وَسَبْعُونَ شُعْبَةً",
                "text_english": "Faith has over seventy branches, the highest of which is the declaration that there is no god but Allah, and the lowest is removing something harmful from the road.",
                "narrator": "Narrated Abu Hurairah",
                "grade": "Sahih",
                "reference": "muslim:35"
            },
            {
                "chapter_id": 2, "number_in_book": 3, "number_in_chapter": 1,
                "text_arabic": "إِنَّ اللَّهَ كَتَبَ الإِحْسَانَ عَلَى كُلِّ شَيْءٍ",
                "text_english": "Verily Allah has prescribed excellence in all things.",
                "narrator": "Narrated Shaddad bin Aws",
                "grade": "Sahih",
                "reference": "muslim:1955"
            },
            {
                "chapter_id": 2, "number_in_book": 4, "number_in_chapter": 2,
                "text_arabic": "مَنْ سَلَكَ طَرِيقًا يَلْتَمِسُ فِيهِ عِلْمًا سَهَّلَ اللَّهُ لَهُ طَرِيقًا إِلَى الْجَنَّةِ",
                "text_english": "Whoever treads a path in search of knowledge, Allah will make easy for him the path to Paradise.",
                "narrator": "Narrated Abu Hurairah",
                "grade": "Sahih",
                "reference": "muslim:2699"
            },
            {
                "chapter_id": 3, "number_in_book": 5, "number_in_chapter": 1,
                "text_arabic": "لاَ تَحَاسَدُوا وَلاَ تَنَاجَشُوا وَلاَ تَبَاغَضُوا وَلاَ تَدَابَرُوا",
                "text_english": "Do not be envious of one another, do not artificially inflate prices, do not hate one another, and do not turn your backs on one another.",
                "narrator": "Narrated Abu Hurairah",
                "grade": "Sahih",
                "reference": "muslim:2564"
            },
            {
                "chapter_id": 3, "number_in_book": 6, "number_in_chapter": 2,
                "text_arabic": "الْمُسْلِمُ أَخُو الْمُسْلِمِ لاَ يَظْلِمُهُ وَلاَ يَخْذُلُهُ وَلاَ يَحْقِرُهُ",
                "text_english": "A Muslim is the brother of a Muslim. He does not wrong him, nor does he forsake him, nor does he despise him.",
                "narrator": "Narrated Abu Hurairah",
                "grade": "Sahih",
                "reference": "muslim:2564"
            },
            {
                "chapter_id": 4, "number_in_book": 7, "number_in_chapter": 1,
                "text_arabic": "إِنَّ اللَّهَ رَفِيقٌ يُحِبُّ الرِّفْقَ وَيُعْطِي عَلَى الرِّفْقِ مَا لاَ يُعْطِي عَلَى الْعُنْفِ",
                "text_english": "Verily Allah is gentle and loves gentleness, and He grants upon gentleness what He does not grant upon harshness.",
                "narrator": "Narrated 'Aisha",
                "grade": "Sahih",
                "reference": "muslim:2593"
            },
            {
                "chapter_id": 4, "number_in_book": 8, "number_in_chapter": 2,
                "text_arabic": "مَنْ لاَ يَرْحَمُ لاَ يُرْحَمُ",
                "text_english": "He who does not show mercy will not be shown mercy.",
                "narrator": "Narrated Jarir bin 'Abdullah",
                "grade": "Sahih",
                "reference": "muslim:2319"
            },
            {
                "chapter_id": 5, "number_in_book": 9, "number_in_chapter": 1,
                "text_arabic": "الدُّعَاءُ هُوَ الْعِبَادَةُ",
                "text_english": "Supplication is worship.",
                "narrator": "Narrated an-Nu'man bin Bashir",
                "grade": "Sahih",
                "reference": "tirmidhi:3372"
            },
            {
                "chapter_id": 5, "number_in_book": 10, "number_in_chapter": 2,
                "text_arabic": "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ",
                "text_english": "The best among you is the one who learns the Quran and teaches it.",
                "narrator": "Narrated 'Uthman bin 'Affan",
                "grade": "Sahih",
                "reference": "bukhari:5027"
            },
        ],
        3: [  # Abu Dawud
            {
                "chapter_id": 1, "number_in_book": 1, "number_in_chapter": 1,
                "text_arabic": "صَلُّوا كَمَا رَأَيْتُمُونِي أُصَلِّي",
                "text_english": "Pray as you have seen me praying.",
                "narrator": "Narrated Malik bin al-Huwayrith",
                "grade": "Sahih",
                "reference": "abudawud:730"
            },
            {
                "chapter_id": 1, "number_in_book": 2, "number_in_chapter": 2,
                "text_arabic": "مِفْتَاحُ الصَّلاَةِ الطُّهُورُ وَتَحْرِيمُهَا التَّكْبِيرُ وَتَحْلِيلُهَا التَّسْلِيمُ",
                "text_english": "The key to prayer is purification, its beginning is takbir and its end is taslim.",
                "narrator": "Narrated 'Ali ibn Abi Talib",
                "grade": "Sahih",
                "reference": "abudawud:61"
            },
            {
                "chapter_id": 2, "number_in_book": 3, "number_in_chapter": 1,
                "text_arabic": "اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ وَأَتْبِعِ السَّيِّئَةَ الْحَسَنَةَ تَمْحُهَا وَخَالِقِ النَّاسَ بِخُلُقٍ حَسَنٍ",
                "text_english": "Fear Allah wherever you are, follow up a bad deed with a good deed and it will wipe it out, and behave well towards people.",
                "narrator": "Narrated Abu Dharr",
                "grade": "Hasan",
                "reference": "abudawud:4246"
            },
            {
                "chapter_id": 2, "number_in_book": 4, "number_in_chapter": 2,
                "text_arabic": "الْمُؤْمِنُ الْقَوِيُّ خَيْرٌ وَأَحَبُّ إِلَى اللَّهِ مِنَ الْمُؤْمِنِ الضَّعِيفِ وَفِي كُلٍّ خَيْرٌ",
                "text_english": "The strong believer is better and more beloved to Allah than the weak believer, although there is good in each.",
                "narrator": "Narrated Abu Hurairah",
                "grade": "Sahih",
                "reference": "muslim:2664"
            },
            {
                "chapter_id": 3, "number_in_book": 5, "number_in_chapter": 1,
                "text_arabic": "إِنَّ مِنْ أَحَبِّكُمْ إِلَىَّ وَأَقْرَبِكُمْ مِنِّي مَجْلِسًا يَوْمَ الْقِيَامَةِ أَحَاسِنُكُمْ أَخْلاَقًا",
                "text_english": "The most beloved of you to me and the nearest to me in assembly on the Day of Resurrection are those with the best character.",
                "narrator": "Narrated Jabir",
                "grade": "Sahih",
                "reference": "tirmidhi:2018"
            },
        ],
        4: [  # Tirmidhi
            {
                "chapter_id": 1, "number_in_book": 1, "number_in_chapter": 1,
                "text_arabic": "الْكَلِمَةُ الطَّيِّبَةُ صَدَقَةٌ",
                "text_english": "A good word is charity.",
                "narrator": "Narrated Abu Hurairah",
                "grade": "Sahih",
                "reference": "tirmidhi:1970"
            },
            {
                "chapter_id": 1, "number_in_book": 2, "number_in_chapter": 2,
                "text_arabic": "تَبَسُّمُكَ فِي وَجْهِ أَخِيكَ صَدَقَةٌ",
                "text_english": "Your smile in the face of your brother is charity.",
                "narrator": "Narrated Abu Dharr",
                "grade": "Sahih",
                "reference": "tirmidhi:1956"
            },
            {
                "chapter_id": 2, "number_in_book": 3, "number_in_chapter": 1,
                "text_arabic": "مَا نَقَصَتْ صَدَقَةٌ مِنْ مَالٍ",
                "text_english": "Charity does not decrease wealth.",
                "narrator": "Narrated Abu Hurairah",
                "grade": "Sahih",
                "reference": "tirmidhi:2029"
            },
            {
                "chapter_id": 2, "number_in_book": 4, "number_in_chapter": 2,
                "text_arabic": "الْمُؤْمِنُ مَنْ أَمِنَهُ النَّاسُ عَلَى دِمَائِهِمْ وَأَمْوَالِهِمْ",
                "text_english": "A believer is one from whom people feel safe regarding their blood and wealth.",
                "narrator": "Narrated Abu Hurairah",
                "grade": "Sahih",
                "reference": "tirmidhi:2627"
            },
            {
                "chapter_id": 3, "number_in_book": 5, "number_in_chapter": 1,
                "text_arabic": "إِنَّ اللَّهَ جَمِيلٌ يُحِبُّ الْجَمَالَ",
                "text_english": "Verily Allah is beautiful and loves beauty.",
                "narrator": "Narrated Abdullah ibn Mas'ud",
                "grade": "Sahih",
                "reference": "muslim:91"
            },
        ],
        5: [  # Nasai
            {
                "chapter_id": 1, "number_in_book": 1, "number_in_chapter": 1,
                "text_arabic": "خَيْرُ الْكَلاَمِ مَا قَلَّ وَدَلَّ",
                "text_english": "The best speech is that which is brief and to the point.",
                "narrator": "Narrated Jabir",
                "grade": "Sahih",
                "reference": "nasai:5891"
            },
            {
                "chapter_id": 1, "number_in_book": 2, "number_in_chapter": 2,
                "text_arabic": "أَحَبُّ الأَعْمَالِ إِلَى اللَّهِ أَدْوَمُهَا وَإِنْ قَلَّ",
                "text_english": "The most beloved of deeds to Allah are the most consistent, even if small.",
                "narrator": "Narrated 'Aisha",
                "grade": "Sahih",
                "reference": "nasai:1649"
            },
            {
                "chapter_id": 2, "number_in_book": 3, "number_in_chapter": 1,
                "text_arabic": "الصَّلاَةُ عَلَى وَقْتِهَا",
                "text_english": "Prayer at its proper time is the most beloved deed to Allah.",
                "narrator": "Narrated Ibn Mas'ud",
                "grade": "Sahih",
                "reference": "nasai:608"
            },
            {
                "chapter_id": 2, "number_in_book": 4, "number_in_chapter": 2,
                "text_arabic": "أَفْضَلُ الصَّلاَةِ صَلاَةُ الرَّجُلِ فِي بَيْتِهِ إِلاَّ الْمَكْتُوبَةَ",
                "text_english": "The best prayer is a person's prayer in his home, except for the obligatory prayer.",
                "narrator": "Narrated Zayd bin Thabit",
                "grade": "Sahih",
                "reference": "nasai:1598"
            },
            {
                "chapter_id": 3, "number_in_book": 5, "number_in_chapter": 1,
                "text_arabic": "مَنْ قَامَ رَمَضَانَ إِيمَانًا وَاحْتِسَابًا غُفِرَ لَهُ مَا تَقَدَّمَ مِنْ ذَنْبِهِ",
                "text_english": "Whoever prays at night during Ramadan with faith and seeking reward, his previous sins will be forgiven.",
                "narrator": "Narrated Abu Hurairah",
                "grade": "Sahih",
                "reference": "nasai:2190"
            },
        ],
        6: [  # Ibn Majah
            {
                "chapter_id": 1, "number_in_book": 1, "number_in_chapter": 1,
                "text_arabic": "طَلَبُ الْعِلْمِ فَرِيضَةٌ عَلَى كُلِّ مُسْلِمٍ",
                "text_english": "Seeking knowledge is an obligation upon every Muslim.",
                "narrator": "Narrated Anas bin Malik",
                "grade": "Sahih",
                "reference": "ibnmajah:224"
            },
            {
                "chapter_id": 1, "number_in_book": 2, "number_in_chapter": 2,
                "text_arabic": "الْعُلَمَاءُ وَرَثَةُ الأَنْبِيَاءِ",
                "text_english": "The scholars are the heirs of the prophets.",
                "narrator": "Narrated Abu Darda",
                "grade": "Sahih",
                "reference": "ibnmajah:223"
            },
            {
                "chapter_id": 2, "number_in_book": 3, "number_in_chapter": 1,
                "text_arabic": "لاَ ضَرَرَ وَلاَ ضِرَارَ",
                "text_english": "There should be neither harm nor reciprocating harm.",
                "narrator": "Narrated Ibn 'Abbas",
                "grade": "Sahih",
                "reference": "ibnmajah:2341"
            },
            {
                "chapter_id": 2, "number_in_book": 4, "number_in_chapter": 2,
                "text_arabic": "إِنَّمَا بُعِثْتُ لأُتَمِّمَ مَكَارِمَ الأَخْلاَقِ",
                "text_english": "I was sent to perfect good character.",
                "narrator": "Narrated Abu Hurairah",
                "grade": "Sahih",
                "reference": "malik:1609"
            },
            {
                "chapter_id": 3, "number_in_book": 5, "number_in_chapter": 1,
                "text_arabic": "أَكْمَلُ الْمُؤْمِنِينَ إِيمَانًا أَحْسَنُهُمْ خُلُقًا",
                "text_english": "The most complete of believers in faith are those with the best character.",
                "narrator": "Narrated Abu Hurairah",
                "grade": "Hasan",
                "reference": "ibnmajah:4259"
            },
        ],
    }

    global_hadith_id = 1
    for book_key, book_id, book_name in books_config:
        print(f"\n  Processing {book_name}...")
        hadiths = []

        if book_id in key_hadiths:
            for h in key_hadiths[book_id]:
                hadiths.append({
                    "id": global_hadith_id,
                    "book_id": book_id,
                    **h
                })
                global_hadith_id += 1

        save_json(hadiths, f"hadith_{book_key}.json")
        print(f"    Added {len(hadiths)} hadiths")

    return True

def generate_dua_categories():
    """Generate dua categories from Hisnul Muslim"""
    print("\n" + "="*60)
    print("GENERATING DUA CATEGORIES")
    print("="*60)

    categories = [
        {"id": 1, "name_english": "Morning Adhkar", "name_arabic": "أذكار الصباح", "icon": "🌅", "display_order": 1, "dua_count": 15},
        {"id": 2, "name_english": "Evening Adhkar", "name_arabic": "أذكار المساء", "icon": "🌙", "display_order": 2, "dua_count": 15},
        {"id": 3, "name_english": "After Prayer", "name_arabic": "أذكار بعد الصلاة", "icon": "🤲", "display_order": 3, "dua_count": 10},
        {"id": 4, "name_english": "Waking Up", "name_arabic": "دعاء الاستيقاظ", "icon": "☀️", "display_order": 4, "dua_count": 5},
        {"id": 5, "name_english": "Before Sleep", "name_arabic": "دعاء النوم", "icon": "😴", "display_order": 5, "dua_count": 8},
        {"id": 6, "name_english": "Entering Home", "name_arabic": "دعاء دخول المنزل", "icon": "🏠", "display_order": 6, "dua_count": 3},
        {"id": 7, "name_english": "Leaving Home", "name_arabic": "دعاء الخروج من المنزل", "icon": "🚪", "display_order": 7, "dua_count": 3},
        {"id": 8, "name_english": "Entering Mosque", "name_arabic": "دعاء دخول المسجد", "icon": "🕌", "display_order": 8, "dua_count": 3},
        {"id": 9, "name_english": "Leaving Mosque", "name_arabic": "دعاء الخروج من المسجد", "icon": "🕋", "display_order": 9, "dua_count": 3},
        {"id": 10, "name_english": "Before Eating", "name_arabic": "دعاء قبل الطعام", "icon": "🍽️", "display_order": 10, "dua_count": 4},
        {"id": 11, "name_english": "After Eating", "name_arabic": "دعاء بعد الطعام", "icon": "✨", "display_order": 11, "dua_count": 4},
        {"id": 12, "name_english": "Traveling", "name_arabic": "دعاء السفر", "icon": "✈️", "display_order": 12, "dua_count": 6},
        {"id": 13, "name_english": "Rain", "name_arabic": "دعاء المطر", "icon": "🌧️", "display_order": 13, "dua_count": 4},
        {"id": 14, "name_english": "Distress & Anxiety", "name_arabic": "دعاء الكرب والهم", "icon": "💚", "display_order": 14, "dua_count": 8},
        {"id": 15, "name_english": "Forgiveness", "name_arabic": "الاستغفار", "icon": "🙏", "display_order": 15, "dua_count": 6},
    ]
    save_json(categories, "dua_categories.json")
    return categories

def generate_duas():
    """Generate duas from Hisnul Muslim"""
    print("\n" + "="*60)
    print("GENERATING DUAS FROM HISNUL MUSLIM")
    print("="*60)

    duas = [
        # Morning Adhkar (Category 1)
        {
            "id": 1, "category_id": 1, "title_english": "Ayatul Kursi", "title_arabic": "آية الكرسي",
            "text_arabic": "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَنْ ذَا الَّذِي يَشْفَعُ عِنْدَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ",
            "transliteration": "Allahu la ilaha illa Huwa, Al-Hayyul-Qayyum. La ta'khudhuhu sinatun wa la nawm. Lahu ma fis-samawati wa ma fil-ard. Man dhal-ladhi yashfa'u 'indahu illa bi-idhnihi. Ya'lamu ma bayna aydihim wa ma khalfahum. Wa la yuhituna bi shay'in min 'ilmihi illa bima sha'a. Wasi'a kursiyyuhus-samawati wal-ard. Wa la ya'uduhu hifdhuhuma. Wa Huwal-'Aliyyul-'Adhim.",
            "translation": "Allah! There is no god but He, the Living, the Self-subsisting, Eternal. No slumber can seize Him nor sleep. His are all things in the heavens and on earth. Who is there that can intercede in His presence except as He permits? He knows what is before them and what is behind them. Nor shall they compass any of His knowledge except as He wills. His Throne extends over the heavens and the earth, and He feels no fatigue in guarding and preserving them. And He is the Most High, the Most Great.",
            "source": "Quran 2:255", "virtue": "Whoever recites this when he rises in the morning will be protected from jinns until he retires in the evening, and whoever recites it when retiring in the evening will be protected from them until he rises in the morning.",
            "repeat_count": 1, "audio_file": None, "display_order": 1
        },
        {
            "id": 2, "category_id": 1, "title_english": "Morning Remembrance", "title_arabic": "أذكار الصباح",
            "text_arabic": "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            "transliteration": "Asbahna wa asbahal-mulku lillah, walhamdu lillah, la ilaha illallahu wahdahu la sharika lah, lahul-mulku wa lahul-hamdu wa Huwa 'ala kulli shay'in Qadir.",
            "translation": "We have reached the morning and at this very time all sovereignty belongs to Allah. All praise is for Allah. None has the right to be worshipped except Allah, alone, without partner. To Him belongs all sovereignty and praise, and He is over all things omnipotent.",
            "source": "Abu Dawud", "virtue": "Said once in the morning",
            "repeat_count": 1, "audio_file": None, "display_order": 2
        },
        {
            "id": 3, "category_id": 1, "title_english": "Seeking Protection", "title_arabic": "دعاء الحفظ",
            "text_arabic": "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ",
            "transliteration": "Allahumma bika asbahna, wa bika amsayna, wa bika nahya, wa bika namutu, wa ilaykan-nushur.",
            "translation": "O Allah, by Your leave we have reached the morning and by Your leave we have reached the evening, by Your leave we live and die and unto You is our resurrection.",
            "source": "Tirmidhi", "virtue": "Recited in the morning",
            "repeat_count": 1, "audio_file": None, "display_order": 3
        },
        {
            "id": 4, "category_id": 1, "title_english": "Sayyidul Istighfar", "title_arabic": "سيد الاستغفار",
            "text_arabic": "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            "transliteration": "Allahumma anta Rabbi la ilaha illa anta, khalaqtani wa ana 'abduka, wa ana 'ala 'ahdika wa wa'dika mastata'tu, a'udhu bika min sharri ma sana'tu, abu'u laka bini'matika 'alayya, wa abu'u bidhanbi faghfir li fa'innahu la yaghfirudh-dhunuba illa anta.",
            "translation": "O Allah, You are my Lord, none has the right to be worshipped except You. You created me and I am Your servant, and I abide by Your covenant and promise as best I can. I seek refuge in You from the evil of what I have done. I acknowledge Your favor upon me and I acknowledge my sin, so forgive me, for verily none can forgive sins except You.",
            "source": "Bukhari", "virtue": "Whoever recites this with conviction in the morning and dies before evening enters Paradise, and whoever recites this with conviction in the evening and dies before morning enters Paradise.",
            "repeat_count": 1, "audio_file": None, "display_order": 4
        },
        {
            "id": 5, "category_id": 1, "title_english": "SubhanAllah wa bihamdihi", "title_arabic": "سبحان الله وبحمده",
            "text_arabic": "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
            "transliteration": "SubhanAllahi wa bihamdihi",
            "translation": "Glory be to Allah and praise be to Him.",
            "source": "Muslim", "virtue": "Whoever recites this 100 times in the morning and evening, his sins will be forgiven even if they are like the foam of the sea.",
            "repeat_count": 100, "audio_file": None, "display_order": 5
        },
        {
            "id": 6, "category_id": 1, "title_english": "Protection from Evil", "title_arabic": "أعوذ بكلمات الله",
            "text_arabic": "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
            "transliteration": "A'udhu bikalimatillahit-tammati min sharri ma khalaq.",
            "translation": "I seek refuge in the perfect words of Allah from the evil of what He has created.",
            "source": "Muslim", "virtue": "Whoever recites this three times in the evening will not be harmed by any poisonous sting that night.",
            "repeat_count": 3, "audio_file": None, "display_order": 6
        },
        {
            "id": 7, "category_id": 1, "title_english": "Surah Al-Ikhlas", "title_arabic": "سورة الإخلاص",
            "text_arabic": "قُلْ هُوَ اللَّهُ أَحَدٌ ۝ اللَّهُ الصَّمَدُ ۝ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝ وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ",
            "transliteration": "Qul Huwa Allahu Ahad. Allahus-Samad. Lam yalid wa lam yulad. Wa lam yakun lahu kufuwan ahad.",
            "translation": "Say: He is Allah, the One. Allah, the Eternal, Absolute. He begets not, nor is He begotten. And there is none like unto Him.",
            "source": "Quran 112:1-4", "virtue": "Reciting this surah three times in the morning and evening is equivalent to reciting the entire Quran.",
            "repeat_count": 3, "audio_file": None, "display_order": 7
        },
        {
            "id": 8, "category_id": 1, "title_english": "Surah Al-Falaq", "title_arabic": "سورة الفلق",
            "text_arabic": "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ ۝ مِنْ شَرِّ مَا خَلَقَ ۝ وَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ ۝ وَمِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ ۝ وَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ",
            "transliteration": "Qul a'udhu bi Rabbil-Falaq. Min sharri ma khalaq. Wa min sharri ghasiqin idha waqab. Wa min sharrin-naffathati fil-'uqad. Wa min sharri hasidin idha hasad.",
            "translation": "Say: I seek refuge with the Lord of the Dawn. From the mischief of created things. From the mischief of Darkness as it overspreads. From the mischief of those who practice secret arts. And from the mischief of the envious one as he practices envy.",
            "source": "Quran 113:1-5", "virtue": "Protection from evil",
            "repeat_count": 3, "audio_file": None, "display_order": 8
        },
        {
            "id": 9, "category_id": 1, "title_english": "Surah An-Nas", "title_arabic": "سورة الناس",
            "text_arabic": "قُلْ أَعُوذُ بِرَبِّ النَّاسِ ۝ مَلِكِ النَّاسِ ۝ إِلَٰهِ النَّاسِ ۝ مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ ۝ الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ ۝ مِنَ الْجِنَّةِ وَالنَّاسِ",
            "transliteration": "Qul a'udhu bi Rabbin-Nas. Malikin-Nas. Ilahin-Nas. Min sharril-waswaasil-khannas. Alladhi yuwaswisu fi sudurin-nas. Minal-jinnati wan-nas.",
            "translation": "Say: I seek refuge with the Lord and Cherisher of Mankind. The King of Mankind. The God of Mankind. From the mischief of the Whisperer who withdraws. Who whispers into the hearts of Mankind. Among Jinns and among men.",
            "source": "Quran 114:1-6", "virtue": "Protection from evil",
            "repeat_count": 3, "audio_file": None, "display_order": 9
        },
        {
            "id": 10, "category_id": 1, "title_english": "La ilaha illallah", "title_arabic": "لا إله إلا الله",
            "text_arabic": "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            "transliteration": "La ilaha illallahu wahdahu la sharika lah, lahul-mulku wa lahul-hamd, wa Huwa 'ala kulli shay'in Qadir.",
            "translation": "None has the right to be worshipped except Allah, alone, without partner. To Him belongs all sovereignty and praise, and He is over all things omnipotent.",
            "source": "Bukhari & Muslim", "virtue": "Whoever says this 10 times will have the reward of freeing four slaves from the children of Isma'il.",
            "repeat_count": 10, "audio_file": None, "display_order": 10
        },

        # Evening Adhkar (Category 2)
        {
            "id": 11, "category_id": 2, "title_english": "Evening Remembrance", "title_arabic": "أذكار المساء",
            "text_arabic": "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            "transliteration": "Amsayna wa amsal-mulku lillah, walhamdu lillah, la ilaha illallahu wahdahu la sharika lah, lahul-mulku wa lahul-hamdu wa Huwa 'ala kulli shay'in Qadir.",
            "translation": "We have reached the evening and at this very time all sovereignty belongs to Allah. All praise is for Allah. None has the right to be worshipped except Allah, alone, without partner. To Him belongs all sovereignty and praise, and He is over all things omnipotent.",
            "source": "Abu Dawud", "virtue": "Said once in the evening",
            "repeat_count": 1, "audio_file": None, "display_order": 1
        },
        {
            "id": 12, "category_id": 2, "title_english": "Evening Protection", "title_arabic": "دعاء المساء",
            "text_arabic": "اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ الْمَصِيرُ",
            "transliteration": "Allahumma bika amsayna, wa bika asbahna, wa bika nahya, wa bika namutu, wa ilaykal-masir.",
            "translation": "O Allah, by Your leave we have reached the evening and by Your leave we have reached the morning, by Your leave we live and die and unto You is our return.",
            "source": "Tirmidhi", "virtue": "Recited in the evening",
            "repeat_count": 1, "audio_file": None, "display_order": 2
        },

        # After Prayer (Category 3)
        {
            "id": 13, "category_id": 3, "title_english": "Istighfar after Prayer", "title_arabic": "الاستغفار بعد الصلاة",
            "text_arabic": "أَسْتَغْفِرُ اللَّهَ",
            "transliteration": "Astaghfirullah",
            "translation": "I seek forgiveness from Allah.",
            "source": "Muslim", "virtue": "Said three times after each obligatory prayer",
            "repeat_count": 3, "audio_file": None, "display_order": 1
        },
        {
            "id": 14, "category_id": 3, "title_english": "Allahumma antas-Salam", "title_arabic": "اللهم أنت السلام",
            "text_arabic": "اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ",
            "transliteration": "Allahumma antas-Salam wa minkas-salam, tabarakta ya dhal-jalali wal-ikram.",
            "translation": "O Allah, You are Peace and from You comes peace. Blessed are You, O Owner of majesty and honor.",
            "source": "Muslim", "virtue": "Said after each prayer",
            "repeat_count": 1, "audio_file": None, "display_order": 2
        },
        {
            "id": 15, "category_id": 3, "title_english": "SubhanAllah", "title_arabic": "سبحان الله",
            "text_arabic": "سُبْحَانَ اللَّهِ",
            "transliteration": "SubhanAllah",
            "translation": "Glory be to Allah.",
            "source": "Bukhari & Muslim", "virtue": "Said 33 times after each obligatory prayer",
            "repeat_count": 33, "audio_file": None, "display_order": 3
        },
        {
            "id": 16, "category_id": 3, "title_english": "Alhamdulillah", "title_arabic": "الحمد لله",
            "text_arabic": "الْحَمْدُ لِلَّهِ",
            "transliteration": "Alhamdulillah",
            "translation": "All praise is for Allah.",
            "source": "Bukhari & Muslim", "virtue": "Said 33 times after each obligatory prayer",
            "repeat_count": 33, "audio_file": None, "display_order": 4
        },
        {
            "id": 17, "category_id": 3, "title_english": "Allahu Akbar", "title_arabic": "الله أكبر",
            "text_arabic": "اللَّهُ أَكْبَرُ",
            "transliteration": "Allahu Akbar",
            "translation": "Allah is the Greatest.",
            "source": "Bukhari & Muslim", "virtue": "Said 34 times after each obligatory prayer",
            "repeat_count": 34, "audio_file": None, "display_order": 5
        },

        # Waking Up (Category 4)
        {
            "id": 18, "category_id": 4, "title_english": "Upon Waking Up", "title_arabic": "دعاء الاستيقاظ من النوم",
            "text_arabic": "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            "transliteration": "Alhamdulillahil-ladhi ahyana ba'da ma amatana wa ilayhin-nushur.",
            "translation": "All praise is for Allah who gave us life after having taken it from us and unto Him is the resurrection.",
            "source": "Bukhari", "virtue": "Said upon waking up",
            "repeat_count": 1, "audio_file": None, "display_order": 1
        },

        # Before Sleep (Category 5)
        {
            "id": 19, "category_id": 5, "title_english": "Before Sleeping", "title_arabic": "دعاء قبل النوم",
            "text_arabic": "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا",
            "transliteration": "Bismika Allahumma amutu wa ahya.",
            "translation": "In Your name, O Allah, I die and I live.",
            "source": "Bukhari", "virtue": "Said before sleeping",
            "repeat_count": 1, "audio_file": None, "display_order": 1
        },
        {
            "id": 20, "category_id": 5, "title_english": "Sleeping Dua", "title_arabic": "دعاء النوم",
            "text_arabic": "اللَّهُمَّ قِنِي عَذَابَكَ يَوْمَ تَبْعَثُ عِبَادَكَ",
            "transliteration": "Allahumma qini 'adhabaka yawma tab'athu 'ibadak.",
            "translation": "O Allah, protect me from Your punishment on the Day You resurrect Your servants.",
            "source": "Abu Dawud", "virtue": "Said three times before sleeping",
            "repeat_count": 3, "audio_file": None, "display_order": 2
        },

        # Entering Home (Category 6)
        {
            "id": 21, "category_id": 6, "title_english": "Entering the Home", "title_arabic": "دعاء دخول المنزل",
            "text_arabic": "بِسْمِ اللَّهِ وَلَجْنَا، وَبِسْمِ اللَّهِ خَرَجْنَا، وَعَلَى اللَّهِ رَبِّنَا تَوَكَّلْنَا",
            "transliteration": "Bismillahi walajna, wa bismillahi kharajna, wa 'ala Allahi rabbina tawakkalna.",
            "translation": "In the name of Allah we enter, and in the name of Allah we leave, and upon our Lord we place our trust.",
            "source": "Abu Dawud", "virtue": "When entering the home",
            "repeat_count": 1, "audio_file": None, "display_order": 1
        },

        # Leaving Home (Category 7)
        {
            "id": 22, "category_id": 7, "title_english": "Leaving the Home", "title_arabic": "دعاء الخروج من المنزل",
            "text_arabic": "بِسْمِ اللَّهِ، تَوَكَّلْتُ عَلَى اللَّهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            "transliteration": "Bismillah, tawakkaltu 'alallah, wa la hawla wa la quwwata illa billah.",
            "translation": "In the name of Allah, I place my trust in Allah, and there is no might nor power except with Allah.",
            "source": "Abu Dawud & Tirmidhi", "virtue": "When leaving the home",
            "repeat_count": 1, "audio_file": None, "display_order": 1
        },

        # Entering Mosque (Category 8)
        {
            "id": 23, "category_id": 8, "title_english": "Entering the Mosque", "title_arabic": "دعاء دخول المسجد",
            "text_arabic": "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            "transliteration": "Allahumma-ftah li abwaba rahmatik.",
            "translation": "O Allah, open for me the doors of Your mercy.",
            "source": "Muslim", "virtue": "When entering the mosque",
            "repeat_count": 1, "audio_file": None, "display_order": 1
        },

        # Leaving Mosque (Category 9)
        {
            "id": 24, "category_id": 9, "title_english": "Leaving the Mosque", "title_arabic": "دعاء الخروج من المسجد",
            "text_arabic": "اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ",
            "transliteration": "Allahumma inni as'aluka min fadlik.",
            "translation": "O Allah, I ask You of Your favor.",
            "source": "Muslim", "virtue": "When leaving the mosque",
            "repeat_count": 1, "audio_file": None, "display_order": 1
        },

        # Before Eating (Category 10)
        {
            "id": 25, "category_id": 10, "title_english": "Before Eating", "title_arabic": "دعاء قبل الطعام",
            "text_arabic": "بِسْمِ اللَّهِ",
            "transliteration": "Bismillah",
            "translation": "In the name of Allah.",
            "source": "Bukhari & Muslim", "virtue": "If one forgets to say Bismillah at the beginning, one should say: Bismillahi fi awwalihi wa akhirihi.",
            "repeat_count": 1, "audio_file": None, "display_order": 1
        },

        # After Eating (Category 11)
        {
            "id": 26, "category_id": 11, "title_english": "After Eating", "title_arabic": "دعاء بعد الطعام",
            "text_arabic": "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنِي هَٰذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ",
            "transliteration": "Alhamdulillahil-ladhi at'amani hadha wa razaqanihi min ghayri hawlin minni wa la quwwah.",
            "translation": "All praise is for Allah who fed me this and provided it for me without any might or power on my part.",
            "source": "Abu Dawud & Tirmidhi", "virtue": "Whoever says this after eating, his past sins will be forgiven.",
            "repeat_count": 1, "audio_file": None, "display_order": 1
        },

        # Traveling (Category 12)
        {
            "id": 27, "category_id": 12, "title_english": "Travel Dua", "title_arabic": "دعاء السفر",
            "text_arabic": "اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، اللَّهُ أَكْبَرُ، سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ، وَإِنَّا إِلَىٰ رَبِّنَا لَمُنْقَلِبُونَ",
            "transliteration": "Allahu Akbar, Allahu Akbar, Allahu Akbar. Subhanal-ladhi sakhkhara lana hadha wa ma kunna lahu muqrinin, wa inna ila Rabbina lamunqalibun.",
            "translation": "Allah is the Greatest, Allah is the Greatest, Allah is the Greatest. Glory be to Him who has subjected this to us, and we could never have it (by ourselves). And verily, to Our Lord we indeed are to return.",
            "source": "Muslim", "virtue": "Said when beginning a journey",
            "repeat_count": 1, "audio_file": None, "display_order": 1
        },

        # Rain (Category 13)
        {
            "id": 28, "category_id": 13, "title_english": "When it Rains", "title_arabic": "دعاء نزول المطر",
            "text_arabic": "اللَّهُمَّ صَيِّبًا نَافِعًا",
            "transliteration": "Allahumma sayyiban nafi'an.",
            "translation": "O Allah, beneficial rain.",
            "source": "Bukhari", "virtue": "Said when it rains",
            "repeat_count": 1, "audio_file": None, "display_order": 1
        },
        {
            "id": 29, "category_id": 13, "title_english": "After Rain", "title_arabic": "دعاء بعد المطر",
            "text_arabic": "مُطِرْنَا بِفَضْلِ اللَّهِ وَرَحْمَتِهِ",
            "transliteration": "Mutirna bi fadlillahi wa rahmatihi.",
            "translation": "We have been given rain by the grace and mercy of Allah.",
            "source": "Bukhari & Muslim", "virtue": "Said after rain has fallen",
            "repeat_count": 1, "audio_file": None, "display_order": 2
        },

        # Distress & Anxiety (Category 14)
        {
            "id": 30, "category_id": 14, "title_english": "Dua for Distress", "title_arabic": "دعاء الكرب",
            "text_arabic": "لَا إِلَٰهَ إِلَّا اللَّهُ الْعَظِيمُ الْحَلِيمُ، لَا إِلَٰهَ إِلَّا اللَّهُ رَبُّ الْعَرْشِ الْعَظِيمِ، لَا إِلَٰهَ إِلَّا اللَّهُ رَبُّ السَّمَاوَاتِ وَرَبُّ الْأَرْضِ وَرَبُّ الْعَرْشِ الْكَرِيمِ",
            "transliteration": "La ilaha illallahul-'Adhimul-Halim, la ilaha illallahu Rabbul-'Arshil-'Adhim, la ilaha illallahu Rabbus-samawati wa Rabbul-ardi wa Rabbul-'Arshil-Karim.",
            "translation": "None has the right to be worshipped except Allah, the Mighty, the Forbearing. None has the right to be worshipped except Allah, Lord of the Magnificent Throne. None has the right to be worshipped except Allah, Lord of the heavens, Lord of the earth, and Lord of the Noble Throne.",
            "source": "Bukhari & Muslim", "virtue": "Dua said during distress",
            "repeat_count": 1, "audio_file": None, "display_order": 1
        },
        {
            "id": 31, "category_id": 14, "title_english": "Dua for Anxiety", "title_arabic": "دعاء الهم والحزن",
            "text_arabic": "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْبُخْلِ وَالْجُبْنِ، وَضَلَعِ الدَّيْنِ وَغَلَبَةِ الرِّجَالِ",
            "transliteration": "Allahumma inni a'udhu bika minal-hammi wal-hazan, wal-'ajzi wal-kasal, wal-bukhli wal-jubn, wa dala'id-dayni wa ghalabatir-rijal.",
            "translation": "O Allah, I seek refuge in You from anxiety and sorrow, weakness and laziness, miserliness and cowardice, the burden of debts and being overpowered by men.",
            "source": "Bukhari", "virtue": "Comprehensive dua for relief from anxiety",
            "repeat_count": 1, "audio_file": None, "display_order": 2
        },

        # Forgiveness (Category 15)
        {
            "id": 32, "category_id": 15, "title_english": "Seeking Forgiveness", "title_arabic": "الاستغفار",
            "text_arabic": "أَسْتَغْفِرُ اللَّهَ الَّذِي لَا إِلَٰهَ إِلَّا هُوَ الْحَيَّ الْقَيُّومَ وَأَتُوبُ إِلَيْهِ",
            "transliteration": "Astaghfirullaha alladhi la ilaha illa Huwal-Hayyul-Qayyum wa atubu ilayhi.",
            "translation": "I seek forgiveness from Allah, there is no god but He, the Living, the Self-Sustaining, and I repent to Him.",
            "source": "Abu Dawud & Tirmidhi", "virtue": "Whoever says this will be forgiven even if he fled from the battlefield.",
            "repeat_count": 1, "audio_file": None, "display_order": 1
        },
        {
            "id": 33, "category_id": 15, "title_english": "Simple Istighfar", "title_arabic": "استغفار بسيط",
            "text_arabic": "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ",
            "transliteration": "Astaghfirullaha wa atubu ilayhi.",
            "translation": "I seek forgiveness from Allah and I repent to Him.",
            "source": "Bukhari & Muslim", "virtue": "The Prophet (ﷺ) used to seek forgiveness from Allah more than 70 times a day.",
            "repeat_count": 100, "audio_file": None, "display_order": 2
        },
    ]

    save_json(duas, "duas.json")
    print(f"  Generated {len(duas)} duas")
    return duas

def generate_islamic_events():
    """Generate Islamic calendar events"""
    print("\n" + "="*60)
    print("GENERATING ISLAMIC EVENTS")
    print("="*60)

    events = [
        {"id": 1, "name_english": "Islamic New Year", "name_arabic": "رأس السنة الهجرية", "hijri_month": 1, "hijri_day": 1, "event_type": "HOLIDAY", "description": "The first day of the Islamic calendar, marking the migration of Prophet Muhammad (ﷺ) from Mecca to Medina.", "is_holiday": True},
        {"id": 2, "name_english": "Day of Ashura", "name_arabic": "يوم عاشوراء", "hijri_month": 1, "hijri_day": 10, "event_type": "FASTING", "description": "A day of fasting commemorating when Allah saved Prophet Musa (Moses) and the Children of Israel from Pharaoh.", "is_holiday": False},
        {"id": 3, "name_english": "Mawlid an-Nabi", "name_arabic": "المولد النبوي", "hijri_month": 3, "hijri_day": 12, "event_type": "CELEBRATION", "description": "The birthday of Prophet Muhammad (ﷺ).", "is_holiday": True},
        {"id": 4, "name_english": "Isra and Mi'raj", "name_arabic": "الإسراء والمعراج", "hijri_month": 7, "hijri_day": 27, "event_type": "CELEBRATION", "description": "The night journey of Prophet Muhammad (ﷺ) from Mecca to Jerusalem and his ascension to the heavens.", "is_holiday": False},
        {"id": 5, "name_english": "Laylat al-Bara'at", "name_arabic": "ليلة البراءة", "hijri_month": 8, "hijri_day": 15, "event_type": "NIGHT", "description": "The Night of Forgiveness, a night of prayer and seeking forgiveness.", "is_holiday": False},
        {"id": 6, "name_english": "Start of Ramadan", "name_arabic": "بداية رمضان", "hijri_month": 9, "hijri_day": 1, "event_type": "FASTING", "description": "The beginning of the holy month of fasting.", "is_holiday": False},
        {"id": 7, "name_english": "Laylat al-Qadr", "name_arabic": "ليلة القدر", "hijri_month": 9, "hijri_day": 27, "event_type": "NIGHT", "description": "The Night of Power, better than a thousand months, when the Quran was first revealed.", "is_holiday": False},
        {"id": 8, "name_english": "Eid al-Fitr (Day 1)", "name_arabic": "عيد الفطر", "hijri_month": 10, "hijri_day": 1, "event_type": "HOLIDAY", "description": "The Festival of Breaking the Fast, celebrating the end of Ramadan.", "is_holiday": True},
        {"id": 9, "name_english": "Eid al-Fitr (Day 2)", "name_arabic": "عيد الفطر", "hijri_month": 10, "hijri_day": 2, "event_type": "HOLIDAY", "description": "Second day of Eid al-Fitr celebrations.", "is_holiday": True},
        {"id": 10, "name_english": "Eid al-Fitr (Day 3)", "name_arabic": "عيد الفطر", "hijri_month": 10, "hijri_day": 3, "event_type": "HOLIDAY", "description": "Third day of Eid al-Fitr celebrations.", "is_holiday": True},
        {"id": 11, "name_english": "Day of Arafah", "name_arabic": "يوم عرفة", "hijri_month": 12, "hijri_day": 9, "event_type": "FASTING", "description": "A blessed day of fasting for those not performing Hajj. Standing at Arafah is the most important pillar of Hajj.", "is_holiday": False},
        {"id": 12, "name_english": "Eid al-Adha (Day 1)", "name_arabic": "عيد الأضحى", "hijri_month": 12, "hijri_day": 10, "event_type": "HOLIDAY", "description": "The Festival of Sacrifice, commemorating Prophet Ibrahim's willingness to sacrifice his son.", "is_holiday": True},
        {"id": 13, "name_english": "Eid al-Adha (Day 2)", "name_arabic": "عيد الأضحى", "hijri_month": 12, "hijri_day": 11, "event_type": "HOLIDAY", "description": "Second day of Eid al-Adha (Days of Tashreeq).", "is_holiday": True},
        {"id": 14, "name_english": "Eid al-Adha (Day 3)", "name_arabic": "عيد الأضحى", "hijri_month": 12, "hijri_day": 12, "event_type": "HOLIDAY", "description": "Third day of Eid al-Adha (Days of Tashreeq).", "is_holiday": True},
        {"id": 15, "name_english": "Eid al-Adha (Day 4)", "name_arabic": "عيد الأضحى", "hijri_month": 12, "hijri_day": 13, "event_type": "HOLIDAY", "description": "Fourth day of Eid al-Adha (Days of Tashreeq).", "is_holiday": True},
        {"id": 16, "name_english": "White Days Fast (13th)", "name_arabic": "صيام الأيام البيض", "hijri_month": 0, "hijri_day": 13, "event_type": "FASTING", "description": "Recommended fasting on the 13th day of every lunar month.", "is_holiday": False},
        {"id": 17, "name_english": "White Days Fast (14th)", "name_arabic": "صيام الأيام البيض", "hijri_month": 0, "hijri_day": 14, "event_type": "FASTING", "description": "Recommended fasting on the 14th day of every lunar month.", "is_holiday": False},
        {"id": 18, "name_english": "White Days Fast (15th)", "name_arabic": "صيام الأيام البيض", "hijri_month": 0, "hijri_day": 15, "event_type": "FASTING", "description": "Recommended fasting on the 15th day of every lunar month.", "is_holiday": False},
    ]

    save_json(events, "islamic_events.json")
    print(f"  Generated {len(events)} events")
    return events

def generate_tasbih_presets():
    """Generate tasbih dhikr presets"""
    print("\n" + "="*60)
    print("GENERATING TASBIH PRESETS")
    print("="*60)

    presets = [
        {"id": 1, "name": "SubhanAllah", "arabic": "سُبْحَانَ اللَّهِ", "transliteration": "SubhanAllah", "translation": "Glory be to Allah", "target_count": 33, "is_custom": False, "display_order": 1},
        {"id": 2, "name": "Alhamdulillah", "arabic": "الْحَمْدُ لِلَّهِ", "transliteration": "Alhamdulillah", "translation": "All praise is for Allah", "target_count": 33, "is_custom": False, "display_order": 2},
        {"id": 3, "name": "Allahu Akbar", "arabic": "اللَّهُ أَكْبَرُ", "transliteration": "Allahu Akbar", "translation": "Allah is the Greatest", "target_count": 34, "is_custom": False, "display_order": 3},
        {"id": 4, "name": "La ilaha illallah", "arabic": "لَا إِلَٰهَ إِلَّا اللَّهُ", "transliteration": "La ilaha illallah", "translation": "There is no god but Allah", "target_count": 100, "is_custom": False, "display_order": 4},
        {"id": 5, "name": "Astaghfirullah", "arabic": "أَسْتَغْفِرُ اللَّهَ", "transliteration": "Astaghfirullah", "translation": "I seek forgiveness from Allah", "target_count": 100, "is_custom": False, "display_order": 5},
        {"id": 6, "name": "La hawla wa la quwwata illa billah", "arabic": "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", "transliteration": "La hawla wa la quwwata illa billah", "translation": "There is no power nor strength except with Allah", "target_count": 100, "is_custom": False, "display_order": 6},
        {"id": 7, "name": "Salawat on Prophet", "arabic": "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ", "transliteration": "Allahumma salli 'ala Muhammad", "translation": "O Allah, send blessings upon Muhammad", "target_count": 100, "is_custom": False, "display_order": 7},
        {"id": 8, "name": "SubhanAllahi wa bihamdihi", "arabic": "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "transliteration": "SubhanAllahi wa bihamdihi", "translation": "Glory be to Allah and praise be to Him", "target_count": 100, "is_custom": False, "display_order": 8},
        {"id": 9, "name": "SubhanAllahil Adhim", "arabic": "سُبْحَانَ اللَّهِ الْعَظِيمِ", "transliteration": "SubhanAllahil-Adhim", "translation": "Glory be to Allah the Magnificent", "target_count": 100, "is_custom": False, "display_order": 9},
        {"id": 10, "name": "Hasbunallahu wa ni'mal wakil", "arabic": "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ", "transliteration": "Hasbunallahu wa ni'mal wakil", "translation": "Allah is sufficient for us, and He is the Best Disposer of affairs", "target_count": 100, "is_custom": False, "display_order": 10},
    ]

    save_json(presets, "tasbih_presets.json")
    print(f"  Generated {len(presets)} presets")
    return presets

def create_database():
    """Create SQLite database from JSON files"""
    print("\n" + "="*60)
    print("GENERATING SQLITE DATABASE")
    print("="*60)

    db_path = OUTPUT_DIR / "nimaz_prepopulated.db"
    if db_path.exists():
        db_path.unlink()

    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    # Create tables
    print("\n  Creating tables...")

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS surahs (
            id INTEGER PRIMARY KEY,
            number INTEGER NOT NULL,
            name_arabic TEXT NOT NULL,
            name_english TEXT NOT NULL,
            name_transliteration TEXT NOT NULL,
            revelation_type TEXT NOT NULL,
            verses_count INTEGER NOT NULL,
            order_revealed INTEGER NOT NULL,
            start_page INTEGER NOT NULL
        )
    ''')

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS ayahs (
            id INTEGER PRIMARY KEY,
            surah_id INTEGER NOT NULL,
            number_in_surah INTEGER NOT NULL,
            number_global INTEGER NOT NULL,
            text_arabic TEXT NOT NULL,
            text_uthmani TEXT NOT NULL,
            juz INTEGER NOT NULL,
            hizb INTEGER NOT NULL,
            page INTEGER NOT NULL,
            sajda INTEGER NOT NULL DEFAULT 0,
            sajda_type TEXT,
            FOREIGN KEY (surah_id) REFERENCES surahs(id)
        )
    ''')

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS translations (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            ayah_id INTEGER NOT NULL,
            translator_id TEXT NOT NULL,
            text TEXT NOT NULL,
            FOREIGN KEY (ayah_id) REFERENCES ayahs(id)
        )
    ''')

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS hadith_books (
            id INTEGER PRIMARY KEY,
            name_english TEXT NOT NULL,
            name_arabic TEXT NOT NULL,
            author TEXT NOT NULL,
            hadith_count INTEGER NOT NULL,
            description TEXT NOT NULL,
            icon TEXT NOT NULL
        )
    ''')

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS hadiths (
            id INTEGER PRIMARY KEY,
            book_id INTEGER NOT NULL,
            chapter_id INTEGER NOT NULL,
            number_in_book INTEGER NOT NULL,
            number_in_chapter INTEGER NOT NULL,
            text_arabic TEXT NOT NULL,
            text_english TEXT NOT NULL,
            narrator TEXT NOT NULL,
            grade TEXT NOT NULL,
            reference TEXT NOT NULL,
            FOREIGN KEY (book_id) REFERENCES hadith_books(id)
        )
    ''')

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS dua_categories (
            id INTEGER PRIMARY KEY,
            name_english TEXT NOT NULL,
            name_arabic TEXT NOT NULL,
            icon TEXT NOT NULL,
            display_order INTEGER NOT NULL,
            dua_count INTEGER NOT NULL
        )
    ''')

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS duas (
            id INTEGER PRIMARY KEY,
            category_id INTEGER NOT NULL,
            title_english TEXT NOT NULL,
            title_arabic TEXT NOT NULL,
            text_arabic TEXT NOT NULL,
            transliteration TEXT NOT NULL,
            translation TEXT NOT NULL,
            source TEXT NOT NULL,
            virtue TEXT,
            repeat_count INTEGER NOT NULL DEFAULT 1,
            audio_file TEXT,
            display_order INTEGER NOT NULL,
            FOREIGN KEY (category_id) REFERENCES dua_categories(id)
        )
    ''')

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS islamic_events (
            id INTEGER PRIMARY KEY,
            name_english TEXT NOT NULL,
            name_arabic TEXT NOT NULL,
            hijri_month INTEGER NOT NULL,
            hijri_day INTEGER NOT NULL,
            event_type TEXT NOT NULL,
            description TEXT NOT NULL,
            is_holiday INTEGER NOT NULL DEFAULT 0
        )
    ''')

    cursor.execute('''
        CREATE TABLE IF NOT EXISTS tasbih_presets (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            arabic TEXT NOT NULL,
            transliteration TEXT NOT NULL,
            translation TEXT NOT NULL,
            target_count INTEGER NOT NULL DEFAULT 33,
            is_custom INTEGER NOT NULL DEFAULT 0,
            display_order INTEGER NOT NULL DEFAULT 0
        )
    ''')

    # Create indices
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_ayahs_surah ON ayahs(surah_id)')
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_ayahs_juz ON ayahs(juz)')
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_translations_ayah ON translations(ayah_id)')
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_hadiths_book ON hadiths(book_id)')
    cursor.execute('CREATE INDEX IF NOT EXISTS idx_duas_category ON duas(category_id)')

    conn.commit()

    # Load and insert data
    print("  Populating database...")

    def load_json(filename):
        filepath = JSON_DIR / filename
        if filepath.exists():
            with open(filepath, 'r', encoding='utf-8') as f:
                return json.load(f)
        return []

    # Surahs
    surahs = load_json('surahs.json')
    for s in surahs:
        cursor.execute('''
            INSERT OR REPLACE INTO surahs VALUES (?,?,?,?,?,?,?,?,?)
        ''', (s['id'], s['number'], s['name_arabic'], s['name_english'],
              s['name_transliteration'], s['revelation_type'], s['verses_count'],
              s['order_revealed'], s['start_page']))
    print(f"    Inserted {len(surahs)} surahs")

    # Ayahs
    ayahs = load_json('ayahs.json')
    for a in ayahs:
        cursor.execute('''
            INSERT OR REPLACE INTO ayahs VALUES (?,?,?,?,?,?,?,?,?,?,?)
        ''', (a['id'], a['surah_id'], a['number_in_surah'], a['number_global'],
              a['text_arabic'], a['text_uthmani'], a['juz'], a['hizb'],
              a['page'], 1 if a.get('sajda') else 0, a.get('sajda_type')))
    print(f"    Inserted {len(ayahs)} ayahs")

    # Translations
    translations = load_json('translations.json')
    for t in translations:
        cursor.execute('''
            INSERT INTO translations (ayah_id, translator_id, text) VALUES (?,?,?)
        ''', (t['ayah_id'], t['translator_id'], t['text']))
    print(f"    Inserted {len(translations)} translations")

    # Hadith Books
    books = load_json('hadith_books.json')
    for b in books:
        cursor.execute('''
            INSERT OR REPLACE INTO hadith_books VALUES (?,?,?,?,?,?,?)
        ''', (b['id'], b['name_english'], b['name_arabic'], b['author'],
              b['hadith_count'], b['description'], b['icon']))
    print(f"    Inserted {len(books)} hadith books")

    # Hadiths
    total_hadiths = 0
    for book_file in ['hadith_bukhari.json', 'hadith_muslim.json', 'hadith_abudawud.json',
                      'hadith_tirmidhi.json', 'hadith_nasai.json', 'hadith_ibnmajah.json']:
        hadiths = load_json(book_file)
        for h in hadiths:
            cursor.execute('''
                INSERT OR REPLACE INTO hadiths VALUES (?,?,?,?,?,?,?,?,?,?)
            ''', (h['id'], h['book_id'], h['chapter_id'], h['number_in_book'],
                  h['number_in_chapter'], h['text_arabic'], h['text_english'],
                  h['narrator'], h['grade'], h['reference']))
        total_hadiths += len(hadiths)
    print(f"    Inserted {total_hadiths} hadiths")

    # Dua Categories
    categories = load_json('dua_categories.json')
    for c in categories:
        cursor.execute('''
            INSERT OR REPLACE INTO dua_categories VALUES (?,?,?,?,?,?)
        ''', (c['id'], c['name_english'], c['name_arabic'], c['icon'],
              c['display_order'], c['dua_count']))
    print(f"    Inserted {len(categories)} dua categories")

    # Duas
    duas = load_json('duas.json')
    for d in duas:
        cursor.execute('''
            INSERT OR REPLACE INTO duas VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
        ''', (d['id'], d['category_id'], d['title_english'], d['title_arabic'],
              d['text_arabic'], d['transliteration'], d['translation'],
              d['source'], d.get('virtue'), d['repeat_count'],
              d.get('audio_file'), d['display_order']))
    print(f"    Inserted {len(duas)} duas")

    # Islamic Events
    events = load_json('islamic_events.json')
    for e in events:
        cursor.execute('''
            INSERT OR REPLACE INTO islamic_events VALUES (?,?,?,?,?,?,?,?)
        ''', (e['id'], e['name_english'], e['name_arabic'], e['hijri_month'],
              e['hijri_day'], e['event_type'], e['description'],
              1 if e.get('is_holiday') else 0))
    print(f"    Inserted {len(events)} events")

    # Tasbih Presets
    presets = load_json('tasbih_presets.json')
    for p in presets:
        cursor.execute('''
            INSERT OR REPLACE INTO tasbih_presets VALUES (?,?,?,?,?,?,?,?)
        ''', (p.get('id'), p['name'], p['arabic'], p['transliteration'],
              p['translation'], p['target_count'], 1 if p.get('is_custom') else 0,
              p['display_order']))
    print(f"    Inserted {len(presets)} tasbih presets")

    # Correct start_page values from actual ayah page data
    cursor.execute("""
        UPDATE surahs SET start_page = (
            SELECT MIN(a.page) FROM ayahs a WHERE a.surah_id = surahs.id
        )
    """)
    print("    Corrected surah start_page values from ayah data")

    # Set Room database version so migrations are skipped
    conn.execute("PRAGMA user_version = 12")
    print("    Set user_version = 12 (Room schema version)")

    conn.commit()
    conn.close()

    db_size = db_path.stat().st_size / 1024 / 1024
    print(f"\n  Database created: {db_path}")
    print(f"  Size: {db_size:.2f} MB")

    return db_path

def main():
    print("="*60)
    print("NIMAZ PRO - DATA GENERATION SCRIPT")
    print("="*60)

    # Step 1: Download Quran data
    if not download_quran_data():
        print("\nERROR: Failed to download Quran data. Exiting.")
        return

    # Step 2: Generate hadith books metadata
    generate_hadith_books()

    # Step 3: Download/generate hadith data
    download_hadith_data()

    # Step 4: Generate dua categories
    generate_dua_categories()

    # Step 5: Generate duas
    generate_duas()

    # Step 6: Generate Islamic events
    generate_islamic_events()

    # Step 7: Generate tasbih presets
    generate_tasbih_presets()

    # Step 8: Create database
    db_path = create_database()

    print("\n" + "="*60)
    print("GENERATION COMPLETE!")
    print("="*60)
    print(f"\nDatabase ready at: {db_path}")
    print(f"\nCopy to: app/src/main/assets/database/nimaz_prepopulated.db")

if __name__ == "__main__":
    main()
