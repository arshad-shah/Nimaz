"""Stage 4 — working database into a byte-reproducible candidate file (§6).

Determinism is not a nice property here, it is the identity mechanism: the
artifact hash is the build id, so a build that is not reproducible means the
hash names something you cannot re-derive. ``main.yml`` builds twice and compares.
"""

from __future__ import annotations

import sqlite3
from pathlib import Path

from ..core import db as dbx, manifest
from ..core.canonical import key_repr
from ..core.errors import BuildError
from ..core.hash import keys_digest, records_hash, sha256_file
from ..core.spec import CollectionSpec


def compile_candidate(
    working: sqlite3.Connection,
    *,
    schema_sql: str,
    specs: dict[str, CollectionSpec],
    out_path: Path,
    user_version: int = 0,
) -> tuple[str, list[manifest.Entry]]:
    """Write the candidate and return (artifact_hash, manifest entries)."""
    candidate = dbx.create_db(out_path)
    try:
        candidate.executescript(schema_sql)
    except sqlite3.Error as exc:
        candidate.close()
        raise BuildError("schema failed on the candidate", detail=str(exc)) from exc

    for table in dbx.tables(working):
        cols = dbx.column_names(working, table)
        order = _order_by(working, table)
        rows = [
            dict(zip(cols, r))
            for r in working.execute(
                f'SELECT {", ".join(chr(34) + c + chr(34) for c in cols)} '
                f'FROM "{table}" ORDER BY {order}'
            ).fetchall()
        ]
        dbx.insert_rows(candidate, table, cols, rows)

    entries = _manifest_entries(working, specs)
    manifest.write(candidate, entries)

    dbx.finalize(candidate, out_path, version=user_version)
    return sha256_file(out_path), entries


def _order_by(conn: sqlite3.Connection, table: str) -> str:
    """A total order for every table, so page layout never depends on insert order."""
    pk = dbx.primary_key(conn, table)
    if pk:
        return ", ".join(f'"{c}"' for c in pk)
    return ", ".join(f'"{c}"' for c in dbx.column_names(conn, table))


def _manifest_entries(
    conn: sqlite3.Connection, specs: dict[str, CollectionSpec]
) -> list[manifest.Entry]:
    entries = []
    for name in sorted(specs):
        spec = specs[name]
        rows = dbx.read_collection(conn, spec)
        entries.append(
            manifest.Entry(
                collection=name,
                version=spec.version,
                content_hash=records_hash(rows, spec.key),
                rows=len(rows),
                keys_digest=keys_digest(rows, spec.key),
            )
        )
    return entries


def collection_keys(conn: sqlite3.Connection, spec: CollectionSpec) -> set[str]:
    return {key_repr(r, spec.key) for r in dbx.read_collection(conn, spec)}
