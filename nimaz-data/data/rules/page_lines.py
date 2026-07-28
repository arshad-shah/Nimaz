"""Mushaf layout rules — the 16-line IndoPak page grid.

The layout is what makes a page render correctly; a missing or duplicated line
number is invisible in a diff and very visible on a device.
"""

from __future__ import annotations

from collections import Counter
from typing import Iterable

from nimaz_data.rules import Failure, rule

LINES_PER_PAGE = 16


@rule(id="page.lines-complete", scope="mushaf.*", severity="blocking")
def lines_complete(ctx) -> Iterable[Failure]:
    """Every page carries lines 1..16, each exactly once."""
    for (page,), rows in ctx.grouped("page"):
        counts = Counter(r.get("line") for r in rows)
        missing = [n for n in range(1, LINES_PER_PAGE + 1) if n not in counts]
        duplicated = sorted(n for n, c in counts.items() if c > 1)
        out_of_range = sorted(
            n for n in counts if not isinstance(n, int) or not 1 <= n <= LINES_PER_PAGE
        )
        if missing:
            yield Failure(key=f"page {page}", detail=f"missing line(s) {_brief(missing)}")
        if duplicated:
            yield Failure(key=f"page {page}", detail=f"duplicate line(s) {_brief(duplicated)}")
        if out_of_range:
            yield Failure(key=f"page {page}", detail=f"line(s) outside 1..16: {_brief(out_of_range)}")


@rule(id="page.word-range", scope="mushaf.*", severity="blocking")
def word_range(ctx) -> Iterable[Failure]:
    """A line's first word position never exceeds its last."""
    for row in ctx.rows:
        first, last = row.get("first_word_position"), row.get("last_word_position")
        if first is None or last is None:
            continue
        if first > last:
            yield Failure(
                key=ctx.key_of(row),
                detail=f"first_word_position {first} > last_word_position {last}",
            )


@rule(id="page.contiguous", scope="mushaf.*", severity="advisory")
def pages_contiguous(ctx) -> Iterable[Failure]:
    """Page numbers form an unbroken run from 1."""
    pages = sorted({r.get("page") for r in ctx.rows if isinstance(r.get("page"), int)})
    if not pages:
        return
    expected = set(range(pages[0], pages[-1] + 1))
    gaps = sorted(expected - set(pages))
    if gaps:
        yield Failure(key=None, detail=f"page gap(s): {_brief(gaps)}")
    if pages[0] != 1:
        yield Failure(key=None, detail=f"pages start at {pages[0]}, not 1")


def _brief(values: list, limit: int = 8) -> str:
    head = ", ".join(str(v) for v in values[:limit])
    return head if len(values) <= limit else f"{head}, … (+{len(values) - limit})"
