"""The vault (§2) and the genesis fingerprint (§13).

Invariant #1: the corpus is never written to. The only write this module makes
anywhere near ``vault/`` is ``chmod 444`` during sealing, and it refuses to seal
over an existing sealed vault.
"""

from __future__ import annotations

import gzip
import json
import os
import pwd
import stat
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path

from .errors import VaultError
from .hash import sha256_file

CORPUS = "corpus.db"
CHECKSUM = "corpus.sha256"


@dataclass(frozen=True)
class VaultStatus:
    path: Path
    exists: bool
    sealed: bool
    writable: bool
    expected: str | None
    actual: str | None
    owner: str | None
    modified: str | None

    @property
    def ok(self) -> bool:
        return (
            self.exists
            and self.sealed
            and not self.writable
            and self.expected is not None
            and self.expected == self.actual
        )

    def to_dict(self) -> dict:
        return {
            "path": str(self.path),
            "exists": self.exists,
            "sealed": self.sealed,
            "writable": self.writable,
            "expected_sha256": self.expected,
            "actual_sha256": self.actual,
            "owner": self.owner,
            "modified": self.modified,
            "ok": self.ok,
        }


def corpus_path(vault_dir: Path) -> Path:
    return vault_dir / CORPUS


def checksum_path(vault_dir: Path) -> Path:
    return vault_dir / CHECKSUM


def _owner(path: Path) -> str | None:
    try:
        return pwd.getpwuid(path.stat().st_uid).pw_name
    except (KeyError, OSError):
        return None


def status(vault_dir: Path) -> VaultStatus:
    db = corpus_path(vault_dir)
    sums = checksum_path(vault_dir)
    if not db.exists():
        return VaultStatus(db, False, False, False, None, None, None, None)

    mode = db.stat().st_mode
    writable = bool(mode & (stat.S_IWUSR | stat.S_IWGRP | stat.S_IWOTH))
    expected = None
    if sums.exists():
        expected = sums.read_text(encoding="utf-8").split()[0].strip() or None
    return VaultStatus(
        path=db,
        exists=True,
        sealed=sums.exists(),
        writable=writable,
        expected=expected,
        actual=sha256_file(db),
        owner=_owner(db),
        modified=datetime.fromtimestamp(db.stat().st_mtime, timezone.utc).isoformat(),
    )


def verify(vault_dir: Path) -> VaultStatus:
    """Stage 1 of every build, and the first thing ``nz doctor`` does."""
    st = status(vault_dir)
    if not st.exists:
        raise VaultError(
            "vault/corpus.db is missing — run `nz vault seal <path-to-db>` first",
            **st.to_dict(),
        )
    if not st.sealed:
        raise VaultError("vault/corpus.sha256 is missing — the vault is unsealed", **st.to_dict())
    if st.expected != st.actual:
        raise VaultError(
            "vault checksum mismatch — the corpus has been modified since sealing",
            **st.to_dict(),
        )
    if st.writable:
        raise VaultError(
            "vault/corpus.db is writable — expected mode 444", **st.to_dict()
        )
    return st


def seal(vault_dir: Path, source: Path, *, force: bool = False) -> VaultStatus:
    """§11.1 — copy the current DB in, chmod 444, record its hash. Once."""
    db = corpus_path(vault_dir)
    if db.exists() and not force:
        raise VaultError(
            "vault already sealed; pass force to replace it", path=str(db)
        )
    if not source.exists():
        raise VaultError("source database not found", path=str(source))

    vault_dir.mkdir(parents=True, exist_ok=True)
    if db.exists():
        db.chmod(0o644)
        db.unlink()
    # Stream rather than shutil.copy2 so we never inherit a writable mode.
    with open(source, "rb") as src, open(db, "wb") as dst:
        while chunk := src.read(1 << 20):
            dst.write(chunk)
        dst.flush()
        os.fsync(dst.fileno())
    digest = sha256_file(db)
    checksum_path(vault_dir).write_text(f"{digest}  {CORPUS}\n", encoding="utf-8")
    db.chmod(0o444)
    return status(vault_dir)


# --- genesis (§13) -----------------------------------------------------------


def write_genesis(
    genesis_path: Path,
    *,
    vault_sha256: str,
    collections: dict[str, dict],
    keys_dir: Path,
    key_lists: dict[str, list[str]],
    sealed_at: str,
) -> dict:
    """Write ``data/genesis.json`` once, plus the gzipped key lists beside it.

    The key lists are what let the guard prove no genesis key is missing without
    opening the vault — which is what makes the vault an archive rather than a
    dependency.
    """
    if genesis_path.exists():
        raise VaultError(
            "genesis.json already exists and is written exactly once",
            path=str(genesis_path),
        )
    keys_dir.mkdir(parents=True, exist_ok=True)
    body = {"sealed": sealed_at, "vault_sha256": vault_sha256, "collections": {}}
    for name in sorted(collections):
        rel = f"keys/{name}.txt.gz"
        payload = "".join(k + "\n" for k in key_lists.get(name, [])).encode("utf-8")
        # mtime=0 so the key list is a function of the keys alone — a gzip header
        # carrying "when init ran" would make genesis unreproducible.
        with open(keys_dir / f"{name}.txt.gz", "wb") as raw:
            with gzip.GzipFile(fileobj=raw, mode="wb", mtime=0) as fh:
                fh.write(payload)
        body["collections"][name] = {**collections[name], "keys": rel}

    genesis_path.parent.mkdir(parents=True, exist_ok=True)
    genesis_path.write_text(json.dumps(body, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return body


def read_genesis(genesis_path: Path) -> dict | None:
    if not genesis_path.exists():
        return None
    return json.loads(genesis_path.read_text(encoding="utf-8"))


def read_genesis_keys(genesis_path: Path, collection: str) -> set[str] | None:
    genesis = read_genesis(genesis_path)
    if not genesis:
        return None
    entry = (genesis.get("collections") or {}).get(collection)
    if not entry or not entry.get("keys"):
        return None
    path = genesis_path.parent / entry["keys"]
    if not path.exists():
        return None
    with gzip.open(path, "rt", encoding="utf-8") as fh:
        return {line.rstrip("\n") for line in fh if line.strip()}
