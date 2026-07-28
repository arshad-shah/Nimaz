"""``nz`` — the CLI front door.

Every command here is a thin wrapper over the core, and every one has a ``--json``
form returning the same schema the MCP server returns (§9). The CLI has no
privileged path: it cannot write to a database any more than the agent can.
"""

from __future__ import annotations

import sqlite3
import sys
from pathlib import Path
from typing import Optional

import typer

from ..build import bootstrap as bootstrapx
from ..build.guard import compare_against_vault
from ..build.pipeline import build as run_build
from ..build.promote import promote as do_promote, rollback as do_rollback
from ..changes.fold import fold as do_fold
from ..changes.model import CollectionChange, Expect, load_pending
from ..changes.writer import write_change
from ..core import db as dbx, manifest, vault as vaultx
from ..core.errors import NzError
from ..core.hash import short
from ..core.project import Project, discover
from ..core.spec import check_dependencies
from ..rules.registry import discover as discover_rules
from ..rules.report import as_github_annotations, as_text
from ..rules.runner import run_rules
from . import output as out

app = typer.Typer(
    add_completion=False,
    no_args_is_help=True,
    help="Nimaz Data Console — compile the corpus from text sources into a verified artifact.",
)
change_app = typer.Typer(no_args_is_help=True, help="The single write funnel (§5).")
source_app = typer.Typer(no_args_is_help=True, help="NDJSON sources.")
vault_app = typer.Typer(no_args_is_help=True, help="The read-only original (§2).")
deps_app = typer.Typer(no_args_is_help=True, help="Cross-collection dependency contract.")
app.add_typer(change_app, name="change")
app.add_typer(source_app, name="source")
app.add_typer(vault_app, name="vault")
app.add_typer(deps_app, name="deps")

ROOT_OPT = typer.Option(None, "--root", help="Project root (defaults to the nearest one).")


def _project(root: Optional[Path]) -> Project:
    return Project(root.resolve()) if root else discover()


def _fail(exc: NzError, as_json: bool) -> None:
    if as_json:
        out.emit_json(exc.to_dict())
    else:
        out.echo(out.paint(f"✗ {exc.message}", out.BLOCK))
        for k, v in exc.detail.items():
            out.echo(f"    {k}: {v}")
    raise typer.Exit(exc.exit_code)


# --- doctor ------------------------------------------------------------------


@app.command()
def doctor(
    root: Optional[Path] = ROOT_OPT,
    json_out: bool = typer.Option(False, "--json"),
) -> None:
    """Session start: verify the vault, the sources and the pending changes."""
    project = _project(root)
    st = vaultx.status(project.vault_dir)
    specs = project.specs()
    pending = load_pending(project.changes_dir) if project.changes_dir.exists() else []
    receipt = manifest.read_receipt(project.receipt_path)
    dep_errors = list(check_dependencies(specs)) if specs else []

    payload = {
        "root": str(project.root),
        "vault": st.to_dict(),
        "schema": project.schema_path.exists(),
        "genesis": project.genesis_path.exists(),
        "collections": len(specs),
        "pending_changes": [c.id for c in pending],
        "current_artifact": (receipt or {}).get("artifact"),
        "dependency_errors": dep_errors,
        "ok": st.ok and project.schema_path.exists() and not dep_errors,
    }
    if json_out:
        out.emit_json(payload)
        raise typer.Exit(0 if payload["ok"] else 1)

    out.heading(f"nimaz-data at {project.root}")
    out.echo()
    state = "ok" if st.ok else ("unknown" if not st.exists else "blocking")
    out.echo(f"{out.dot(state)} vault  {st.path}")
    if st.exists:
        out.echo(f"    sha256   {short(st.actual or '', 12)}  (expected {short(st.expected or '—', 12)})")
        out.echo(f"    mode     {'444 (read-only)' if not st.writable else 'WRITABLE — expected 444'}")
        out.echo(f"    touched  {st.modified} by {st.owner or 'unknown'}")
    else:
        out.echo("    absent — run `nz vault seal <path-to-nimaz.db>`")

    out.echo(f"{out.dot('ok' if project.schema_path.exists() else 'unknown')} schema  "
             f"{'data/schema.sql' if project.schema_path.exists() else 'missing — run `nz init`'}")
    out.echo(f"{out.dot('ok' if project.genesis_path.exists() else 'unknown')} genesis "
             f"{'sealed' if project.genesis_path.exists() else 'not written'}")
    out.echo(f"{out.dot('ok' if specs else 'unknown')} sources {len(specs)} collection(s)")
    out.echo(f"{out.dot('pending' if pending else 'ok')} changes {len(pending)} pending")
    for change in pending:
        out.echo(f"    {change.id}  {change.title}  [{change.origin}]")
    if dep_errors:
        out.echo(f"{out.dot('blocking')} deps")
        for line in dep_errors:
            out.echo(f"    {line}")
    if receipt:
        out.echo(f"{out.dot('ok')} out     {receipt.get('artifact')}")
    raise typer.Exit(0 if payload["ok"] else 1)


