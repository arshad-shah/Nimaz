"""Rendering a validation report — text, JSON, and GitHub annotations (§14)."""

from __future__ import annotations

from pathlib import Path

from ..core.ndjson import line_index
from ..core.spec import CollectionSpec
from .runner import ValidationReport


def as_text(report: ValidationReport, *, sample: int = 5) -> str:
    if report.ok and not report.advisory:
        return f"{report.rules_run} rules, no failures."

    lines: list[str] = []
    for result in report.results:
        if not result.failures and not result.error:
            continue
        mark = "BLOCK" if result.blocking else "advise"
        head = f"[{mark}] {result.rule_id} on {result.collection}"
        if result.error:
            lines.append(f"{head}: rule raised — {result.error}")
            continue
        lines.append(f"{head}: {len(result.failures)} failure(s)")
        for failure in result.failures[:sample]:
            key = failure.key_str()
            lines.append(f"    {key + ': ' if key else ''}{failure.detail}")
        if len(result.failures) > sample:
            lines.append(f"    … {len(result.failures) - sample} more")
    lines.append("")
    lines.append(
        f"{report.rules_run} rules, {report.total_failures} failure(s), "
        f"{len(report.blocking)} blocking."
    )
    return "\n".join(lines)


def as_github_annotations(
    report: ValidationReport, specs: dict[str, CollectionSpec], repo_root: Path
) -> list[str]:
    """``::error file=…,line=…::…`` lines, so a failure lands on the diff (§14).

    Resolving key -> line number costs one pass over the source file per
    collection that actually failed, which is why it happens here rather than
    during validation.
    """
    out: list[str] = []
    indexes: dict[str, dict] = {}

    for result in report.results:
        if not result.failures:
            continue
        spec = specs.get(result.collection)
        if spec is None or spec.path is None:
            continue
        if result.collection not in indexes:
            try:
                indexes[result.collection] = line_index(spec.records_path, spec.key)
            except Exception:
                indexes[result.collection] = {}
        index = indexes[result.collection]

        try:
            rel = spec.records_path.resolve().relative_to(repo_root.resolve())
        except ValueError:
            rel = spec.records_path
        level = "error" if result.blocking else "warning"

        for failure in result.failures:
            line = index.get(_coerce_key(failure.key_str(), spec))
            location = f"file={rel}" + (f",line={line}" if line else "")
            detail = failure.detail.replace("\n", " ")
            out.append(f"::{level} {location},title={result.rule_id}::{detail}")
    return out


def _coerce_key(key_str: str, spec: CollectionSpec) -> tuple:
    parts = key_str.split("|") if key_str else []
    if len(parts) != len(spec.key):
        return tuple(parts)
    coerced = []
    for part in parts:
        if part == "":
            coerced.append(None)
        elif part.lstrip("-").isdigit():
            coerced.append(int(part))
        else:
            coerced.append(part)
    return tuple(coerced)
