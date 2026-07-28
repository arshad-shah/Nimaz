"""Shape rules for the Quran collections.

No corpus constants live in this file. "There are 6236 ayahs" is a fact about the
data, so it is declared in ``collection.yaml`` as ``floors.rows_exact`` and
asserted generically; a translation's completeness is checked against the Quran
collection actually in the build rather than against a number typed twice.
"""

from __future__ import annotations

from typing import Iterable

from nimaz_data.rules import Failure, rule

QURAN_KIND = "quran"


@rule(id="rows.exact", scope="*", severity="blocking")
def rows_exact(ctx) -> Iterable[Failure]:
    """A collection that declares an exact row count has it."""
    expected = ctx.collection.floors.rows_exact
    if expected is None:
        return
    if len(ctx.rows) != expected:
        yield Failure(
            key=None,
            detail=f"{len(ctx.rows)} rows, but collection.yaml declares exactly {expected}",
        )


@rule(id="quran.ayah-sequence", scope="kind:quran", severity="blocking")
def ayah_sequence(ctx) -> Iterable[Failure]:
    """Within a surah, ayah numbers run 1..N with no gaps and no repeats."""
    for (surah,), rows in ctx.grouped("surah_id"):
        numbers = sorted(r.get("number_in_surah") for r in rows)
        expected = list(range(1, len(numbers) + 1))
        if numbers == expected:
            continue
        gaps = sorted(set(expected) - set(numbers))
        dupes = sorted({n for n in numbers if numbers.count(n) > 1})
        detail = []
        if gaps:
            detail.append(f"missing {gaps[:5]}")
        if dupes:
            detail.append(f"duplicated {dupes[:5]}")
        yield Failure(key=f"surah {surah}", detail="; ".join(detail) or "out of order")


@rule(id="quran.surah-known", scope="kind:quran", severity="blocking")
def surah_known(ctx) -> Iterable[Failure]:
    """Every ayah belongs to a surah that the surah index actually lists."""
    index = _collections_of_kind(ctx, "quran-index")
    if not index:
        return
    known: set = set()
    for name in index:
        known.update(r.get("number") or r.get("id") for r in ctx.rows_of(name))
    orphans = sorted({r.get("surah_id") for r in ctx.rows} - known)
    if orphans:
        yield Failure(key=None, detail=f"ayahs reference unknown surah(s): {orphans[:10]}")


@rule(id="translation.coverage", scope="kind:translation", severity="blocking")
def translation_coverage(ctx) -> Iterable[Failure]:
    """A translation covers every ayah in the build.

    A partial translation is not a smaller translation — it is a hole the app
    renders as a blank verse, so this blocks rather than advises.
    """
    quran = _collections_of_kind(ctx, QURAN_KIND)
    if not quran:
        return
    ayah_ids = {r.get("id") for name in quran for r in ctx.rows_of(name)}
    covered = {r.get("ayah_id") for r in ctx.rows}
    missing = sorted(a for a in ayah_ids - covered if a is not None)
    extra = sorted(a for a in covered - ayah_ids if a is not None)
    if missing:
        yield Failure(
            key=None,
            detail=f"{len(missing)} ayah(s) untranslated, e.g. {missing[:8]}",
        )
    if extra:
        yield Failure(
            key=None, detail=f"{len(extra)} row(s) reference no ayah, e.g. {extra[:8]}"
        )


@rule(id="translation.non-empty", scope="kind:translation", severity="blocking")
def translation_non_empty(ctx) -> Iterable[Failure]:
    """No blank translation text."""
    for row in ctx.rows:
        text = row.get("text")
        if not isinstance(text, str) or not text.strip():
            yield Failure(key=ctx.key_of(row), detail="empty translation text")


def _collections_of_kind(ctx, kind: str) -> list[str]:
    return sorted(name for name, spec in ctx.specs.items() if spec.kind == kind)
