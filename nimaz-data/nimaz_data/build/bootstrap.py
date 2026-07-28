"""§11 — bootstrap from today's ``nimaz.db``, and ``nz source export``.

Step 3 is the one that cannot be skipped: build from the exported sources with
zero changes and prove the candidate is hash-identical, per collection, to the
vault. Until that round trip passes, the database is still the source of truth
and no change should be authored.
"""

from __future__ import annotations

import sqlite3
from dataclasses import dataclass, field, replace
from datetime import datetime, timezone
from pathlib import Path

from ..core import db as dbx, vault as vaultx
from ..core.canonical import key_repr
from ..core.errors import BuildError, SpecError
from ..core.hash import keys_digest, records_hash
from ..core.ndjson import write_records
from ..core.project import Config, Project
from ..core.spec import CollectionSpec, Floors, Provenance, Source, dump_spec


@dataclass
class InitReport:
    schema_tables: int = 0
    collections: list[str] = field(default_factory=list)
    skipped_user_tables: list[str] = field(default_factory=list)
    empty_tables: list[str] = field(default_factory=list)
    uncovered_tables: list[str] = field(default_factory=list)
    genesis: dict | None = None

    def to_dict(self) -> dict:
        return {
            "schema_tables": self.schema_tables,
            "collections": self.collections,
            "skipped_user_tables": self.skipped_user_tables,
            "empty_tables": self.empty_tables,
            "uncovered_tables": self.uncovered_tables,
            "genesis": bool(self.genesis),
        }


def derive_specs(conn: sqlite3.Connection, config: Config) -> list[CollectionSpec]:
    """Turn the vault's tables into collection specs, honouring ``console.yaml``.

    One collection per table, except where ``splits`` names a discriminator
    column — then one collection per distinct value, which is what makes each
    translation and each tafseer an independently versioned line (§4) without
    each of them getting its own table.
    """
    skip = set(config.user_tables) | set(config.ignore_tables)
    specs: list[CollectionSpec] = []

    for table in dbx.tables(conn):
        if table in skip or table.startswith("_"):
            continue
        if dbx.row_count(conn, table) == 0:
            continue

        exclude = tuple(config.exclude_columns.get(table) or ())
        key = tuple(config.keys.get(table) or ()) or tuple(dbx.primary_key(conn, table))
        protected = tuple(config.protected.get(table) or ())
        if not key:
            raise SpecError(
                "table has no primary key and no key declared in console.yaml",
                table=table,
            )

        discriminator = config.splits.get(table)
        if not discriminator:
            specs.append(
                _spec_for(conn, table, _name_for(config, table), key, exclude, protected, {})
            )
            continue

        values = [
            r[0]
            for r in conn.execute(
                f'SELECT DISTINCT "{discriminator}" FROM "{table}" '
                f'ORDER BY "{discriminator}"'
            ).fetchall()
        ]
        for value in values:
            name = _name_for(config, table, value)
            specs.append(
                _spec_for(
                    conn,
                    table,
                    name,
                    key,
                    tuple(sorted(set(exclude) | {discriminator})),
                    protected,
                    {discriminator: value},
                )
            )
    return specs


def _name_for(config: Config, table: str, value: object | None = None) -> str:
    template = config.naming.get(table)
    if value is None:
        return template or table.replace("_", ".")
    if template:
        return template.format(value=value, table=table)
    return f"{table.replace('_', '.')}.{value}"


def _spec_for(
    conn: sqlite3.Connection,
    table: str,
    name: str,
    key: tuple[str, ...],
    exclude: tuple[str, ...],
    protected: tuple[str, ...],
    where: dict,
) -> CollectionSpec:
    source = Source(table=table, where=dict(where), exclude_columns=exclude)
    probe = CollectionSpec(name=name, key=key, source=source)
    cols = set(dbx.collection_columns(conn, probe))
    schema = {c["name"]: _type_of(c) for c in dbx.columns(conn, table) if c["name"] in cols}
    rows = dbx.collection_count(conn, probe)
    return CollectionSpec(
        name=name,
        key=key,
        source=source,
        kind=_kind_for(table),
        version="1.0.0",
        protected=tuple(p for p in protected if p in schema),
        floors=Floors(rows_min=rows),
        schema=schema,
        provenance=Provenance(notes="TODO: fill in translator, license and retrieved (§16)"),
    )


