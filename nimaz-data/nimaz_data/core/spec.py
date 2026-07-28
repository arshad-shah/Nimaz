"""Collections (§4) — the unit of versioning, validation, shipping and rollback."""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Iterator

import yaml

from .errors import SpecError

_NAME_RE = re.compile(r"^[a-z0-9]+(\.[a-z0-9_-]+)*$")

# Constraint grammar for depends_on / requires. Deliberately tiny: >=, >, <=, <, ==.
_CONSTRAINT_RE = re.compile(r"^(>=|<=|==|>|<)\s*(.+)$")


@dataclass(frozen=True)
class Source:
    """Where a collection's rows live in the compiled database.

    A collection is usually a whole table, but it does not have to be. ``where``
    carves one edition out of a shared table — that is what makes
    ``tr.en.sahih`` and ``tr.bn`` independently versioned collections (§4)
    without giving every translation its own table, and what makes adding the
    sixteenth translation a file rather than a schema change (§16).
    """

    table: str
    where: dict[str, Any] = field(default_factory=dict)
    exclude_columns: tuple[str, ...] = ()

    def where_sql(self, alias: str = "") -> tuple[str, list]:
        if not self.where:
            return "", []
        prefix = f"{alias}." if alias else ""
        clauses = " AND ".join(f"{prefix}{c} = ?" for c in sorted(self.where))
        return clauses, [self.where[c] for c in sorted(self.where)]


@dataclass(frozen=True)
class Floors:
    """``rows_min`` is the hard floor the build refuses to go below, no exceptions.

    ``rows_exact`` is for collections whose size is a known fact rather than a
    measurement — there are 6236 ayahs, and a translation that has 6235 rows is
    a hole in the app, not a smaller translation. ``nz init`` never invents one;
    it is declared by hand, which is what makes it a claim rather than a
    snapshot of whatever happened to be there.
    """

    rows_min: int = 0
    rows_exact: int | None = None


@dataclass(frozen=True)
class Provenance:
    """Structured, not prose (§16). A rule asserts these are present and non-empty."""

    translator: str | None = None
    license: str | None = None
    retrieved: str | None = None
    upstream: str | None = None
    notes: str | None = None

    def missing(self, required: tuple[str, ...]) -> list[str]:
        return [f for f in required if not (getattr(self, f) or "").strip()]


@dataclass(frozen=True)
class CollectionSpec:
    name: str
    key: tuple[str, ...]
    source: Source
    kind: str = "generic"
    version: str = "0.0.0"
    depends_on: dict[str, str] = field(default_factory=dict)
    protected: tuple[str, ...] = ()
    floors: Floors = field(default_factory=Floors)
    schema: dict[str, str] = field(default_factory=dict)
    provenance: Provenance = field(default_factory=Provenance)
    path: Path | None = None

    @property
    def dir(self) -> Path:
        if self.path is None:
            raise SpecError("collection has no on-disk location", collection=self.name)
        return self.path.parent

    @property
    def records_path(self) -> Path:
        return self.dir / "records.ndjson"

    @property
    def columns(self) -> tuple[str, ...]:
        return tuple(self.schema)


def _as_tuple(value: Any, field_name: str, name: str) -> tuple[str, ...]:
    if value is None:
        return ()
    if isinstance(value, str):
        return (value,)
    if isinstance(value, (list, tuple)):
        return tuple(str(v) for v in value)
    raise SpecError(f"{field_name} must be a list", collection=name)


def parse_spec(data: dict, path: Path | None = None) -> CollectionSpec:
    name = str(data.get("name") or "").strip()
    if not _NAME_RE.match(name):
        raise SpecError(
            "collection name must be lowercase dot-separated (e.g. tr.en.sahih)",
            name=name,
            path=str(path) if path else None,
        )

    key = _as_tuple(data.get("key"), "key", name)
    if not key:
        raise SpecError("collection needs a key", collection=name)

    raw_source = data.get("source") or {}
    if isinstance(raw_source, str):
        raw_source = {"table": raw_source}
    table = str(raw_source.get("table") or "").strip()
    if not table:
        raise SpecError("collection needs source.table", collection=name)

    source = Source(
        table=table,
        where=dict(raw_source.get("where") or {}),
        exclude_columns=_as_tuple(
            raw_source.get("exclude_columns"), "source.exclude_columns", name
        ),
    )

    schema = {str(k): str(v) for k, v in (data.get("schema") or {}).items()}
    missing_key_cols = [k for k in key if schema and k not in schema]
    if missing_key_cols:
        raise SpecError(
            "key columns are absent from the declared schema",
            collection=name,
            columns=missing_key_cols,
        )

    protected = _as_tuple(data.get("protected"), "protected", name)
    unknown_protected = [p for p in protected if schema and p not in schema]
    if unknown_protected:
        raise SpecError(
            "protected names a column that is not in the schema",
            collection=name,
            columns=unknown_protected,
        )

    floors_raw = data.get("floors") or {}
    prov_raw = data.get("provenance") or {}

    return CollectionSpec(
        name=name,
        key=key,
        source=source,
        kind=str(data.get("kind") or "generic"),
        version=str(data.get("version") or "0.0.0"),
        depends_on={str(k): str(v) for k, v in (data.get("depends_on") or {}).items()},
        protected=protected,
        floors=Floors(
            rows_min=int(floors_raw.get("rows_min") or 0),
            rows_exact=(
                int(floors_raw["rows_exact"])
                if floors_raw.get("rows_exact") is not None
                else None
            ),
        ),
        schema=schema,
        provenance=Provenance(
            translator=prov_raw.get("translator"),
            license=prov_raw.get("license"),
            retrieved=str(prov_raw["retrieved"]) if prov_raw.get("retrieved") else None,
            upstream=prov_raw.get("upstream"),
            notes=prov_raw.get("notes"),
        ),
        path=path,
    )


