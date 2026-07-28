"""The agent surface (§9) — same core, no privileged path.

The six tools are implemented here as plain functions returning the same
payloads the CLI's ``--json`` forms return, and the MCP wiring is a thin shell
over them. ``propose`` is the only mutating tool and it does not mutate: it
writes a change directory that a human reviews and a build gate enforces.
"""

from __future__ import annotations

import getpass
import sqlite3
from pathlib import Path
from typing import Any

from ..build.pipeline import build as run_build
from ..changes.model import CollectionChange, Expect
from ..changes.writer import write_change
from ..core import db as dbx, manifest
from ..core.canonical import key_repr
from ..core.diff import diff_records
from ..core.errors import NzError
from ..core.ndjson import read_records
from ..core.project import Project, discover
from ..rules.runner import run_rules

MAX_ROWS = 500

TOOLS = ("describe", "query", "get", "validate", "diff", "propose")


def _project(root: str | None = None) -> Project:
    return Project(Path(root).resolve()) if root else discover()


def describe(root: str | None = None) -> dict:
    """Collections, versions, hashes, build receipt."""
    project = _project(root)
    specs = project.specs()
    receipt = manifest.read_receipt(project.receipt_path) or {}
    return {
        "artifact": receipt.get("artifact"),
        "built": receipt.get("built"),
        "collections": {
            name: {
                "version": spec.version,
                "kind": spec.kind,
                "key": list(spec.key),
                "rows_min": spec.floors.rows_min,
                "protected": list(spec.protected),
                "depends_on": spec.depends_on,
                **(receipt.get("collections", {}).get(name, {})),
            }
            for name, spec in sorted(specs.items())
        },
    }


def query(sql: str, limit: int = MAX_ROWS, root: str | None = None) -> dict:
    """Read-only SQL against out/current, row-capped."""
    project = _project(root)
    limit = max(1, min(int(limit), MAX_ROWS))
    conn = dbx.open_readonly(project.current_link)
    try:
        rows = [dict(r) for r in conn.execute(sql).fetchmany(limit)]
    except sqlite3.Error as exc:
        raise NzError("query failed", detail=str(exc)) from exc
    finally:
        conn.close()
    return {"rows": rows, "count": len(rows), "capped": len(rows) == limit}


def get(collection: str, key: list, root: str | None = None) -> dict:
    """One record by key."""
    project = _project(root)
    spec = project.specs().get(collection)
    if spec is None:
        raise NzError("unknown collection", collection=collection)
    if len(key) != len(spec.key):
        raise NzError("key arity mismatch", expected=list(spec.key), got=key)
    wanted = "|".join("" if k is None else str(k) for k in key)
    for record in read_records(spec.records_path):
        if key_repr(record, spec.key) == wanted:
            return {"collection": collection, "key": key, "record": record}
    return {"collection": collection, "key": key, "record": None}


def validate(
    collections: list[str] | None = None,
    rules: list[str] | None = None,
    root: str | None = None,
) -> dict:
    """Full or scoped rule run, structured failures."""
    project = _project(root)
    conn = dbx.open_readonly(project.current_link)
    try:
        report = run_rules(
            conn,
            project.specs(),
            rules_dir=project.rules_dir,
            only=rules or (),
            collections=collections or (),
        )
    finally:
        conn.close()
    return report.to_dict()


def diff(collection: str, root: str | None = None) -> dict:
    """Sources vs the current artifact, for one collection."""
    project = _project(root)
    spec = project.specs().get(collection)
    if spec is None:
        raise NzError("unknown collection", collection=collection)
    conn = dbx.open_readonly(project.current_link)
    try:
        shipped = dbx.read_collection(conn, spec)
    finally:
        conn.close()
    sources = read_records(spec.records_path)
    return diff_records(collection, shipped, sources, spec.key).to_dict()


def propose(
    title: str,
    collections: dict[str, dict],
    up_sql: str,
    down_sql: str = "",
    rationale: str = "",
    root: str | None = None,
) -> dict:
    """Write a change directory with ``origin: agent``. The only mutating tool.

    It writes a proposal, not data. What makes that safe is not this function —
    it is that the change goes through the same gate as everything else, and the
    pipeline cannot tell where it came from.
    """
    project = _project(root)
    project.ensure_dirs()
    parsed = {
        name: CollectionChange(
            bump=str(body.get("bump") or "patch"),
            expect=Expect(
                rows_delta=body.get("rows_delta"),
                rows_after=body.get("rows_after"),
                keys_touched=body.get("keys_touched"),
            ),
            protected=tuple(body.get("protected") or ()),
        )
        for name, body in collections.items()
    }
    change = write_change(
        project.changes_dir,
        title=title,
        author=f"agent:{getpass.getuser()}",
        origin="agent",
        collections=parsed,
        up_sql=up_sql,
        down_sql=down_sql,
        rationale=rationale,
    )
    return {
        "change": change.to_dict(),
        "note": "written to data/changes/ for human review; not applied",
    }


def dry_run(root: str | None = None) -> dict:
    """Build without promoting — what an agent should call after ``propose``."""
    return run_build(_project(root)).to_dict()


def serve() -> None:  # pragma: no cover - requires the `agent` extra
    """Expose the functions above over MCP."""
    try:
        from mcp.server.fastmcp import FastMCP
    except ImportError as exc:
        raise NzError(
            "the MCP server needs the `agent` extra: pip install -e '.[agent]'"
        ) from exc

    server = FastMCP("nimaz")
    for name in TOOLS:
        server.add_tool(globals()[name], name=f"nimaz.{name}")
    server.add_tool(dry_run, name="nimaz.dry_run")
    server.run()


def call(tool: str, **kwargs: Any) -> dict:
    """Dispatch by name, so the CLI and the MCP shell share one entry point."""
    if tool not in TOOLS and tool != "dry_run":
        raise NzError("unknown tool", tool=tool, available=list(TOOLS))
    return globals()[tool](**kwargs)
