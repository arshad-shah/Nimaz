"""One exception family, so the CLI and the MCP server report failures identically."""

from __future__ import annotations

from typing import Any


class NzError(Exception):
    """Base for every expected failure. Carries structured detail for ``--json``."""

    exit_code = 1

    def __init__(self, message: str, **detail: Any) -> None:
        super().__init__(message)
        self.message = message
        self.detail = detail

    def to_dict(self) -> dict:
        return {"error": type(self).__name__, "message": self.message, **self.detail}


class VaultError(NzError):
    """The vault is missing, mutated, or writable. Nothing continues past this."""


class SpecError(NzError):
    """A collection.yaml is malformed or disagrees with the schema."""


class ChangeError(NzError):
    """A change directory is malformed, or its expect{} did not match reality."""


class GuardError(NzError):
    """Stage 6 said no: a floor, a protected field, or an undeclared key loss."""


class RuleError(NzError):
    """A blocking rule failed, or a rule plugin itself blew up."""


class BuildError(NzError):
    """Anything else that stops the pipeline before an artifact exists."""


class PromoteError(NzError):
    """The artifact exists but must not become out/current."""