def load_spec(path: Path) -> CollectionSpec:
    with open(path, "r", encoding="utf-8") as fh:
        data = yaml.safe_load(fh) or {}
    return parse_spec(data, path=path)


def load_all(collections_dir: Path) -> dict[str, CollectionSpec]:
    """Every collection.yaml under ``data/collections``, keyed by name."""
    specs: dict[str, CollectionSpec] = {}
    for path in sorted(collections_dir.glob("*/collection.yaml")):
        spec = load_spec(path)
        if spec.name in specs:
            raise SpecError("duplicate collection name", collection=spec.name)
        if spec.dir.name != spec.name:
            raise SpecError(
                "collection directory name must equal the collection name",
                collection=spec.name,
                directory=spec.dir.name,
            )
        specs[spec.name] = spec
    return specs


def dump_spec(spec: CollectionSpec) -> str:
    """Round-trippable YAML. Written by ``nz init`` and then owned by a human."""
    body: dict[str, Any] = {
        "name": spec.name,
        "kind": spec.kind,
        "version": spec.version,
        "key": list(spec.key),
        "source": {"table": spec.source.table},
    }
    if spec.source.where:
        body["source"]["where"] = dict(spec.source.where)
    if spec.source.exclude_columns:
        body["source"]["exclude_columns"] = list(spec.source.exclude_columns)
    if spec.depends_on:
        body["depends_on"] = dict(spec.depends_on)
    body["protected"] = list(spec.protected)
    body["floors"] = {"rows_min": spec.floors.rows_min}
    if spec.floors.rows_exact is not None:
        body["floors"]["rows_exact"] = spec.floors.rows_exact
    body["provenance"] = {
        k: v
        for k, v in {
            "translator": spec.provenance.translator,
            "license": spec.provenance.license,
            "retrieved": spec.provenance.retrieved,
            "upstream": spec.provenance.upstream,
            "notes": spec.provenance.notes,
        }.items()
        if v
    }
    body["schema"] = dict(spec.schema)
    return yaml.safe_dump(body, sort_keys=False, allow_unicode=True, width=100)


# --- dependency constraints (§14, `nz deps check`) ---------------------------


def _version_tuple(v: str) -> tuple:
    parts: list[Any] = []
    for chunk in re.split(r"[.\-+]", v.strip()):
        parts.append((0, int(chunk)) if chunk.isdigit() else (1, chunk))
    return tuple(parts)


def satisfies(version: str, constraint: str) -> bool:
    """``satisfies("2026.07.3", ">=2026.07.3")`` -> True."""
    m = _CONSTRAINT_RE.match(constraint.strip())
    if not m:
        raise SpecError("unparseable version constraint", constraint=constraint)
    op, target = m.group(1), m.group(2).strip()
    left, right = _version_tuple(version), _version_tuple(target)
    return {
        ">=": left >= right,
        ">": left > right,
        "<=": left <= right,
        "<": left < right,
        "==": left == right,
    }[op]


def check_dependencies(specs: dict[str, CollectionSpec]) -> Iterator[str]:
    """Yield one message per unsatisfiable ``depends_on`` edge."""
    for spec in specs.values():
        for dep, constraint in sorted(spec.depends_on.items()):
            if dep not in specs:
                yield f"{spec.name} depends on {dep}, which is not in this build"
                continue
            have = specs[dep].version
            if not satisfies(have, constraint):
                yield (
                    f"{spec.name} requires {dep} {constraint}, "
                    f"but this build has {dep} {have}"
                )
