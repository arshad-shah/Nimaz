# Ayah division columns (audit §2.4): Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the ruku' and hizb-quarter lookups out of the Quran read path and into the content build, so the eight duplicated `QuranDao` projections become plain indexed selects.

**Architecture:** Four derived columns on `ayahs` — `ruku_number`, `ruku_end_ayah_id`, `rub_number`, `rub_start_ayah_id` — computed by the nimaz-data importer from the same Tanzil metadata that already produces the `rukus` and `hizb_quarters` collections, and cross-checked against those tables by a validation rule at build time. The app then reads columns instead of computing range joins.

**Tech Stack:** nimaz-data (Python 3.11, `nz` pipeline), Room 2.8.4 / SQLite, Kotlin.

## Global Constraints

- **Three phases across two repositories, in this order.** See "Why the spec's ordering was wrong" below — this is not the sequence the design doc originally described.
- nimaz-data work happens in `/Users/ShahA/Documents/practice/nimaz-data` on a branch off `main`; the app work happens in `/Users/ShahA/StudioProjects/Nimaz` on the `epic/audit` stack.
- nimaz-data gate: `make check` — "exactly what CI runs, so green here means a green PR".
- App gate: `compileDebugKotlin`, `testDebugUnitTest`, `lintDebug`, `check_docs.py`.
- Commit messages carry no `Co-Authored-By` trailer.
- Issues: [nimaz-data#16](https://github.com/arshad-shah/nimaz-data/issues/16) and Nimaz #489. Epic: #460.

---

## Why the spec's ordering was wrong

`docs/superpowers/specs/2026-08-10-code-audit-remediation-design.md` §3 says nimaz-data goes first and the app follows. **It cannot.**

`nz build` stage 8 (`app`) validates the compiled candidate against **the app's exported Room schema**, which nimaz-data does not author — it imports it:

```
nimaz_data/app/sync.py:32
SCHEMA_EXPORT_DIR = "app/schemas/com.arshadshah.nimaz.data.local.database.NimazDatabase"
```

`check()` in `nimaz_data/app/contract.py` compares `PRAGMA user_version` against the exported schema version, compares the identity hash, and checks every table's columns. `data/console.yaml` currently stamps `user_version: 24`, and the app's newest export is `app/schemas/…/24.json`.

So a nimaz-data build that adds four columns to `ayahs` while the app still exports schema 24 **fails stage 8** — correctly, because an artifact whose shape the app does not know is exactly what that gate exists to stop.

The real sequence is therefore:

| Phase | Repo | What | Gates the next |
|---|---|---|---|
| **A** | Nimaz | Entity columns, `NIMAZ_DATABASE_VERSION` 24→25, migration 24→25, export `25.json` | nimaz-data cannot build without `25.json` |
| **B** | nimaz-data | `nz app sync`, importer derivation, validation rule, `user_version: 25`, release `data-v9` | the app cannot read columns that no artifact carries |
| **C** | Nimaz | Pin `data-v9`, rewrite the eight queries, drop the range joins | — |

Phase A ships columns that are empty until Phase B's artifact arrives, so **Phase A must not change a single query**. That is what makes it safe to land early in the stack.

Update the spec's §3 and epic #460's cross-repo note as part of Phase A.

---

# Phase A — app schema (Nimaz repo)

Branch: `epic/audit-09a-ayah-division-schema`, added to the stack with `gh stack add`.

### Task A1: Add the four columns to the ayah entity

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/data/local/database/entity/QuranEntities.kt` (the `tableName = "ayahs"` entity)
- Modify: `app/src/main/java/com/arshadshah/nimaz/data/local/database/NimazDatabase.kt:63` and the migration list

**Interfaces:**
- Produces: four nullable `Int?` columns on the ayah entity — `rukuNumber` / `ruku_number`, `rukuEndAyahId` / `ruku_end_ayah_id`, `rubNumber` / `rub_number`, `rubStartAyahId` / `rub_start_ayah_id`. Phase B's importer must emit exactly these SQL names; Phase C's queries read them.

**Why nullable:** between this PR shipping and `data-v9` reaching a device, the columns exist and are empty. A non-null column with a default would make "not yet populated" indistinguishable from "ruku' 0", and Phase C's queries need to tell those apart to decide whether to fall back.

- [ ] **Step 1: Write the failing migration test**

Add to the existing migration test suite (alongside `MigrationTest`/`MigrationChainTest`):

```kotlin
@Test
fun migrate24To25_addsAyahDivisionColumns() {
    helper.createDatabase(TEST_DB, 24).close()

    val db = helper.runMigrationsAndValidate(TEST_DB, 25, true, MIGRATION_24_25)

    val columns = db.query("PRAGMA table_info(ayahs)").use { cursor ->
        buildSet {
            while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
    }
    assertThat(columns).containsAtLeast(
        "ruku_number", "ruku_end_ayah_id", "rub_number", "rub_start_ayah_id",
    )
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests '*Migration*'`
Expected: FAIL — `MIGRATION_24_25` is unresolved.

- [ ] **Step 3: Add the columns to the entity**

In `QuranEntities.kt`, on the `ayahs` entity, add:

```kotlin
    @ColumnInfo(name = "ruku_number") val rukuNumber: Int? = null,
    @ColumnInfo(name = "ruku_end_ayah_id") val rukuEndAyahId: Int? = null,
    @ColumnInfo(name = "rub_number") val rubNumber: Int? = null,
    @ColumnInfo(name = "rub_start_ayah_id") val rubStartAyahId: Int? = null,
```

- [ ] **Step 4: Write the migration and bump the version**

In `NimazDatabase.kt`, change `const val NIMAZ_DATABASE_VERSION = 24` to `25`, and add:

```kotlin
/**
 * Four derived columns on `ayahs`, so the reader stops recomputing a verse's ruku'
 * and hizb-quarter on every read. They are computed by nimaz-data and arrive with
 * the content artifact (data-v9); until one does they are NULL, which is why they
 * are nullable rather than defaulted. See audit §2.4 / arshad-shah/nimaz-data#16.
 */
val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ayahs ADD COLUMN ruku_number INTEGER")
        db.execSQL("ALTER TABLE ayahs ADD COLUMN ruku_end_ayah_id INTEGER")
        db.execSQL("ALTER TABLE ayahs ADD COLUMN rub_number INTEGER")
        db.execSQL("ALTER TABLE ayahs ADD COLUMN rub_start_ayah_id INTEGER")
    }
}
```

Register it wherever the other migrations are listed, and confirm `MigrationChainTest` picks it up — that test is the reason the chain is trustworthy, so it must exercise 24→25 without being edited to special-case it.

- [ ] **Step 5: Run the migration tests**

Run: `./gradlew :app:testDebugUnitTest --tests '*Migration*'`
Expected: PASS, including the existing chain test.

- [ ] **Step 6: Export the Room schema**

Run: `./gradlew :app:kspDebugKotlin`
Then: `ls app/schemas/com.arshadshah.nimaz.data.local.database.NimazDatabase/`
Expected: a new `25.json`. **This file is Phase B's input** — nimaz-data reads the identity hash out of it and can do nothing until it exists.

- [ ] **Step 7: Update the docs that own this**

`docs/SUBSYSTEMS.md` §5 — the schema-version line, which `scripts/check_docs.py` check SUB-01 compares against `NIMAZ_DATABASE_VERSION`. Also record the migration in whatever migration table that section carries.

Run: `python3 scripts/check_docs.py`
Expected: `All 23 documentation checks passed` — SUB-01 in particular now reads 25.

- [ ] **Step 8: Correct the design doc's ordering**

In `docs/superpowers/specs/2026-08-10-code-audit-remediation-design.md` §3, replace the "nimaz-data goes first" claim with the three-phase table from this plan. A spec that describes an impossible order is worse than no spec.

- [ ] **Step 9: Full gate, then commit**

```bash
./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest && ./gradlew :app:lintDebug
python3 scripts/check_docs.py
git add app/ docs/
git commit -m "feat(db): add ayah division columns at schema 25

Four nullable columns on ayahs — ruku_number, ruku_end_ayah_id, rub_number,
rub_start_ayah_id — so the reader can stop recomputing a verse's ruku' and
hizb-quarter with range joins on every read.

Empty until a data-v9 artifact carries them, so no query changes here. The
export at app/schemas/.../25.json is what nimaz-data validates its candidate
against, which is why this lands before the content work rather than after.

Refs #489, arshad-shah/nimaz-data#16"
```

- [ ] **Step 10: Update the tracking**

```bash
gh issue comment 489 --body "Phase A landed: schema 25 exports the four columns, empty for now. nimaz-data#16 is unblocked — it can \`nz app sync\` the new export."
gh issue comment 16 -R arshad-shah/nimaz-data --body "Unblocked: Nimaz now exports app/schemas/.../25.json with the four columns. Phase B can start."
```

---

# Phase B — the content build (nimaz-data repo)

Branch off `main`. **Do not start until `25.json` exists in the app repo.**

### Task B1: Import the app's new schema

**Files:**
- Modify: `data/console.yaml` (`user_version: 24` → `25`)
- Create: `data/app/room/25.json` (written by the sync command, not by hand)

- [ ] **Step 1: Confirm the app export is present**

```bash
ls /Users/ShahA/StudioProjects/Nimaz/app/schemas/com.arshadshah.nimaz.data.local.database.NimazDatabase/25.json
```
Expected: the file exists. If not, Phase A is not finished — stop.

- [ ] **Step 2: Bump the stamped version**

In `data/console.yaml`, change `user_version: 24` to `user_version: 25`, and add a line to the comment block above it in the established style, e.g.:

```yaml
# 25, not 24: four derived columns on `ayahs` (ruku_number, ruku_end_ayah_id,
# rub_number, rub_start_ayah_id). Added columns, so the app needs a Room
# migration to meet them — hence a version move rather than a content patch.
user_version: 25
```

- [ ] **Step 3: Sync the schema**

```bash
make setup   # if the venv is not already there
.venv/bin/nz app sync --app-repo /Users/ShahA/StudioProjects/Nimaz
```
Expected: `data/app/room/25.json` written, reporting version 25 and an identity hash.

- [ ] **Step 4: Commit the sync on its own**

```bash
git add data/console.yaml data/app/room/25.json
git commit -m "app: import the Nimaz schema 25 export

Four derived columns on ayahs. The build cannot produce them until the
importer does (next commit); this is the contract they will be checked against."
```

### Task B2: Derive the columns in the importer

**Files:**
- Modify: `data/collections/quran.uthmani/collection.yaml` (schema block, `version`)
- Modify: `data/importers/quran.py` (the ayahs records) and/or `data/importers/quran_structure.py` (reuse its range helpers)

**Interfaces:**
- Consumes: the Tanzil `quran-data.xml` ranges that `quran_structure.py` already parses via `_starts(root, group)` and `_ranges(starts)` to build `rukus` (556 rows) and `hizb_quarters` (240 rows).
- Produces: four new integer fields on every one of the 6,236 ayah records, named exactly `ruku_number`, `ruku_end_ayah_id`, `rub_number`, `rub_start_ayah_id`.

**Design note:** derive from the **same upstream** that produces `rukus` and `hizb_quarters`, not from the compiled tables. The whole point of this repo is that the database is a build output — deriving one output from another would make the NDJSON no longer describe the artifact. The validation rule in Task B3 is what proves the two derivations agree.

- [ ] **Step 1: Write the failing test**

Create `tests/test_ayah_divisions.py`:

```python
"""The four derived columns must agree with the ranges they are derived from."""
import pytest

from data.importers import quran_structure


def test_every_ayah_falls_in_exactly_one_ruku(ayah_records, ruku_ranges):
    for record in ayah_records:
        number = record["ruku_number"]
        start, end = ruku_ranges[number]
        assert start <= record["id"] <= end, (
            f"ayah {record['id']} claims ruku' {number}, whose range is {start}-{end}"
        )


def test_ruku_end_ayah_id_matches_the_ruku_range(ayah_records, ruku_ranges):
    for record in ayah_records:
        assert record["ruku_end_ayah_id"] == ruku_ranges[record["ruku_number"]][1]


def test_every_ayah_falls_in_exactly_one_rub(ayah_records, rub_ranges):
    for record in ayah_records:
        number = record["rub_number"]
        start, end = rub_ranges[number]
        assert start <= record["id"] <= end


def test_rub_start_ayah_id_matches_the_rub_range(ayah_records, rub_ranges):
    for record in ayah_records:
        assert record["rub_start_ayah_id"] == rub_ranges[record["rub_number"]][0]


def test_the_derivation_covers_the_whole_corpus(ayah_records):
    assert len(ayah_records) == 6236
    assert all(r["ruku_number"] is not None for r in ayah_records)
    assert all(r["rub_number"] is not None for r in ayah_records)
    assert len({r["ruku_number"] for r in ayah_records}) == 556
    assert len({r["rub_number"] for r in ayah_records}) == 240
```

Add the three fixtures to `tests/conftest.py`, following whatever pattern `tests/test_thematic.py` already uses to reach importer output — reuse it rather than inventing a second way in.

- [ ] **Step 2: Run to verify it fails**

Run: `make test` (or `.venv/bin/pytest tests/test_ayah_divisions.py -v`)
Expected: FAIL — `KeyError: 'ruku_number'`.

- [ ] **Step 3: Declare the columns in the collection**

In `data/collections/quran.uthmani/collection.yaml`, bump `version: 2.0.0` → `2.1.0`, and add to `schema:`:

```yaml
  ruku_number: int
  ruku_end_ayah_id: int
  rub_number: int
  rub_start_ayah_id: int
```

Extend the `provenance.notes` to say these are **derived**, from the same Tanzil metadata as `quran.rukus` and `quran.hizb-quarters`, so a later reader does not go looking for an upstream file that carries them.

- [ ] **Step 4: Implement the derivation**

Factor the range lookup out of `quran_structure.py` so both collections use one implementation — a function taking the parsed ranges and returning, for an ayah id, the containing `(number, start, end)`. Ranges are contiguous and sorted, so `bisect` over the start offsets is the right shape; a linear scan per ayah is 6,236 × 796 comparisons for no reason.

Then emit the four fields on each ayah record.

- [ ] **Step 5: Run the tests**

Run: `make test`
Expected: all pass, including the existing 113.

- [ ] **Step 6: Commit**

```bash
git add data/collections/quran.uthmani/collection.yaml data/importers/ tests/
git commit -m "quran: derive each ayah's ruku' and hizb-quarter at import

Four columns on ayahs, from the same Tanzil ranges that produce quran.rukus
and quran.hizb-quarters. The app was recomputing these on every read with two
range joins and a regrouping subquery over the whole rukus table — including
for single-ayah lookups.

Refs #16"
```

### Task B3: Make the build prove the derivation

**Files:**
- Modify: `data/rules/quran.py`

**Interfaces:**
- Consumes: the compiled candidate connection, as the other rules in this file do.
- Produces: a rule that fails stage 6 (`validate`) if any ayah's derived columns disagree with the `rukus` / `hizb_quarters` tables in the same candidate.

**Why both this and the importer test:** the test proves the derivation is right at import time. This rule proves it is *still* right in the compiled artifact — after changes have been applied (stage 3) and the tables written (stage 4). They catch different failures, and the issue asks for the build-time one specifically.

- [ ] **Step 1: Write the rule, following the file's existing shape**

Add to `data/rules/quran.py` a rule that runs, in SQL, the equivalent of the range joins the app is about to delete, and reports every row where the stored column disagrees:

```sql
SELECT a.id, a.ruku_number, r.number
  FROM ayahs a
  LEFT JOIN rukus r ON a.id BETWEEN r.start_ayah_id AND r.end_ayah_id
 WHERE a.ruku_number IS NOT r.number
    OR a.ruku_end_ayah_id IS NOT r.end_ayah_id
 LIMIT 20
```

…and the matching query for `hizb_quarters` / `rub_number` / `rub_start_ayah_id`. Report each mismatch as a finding in the file's established format; a `LIMIT` keeps the failure readable rather than printing 6,236 rows.

This is the one place the range join should still exist. It runs once per build, not once per read.

- [ ] **Step 2: Verify the rule fires when the data is wrong**

Temporarily corrupt one record's `ruku_number` in the importer output, run `make check`, and confirm the build fails at the `validate` stage naming that ayah. **Then revert the corruption.** A validation rule never observed failing is not known to work.

- [ ] **Step 3: Run the full pipeline**

```bash
make check
```
Expected: green — all eight stages, including `app` (stage 8) now checking against schema 25, and `guard` (stage 7) accepting four added columns against the floors and protected fields.

If `guard` objects to the added columns, that is a genuine policy question about `receipts/genesis` and not something to force past — stop and report it.

- [ ] **Step 4: Build and promote**

```bash
make build
make promote
make doctor
```
Expected: `doctor` reports the new artifact as current, stamped `user_version` 25.

- [ ] **Step 5: Commit and open the PR**

```bash
git add data/rules/quran.py
git commit -m "rules: check the derived ayah divisions against their source ranges

The range joins the app is deleting move here, where they run once per build
instead of once per read.

Closes #16"
git push -u origin HEAD
gh pr create -R arshad-shah/nimaz-data --fill
```

- [ ] **Step 6: Cut the release**

After the PR merges, tag `data-v9` following whatever `data-v8` did (check `git tag -n` and `.github/workflows/` for the release lane). Then comment on Nimaz #489 with the tag and the artifact identity hash, because Phase C pins to it.

---

# Phase C — the app reads columns (Nimaz repo)

Branch: `epic/audit-09-ayah-division-queries`. **Blocked until `data-v9` is released.**

### Task C1: Pin the new artifact

- [ ] **Step 1: Find the pin and bump it**

```bash
grep -rn "data-v8" gradle/ app/build.gradle.kts *.gradle.kts 2>/dev/null
```
Change it to `data-v9`, then `./gradlew :app:fetchNimazData` and confirm it resolves.

- [ ] **Step 2: Verify the artifact actually carries the columns**

```bash
sqlite3 <path to the fetched artifact> "PRAGMA table_info(ayahs)" | grep -E "ruku_|rub_"
sqlite3 <path to the fetched artifact> "SELECT COUNT(*) FROM ayahs WHERE ruku_number IS NULL"
```
Expected: four rows, and a count of **0**. If the count is non-zero, stop — Phase C's whole premise is that the columns are populated, and a partial fill would produce silently wrong ruku' badges rather than an error.

### Task C2: Rewrite the projection

**Files:**
- Modify: `app/src/main/java/com/arshadshah/nimaz/data/local/dao/QuranDao.kt` and the `@DatabaseView` added by PR 8

**Interfaces:**
- Consumes: `ayah_with_text`, the view declared in PR 8 (stack PR 8, issue #488).
- Produces: no API change — the DAO's Kotlin signatures and the domain models are untouched. This is the point: it is a pure query-shape change, so the existing repository and DAO tests are the regression net.

- [ ] **Step 1: Confirm the tests that will catch a regression exist and pass**

```bash
./gradlew :app:testDebugUnitTest --tests '*AyahDivisions*' --tests '*QuranDao*' --tests '*QuranRepository*'
```
Expected: PASS. `AyahDivisionsTest` already pins this logic, and PRs 6/7 added the DAO and repository coverage. **If they do not pass before the change, do not make the change** — you would have no way to tell the rewrite from a pre-existing break.

- [ ] **Step 2: Rewrite the view**

In the `@DatabaseView` from PR 8, delete the two range joins and the `MIN(number)` regrouping subquery, and select `a.ruku_number`, `a.ruku_end_ayah_id`, `a.rub_number`, `a.rub_start_ayah_id` directly. The view's output columns must keep their existing names so nothing downstream moves.

- [ ] **Step 3: Run the full unit suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS with no test edited. A test that needed changing means the rewrite changed behaviour, which it must not.

- [ ] **Step 4: Measure, so the claim is evidence**

Time `getAyahWithTextById` and `getAyahsWithTextBySurah` for Al-Baqarah before and after (an instrumented benchmark, or `EXPLAIN QUERY PLAN` showing the `SCAN rukus` gone). Put both numbers in the PR body. This PR's entire justification is performance; ship it with a measurement or the next audit will ask the same question again.

- [ ] **Step 5: Docs, gate, commit**

Update `docs/SUBSYSTEMS.md` §5 for the query-shape change, then run the four gate commands, then:

```bash
git add app/ docs/
git commit -m "perf(quran): read precomputed ayah divisions instead of range joins

The projection joined rukus and hizb_quarters on BETWEEN — which SQLite cannot
serve from an index — and re-grouped the whole rukus table in a subquery on
every call, including single-ayah lookups. nimaz-data now ships the four values
as columns (data-v9), so all of it becomes a plain indexed select.

Closes #489"
```

- [ ] **Step 6: Close out the tracking**

Comment the before/after numbers on #489 and on nimaz-data#16, and tick the relevant line in the epic #460 body.