# --- bootstrap ---------------------------------------------------------------


@vault_app.command("seal")
def vault_seal(
    source: Path = typer.Argument(..., help="The database to archive as the vault."),
    root: Optional[Path] = ROOT_OPT,
    force: bool = typer.Option(False, "--force", help="Replace an already-sealed vault."),
    json_out: bool = typer.Option(False, "--json"),
) -> None:
    """§11.1 — copy the current DB into the vault, chmod 444, record its hash."""
    project = _project(root)
    try:
        st = vaultx.seal(project.vault_dir, source, force=force)
    except NzError as exc:
        _fail(exc, json_out)
        return
    if json_out:
        out.emit_json(st.to_dict())
    else:
        out.echo(f"{out.dot('ok')} sealed {st.path}")
        out.echo(f"    sha256 {st.actual}")


@vault_app.command("verify")
def vault_verify(
    root: Optional[Path] = ROOT_OPT, json_out: bool = typer.Option(False, "--json")
) -> None:
    """Re-check the vault checksum. What ``witness.yml`` runs nightly."""
    project = _project(root)
    try:
        st = vaultx.verify(project.vault_dir)
    except NzError as exc:
        _fail(exc, json_out)
        return
    if json_out:
        out.emit_json(st.to_dict())
    else:
        out.echo(f"{out.dot('ok')} vault intact — {short(st.actual or '', 16)}")


@app.command()
def init(
    root: Optional[Path] = ROOT_OPT,
    json_out: bool = typer.Option(False, "--json"),
) -> None:
    """§11.2 — decompose the sealed vault into schema.sql, collections and sources."""
    project = _project(root)
    try:
        report = bootstrapx.init_from_vault(project)
    except NzError as exc:
        _fail(exc, json_out)
        return
    if json_out:
        out.emit_json(report.to_dict())
        return
    out.heading(f"exported {len(report.collections)} collection(s) from the vault")
    for name in report.collections:
        out.echo(f"    {name}")
    if report.skipped_user_tables:
        out.echo(out.paint(f"  user tables (schema only): {len(report.skipped_user_tables)}", out.DIM))
    if report.uncovered_tables:
        out.echo(out.paint(f"  ⚠ tables with rows and no collection: {report.uncovered_tables}", out.PEND))
    out.echo()
    out.echo("Next: `nz build --against-vault` — the round trip must be lossless")
    out.echo("before any change is authored (§11.3).")


