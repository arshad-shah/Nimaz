"""Running the rule set against a candidate (§8)."""

from __future__ import annotations

import sqlite3
from dataclasses import dataclass, field
from pathlib import Path
from typing import Sequence

from ..core.errors import RuleError
from ..core.spec import CollectionSpec
from .registry import ADVISORY, BLOCKING, Ctx, Failure, Rule, discover


@dataclass
class RuleResult:
    rule_id: str
    collection: str
    severity: str
    failures: list[Failure] = field(default_factory=list)
    error: str | None = None

    @property
    def blocking(self) -> bool:
        return self.severity == BLOCKING and bool(self.failures or self.error)

    def to_dict(self, *, sample: int = 25) -> dict:
        return {
            "rule": self.rule_id,
            "collection": self.collection,
            "severity": self.severity,
            "count": len(self.failures),
            "error": self.error,
            "failures": [
                {"key": f.key_str(), "detail": f.detail, "fixable": f.fix is not None}
                for f in self.failures[:sample]
            ],
            "truncated": max(0, len(self.failures) - sample),
        }


@dataclass
class ValidationReport:
    results: list[RuleResult] = field(default_factory=list)
    rules_run: int = 0

    @property
    def blocking(self) -> list[RuleResult]:
        return [r for r in self.results if r.blocking]

    @property
    def advisory(self) -> list[RuleResult]:
        return [r for r in self.results if r.severity == ADVISORY and r.failures]

    @property
    def ok(self) -> bool:
        return not self.blocking

    @property
    def total_failures(self) -> int:
        return sum(len(r.failures) for r in self.results)

    def to_dict(self) -> dict:
        return {
            "ok": self.ok,
            "rules_run": self.rules_run,
            "failures": self.total_failures,
            "blocking": len(self.blocking),
            "results": [r.to_dict() for r in self.results if r.failures or r.error],
        }

    def raise_if_blocking(self) -> None:
        if self.ok:
            return
        raise RuleError(
            "blocking rules failed",
            rules=[r.rule_id for r in self.blocking],
            detail=[r.to_dict(sample=5) for r in self.blocking],
        )


def run_rules(
    conn: sqlite3.Connection,
    specs: dict[str, CollectionSpec],
    *,
    rules_dir: Path,
    only: Sequence[str] = (),
    collections: Sequence[str] = (),
) -> ValidationReport:
    """Run every rule whose scope matches, over every collection in scope.

    Rules are run sequentially against a single read-only connection. §8 calls
    for a parallel pool; sequential is what the current corpus needs and it keeps
    a rule failure attributable to one rule rather than to a worker. The Ctx API
    is already the only thing a rule touches, so parallelising later changes this
    function and nothing else.
    """
    all_rules: list[Rule] = discover(rules_dir)
    if only:
        wanted = set(only)
        all_rules = [r for r in all_rules if r.id in wanted]
        missing = sorted(wanted - {r.id for r in all_rules})
        if missing:
            raise RuleError("unknown rule id", rules=missing)

    scope_names = set(collections) if collections else set(specs)
    report = ValidationReport()

    for rule_obj in all_rules:
        matched = False
        for name in sorted(scope_names):
            spec = specs.get(name)
            if spec is None:
                raise RuleError("unknown collection", collection=name)
            if not rule_obj.applies_to_spec(spec):
                continue
            matched = True
            result = RuleResult(rule_obj.id, name, rule_obj.severity)
            ctx = Ctx(collection=spec, conn=conn, specs=specs)
            try:
                result.failures = list(rule_obj.fn(ctx))
            except Exception as exc:  # a rule that crashes is a failing rule
                result.error = f"{type(exc).__name__}: {exc}"
            report.results.append(result)
        if matched:
            report.rules_run += 1

    return report
