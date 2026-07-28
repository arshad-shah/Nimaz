# nimaz-data — the Nimaz Data Console

`nz`. One core, three front doors (TUI, CLI, MCP). The design this implements is
[`docs/nimaz-data-console.md`](../docs/nimaz-data-console.md); section numbers
below refer to it.

The short version: **the database is a build output, not a source.** It is
compiled from NDJSON text, validated in full, checked against a sealed original,
and only then promoted. Every write — console edit, hand-written SQL, agent
proposal — becomes the same artifact: a change directory that the pipeline
cannot tell the origin of.

## Status

Working and under test — 46 unit tests, plus a full bootstrap run against the real
147 MB shipped corpus (24 collections, 60,922 content rows) that came back **byte-identical**:

| | |
|---|---|
| **§2 three databases** | vault (`ro&immutable`, 444), candidate (`.build/`), distribution (`out/`) |
| **§4 collections** | `collection.yaml`, per-collection versions, floors, protected fields, shared-table splits |
| **§5 change funnel** | change dirs, `expect{}` enforcement, collateral detection, protected second lock, `fold` |
| **§6 build** | all six stages, byte-reproducible artifact, corpus guard |
| **§7 output** | `build.json` receipt, atomic promote, pointer rollback, retention |
| **§8 rules** | registry, discovery from `data/rules/`, 15 rules shipped, GitHub annotations |
| **§9 agent** | all six tools as functions + `--json` CLI parity; MCP shell behind the `agent` extra |
| **§10 UI** | **L0 tokens only.** L1–L4 not built — see [`nimaz_data/ui/README.md`](nimaz_data/ui/README.md) |
| **§11 bootstrap** | `nz vault seal`, `nz init`, `nz build --against-vault`, genesis fingerprint |
| **§13 genesis** | written once, gzipped key lists, guard walks it without opening the vault |
| **§16 importers** | **not built.** `kind:` and structured provenance exist; `nz import` does not |

Deviations from the document are listed at the end of this file.

## The bootstrap, actually run

Numbers below are from a real run against the **shipped** asset,
`app/src/main/assets/database/nimaz_prepopulated.db` (sha256 `6b57b4d4…`,
147,292,160 bytes, `user_version = 12`), not from the test fixture.

**Seal the shipped asset, not `nimaz-pro-data/output/`.** The two differ: the
generator output carries **379 hadiths with empty `text_arabic`/`text_english`**
that the JSON sources do not have and the shipped asset does not have. It is
stale, damaged output, and shipping it would blank 379 hadiths on device. The
`hadith.non-empty` rule now catches it — `nz validate --db
nimaz-pro-data/output/nimaz_prepopulated.db --rule hadith.non-empty` reports all
379. That is the whole argument for this tool in one example: the old pipeline
had no stage that would have said so.

| | |
|---|---|
| vault sealed | 1.6 s |
| `nz init` — 24 collections + 20 user tables (schema only) | 5.6 s → 125 MB NDJSON |
| `nz build --against-vault` — 60,922 content rows | 15.3 s |
| **round trip** | **24/24 collections, 38/38 tables byte-identical to the vault** |
| three consecutive builds | byte-identical (`cmp`), same artifact hash each time |
| full rule run | 13 rules, **1 blocking finding** |

The round trip was re-checked by a script that does not import `nimaz_data` at
all — plain `sqlite3` + `hashlib`, XOR of per-row digests so row order cannot
mask a difference. The candidate's only structural difference from the vault is
the `_manifest` table the build adds.

