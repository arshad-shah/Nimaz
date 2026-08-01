# Data retirement ledger

**For agents:** when told *"version X is in prod"*, do exactly this:

1. Read `retirement.yaml` in this directory.
2. For every entry whose `retire_when.min_prod_version_code` is **≤ X**, and whose
   `blocked_by` entries are all already `status: retired`, perform its `steps` and run its
   `verify` commands.
3. Set that entry's `status` to `retired` and record `retired_in_version_code`.
4. Do **not** touch entries whose gate is above X. Report them as pending with the version
   that would unblock them.

Nothing here is retired on judgement. Each entry names the exact prod version that makes it
safe and the reason that version is the threshold.

One exception, marked in the entry itself: `git-history-purge` has passed its gate but is
**held for explicit human sign-off**. It is the only entry whose damage cannot be checked for
beforehand — see "The one that stays pending" below.

---

## Why retirement is gated on the *deployed* version

Every item in this ledger is something that exists only to carry data to users who do not
have it yet. Deleting one early means those users never get that data — and unlike a code
bug, it is silent: the app runs, the screen is just empty.

The gate is always the same shape: *once every user who could still be running an older
build has received the content by some other route, the carrier can go.* That is a fact about
what is installed, not about what is merged, which is why the trigger is the prod version
code rather than a date or a milestone.

`versionCode` is the authority (`app/build.gradle.kts`), not `versionName`. Google Play's
minimum-supported-version and staged-rollout state are what make a version "in prod" — an
entry is only safe when the *oldest* build still receiving users has passed the gate.

---

## Where this stands at 385

The seeders are gone. All six of them, plus the tracked LFS asset and the stale upstream
guard, retired at versionCode 385; only `git-history-purge` remains.

For the record of what that means: `assets/database/nimaz_prepopulated.db` used to be a
tracked 140 MB blob stamped `user_version = 12`, and eight migrations plus six content seeders
turned it into what a user actually saw. Today the app fetches a hash-pinned artifact at
`schemaVersion 23` from `arshad-shah/nimaz-data`, and that artifact carries the whole corpus —
15 translations, 4 Mushaf editions, the 379 hadith repairs, and the Help, Dua and Qaida
content. The seeders had become no-ops running over data that was already there.

`app/src/main/assets/` went from about 31 MB to an empty `adhan/.gitkeep` as a result — the
app ships no bundled content assets at all any more.

## The subtlety that set most of the gates

Two seeders were **lazy**: `MushafLayoutSeeder` ran per script on first use, and
`QuranTranslationSeeder` per translation on first read. So an existing install could sit at
the current schema holding only Saheeh International and the Madani layout, with the other 14
translations and 3 editions still sitting unparsed in the APK.

That meant an upgrading device was **not** at "full content" — it was at an unknown subset,
and which subset depended on what that user happened to open. So the content patch could not
treat the current version as its baseline until the seeders had had one full release to
converge everyone. That is why those entries were gated a release later than looked necessary,
and at 385 they are four releases clear of it.

---

## Entries

The machine-readable source of truth is [`retirement.yaml`](retirement.yaml). The table below
is generated from it for humans and must be kept in step.

| id | what went | gate (`versionCode`) | status |
|---|---|---|---|
| `lfs-prepopulated-asset` | `assets/database/nimaz_prepopulated.db` + the `*.db` LFS rule | 379 | **retired in 385** (steps were already performed in `0b37228`; only the status was outstanding) |
| `hadith-backfill-seeder` | `HadithBackfillSeeder`, `assets/hadith/hadith_fills.json` | 380 | **retired in 385** |
| `quran-translation-seeder` | `QuranTranslationSeeder`, `assets/quran/translations/` (18 MB) | 381 | **retired in 385** |
| `mushaf-layout-seeder` | `MushafLayoutSeeder`, `assets/quran/mushaf/` (12 MB) | 381 | **retired in 385** |
| `help-dua-qaida-seeders` | `HelpContentSeeder`, `DuaContentSeeder`, `QaidaContentSeeder` and their assets | 381 | **retired in 385** |
| `upstream-generator-scripts` | the drift guards pointing at `nimaz-pro-data/scripts/` | 380 | **retired in 385** (`tajweed_data_checks.yml` deleted; the assertion runs in the data repo) |
| `git-history-purge` | `nimaz-pro-data/**` and every `.db` blob, from all git history | 380 | **pending — gate passed, held for sign-off** |

Each retired entry carries a `retirement_notes` field in the YAML recording what was checked
before the deletion and what was deliberately kept. Two worth surfacing here:

- **`seededTranslationId` was kept**, renamed to `translationId`. It also normalised an
  unknown translator id to `DEFAULT` via `fromId()` — behaviour that has nothing to do with
  seeding and would have been silently dropped by removing the whole function.
- **`seededFlow` was kept** in the Help and Dua repositories as `deferredFlow`. Besides
  seeding, it deferred building the DAO flow until collection time, which callers rely on.

### Not retiring

- **`createFromAsset` itself.** Still how a fresh install gets its database; only the *source*
  of that file changed, from a tracked LFS blob to a fetched, hash-pinned artifact.
- **The migrations.** They carry schema, which every install still needs. What they never
  carried is content, and that does not change.
- **`ContentPatchSeeder`.** This is the mechanism the others retired *into*, not a carrier
  with an expiry.

---

## The one that stays pending

`git-history-purge` has passed its gate (380) and its blocker is retired, so by the rule at the
top of this file it is actionable. It is deliberately not being performed.

Every other entry in this ledger can be checked before the fact — you can query the artifact,
diff the row counts, and run a test that names the table that would have shipped empty. This
one cannot. It rewrites every commit SHA, invalidates all remote branches, and force-pushes;
the failure mode is not an empty table but every commit link in every review, issue and release
note pointing at nothing. And per its own `caveats`, it does not even reclaim the LFS storage
quota without a separate GitHub support request.

So it needs a human to say so explicitly, and to pick a moment when nobody has work in flight.
**Ask before running it.** Do not read this section as a reason to drop the entry — the work is
still worth doing, on purpose, with a backup mirror and warning given.

---

## Verifying a retirement was safe

After performing an entry's `steps`, its `verify` commands must pass:

```bash
./gradlew :app:compileDebugKotlin      # KSP: Hilt and Room wiring still resolves
./gradlew :app:testDebugUnitTest       # includes DeviceStateCorpusTest
```

`DeviceStateCorpusTest` is the one that matters, and what it asserts inverted at 385. It used
to manufacture device state by running the real migrations *and* the real seeders, so that the
corpus could be sealed from whatever they produced. With the seeders gone it opens the artifact
through Room, runs the migration chain, and asserts that **nothing needs to fill anything**:
every content table non-empty, no hadith with blank Arabic, each line-accurate edition's stored
layout agreeing with `MushafScript` on lines per page, the 15 translator ids matching the
catalogue exactly, no user table present, schema at `NIMAZ_DATABASE_VERSION`.

That makes it the standing guard on the artifact rather than on the seeders — a collection
dropped upstream, a `data.lock.json` rolled back past a table, or an importer that emitted zero
rows all land as a named empty table instead of an empty screen.

**It needs the artifact, so it needs a credential.** `fetchNimazData` resolves
`NIMAZ_DATA_TOKEN`, `nimazDataToken`, or `gh auth token` against the private data repo; without
one the task fails and this test cannot run locally. `compileDebugKotlin` does not depend on the
fetch and runs fine without a token. See [`CONTENT_REPO_AUTH.md`](CONTENT_REPO_AUTH.md).
