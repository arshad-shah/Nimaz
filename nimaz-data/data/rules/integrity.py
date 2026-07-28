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


@rule(id="text.no-control-chars", scope="*", severity="blocking")
def no_control_chars(ctx) -> Iterable[Failure]:
    """No BOM or zero-width marks in text. Issue #290, as a rule instead of a script."""
    bad = {"﻿": "BOM", "​": "ZWSP", "‌": "ZWNJ", "‍": "ZWJ", "\x00": "NUL"}
    text_columns = [c for c, t in ctx.collection.schema.items() if t.startswith("text")]
    for row in ctx.rows:
        for col in text_columns:
            value = row.get(col)
            if not isinstance(value, str):
                continue
            found = sorted({name for ch, name in bad.items() if ch in value})
            if found:
                yield Failure(key=ctx.key_of(row), detail=f"{col} contains {', '.join(found)}")