@source_app.command("export")
def source_export(
    root: Optional[Path] = ROOT_OPT,
    source: Optional[Path] = typer.Option(
        None, "--from", help="Database to export from (defaults to the vault)."
    ),
    collection: list[str] = typer.Option([], "--collection", "-c"),
    json_out: bool = typer.Option(False, "--json"),
) -> None:
    """Rewrite records.ndjson from a database, canonically formatted."""
    project = _project(root)
    db_path = source or vaultx.corpus_path(project.vault_dir)
    try:
        reports = bootstrapx.export_sources(project, db_path, collections=list(collection) or None)
    except NzError as exc:
        _fail(exc, json_out)
        return
    if json_out:
        out.emit_json({"source": str(db_path), "collections": [r.to_dict() for r in reports]})
        return
    out.table(
        [
            [r.collection, r.rows, f"{r.bytes / 1024:.0f} KiB", short(r.content_hash), "yes" if r.changed else ""]
            for r in reports
        ],
        ["collection", "rows", "size", "hash", "changed"],
    )


# --- build / verify / promote -------------------------------------------------


@app.command()
def build(
    root: Optional[Path] = ROOT_OPT,
    against_vault: bool = typer.Option(
        False, "--against-vault", help="Also verify the vault and compare the candidate to it."
    ),
    confirm_protected: bool = typer.Option(False, "--confirm-protected"),
    collection: list[str] = typer.Option([], "--collection", "-c", help="Scoped build (pr.yml)."),
    skip_rules: bool = typer.Option(False, "--skip-rules"),
    json_out: bool = typer.Option(False, "--json"),
) -> None:
    """Six stages, each a hard gate (§6). Leaves the candidate on disk either way."""
    project = _project(root)
    try:
        result = run_build(
            project,
            confirm_protected=confirm_protected,
            skip_vault=not against_vault,
            only_collections=list(collection),
            skip_rules=skip_rules,
        )
    except NzError as exc:
        _fail(exc, json_out)
        return

    vault_report = None
    if against_vault and result.ok and result.candidate:
        vault_conn = dbx.open_vault(vaultx.corpus_path(project.vault_dir))
        cand_conn = dbx.open_readonly(result.candidate)
        try:
            vault_report = compare_against_vault(vault_conn, cand_conn, project.specs())
        finally:
            vault_conn.close()
            cand_conn.close()

    payload = result.to_dict()
    if vault_report is not None:
        payload["against_vault"] = vault_report.to_dict()

    if json_out:
        out.emit_json(payload)
        raise typer.Exit(0 if result.ok and (vault_report is None or vault_report.ok) else 1)

    _print_build(result, vault_report)
    raise typer.Exit(0 if result.ok and (vault_report is None or vault_report.ok) else 1)


def _print_build(result, vault_report) -> None:
    for stage in result.stages:
        state = "unknown" if stage.skipped else ("ok" if stage.ok else "blocking")
        label = f"{stage.name:<9}"
        note = "skipped" if stage.skipped else stage.error or _stage_note(stage)
        out.echo(f"{out.dot(state)} {label} {out.paint(f'{stage.seconds * 1000:>6.0f}ms', out.DIM)}  {note}")

    if result.outcomes:
        out.echo()
        out.heading("declared vs actual")
        rows = []
        for outcome in result.outcomes:
            for name in sorted(outcome.actual):
                declared, actual = outcome.declared.get(name, {}), outcome.actual[name]
                rows.append(
                    [
                        outcome.change.id,
                        name,
                        declared.get("rows_delta", "—"),
                        actual["rows_delta"],
                        declared.get("rows_after", "—"),
                        actual["rows_after"],
                    ]
                )
        out.table(rows, ["change", "collection", "Δ decl", "Δ real", "after decl", "after real"])

    if result.validation and result.validation.total_failures:
        out.echo()
        out.echo(as_text(result.validation))
    if result.guard and result.guard.findings:
        out.echo()
        out.heading("guard")
        for finding in result.guard.findings:
            out.echo(f"{out.dot('blocking' if finding.blocking else 'pending')} "
                     f"{finding.collection} [{finding.check}] {finding.detail}")
    if vault_report is not None:
        out.echo()
        state = "ok" if vault_report.ok else "blocking"
        out.echo(f"{out.dot(state)} round trip against the vault: "
                 f"{len(vault_report.checked)} collection(s), {len(vault_report.findings)} mismatch(es)")
        for finding in vault_report.findings:
            out.echo(f"    {finding.collection} [{finding.check}] {finding.detail}")

    out.echo()
    if result.ok:
        out.echo(f"{out.dot('pending')} candidate {result.candidate}")
        out.echo(f"    sha256 {result.artifact_hash}")
        out.echo("    not promoted — run `nz promote` to make it out/current")
    else:
        out.echo(out.paint(f"✗ build failed at stage `{result.failed_stage}`: {result.error}", out.BLOCK))


