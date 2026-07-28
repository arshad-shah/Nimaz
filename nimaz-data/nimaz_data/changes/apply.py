"""Stage 3 — apply changes to the working database and hold them to what they declared.

A change's ``up.sql`` runs against the working database that stage 2 loaded the
NDJSON sources into, not against a shipped artifact. So a change is authored as
SQL (which is the natural way to say "set rule = 'ikhfa' where …") while the
thing that gets compiled is still the record set — and there is no "applied"
state anywhere that could drift from the sources.
"""

from __future__ import annotations

import sqlite3
from dataclasses import dataclass, field
from typing import Iterable

from ..core import db as dbx
from ..core.canonical import key_repr
from ..core.errors import ChangeError
from ..core.hash import records_hash
from ..core.spec import CollectionSpec, satisfies
from .model import Change


@dataclass(frozen=True)
class Snapshot:
    """A collection before or after a change.

    ``rows`` is populated only for collections the change declares — a full
    before-image of every collection on every change would cost more than the
    build. For the rest, the content hash is enough to answer the only question
    asked of them: did you move when nobody said you would?
    """

    rows: int
    content_hash: str
    keys: frozenset[str]
    image: dict[str, dict] | None = None


@dataclass
class ChangeOutcome:
    change: Change
    declared: dict[str, dict] = field(default_factory=dict)
    actual: dict[str, dict] = field(default_factory=dict)

    def to_dict(self) -> dict:
        return {
            "id": self.change.id,
            "title": self.change.title,
            "origin": self.change.origin,
            "declared": self.declared,
            "actual": self.actual,
        }


def snapshot(
    conn: sqlite3.Connection, spec: CollectionSpec, *, full: bool = False
) -> Snapshot:
    rows = dbx.read_collection(conn, spec)
    keys = [key_repr(r, spec.key) for r in rows]
    return Snapshot(
        rows=len(rows),
        content_hash=records_hash(rows, spec.key),
        keys=frozenset(keys),
        image=dict(zip(keys, rows)) if full else None,
    )


def snapshot_all(
    conn: sqlite3.Connection,
    specs: Iterable[CollectionSpec],
    *,
    full_for: Iterable[str] = (),
) -> dict[str, Snapshot]:
    wanted = set(full_for)
    return {s.name: snapshot(conn, s, full=s.name in wanted) for s in specs}


def apply_change(
    conn: sqlite3.Connection,
    change: Change,
    specs: dict[str, CollectionSpec],
    *,
    confirm_protected: bool = False,
) -> ChangeOutcome:
    """Run one change and compare reality to ``expect{}``.

    Raises rather than reports: a change whose blast radius does not match what
    it declared must never reach validation, let alone promotion (§5).
    """
    unknown = sorted(set(change.collections) - set(specs))
    if unknown:
        raise ChangeError(
            "change declares a collection that does not exist",
            id=change.id,
            collections=unknown,
        )

    for dep, constraint in sorted(change.requires.items()):
        if dep not in specs:
            raise ChangeError(
                "change requires a collection that is not in this build",
                id=change.id,
                collection=dep,
            )
        if not satisfies(specs[dep].version, constraint):
            raise ChangeError(
                "change requirement not satisfied",
                id=change.id,
                collection=dep,
                required=constraint,
                actual=specs[dep].version,
            )

    declared_names = set(change.collections)
    before = snapshot_all(conn, specs.values(), full_for=declared_names)

    sql = change.up_sql.read_text(encoding="utf-8")
    try:
        conn.executescript(sql)
        conn.commit()
    except sqlite3.Error as exc:
        conn.rollback()
        raise ChangeError(
            "up.sql failed", id=change.id, path=str(change.up_sql), detail=str(exc)
        ) from exc

    after = snapshot_all(conn, specs.values(), full_for=declared_names)
    outcome = ChangeOutcome(change=change)

    collateral = [
        name
        for name in sorted(specs)
        if name not in declared_names
        and before[name].content_hash != after[name].content_hash
    ]
    if collateral:
        raise ChangeError(
            "change modified collections it did not declare",
            id=change.id,
            collections=collateral,
        )

    for name, cc in sorted(change.collections.items()):
        spec = specs[name]
        b, a = before[name], after[name]
        actual = {
            "rows_delta": a.rows - b.rows,
            "rows_after": a.rows,
            "keys_touched": _keys_touched(spec, b, a),
        }
        outcome.declared[name] = cc.expect.to_dict()
        outcome.actual[name] = actual

        for field_name, value in cc.expect.to_dict().items():
            if actual[field_name] != value:
                raise ChangeError(
                    f"declared {field_name} does not match reality",
                    id=change.id,
                    collection=name,
                    declared=value,
                    actual=actual[field_name],
                )

        _check_protected(change, spec, b, a, cc.protected, confirm_protected)

    return outcome


def _keys_touched(spec: CollectionSpec, before: Snapshot, after: Snapshot) -> int:
    """Added + removed + modified-in-place, counted exactly."""
    if before.content_hash == after.content_hash:
        return 0
    touched = len(before.keys ^ after.keys)
    left, right = before.image or {}, after.image or {}
    for key in before.keys & after.keys:
        if left.get(key) != right.get(key):
            touched += 1
    return touched


def _check_protected(
    change: Change,
    spec: CollectionSpec,
    before: Snapshot,
    after: Snapshot,
    declared: tuple[str, ...],
    confirm_protected: bool,
) -> None:
    """The one place in the system with a second lock (§4)."""
    if not spec.protected:
        return
    left, right = before.image or {}, after.image or {}
    moved: set[str] = set()

    for key in before.keys & after.keys:
        a, b = left.get(key, {}), right.get(key, {})
        moved.update(c for c in spec.protected if a.get(c) != b.get(c))
    # A row that carries protected content and is added or removed is a change to
    # that content, whatever the field-level comparison says.
    if before.keys ^ after.keys:
        moved.update(spec.protected)

    if not moved:
        return
    undeclared = sorted(moved - set(declared))
    if undeclared:
        raise ChangeError(
            "change modifies protected fields without declaring them",
            id=change.id,
            collection=spec.name,
            fields=undeclared,
        )
    if not confirm_protected:
        raise ChangeError(
            "change modifies protected fields — re-run with --confirm-protected",
            id=change.id,
            collection=spec.name,
            fields=sorted(moved),
        )
