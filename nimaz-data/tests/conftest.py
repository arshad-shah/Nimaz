"""A miniature corpus with the real Nimaz schema shape.

The real vault is a 146 MB Git-LFS artifact that is deliberately not in any
repo, so the tests build their own: the same table shapes, the same surrogate-id
and shared-table problems (translations keyed by ayah, tafseer by surah+ayah),
at a size that runs in milliseconds. Everything the pipeline does to the real
corpus, it does here.
"""

from __future__ import annotations

import sqlite3
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from nimaz_data.build.bootstrap import init_from_vault  # noqa: E402
from nimaz_data.core import vault as vaultx  # noqa: E402
from nimaz_data.core.project import Project  # noqa: E402

SCHEMA = """
CREATE TABLE surahs (
    id INTEGER NOT NULL PRIMARY KEY,
    number INTEGER NOT NULL,
    name_arabic TEXT NOT NULL,
    name_english TEXT NOT NULL,
    verses_count INTEGER NOT NULL
);
CREATE TABLE ayahs (
    id INTEGER NOT NULL PRIMARY KEY,
    surah_id INTEGER NOT NULL,
    number_in_surah INTEGER NOT NULL,
    text_arabic TEXT NOT NULL,
    text_uthmani TEXT NOT NULL,
    page INTEGER NOT NULL,
    transliteration TEXT
);
CREATE TABLE translations (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    ayah_id INTEGER NOT NULL,
    text TEXT NOT NULL,
    translator_id TEXT NOT NULL
);
CREATE TABLE mushaf_layout_indopak16 (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    page INTEGER NOT NULL,
    line INTEGER NOT NULL,
    line_type TEXT NOT NULL,
    surah_id INTEGER NOT NULL,
    ayah_id INTEGER,
    first_word_position INTEGER,
    last_word_position INTEGER
);
CREATE TABLE quran_bookmarks (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    ayahId INTEGER NOT NULL,
    note TEXT
);
CREATE INDEX index_ayahs_surah_id ON ayahs(surah_id);
CREATE INDEX index_translations_ayah_id ON translations(ayah_id);
"""

CONSOLE_YAML = """
user_version: 12
user_tables:
  - quran_bookmarks
splits:
  translations: translator_id
naming:
  ayahs: quran.uthmani
  surahs: quran.surahs
  mushaf_layout_indopak16: mushaf.indopak16
  translations: "tr.{value}"
keys:
  translations: [ayah_id]
  mushaf_layout_indopak16: [page, line]
exclude_columns:
  translations: [id]
  mushaf_layout_indopak16: [id]
protected:
  ayahs: [text_uthmani, text_arabic]
retain: 3
"""

SURAHS = [
    (1, 1, "الفاتحة", "The Opening", 7),
    (2, 2, "البقرة", "The Cow", 5),
    (3, 3, "آل عمران", "The Family of Imran", 4),
]
TRANSLATORS = ("en.sahih", "bn.bengali")
PAGES = (1, 2)


def _ayahs() -> list[tuple]:
    rows = []
    ayah_id = 1
    for surah_id, _, _, _, count in SURAHS:
        for n in range(1, count + 1):
            rows.append(
                (
                    ayah_id,
                    surah_id,
                    n,
                    f"نص {surah_id}:{n}",
                    f"نَصّ {surah_id}:{n}",
                    1 if ayah_id <= 8 else 2,
                    f"nass {surah_id}:{n}",
                )
            )
            ayah_id += 1
    return rows


def build_vault_db(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(path)
    conn.executescript(SCHEMA)
    conn.execute("PRAGMA user_version = 12")
    conn.executemany("INSERT INTO surahs VALUES (?,?,?,?,?)", SURAHS)

    ayahs = _ayahs()
    conn.executemany("INSERT INTO ayahs VALUES (?,?,?,?,?,?,?)", ayahs)

    # Insert translations in a deliberately unhelpful order — by translator, then
    # descending ayah — so that a build which depends on insertion order fails.
    for translator in TRANSLATORS:
        conn.executemany(
            "INSERT INTO translations (ayah_id, text, translator_id) VALUES (?,?,?)",
            [(a[0], f"{translator} rendering of {a[0]}", translator) for a in reversed(ayahs)],
        )

    layout = []
    for page in PAGES:
        for line in range(1, 17):
            layout.append((page, line, "ayah", 1, None, 1, 4))
    conn.executemany(
        "INSERT INTO mushaf_layout_indopak16 "
        "(page, line, line_type, surah_id, ayah_id, first_word_position, last_word_position) "
        "VALUES (?,?,?,?,?,?,?)",
        layout,
    )
    conn.commit()
    conn.close()


@pytest.fixture
def project(tmp_path: Path) -> Project:
    """A sealed vault, an initialised project, and nothing built yet."""
    proj = Project(tmp_path)
    proj.ensure_dirs()
    (proj.data_dir / "console.yaml").write_text(CONSOLE_YAML, encoding="utf-8")

    raw = tmp_path / "seed" / "nimaz.db"
    build_vault_db(raw)
    vaultx.seal(proj.vault_dir, raw)

    # Rules live in the repo, not in the temp project; point at the real ones.
    repo_rules = Path(__file__).resolve().parents[1] / "data" / "rules"
    proj.rules_dir.rmdir()
    proj.rules_dir.symlink_to(repo_rules)

    init_from_vault(proj)
    _fill_provenance(proj)
    return proj


def _fill_provenance(proj: Project) -> None:
    """What a maintainer does after `nz init`: replace the TODO with real fields.

    Doing it here rather than in the fixture DB keeps `provenance.complete`
    honest — the collections start out failing it, exactly as a freshly
    initialised project does.
    """
    import yaml

    for name, translator in (
        ("tr.en.sahih", "Saheeh International"),
        ("tr.bn.bengali", "Muhiuddin Khan"),
    ):
        path = proj.collections_dir / name / "collection.yaml"
        body = yaml.safe_load(path.read_text(encoding="utf-8"))
        body["provenance"] = {
            "translator": translator,
            "license": "see LICENSES_TRANSLATIONS.md",
            "retrieved": "2026-07-27",
        }
        path.write_text(yaml.safe_dump(body, sort_keys=False, allow_unicode=True), encoding="utf-8")


@pytest.fixture
def initialised(project: Project) -> Project:
    return project
