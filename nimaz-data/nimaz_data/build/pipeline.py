"""``nz build`` — six stages, each a hard gate (§6).

    1  verify vault      corpus.db hash matches, opened immutable
    2  load sources      NDJSON -> typed records, schema checked
    3  apply changes     ordered; expect{} enforced per change
    4  compile           candidate DB written to .build/, deterministic
    5  validate          full rule run against the candidate
    6  guard             candidate vs receipts/genesis, floors, protected fields

Failure at any stage leaves ``out/current`` untouched and the candidate on disk
for inspection. Nothing partial is ever visible to the app build.
"""

from __future__ import annotations

import time
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Sequence

from ..changes.apply import ChangeOutcome, apply_change
from ..changes.model import Change, load_pending
from ..core import db as dbx, manifest, vault as vaultx
from ..core.errors import BuildError, NzError
from ..core.hash import short
from ..core.project import Project
from ..core.spec import CollectionSpec, check_dependencies
from ..rules.runner import ValidationReport, run_rules
from .compile import compile_candidate
from .guard import GuardReport, run_guard
from .sources import create_working, load_sources

STAGES = ("vault", "sources", "changes", "compile", "validate", "guard")


@dataclass
class Stage:
    name: str
    ok: bool = False
    skipped: bool = False
    seconds: float = 0.0
    detail: dict = field(default_factory=dict)
    error: str | None = None

    def to_dict(self) -> dict:
        return {
            "stage": self.name,
            "ok": self.ok,
            "skipped": self.skipped,
            "seconds": round(self.seconds, 3),
            "error": self.error,
            **self.detail,
        }


@dataclass
class BuildResult:
    run_id: str
    stages: list[Stage] = field(default_factory=list)
    candidate: Path | None = None
    artifact_hash: str | None = None
    entries: list[manifest.Entry] = field(default_factory=list)
    outcomes: list[ChangeOutcome] = field(default_factory=list)
    validation: ValidationReport | None = None
    guard: GuardReport | None = None
    receipt: dict | None = None
    failed_stage: str | None = None
    error: str | None = None

    @property
    def ok(self) -> bool:
        return self.failed_stage is None

    def to_dict(self) -> dict:
        return {
            "ok": self.ok,
            "run_id": self.run_id,
            "candidate": str(self.candidate) if self.candidate else None,
            "artifact": f"sha256:{self.artifact_hash}" if self.artifact_hash else None,
            "failed_stage": self.failed_stage,
            "error": self.error,
            "stages": [s.to_dict() for s in self.stages],
            "changes": [o.to_dict() for o in self.outcomes],
            "validation": self.validation.to_dict() if self.validation else None,
            "guard": self.guard.to_dict() if self.guard else None,
            "receipt": self.receipt,
        }


def run_id(now: datetime | None = None) -> str:
    """Microsecond precision so two builds in the same second get their own candidate.

    The run id names a file in ``.build/`` and nothing else — it is deliberately
    absent from the artifact, whose identity is its content hash.
    """
    return (now or datetime.now(timezone.utc)).strftime("%Y%m%dT%H%M%S%f")


