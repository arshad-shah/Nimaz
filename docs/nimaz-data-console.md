# Nimaz Data Console — system design

Tool name: `nz`. Python package: `nimaz_data`. One core, three front doors (TUI, CLI, MCP).

> **Implementation:** lives in [`nimaz-data/`](../nimaz-data/). See
> [`nimaz-data/README.md`](../nimaz-data/README.md) for what is built, how to run
> the §11 bootstrap, and the list of places the implementation deviates from this
> document. §18 below summarises the status.

---

## 1. Five invariants

Everything below exists to hold these. If a design choice breaks one, the choice loses.

1. **The corpus is never written to.** The original DB lives in a vault, opened read-only and immutable, hash-checked on every session start. No code path reaches it with a write handle.
2. **The database is a build output, not a source.** It is compiled from text sources. It is never hand-edited, never seeded at runtime, never migrated in place.
3. **Every write goes through one funnel.** Console edits, hand-written SQL and agent proposals all produce the same artifact: a change directory on disk. The pipeline cannot tell them apart, so it treats them identically.
4. **Nothing is promoted before it passes.** A candidate DB is built in scratch space, validated in full, checked against the vault, and only then atomically swapped into place.
5. **A version is derived, never typed.** Content hash decides identity. Humans declare *intent* (patch/minor/major); the system computes the number.

---

## 2. The three databases

Confusing these is how corpora die, so they never share a directory, a handle, or a naming convention.

| | path | mode | lifetime |
|---|---|---|---|
| **Vault** | `vault/corpus.db` | `file:…?mode=ro&immutable=1`, `chmod 444` | permanent, never mutated |
| **Candidate** | `.build/candidate-<runid>.db` | read/write | destroyed after every run |
| **Distribution** | `out/nimaz-<hash>.db` + `out/current` | read-only once written | last N retained |

Session start runs `nz doctor`: verifies `vault/corpus.db` matches `vault/corpus.sha256`, refuses to continue on mismatch, and prints who last touched the file. The vault is also the reference for the corpus guard in §6.

Promotion is `fsync` then `os.replace()` — atomic on POSIX. `out/previous` always points at the last-known-good artifact, so rollback is a pointer move, not a rebuild.

---

## 3. Repository layout

```
nimaz-data/
  vault/
    corpus.db                     read-only original, never touched
    corpus.sha256
  data/
    collections/
      quran.uthmani/
        collection.yaml           schema, key, protected fields, floors
        records.ndjson            canonical, one record per line, sorted
      tajweed.spans/
      tr.en.saheeh/
      mushaf.indopak16/
      dua.collection/
    changes/
      20260728T1412_tajweed-v3-surah2/
        change.yaml
        up.sql
        down.sql
      applied/                    folded changes, kept for audit
    rules/
      span_bounds.py
      span_overlap.py
      page_lines.py
      _shared.py
  build/
    compile.py                    sources + changes -> candidate
    determinism.py
  out/
    nimaz-c04b8e1a.db
    build.json
    current -> nimaz-c04b8e1a.db
    previous -> nimaz-a91f3c7d.db
  nimaz_data/                     the Python package
    core/    db.py hash.py manifest.py diff.py vault.py
    changes/ model.py writer.py apply.py fold.py
    rules/   registry.py runner.py report.py
    build/   pipeline.py promote.py
    ui/      tokens.tcss primitives/ elements/ patterns/ screens/
    cli/     main.py (Typer)
    agent/   server.py (MCP)
```

Sources are NDJSON because the diff has to be reviewable in a pull request. One record per line, keys sorted, canonical Unicode normalisation (NFC), stable ordering by primary key. A source file rewritten by the tool is byte-identical to one rewritten by hand.

---

## 4. Collections and per-collection tags

A collection is the unit of versioning, validation, shipping and rollback.

`collection.yaml`:

```yaml
name: tajweed.spans
key: [surah, ayah, seq]
depends_on:
  quran.uthmani: ">=2026.07.3"
protected: []                 # fields that need a second signal to change
floors:
  rows_min: 41000             # build fails below this, no exceptions
schema:
  surah: int
  ayah: int
  seq: int
  rule: enum[ghunnah,idgham,ikhfa,iqlab,qalqalah,madd_tabii,
             madd_munfasil,madd_muttasil,madd_lazim,lam_shamsi,silent]
  start: int
  end: int
```