def _stage_note(stage) -> str:
    return ", ".join(f"{k}={v}" for k, v in stage.detail.items() if k != "error_detail")


@app.command()
def promote(
    root: Optional[Path] = ROOT_OPT,
    candidate: Optional[Path] = typer.Option(
        None,
        "--candidate",
        help="Promote a pre-built candidate. Rebuilds to derive the receipt.",
    ),
    confirm_protected: bool = typer.Option(False, "--confirm-protected"),
    json_out: bool = typer.Option(False, "--json"),
) -> None:
    """Validate, then atomically swap the candidate into out/ (§7).

    The build always runs, even when ``--candidate`` names a file: the receipt is
    derived from the pipeline, and promoting an artifact whose receipt nobody
    computed would put a file in ``out/`` that ``build.json`` does not describe.
    ``--candidate`` then asserts the rebuild produced that exact artifact.
    """
    project = _project(root)
    try:
        result = run_build(project, confirm_protected=confirm_protected)
        if not result.ok:
            raise NzError(
                f"refusing to promote: build failed at `{result.failed_stage}`",
                detail=result.error,
            )
        if candidate is not None:
            from ..core.hash import sha256_file

            asked = sha256_file(candidate)
            if asked != result.artifact_hash:
                raise NzError(
                    "the named candidate is not what the sources build to",
                    candidate=str(candidate),
                    candidate_hash=asked,
                    rebuilt_hash=result.artifact_hash,
                )
        candidate, receipt = result.candidate, result.receipt
        promoted = do_promote(candidate, project.out_dir, receipt=receipt, retain=project.config.retain)
    except NzError as exc:
        _fail(exc, json_out)
        return
    if json_out:
        out.emit_json(promoted.to_dict())
        return
    out.echo(f"{out.dot('ok')} current  -> {promoted.artifact.name}")
    if promoted.previous:
        out.echo(f"{out.dot('unknown')} previous -> {promoted.previous.name}")


@app.command()
def rollback(
    root: Optional[Path] = ROOT_OPT, json_out: bool = typer.Option(False, "--json")
) -> None:
    """Repoint current at previous. A pointer move, not a rebuild."""
    project = _project(root)
    try:
        result = do_rollback(project.out_dir)
    except NzError as exc:
        _fail(exc, json_out)
        return
    if json_out:
        out.emit_json(result.to_dict())
    else:
        out.echo(f"{out.dot('ok')} current -> {result.artifact.name}")


@app.command()
def verify(
    root: Optional[Path] = ROOT_OPT,
    against_vault: bool = typer.Option(False, "--against-vault"),
    json_out: bool = typer.Option(False, "--json"),
) -> None:
    """§11.3 — build from sources and prove the result matches the vault, per collection."""
    if not against_vault:
        out.echo("nothing to verify against; pass --against-vault")
        raise typer.Exit(2)
    build(root=root, against_vault=True, confirm_protected=False, collection=[],
          skip_rules=False, json_out=json_out)


# --- validate ----------------------------------------------------------------


