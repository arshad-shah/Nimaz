"""Helpers shared by rule modules. Leading underscore keeps it out of discovery."""

from __future__ import annotations

from typing import Iterable, Iterator, Sequence, TypeVar

T = TypeVar("T")


def overlapping_pairs(
    spans: Sequence[dict], start: str = "start", end: str = "end"
) -> Iterator[tuple[dict, dict]]:
    """Every pair of half-open [start, end) spans that overlap, each pair once."""
    ordered = sorted(spans, key=lambda s: (s.get(start) or 0, s.get(end) or 0))
    for i, a in enumerate(ordered):
        a_end = a.get(end)
        if a_end is None:
            continue
        for b in ordered[i + 1 :]:
            b_start = b.get(start)
            if b_start is None or b_start >= a_end:
                break
            yield a, b


def consecutive(values: Iterable[int]) -> Iterator[tuple[int, int]]:
    """Yield (expected, actual) for the first gap in a 1..N run."""
    for expected, actual in enumerate(sorted(values), start=1):
        if expected != actual:
            yield expected, actual
            return


def truthy(value: object) -> bool:
    return bool(value) and str(value).strip() != ""