`quran.uthmani` declares `protected: [text_uthmani]` — the actual scripture. Touching a protected field requires an explicit declaration in the change plus a `--confirm-protected` signal. That is the only place in the system with a second lock.

**Version lines are independent.** `tajweed.spans@0.4.1` and `quran.uthmani@2026.07.3` move at their own pace. The artifact carries a `_manifest` table listing the exact version and content hash of every collection inside it — so "which tajweed data is in app build 812" is a lookup, not an investigation.

Version numbers are computed: content hash changed + `bump: minor` declared in the change → next minor. Nobody types a version into two files, which is exactly the duplicate-content-version trap.

---

## 5. Changes — the single write funnel

The Hasura model, adapted. The console never mutates data. When you edit a span in the inspector, the tool writes a change directory and shows it to you in the Changes pane. Hand-written changes drop into the same directory and are picked up identically.

```
data/changes/20260728T1412_tajweed-v3-surah2/
  change.yaml
  up.sql
  down.sql
```

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
requires:
  quran.uthmani: ">=2026.07.3"
rationale: |
  v3 parser resolves the ikhfa/madd_lazim overlap reported in #287.
```

`expect` is the important field. You declare the blast radius before the build runs, and the build fails if reality disagrees — a change that says `+43` and delivers `-4000` never reaches validation, let alone promotion. Silent mass deletion becomes structurally impossible rather than merely unlikely.

Ordering is by id (timestamp prefix). Builds are **full rebuilds**: sources plus every unfolded change, applied in order, from zero. There is no "applied" state that can drift from reality, because nothing persists between builds.

When the change list grows, `nz change fold` replays them into the source NDJSON and moves the directories to `changes/applied/` — migration squashing, with the audit trail kept.

---

## 6. Build and gate

`nz build` — six stages, each a hard gate:

```
1  verify vault      corpus.db hash matches, opened immutable
2  load sources      NDJSON -> typed records, schema + NFC checked
3  apply changes     ordered; expect{} enforced per change
4  compile           candidate DB written to .build/, deterministic
5  validate          full rule run against the candidate
6  guard             candidate vs vault diff, floors, protected fields
```

**Determinism.** Fixed page size, explicit rowids, insert order fixed by primary key, `PRAGMA journal_mode=DELETE`, `VACUUM`, then hash. Same inputs produce a byte-identical file, so the artifact hash *is* the build identity — reproducible on your machine and in CI.

**Corpus guard (stage 6)** is separate from the rules, and answers one question: *did we lose anything?*

- every collection at or above its `rows_min` floor
- no protected field differs from the vault unless declared and confirmed
- per-collection row delta within the declared `expect`
- no collection dropped out of the manifest
- key-set diff: any key present in the vault and absent in the candidate must be listed in the change

Failure at any stage leaves `out/current` untouched and the candidate on disk for inspection. Nothing partial is ever visible to the app build.

---

## 7. Output

```
nz build            -> candidate + full report
nz promote          -> validates, then atomic swap into out/
nz rollback         -> repoint current at previous
```

The build produces one file and one description of it:

```json
{
  "artifact": "sha256:c04b8e1a…",
  "built": "2026-07-28T14:31:07Z",
  "collections": {
    "quran.uthmani":    { "version": "2026.07.3", "hash": "4e02b19…", "rows": 6236 },
    "tajweed.spans":    { "version": "0.4.1",     "hash": "c04b8e1…", "rows": 41904 },
    "mushaf.indopak16": { "version": "1.0.0",     "hash": "77c1de4…", "rows": 548 }
  }
}
```

`build.json` is not a network contract — it is the receipt. It says what is inside the artifact, so "which tajweed version is in app build 812" is a lookup instead of an investigation, and so the guard has something to compare the next build against.

**Handoff to the app.** The Android build takes `out/current` as a prebuilt database and bundles it. It pins the artifact hash, so a build either has the data it expects or fails. That is the whole integration: one file, one hash, no parsing, no seeding, no assets directory.

There is one lineage — sources → build → artifact — and nothing downstream ever holds a second copy to reconcile.

---

## 8. Rules

A rule is a small declarative plugin, discovered by the registry:

```python
@rule(id="span.overlap", scope="tajweed.spans", severity="blocking")
def no_overlap(ctx: Ctx) -> Iterable[Failure]:
    for key, spans in ctx.grouped("surah", "ayah"):
        for a, b in overlapping_pairs(spans):
            yield Failure(key=key, detail=f"{a.rule}[{a.start},{a.end}] "
                                          f"overlaps {b.rule}[{b.start},{b.end}]",
                          fix=None)
