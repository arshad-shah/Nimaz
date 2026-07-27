# ADR-002 — Catalogue in `domain`, bindings in their own layer, paired by test

**Status:** Accepted · **Date:** 2026-07-27

## Context

The goal is "adding an edition means adding one row". The obvious way to reach it is one object
per edition holding everything about it: display name, language, page count, the bundled asset
path, the audio CDN id, and the Compose `FontFamily` to draw it with.

That object cannot live in `domain`. `CLAUDE.md` rule 1 says domain never imports `data`, and a
`FontFamily` is a Compose type — presentation. Putting the whole thing in `presentation` is what
the codebase already did (the translation picker's hardcoded pair list, `SelectReciterScreen`'s
`popularReciters`) and is exactly the anti-pattern this work removes.

## Decision

Split the registry by layer along what each piece actually *is*, and pin the halves together with
a test.

| Layer | Holds | File |
|---|---|---|
| `domain` | pure metadata: `id`, display name, language tag, direction, page count, capability flags, `fontId` as a **String** | `domain/model/quran/catalogue/QuranEditions.kt` |
| `data` | asset paths, content versions, audio CDN ids + bitrates | `data/local/quran/QuranContentAssets.kt` |
| `presentation` | `fontId` → `FontFamily` | `presentation/theme/Type.kt` (`QuranArabicFont`) |

`QuranEditionRegistryTest` asserts the id sets agree — every line-accurate layout has an asset
binding, every reciter has a CDN binding, catalogue font ids and `QuranArabicFont` ids are the same
set, and no binding names an edition the catalogue does not know.

## Consequences

**Cost.** Adding an edition touches two small files rather than one (three for a font, which also
needs its `FontFamily`). §14 of the spec documents that as the standing procedure.

**Benefit.** The layering rule holds, and — more importantly — a half-added edition fails the build
instead of shipping. Before this, the failure mode was silent: `QuranAudioManager`'s CDN map
carried five reciters the picker never offered, and Maher Al-Muaiqly's name was spelled two
different ways depending on which of the three copies rendered it. Those are precisely the bugs
the pairing test now makes impossible.

**Alternative rejected.** Codegen from a single manifest into all three layers. It would give one
literal edit site, but adds a build step and a generated-source review burden for a registry that
gains an entry a few times a year. The test-paired split costs one extra line per edition and no
build machinery.