def _type_of(col: dict) -> str:
    decl = (col.get("type") or "").upper()
    base = "int" if "INT" in decl else "real" if ("REAL" in decl or "FLOA" in decl or "DOUB" in decl) else "text"
    return base if col.get("notnull") else f"{base}?"


def _kind_for(table: str) -> str:
    return {
        "translations": "translation",
        "tafseer_texts": "tafseer",
        "hadiths": "hadith",
        "ayahs": "quran",
        "surahs": "quran-index",
        "mushaf_layout_indopak16": "mushaf",
    }.get(table, "generic")


def init_from_vault(project: Project, *, sealed_at: str | None = None) -> InitReport:
    """§11.1–11.2 — decompose the sealed vault into schema, collections and sources."""
    st = vaultx.verify(project.vault_dir)
    project.ensure_dirs()
    config = project.config
    report = InitReport()

    conn = dbx.open_vault(vaultx.corpus_path(project.vault_dir))
    try:
        statements = dbx.schema_statements(conn)
        report.schema_tables = len(dbx.tables(conn))
        project.schema_path.write_text(
            "-- Generated by `nz init` from the sealed vault. The schema is a source,\n"
            "-- not a build output: edit it through a change, never by hand-migrating a DB.\n"
            f"PRAGMA user_version = {dbx.user_version(conn)};\n\n"
            + "\n".join(statements)
            + "\n",
            encoding="utf-8",
        )

        specs = derive_specs(conn, config)
        covered = {s.source.table for s in specs}
        for table in dbx.tables(conn):
            if table in config.user_tables:
                report.skipped_user_tables.append(table)
            elif dbx.row_count(conn, table) == 0:
                report.empty_tables.append(table)
            elif table not in covered:
                report.uncovered_tables.append(table)

        genesis_collections: dict[str, dict] = {}
        key_lists: dict[str, list[str]] = {}

        for derived in specs:
            spec = replace(
                derived,
                path=project.collections_dir / derived.name / "collection.yaml",
            )
            spec.dir.mkdir(parents=True, exist_ok=True)
            spec.path.write_text(dump_spec(spec), encoding="utf-8")
            rows = dbx.read_collection(conn, spec)
            write_records(spec.records_path, rows, spec.key)
            report.collections.append(spec.name)
            genesis_collections[spec.name] = {
                "rows": len(rows),
                "hash": records_hash(rows, spec.key),
                "keys_digest": keys_digest(rows, spec.key),
            }
            key_lists[spec.name] = sorted(key_repr(r, spec.key) for r in rows)

        if not project.genesis_path.exists():
            report.genesis = vaultx.write_genesis(
                project.genesis_path,
                vault_sha256=st.actual or "",
                collections=genesis_collections,
                keys_dir=project.keys_dir,
                key_lists=key_lists,
                sealed_at=sealed_at
                or datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
            )
    finally:
        conn.close()

    return report


@dataclass
class ExportReport:
    collection: str
    rows: int
    bytes: int
    content_hash: str
    changed: bool

    def to_dict(self) -> dict:
        return {
            "collection": self.collection,
            "rows": self.rows,
            "bytes": self.bytes,
            "hash": self.content_hash,
            "changed": self.changed,
        }


def export_sources(
    project: Project, source_db: Path, *, collections: list[str] | None = None
) -> list[ExportReport]:
    """Rewrite ``records.ndjson`` from a database.

    Used two ways: at bootstrap from the vault, and in CI where the *candidate*
    is exported back to NDJSON and rebuilt, so the round-trip proof is re-run on
    every merge rather than asserted once and assumed forever (§14).
    """
    specs = project.specs()
    if not specs:
        raise BuildError("no collections to export", path=str(project.collections_dir))
    wanted = set(collections) if collections else set(specs)

    conn = (
        dbx.open_vault(source_db)
        if source_db.resolve() == vaultx.corpus_path(project.vault_dir).resolve()
        else dbx.open_readonly(source_db)
    )
    out: list[ExportReport] = []
    try:
        for name in sorted(wanted):
            spec = specs[name]
            before = (
                records_hash(_safe_read(spec), spec.key) if spec.records_path.exists() else None
            )
            rows = dbx.read_collection(conn, spec)
            digest = records_hash(rows, spec.key)
            n, size = write_records(spec.records_path, rows, spec.key)
            out.append(ExportReport(name, n, size, digest, changed=before != digest))
    finally:
        conn.close()
    return out


def _safe_read(spec: CollectionSpec) -> list[dict]:
    from ..core.ndjson import read_records

    try:
        return read_records(spec.records_path)
    except Exception:
        return []
