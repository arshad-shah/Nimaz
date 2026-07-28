"""§8 rules, §5 fold, §9 agent surface — the parts that are easy to get subtly wrong."""

from __future__ import annotations

import pytest
import yaml

from nimaz_data.agent import server as agent
from nimaz_data.build.pipeline import build
from nimaz_data.build.promote import promote
from nimaz_data.changes.fold import fold
from nimaz_data.changes.model import CollectionChange, Expect, load_pending
from nimaz_data.changes.writer import write_change
from nimaz_data.core import db as dbx
from nimaz_data.core.errors import ChangeError, NzError
from nimaz_data.rules.registry import discover
from nimaz_data.rules.report import as_github_annotations, as_text
from nimaz_data.rules.runner import run_rules


# --- rules --------------------------------------------------------------------


def test_every_rule_declares_a_severity_and_a_scope(project):
    rules = discover(project.rules_dir)
    assert rules
    for r in rules:
        assert r.severity in ("blocking", "advisory")
        assert r.scope


def test_a_clean_corpus_has_no_blocking_failures(project):
    result = build(project)
    conn = dbx.open_readonly(result.candidate)
    try:
        report = run_rules(conn, project.specs(), rules_dir=project.rules_dir)
    finally:
        conn.close()
    assert report.ok, as_text(report)


def test_provenance_is_a_rule_not_a_readme(project):
    """The malformed-source-string case: a dropped field fails the build."""
    path = project.collections_dir / "tr.en.sahih" / "collection.yaml"
    body = yaml.safe_load(path.read_text(encoding="utf-8"))
    body["provenance"]["license"] = ""
    path.write_text(yaml.safe_dump(body, sort_keys=False, allow_unicode=True), encoding="utf-8")

    result = build(project)
    assert not result.ok
    assert "provenance.complete" in {r.rule_id for r in result.validation.blocking}


def test_page_layout_rules_see_a_missing_line(project):
    write_change(
        project.changes_dir,
        title="lose a line",
        author="test",
        origin="hand",
        collections={
            "mushaf.indopak16": CollectionChange(expect=Expect(rows_delta=-1, rows_after=31))
        },
        up_sql="DELETE FROM mushaf_layout_indopak16 WHERE page = 1 AND line = 9;",
    )
    result = build(project)
    assert not result.ok
    assert "page.lines-complete" in {r.rule_id for r in result.validation.blocking}


def test_annotations_point_at_the_offending_source_line(project):
    spec = project.specs()["tr.en.sahih"]
    lines = spec.records_path.read_text(encoding="utf-8").splitlines()
    lines[2] = lines[2].replace('"text":"en.sahih rendering of 3"', '"text":"  "')
    spec.records_path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    result = build(project, skip_rules=True)
    conn = dbx.open_readonly(result.candidate)
    try:
        report = run_rules(
            conn, project.specs(), rules_dir=project.rules_dir, only=["translation.non-empty"]
        )
    finally:
        conn.close()

    annotations = as_github_annotations(report, project.specs(), project.root)
    assert annotations
    assert annotations[0].startswith("::error file=data/collections/tr.en.sahih/records.ndjson")
    assert ",line=3," in annotations[0]


# --- fold ---------------------------------------------------------------------


def test_fold_replays_changes_into_the_sources(project):
    write_change(
        project.changes_dir,
        title="revise one",
        author="test",
        origin="hand",
        collections={"tr.en.sahih": CollectionChange(expect=Expect(rows_delta=0, keys_touched=1))},
        up_sql="UPDATE translations SET text = 'folded text' "
        "WHERE translator_id = 'en.sahih' AND ayah_id = 1;",
    )
    before = build(project)
    assert before.ok

    report = fold(project)
    assert report.committed
    assert report.hash_before == report.hash_after
    assert "tr.en.sahih" in report.rewritten

    assert load_pending(project.changes_dir) == []
    assert (project.applied_dir / report.folded[0]).exists()

    spec = project.specs()["tr.en.sahih"]
    assert "folded text" in spec.records_path.read_text(encoding="utf-8")

    after = build(project)
    assert after.artifact_hash == before.artifact_hash


def test_fold_is_a_no_op_with_nothing_pending(project):
    report = fold(project)
    assert report.folded == []
    assert not report.committed


# --- agent --------------------------------------------------------------------


def test_agent_describe_matches_the_cli_payload(project):
    result = build(project)
    promote(result.candidate, project.out_dir, receipt=result.receipt)

    described = agent.describe(root=str(project.root))
    assert described["artifact"] == f"sha256:{result.artifact_hash}"
    assert set(described["collections"]) == set(project.specs())


def test_agent_query_is_read_only_and_capped(project):
    result = build(project)
    promote(result.candidate, project.out_dir, receipt=result.receipt)

    rows = agent.query("SELECT ayah_id, text FROM translations", limit=3, root=str(project.root))
    assert rows["count"] == 3 and rows["capped"]

    with pytest.raises(NzError):
        agent.query("DELETE FROM translations", root=str(project.root))


def test_agent_propose_writes_a_change_and_nothing_else(project):
    before = build(project)
    assert before.ok

    proposed = agent.propose(
        title="agent suggests a fix",
        collections={"tr.en.sahih": {"bump": "patch", "rows_delta": 0, "keys_touched": 1}},
        up_sql="UPDATE translations SET text = 'agent text' "
        "WHERE translator_id = 'en.sahih' AND ayah_id = 4;",
        rationale="found by the agent",
        root=str(project.root),
    )
    assert proposed["change"]["origin"] == "agent"

    # Nothing was applied: the sources are untouched until a build runs.
    spec = project.specs()["tr.en.sahih"]
    assert "agent text" not in spec.records_path.read_text(encoding="utf-8")

    # And the change goes through exactly the same gate as a hand-written one.
    after = build(project)
    assert after.ok, after.error
    assert after.artifact_hash != before.artifact_hash


def test_agent_propose_is_held_to_its_declaration(project):
    agent.propose(
        title="agent lies",
        collections={"tr.en.sahih": {"rows_delta": 99}},
        up_sql="UPDATE translations SET text = 'x' "
        "WHERE translator_id = 'en.sahih' AND ayah_id = 5;",
        root=str(project.root),
    )
    result = build(project)
    assert not result.ok
    assert result.failed_stage == "changes"


def test_agent_get_finds_a_record_by_key(project):
    got = agent.get("tr.en.sahih", [1], root=str(project.root))
    assert got["record"]["text"].endswith("of 1")
    assert agent.get("tr.en.sahih", [9999], root=str(project.root))["record"] is None

    with pytest.raises(NzError):
        agent.get("tr.en.sahih", [1, 2], root=str(project.root))


def test_a_malformed_change_is_rejected_at_load(project):
    d = project.changes_dir / "not-a-valid-id"
    d.mkdir()
    (d / "change.yaml").write_text("id: nope\n", encoding="utf-8")
    (d / "up.sql").write_text("-- x\n", encoding="utf-8")
    with pytest.raises(ChangeError):
        load_pending(project.changes_dir)
