"""The terminal console (§10).

Not built yet — L0 tokens are real, L1–L4 are not. This module exists so that
``nz ui`` fails with a sentence rather than an ImportError, and so the entry
point is already wired when the widget tiers land.
"""

from __future__ import annotations

from ..core.errors import NzError
from ..core.project import Project


def run(project: Project) -> None:
    raise NzError(
        "the console (L1–L4) is not built yet — the CLI is the working front door. "
        "L0 tokens live in nimaz_data/ui/tokens.tcss; see nimaz_data/ui/README.md.",
        root=str(project.root),
    )
