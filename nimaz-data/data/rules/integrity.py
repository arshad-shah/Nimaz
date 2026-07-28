"""Structural rules that apply to every collection, whatever it holds."""

from __future__ import annotations

import unicodedata
from typing import Iterable

from nimaz_data.rules import Failure, rule


@rule(id="key.unique", scope="*", severity="blocking")
def key_unique(ctx) -> Iterable[Failure]:
    """No two records share a key.

    SQLite enforces this for a real primary key, but a collection's key is the
    *natural* key, which for split collections (one translation out of a shared
    table) is not the table's primary key at all. So it is checked here.
    """
    seen: dict[str, int] = {}
    for row in ctx.rows:
        k = ctx.key_of(row)
        seen[k] = seen.get(k, 0) + 1
    for k, count in seen.items():
        if count > 1:
            yield Failure(key=k, detail=f"key appears {count} times")


@rule(id="key.not-null", scope="*", severity="blocking")
def key_not_null(ctx) -> Iterable[Failure]:
    """A key column that is NULL makes the record unaddressable by a change."""
    nullable_by_design = set(ctx.collection.key) & {
        c for c, t in ctx.collection.schema.items() if t.endswith("?")
    }
    for row in ctx.rows:
        nulls = [
            c
            for c in ctx.collection.key
            if row.get(c) is None and c not in nullable_by_design
        ]
        if nulls:
            yield Failure(key=ctx.key_of(row), detail=f"NULL key column(s): {', '.join(nulls)}")


@rule(id="text.nfc", scope="*", severity="advisory")
def text_nfc(ctx) -> Iterable[Failure]:
    """Report text that is not NFC-normalised.

    Advisory on purpose. Normalising vocalised Arabic can reorder combining
    marks, and invariant #1 says we do not rewrite the corpus to make a check
    pass — importers normalise on the way in, and anything already in the vault
    gets reported rather than silently changed.
    """
    text_columns = [c for c, t in ctx.collection.schema.items() if t.startswith("text")]
    if not text_columns:
        return
    reported = 0
    for row in ctx.rows:
        for col in text_columns:
            value = row.get(col)
            if isinstance(value, str) and not unicodedata.is_normalized("NFC", value):
                reported += 1
                if reported <= 200:
                    yield Failure(key=ctx.key_of(row), detail=f"{col} is not NFC-normalised")
    if reported > 200:
        yield Failure(key=None, detail=f"… and {reported - 200} more non-NFC values")


BOM_LIKE = {"﻿": "BOM", "\x00": "NUL", "​": "ZWSP"}
JOINERS = {"‌": "ZWNJ", "‍": "ZWJ"}


@rule(id="text.no-bom", scope="*", severity="blocking")
def no_bom(ctx) -> Iterable[Failure]:
    """No byte-order mark, NUL or zero-width space anywhere in a text field.

    None of these can be legitimate inside a stored string: a BOM is an encoding
    artefact, a NUL truncates in C, a ZWSP is invisible whitespace nobody typed
    on purpose. Contrast `text.zero-width` — that one is advisory precisely
    because its characters do carry meaning.
    """
    for row in ctx.rows:
        for col in _text_columns(ctx):
            value = row.get(col)
            if not isinstance(value, str):
                continue
            found = sorted({name for ch, name in BOM_LIKE.items() if ch in value})
            if found:
                yield Failure(key=ctx.key_of(row), detail=f"{col} contains {', '.join(found)}")


@rule(id="text.zero-width", scope="*", severity="advisory")
def zero_width_joiners(ctx) -> Iterable[Failure]:
    """Report ZWNJ/ZWJ per column. Advisory, because they are frequently correct.

    ZWNJ is meaningful Urdu and Persian typography, and ZWJ is how an emoji
    sequence is built — `dua_categories.icon` holds a literal family emoji. A
    rule that blocked on these would be demanding that the corpus be wrong, so
    it reports a count per column and lets a human look.
    """
    counts: dict[str, int] = {}
    for row in ctx.rows:
        for col in _text_columns(ctx):
            value = row.get(col)
            if isinstance(value, str) and any(ch in value for ch in JOINERS):
                counts[col] = counts.get(col, 0) + 1
    for col, n in sorted(counts.items()):
        yield Failure(key=None, detail=f"{col} carries ZWNJ/ZWJ in {n} row(s)")


def _text_columns(ctx) -> list[str]:
    return [c for c, t in ctx.collection.schema.items() if t.startswith("text")]
