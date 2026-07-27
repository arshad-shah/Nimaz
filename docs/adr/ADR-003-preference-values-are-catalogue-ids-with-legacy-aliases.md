# ADR-003 — Preference values are catalogue ids, with a legacy alias map

**Status:** Accepted · **Date:** 2026-07-27

## Context

Edition selections are persisted, and the values predate the registry:

- `PreferencesDataStore.quranMushafScript` stored the `MushafScript` **enum name** — `MADANI` or
  `INDOPAK_16` — because the enum was the source of truth.
- The translator preference stored `sahih_international`, already an id-shaped string.
- The reciter preference stored ids the audio layer accepted under two spellings each for two
  reciters (`alafasy`/`mishary`, `muaiqly`/`maher`), because `RECITER_CDN_MAP` and the picker had
  drifted apart.

The registry standardises on lowercase snake_case ids (`madani`, `indopak16`). Anyone who selected
the 16-line view before this change has `INDOPAK_16` on disk. If resolution only matched ids, their
reader would silently revert to the 604-page Madani edition on update — a change they never asked
for, with no error to explain it.

## Decision

Catalogue entries carry `legacyKeys: Set<String>`. Resolution order is:

1. exact `id` match,
2. any entry whose `legacyKeys` contains the stored value,
3. the axis default.

**The stored value is never rewritten.** No migration job, no write-on-read. `MADANI` stays
`MADANI` on disk and resolves to the `madani` edition every time.

## Consequences

**Zero-risk upgrade.** There is no migration to get wrong, no partial-write window, and nothing to
undo if the registry is reverted — an old build reading a new install's `madani` value falls back
to its own default, which *is* Madani.

**A small permanent alias table.** That is the right place for the knowledge: "this edition used to
be called X" is a fact about the edition, and it sits on the edition.

**A constraint.** A legacy key must never collide with a real id on the same axis, since ids win
and the shadowed edition would become unreachable. `QuranEditionsTest` asserts this, along with id
uniqueness per axis.

**One deliberate behaviour change.** The *default* value `PreferencesDataStore` returns when the
preference is absent is now `madani` rather than `MADANI`. Both resolve to the same edition, so
nothing downstream can tell the difference; `SettingsRepositoryTest` covers both the new default
and the pre-registry values round-tripping.
