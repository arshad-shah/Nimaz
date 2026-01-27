# Nimaz Pro - Data Sourcing & Integration Guide

## For Parallel Claude Code Instance

**Purpose**: Generate all Islamic content data in the exact JSON format required by the app.

---

# Quick Start

## Output Structure

```
nimaz-pro-data/
├── json/
│   ├── surahs.json              # 114 surahs
│   ├── ayahs.json               # 6,236 verses
│   ├── translations.json        # Sahih International
│   ├── hadith_books.json        # 6 collections metadata
│   ├── hadith_bukhari.json      # Bukhari hadiths
│   ├── hadith_muslim.json       # Muslim hadiths
│   ├── hadith_abudawud.json     # Abu Dawud hadiths
│   ├── hadith_tirmidhi.json     # Tirmidhi hadiths
│   ├── hadith_nasai.json        # Nasai hadiths
│   ├── hadith_ibnmajah.json     # Ibn Majah hadiths
│   ├── dua_categories.json      # 15 categories
│   ├── duas.json                # 200+ duas
│   ├── islamic_events.json      # Calendar events
│   └── tasbih_presets.json      # Dhikr presets
└── scripts/
    └── generate_database.py     # SQLite generator
```

---

# 1. Quran Data

## Sources
- **Tanzil.net**: https://tanzil.net/download/ (Arabic text)
- **Quran.com API**: https://api.quran.com/api/v4/ (Translations)
- **quran-json GitHub**: https://github.com/semarketir/quranjson

## surahs.json Format

```json
[
  {
    "id": 1,
    "number": 1,
    "name_arabic": "الفاتحة",
    "name_english": "The Opening",
    "name_transliteration": "Al-Fatihah",
    "revelation_type": "MECCAN",
    "verses_count": 7,
    "order_revealed": 5,
    "start_page": 1
  }
]
```

**Required**: All 114 surahs with accurate metadata.

## ayahs.json Format

```json
[
  {
    "id": 1,
    "surah_id": 1,
    "number_in_surah": 1,
    "number_global": 1,
    "text_arabic": "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
    "text_uthmani": "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ",
    "juz": 1,
    "hizb": 1,
    "page": 1,
    "sajda": false,
    "sajda_type": null
  }
]
```

**Required**: All 6,236 ayahs with juz/hizb/page info.

**Sajda Verses** (mark `sajda: true`):
- 7:206, 13:15, 16:50, 17:109, 19:58, 22:18, 22:77, 25:60, 27:26, 32:15, 38:24, 41:38, 53:62, 84:21, 96:19

## translations.json Format

```json
[
  {
    "id": 1,
    "ayah_id": 1,
    "translator_id": "sahih_international",
    "text": "In the name of Allah, the Entirely Merciful, the Especially Merciful."
  }
]
```

**Recommended Translations**:
- Sahih International (primary)
- Pickthall (optional)
- Yusuf Ali (optional)

---

# 2. Hadith Data

## Sources
- **Sunnah.com API**: https://sunnah.com/developers (requires free API key)
- **HadithAPI**: https://hadithapi.com/
- **GitHub**: https://github.com/semarketir/hadith-json

## hadith_books.json Format

```json
[
  {
    "id": 1,
    "name_english": "Sahih al-Bukhari",
    "name_arabic": "صحيح البخاري",
    "author": "Imam Muhammad al-Bukhari",
    "hadith_count": 7563,
    "description": "The most authentic collection of Hadith.",
    "icon": "📗"
  }
]
```

**Books Required**:
| ID | Name | Arabic | Hadiths |
|----|------|--------|---------|
| 1 | Sahih al-Bukhari | صحيح البخاري | ~7,563 |
| 2 | Sahih Muslim | صحيح مسلم | ~7,500 |
| 3 | Sunan Abu Dawud | سنن أبي داود | ~5,274 |
| 4 | Jami at-Tirmidhi | جامع الترمذي | ~3,956 |
| 5 | Sunan an-Nasai | سنن النسائي | ~5,758 |
| 6 | Sunan Ibn Majah | سنن ابن ماجه | ~4,341 |

## hadith_[book].json Format

