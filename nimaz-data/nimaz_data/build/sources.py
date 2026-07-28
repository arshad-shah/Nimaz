"""Stage 2 — NDJSON sources into the working database.

The working database is where changes are applied and where the compiler reads
from. It is in memory, it lives for one build, and it is thrown away — there is
no persisted "applied" state that could disagree with the sources (§5).
"""

from __future__ import annotations

import sqlite3
from dataclasses import dataclass
from typing import Iterable

from ..core import db as dbx
from ..core.canonical import sort_records
from ..core.errors import BuildError
from ..core.ndjson import read_records
from ..core.spec import CollectionSpec


@dataclass(frozen=True)
class LoadReport:
    collection: str
    rows: int
    synthesised_column: str | None


def create_working(schema_sql: str) -> sqlite3.Connection:
    conn = dbx.memory_db()
    try:
        conn.executescript(schema_sql)
    except sqlite3.Error as exc:
        raise BuildError("data/schema.sql failed to execute", detail=str(exc)) from exc
    conn.commit()
    return conn


def load_sources(
    conn: sqlite3.Connection, specs: Iterable[CollectionSpec]
) -> list[LoadReport]:
    """Insert every collection's records, assigning surrogate ids deterministically.

    A collection that excludes its table's INTEGER PRIMARY KEY (translations and
    tafseer both do — the id is a surrogate that carries no meaning) has that
    column synthesised here as ``1..N`` over the table's collections in name
    order, then key order. Doing it at load time rather than at compile time
    means a change's ``up.sql`` sees the same ids the artifact will, and the ids
    are a function of the sources rather than of insertion order.
    """
    specs = list(specs)
    by_table: dict[str, list[CollectionSpec]] = {}
    for spec in specs:
        by_table.setdefault(spec.source.table, []).append(spec)

    reports: list[LoadReport] = []
    for table in sorted(by_table):
        next_id = 1
        surrogate = _surrogate_column(conn, table, by_table[table])
        for spec in sorted(by_table[table], key=lambda s: s.name):
            records = read_records(spec.records_path)
            _check_schema(spec, records)
            rows = sort_records(records, spec.key)
            for row in rows:
                row.update(spec.source.where)
                if surrogate:
                    row[surrogate] = next_id
                    next_id += 1
            cols = _insert_columns(conn, table, spec, surrogate)
            dbx.insert_rows(conn, table, cols, rows)
            reports.append(LoadReport(spec.name, len(rows), surrogate))
    conn.commit()
    return reports


def _surrogate_column(
    conn: sqlite3.Connection, table: str, specs: list[CollectionSpec]
) -> str | None:
    """The table's single-column INTEGER PRIMARY KEY, if every collection drops it."""
    pk = dbx.primary_key(conn, table)
    if len(pk) != 1:
        return None
    col = pk[0]
    if all(col in s.source.exclude_columns for s in specs):
        return col
    return None


def _insert_columns(
    conn: sqlite3.Connection,
    table: str,
    spec: CollectionSpec,
    surrogate: str | None,
) -> list[str]:
    present = set(dbx.column_names(conn, table))
    cols = [c for c in spec.schema if c in present]
    for extra in (*spec.source.where.keys(), surrogate):
        if extra and extra in present and extra not in cols:
            cols.append(extra)
    missing = [c for c in spec.schema if c not in present]
    if missing:
        raise BuildError(
            "collection declares columns the schema does not have",
            collection=spec.name,
            table=table,
            columns=missing,
        )
    return cols


def _check_schema(spec: CollectionSpec, records: list[dict]) -> None:
    """Cheap structural check: every record has exactly the declared columns."""
    if not spec.schema:
        return
    declared = set(spec.schema)
    for i, record in enumerate(records):
        extra = sorted(set(record) - declared)
        if extra:
            raise BuildError(
                "record has columns the collection does not declare",
                collection=spec.name,
                line=i + 1,
                columns=extra,
            )
        missing = sorted(declared - set(record))
        if missing:
            raise BuildError(
                "record is missing declared columns",
                collection=spec.name,
                line=i + 1,
                columns=missing,
            )
