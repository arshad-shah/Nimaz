"""Provenance is structured, not prose (§16).

The rule that would have caught the malformed source string already in the tree:
``alquran.cloud edition 'en.pickthall' ()`` — an empty parenthetical where a
field was dropped. Once provenance is fields rather than a sentence, a dropped
field is a failing build instead of a string nobody reads.
"""

from __future__ import annotations

from typing import Iterable

from nimaz_data.rules import Failure, rule

# Kinds whose provenance is a licensing obligation rather than a nicety.
ATTRIBUTED_KINDS = {"translation", "tafseer", "hadith"}


@rule(id="provenance.complete", scope="*", severity="blocking")
def provenance_complete(ctx) -> Iterable[Failure]:
    """Every attributed collection declares a translator, a license and a retrieval date."""
    spec = ctx.collection
    if spec.kind not in ATTRIBUTED_KINDS:
        return
    for field in ("translator", "license", "retrieved"):
        value = getattr(spec.provenance, field, None)
        if not (value or "").strip():
            yield Failure(
                key=None,
                detail=f"collection.yaml is missing provenance.{field}",
            )


@rule(id="provenance.placeholder", scope="*", severity="advisory")
def provenance_placeholder(ctx) -> Iterable[Failure]:
    """Flag provenance that is present but obviously unfilled."""
    notes = (ctx.collection.provenance.notes or "").upper()
    if "TODO" in notes:
        yield Failure(key=None, detail="provenance still carries the `nz init` TODO")
