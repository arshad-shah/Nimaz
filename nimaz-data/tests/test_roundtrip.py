"""§11.3 — the step that cannot be skipped.

Until the round trip proves the NDJSON sources are a lossless re-encoding of the
corpus, the database is still the source of truth and no change should be
authored. §14 re-runs the same assertion on every merge, so the proof does not
quietly stop being true.
"""

from __future__ import annotations

from nimaz_data.build.bootstrap import export_sources
from nimaz_data.build.guard import compare_against_vault
from nimaz_data.build.pipeline import build
from nimaz_data.core import db as dbx, vault as vaultx
from nimaz_data.core.hash import records_hash
from nimaz_data.core.ndjson import read_records


def test_init_produces_a_collection_per_content_table(project):
    names = set(project.specs())
    assert names == {
        "quran.uthmani",
        "quran.surahs",
        "mushaf.indopak16",
        "tr.en.sahih",
        "tr.bn.bengali",
    }
    # The user table is schema-only: it gets no collection, and no floor.
    assert "quran_bookmarks" in project.schema_sql()


def test_sources_match_the_vault_exactly(project):
    """Every exported NDJSON file hashes to the same content as the vault table."""
    conn = dbx.open_vault(vaultx.corpus_path(project.vault_dir))
    try:
        for name, spec in project.specs().items():
            vault_hash = records_hash(dbx.read_collection(conn, spec), spec.key)
            source_hash = records_hash(read_records(spec.records_path), spec.key)
            assert vault_hash == source_hash, name
    finally:
        conn.close()


def test_build_from_sources_is_lossless(project):
    """The whole point: compile from text and land back on the corpus."""
    result = build(project, skip_vault=False)
    assert result.ok, (result.failed_stage, result.error)

    vault = dbx.open_vault(vaultx.corpus_path(project.vault_dir))
    candidate = dbx.open_readonly(result.candidate)
    try:
        report = compare_against_vault(vault, candidate, project.specs())
    finally:
        vault.close()
        candidate.close()

    assert report.ok, [f.to_dict() for f in report.findings]
    assert set(report.checked) == set(project.specs())


def test_candidate_export_round_trips_again(project):
    """What main.yml does: export the candidate back out, rebuild, compare (§14)."""
    first = build(project)
    assert first.ok, first.error

    reports = export_sources(project, first.candidate)
    assert all(not r.changed for r in reports), [r.to_dict() for r in reports if r.changed]

    second = build(project)
    assert second.ok, second.error
    assert first.artifact_hash == second.artifact_hash


def test_surrogate_ids_do_not_leak_into_content(project):
    """translations.id is a surrogate: excluded from the source and from the hash."""
    spec = project.specs()["tr.en.sahih"]
    records = read_records(spec.records_path)
    assert records, "no rows exported"
    assert "id" not in records[0]
    assert "translator_id" not in records[0]
    assert set(records[0]) == {"ayah_id", "text"}
