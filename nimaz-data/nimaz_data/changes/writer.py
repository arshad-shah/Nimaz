"""Writing a change directory. The console, the CLI and the agent all land here."""

from __future__ import annotations

import re
from datetime import datetime, timezone
from pathlib import Path

from ..core.errors import ChangeError
from .model import Change, CollectionChange, Expect, dump_change

_SLUG_RE = re.compile(r"[^a-z0-9]+")


def slugify(text: str) -> str:
    slug = _SLUG_RE.sub("-", text.lower()).strip("-")
    return slug[:60] or "change"


def new_id(title: str, *, now: datetime | None = None) -> str:
    stamp = (now or datetime.now(timezone.utc)).strftime("%Y%m%dT%H%M")
    return f"{stamp}_{slugify(title)}"


def write_change(
    changes_dir: Path,
    *,
    title: str,
    author: str,
    origin: str,
    collections: dict[str, CollectionChange],
    up_sql: str,
    down_sql: str = "",
    requires: dict[str, str] | None = None,
    rationale: str = "",
    now: datetime | None = None,
) -> Change:
    """Create ``<changes_dir>/<id>/`` with change.yaml, up.sql and down.sql.

    Returns the parsed change, so a caller that just wrote one validates it by
    construction rather than by remembering to.
    """
    cid = new_id(title, now=now)
    target = changes_dir / cid
    if target.exists():
        raise ChangeError("a change with this id already exists", id=cid, path=str(target))
    if not collections:
        raise ChangeError("a change must declare at least one collection", id=cid)

    change = Change(
        id=cid,
        title=title,
        author=author,
        origin=origin,
        collections=collections,
        requires=dict(requires or {}),
        rationale=rationale,
        path=target / "change.yaml",
    )

    target.mkdir(parents=True)
    (target / "change.yaml").write_text(dump_change(change), encoding="utf-8")
    (target / "up.sql").write_text(_sql_body(up_sql), encoding="utf-8")
    (target / "down.sql").write_text(_sql_body(down_sql), encoding="utf-8")
    return change


def _sql_body(sql: str) -> str:
    sql = sql.strip()
    if not sql:
        return "-- no statements\n"
    return sql + ("\n" if not sql.endswith("\n") else "")


def expect_from_counts(before: int, after: int, keys_touched: int) -> Expect:
    return Expect(rows_delta=after - before, rows_after=after, keys_touched=keys_touched)
