"""The CLI front door. Every command has a --json form with the same schema the
MCP server returns (§9), so these assert the shape, not just the exit code."""

from __future__ import annotations

import json

import pytest
from typer.testing import CliRunner

from nimaz_data.build.pipeline import build
from nimaz_data.cli.main import app

runner = CliRunner()


def _json(result):
    return json.loads(result.stdout)


def _run(*args):
    return runner.invoke(app, list(args), catch_exceptions=False)


def test_doctor_reports_a_healthy_project(project):
    result = _run("doctor", "--root", str(project.root), "--json")
    assert result.exit_code == 0, result.stdout
    payload = _json(result)
    assert payload["ok"] and payload["vault"]["ok"]
    assert payload["collections"] == 5
    assert payload["pending_changes"] == []


def test_doctor_fails_on_a_tampered_vault(project):
    corpus = project.vault_dir / "corpus.db"
    corpus.chmod(0o644)
    with open(corpus, "r+b") as fh:
        fh.seek(0x2000)
        fh.write(b"\x00")
    corpus.chmod(0o444)

    result = _run("doctor", "--root", str(project.root), "--json")
    assert result.exit_code == 1
    assert _json(result)["vault"]["ok"] is False


def test_build_json_carries_every_stage(project):
    result = _run("build", "--root", str(project.root), "--json")
    assert result.exit_code == 0, result.stdout
    payload = _json(result)
    assert [s["stage"] for s in payload["stages"]] == [
        "vault",
        "sources",
        "changes",
        "compile",
        "validate",
        "guard",
    ]
    assert payload["artifact"].startswith("sha256:")


def test_deps_check_passes_and_then_fails(project):
    import yaml

    assert _run("deps", "check", "--root", str(project.root), "--json").exit_code == 0

    path = project.collections_dir / "tr.en.sahih" / "collection.yaml"
    body = yaml.safe_load(path.read_text(encoding="utf-8"))
    body["depends_on"] = {"quran.uthmani": ">=9.9.9"}
    path.write_text(yaml.safe_dump(body, sort_keys=False, allow_unicode=True), encoding="utf-8")

    result = _run("deps", "check", "--root", str(project.root), "--json")
    assert result.exit_code == 1
    assert _json(result)["errors"]


def test_change_new_then_list(project):
    created = _run(
        "change",
        "new",
        "add a note",
        "-c",
        "tr.en.sahih",
        "--root",
        str(project.root),
        "--json",
    )
    assert created.exit_code == 0
    change_id = _json(created)["id"]

    listed = _run("change", "list", "--root", str(project.root), "--json")
    assert [c["id"] for c in _json(listed)] == [change_id]


def test_query_is_capped_and_read_only(project):
    from nimaz_data.build.promote import promote

    result = build(project)
    promote(result.candidate, project.out_dir, receipt=result.receipt)

    ok = _run(
        "query",
        "SELECT ayah_id FROM translations",
        "--root",
        str(project.root),
        "--limit",
        "2",
        "--json",
    )
    assert _json(ok)["count"] == 2

    bad = _run(
        "query", "DROP TABLE translations", "--root", str(project.root), "--json"
    )
    assert bad.exit_code == 1


def test_rules_command_lists_what_will_run(project):
    result = _run("rules", "--root", str(project.root), "--json")
    ids = {r["id"] for r in _json(result)}
    assert {"key.unique", "provenance.complete", "translation.coverage"} <= ids


def test_ui_says_what_is_not_built_yet(project):
    result = runner.invoke(app, ["ui", "--root", str(project.root)])
    assert result.exit_code != 0
