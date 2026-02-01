#!/usr/bin/env python3
"""
Download tafseer data from quran.com API (v4).
Tafseers:
  - Ibn Kathir (Abridged) English: en-tafisr-ibn-kathir
  - Ma'arif al-Qur'an English: en-tafsir-maarif-ul-quran
"""

import json
import re
import time
import urllib.request
import urllib.error
from html.parser import HTMLParser
from pathlib import Path

JSON_DIR = Path(__file__).parent.parent / "json"
JSON_DIR.mkdir(exist_ok=True)

# Surah ayah counts (1-indexed by surah number)
SURAH_AYAH_COUNTS = [
    0,  # placeholder for index 0
    7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
    123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
    112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
    34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
    54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
    60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
    14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
    28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
    29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
    15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
    11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
    5, 4, 5, 6
]

API_BASE = "https://api.quran.com/api/v4/tafsirs"

TAFSEERS = [
    ("en-tafisr-ibn-kathir", "Ibn Kathir", "tafseer_ibn_kathir.json"),
    ("en-tafsir-maarif-ul-quran", "Ma'arif al-Qur'an", "tafseer_maariful_quran.json"),
]


class HTMLStripper(HTMLParser):
    """Strip HTML tags and convert to plain text with paragraph breaks."""

    def __init__(self):
        super().__init__()
        self.result = []
        self._in_block = False

    def handle_starttag(self, tag, attrs):
        if tag in ('p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'div', 'br', 'li'):
            if self.result and self.result[-1] != '\n':
                self.result.append('\n')
            self._in_block = True

    def handle_endtag(self, tag):
        if tag in ('p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'div', 'li'):
            self.result.append('\n')
            self._in_block = False

    def handle_data(self, data):
        self.result.append(data)

    def get_text(self):
        text = ''.join(self.result)
        # Collapse multiple newlines into double newline (paragraph break)
        text = re.sub(r'\n{3,}', '\n\n', text)
        return text.strip()


def strip_html(html_text):
    """Convert HTML to clean plain text."""
    if not html_text:
        return ""
    stripper = HTMLStripper()
    stripper.feed(html_text)
    return stripper.get_text()


def fetch_url(url, retries=3):
    """Fetch a URL with retries and return parsed JSON."""
    for attempt in range(retries):
        try:
            req = urllib.request.Request(url)
            req.add_header('User-Agent', 'NimazPro/1.0')
            req.add_header('Accept', 'application/json')
            with urllib.request.urlopen(req, timeout=30) as resp:
                return json.loads(resp.read().decode('utf-8'))
        except (urllib.error.URLError, urllib.error.HTTPError, Exception) as e:
            if attempt < retries - 1:
                wait = 2 ** (attempt + 1)
                print(f"    Retry {attempt + 1}/{retries} after error: {e} (waiting {wait}s)")
                time.sleep(wait)
            else:
                raise


def fetch_all_pages(base_url):
    """Fetch all pages from a paginated quran.com API endpoint."""
    all_tafsirs = []
    page = 1
    while True:
        url = f"{base_url}?page={page}"
        data = fetch_url(url)
        tafsirs = data.get("tafsirs", [])
        all_tafsirs.extend(tafsirs)
        pagination = data.get("pagination", {})
        total_pages = pagination.get("total_pages", 1)
        if page >= total_pages:
            break
        page += 1
        time.sleep(0.15)
    return all_tafsirs


def download_tafseer(slug, name, output_filename):
    """Download all ayah tafseers for a given tafseer slug using by_chapter endpoint."""
    output_path = JSON_DIR / output_filename
    results = []
    ayah_global_id = 0

    print(f"\nDownloading {name} ({slug})...", flush=True)

    for surah in range(1, 115):
        ayah_count = SURAH_AYAH_COUNTS[surah]
        base_url = f"{API_BASE}/{slug}/by_chapter/{surah}"

        try:
            tafsirs = fetch_all_pages(base_url)

            # Build a lookup by verse_key
            tafsir_map = {}
            for t in tafsirs:
                verse_key = t.get("verse_key", "")
                tafsir_map[verse_key] = strip_html(t.get("text", ""))

            for ayah in range(1, ayah_count + 1):
                ayah_global_id += 1
                verse_key = f"{surah}:{ayah}"
                text = tafsir_map.get(verse_key, "")
                results.append({
                    "ayah_id": ayah_global_id,
                    "surah_number": surah,
                    "ayah_number": ayah,
                    "text": text
                })

        except Exception as e:
            print(f"  FAILED Surah {surah}: {e}", flush=True)
            # Fill in empty entries for this surah
            for ayah in range(1, ayah_count + 1):
                ayah_global_id += 1
                results.append({
                    "ayah_id": ayah_global_id,
                    "surah_number": surah,
                    "ayah_number": ayah,
                    "text": ""
                })

        print(f"  Surah {surah}/114 done ({ayah_count} ayahs, {len(tafsirs)} tafsir entries)", flush=True)

        # Rate limiting - be respectful to the API
        time.sleep(0.2)

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(results, f, ensure_ascii=False, indent=2)

    non_empty = sum(1 for r in results if r['text'])
    print(f"Saved {len(results)} entries ({non_empty} non-empty) to {output_path}")
    return results


def main():
    print("=" * 60)
    print("Nimaz Pro - Tafseer Data Downloader")
    print("Using quran.com API v4")
    print("=" * 60)

    for slug, name, filename in TAFSEERS:
        download_tafseer(slug, name, filename)

    print("\nDone!")


if __name__ == "__main__":
    main()
