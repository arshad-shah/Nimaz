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

## The current situation, in one paragraph

`assets/database/nimaz_prepopulated.db` is stamped `user_version = 12`; the app is at 20.
Eight migrations and six content seeders turn that asset into what a user actually sees. The
`nz` data console now owns the whole corpus at v20 — 15 translations, 4 Mushaf editions, the
379 hadith repairs, and the Help, Dua and Qaida content — and ships it as one hash-pinned
artifact fetched from `arshad-shah/nimaz-data`. The seeders are therefore redundant *for
anyone who installs from the new artifact*, and still load-bearing for everyone else. This
ledger tracks the gap closing.

## The subtlety that sets most of the gates

Two seeders are **lazy**: `MushafLayoutSeeder` runs per script on first use, and
`QuranTranslationSeeder` per translation on first read. So an existing install can sit at
schema v20 holding only Saheeh International and the Madani layout, with the other 14
translations and 3 editions still sitting unparsed in the APK.

This means an upgrading device is **not** at "full v20 content" — it is at an unknown subset,
and which subset depends on what that user happened to open. So the content patch cannot
treat v20 as its baseline until the seeders have had one full release to converge everyone.
That is the reason these entries are gated a release later than looks necessary.

---

## Entries

The machine-readable source of truth is [`retirement.yaml`](retirement.yaml). The table below
is generated from it for humans and must be kept in step.

| id | what goes | gate (`versionCode`) | why then |
|---|---|---|---|
| `lfs-prepopulated-asset` | `assets/database/nimaz_prepopulated.db` + the `*.db` LFS rule | 379 | The artifact is fetched and hash-verified at build time from the first release that ships `data.lock.json`. Nothing reads the asset once the fetch task is wired, so it goes with the release that introduces it. |
| `hadith-backfill-seeder` | `HadithBackfillSeeder`, `assets/hadith/hadith_fills.json` | 380 | The 379 repairs are folded into the corpus sources, so the artifact ships them already applied. Existing installs get them from the `ContentPatchSeeder` instead. One release of overlap so a device upgrading from 378 has run one of the two paths. |
| `quran-translation-seeder` | `QuranTranslationSeeder`, `assets/quran/translations/` (18 MB) | 381 | Lazy seeder: needs the 380 release to converge users who never opened a non-default translation. From 381 the artifact carries all 15 and the patch covers upgraders. |
| `mushaf-layout-seeder` | `MushafLayoutSeeder`, `assets/quran/mushaf/` (12 MB) | 381 | Same lazy-seeding argument as translations; the 3 non-Madani editions are only populated once a user selects them. |
| `help-dua-qaida-seeders` | `HelpContentSeeder`, `DuaContentSeeder`, `QaidaContentSeeder` and their assets | 381 | Eager seeders, so convergence is complete one release after the artifact carries their content. Grouped because they share the content-version-gate pattern and retire identically. |
| `upstream-generator-scripts` | the `--check` catalogue drift guards pointing at `nimaz-pro-data/scripts/` | 380 | Those scripts moved to `arshad-shah/nimaz-data` under `upstream/scripts/`; the Kotlin-side catalogue check must point at the new repo or be replaced by `nz import --check`. |
| `git-history-purge` | `nimaz-pro-data/**` and every `.db` blob, from all git history | 380 | Destructive and irreversible: rewrites every commit SHA. Only safe once the data repo is proven — the artifact fetch green in CI and one release shipped from it. See §8 of the design spec. |

### Not retiring

- **`createFromAsset` itself.** Still how a fresh install gets its database; only the *source*
  of that file changes, from a tracked LFS blob to a fetched, hash-pinned artifact.
- **The migrations.** They carry schema, which every install still needs. What they never
  carried is content, and that does not change.
- **`ContentPatchSeeder`.** This is the mechanism the others retire *into*, not a carrier
  with an expiry.

---

## Verifying a retirement was safe

After performing an entry's `steps`, its `verify` commands must pass. Two run for every entry:

```bash
./gradlew :app:compileDebugKotlin      # KSP: Hilt and Room wiring still resolves
./gradlew :app:testDebugUnitTest       # includes DeviceStateCorpusTest
```

`DeviceStateCorpusTest` is the one that matters. It rebuilds real device state from the asset,
the migrations and whichever seeders still exist, and asserts every content table is non-empty.
Delete a seeder whose content is not yet in the artifact and that test fails with the table
that would have shipped empty — which is the whole reason it exists.