```json
[
  {
    "id": 1,
    "book_id": 1,
    "chapter_id": 1,
    "number_in_book": 1,
    "number_in_chapter": 1,
    "text_arabic": "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ...",
    "text_english": "Actions are judged by intentions...",
    "narrator": "Narrated 'Umar bin Al-Khattab",
    "grade": "Sahih",
    "reference": "bukhari:1"
  }
]
```

**Grade Values**: `Sahih`, `Hasan`, `Da'if`, `Mawdu`

---

# 3. Duas & Adhkar

## Source
- **Hisnul Muslim** (Fortress of the Muslim) - Public domain

## dua_categories.json Format

```json
[
  {
    "id": 1,
    "name_english": "Morning Adhkar",
    "name_arabic": "أذكار الصباح",
    "icon": "🌅",
    "display_order": 1,
    "dua_count": 15
  }
]
```

**Categories Required**:
1. Morning Adhkar (أذكار الصباح)
2. Evening Adhkar (أذكار المساء)
3. After Prayer (أذكار بعد الصلاة)
4. Waking Up (دعاء الاستيقاظ)
5. Before Sleep (دعاء النوم)
6. Entering Home (دعاء دخول المنزل)
7. Leaving Home (دعاء الخروج من المنزل)
8. Entering Mosque (دعاء دخول المسجد)
9. Leaving Mosque (دعاء الخروج من المسجد)
10. Before Eating (دعاء قبل الطعام)
11. After Eating (دعاء بعد الطعام)
12. Traveling (دعاء السفر)
13. Rain (دعاء المطر)
14. Distress & Anxiety (دعاء الكرب)
15. Forgiveness (الاستغفار)

## duas.json Format

```json
[
  {
    "id": 1,
    "category_id": 1,
    "title_english": "Ayatul Kursi",
    "title_arabic": "آية الكرسي",
    "text_arabic": "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ...",
    "transliteration": "Allahu la ilaha illa Huwa, Al-Hayyul-Qayyum...",
    "translation": "Allah! There is no god but He, the Living, the Self-subsisting...",
    "source": "Quran 2:255",
    "virtue": "Whoever recites this when he rises in the morning will be protected...",
    "repeat_count": 1,
    "audio_file": null,
    "display_order": 1
  }
]
```

---

# 4. Islamic Events

## islamic_events.json Format

```json
[
  {
    "id": 1,
    "name_english": "Islamic New Year",
    "name_arabic": "رأس السنة الهجرية",
    "hijri_month": 1,
    "hijri_day": 1,
    "event_type": "HOLIDAY",
    "description": "The first day of the Islamic calendar.",
    "is_holiday": true
  }
]
```

**Event Types**: `HOLIDAY`, `FASTING`, `NIGHT`, `CELEBRATION`

**Required Events**:
| Event | Month | Day | Type |
|-------|-------|-----|------|
| Islamic New Year | 1 | 1 | HOLIDAY |
| Day of Ashura | 1 | 10 | FASTING |
| Mawlid an-Nabi | 3 | 12 | CELEBRATION |
| Isra and Mi'raj | 7 | 27 | CELEBRATION |
| Start of Ramadan | 9 | 1 | FASTING |
| Laylat al-Qadr | 9 | 27 | NIGHT |
| Eid al-Fitr | 10 | 1-3 | HOLIDAY |
| Day of Arafah | 12 | 9 | FASTING |
| Eid al-Adha | 12 | 10-13 | HOLIDAY |

---

# 5. Tasbih Presets

## tasbih_presets.json Format

```json
[
  {
    "id": 1,
    "name": "SubhanAllah",
    "arabic": "سُبْحَانَ اللَّهِ",
    "transliteration": "SubhanAllah",
    "translation": "Glory be to Allah",
    "target_count": 33,
    "is_custom": false,
    "display_order": 1
  }
]
```

**Required Presets**:
1. SubhanAllah (سُبْحَانَ اللَّهِ) - 33x
2. Alhamdulillah (الْحَمْدُ لِلَّهِ) - 33x
3. Allahu Akbar (اللَّهُ أَكْبَرُ) - 34x
4. La ilaha illallah (لَا إِلَٰهَ إِلَّا اللَّهُ) - 100x
5. Astaghfirullah (أَسْتَغْفِرُ اللَّهَ) - 100x
6. La hawla wa la quwwata illa billah - 100x
7. Salawat on Prophet ﷺ - 100x
8. SubhanAllahi wa bihamdihi - 100x

