"""Content hashing. Identity in this system is a hash, never a typed number (§1.5)."""

from __future__ import annotations

import hashlib
from pathlib import Path
from typing import Iterable, Mapping, Sequence

from .canonical import canonical_line, key_repr, sort_records

_CHUNK = 1 << 20


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        while chunk := fh.read(_CHUNK):
            h.update(chunk)
    return h.hexdigest()


def records_hash(records: Iterable[Mapping], key: Sequence[str]) -> str:
    """Hash of a collection's canonical NDJSON form.

    This is the collection content hash that appears in ``_manifest``,
    ``build.json`` and ``genesis.json``. It is computed from the records, not
    from the file, so an exported file and an in-memory candidate that agree
    produce the same digest — which is what makes the round-trip proof (§11.3)
    a hash comparison rather than a diff.
    """
    h = hashlib.sha256()
    for record in sort_records(records, key):
        h.update(canonical_line(record))
    return h.hexdigest()


def keys_digest(records: Iterable[Mapping], key: Sequence[str]) -> str:
    """Hash of the key set alone — what the genesis chain proves nothing fell out of."""
    h = hashlib.sha256()
    for record in sort_records(records, key):
        h.update(key_repr(record, key).encode("utf-8") + b"\n")
    return h.hexdigest()


def short(digest: str, n: int = 8) -> str:
    return digest[:n]