```

- `severity: blocking` stops promotion; `advisory` reports and does not.
- `fix=` returns a callable that emits a **change directory** — an autofix is just another change, reviewable like any other. It never writes to a database.
- Rules run against the candidate, in parallel, with a shared read-only connection pool.

---

## 9. Agent surface

Same core, no privileged path.

| tool | effect |
|---|---|
| `nimaz.describe` | collections, versions, hashes, build receipt |
| `nimaz.query` | read-only SQL against `out/current`, row-capped |
| `nimaz.get` | record by key |
| `nimaz.validate` | full or scoped rule run, structured failures |
| `nimaz.diff` | tag ↔ tag, tag ↔ candidate |
| `nimaz.propose` | writes a change directory with `origin: agent` |

`propose` is the only mutating tool and it does not mutate — it produces a change that a human reviews in the Changes pane and a build gate enforces. Every CLI command has a `--json` form with the same schema the MCP server returns.

---

## 10. UI primitives layer

Five tiers, strictly one-directional: a tier may only use tiers below it. Screens contain no raw colour, no raw spacing, no one-off components.

**L0 — tokens.** Semantic, never literal. `--surface-0/1/2`, `--edge`, `--edge-soft`, `--ink`, `--ink-2`, `--ink-3`, state colours `--ok --pend --block --unknown`, space scale `--s1..--s8` on a 4px base, type scale `--fz-micro..--fz-display`, one radius, one border width, two motion durations. A component naming a hex value is a bug.

**L1 — primitives.** No product meaning: `Stack`, `Row`, `Box`, `Text` (role variants: label, body, lead, title, data, arabic), `Rule`, `Icon`, `Spacer`. All spacing comes from the scale via gap, never ad-hoc margins.

**L2 — elements.** `Panel` (head/body/foot), `Button`, `Chip`, `StateDot`, `KeyHint`, `Table`, `ListRow`, `Callout`, `CodeBlock`, `Stat`, `Field`.

**L3 — patterns.** Product-aware compositions: `Inspector`, `PipelineTrack`, `DriftRibbon`, `ChangeRow`, `RuleRow`, `ManifestGrid`, `SplitView`, `DiffView`.

**L4 — screens.** Collections, Browse, Changes, Build, Rules, Agent.

**Textual mapping.** L0 becomes `tokens.tcss` variables (`$surface-1`, `$ok`, …); L1–L3 become `Widget` subclasses each with a sibling `.tcss` that references only tokens. The browser prototype and the terminal app share the same names, so a change to the token file moves both. `textual-serve` renders the identical widget tree in a browser for anyone who does not want a terminal.

**State colour has one meaning everywhere:** teal = matches the manifest, gold = changed and not yet validated, mulberry = blocking, violet = never verified. It is used for status only — never decoratively.

---

## 11. Bootstrap from today's `nimaz.db`

1. Copy the current DB into `vault/`, `chmod 444`, record its hash.
2. `nz source export` — decompose the vault into `data/collections/*/records.ndjson`, canonically formatted.
3. `nz build` from those sources with zero changes, then `nz verify --against-vault` — the candidate must be row-identical and hash-identical per collection to the vault. This proves the export is lossless before anything is trusted.
4. Commit sources, cut initial per-collection tags from the vault content hashes.
5. From here the vault is a reference, not a dependency. Every future artifact is compiled.

Step 3 is the one that cannot be skipped. Until the round trip proves lossless, the DB is still the source of truth and no change should be authored.

---

## 12. Nothing binary in git

SQLite files do not delta-compress. Page contents shift on every write, so each commit stores a fresh copy of the whole file — a large DB touched forty times becomes gigabytes of history and LFS bandwidth you are billed for. NDJSON sources delta beautifully: a change touching twelve rows costs kilobytes.

**Tracked:** sources, changes, rules, `collection.yaml`, the `build.json` chain, `genesis.json`. Text, on the order of tens of MB total, no LFS.

**Never tracked:** `vault/`, `.build/`, `out/`. Gitignored, plus a CI job that fails on any tracked `*.db` or any file over a size threshold — so the repo cannot quietly become expensive.

Where each artifact actually lives:

| | home | why |
|---|---|---|
| vault | cold object storage, immutable + checksummed, plus one offline copy | witness only after bootstrap; never deleted |
| candidate | CI scratch / local `.build/` | disposable by design |
| out | attached to the git tag, keyed by artifact hash | the app build fetches it; nobody commits it |

The Android repo holds no DB either. A Gradle task resolves the pinned artifact hash, fetches it from the tag, verifies the hash, and bundles it. Two repos, zero binaries.

**Why this carries no loss risk.** After the round-trip proof (§11), the sources contain every byte the corpus contains — the DB is a lossless re-encoding, not a second copy. The chain of release receipts in git is an append-only record of row counts, key digests and hashes reaching back to genesis, so any loss is attributable to the commit that caused it. The vault survives in cold storage as an independent witness that never depends on git being intact.

---

## 13. Genesis fingerprint

`data/genesis.json` is written once at bootstrap from the vault and never edited:

```json
{
  "sealed": "2026-07-21T09:14:00Z",
  "vault_sha256": "4e02b19…",
  "collections": {
    "tajweed.spans": { "rows": 41644, "hash": "2b90fa5…", "keys": "keys/tajweed.spans.txt.gz" }
  }
}
```

The key lists are sorted, gzipped, and small — tens of KB, not megabytes. The guard walks candidate → last release receipt → … → genesis, so it can prove that no key present at genesis is absent now **without opening the vault**. That is what makes the vault an archive rather than a dependency.

---

## 14. CI

Two tiers, because a fast check and a total check answer different questions.

**`pr.yml` — scoped, target 30s.** Only the collections the diff touches. Sound because collections are independently versioned and dependencies are declared. Loads those sources, applies staged changes, enforces every `expect{}`, runs rules scoped to those collections, and guards against the last release receipt. Posts the declared-vs-actual table as a PR comment. A blocking rule fails the check; there is no override.

**`main.yml` — full, minutes.** Full rebuild from zero, all 41 rules, guard against the genesis chain, then **build twice and compare hashes**. Reproducibility is itself an integrity check: a non-deterministic build means the artifact hash is not the identity you believe it is.

**Cache.** Keyed by the content hash of each collection's sources plus its changes. A hit means byte-identical input, so reuse is safe, and deterministic assembly plus the final artifact hash verifies it. Untouched collections never recompile — that is where the speed comes from, and it weakens nothing.

**`release.yml`** — triggered by a collection tag. Full build, then append `build.json` to the chain in git and attach the artifact to the tag. Nothing is uploaded anywhere; the Android build pulls the artifact from the tag by hash.

**`witness.yml`** — nightly. Rebuilds from sources at the last release commit and compares to the published artifact hash, and re-verifies the cold-storage vault checksum. Catches any drift between what is published and what the sources produce, and confirms the witness is still intact.

**Round-trip runs continuously, not once.** §11 step 3 proves the NDJSON sources are a lossless re-encoding of the corpus, and §12 leans on that proof to justify keeping no binary in git. A proof asserted once at bootstrap and assumed forever is exactly the kind of thing that quietly stops being true. `main.yml` therefore exports the candidate back to NDJSON, rebuilds from that export with zero changes, and asserts per-collection hash equality — the same check that gates a fold, run on every merge. If sources ever cease to be a complete copy, that job is what says so.

**Dependency contract.** `pr.yml` also runs `nz deps check`: every `depends_on` constraint in every `collection.yaml` must be satisfiable by the versions in this build. Cheap, and it catches the one class of breakage validation cannot see — an artifact that is internally valid but internally inconsistent.

**Reporting.** `nz validate --annotate github` puts a `span.overlap` failure inline on the offending line of `records.ndjson` in the diff rather than in a log nobody opens.

---

## 15. Decisions closed

- **Fold at tag.** A fold is only committed if the artifact hash is identical before and after — a fold that changes the output is a bug, and CI proves it on every run. Zero risk, and the change list never outlives one tag cycle.
- **Vault: cold storage plus an offline copy, never git, never deleted.** Day-to-day the guard runs against the genesis chain, so the vault is never in the hot path.
- **Delivery is out of scope.** The app bundles the artifact. Per-collection packs, a manifest server and on-demand download are a later feature, gated on a size number, not on architecture — see §17.

---

## 16. Importers — where the churn actually lives

Everything above protects data that is already in the repo. The churn is upstream of that, and it is the reason a data change is currently a code change.

Adding one translation today touches, at minimum: a bespoke download script, `generate_database.py`, a DTO, a seeder, a Room entity, a schema version, a migration, migration tests, a copy in `assets/`, and a hand-typed `contentVersion`. Ten places, none of which are the data. That is why the last data PR was 95 files.

The target: **adding a collection is a file, not a change to the tool.**

```yaml
# data/collections/tr.bn/collection.yaml
name: tr.bn
key: [surah, ayah]
kind: translation                    # picks the schema and the rule set
import:
  adapter: alquran-cloud             # one of a small set
  edition: bn.bengali
  url: https://api.alquran.cloud/v1/quran/bn.bengali
provenance:
  translator: Muhiuddin Khan
  license: see LICENSES_TRANSLATIONS.md
  retrieved: 2026-07-27
floors:
  rows_min: 6236
```

`nz import tr.bn` fetches, maps through the adapter, normalises to NFC, sorts by key, writes `records.ndjson`, and stages a change with a declared `expect{}`. The sixteenth translation is that file and nothing else — no new script, no new parser, no app-side code at all.

An adapter is small and there are few of them: `alquran-cloud`, `mushaf-layout`, `local-json`. Adding a genuinely new upstream shape means writing one adapter, which is real work exactly once, rather than a new 300-line script each time. Re-running an import is idempotent: same upstream, same bytes, no change staged, so a re-import is a safe way to check whether upstream moved under you.

`kind:` is what removes the per-collection code on the other end too. A `translation` compiles to the same table shape and inherits the same rules whatever the language, which is why the build needs no knowledge of Bengali specifically.

**Provenance is structured, not prose.** `LICENSES_TRANSLATIONS.md` becomes fields, and a rule asserts every collection has a translator, a license and a retrieval date. That rule would have caught the malformed source string already in the tree — `alquran.cloud edition 'en.pickthall' ()`, with an empty parenthetical where a field was dropped.

---

## 17. Not in scope

This is a local tool for one maintainer and one agent. Deliberately absent:

- per-collection packs, a manifest server, on-demand download
- signing, CDN, object storage for anything except the vault archive
- any network dependency in the build — after `nz import`, everything works offline

The app bundles the artifact and pins its hash. Dynamic delivery becomes worth building when the bundled artifact makes the APK bigger than is acceptable to ship — a size number, measurable after the first real build. Per-collection tags are what make that possible later; they cost nothing now and they are how versioning and validation work regardless.

---

## 18. Implementation status

The tool lives in [`nimaz-data/`](../nimaz-data/). Sections 2, 4, 5, 6, 7, 8, 9,
11 and 13 are implemented and under test; the round-trip proof, determinism and
every gate in the change funnel are asserted by the suite, which builds its own
miniature corpus with the real schema shape.

Not yet built: **§10 L1–L4** (L0 tokens are real; the CLI is the working front
door) and **§16 `nz import`** (`kind:`, structured provenance and the
`provenance.complete` rule exist, so the contract an adapter must satisfy is
already defined).

`nimaz-data/README.md` carries the full status table and an explicit list of the
seven places the implementation deviates from this document, with reasons. The
most consequential: **export preserves the vault bytes verbatim rather than
NFC-normalising them** (§3), because normalising vocalised Arabic can reorder
combining marks and invariant #1 says the corpus is never rewritten — not even to
make a check pass. A `text.nfc` advisory rule reports non-NFC values instead.

### What this replaces

Today `nimaz-pro-data/scripts/` holds ~24 bespoke scripts, the shipped database
is a 147 MB Git-LFS blob tracked in two places with two different hashes, and
`generate_database.py` documents the drift in its own comments:

> the shipped prepackaged DB is stamped at `user_version = 12` and is **NOT**
> regenerated for this feature — on device, `MIGRATION_17_18` creates the empty
> column/table and `QuranIndopakSeeder` fills them from the bundled JSON assets

That is invariant #2 — *the database is a build output, not a source* — failing
in the tree right now: a shipped artifact that cannot be reproduced from its
generator, patched at runtime by a seeder reading a second copy of the data.
Retiring it is the point of the tool, and it is safe to do only after the §11
round trip passes on the real corpus.
