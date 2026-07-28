"""``nz change fold`` (§5, §15) — replay changes into the sources.

Migration squashing, with the audit trail kept. The safety property is stated in
§15: a fold is only committed if the artifact hash is identical before and after.
A fold that changes the output is a bug, so this refuses to move anything until
it has rebuilt and compared.
"""

from __future__ import annotations

import shutil
from dataclasses import dataclass, field
from pathlib import Path

from ..build.pipeline import build
from ..core.errors import ChangeError
from ..core.hash import records_hash
from ..core.ndjson import read_records, write_records
from ..core.project import Project
from ..core import db as dbx
from .model import load_pending


@dataclass
class FoldReport:
    folded: list[str] = field(default_factory=list)
    rewritten: list[str] = field(default_factory=list)
    hash_before: str | None = None
    hash_after: str | None = None
    committed: bool = False

    def to_dict(self) -> dict:
        return {
            "folded": self.folded,
            "rewritten": self.rewritten,
            "hash_before": self.hash_before,
            "hash_after": self.hash_after,
            "committed": self.committed,
        }


def fold(project: Project, *, dry_run: bool = False) -> FoldReport:
    pending = load_pending(project.changes_dir)
    report = FoldReport(folded=[c.id for c in pending])
    if not pending:
        return report

    first = build(project, confirm_protected=True)
    if not first.ok:
        raise ChangeError(
            "cannot fold: the build with these changes does not pass",
            stage=first.failed_stage,
            detail=first.error,
        )
    report.hash_before = first.artifact_hash

    # Rewrite each source file from the candidate — this is the fold itself.
    specs = project.specs()
    conn = dbx.open_readonly(first.candidate)
    originals: dict[str, bytes] = {}
    try:
        for name in sorted(specs):
            spec = specs[name]
            rows = dbx.read_collection(conn, spec)
            digest = records_hash(rows, spec.key)
            existing = (
                records_hash(read_records(spec.records_path), spec.key)
                if spec.records_path.exists()
                else None
            )
            if digest == existing:
                continue
            originals[name] = (
                spec.records_path.read_bytes() if spec.records_path.exists() else b""
            )
            write_records(spec.records_path, rows, spec.key)
            report.rewritten.append(name)
    finally:
        conn.close()

    # Rebuild from the rewritten sources with the changes moved out of the way.
    staged = _stage_out(project, [c.id for c in pending])
    try:
        second = build(project)
        report.hash_after = second.artifact_hash
        identical = second.ok and second.artifact_hash == report.hash_before
        if dry_run or not identical:
            _restore(project, staged, originals, specs)
            if not identical:
                raise ChangeError(
                    "fold changed the artifact — refusing to commit it",
                    hash_before=report.hash_before,
                    hash_after=report.hash_after,
                    stage=second.failed_stage,
                    detail=second.error,
                )
            return report
    except ChangeError:
        raise
    except Exception:
        _restore(project, staged, originals, specs)
        raise

    report.committed = True
    return report


def _stage_out(project: Project, ids: list[str]) -> list[tuple[Path, Path]]:
    project.applied_dir.mkdir(parents=True, exist_ok=True)
    moved = []
    for cid in ids:
        src = project.changes_dir / cid
        dst = project.applied_dir / cid
        if dst.exists():
            raise ChangeError("change already present in applied/", id=cid)
        shutil.move(str(src), str(dst))
        moved.append((src, dst))
    return moved


def _restore(project: Project, staged: list[tuple[Path, Path]], originals: dict, specs: dict) -> None:
    for src, dst in staged:
        if dst.exists():
            shutil.move(str(dst), str(src))
    for name, blob in originals.items():
        spec = specs[name]
        if blob:
            spec.records_path.write_bytes(blob)
        elif spec.records_path.exists():
            spec.records_path.unlink()
