"""Output shaping. Every command has a ``--json`` form (§9), and it is the same
schema the MCP server returns — one core, three front doors."""

from __future__ import annotations

import json
import sys
from typing import Any, Sequence

# §10 state colours, as the terminal sees them. Teal = matches the manifest,
# gold = changed and not yet validated, mulberry = blocking, violet = never
# verified. Status only, never decoration.
OK = "\x1b[36m"
PEND = "\x1b[33m"
BLOCK = "\x1b[35m"
UNKNOWN = "\x1b[34m"
DIM = "\x1b[2m"
BOLD = "\x1b[1m"
RESET = "\x1b[0m"


def _tty() -> bool:
    return sys.stdout.isatty()


def paint(text: str, colour: str) -> str:
    return f"{colour}{text}{RESET}" if _tty() else text


def emit_json(payload: Any) -> None:
    json.dump(payload, sys.stdout, indent=2, ensure_ascii=False, default=str)
    sys.stdout.write("\n")


def echo(line: str = "") -> None:
    sys.stdout.write(line + "\n")


def heading(text: str) -> None:
    echo(paint(text, BOLD))


def dot(state: str) -> str:
    return paint("●", {"ok": OK, "pending": PEND, "blocking": BLOCK}.get(state, UNKNOWN))


def table(rows: Sequence[Sequence[Any]], headers: Sequence[str]) -> None:
    """A plain aligned table. No box drawing — this gets pasted into PR comments."""
    if not rows:
        echo(paint("(nothing)", DIM))
        return
    cells = [[str(c) for c in row] for row in rows]
    widths = [
        max(len(str(headers[i])), *(len(row[i]) for row in cells))
        for i in range(len(headers))
    ]
    echo("  ".join(paint(str(h).ljust(w), DIM) for h, w in zip(headers, widths)))
    for row in cells:
        echo("  ".join(c.ljust(w) for c, w in zip(row, widths)))


def markdown_table(rows: Sequence[Sequence[Any]], headers: Sequence[str]) -> str:
    """The declared-vs-actual table ``pr.yml`` posts as a PR comment (§14)."""
    out = ["| " + " | ".join(str(h) for h in headers) + " |"]
    out.append("|" + "|".join("---" for _ in headers) + "|")
    for row in rows:
        out.append("| " + " | ".join(str(c) for c in row) + " |")
    return "\n".join(out)
