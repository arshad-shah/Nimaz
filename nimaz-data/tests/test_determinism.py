"""§6 — determinism, and why it is an integrity check rather than a nicety.

The artifact hash *is* the build identity. A non-deterministic build means the
hash is not the identity you believe it is, so `main.yml` builds twice and
compares; these tests are the same assertion at unit scale.
"""

from __future__ import annotations

import shutil

from nimaz_data.build.pipeline import build
from nimaz_data.core.hash import sha256_file
from nimaz_data.core.ndjson import read_records, write_records


def test_two_builds_produce_a_byte_identical_file(project):
    first = build(project)
    second = build(project)
    assert first.ok and second.ok
    assert first.candidate != second.candidate
    assert sha256_file(first.candidate) == sha256_file(second.candidate)


def test_source_line_order_does_not_change_the_artifact(project):
    """A hand-edit that appends a line rather than inserting it sorts back into place."""
    baseline = build(project)
    assert baseline.ok

    spec = project.specs()["tr.en.sahih"]
    records = read_records(spec.records_path)
    shuffled = records[5:] + records[:5]
    with open(spec.records_path, "wb") as fh:
        from nimaz_data.core.canonical import canonical_line

        for record in shuffled:
            fh.write(canonical_line(record))

    reshuffled = build(project)
    assert reshuffled.ok
    assert reshuffled.artifact_hash == baseline.artifact_hash


def test_rewriting_a_source_file_is_a_no_op(project):
    """A file rewritten by the tool is byte-identical to one rewritten by hand (§3)."""
    spec = project.specs()["quran.uthmani"]
    before = spec.records_path.read_bytes()
    write_records(spec.records_path, read_records(spec.records_path), spec.key)
    assert spec.records_path.read_bytes() == before


def test_manifest_travels_inside_the_artifact(project):
    from nimaz_data.core import db as dbx, manifest

    result = build(project)
    assert result.ok
    conn = dbx.open_readonly(result.candidate)
    try:
        entries = manifest.read(conn)
    finally:
        conn.close()

    assert set(entries) == set(project.specs())
    for name, entry in entries.items():
        assert entry.rows > 0, name
        assert len(entry.content_hash) == 64


def test_a_moved_project_still_hashes_the_same(project, tmp_path):
    """Nothing about the build depends on where the project lives on disk."""
    from nimaz_data.core.project import Project

    first = build(project)
    assert first.ok

    clone_root = tmp_path / "elsewhere"
    shutil.copytree(project.root, clone_root, symlinks=True)
    clone = Project(clone_root)
    shutil.rmtree(clone.build_dir, ignore_errors=True)
    clone.build_dir.mkdir(parents=True, exist_ok=True)

    second = build(clone)
    assert second.ok, second.error
    assert second.artifact_hash == first.artifact_hash