@app.command()
def validate(
    root: Optional[Path] = ROOT_OPT,
    target: Optional[Path] = typer.Option(None, "--db", help="Database to validate (default out/current)."),
    collection: list[str] = typer.Option([], "--collection", "-c"),
    only: list[str] = typer.Option([], "--rule"),
    annotate: Optional[str] = typer.Option(None, "--annotate", help="`github` for inline annotations."),
    json_out: bool = typer.Option(False, "--json"),
) -> None:
    """Run the rule set. Blocking failures exit non-zero; advisory ones do not."""
    project = _project(root)
    db_path = target or project.current_link
    try:
        conn = dbx.open_readonly(db_path)
    except NzError as exc:
        _fail(exc, json_out)
        return
    try:
        report = run_rules(
            conn,
            project.specs(),
            rules_dir=project.rules_dir,
            only=list(only),
            collections=list(collection),
        )
    except NzError as exc:
        conn.close()
        _fail(exc, json_out)
        return
    finally:
        if not conn:  # pragma: no cover - defensive
            pass
    conn.close()

    if annotate == "github":
        for line in as_github_annotations(report, project.specs(), project.root):
            out.echo(line)
    if json_out:
        out.emit_json(report.to_dict())
    elif annotate != "github":
        out.echo(as_text(report))
    raise typer.Exit(0 if report.ok else 1)


@app.command("rules")
def list_rules(
    root: Optional[Path] = ROOT_OPT, json_out: bool = typer.Option(False, "--json")
) -> None:
    """List the discovered rules."""
    project = _project(root)
    rules = discover_rules(project.rules_dir)
    if json_out:
        out.emit_json(
            [
                {"id": r.id, "scope": r.scope, "severity": r.severity, "doc": r.doc.strip().splitlines()[0] if r.doc.strip() else ""}
                for r in rules
            ]
        )
        return
    out.table(
        [[r.id, r.scope, r.severity, (r.doc.strip().splitlines() or [""])[0]] for r in rules],
        ["rule", "scope", "severity", "what it asserts"],
    )


# --- describe / query / diff --------------------------------------------------


@app.command()
def describe(
    root: Optional[Path] = ROOT_OPT, json_out: bool = typer.Option(False, "--json")
) -> None:
    """Collections, versions, hashes, build receipt. Same payload as ``nimaz.describe``."""
    project = _project(root)
    specs = project.specs()
    receipt = manifest.read_receipt(project.receipt_path)
    payload = {
        "root": str(project.root),
        "artifact": (receipt or {}).get("artifact"),
        "built": (receipt or {}).get("built"),
        "collections": {
            name: {
                "version": spec.version,
                "kind": spec.kind,
                "key": list(spec.key),
                "table": spec.source.table,
                "rows_min": spec.floors.rows_min,
                "protected": list(spec.protected),
                "depends_on": spec.depends_on,
                **((receipt or {}).get("collections", {}).get(name, {})),
            }
            for name, spec in sorted(specs.items())
        },
    }
    if json_out:
        out.emit_json(payload)
        return
    out.table(
        [
            [name, c.get("version"), c.get("rows", "—"), short(str(c.get("hash", "—"))), c.get("kind")]
            for name, c in payload["collections"].items()
        ],
        ["collection", "version", "rows", "hash", "kind"],
    )


@app.command()
def query(
    sql: str = typer.Argument(..., help="Read-only SQL."),
    root: Optional[Path] = ROOT_OPT,
    target: Optional[Path] = typer.Option(None, "--db"),
    limit: int = typer.Option(200, "--limit"),
    json_out: bool = typer.Option(False, "--json"),
) -> None:
    """Read-only SQL against out/current, row-capped. Same as ``nimaz.query``."""
    project = _project(root)
    db_path = target or project.current_link
    try:
        conn = dbx.open_readonly(db_path)
    except NzError as exc:
        _fail(exc, json_out)
        return
    try:
        rows = [dict(r) for r in conn.execute(sql).fetchmany(limit)]
    except sqlite3.Error as exc:
        conn.close()
        _fail(NzError("query failed", detail=str(exc)), json_out)
        return
    conn.close()
    if json_out:
        out.emit_json({"rows": rows, "count": len(rows), "capped": len(rows) == limit})
        return
    if not rows:
        out.echo("(no rows)")
        return
    headers = list(rows[0])
    out.table([[r.get(h) for h in headers] for r in rows], headers)


# --- changes ------------------------------------------------------------------


