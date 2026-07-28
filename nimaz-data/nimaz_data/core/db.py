"""SQLite access. Three databases, three ways of opening them (§2).

Nothing in this module opens ``vault/corpus.db`` with a write handle, and there
is no code path that takes a mode argument — the read-only-ness is in the
function you call, not in an argument you might get wrong.
"""

from __future__ import annotations

import os
import sqlite3
from contextlib import contextmanager
from pathlib import Path
from typing import Any, Iterator, Sequence

from .errors import BuildError, VaultError
from .spec import CollectionSpec

# Determinism knobs (§6). Fixed so that the same inputs give a byte-identical file.
PAGE_SIZE = 4096
ENCODING = "UTF-8"

# Byte ranges in the SQLite header that carry no schema or row information but do
# change between otherwise identical builds. Zeroing them is what makes "build
# twice and compare hashes" a test of our determinism rather than of SQLite's.
#   24..27  file change counter
#   92..95  version-valid-for number
#   96..99  SQLITE_VERSION_NUMBER of the library that last wrote the file
_VOLATILE_HEADER_RANGES = ((24, 28), (92, 96), (96, 100))


def open_vault(path: Path) -> sqlite3.Connection:
    """Open the vault read-only and immutable. The only way this module sees it."""
    if not path.exists():
        raise VaultError("vault database not found", path=str(path))
    uri = f"file:{path.resolve().as_posix()}?mode=ro&immutable=1"
    conn = sqlite3.connect(uri, uri=True)
    conn.row_factory = sqlite3.Row
    return conn


def open_readonly(path: Path) -> sqlite3.Connection:
    """Read-only handle on a built artifact (``out/current``, a candidate)."""
    if not path.exists():
        raise BuildError("database not found", path=str(path))
    uri = f"file:{path.resolve().as_posix()}?mode=ro"
    conn = sqlite3.connect(uri, uri=True)
    conn.row_factory = sqlite3.Row
    return conn


def create_db(path: Path) -> sqlite3.Connection:
    """A fresh, empty, writable database. Candidates and working DBs only."""
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        path.unlink()
    conn = sqlite3.connect(path)
    conn.row_factory = sqlite3.Row
    conn.executescript(
        f"PRAGMA page_size = {PAGE_SIZE};\n"
        f"PRAGMA encoding = '{ENCODING}';\n"
        "PRAGMA journal_mode = DELETE;\n"
        "PRAGMA foreign_keys = OFF;\n"
    )
    return conn


def memory_db() -> sqlite3.Connection:
    """The working database that sources are loaded into and changes applied to."""
    conn = sqlite3.connect(":memory:")
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = OFF")
    return conn


@contextmanager
def closing(conn: sqlite3.Connection) -> Iterator[sqlite3.Connection]:
    try:
        yield conn
    finally:
        conn.close()


# --- introspection -----------------------------------------------------------


def tables(conn: sqlite3.Connection) -> list[str]:
    rows = conn.execute(
        "SELECT name FROM sqlite_master WHERE type = 'table' "
        "AND name NOT LIKE 'sqlite_%' ORDER BY name"
    ).fetchall()
    return [r["name"] for r in rows]


def columns(conn: sqlite3.Connection, table: str) -> list[dict]:
    return [dict(r) for r in conn.execute(f'PRAGMA table_info("{table}")').fetchall()]


def column_names(conn: sqlite3.Connection, table: str) -> list[str]:
    return [c["name"] for c in columns(conn, table)]


def primary_key(conn: sqlite3.Connection, table: str) -> list[str]:
    pk = [c for c in columns(conn, table) if c["pk"]]
    pk.sort(key=lambda c: c["pk"])
    return [c["name"] for c in pk]


def row_count(conn: sqlite3.Connection, table: str) -> int:
    return int(conn.execute(f'SELECT COUNT(*) FROM "{table}"').fetchone()[0])


