"""Canonical text and JSON encoding.

Everything that gets hashed goes through here first, so that a file rewritten by
the tool is byte-identical to one rewritten by hand (§3).

On Unicode: §3 calls for NFC normalisation. We deliberately do *not* normalise on
export — invariant #1 says the corpus is never written to, and NFC can reorder
combining marks in vocalised Arabic. Export preserves the vault bytes verbatim;
the advisory ``text.nfc`` rule reports anything that is not already NFC, and
importers (§16) normalise on the way in, where it is a genuine choice rather than
a silent rewrite of scripture.
"""

from __future__ import annotations

import json
import unicodedata
from typing import Any, Iterable, Mapping, Sequence

# json.dumps settings used everywhere a record becomes bytes.
_JSON = dict(
    ensure_ascii=False,
    sort_keys=True,
    separators=(",", ":"),
    allow_nan=False,
)


def nfc(value: str) -> str:
    """NFC-normalise a string."""
    return unicodedata.normalize("NFC", value)


def is_nfc(value: str) -> bool:
    return unicodedata.is_normalized("NFC", value)


def canonical_json(obj: Any) -> str:
    """One record, one line, keys sorted, no incidental whitespace."""
    return json.dumps(obj, **_JSON)


def canonical_line(record: Mapping[str, Any]) -> bytes:
    return canonical_json(dict(record)).encode("utf-8") + b"\n"


def key_of(record: Mapping[str, Any], key: Sequence[str]) -> tuple:
    """The sort/identity tuple for a record.

    ``None`` sorts before everything and ints before strings, so that a key
    column that is nullable (``mushaf_layout_indopak16.ayah_id``) still yields a
    total order rather than a TypeError halfway through a 41k-row sort.
    """
    out = []
    for k in key:
        v = record.get(k)
        if v is None:
            out.append((0, 0, ""))
        elif isinstance(v, bool):
            out.append((1, int(v), ""))
        elif isinstance(v, (int, float)):
            out.append((1, v, ""))
        else:
            out.append((2, 0, str(v)))
    return tuple(out)


def sort_records(
    records: Iterable[Mapping[str, Any]], key: Sequence[str]
) -> list[dict]:
    return sorted((dict(r) for r in records), key=lambda r: key_of(r, key))


def key_repr(record: Mapping[str, Any], key: Sequence[str]) -> str:
    """Human- and digest-friendly rendering of a record's key: ``2|255|3``."""
    return "|".join("" if record.get(k) is None else str(record.get(k)) for k in key)
