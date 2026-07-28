"""Promotion and rollback (§7).

``fsync`` then ``os.replace`` — atomic on POSIX. ``out/previous`` always points at
the last-known-good artifact, so a rollback is a pointer move rather than a rebuild.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from ..core import manifest
from ..core.errors import PromoteError
from ..core.hash import sha256_file


@dataclass(frozen=True)
class PromoteResult:
    artifact: Path
    artifact_hash: str
    current: Path
    previous: Path | None
    retired: list[Path]

    def to_dict(self) -> dict:
        return {
            "artifact": str(self.artifact),
            "hash": f"sha256:{self.artifact_hash}",
            "current": str(self.current),
            "previous": str(self.previous) if self.previous else None,
            "retired": [str(p) for p in self.retired],
        }


def _resolve(link: Path) -> Path | None:
    if not link.is_symlink():
        return None
    target = link.parent / os.readlink(link)
    return target if target.exists() else None


def _point(link: Path, target: Path) -> None:
    tmp = link.parent / (link.name + ".tmp")
    if tmp.exists() or tmp.is_symlink():
        tmp.unlink()
    tmp.symlink_to(target.name)
    os.replace(tmp, link)
    _fsync_dir(link.parent)


def _fsync_dir(path: Path) -> None:
    fd = os.open(path, os.O_RDONLY)
    try:
        os.fsync(fd)
    finally:
        os.close(fd)


def promote(
    candidate: Path,
    out_dir: Path,
    *,
    receipt: dict | None = None,
    retain: int = 5,
) -> PromoteResult:
    """Move a validated candidate into ``out/`` and repoint ``current``."""
    if not candidate.exists():
        raise PromoteError("candidate not found", path=str(candidate))
    out_dir.mkdir(parents=True, exist_ok=True)

    digest = sha256_file(candidate)
    target = out_dir / f"nimaz-{digest[:8]}.db"

    if target.exists() and sha256_file(target) != digest:
        raise PromoteError(
            "an artifact with this short hash exists with different content",
            path=str(target),
        )

    if not target.exists():
        tmp = out_dir / f".{target.name}.tmp"
        with open(candidate, "rb") as src, open(tmp, "wb") as dst:
            while chunk := src.read(1 << 20):
                dst.write(chunk)
            dst.flush()
            os.fsync(dst.fileno())
        os.replace(tmp, target)
        _fsync_dir(out_dir)
        target.chmod(0o444)

    current_link = out_dir / "current"
    previous_link = out_dir / "previous"
    was_current = _resolve(current_link)

    if was_current and was_current.resolve() != target.resolve():
        _point(previous_link, was_current)
    _point(current_link, target)

    if receipt is not None:
        manifest.write_receipt(out_dir / "build.json", receipt)

    retired = _retire(out_dir, keep={target, was_current, _resolve(previous_link)}, retain=retain)
    return PromoteResult(
        artifact=target,
        artifact_hash=digest,
        current=current_link,
        previous=_resolve(previous_link),
        retired=retired,
    )


def rollback(out_dir: Path) -> PromoteResult:
    """Repoint ``current`` at ``previous``. Two pointers, swapped."""
    current_link, previous_link = out_dir / "current", out_dir / "previous"
    target = _resolve(previous_link)
    if target is None:
        raise PromoteError("no previous artifact to roll back to", path=str(previous_link))
    was_current = _resolve(current_link)

    _point(current_link, target)
    if was_current:
        _point(previous_link, was_current)

    return PromoteResult(
        artifact=target,
        artifact_hash=sha256_file(target),
        current=current_link,
        previous=_resolve(previous_link),
        retired=[],
    )


def _retire(out_dir: Path, *, keep: set, retain: int) -> list[Path]:
    """Keep the last N artifacts; never touch what current or previous point at."""
    protected = {p.resolve() for p in keep if p}
    artifacts = sorted(
        (p for p in out_dir.glob("nimaz-*.db")),
        key=lambda p: p.stat().st_mtime,
        reverse=True,
    )
    retired = []
    for path in artifacts[retain:]:
        if path.resolve() in protected:
            continue
        path.chmod(0o644)
        path.unlink()
        retired.append(path)
    return retired
