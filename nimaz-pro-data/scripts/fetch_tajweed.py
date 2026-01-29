#!/usr/bin/env python3
"""
Nimaz Pro - Tajweed Data Fetch Script

Downloads tajweed-annotated Quran text from Quran.com API and updates the database.
The tajweed text contains HTML-like tags indicating pronunciation rules:
<tajweed class="ghunnah">نّ</tajweed>

API Endpoint: https://api.quran.com/api/v4/quran/verses/uthmani_tajweed
"""

import json
import os
import sqlite3
import urllib.request
import urllib.error
import ssl
import time
from pathlib import Path
from typing import Dict, List, Any, Optional

# Disable SSL verification (for development)
ssl._create_default_https_context = ssl._create_unverified_context

BASE_DIR = Path(__file__).parent.parent
JSON_DIR = BASE_DIR / "json"
DATABASE_PATH = BASE_DIR / "output" / "nimaz_prepopulated.db"

# Quran.com API base URL
QURAN_API_BASE = "https://api.quran.com/api/v4"


def download_json(url: str, retries: int = 3) -> Optional[Any]:
    """Download and parse JSON from URL with retries"""
    for attempt in range(retries):
        try:
            print(f"    Downloading: {url}")
            req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=60) as response:
                return json.loads(response.read().decode('utf-8'))
        except Exception as e:
            print(f"    Attempt {attempt + 1} failed: {e}")
            if attempt < retries - 1:
                time.sleep(2)
    return None


def fetch_tajweed_by_page(page: int) -> Optional[List[Dict]]:
    """
    Fetch tajweed text for a specific Quran page.

    Args:
        page: Quran page number (1-604)

    Returns:
        List of verse data with tajweed text, or None if failed
    """
    url = f"{QURAN_API_BASE}/quran/verses/uthmani_tajweed?page_number={page}"
    data = download_json(url)

    if data and "verses" in data:
        return data["verses"]
    return None


def fetch_all_tajweed_data() -> Dict[int, str]:
    """
    Fetch tajweed text for all ayahs in the Quran.

    Returns:
        Dictionary mapping ayah global ID to tajweed text
    """
    print("\n" + "=" * 60)
    print("FETCHING TAJWEED DATA FROM QURAN.COM API")
    print("=" * 60)

    tajweed_data: Dict[int, str] = {}
    total_pages = 604

    for page in range(1, total_pages + 1):
        print(f"\n  Page {page}/{total_pages}")

        verses = fetch_tajweed_by_page(page)

        if verses:
            for verse in verses:
                # verse_key format is "surah:ayah" e.g., "1:1"
                verse_key = verse.get("verse_key", "")
                text_uthmani_tajweed = verse.get("text_uthmani_tajweed", "")

                if verse_key and text_uthmani_tajweed:
                    parts = verse_key.split(":")
                    if len(parts) == 2:
                        surah_num = int(parts[0])
                        ayah_num = int(parts[1])

                        # Calculate global ayah ID
                        # This needs to match the ID calculation used in the database
                        # For now, we'll store by surah:ayah and map later
                        key = f"{surah_num}:{ayah_num}"
                        tajweed_data[key] = text_uthmani_tajweed

            print(f"    Fetched {len(verses)} verses")
        else:
            print(f"    ERROR: Failed to fetch page {page}")

        # Rate limiting - be nice to the API
        time.sleep(0.5)

    print(f"\nTotal tajweed verses fetched: {len(tajweed_data)}")
    return tajweed_data


def save_tajweed_json(tajweed_data: Dict[str, str]):
    """Save tajweed data to JSON file"""
    JSON_DIR.mkdir(parents=True, exist_ok=True)
    filepath = JSON_DIR / "tajweed.json"

    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(tajweed_data, f, ensure_ascii=False, indent=2)

    print(f"\nSaved tajweed data to: {filepath}")


def update_database(tajweed_data: Dict[str, str]):
    """
    Update the SQLite database with tajweed text.

    Args:
        tajweed_data: Dictionary mapping "surah:ayah" to tajweed text
    """
    print("\n" + "=" * 60)
    print("UPDATING DATABASE WITH TAJWEED DATA")
    print("=" * 60)

    if not DATABASE_PATH.exists():
        print(f"ERROR: Database not found at {DATABASE_PATH}")
        print("Please run generate_database.py first.")
        return

    conn = sqlite3.connect(DATABASE_PATH)
    cursor = conn.cursor()

    # Check if text_tajweed column exists, add if not
    cursor.execute("PRAGMA table_info(ayahs)")
    columns = [col[1] for col in cursor.fetchall()]

    if "text_tajweed" not in columns:
        print("Adding text_tajweed column to ayahs table...")
        cursor.execute("ALTER TABLE ayahs ADD COLUMN text_tajweed TEXT")
        conn.commit()

    # Update ayahs with tajweed text
    updated_count = 0

    # Get all ayahs from database
    # surah_id equals surah number in this database
    cursor.execute("""
        SELECT id, surah_id as surah_number, number_in_surah
        FROM ayahs
    """)

    ayahs = cursor.fetchall()

    for ayah_id, surah_num, ayah_num in ayahs:
        key = f"{surah_num}:{ayah_num}"

        if key in tajweed_data:
            cursor.execute(
                "UPDATE ayahs SET text_tajweed = ? WHERE id = ?",
                (tajweed_data[key], ayah_id)
            )
            updated_count += 1

    conn.commit()
    conn.close()

    print(f"\nUpdated {updated_count} ayahs with tajweed text")


def main():
    """Main entry point"""
    print("=" * 60)
    print("NIMAZ PRO - TAJWEED DATA FETCH SCRIPT")
    print("=" * 60)

    # Check if we already have tajweed data cached
    cached_file = JSON_DIR / "tajweed.json"

    if cached_file.exists():
        print(f"\nFound cached tajweed data at: {cached_file}")
        user_input = input("Use cached data? (y/n): ").strip().lower()

        if user_input == 'y':
            with open(cached_file, 'r', encoding='utf-8') as f:
                tajweed_data = json.load(f)
            print(f"Loaded {len(tajweed_data)} tajweed entries from cache")
        else:
            tajweed_data = fetch_all_tajweed_data()
            save_tajweed_json(tajweed_data)
    else:
        tajweed_data = fetch_all_tajweed_data()
        save_tajweed_json(tajweed_data)

    print("\n" + "=" * 60)
    print("TAJWEED DATA FETCH COMPLETE!")
    print("=" * 60)
    print("\nRun generate_database.py to regenerate the database with tajweed data.")


if __name__ == "__main__":
    main()