def schema_statements(conn: sqlite3.Connection) -> list[str]:
    """Every CREATE statement in the database, tables first, then indexes/triggers/views.

    Ordering is (type-rank, name) rather than sqlite_master's insertion order so
    that two vaults with the same schema produce the same ``data/schema.sql``.
    """
    rank = {"table": 0, "view": 1, "index": 2, "trigger": 3}
    rows = conn.execute(
        "SELECT type, name, tbl_name, sql FROM sqlite_master "
        "WHERE sql IS NOT NULL AND name NOT LIKE 'sqlite_%'"
    ).fetchall()
    ordered = sorted(rows, key=lambda r: (rank.get(r["type"], 9), r["tbl_name"], r["name"]))
    return [r["sql"].strip().rstrip(";") + ";" for r in ordered]


def user_version(conn: sqlite3.Connection) -> int:
    return int(conn.execute("PRAGMA user_version").fetchone()[0])


# --- reading a collection ----------------------------------------------------


def collection_columns(conn: sqlite3.Connection, spec: CollectionSpec) -> list[str]:
    present = column_names(conn, spec.source.table)
    excluded = set(spec.source.exclude_columns)
    return [c for c in present if c not in excluded]


def read_collection(
    conn: sqlite3.Connection, spec: CollectionSpec, cols: Sequence[str] | None = None
) -> list[dict]:
    """All rows of a collection, as plain dicts, in key order.

    ``exclude_columns`` drops surrogate AUTOINCREMENT ids that carry no meaning
    and would otherwise make the content hash depend on insertion order.
    """
    use = list(cols) if cols is not None else collection_columns(conn, spec)
    select = ", ".join(f'"{c}"' for c in use)
    where, params = spec.source.where_sql()
    sql = f'SELECT {select} FROM "{spec.source.table}"'
    if where:
        sql += f" WHERE {where}"
    order = ", ".join(f'"{k}"' for k in spec.key)
    sql += f" ORDER BY {order}"
    return [dict(zip(use, row)) for row in conn.execute(sql, params).fetchall()]


def collection_keys(conn: sqlite3.Connection, spec: CollectionSpec) -> set[tuple]:
    select = ", ".join(f'"{k}"' for k in spec.key)
    where, params = spec.source.where_sql()
    sql = f'SELECT {select} FROM "{spec.source.table}"'
    if where:
        sql += f" WHERE {where}"
    return {tuple(row) for row in conn.execute(sql, params).fetchall()}


def collection_count(conn: sqlite3.Connection, spec: CollectionSpec) -> int:
    where, params = spec.source.where_sql()
    sql = f'SELECT COUNT(*) FROM "{spec.source.table}"'
    if where:
        sql += f" WHERE {where}"
    return int(conn.execute(sql, params).fetchone()[0])


def insert_rows(
    conn: sqlite3.Connection, table: str, cols: Sequence[str], rows: Sequence[dict]
) -> None:
    if not rows:
        return
    placeholders = ", ".join("?" for _ in cols)
    names = ", ".join(f'"{c}"' for c in cols)
    conn.executemany(
        f'INSERT INTO "{table}" ({names}) VALUES ({placeholders})',
        [tuple(r.get(c) for c in cols) for r in rows],
    )


# --- deterministic finish ----------------------------------------------------


def finalize(conn: sqlite3.Connection, path: Path, *, version: int = 0) -> None:
    """VACUUM, close, then flatten the volatile header bytes.

    VACUUM rewrites the file with pages in rowid order and no free pages, which
    removes the last dependence on the order rows happened to be inserted in.
    What it does not remove is the header counters, so we do that ourselves.
    """
    conn.execute(f"PRAGMA user_version = {int(version)}")
    conn.commit()
    conn.execute("PRAGMA journal_mode = DELETE")
    conn.execute("VACUUM")
    conn.commit()
    conn.close()

    with open(path, "r+b") as fh:
        for start, end in _VOLATILE_HEADER_RANGES:
            fh.seek(start)
            fh.write(b"\x00" * (end - start))
        fh.flush()
        os.fsync(fh.fileno())
