"""Rule registration and discovery (§8).

A rule is a small declarative plugin in ``data/rules/*.py``. It gets a read-only
view of the candidate and yields failures. It never writes to a database; the
most a rule can do is hand back a callable that emits a *change directory*, so
an autofix is reviewable like anything else that writes.
"""

from __future__ import annotations

import importlib.util
import sqlite3
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Callable, Iterable, Iterator, Sequence

from ..core import db as dbx
from ..core.canonical import key_repr
from ..core.errors import RuleError
from ..core.spec import CollectionSpec

BLOCKING = "blocking"
ADVISORY = "advisory"


@dataclass(frozen=True)
class Failure:
    key: str | tuple | None
    detail: str
    fix: Callable[[], Any] | None = None

    def key_str(self) -> str:
        if self.key is None:
            return ""
        if isinstance(self.key, tuple):
            return "|".join(str(k) for k in self.key)
        return str(self.key)


@dataclass(frozen=True)
class Rule:
    id: str
    scope: str
    severity: str
    fn: Callable[["Ctx"], Iterable[Failure]]
    doc: str = ""

    def applies_to(self, name: str) -> bool:
        if self.scope in ("*", "any"):
            return True
        if self.scope.endswith(".*"):
            return name.startswith(self.scope[:-1])
        if self.scope.startswith("kind:"):
            return False  # resolved against the spec, not the name
        return self.scope == name

    def applies_to_spec(self, spec: CollectionSpec) -> bool:
        if self.scope.startswith("kind:"):
            return spec.kind == self.scope[5:]
        return self.applies_to(spec.name)


REGISTRY: dict[str, Rule] = {}

# Ids claimed during the current discovery pass. Two rules colliding on an id in
# one pass is a genuine mistake; the same rule re-registering because discovery
# ran again (a second build in one process) is not.
_claimed: set[str] = set()


def rule(*, id: str, scope: str = "*", severity: str = BLOCKING):
    """Decorator. ``@rule(id="span.overlap", scope="tajweed.spans", severity="blocking")``"""

    def wrap(fn: Callable[["Ctx"], Iterable[Failure]]) -> Callable:
        if severity not in (BLOCKING, ADVISORY):
            raise RuleError("severity must be blocking or advisory", rule=id)
        if id in _claimed:
            raise RuleError("duplicate rule id", rule=id)
        _claimed.add(id)
        REGISTRY[id] = Rule(id=id, scope=scope, severity=severity, fn=fn, doc=fn.__doc__ or "")
        return fn

    return wrap


def discover(rules_dir: Path) -> list[Rule]:
    """Import every ``data/rules/*.py`` so its decorators run. Leading ``_`` is shared code."""
    if not rules_dir.exists():
        return sorted(REGISTRY.values(), key=lambda r: r.id)
    _claimed.clear()
    REGISTRY.clear()
    # So a rule module can `from _shared import …` the way the layout in §3 implies.
    if str(rules_dir) not in sys.path:
        sys.path.insert(0, str(rules_dir))
    for path in sorted(rules_dir.glob("*.py")):
        if path.name.startswith("_"):
            continue
        mod_name = f"nimaz_data_rules.{path.stem}"
        spec = importlib.util.spec_from_file_location(mod_name, path)
        if spec is None or spec.loader is None:
            raise RuleError("cannot load rule module", path=str(path))
        module = importlib.util.module_from_spec(spec)
        sys.modules[mod_name] = module
        try:
            spec.loader.exec_module(module)
        except Exception as exc:  # a broken plugin is a build failure, not a warning
            raise RuleError("rule module raised on import", path=str(path), detail=str(exc)) from exc
    return sorted(REGISTRY.values(), key=lambda r: r.id)


@dataclass
class Ctx:
    """What a rule sees: one collection, read-only, already materialised."""

    collection: CollectionSpec
    conn: sqlite3.Connection
    specs: dict[str, CollectionSpec] = field(default_factory=dict)
    _rows: list[dict] | None = None

    @property
    def rows(self) -> list[dict]:
        if self._rows is None:
            self._rows = dbx.read_collection(self.conn, self.collection)
        return self._rows

    @property
    def key(self) -> tuple[str, ...]:
        return self.collection.key

    def key_of(self, row: dict) -> str:
        return key_repr(row, self.collection.key)

    def grouped(self, *columns: str) -> Iterator[tuple[tuple, list[dict]]]:
        """Rows bucketed by a column tuple, in sorted bucket order."""
        buckets: dict[tuple, list[dict]] = {}
        for row in self.rows:
            buckets.setdefault(tuple(row.get(c) for c in columns), []).append(row)
        for k in sorted(buckets, key=lambda t: tuple((v is not None, v) for v in t)):
            yield k, buckets[k]

    def rows_of(self, collection: str) -> list[dict]:
        """Another collection's rows — for cross-collection referential rules."""
        spec = self.specs.get(collection)
        if spec is None:
            raise RuleError("unknown collection", collection=collection)
        return dbx.read_collection(self.conn, spec)

    def query(self, sql: str, params: Sequence = ()) -> list[dict]:
        return [dict(r) for r in self.conn.execute(sql, tuple(params)).fetchall()]
