#!/usr/bin/env python3
"""
Download transliteration data from Al Quran Cloud API
API: https://api.alquran.cloud/v1/quran/en.transliteration
"""
import json
import urllib.request
from pathlib import Path

API_URL = "https://api.alquran.cloud/v1/quran/en.transliteration"
OUTPUT_FILE = Path(__file__).parent.parent / "json" / "transliteration.json"

def download_transliteration():
    print(f"Downloading transliteration data from {API_URL}...")

    try:
        with urllib.request.urlopen(API_URL) as response:
            data = json.loads(response.read().decode())

        if data['code'] != 200:
            print(f"Error: API returned code {data['code']}")
            return False

        # Extract transliteration mapping: ayah_global_id -> transliteration text
        transliteration_map = {}
        ayah_count = 0

        for surah in data['data']['surahs']:
            for ayah in surah['ayahs']:
                # Use numberInSurah as global ayah number (1-6236)
                # The API uses 'number' field which is the global ayah number
                global_id = ayah['number']
                transliteration = ayah['text']
                transliteration_map[global_id] = transliteration
                ayah_count += 1

        print(f"Downloaded transliteration for {ayah_count} ayahs")

        # Save to JSON file
        OUTPUT_FILE.parent.mkdir(exist_ok=True, parents=True)
        with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
            json.dump(transliteration_map, f, ensure_ascii=False, indent=2)

        print(f"Saved to {OUTPUT_FILE}")
        return True

    except Exception as e:
        print(f"Error downloading transliteration data: {e}")
        return False

if __name__ == "__main__":
    success = download_transliteration()
    if success:
        print("\nTransliteration data downloaded successfully!")
    else:
        print("\nFailed to download transliteration data")
        exit(1)
