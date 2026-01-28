#!/usr/bin/env python3
"""
Verify the generated database has all required tables and columns
"""
import sqlite3
from pathlib import Path

DB_FILE = Path(__file__).parent.parent / "output" / "nimaz_prepopulated.db"

def verify_database():
    conn = sqlite3.connect(DB_FILE)
    cursor = conn.cursor()

    # Get all tables
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")
    tables = [row[0] for row in cursor.fetchall()]

    print("=" * 60)
    print("Database Tables")
    print("=" * 60)
    for table in tables:
        print(f"  [OK] {table}")

    print(f"\nTotal: {len(tables)} tables")

    # Expected Room entity tables (22 total)
    expected_tables = [
        'surahs', 'ayahs', 'translations', 'quran_bookmarks', 'quran_favorites',
        'reading_progress', 'surah_info',  # Quran (7)
        'hadith_books', 'hadiths', 'hadith_bookmarks',  # Hadith (3)
        'dua_categories', 'duas', 'dua_bookmarks', 'dua_progress',  # Duas (4)
        'prayer_records', 'fast_records', 'makeup_fasts',  # Prayer/Fasting (3)
        'tasbih_presets', 'tasbih_sessions',  # Tasbih (2)
        'zakat_history',  # Zakat (1)
        'locations', 'islamic_events'  # Other (2)
    ]

    missing = set(expected_tables) - set(tables)
    if missing:
        print(f"\n[WARN] Missing tables: {missing}")
    else:
        print("\n[OK] All 22 entity tables present!")

    # Check ayahs table has transliteration column
    cursor.execute("PRAGMA table_info(ayahs)")
    ayah_columns = [row[1] for row in cursor.fetchall()]
    if 'transliteration' in ayah_columns:
        print("[OK] Ayahs table has transliteration column")
    else:
        print("[WARN] Ayahs table missing transliteration column")

    # Check ayahs table has data with transliteration
    cursor.execute("SELECT COUNT(*) FROM ayahs WHERE transliteration IS NOT NULL")
    transliteration_count = cursor.fetchone()[0]
    print(f"[OK] {transliteration_count} ayahs have transliteration data")

    # Check surah_info has data
    cursor.execute("SELECT COUNT(*) FROM surah_info")
    surah_info_count = cursor.fetchone()[0]
    print(f"[OK] {surah_info_count} surah info entries")

    # Check indices
    print("\n" + "=" * 60)
    print("Database Indices")
    print("=" * 60)
    cursor.execute("SELECT name FROM sqlite_master WHERE type='index' ORDER BY name")
    indices = [row[0] for row in cursor.fetchall()]
    for idx in indices:
        if not idx.startswith('sqlite_'):  # Skip auto-generated indices
            print(f"  [OK] {idx}")

    print(f"\nTotal: {len([i for i in indices if not i.startswith('sqlite_')])} custom indices")

    # Check specific required indices
    required_indices = [
        'index_ayahs_page',
        'index_hadiths_chapter_id',
        'index_islamic_events_hijri_month_hijri_day'
    ]

    print("\nChecking required indices:")
    for idx in required_indices:
        if idx in indices:
            print(f"  [OK] {idx}")
        else:
            print(f"  [WARN] Missing: {idx}")

    conn.close()

    print("\n" + "=" * 60)
    print("Verification Complete")
    print("=" * 60)

if __name__ == "__main__":
    verify_database()
