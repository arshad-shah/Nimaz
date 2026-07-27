# ADR-001 — One generic `mushaf_layouts` table, not a table per edition

**Status:** Accepted · **Date:** 2026-07-27 · **Supersedes:** the `mushaf_layout_indopak16` table added in `MIGRATION_17_18`

## Context

The 16-line IndoPak edition (#263) shipped as a dedicated Room table, `mushaf_layout_indopak16`,
holding ~13,970 line-segment rows. The table name is a literal in every query that reads it, so
adding a second line-accurate edition by copying the pattern would mean:

- a second entity (`MushafLayoutIndopak15Entity`) identical but for its `@Entity(tableName = …)`,
- a second copy of all six DAO methods (`getIndopak16PageAyahRanges`, `countMushafLayoutIndopak16`,
  `deleteAllMushafLayoutIndopak16`, `insertMushafLayoutIndopak16`, `getMushafLayoutByPage`,
  `replaceMushafIndopak16`) — 14 references to the literal table name across `QuranDao.kt`,
- a second seeder,
- a second migration,

and again for every edition after that. Three near-identical tables drift: a fix to one page-range
query silently leaves the others wrong.

## Decision

Replace `mushaf_layout_indopak16` with a single `mushaf_layouts` table carrying a `layout_id`
TEXT discriminator alongside the existing columns. DAO methods take `layoutId` as a parameter;
`QuranRepositoryImpl` passes the active edition's catalogue id through.

One migration, once. Every subsequent layout is data only: a JSON asset, a catalogue entry, an
asset binding, a licence block.

The index becomes `(layout_id, page, line)` — the discriminator has to lead, since every read is
scoped to one edition.

## Consequences

**Easier.** Adding a layout touches no entity, no DAO, no migration. The fidelity test can run its
invariants over *every* catalogue layout rather than the one, so a bad asset fails CI on arrival.

**Harder.** One non-trivial migration that must copy the existing rows and stay idempotent when it
runs after `createFromAsset` on a fresh install — the pattern every migration in `NimazDatabase.kt`
already follows. It must be verified with `MigrationTestHelper` for row preservation, idempotency
and old-table removal, because getting it wrong silently empties an existing user's 16-line reader.

**Neutral.** Row count grows ~14k per layout. Trivial for SQLite given the composite index; the
asset (~2.5 MB raw per layout) is the real budget line, not the table.

## Notes

`getMushafLayoutByPage` joins the layout rows to `ayahs` and selects `a.text_indopak` as a literal
column name. Room cannot parameterise a column name in `@Query`, so the generic version must either
select both candidate columns and let the mapper pick by the edition's
`AyahTextSource`, or branch on it in the repository. That is an implementation constraint of this
ADR, not a reason against it.
