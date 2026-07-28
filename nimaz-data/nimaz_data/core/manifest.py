"""The ``_manifest`` table (§4) — what is actually inside an artifact.

Every field here is derived from content. Nothing time-based goes in, because the
manifest is part of the file whose hash has to be reproducible; the timestamp
lives in ``build.json``, which is a receipt rather than a payload.
"""

from __future__ import annotations

import json
import sqlite3
from dataclasses import asdict, dataclass
from pathlib import Path

DDL = """
CREATE TABLE IF NOT EXISTS _manifest (
    collection   TEXT NOT NULL PRIMARY KEY,
    version      TEXT NOT NULL,
    content_hash TEXT NOT NULL,
    rows         INTEGER NOT NULL,
    keys_digest  TEXT NOT NULL
)
"""


@dataclass(frozen=True)
class Entry:
    collection: str
    version: str
    content_hash: str
    rows: int
    keys_digest: str


def write(conn: sqlite3.Connection, entries: list[Entry]) -> None:
    conn.execute(DDL)
    conn.execute("DELETE FROM _manifest")
    conn.executemany(
        "INSERT INTO _manifest (collection, version, content_hash, rows, keys_digest) "
        "VALUES (?, ?, ?, ?, ?)",
        [
            (e.collection, e.version, e.content_hash, e.rows, e.keys_digest)
            for e in sorted(entries, key=lambda e: e.collection)
        ],
    )
    conn.commit()


def read(conn: sqlite3.Connection) -> dict[str, Entry]:
    try:
        rows = conn.execute(
            "SELECT collection, version, content_hash, rows, keys_digest "
            "FROM _manifest ORDER BY collection"
        ).fetchall()
    except sqlite3.OperationalError:
        return {}
    return {
        r["collection"]: Entry(
            r["collection"], r["version"], r["content_hash"], int(r["rows"]), r["keys_digest"]
        )
        for r in rows
    }


# --- build.json, the receipt (§7) --------------------------------------------


def receipt(
    *, artifact_hash: str, built: str, entries: list[Entry], parent: str | None = None
) -> dict:
    return {
        "artifact": f"sha256:{artifact_hash}",
        "built": built,
        "parent": parent,
        "collections": {
            e.collection: {
                "version": e.version,
                "hash": e.content_hash,
                "rows": e.rows,
                "keys": e.keys_digest,
            }
            for e in sorted(entries, key=lambda e: e.collection)
        },
    }


def write_receipt(path: Path, body: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(body, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def read_receipt(path: Path) -> dict | None:
    if not path.exists():
        return None
    return json.loads(path.read_text(encoding="utf-8"))
