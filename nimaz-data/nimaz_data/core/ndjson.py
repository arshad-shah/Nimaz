"""NDJSON sources — the only representation of the corpus that git ever sees (§12)."""

from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Iterable, Iterator, Mapping, Sequence

from .canonical import canonical_line, sort_records
from .errors import BuildError


def write_records(
    path: Path, records: Iterable[Mapping], key: Sequence[str]
) -> tuple[int, int]:
    """Write a collection's records canonically. Returns (rows, bytes).

    Written via a temp file and ``os.replace`` so an interrupted export leaves
    the previous source file intact rather than a half-written one.
    """
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    rows = 0
    size = 0
    with open(tmp, "wb") as fh:
        for record in sort_records(records, key):
            line = canonical_line(record)
            fh.write(line)
            rows += 1
            size += len(line)
        fh.flush()
        os.fsync(fh.fileno())
    os.replace(tmp, path)
    return rows, size


def iter_records(path: Path) -> Iterator[dict]:
    if not path.exists():
        raise BuildError("source file missing", path=str(path))
    with open(path, "r", encoding="utf-8") as fh:
        for lineno, line in enumerate(fh, start=1):
            line = line.strip()
            if not line:
                continue
            try:
                record = json.loads(line)
            except json.JSONDecodeError as exc:
                raise BuildError(
                    "malformed NDJSON line", path=str(path), line=lineno, detail=str(exc)
                ) from exc
            if not isinstance(record, dict):
                raise BuildError(
                    "NDJSON line is not an object", path=str(path), line=lineno
                )
            yield record


def read_records(path: Path) -> list[dict]:
    return list(iter_records(path))


def line_index(path: Path, key: Sequence[str]) -> dict[tuple, int]:
    """Map key tuple -> 1-based line number.

    Used by ``nz validate --annotate github`` (§14) to put a rule failure on the
    offending line of the diff instead of in a log nobody opens.
    """
    index: dict[tuple, int] = {}
    for lineno, record in enumerate(iter_records(path), start=1):
        index[tuple(record.get(k) for k in key)] = lineno
    return index