---

# 6. Database Generation Script

Create `scripts/generate_database.py`:

```python
#!/usr/bin/env python3
"""
Nimaz Pro - SQLite Database Generator
Converts JSON files to pre-populated Room database
"""

import sqlite3
import json
from pathlib import Path

JSON_DIR = Path("json")
OUTPUT_DB = Path("output/nimaz_prepopulated.db")
OUTPUT_DB.parent.mkdir(exist_ok=True)

def create_tables(conn):
    """Create all tables matching Room entity definitions"""
    cursor = conn.cursor()
    
    # Surahs
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
    
    # Ayahs
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
    
    # Translations
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS translations (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            ayah_id INTEGER NOT NULL,
            translator_id TEXT NOT NULL,
            text TEXT NOT NULL,
            FOREIGN KEY (ayah_id) REFERENCES ayahs(id)
        )
    ''')
    
    # Hadith Books
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
    
    # Hadiths
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
    
    # Dua Categories
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
    
    # Duas
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
    
    # Islamic Events
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
    
    # Tasbih Presets
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

def load_json(filename):
    """Load JSON file"""
    filepath = JSON_DIR / filename
    if filepath.exists():
        with open(filepath, 'r', encoding='utf-8') as f:
            return json.load(f)
    return []

def populate_database(conn):
    """Populate database from JSON files"""
    cursor = conn.cursor()
    
    # Surahs
    surahs = load_json('surahs.json')
    for s in surahs:
        cursor.execute('''
            INSERT OR REPLACE INTO surahs VALUES (?,?,?,?,?,?,?,?,?)
        ''', (s['id'], s['number'], s['name_arabic'], s['name_english'],
              s['name_transliteration'], s['revelation_type'], s['verses_count'],
              s['order_revealed'], s['start_page']))
    print(f"Inserted {len(surahs)} surahs")
    
    # Ayahs
    ayahs = load_json('ayahs.json')
    for a in ayahs:
        cursor.execute('''
            INSERT OR REPLACE INTO ayahs VALUES (?,?,?,?,?,?,?,?,?,?,?)
        ''', (a['id'], a['surah_id'], a['number_in_surah'], a['number_global'],
              a['text_arabic'], a['text_uthmani'], a['juz'], a['hizb'],
              a['page'], 1 if a.get('sajda') else 0, a.get('sajda_type')))
    print(f"Inserted {len(ayahs)} ayahs")
    
    # Translations
    translations = load_json('translations.json')
    for t in translations:
        cursor.execute('''
            INSERT INTO translations (ayah_id, translator_id, text) VALUES (?,?,?)
        ''', (t['ayah_id'], t['translator_id'], t['text']))
    print(f"Inserted {len(translations)} translations")
    
    # Hadith Books
    books = load_json('hadith_books.json')
    for b in books:
        cursor.execute('''
            INSERT OR REPLACE INTO hadith_books VALUES (?,?,?,?,?,?,?)
        ''', (b['id'], b['name_english'], b['name_arabic'], b['author'],
              b['hadith_count'], b['description'], b['icon']))
    print(f"Inserted {len(books)} hadith books")
    
    # Hadiths (all books)
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
        print(f"  Inserted {len(hadiths)} hadiths from {book_file}")
    print(f"Total hadiths: {total_hadiths}")
    
    # Dua Categories
    categories = load_json('dua_categories.json')
    for c in categories:
        cursor.execute('''
            INSERT OR REPLACE INTO dua_categories VALUES (?,?,?,?,?,?)
        ''', (c['id'], c['name_english'], c['name_arabic'], c['icon'],
              c['display_order'], c['dua_count']))
    print(f"Inserted {len(categories)} dua categories")
    
    # Duas
    duas = load_json('duas.json')
    for d in duas:
        cursor.execute('''
            INSERT OR REPLACE INTO duas VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
        ''', (d['id'], d['category_id'], d['title_english'], d['title_arabic'],
              d['text_arabic'], d['transliteration'], d['translation'],
              d['source'], d.get('virtue'), d['repeat_count'],
              d.get('audio_file'), d['display_order']))
    print(f"Inserted {len(duas)} duas")
    
    # Islamic Events
    events = load_json('islamic_events.json')
    for e in events:
        cursor.execute('''
            INSERT OR REPLACE INTO islamic_events VALUES (?,?,?,?,?,?,?,?)
        ''', (e['id'], e['name_english'], e['name_arabic'], e['hijri_month'],
              e['hijri_day'], e['event_type'], e['description'],
              1 if e.get('is_holiday') else 0))
    print(f"Inserted {len(events)} events")
    
    # Tasbih Presets
    presets = load_json('tasbih_presets.json')
    for p in presets:
        cursor.execute('''
            INSERT OR REPLACE INTO tasbih_presets VALUES (?,?,?,?,?,?,?,?)
        ''', (p.get('id'), p['name'], p['arabic'], p['transliteration'],
              p['translation'], p['target_count'], 1 if p.get('is_custom') else 0,
              p['display_order']))
    print(f"Inserted {len(presets)} tasbih presets")
    
    conn.commit()

def main():
    print("=" * 60)
    print("Nimaz Pro - Database Generator")
    print("=" * 60)
    
    # Remove existing database
    if OUTPUT_DB.exists():
        OUTPUT_DB.unlink()
    
    conn = sqlite3.connect(OUTPUT_DB)
    
    print("\nCreating tables...")
    create_tables(conn)
    
    print("\nPopulating database...")
    populate_database(conn)
    
    conn.close()
    
    print(f"\nDatabase created: {OUTPUT_DB}")
    print(f"Size: {OUTPUT_DB.stat().st_size / 1024 / 1024:.2f} MB")

if __name__ == "__main__":
    main()
```

