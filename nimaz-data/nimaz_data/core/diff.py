"""Diffing — what the guard (§6) and ``nz diff`` (§9) both stand on."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Mapping, Sequence

from .canonical import key_repr


@dataclass
class CollectionDiff:
    collection: str
    rows_before: int = 0
    rows_after: int = 0
    added: list[str] = field(default_factory=list)
    removed: list[str] = field(default_factory=list)
    changed: list[dict] = field(default_factory=list)

    @property
    def rows_delta(self) -> int:
        return self.rows_after - self.rows_before

    @property
    def keys_touched(self) -> int:
        return len(self.added) + len(self.removed) + len(self.changed)

    @property
    def empty(self) -> bool:
        return not (self.added or self.removed or self.changed)

    def to_dict(self, *, sample: int = 20) -> dict:
        return {
            "collection": self.collection,
            "rows_before": self.rows_before,
            "rows_after": self.rows_after,
            "rows_delta": self.rows_delta,
            "keys_touched": self.keys_touched,
            "added": self.added[:sample],
            "removed": self.removed[:sample],
            "changed": self.changed[:sample],
            "truncated": max(
                0,
                len(self.added) + len(self.removed) + len(self.changed) - 3 * sample,
            ),
        }


def _index(records: Sequence[Mapping], key: Sequence[str]) -> dict[str, dict]:
    return {key_repr(r, key): dict(r) for r in records}


def diff_records(
    collection: str,
    before: Sequence[Mapping],
    after: Sequence[Mapping],
    key: Sequence[str],
    *,
    fields: Sequence[str] | None = None,
) -> CollectionDiff:
    """Key-set and per-field diff between two versions of a collection.

    ``fields`` narrows the comparison — the guard uses it to ask the one question
    it cares about ("did a protected field move?") without paying for a full
    field-by-field comparison of six thousand ayahs.
    """
    left, right = _index(before, key), _index(after, key)
    out = CollectionDiff(collection, rows_before=len(before), rows_after=len(after))
    out.added = sorted(right.keys() - left.keys())
    out.removed = sorted(left.keys() - right.keys())

    for k in sorted(left.keys() & right.keys()):
        a, b = left[k], right[k]
        cols = list(fields) if fields is not None else sorted(set(a) | set(b))
        deltas = {c: [a.get(c), b.get(c)] for c in cols if a.get(c) != b.get(c)}
        if deltas:
            out.changed.append({"key": k, "fields": deltas})
    return out


def diff_key_sets(
    before: set[tuple], after: set[tuple]
) -> tuple[list[tuple], list[tuple]]:
    return sorted(after - before), sorted(before - after)


def sort_key_strings(keys: set[str]) -> list[str]:
    """Sort ``2|255|3``-style key strings numerically where each part is numeric."""

    def coerce(s: str) -> tuple:
        out: list[Any] = []
        for p in s.split("|"):
            out.append((0, int(p), "") if p.lstrip("-").isdigit() else (1, 0, p))
        return tuple(out)

    return sorted(keys, key=coerce)