def build(
    project: Project,
    *,
    confirm_protected: bool = False,
    skip_vault: bool = True,
    only_collections: Sequence[str] = (),
    skip_rules: bool = False,
    now: datetime | None = None,
) -> BuildResult:
    """Run the pipeline. Returns a result; raises only on programming errors.

    ``skip_vault`` defaults to True because day to day the guard runs against the
    genesis chain and the vault is not in the hot path (§15). ``nz build
    --against-vault`` is the bootstrap and nightly-witness path.
    """
    rid = run_id(now)
    result = BuildResult(run_id=rid)
    project.ensure_dirs()

    specs_all = project.specs()
    if only_collections:
        wanted = set(only_collections)
        unknown = sorted(wanted - set(specs_all))
        if unknown:
            raise BuildError("unknown collection", collections=unknown)
        specs = {k: v for k, v in specs_all.items() if k in wanted}
    else:
        specs = specs_all

    if not specs:
        raise BuildError(
            "no collections found — run `nz init` against a sealed vault first",
            path=str(project.collections_dir),
        )

    dep_errors = list(check_dependencies(specs))
    if dep_errors and not only_collections:
        raise BuildError("dependency constraints unsatisfied", errors=dep_errors)

    working = None
    try:
        # --- 1 vault -------------------------------------------------------
        with _stage(result, "vault") as st:
            if skip_vault:
                st.skipped = True
                st.detail = {"reason": "guarding against the genesis chain"}
            else:
                vault_status = vaultx.verify(project.vault_dir)
                st.detail = {
                    "sha256": short(vault_status.actual or "", 12),
                    "mode": "444",
                    "touched": vault_status.modified,
                }

        # --- 2 sources -----------------------------------------------------
        with _stage(result, "sources") as st:
            working = create_working(project.schema_sql())
            reports = load_sources(working, specs.values())
            st.detail = {
                "collections": len(reports),
                "rows": sum(r.rows for r in reports),
            }

        # --- 3 changes -----------------------------------------------------
        pending: list[Change] = load_pending(project.changes_dir)
        with _stage(result, "changes") as st:
            for change in pending:
                result.outcomes.append(
                    apply_change(
                        working, change, specs, confirm_protected=confirm_protected
                    )
                )
            st.detail = {"applied": len(pending), "ids": [c.id for c in pending]}

        # --- 4 compile -----------------------------------------------------
        candidate_path = project.build_dir / f"candidate-{rid}.db"
        with _stage(result, "compile") as st:
            artifact_hash, entries = compile_candidate(
                working,
                schema_sql=project.schema_sql(),
                specs=specs,
                out_path=candidate_path,
                user_version=project.config.user_version,
            )
            result.candidate = candidate_path
            result.artifact_hash = artifact_hash
            result.entries = entries
            st.detail = {
                "artifact": f"sha256:{short(artifact_hash)}",
                "bytes": candidate_path.stat().st_size,
                "path": str(candidate_path),
            }

        candidate = dbx.open_readonly(candidate_path)
        try:
            # --- 5 validate -------------------------------------------------
            with _stage(result, "validate") as st:
                if skip_rules:
                    st.skipped = True
                else:
                    report = run_rules(candidate, specs, rules_dir=project.rules_dir)
                    result.validation = report
                    st.detail = {
                        "rules": report.rules_run,
                        "failures": report.total_failures,
                        "blocking": len(report.blocking),
                    }
                    report.raise_if_blocking()

            # --- 6 guard ----------------------------------------------------
            with _stage(result, "guard") as st:
                guard = run_guard(
                    candidate,
                    specs,
                    entries=result.entries,
                    pending=pending,
                    genesis_path=project.genesis_path,
                    previous_receipt=manifest.read_receipt(project.receipt_path),
                )
                result.guard = guard
                st.detail = {"findings": len(guard.findings), "blocking": len(guard.blocking)}
                guard.raise_if_blocking()
        finally:
            candidate.close()

        previous = manifest.read_receipt(project.receipt_path)
        result.receipt = manifest.receipt(
            artifact_hash=result.artifact_hash or "",
            built=(now or datetime.now(timezone.utc)).replace(microsecond=0).isoformat(),
            entries=result.entries,
            parent=(previous or {}).get("artifact"),
        )
    except NzError as exc:
        result.error = exc.message
        if result.stages and not result.stages[-1].ok:
            result.failed_stage = result.stages[-1].name
        else:
            result.failed_stage = "build"
        result.stages[-1].detail.setdefault("error_detail", exc.detail)
    finally:
        if working is not None:
            working.close()

    return result


class _stage:
    """Context manager that times a stage and records its failure without swallowing it."""

    def __init__(self, result: BuildResult, name: str) -> None:
        self.result = result
        self.stage = Stage(name)
        self.start = 0.0

    def __enter__(self) -> Stage:
        self.start = time.perf_counter()
        self.result.stages.append(self.stage)
        return self.stage

    def __exit__(self, exc_type, exc, tb) -> bool:
        self.stage.seconds = time.perf_counter() - self.start
        if exc is None:
            self.stage.ok = True
            return False
        self.stage.ok = False
        self.stage.error = getattr(exc, "message", str(exc))
        return False
