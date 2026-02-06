#!/usr/bin/env python3
"""
Nimaz Pro - SQLite Database Generator
Converts JSON files to pre-populated Room database
"""

import sqlite3
import json
from pathlib import Path
from preparse_tajweed import preparse_single

JSON_DIR = Path(__file__).parent.parent / "json"
OUTPUT_DB = Path(__file__).parent.parent / "output" / "nimaz_prepopulated.db"
OUTPUT_DB.parent.mkdir(exist_ok=True)

def create_tables(conn):
    """Create all tables matching Room entity definitions"""
    cursor = conn.cursor()

    # Surahs
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS surahs (
            id INTEGER NOT NULL PRIMARY KEY,
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
            id INTEGER NOT NULL PRIMARY KEY,
            surah_id INTEGER NOT NULL,
            number_in_surah INTEGER NOT NULL,
            number_global INTEGER NOT NULL,
            text_arabic TEXT NOT NULL,
            text_uthmani TEXT NOT NULL,
            juz INTEGER NOT NULL,
            hizb INTEGER NOT NULL,
            page INTEGER NOT NULL,
            sajda INTEGER NOT NULL,
            sajda_type TEXT,
            transliteration TEXT,
            text_tajweed TEXT,
            FOREIGN KEY (surah_id) REFERENCES surahs(id) ON DELETE CASCADE
        )
    ''')

    # Translations
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS translations (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            ayah_id INTEGER NOT NULL,
            text TEXT NOT NULL,
            translator_id TEXT NOT NULL,
            FOREIGN KEY (ayah_id) REFERENCES ayahs(id) ON DELETE CASCADE
        )
    ''')

    # Hadith Books
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS hadith_books (
            id INTEGER NOT NULL PRIMARY KEY,
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
            id INTEGER NOT NULL PRIMARY KEY,
            book_id INTEGER NOT NULL,
            chapter_id INTEGER NOT NULL,
            number_in_book INTEGER NOT NULL,
            number_in_chapter INTEGER NOT NULL,
            text_arabic TEXT NOT NULL,
            text_english TEXT NOT NULL,
            narrator TEXT NOT NULL,
            grade TEXT NOT NULL,
            reference TEXT NOT NULL,
            FOREIGN KEY (book_id) REFERENCES hadith_books(id) ON DELETE CASCADE
        )
    ''')

    # Dua Categories
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS dua_categories (
            id INTEGER NOT NULL PRIMARY KEY,
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
            id INTEGER NOT NULL PRIMARY KEY,
            category_id INTEGER NOT NULL,
            title_english TEXT NOT NULL,
            title_arabic TEXT NOT NULL,
            text_arabic TEXT NOT NULL,
            transliteration TEXT NOT NULL,
            translation TEXT NOT NULL,
            source TEXT NOT NULL,
            virtue TEXT,
            repeat_count INTEGER NOT NULL,
            audio_file TEXT,
            display_order INTEGER NOT NULL,
            FOREIGN KEY (category_id) REFERENCES dua_categories(id) ON DELETE CASCADE
        )
    ''')

    # Islamic Events
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS islamic_events (
            id INTEGER NOT NULL PRIMARY KEY,
            name_english TEXT NOT NULL,
            name_arabic TEXT NOT NULL,
            hijri_month INTEGER NOT NULL,
            hijri_day INTEGER NOT NULL,
            event_type TEXT NOT NULL,
            description TEXT NOT NULL,
            is_holiday INTEGER NOT NULL
        )
    ''')

    # Tasbih Presets
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS tasbih_presets (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            arabic TEXT NOT NULL,
            transliteration TEXT NOT NULL,
            translation TEXT NOT NULL,
            target_count INTEGER NOT NULL,
            is_custom INTEGER NOT NULL,
            display_order INTEGER NOT NULL
        )
    ''')

    # Surah Info
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS surah_info (
            surahNumber INTEGER NOT NULL PRIMARY KEY,
            description TEXT NOT NULL,
            themes TEXT NOT NULL
        )
    ''')

    # Reading Progress
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS reading_progress (
            id INTEGER NOT NULL PRIMARY KEY,
            lastReadSurah INTEGER NOT NULL,
            lastReadAyah INTEGER NOT NULL,
            lastReadPage INTEGER NOT NULL,
            lastReadJuz INTEGER NOT NULL,
            totalAyahsRead INTEGER NOT NULL,
            currentKhatmaCount INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
        )
    ''')

    # Quran Bookmarks
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS quran_bookmarks (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            ayahId INTEGER NOT NULL,
            surahNumber INTEGER NOT NULL,
            ayahNumber INTEGER NOT NULL,
            note TEXT,
            color TEXT,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
        )
    ''')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_quran_bookmarks_ayahId ON quran_bookmarks(ayahId)')

    # Quran Favorites
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS quran_favorites (
            ayahId INTEGER NOT NULL PRIMARY KEY,
            surahNumber INTEGER NOT NULL,
            ayahNumber INTEGER NOT NULL,
            createdAt INTEGER NOT NULL
        )
    ''')

    # Hadith Bookmarks
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS hadith_bookmarks (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            hadithId INTEGER NOT NULL,
            bookId INTEGER NOT NULL,
            hadithNumber INTEGER NOT NULL,
            note TEXT,
            color TEXT,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
        )
    ''')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_hadith_bookmarks_hadithId ON hadith_bookmarks(hadithId)')

    # Dua Bookmarks
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS dua_bookmarks (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            duaId INTEGER NOT NULL,
            categoryId INTEGER NOT NULL,
            note TEXT,
            isFavorite INTEGER NOT NULL,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
        )
    ''')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_dua_bookmarks_duaId ON dua_bookmarks(duaId)')

    # Dua Progress
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS dua_progress (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            duaId INTEGER NOT NULL,
            date INTEGER NOT NULL,
            completedCount INTEGER NOT NULL,
            targetCount INTEGER NOT NULL,
            isCompleted INTEGER NOT NULL,
            createdAt INTEGER NOT NULL
        )
    ''')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_dua_progress_duaId ON dua_progress(duaId)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_dua_progress_date ON dua_progress(date)')

    # Prayer Records
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS prayer_records (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            date INTEGER NOT NULL,
            prayerName TEXT NOT NULL,
            status TEXT NOT NULL,
            prayedAt INTEGER,
            scheduledTime INTEGER NOT NULL,
            isJamaah INTEGER NOT NULL,
            isQadaFor INTEGER,
            note TEXT,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
        )
    ''')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_prayer_records_date ON prayer_records(date)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_prayer_records_prayerName ON prayer_records(prayerName)')
    cursor.execute('CREATE UNIQUE INDEX IF NOT EXISTS index_prayer_records_date_prayerName ON prayer_records(date, prayerName)')

    # Fast Records
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS fast_records (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            date INTEGER NOT NULL,
            hijriDate TEXT,
            hijriMonth INTEGER,
            hijriYear INTEGER,
            fastType TEXT NOT NULL,
            status TEXT NOT NULL,
            exemptionReason TEXT,
            suhoorTime INTEGER,
            iftarTime INTEGER,
            note TEXT,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
        )
    ''')
    cursor.execute('CREATE UNIQUE INDEX IF NOT EXISTS index_fast_records_date ON fast_records(date)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_fast_records_hijriMonth ON fast_records(hijriMonth)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_fast_records_fastType ON fast_records(fastType)')

    # Makeup Fasts
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS makeup_fasts (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            originalDate INTEGER NOT NULL,
            originalHijriDate TEXT,
            reason TEXT NOT NULL,
            status TEXT NOT NULL,
            completedDate INTEGER,
            fidyaAmount REAL,
            note TEXT,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
        )
    ''')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_makeup_fasts_originalDate ON makeup_fasts(originalDate)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_makeup_fasts_status ON makeup_fasts(status)')

    # Khatams (Quran completion tracking)
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS khatams (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            notes TEXT,
            status TEXT NOT NULL DEFAULT 'active',
            is_active INTEGER NOT NULL DEFAULT 0,
            daily_target INTEGER NOT NULL DEFAULT 20,
            deadline INTEGER,
            reminder_enabled INTEGER NOT NULL DEFAULT 0,
            reminder_time TEXT,
            total_ayahs_read INTEGER NOT NULL DEFAULT 0,
            created_at INTEGER NOT NULL,
            started_at INTEGER,
            completed_at INTEGER,
            updated_at INTEGER NOT NULL
        )
    ''')

    # Khatam Ayahs
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS khatam_ayahs (
            khatam_id INTEGER NOT NULL,
            ayah_id INTEGER NOT NULL,
            read_at INTEGER NOT NULL,
            PRIMARY KEY(khatam_id, ayah_id),
            FOREIGN KEY(khatam_id) REFERENCES khatams(id) ON DELETE CASCADE
        )
    ''')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_khatam_ayahs_khatam_id ON khatam_ayahs(khatam_id)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_khatam_ayahs_ayah_id ON khatam_ayahs(ayah_id)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_khatam_ayahs_read_at ON khatam_ayahs(read_at)')

    # Khatam Daily Log
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS khatam_daily_log (
            khatam_id INTEGER NOT NULL,
            date INTEGER NOT NULL,
            ayahs_read INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(khatam_id, date),
            FOREIGN KEY(khatam_id) REFERENCES khatams(id) ON DELETE CASCADE
        )
    ''')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_khatam_daily_log_khatam_id ON khatam_daily_log(khatam_id)')

    # Tasbih Sessions
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS tasbih_sessions (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            presetId INTEGER,
            presetName TEXT,
            date INTEGER NOT NULL,
            currentCount INTEGER NOT NULL,
            targetCount INTEGER NOT NULL,
            totalLaps INTEGER NOT NULL,
            isCompleted INTEGER NOT NULL,
            duration INTEGER,
            startedAt INTEGER NOT NULL,
            completedAt INTEGER,
            note TEXT
        )
    ''')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_tasbih_sessions_presetId ON tasbih_sessions(presetId)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_tasbih_sessions_date ON tasbih_sessions(date)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_tasbih_sessions_isCompleted ON tasbih_sessions(isCompleted)')

    # Zakat History
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS zakat_history (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            calculatedAt INTEGER NOT NULL,
            totalAssets REAL NOT NULL,
            totalLiabilities REAL NOT NULL,
            netWorth REAL NOT NULL,
            zakatDue REAL NOT NULL,
            nisabType TEXT NOT NULL,
            nisabValue REAL NOT NULL,
            isPaid INTEGER NOT NULL,
            paidAt INTEGER,
            notes TEXT
        )
    ''')

    # Tafseer Texts (pre-populated content)
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS tafseer_texts (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            ayah_id INTEGER NOT NULL,
            surah_number INTEGER NOT NULL,
            ayah_number INTEGER NOT NULL,
            tafseer_id TEXT NOT NULL,
            text TEXT NOT NULL,
            FOREIGN KEY (ayah_id) REFERENCES ayahs(id) ON DELETE CASCADE
        )
    ''')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_tafseer_texts_ayah_id ON tafseer_texts(ayah_id)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_tafseer_texts_tafseer_id ON tafseer_texts(tafseer_id)')
    cursor.execute('CREATE UNIQUE INDEX IF NOT EXISTS index_tafseer_texts_ayah_tafseer ON tafseer_texts(ayah_id, tafseer_id)')

    # Tafseer Highlights (user data)
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS tafseer_highlights (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            ayah_id INTEGER NOT NULL,
            tafseer_id TEXT NOT NULL,
            start_offset INTEGER NOT NULL,
            end_offset INTEGER NOT NULL,
            color TEXT NOT NULL,
            note TEXT,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
    ''')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_tafseer_highlights_ayah_tafseer ON tafseer_highlights(ayah_id, tafseer_id)')

    # Tafseer Notes (user data)
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS tafseer_notes (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            ayah_id INTEGER NOT NULL,
            tafseer_id TEXT NOT NULL,
            text TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL
        )
    ''')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_tafseer_notes_ayah_tafseer ON tafseer_notes(ayah_id, tafseer_id)')

    # Locations
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS locations (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            latitude REAL NOT NULL,
            longitude REAL NOT NULL,
            timezone TEXT NOT NULL,
            country TEXT,
            city TEXT,
            isCurrentLocation INTEGER NOT NULL,
            isFavorite INTEGER NOT NULL,
            calculationMethod TEXT,
            asrCalculation TEXT,
            highLatitudeRule TEXT,
            fajrAngle REAL,
            ishaAngle REAL,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
        )
    ''')

    # Asma ul Husna (99 Names of Allah)
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS asma_ul_husna (
            id INTEGER NOT NULL PRIMARY KEY,
            number INTEGER NOT NULL,
            name_arabic TEXT NOT NULL,
            name_transliteration TEXT NOT NULL,
            name_english TEXT NOT NULL,
            meaning TEXT NOT NULL,
            explanation TEXT NOT NULL,
            benefits TEXT NOT NULL,
            quran_references TEXT NOT NULL,
            usage_in_dua TEXT NOT NULL,
            display_order INTEGER NOT NULL
        )
    ''')

    # Asma ul Husna Bookmarks (user data)
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS asma_ul_husna_bookmarks (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name_id INTEGER NOT NULL,
            is_favorite INTEGER NOT NULL DEFAULT 1,
            created_at INTEGER NOT NULL
        )
    ''')
    cursor.execute('CREATE UNIQUE INDEX IF NOT EXISTS index_asma_ul_husna_bookmarks_name_id ON asma_ul_husna_bookmarks(name_id)')

    # Asma un Nabi (99 Names of Prophet Muhammad)
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS asma_un_nabi (
            id INTEGER NOT NULL PRIMARY KEY,
            number INTEGER NOT NULL,
            name_arabic TEXT NOT NULL,
            name_transliteration TEXT NOT NULL,
            name_english TEXT NOT NULL,
            meaning TEXT NOT NULL,
            explanation TEXT NOT NULL,
            source TEXT NOT NULL,
            display_order INTEGER NOT NULL
        )
    ''')

    # Asma un Nabi Bookmarks (user data)
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS asma_un_nabi_bookmarks (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name_id INTEGER NOT NULL,
            is_favorite INTEGER NOT NULL DEFAULT 1,
            created_at INTEGER NOT NULL
        )
    ''')
    cursor.execute('CREATE UNIQUE INDEX IF NOT EXISTS index_asma_un_nabi_bookmarks_name_id ON asma_un_nabi_bookmarks(name_id)')

    # Prophets
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS prophets (
            id INTEGER NOT NULL PRIMARY KEY,
            number INTEGER NOT NULL,
            name_arabic TEXT NOT NULL,
            name_english TEXT NOT NULL,
            name_transliteration TEXT NOT NULL,
            title_arabic TEXT NOT NULL,
            title_english TEXT NOT NULL,
            story_summary TEXT NOT NULL,
            key_lessons TEXT NOT NULL,
            quran_mentions TEXT NOT NULL,
            era TEXT NOT NULL,
            lineage TEXT NOT NULL,
            years_lived TEXT NOT NULL,
            place_of_preaching TEXT NOT NULL,
            miracles TEXT NOT NULL,
            display_order INTEGER NOT NULL
        )
    ''')

    # Prophet Bookmarks (user data)
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS prophet_bookmarks (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            prophet_id INTEGER NOT NULL,
            is_favorite INTEGER NOT NULL DEFAULT 1,
            created_at INTEGER NOT NULL
        )
    ''')
    cursor.execute('CREATE UNIQUE INDEX IF NOT EXISTS index_prophet_bookmarks_prophet_id ON prophet_bookmarks(prophet_id)')

    # Create indices
    cursor.execute('CREATE INDEX IF NOT EXISTS index_ayahs_surah_id ON ayahs(surah_id)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_ayahs_juz ON ayahs(juz)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_ayahs_page ON ayahs(page)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_translations_ayah_id ON translations(ayah_id)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_hadiths_book_id ON hadiths(book_id)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_hadiths_chapter_id ON hadiths(chapter_id)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_duas_category_id ON duas(category_id)')
    cursor.execute('CREATE INDEX IF NOT EXISTS index_islamic_events_hijri_month_hijri_day ON islamic_events(hijri_month, hijri_day)')

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

    # Load transliteration data
    transliterations = load_json('transliteration.json')
    if transliterations:
        print(f"Loaded transliteration data for {len(transliterations)} ayahs")
    else:
        print("Warning: No transliteration data found")

    # Load tajweed data and pre-parse it
    tajweed_data = load_json('tajweed.json')
    if tajweed_data:
        print(f"Loaded tajweed data for {len(tajweed_data)} ayahs")
        print("Pre-parsing tajweed data to JSON format...")
    else:
        print("Warning: No tajweed data found")

    # Ayahs (with transliteration and tajweed)
    ayahs = load_json('ayahs.json')
    for a in ayahs:
        # Get transliteration from transliteration.json using number_global
        # The JSON uses string keys, so convert to string
        transliteration = transliterations.get(str(a['number_global'])) if transliterations else None

        # Get tajweed text using "surah:ayah" key format and pre-parse it
        tajweed_key = f"{a['surah_id']}:{a['number_in_surah']}"
        raw_tajweed = tajweed_data.get(tajweed_key) if tajweed_data else None
        # Pre-parse HTML tajweed to JSON format for efficient rendering
        text_tajweed = preparse_single(raw_tajweed) if raw_tajweed else None

        cursor.execute('''
            INSERT OR REPLACE INTO ayahs VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
        ''', (a['id'], a['surah_id'], a['number_in_surah'], a['number_global'],
              a['text_arabic'], a['text_uthmani'], a['juz'], a['hizb'],
              a['page'], 1 if a.get('sajda') else 0, a.get('sajda_type'),
              transliteration, text_tajweed))
    print(f"Inserted {len(ayahs)} ayahs")

    # Surah Info
    surah_info = load_json('surah_info.json')
    if surah_info:
        for si in surah_info:
            cursor.execute('''
                INSERT OR REPLACE INTO surah_info VALUES (?,?,?)
            ''', (si['surahNumber'], si['description'], si['themes']))
        print(f"Inserted {len(surah_info)} surah info entries")

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

    # Tafseer - Ibn Kathir
    tafseer_ibn_kathir = load_json('tafseer_ibn_kathir.json')
    if tafseer_ibn_kathir:
        for t in tafseer_ibn_kathir:
            cursor.execute('''
                INSERT INTO tafseer_texts (ayah_id, surah_number, ayah_number, tafseer_id, text)
                VALUES (?,?,?,?,?)
            ''', (t['ayah_id'], t['surah_number'], t['ayah_number'], 'ibn_kathir_en', t['text']))
        print(f"Inserted {len(tafseer_ibn_kathir)} Ibn Kathir tafseer entries")
    else:
        print("Warning: No Ibn Kathir tafseer data found")

    # Tafseer - Ma'arif al-Qur'an
    tafseer_maariful = load_json('tafseer_maariful_quran.json')
    if tafseer_maariful:
        for t in tafseer_maariful:
            cursor.execute('''
                INSERT INTO tafseer_texts (ayah_id, surah_number, ayah_number, tafseer_id, text)
                VALUES (?,?,?,?,?)
            ''', (t['ayah_id'], t['surah_number'], t['ayah_number'], 'maariful_quran_en', t['text']))
        print(f"Inserted {len(tafseer_maariful)} Ma'arif al-Qur'an tafseer entries")
    else:
        print("Warning: No Ma'arif al-Qur'an tafseer data found")

    # Asma ul Husna (99 Names of Allah)
    asma_ul_husna = load_json('asma_ul_husna.json')
    if asma_ul_husna:
        for a in asma_ul_husna:
            quran_refs = json.dumps(a.get('quran_references', []))
            cursor.execute('''
                INSERT OR REPLACE INTO asma_ul_husna VALUES (?,?,?,?,?,?,?,?,?,?,?)
            ''', (a['id'], a['number'], a['name_arabic'], a['name_transliteration'],
                  a['name_english'], a['meaning'], a['explanation'], a['benefits'],
                  quran_refs, a['usage_in_dua'], a['display_order']))
        print(f"Inserted {len(asma_ul_husna)} Asma ul Husna entries")
    else:
        print("Warning: No Asma ul Husna data found")

    # Asma un Nabi (99 Names of Prophet Muhammad)
    asma_un_nabi = load_json('asma_un_nabi.json')
    if asma_un_nabi:
        for a in asma_un_nabi:
            cursor.execute('''
                INSERT OR REPLACE INTO asma_un_nabi VALUES (?,?,?,?,?,?,?,?,?)
            ''', (a['id'], a['number'], a['name_arabic'], a['name_transliteration'],
                  a['name_english'], a['meaning'], a['explanation'], a['source'],
                  a['display_order']))
        print(f"Inserted {len(asma_un_nabi)} Asma un Nabi entries")
    else:
        print("Warning: No Asma un Nabi data found")

    # Prophets
    prophets = load_json('prophets.json')
    if prophets:
        for p in prophets:
            key_lessons = json.dumps(p.get('key_lessons', []))
            quran_mentions = json.dumps(p.get('quran_mentions', []))
            miracles = json.dumps(p.get('miracles', []))
            cursor.execute('''
                INSERT OR REPLACE INTO prophets VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ''', (p['id'], p['number'], p['name_arabic'], p['name_english'],
                  p['name_transliteration'], p['title_arabic'], p['title_english'],
                  p['story_summary'], key_lessons, quran_mentions, p['era'],
                  p['lineage'], p['years_lived'], p['place_of_preaching'],
                  miracles, p['display_order']))
        print(f"Inserted {len(prophets)} prophet entries")
    else:
        print("Warning: No prophets data found")

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

    # Set Room database version so migrations are skipped
    conn.execute("PRAGMA user_version = 10")
    conn.commit()
    print("\nSet user_version = 10 (Room schema version)")

    conn.close()

    print(f"\nDatabase created: {OUTPUT_DB}")
    print(f"Size: {OUTPUT_DB.stat().st_size / 1024 / 1024:.2f} MB")

if __name__ == "__main__":
    main()
