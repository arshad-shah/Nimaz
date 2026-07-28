"""Stage 6 — the corpus guard.

Separate from the rules, and answering one question: *did we lose anything?*

The rules ask whether the data is correct. The guard asks whether it is still
all there. Those fail differently and for different reasons, so they are not the
same pass — a corpus can be entirely valid and half gone.
"""

from __future__ import annotations

import sqlite3
from dataclasses import dataclass, field
from pathlib import Path
from typing import Sequence

from ..changes.model import Change
from ..core import db as dbx, manifest, vault as vaultx
from ..core.canonical import key_repr
from ..core.diff import sort_key_strings
from ..core.errors import GuardError
from ..core.hash import records_hash
from ..core.spec import CollectionSpec


@dataclass
class GuardFinding:
    collection: str
    check: str
    detail: str
    blocking: bool = True

    def to_dict(self) -> dict:
        return {
            "collection": self.collection,
            "check": self.check,
            "detail": self.detail,
            "blocking": self.blocking,
        }


@dataclass
class GuardReport:
    findings: list[GuardFinding] = field(default_factory=list)
    checked: list[str] = field(default_factory=list)

    @property
    def blocking(self) -> list[GuardFinding]:
        return [f for f in self.findings if f.blocking]

    @property
    def ok(self) -> bool:
        return not self.blocking

    def to_dict(self) -> dict:
        return {
            "ok": self.ok,
            "checked": self.checked,
            "findings": [f.to_dict() for f in self.findings],
        }

    def raise_if_blocking(self) -> None:
        if self.ok:
            return
        raise GuardError(
            "corpus guard refused the candidate",
            findings=[f.to_dict() for f in self.blocking],
        )


def run_guard(
    candidate: sqlite3.Connection,
    specs: dict[str, CollectionSpec],
    *,
    entries: Sequence[manifest.Entry],
    pending: Sequence[Change],
    genesis_path: Path,
    previous_receipt: dict | None,
) -> GuardReport:
    report = GuardReport()
    by_name = {e.collection: e for e in entries}
    declared_shrink = _collections_declaring_loss(pending)
    declared_any = {name for c in pending for name in c.collections}

    for name in sorted(specs):
        spec = specs[name]
        entry = by_name.get(name)
        report.checked.append(name)

        if entry is None:
            report.findings.append(
                GuardFinding(name, "manifest", "collection missing from the manifest")
            )
            continue

        # 1. Floors — no exceptions, not even a declared one.
        if entry.rows < spec.floors.rows_min:
            report.findings.append(
                GuardFinding(
                    name,
                    "floor",
                    f"{entry.rows} rows is below the floor of {spec.floors.rows_min}",
                )
            )

        # 2. Nothing moved that no change claimed.
        prev = ((previous_receipt or {}).get("collections") or {}).get(name)
        if prev and prev.get("hash") != entry.content_hash and name not in declared_any:
            report.findings.append(
                GuardFinding(
                    name,
                    "undeclared-drift",
                    "content hash differs from the last receipt but no pending change "
                    "declares this collection",
                )
            )

        # 3. Row delta against the last receipt, for collections nobody declared.
        if prev and name not in declared_any and int(prev.get("rows", 0)) != entry.rows:
            report.findings.append(
                GuardFinding(
                    name,
                    "undeclared-rows",
                    f"rows moved {prev.get('rows')} -> {entry.rows} with no change declaring it",
                )
            )

        # 4. Keys present at genesis must still be here, unless a change said otherwise.
        genesis_keys = vaultx.read_genesis_keys(genesis_path, name)
        if genesis_keys is not None:
            now = {key_repr(r, spec.key) for r in dbx.read_collection(candidate, spec)}
            lost = genesis_keys - now
            if lost and name not in declared_shrink:
                sample = sort_key_strings(lost)[:10]
                report.findings.append(
                    GuardFinding(
                        name,
                        "genesis-keys",
                        f"{len(lost)} key(s) present at genesis are absent and no change "
                        f"declares a row loss for this collection; e.g. {', '.join(sample)}",
                    )
                )
            elif lost:
                report.findings.append(
                    GuardFinding(
                        name,
                        "genesis-keys",
                        f"{len(lost)} key(s) removed since genesis, declared by a change",
                        blocking=False,
                    )
                )

    # 5. A collection that was in the last receipt and is gone entirely.
    for name in sorted((previous_receipt or {}).get("collections") or {}):
        if name not in specs:
            report.findings.append(
                GuardFinding(name, "dropped", "collection was in the last receipt and is now absent")
            )

    return report


def _collections_declaring_loss(pending: Sequence[Change]) -> set[str]:
    out = set()
    for change in pending:
        for name, cc in change.collections.items():
            delta = cc.expect.rows_delta
            if delta is not None and delta < 0:
                out.add(name)
    return out


def compare_against_vault(
    vault: sqlite3.Connection,
    candidate: sqlite3.Connection,
    specs: dict[str, CollectionSpec],
) -> GuardReport:
    """§11.3 — the round-trip proof, run as a hash comparison per collection.

    This is the check that has to pass before any change is authored, and the one
    ``main.yml`` re-runs on every merge so the proof does not quietly expire.
    """
    report = GuardReport()
    for name in sorted(specs):
        spec = specs[name]
        report.checked.append(name)
        try:
            vault_rows = dbx.read_collection(vault, spec)
        except sqlite3.OperationalError as exc:
            report.findings.append(
                GuardFinding(name, "vault-read", f"cannot read from the vault: {exc}")
            )
            continue
        cand_rows = dbx.read_collection(candidate, spec)

        v_hash = records_hash(vault_rows, spec.key)
        c_hash = records_hash(cand_rows, spec.key)
        if v_hash == c_hash:
            continue

        if len(vault_rows) != len(cand_rows):
            report.findings.append(
                GuardFinding(
                    name,
                    "vault-rows",
                    f"vault has {len(vault_rows)} rows, candidate has {len(cand_rows)}",
                )
            )
        else:
            report.findings.append(
                GuardFinding(
                    name,
                    "vault-content",
                    f"same row count but different content ({v_hash[:8]} vs {c_hash[:8]})",
                )
            )
    return report