---

# 7. Integration with Android App

## Step 1: Place Database in Assets

```
app/src/main/assets/database/nimaz_prepopulated.db
```

## Step 2: Update DatabaseModule.kt

The app's `DatabaseModule.kt` already configured to load from assets:

```kotlin
@Provides
@Singleton
fun provideDatabase(@ApplicationContext context: Context): NimazDatabase {
    return Room.databaseBuilder(
        context,
        NimazDatabase::class.java,
        "nimaz_database"
    )
    .createFromAsset("database/nimaz_prepopulated.db")
    .fallbackToDestructiveMigration()
    .build()
}
```

## Step 3: Verify Data

After app launch, query counts should match:
- Surahs: 114
- Ayahs: 6,236
- Hadith Books: 6
- Hadiths: ~34,000
- Dua Categories: 15
- Duas: 200+
- Events: 18+
- Tasbih Presets: 8+

---

# 8. Claude Code Prompt

Use this prompt to run data generation in parallel:

```
I need you to generate all Islamic content data for the Nimaz Pro Android app.

## Task
Create JSON files matching the exact formats specified in this document, then generate a pre-populated SQLite database.

## Data Sources to Use
1. Quran: Use quran-json GitHub or Tanzil.net
2. Hadith: Use Sunnah.com API (get free key) or hadith-json GitHub
3. Duas: Compile from Hisnul Muslim
4. Events: Use the provided list
5. Tasbih: Use the provided presets

## Output Requirements
1. Generate all JSON files in the exact formats shown
2. Run generate_database.py to create nimaz_prepopulated.db
3. Verify all data counts are correct
4. Output the database file ready for Android assets folder

## Quality Checks
- All Arabic text must be properly encoded UTF-8
- All IDs must be sequential and unique
- Foreign key relationships must be valid
- No null values in required fields

Start by creating the folder structure and downloading Quran data first.
```

---

# Summary

| Data Type | Records | Source | Format |
|-----------|---------|--------|--------|
| Surahs | 114 | Tanzil/quran-json | surahs.json |
| Ayahs | 6,236 | Tanzil/quran-json | ayahs.json |
| Translations | 6,236 | Quran.com API | translations.json |
| Hadith Books | 6 | Manual | hadith_books.json |
| Hadiths | ~34,000 | Sunnah.com/GitHub | hadith_*.json |
| Dua Categories | 15 | Manual | dua_categories.json |
| Duas | 200+ | Hisnul Muslim | duas.json |
| Events | 18+ | Manual | islamic_events.json |
| Tasbih Presets | 8+ | Manual | tasbih_presets.json |

**Final Output**: `nimaz_prepopulated.db` (~15-25 MB)
