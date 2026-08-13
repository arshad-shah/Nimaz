"""Tests for scripts/coverage_summary.py.

Run: python3 -m pytest scripts/test_coverage_summary.py -v
"""

import subprocess
import sys
import textwrap
from pathlib import Path

SCRIPT = Path(__file__).resolve().parent / "coverage_summary.py"

FIXTURE = textwrap.dedent(
    """\
    <?xml version="1.0" encoding="UTF-8"?>
    <report name="app">
      <counter type="INSTRUCTION" missed="300" covered="700"/>
      <counter type="BRANCH" missed="40" covered="60"/>
      <counter type="LINE" missed="25" covered="75"/>
    </report>
    """
)


def _run(*args):
    return subprocess.run(
        [sys.executable, str(SCRIPT), *args], capture_output=True, text=True
    )


def test_summary_reports_each_counter_as_a_percentage(tmp_path: Path):
    xml = tmp_path / "jacocoTestReport.xml"
    xml.write_text(FIXTURE)

    result = _run(str(xml))

    assert result.returncode == 0, result.stderr
    assert "75.0%" in result.stdout  # lines: 75 covered of 100
    assert "60.0%" in result.stdout  # branches: 60 covered of 100
    assert "70.0%" in result.stdout  # instructions: 700 covered of 1000


def test_summary_never_fails_on_a_missing_report(tmp_path: Path):
    """A PR must not go red because coverage did not run."""
    result = _run(str(tmp_path / "nope.xml"))

    assert result.returncode == 0
    assert "no coverage report" in result.stdout.lower()


def test_a_report_with_no_classes_says_so_loudly(tmp_path: Path):
    """The exact shape jacoco produced here for months: a sessioninfo and nothing else.

    An empty table reads as a formatting glitch, so it has to name the cause.
    """
    xml = tmp_path / "empty.xml"
    xml.write_text(
        '<?xml version="1.0"?><report name="app">'
        '<sessioninfo id="x" start="0" dump="1"/></report>'
    )

    result = _run(str(xml))

    assert result.returncode == 0
    assert "no classes" in result.stdout.lower()
    assert "classDirectories" in result.stdout


def test_summary_does_not_divide_by_zero(tmp_path: Path):
    xml = tmp_path / "zero.xml"
    xml.write_text(
        '<?xml version="1.0"?><report name="app">'
        '<counter type="LINE" missed="0" covered="0"/></report>'
    )

    result = _run(str(xml))

    assert result.returncode == 0
    assert "0.0%" in result.stdout