**The one blocking finding was real, and it is on devices.** `ayahs.text_arabic` and
`ayahs.text_uthmani` both carry `U+FEFF` at index 0 of ayah 1:1 — a BOM inside
the Bismillah. `generate_database.py` strips it from `text_arabic` via
`normalise_uthmani` (#290) but never from `text_uthmani`, and the shipped asset
predates that fix, so regenerating would not have caught it either.

Fixing it exercised the strictest path in the system, because both columns are
`protected`:

```
change declares neither          -> stage 3: "modifies protected fields without declaring them"
change declares expect{} only    -> stage 3: same refusal
change declares protected: [...] -> stage 3: "re-run with --confirm-protected"
nz build --confirm-protected     -> green, 0 blocking
```

The promoted artifact then differs from the vault in **exactly one table, one
row, two columns**; the other 60,921 rows are byte-identical.

Two rules were wrong when real data hit them, and were corrected rather than
suppressed: a single `text.no-control-chars` rule blocked on ZWJ inside
`dua_categories.icon` (a legitimate `👨‍👩‍👧` emoji sequence) and on ZWNJ
inside Arabic quoted in the tafseer (meaningful Urdu/Persian typography). It is
now `text.no-bom` (blocking — a BOM, NUL or ZWSP is never legitimate inside a
stored string) and `text.zero-width` (advisory — those characters carry meaning).

### Why not commit the database (§12), measured

42 commits, the same corpus, one row edited per commit:

| | git history |
|---|---|
| `nimaz.db` committed, before `git gc` | **1,294 MB** |
| `nimaz.db` committed, after `git gc` (76 s of CPU) | 47 MB |
| NDJSON sources committed, after `git gc` | **22 MB** |

Packed, git deltas SQLite better than §12 implies — but that is the number you
get only after paying 76 seconds per repack, and **this repo stores the database
through Git LFS, where no delta applies at all**: LFS keeps every version whole,
so 42 versions is 42 × 140 MB of storage and bandwidth. The diff is the other
half of it — the same edit reads as `1 insertion(+), 1 deletion(-)` in NDJSON and
`Bin 147292160 -> 147292160 bytes` in SQLite. One of those is reviewable.

`data/collections/` is **not committed** in this branch. The round trip proves it
could be, but 125 MB of text in the Android repo is a call for a human to make —
and §12's own answer is that the console eventually lives in its own repo.

## Install

```bash
cd nimaz-data
pip install -e '.[dev]'        # add ,tui or ,agent when those land
```

Python 3.11+. No other runtime dependency: the build is pure `sqlite3`, `yaml`
and `typer`, and after `nz import` (when it exists) nothing touches the network.

## Bootstrap — §11, in order

```bash
nz vault seal ../app/src/main/assets/database/nimaz_prepopulated.db   # 1  archive + 444 + hash
nz doctor                                                      #    confirm the vault is intact
nz init                                                        # 2  vault -> schema.sql + collections + genesis
nz build --against-vault                                       # 3  THE ROUND TRIP — must be lossless
```

**Step 3 is the one that cannot be skipped.** It compiles from the exported
NDJSON and asserts, per collection, that the result is hash-identical to the
vault. Until it passes, the database is still the source of truth and no change
should be authored. `nz init` leaves a `TODO` in every collection's provenance —
filling those in is part of the bootstrap, and `provenance.complete` blocks the
build until the attributed collections have a translator, a license and a
retrieval date.

Then commit `data/`, and from there the vault is a reference rather than a
dependency: the guard runs against `genesis.json` and the receipt chain.

## Daily use

```bash
nz doctor                                   # vault, sources, pending changes, current artifact
nz build                                    # six gates; candidate stays in .build/ either way
nz promote                                  # atomic swap into out/current
nz rollback                                 # repoint current at previous

nz change new "fix ikhfa overlap" -c tajweed.spans
nz change list
nz change fold                              # replay into sources; refuses if the hash moves

nz validate --annotate github               # inline annotations on the offending NDJSON line
nz rules                                    # what will run
nz describe                                 # what is in the artifact
nz query "SELECT * FROM ayahs LIMIT 5"      # read-only, row-capped
nz deps check                               # every depends_on satisfiable by this build
```

Every command takes `--json` and returns the same payload the MCP server
returns. `--root` picks a project explicitly; otherwise the nearest one wins.

## Authoring a change

A change is a directory. `nz change new` writes the skeleton; you fill in the
SQL and — this is the part that matters — the `expect{}`.

```yaml
id: 20260728T1412_tajweed-v3-surah2
title: Import v3 tajweed rule-set for surah 2
author: arshad
origin: console            # console | hand | agent — audit only, no behaviour
collections:
  tajweed.spans:
    bump: minor
    expect:
      rows_delta: +43
      rows_after: 41904
      keys_touched: 12
rationale: |
  v3 parser resolves the ikhfa/madd_lazim overlap reported in #287.
```

`up.sql` runs against a working database loaded from the NDJSON sources — not
against a shipped artifact — so a change is authored as SQL while the thing that
gets compiled is still the record set. Nothing persists between builds, so there
is no "applied" state that can drift.

Declare `+43` and deliver `-4000` and the build stops at stage 3. Touch a
collection you did not declare and it stops at stage 3. Touch `text_uthmani`
without declaring it *and* passing `--confirm-protected` and it stops at stage 3.

## Adding a collection

A file. `data/collections/<name>/collection.yaml` plus `records.ndjson`:

```yaml
name: tr.bn.bengali
kind: translation          # picks the rule set — coverage, non-empty, provenance
version: 1.0.0
key: [ayah_id]
source:
  table: translations      # one edition out of a shared table
  where: { translator_id: bn.bengali }
  exclude_columns: [id]    # surrogate; excluded from the content hash
floors:
  rows_min: 6236
provenance:
  translator: Muhiuddin Khan
  license: see LICENSES_TRANSLATIONS.md
  retrieved: 2026-07-27
schema:
  ayah_id: int
  text: text
```

`kind:` is what removes the per-collection code: a `translation` compiles to the
same table shape and inherits the same rules whatever the language.

## Writing a rule

`data/rules/*.py`, discovered by the registry. A rule reads and yields; it never
writes to a database.

```python
from nimaz_data.rules import Failure, rule

@rule(id="span.overlap", scope="tajweed.spans", severity="blocking")
def no_overlap(ctx):
    """No two tajweed spans on the same ayah overlap."""
    for (surah, ayah), spans in ctx.grouped("surah", "ayah"):
        for a, b in overlapping_pairs(spans):
            yield Failure(key=f"{surah}:{ayah}", detail=f"{a['rule']} overlaps {b['rule']}")
```

`scope` is a collection name, a `prefix.*` glob, `kind:translation`, or `*`.
`severity: blocking` stops promotion; `advisory` reports and does not.

## Layout

```
nimaz-data/
  vault/            corpus.db + corpus.sha256   (gitignored — cold storage)
  data/
    console.yaml    schema-wide policy: user tables, splits, natural keys, protected
    schema.sql      generated by `nz init`; a source, not a migration
    genesis.json    written once; + keys/*.txt.gz
    collections/    one directory per collection
    changes/        pending; applied/ is folded history
    rules/          rule plugins
  nimaz_data/       the package: core/ changes/ rules/ build/ cli/ ui/ agent/
  tests/
  .build/  out/     gitignored
```

## Nothing binary in git

`vault/`, `.build/` and `out/` are gitignored. SQLite files do not
delta-compress — page contents shift on every write, so each commit stores a
fresh copy of the whole file. NDJSON delta-compresses beautifully: a change
touching twelve rows costs kilobytes.

This repo currently tracks two ~147 MB databases through Git LFS
(`app/src/main/assets/database/nimaz_prepopulated.db` and
`nimaz-pro-data/output/nimaz_prepopulated.db`, with different hashes). Retiring
them is the point of this tool, and it is safe to do only *after* the §11 round
trip passes on the real corpus.

## Deviations from the design document

Recorded here rather than silently, because a design that quietly diverges from
its implementation is worse than one that admits where it does not.

1. **§3 puts `compile.py` and `determinism.py` in a top-level `build/`** *and*
   `pipeline.py` in `nimaz_data/build/`. Everything lives in the package here;
   two build directories would be two places to look.
2. **§3 says sources are NFC-normalised.** Export preserves the vault bytes
   verbatim instead, and `text.nfc` reports non-NFC values as advisory.
   Normalising vocalised Arabic can reorder combining marks, and invariant #1
   says the corpus is never rewritten — not even to make a check pass. Importers
   normalise on the way in, where it is a choice rather than a silent edit.
3. **§6 stage 6 compares protected fields against the vault.** Day to day the
   guard runs against the genesis chain and never opens the vault (§13, §15), so
   protected fields are enforced per change at stage 3 — where the before-image
   exists — and the vault comparison runs under `--against-vault`.
4. **§8 says rules run in parallel over a connection pool.** They run
   sequentially against one read-only connection. The full run is milliseconds on
   this corpus, and a failure is attributable to a rule rather than to a worker.
   `Ctx` is the only thing a rule touches, so parallelising changes one function.
5. **§10 L1–L4 are not built.** L0 tokens are real and the CLI uses the same four
   state colours. Writing screens before the tiers beneath them is how one-off
   components get into a design system.
6. **§16 `nz import` is not built.** `kind:`, structured provenance and the
   `provenance.complete` rule are, so the contract an adapter has to satisfy
   already exists.
7. **§4's `tajweed.spans` does not exist yet.** Tajweed currently lives in
   `ayahs.text_tajweed` as pre-parsed JSON. Splitting it into a spans collection
   is a schema change, and it is exactly the kind of change this tool is for —
   `data/rules/_shared.py` already carries `overlapping_pairs` for it.