@change_app.command("new")
def change_new(
    title: str = typer.Argument(...),
    collection: list[str] = typer.Option(..., "--collection", "-c", help="Collections this touches."),
    sql: str = typer.Option("", "--sql", help="up.sql body; omit to get a stub."),
    down: str = typer.Option("", "--down"),
    bump: str = typer.Option("patch", "--bump"),
    author: str = typer.Option("", "--author"),
    origin: str = typer.Option("hand", "--origin"),
    rationale: str = typer.Option("", "--rationale"),
    root: Optional[Path] = ROOT_OPT,
    json_out: bool = typer.Option(False, "--json"),
) -> None:
    """Write a change directory. The console and the agent land on this same code."""
    import getpass

    project = _project(root)
    project.ensure_dirs()
    try:
        change = write_change(
            project.changes_dir,
            title=title,
            author=author or getpass.getuser(),
            origin=origin,
            collections={c: CollectionChange(bump=bump, expect=Expect()) for c in collection},
            up_sql=sql or "-- SQL against the working database built from data/collections/*\n",
            down_sql=down,
            rationale=rationale,
        )
    except NzError as exc:
        _fail(exc, json_out)
        return
    if json_out:
        out.emit_json(change.to_dict())
        return
    out.echo(f"{out.dot('pending')} {change.id}")
    out.echo(f"    {change.dir}")
    out.echo("    fill in expect{} in change.yaml, then `nz build`")


@change_app.command("list")
def change_list(
    root: Optional[Path] = ROOT_OPT, json_out: bool = typer.Option(False, "--json")
) -> None:
    """Pending changes, in the order they will be applied."""
    project = _project(root)
    pending = load_pending(project.changes_dir)
    if json_out:
        out.emit_json([c.to_dict() for c in pending])
        return
    out.table(
        [[c.id, c.origin, ", ".join(sorted(c.collections)), c.title] for c in pending],
        ["id", "origin", "collections", "title"],
    )


@change_app.command("fold")
def change_fold(
    root: Optional[Path] = ROOT_OPT,
    dry_run: bool = typer.Option(False, "--dry-run"),
    json_out: bool = typer.Option(False, "--json"),
) -> None:
    """Replay changes into the sources — only if the artifact hash is unchanged (§15)."""
    project = _project(root)
    try:
        report = do_fold(project, dry_run=dry_run)
    except NzError as exc:
        _fail(exc, json_out)
        return
    if json_out:
        out.emit_json(report.to_dict())
        return
    out.echo(f"{out.dot('ok' if report.committed else 'pending')} "
             f"folded {len(report.folded)} change(s), rewrote {len(report.rewritten)} source file(s)")
    out.echo(f"    hash before {short(report.hash_before or '—', 12)} / after {short(report.hash_after or '—', 12)}")


# --- deps ---------------------------------------------------------------------


@deps_app.command("check")
def deps_check(
    root: Optional[Path] = ROOT_OPT, json_out: bool = typer.Option(False, "--json")
) -> None:
    """Every depends_on constraint must be satisfiable by this build (§14)."""
    project = _project(root)
    errors = list(check_dependencies(project.specs()))
    if json_out:
        out.emit_json({"ok": not errors, "errors": errors})
    else:
        if errors:
            for line in errors:
                out.echo(f"{out.dot('blocking')} {line}")
        else:
            out.echo(f"{out.dot('ok')} all dependency constraints satisfied")
    raise typer.Exit(1 if errors else 0)


@app.command()
def ui(root: Optional[Path] = ROOT_OPT) -> None:
    """Launch the terminal console (§10). Requires the `tui` extra."""
    from ..ui.app import run

    run(_project(root))


def main() -> None:  # pragma: no cover - entry point
    try:
        app()
    except NzError as exc:
        out.echo(out.paint(f"✗ {exc.message}", out.BLOCK))
        sys.exit(exc.exit_code)


if __name__ == "__main__":  # pragma: no cover
    main()
