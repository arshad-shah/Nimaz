"""Changes (§5) — the single write funnel.

A change is a directory. The console writes one, a human writes one, an agent
writes one; the pipeline cannot tell them apart, which is the point. ``origin``
is recorded for audit and is never branched on.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import yaml

from ..core.errors import ChangeError

ID_RE = re.compile(r"^\d{8}T\d{4}_[a-z0-9][a-z0-9-]*$")
ORIGINS = ("console", "hand", "agent")
BUMPS = ("patch", "minor", "major")


@dataclass(frozen=True)
class Expect:
    """The declared blast radius. Absent fields are simply not checked.

    Declaring ``+43`` and delivering ``-4000`` is what this exists to stop, so a
    change that declares nothing at all is legal but earns no protection — and
    ``nz change new`` always writes at least ``rows_delta``.
    """

    rows_delta: int | None = None
    rows_after: int | None = None
    keys_touched: int | None = None

    @property
    def declared(self) -> bool:
        return any(v is not None for v in (self.rows_delta, self.rows_after, self.keys_touched))

    def to_dict(self) -> dict:
        return {
            k: v
            for k, v in {
                "rows_delta": self.rows_delta,
                "rows_after": self.rows_after,
                "keys_touched": self.keys_touched,
            }.items()
            if v is not None
        }


@dataclass(frozen=True)
class CollectionChange:
    bump: str = "patch"
    expect: Expect = field(default_factory=Expect)
    protected: tuple[str, ...] = ()


@dataclass(frozen=True)
class Change:
    id: str
    title: str
    author: str
    origin: str
    collections: dict[str, CollectionChange]
    requires: dict[str, str] = field(default_factory=dict)
    rationale: str = ""
    path: Path | None = None

    @property
    def dir(self) -> Path:
        if self.path is None:
            raise ChangeError("change has no on-disk location", change=self.id)
        return self.path.parent

    @property
    def up_sql(self) -> Path:
        return self.dir / "up.sql"

    @property
    def down_sql(self) -> Path:
        return self.dir / "down.sql"

    def touches_protected(self, collection: str) -> tuple[str, ...]:
        cc = self.collections.get(collection)
        return cc.protected if cc else ()

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "title": self.title,
            "author": self.author,
            "origin": self.origin,
            "collections": {
                name: {
                    "bump": cc.bump,
                    "expect": cc.expect.to_dict(),
                    **({"protected": list(cc.protected)} if cc.protected else {}),
                }
                for name, cc in sorted(self.collections.items())
            },
            "requires": dict(self.requires),
            "rationale": self.rationale,
            "path": str(self.dir) if self.path else None,
        }


def parse_change(data: dict, path: Path | None = None) -> Change:
    cid = str(data.get("id") or "").strip()
    if not ID_RE.match(cid):
        raise ChangeError(
            "change id must be <YYYYMMDDTHHMM>_<slug> — ordering is by id",
            id=cid,
            path=str(path) if path else None,
        )

    origin = str(data.get("origin") or "hand")
    if origin not in ORIGINS:
        raise ChangeError("unknown origin", id=cid, origin=origin, allowed=list(ORIGINS))

    raw = data.get("collections") or {}
    if not raw:
        raise ChangeError("change declares no collections", id=cid)

    collections: dict[str, CollectionChange] = {}
    for name, body in raw.items():
        body = body or {}
        bump = str(body.get("bump") or "patch")
        if bump not in BUMPS:
            raise ChangeError(
                "bump must be patch, minor or major", id=cid, collection=name, bump=bump
            )
        exp = body.get("expect") or {}
        collections[str(name)] = CollectionChange(
            bump=bump,
            expect=Expect(
                rows_delta=_int_or_none(exp.get("rows_delta")),
                rows_after=_int_or_none(exp.get("rows_after")),
                keys_touched=_int_or_none(exp.get("keys_touched")),
            ),
            protected=tuple(body.get("protected") or ()),
        )

    return Change(
        id=cid,
        title=str(data.get("title") or cid),
        author=str(data.get("author") or "unknown"),
        origin=origin,
        collections=collections,
        requires={str(k): str(v) for k, v in (data.get("requires") or {}).items()},
        rationale=str(data.get("rationale") or "").strip(),
        path=path,
    )


def _int_or_none(value: Any) -> int | None:
    if value is None or value == "":
        return None
    if isinstance(value, str):
        value = value.strip().lstrip("+")
    return int(value)


def load_change(change_dir: Path) -> Change:
    path = change_dir / "change.yaml"
    if not path.exists():
        raise ChangeError("change.yaml missing", path=str(change_dir))
    data = yaml.safe_load(path.read_text(encoding="utf-8")) or {}
    change = parse_change(data, path=path)
    if change.dir.name != change.id:
        raise ChangeError(
            "change directory name must equal the change id",
            id=change.id,
            directory=change.dir.name,
        )
    if not change.up_sql.exists():
        raise ChangeError("up.sql missing", id=change.id, path=str(change.up_sql))
    return change


def load_pending(changes_dir: Path) -> list[Change]:
    """Unfolded changes, in id order. ``applied/`` is history and is skipped."""
    if not changes_dir.exists():
        return []
    out = []
    for d in sorted(p for p in changes_dir.iterdir() if p.is_dir() and p.name != "applied"):
        out.append(load_change(d))
    return sorted(out, key=lambda c: c.id)


def dump_change(change: Change) -> str:
    body = change.to_dict()
    body.pop("path", None)
    if not body["requires"]:
        body.pop("requires")
    return yaml.safe_dump(body, sort_keys=False, allow_unicode=True, width=100)
