"""The repository layout (§3), resolved once and passed around.

Every path in the system hangs off a ``Project``. Nothing computes a path by
string-joining relative to the current working directory, so ``nz`` behaves the
same run from ``nimaz-data/`` as from anywhere below it.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import yaml

from .errors import NzError
from .spec import CollectionSpec, load_all

CONFIG_NAME = "console.yaml"


@dataclass(frozen=True)
class Config:
    """``data/console.yaml`` — the small amount of policy that is not a collection.

    ``user_tables`` is the important one. The shipped database contains tables
    the app writes to at runtime (bookmarks, prayer records, khatams). They are
    part of the *schema* and must exist, empty, in the artifact — but they are
    not part of the *corpus*, so they get no collection, no floor and no rows.
    Conflating the two is how a build ends up "losing" 0 rows of user data that
    was never supposed to be there.
    """

    user_tables: tuple[str, ...] = ()
    ignore_tables: tuple[str, ...] = ()
    splits: dict[str, str] = field(default_factory=dict)
    naming: dict[str, str] = field(default_factory=dict)
    keys: dict[str, list[str]] = field(default_factory=dict)
    exclude_columns: dict[str, list[str]] = field(default_factory=dict)
    protected: dict[str, list[str]] = field(default_factory=dict)
    user_version: int = 0
    retain: int = 5
    required_provenance: tuple[str, ...] = ()

    @classmethod
    def load(cls, path: Path) -> "Config":
        if not path.exists():
            return cls()
        data = yaml.safe_load(path.read_text(encoding="utf-8")) or {}

        def table_map(name: str) -> dict[str, list[str]]:
            return {
                str(k): [str(x) for x in (v or [])]
                for k, v in (data.get(name) or {}).items()
            }

        return cls(
            user_tables=tuple(data.get("user_tables") or ()),
            ignore_tables=tuple(data.get("ignore_tables") or ()),
            splits={str(k): str(v) for k, v in (data.get("splits") or {}).items()},
            naming={str(k): str(v) for k, v in (data.get("naming") or {}).items()},
            keys=table_map("keys"),
            exclude_columns=table_map("exclude_columns"),
            protected=table_map("protected"),
            user_version=int(data.get("user_version") or 0),
            retain=int(data.get("retain") or 5),
            required_provenance=tuple(data.get("required_provenance") or ()),
        )


@dataclass(frozen=True)
class Project:
    root: Path

    # --- directories ---
    @property
    def vault_dir(self) -> Path:
        return self.root / "vault"

    @property
    def data_dir(self) -> Path:
        return self.root / "data"

    @property
    def collections_dir(self) -> Path:
        return self.data_dir / "collections"

    @property
    def changes_dir(self) -> Path:
        return self.data_dir / "changes"

    @property
    def applied_dir(self) -> Path:
        return self.changes_dir / "applied"

    @property
    def rules_dir(self) -> Path:
        return self.data_dir / "rules"

    @property
    def keys_dir(self) -> Path:
        return self.data_dir / "keys"

    @property
    def build_dir(self) -> Path:
        return self.root / ".build"

    @property
    def out_dir(self) -> Path:
        return self.root / "out"

    # --- files ---
    @property
    def config_path(self) -> Path:
        return self.data_dir / CONFIG_NAME

    @property
    def schema_path(self) -> Path:
        return self.data_dir / "schema.sql"

    @property
    def genesis_path(self) -> Path:
        return self.data_dir / "genesis.json"

    @property
    def receipt_path(self) -> Path:
        return self.out_dir / "build.json"

    @property
    def current_link(self) -> Path:
        return self.out_dir / "current"

    @property
    def previous_link(self) -> Path:
        return self.out_dir / "previous"

    # --- derived ---
    @property
    def config(self) -> Config:
        return Config.load(self.config_path)

    def specs(self) -> dict[str, CollectionSpec]:
        if not self.collections_dir.exists():
            return {}
        return load_all(self.collections_dir)

    def schema_sql(self) -> str:
        if not self.schema_path.exists():
            raise NzError(
                "data/schema.sql is missing — run `nz init` against the vault first",
                path=str(self.schema_path),
            )
        return self.schema_path.read_text(encoding="utf-8")

    def ensure_dirs(self) -> None:
        for d in (
            self.data_dir,
            self.collections_dir,
            self.changes_dir,
            self.applied_dir,
            self.rules_dir,
            self.build_dir,
            self.out_dir,
        ):
            d.mkdir(parents=True, exist_ok=True)

    def to_dict(self) -> dict[str, Any]:
        return {"root": str(self.root)}


def discover(start: Path | None = None) -> Project:
    """Walk up looking for ``data/console.yaml``; fall back to the cwd."""
    here = (start or Path.cwd()).resolve()
    for candidate in (here, *here.parents):
        if (candidate / "data" / CONFIG_NAME).exists():
            return Project(candidate)
        if (candidate / "nimaz_data").is_dir() and (candidate / "pyproject.toml").exists():
            return Project(candidate)
    return Project(here)
