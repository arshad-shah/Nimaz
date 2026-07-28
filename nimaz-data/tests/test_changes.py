"""§5 — the single write funnel, and the part of it that does the actual work.

`expect` is the important field: you declare the blast radius before the build
runs, and the build fails if reality disagrees. These tests are the proof that
silent mass deletion is structurally impossible rather than merely unlikely.
"""

from __future__ import annotations

import pytest

from nimaz_data.build.pipeline import build
from nimaz_data.changes.model import CollectionChange, Expect, load_pending
from nimaz_data.changes.writer import write_change
from nimaz_data.core.errors import ChangeError


def _change(project, *, sql, collection="tr.en.sahih", expect=None, protected=(), title="t"):
    return write_change(
        project.changes_dir,
        title=title,
        author="test",
        origin="console",
        collections={
            collection: CollectionChange(
                bump="patch", expect=expect or Expect(), protected=tuple(protected)
            )
        },
        up_sql=sql,
    )


def test_a_truthful_change_applies(project):
    before = build(project)
    assert before.ok

    _change(
        project,
        sql="UPDATE translations SET text = 'revised' "
        "WHERE translator_id = 'en.sahih' AND ayah_id = 1;",
        expect=Expect(rows_delta=0, rows_after=16, keys_touched=1),
    )

    after = build(project)
    assert after.ok, (after.failed_stage, after.error)
    assert after.artifact_hash != before.artifact_hash
    assert after.outcomes[0].actual["tr.en.sahih"]["keys_touched"] == 1


def test_a_change_that_lies_about_its_blast_radius_is_refused(project):
    """The +43/-4000 case. Declared and actual disagree, so nothing is built."""
    _change(
        project,
        sql="DELETE FROM translations WHERE translator_id = 'en.sahih' AND ayah_id > 3;",
        expect=Expect(rows_delta=+43),
    )
    result = build(project)
    assert not result.ok
    assert result.failed_stage == "changes"
    assert "rows_delta" in result.error


def test_a_change_cannot_touch_a_collection_it_did_not_declare(project):
    """Collateral damage is a build failure, not a surprise in the diff."""
    _change(
        project,
        sql="UPDATE translations SET text = 'x' WHERE translator_id = 'bn.bengali';",
        collection="tr.en.sahih",
        expect=Expect(rows_delta=0),
    )
    result = build(project)
    assert not result.ok
    assert result.failed_stage == "changes"
    assert "did not declare" in result.error


def test_protected_fields_need_a_declaration(project):
    """Scripture does not move because someone wrote an UPDATE."""
    _change(
        project,
        sql="UPDATE ayahs SET text_uthmani = 'tampered' WHERE id = 1;",
        collection="quran.uthmani",
        expect=Expect(rows_delta=0, rows_after=16, keys_touched=1),
    )
    result = build(project)
    assert not result.ok
    assert "protected" in result.error


def test_protected_fields_need_a_second_signal_even_when_declared(project):
    _change(
        project,
        sql="UPDATE ayahs SET text_uthmani = 'corrected' WHERE id = 1;",
        collection="quran.uthmani",
        expect=Expect(rows_delta=0, rows_after=16, keys_touched=1),
        protected=["text_uthmani"],
    )
    refused = build(project)
    assert not refused.ok
    assert "--confirm-protected" in refused.error

    allowed = build(project, confirm_protected=True)
    assert allowed.ok, (allowed.failed_stage, allowed.error)


def test_changes_apply_in_id_order(project):
    import time

    first = _change(
        project,
        sql="UPDATE translations SET text = 'first' "
        "WHERE translator_id = 'en.sahih' AND ayah_id = 1;",
        expect=Expect(rows_delta=0, keys_touched=1),
        title="aaa",
    )
    time.sleep(0.01)
    second = _change(
        project,
        sql="UPDATE translations SET text = 'second' "
        "WHERE translator_id = 'en.sahih' AND ayah_id = 1;",
        expect=Expect(rows_delta=0, keys_touched=1),
        title="bbb",
    )
    ordered = [c.id for c in load_pending(project.changes_dir)]
    assert ordered == sorted([first.id, second.id])

    result = build(project)
    assert result.ok, result.error


def test_a_change_directory_without_up_sql_is_rejected(project):
    change = _change(project, sql="-- nothing", expect=Expect(rows_delta=0))
    change.up_sql.unlink()
    with pytest.raises(ChangeError):
        load_pending(project.changes_dir)
