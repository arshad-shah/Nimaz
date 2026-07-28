"""§6 stage 6 and §13 — the guard answers one question: did we lose anything?

The rules ask whether the data is correct. The guard asks whether it is still all
there. A corpus can be entirely valid and half gone, which is why these are two
passes and not one.
"""

from __future__ import annotations

import yaml

from nimaz_data.build.pipeline import build
from nimaz_data.build.promote import promote, rollback
from nimaz_data.changes.model import CollectionChange, Expect
from nimaz_data.changes.writer import write_change
from nimaz_data.core import manifest


def _set_floor(project, collection: str, rows_min: int) -> None:
    path = project.collections_dir / collection / "collection.yaml"
    body = yaml.safe_load(path.read_text(encoding="utf-8"))
    body["floors"]["rows_min"] = rows_min
    path.write_text(yaml.safe_dump(body, sort_keys=False, allow_unicode=True), encoding="utf-8")


def test_genesis_records_the_key_set(project):
    genesis = yaml.safe_load(project.genesis_path.read_text(encoding="utf-8"))
    assert genesis["vault_sha256"]
    assert set(genesis["collections"]) == set(project.specs())
    for name, entry in genesis["collections"].items():
        assert (project.data_dir / entry["keys"]).exists(), name


def _drop_translations(project, *, where: str, rows_delta: int, rows_after: int) -> None:
    write_change(
        project.changes_dir,
        title="drop translations",
        author="test",
        origin="hand",
        collections={
            "tr.en.sahih": CollectionChange(
                expect=Expect(rows_delta=rows_delta, rows_after=rows_after)
            )
        },
        up_sql=f"DELETE FROM translations WHERE translator_id = 'en.sahih' AND {where};",
    )


def test_rules_catch_an_incomplete_translation_before_the_guard(project):
    """Stage 5 runs before stage 6, and a hole in a translation is a rule's business."""
    _drop_translations(project, where="ayah_id > 3", rows_delta=-13, rows_after=3)
    result = build(project)
    assert not result.ok
    assert result.failed_stage == "validate"
    failed = {r.rule_id for r in result.validation.blocking}
    assert "translation.coverage" in failed


def test_a_floor_stops_a_deletion_even_when_it_was_declared(project):
    """`rows_min` has no exceptions — not even a truthfully declared one.

    Rules are skipped here so the assertion is about the guard rather than about
    which gate happens to fire first; the test above covers the ordering.
    """
    _drop_translations(project, where="ayah_id > 3", rows_delta=-13, rows_after=3)
    result = build(project, skip_rules=True)
    assert not result.ok
    assert result.failed_stage == "guard"
    assert "floor" in {f.check for f in result.guard.blocking}


def test_genesis_keys_cannot_vanish_without_a_declared_loss(project):
    """A key present at genesis and absent now must be accounted for.

    A change that removes rows while declaring a positive delta cannot exist —
    stage 3 catches that — so the deletion is declared honestly and the floor
    lowered. The genesis chain still notices, and records it as declared.
    """
    _set_floor(project, "tr.en.sahih", 0)
    _drop_translations(project, where="ayah_id = 16", rows_delta=-1, rows_after=15)

    result = build(project, skip_rules=True)
    assert result.ok, (result.failed_stage, result.error)
    advisory = [f for f in result.guard.findings if f.check == "genesis-keys"]
    assert advisory and not advisory[0].blocking


def test_an_undeclared_genesis_key_loss_is_blocking(project):
    """The same deletion, with no change declaring a row loss, is refused."""
    _set_floor(project, "tr.en.sahih", 0)
    spec = project.specs()["tr.en.sahih"]
    lines = spec.records_path.read_text(encoding="utf-8").splitlines()
    spec.records_path.write_text("\n".join(lines[:-1]) + "\n", encoding="utf-8")

    result = build(project, skip_rules=True)
    assert not result.ok
    assert result.failed_stage == "guard"
    assert "genesis-keys" in {f.check for f in result.guard.blocking}


def test_undeclared_source_drift_is_caught_against_the_last_receipt(project):
    """Hand-editing an NDJSON file without a change is drift, and the guard says so."""
    first = build(project)
    assert first.ok
    manifest.write_receipt(project.receipt_path, first.receipt)

    spec = project.specs()["tr.bn.bengali"]
    lines = spec.records_path.read_text(encoding="utf-8").splitlines()
    lines[0] = lines[0].replace("rendering", "RENDERING")
    spec.records_path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    second = build(project)
    assert not second.ok
    assert second.failed_stage == "guard"
    assert {f.check for f in second.guard.blocking} == {"undeclared-drift"}


def test_promote_and_rollback_are_pointer_moves(project):
    first = build(project)
    assert first.ok
    promoted = promote(first.candidate, project.out_dir, receipt=first.receipt)
    assert project.current_link.resolve() == promoted.artifact.resolve()
    assert manifest.read_receipt(project.receipt_path)["artifact"].startswith("sha256:")

    write_change(
        project.changes_dir,
        title="tweak",
        author="test",
        origin="hand",
        collections={"tr.en.sahih": CollectionChange(expect=Expect(rows_delta=0, keys_touched=1))},
        up_sql="UPDATE translations SET text = 'v2' "
        "WHERE translator_id = 'en.sahih' AND ayah_id = 2;",
    )
    second = build(project)
    assert second.ok, second.error
    promote(second.candidate, project.out_dir, receipt=second.receipt)

    assert project.previous_link.resolve() == promoted.artifact.resolve()
    rolled = rollback(project.out_dir)
    assert rolled.artifact.resolve() == promoted.artifact.resolve()
    assert project.current_link.resolve() == promoted.artifact.resolve()


def test_artifact_is_written_read_only(project):
    import os

    result = build(project)
    promoted = promote(result.candidate, project.out_dir, receipt=result.receipt)
    mode = os.stat(promoted.artifact).st_mode & 0o222
    assert mode == 0, "a promoted artifact must not be writable"
