# Event Cards, Celebration Announcements & Granular Routing — design

**Date:** 2026-07-24
**Branch base:** `fix/206-light-mode-card-contrast` (ahead of the source spec's `d1427b2`)
**Source:** the grounded implementation spec supplied in-session ("Granular Announcement Routing
& Celebration Event Cards"), reconciled against the live tree.

This document is the design of record. It keeps the source spec's substance and records three
reconciliation deltas plus one new requirement (horizontal carousel) that change the shape of the
work. Section numbers below mirror the source spec where useful.

---

## 0. Decisions taken in brainstorming

1. **Scope: full spec, sequenced.** All 10 steps land, but steps 1–5 (routing + `EventCard` +
   `JumuahCard` rebuild + previews) ship first so the visual layer is verifiable before any FCM
   plumbing. Each step is independently shippable and revertible.
2. **`EventCard` is a new organism**, not a rebuild of the existing `IslamicEventCard` molecule.
   `IslamicEventCard` is **left untouched** and its call sites are not migrated in this work — it
   is recorded as a future-cleanup item (§8). "New" means new *organism*; it still composes
   existing atoms — no new geometry, no hand-rolled wells or dividers.
3. **Event cards live in a horizontal carousel, not a vertical stack** (new requirement). They
   reuse the existing `NimazCarousel` organism exactly as `TodayCarousel` does. Jumu'ah becomes a
   **page inside that carousel** on Home rather than a standalone stacked item.

---

## 1. Verified component inventory (reuse targets — no hand-rolling)

All paths under `app/src/main/java/com/arshadshah/nimaz/`. Confirmed present on the current branch.

| Need | Reuse | Path / signature notes |
|---|---|---|
| Card surface | `NimazCard(tone = NimazTone.NEUTRAL, style = NimazCardStyle.ELEVATED)` | `atoms/NimazCard.kt`. **tone and style are two separate enums** (delta vs source spec). `NimazCardStyle{FILLED,ELEVATED,OUTLINED,GRADIENT}`, `NimazTone{NEUTRAL,MUTED,ACCENT,PROMINENT,SUCCESS,WARNING,ERROR,TRANSPARENT}` |
| Icon well (38.dp) | `NimazIcon(type = CONTAINED, containerShape = ROUNDED_SQUARE)` **or** `NimazIconWell.kt` | `atoms/NimazIcon.kt`. Do **not** hand-roll a `Box` + `clip` + `background` well (JumuahCard's current approach) |
| Arabic line | `ArabicText(size = ArabicTextSize.SMALL)` | `atoms/ArabicText.kt`. `ArabicTextSize{SMALL,MEDIUM,LARGE,EXTRA_LARGE,QURAN}` |
| Unwan divider | `QuranOrnamentalDivider(color, horizontalPadding, verticalPadding)` | `atoms/QuranOrnamentalDivider.kt`. Replaces the manual `1.dp` `Box` divider |
| Eid burst | `QaidaCelebrationBurst(play, gold, teal)` | `atoms/QaidaCelebrationBurst.kt`. `play=false` renders a still frame |
| Motif plate | `NimazPatternBackground(style, enabled, surface, alphaScale)` | `atoms/NimazPatternBackground.kt`. **Reads `LocalShowIslamicPatterns` itself** — wrap content in it; never read the local directly |
| Pattern styles | `NimazPatternStyle{NONE,CORNER_MEDALLION,LATTICE,STAR_FIELD,ATELIER}` | `theme/NimazPatternStyle.kt` |
| Medallion / floret | `ShamsaMedallion(number,…)` / `DiamondFloret(color,size,alpha)` | `atoms/ShamsaMedallion.kt` |
| Buttons (CTAs) | `NimazButton(variant, size, type)` | `atoms/NimazButton.kt`. `variant{FILLED,TONAL,OUTLINED,TEXT,DESTRUCTIVE}`, `size{SMALL,MEDIUM,LARGE}`, `type{STANDARD,PILL}`; `accent: Color?` escape hatch for feature-art hues |
| Carousel | `NimazCarousel(count, pageHeight, horizontalPadding, pageSpacing, showIndicators, pageContent)` | `organisms/NimazCarousel.kt`. Edge-peeking `NimazPager` + `NimazPageIndicator`; swipe-only, no auto-advance; hides dots for a single page; renders nothing for `count <= 0` |
| Carousel analog | `TodayCarousel` | `organisms/TodayCarousel.kt` — the exact pattern to copy: build a filtered list, feed `NimazCarousel` |
| Palette | `NimazPalette` (raw) / `NimazColors` (semantic) | `theme/Palette.kt`, `theme/Color.kt`. `GreenDeep 0xFF2E7D32`, `Gold500 0xFFEAB308`, `GoldDark 0xFFCA8A04`, `MatPurple 0xFF9C27B0`, `Teal700 0xFF0F766E`, `Teal500 0xFF14B8A6`, `Amber700 0xFFB45309` |
| Calendar | `IslamicEvents.events` / `IslamicEvent` (`domain/model/IslamicCalendarModels.kt`); `HijriDateCalculator` (`core/util/`) | To be re-verified at implementation of §6–7 |

**Prior art left in place:** `molecules/IslamicEventCard.kt` (icon-well + Arabic title + hijri
badge + days-until + description, driven by `HijriDateCalculator.EventType`). Overlaps the new
`EventCard` — see §8.

**Contrast rule (light/white surface) — do not break it:** accent lives in the icon well, chip
tint, border, divider. Body copy stays `onSurface`/`onSurfaceVariant`. Gold is structural, never
text. Ratios in the source spec §0.3 are for the light surface only; dark theme re-checked in
previews.

---

## 2. `EventCard` organism (source spec §3)

New: `presentation/components/organisms/EventCard.kt`. Composes the atoms above.

```kotlin
@Composable
fun EventCard(
    accent: Color,
    icon: ImageVector,
    eyebrow: String,
    arabic: String?,
    headline: String,
    body: String,
    modifier: Modifier = Modifier,
    transliteration: String? = null,
    proof: Pair<String, String>? = null,       // ref, text — omit chip entirely if null
    trailing: (@Composable () -> Unit)? = null, // Jumu'ah time column
    highlight: (@Composable () -> Unit)? = null,// countdown chip
    ornament: EventOrnament = EventOrnament.None,
    primaryAction: EventAction? = null,
    secondaryAction: EventAction? = null,
    onDismiss: (() -> Unit)? = null,
)

sealed interface EventOrnament {
    data object None : EventOrnament
    data class Pattern(val style: NimazPatternStyle) : EventOrnament
    data class Burst(val play: Boolean) : EventOrnament
    data object Divider : EventOrnament
}

data class EventAction(val label: String, val onClick: () -> Unit)
```

Anatomy = JumuahCard's skeleton (verified in `JumuahCard.kt`) extended by the proof chip, second
CTA, ornament slot, and dismiss. `trailing` and `highlight` are exactly JumuahCard's two existing
blocks (time column, countdown box), so the migration is re-parenting, not a rewrite.

**Ornament rules:** `Burst`/`Pattern` render only through `NimazPatternBackground`/the atom's own
gating (respects `LocalShowIslamicPatterns`). `Burst.play = false` when `LocalInspectionMode` or
reduced motion. No emoji/ASCII ornaments — icons via `NimazIcon`.

**Accent + ornament per occasion** (source spec §3.3 table): Jumu'ah `GreenDeep`+Divider; Eid
al-Fitr `GoldDark` (well/border `Gold500`) + Burst + Divider; Eid al-Adha `Teal700` +
Pattern(CORNER_MEDALLION) + Divider; Ramadan `MatPurple` + Pattern(LATTICE); Laylat al-Qadr
`MatPurple` + Pattern(STAR_FIELD); Arafah/Ashura `Teal700` + Divider; Mawlid/Hijri-new-year
`Amber700` + Divider; Generic `Teal700` + Divider.

**Previews (the "so I can see it" deliverable):** every occasion × light/dark, plus Jumu'ah
active/passed, patterns-off, and 200% font scale. These are the acceptance surface for the card
layer before any FCM work.

---

## 3. `EventsCarousel` organism + Home wiring (new requirement)

New: `presentation/components/organisms/EventsCarousel.kt`, shaped like `TodayCarousel`:

```kotlin
@Composable
fun EventsCarousel(
    events: List<EventCardUi>,     // presentation model built in the VM/use case
    modifier: Modifier = Modifier,
    pageHeight: Dp = /* sized to the richest variant */,
    horizontalPadding: Dp = 20.dp,
) {
    if (events.isEmpty()) return
    NimazCarousel(count = events.size, pageHeight = pageHeight, horizontalPadding = horizontalPadding) { i ->
        EventCard(/* map events[i] -> EventCard params */)
    }
}
```

- **`NimazCarousel` uses one fixed `pageHeight` for every page.** Size it to the tallest expected
  variant (Eid: Arabic + transliteration + proof chip + two CTAs). Shorter cards (Jumu'ah, Generic)
  top-align within that height. Confirm the chosen height in previews at 200% font scale.
- **Jumu'ah folds into the carousel.** On Home, the current `item { JumuahCard(...) }` is removed;
  Jumu'ah becomes one `EventCardUi` in the list when `isFriday`. The standalone `JumuahCard`
  organism still exists (rebuilt on `EventCard`) for reuse/tests, but Home routes through the
  carousel.
- **Placement (compact LazyColumn, `HomeScreen.kt`):** replace the `if (isFriday) item { JumuahCard }`
  block with `if (events.isNotEmpty()) item("events") { EventsCarousel(...) }`, sitting where
  JumuahCard was — between the `HomeBannerCarousel` item and the `today_section` item. The
  `AnnouncementBanner` item stays separate above the banner carousel.
- **Placement (tablet two-pane):** follow the full-width `HomeBannerCarousel` placement above the
  split (the right column uses stacked `TodayInfoCards`, not a carousel; event cards must stay a
  carousel, so they go full-width up top).
- **Ordering within the list:** at most two pushed/local cards by `IslamicEvent.priority` desc,
  pushed before local; Jumu'ah is always present as its own page when `isFriday` (never suppressed).
- Home UI state gains `events: List<EventCardUi>` (built by §7's use case). The existing
  `isFriday/jumuahTime/timeUntilJumuah/isJumuahPassed` fields feed the Jumu'ah page's construction.

---

## 4. Granular routing (source spec §1)

Unchanged from source spec:
- Parameterised `announcementRoute` grammar (§1.2): static `when` first, then
  `parameterisedAnnouncementRoute` matcher over path segments; integer args range-checked, string
  ids accepted syntactically (destination screens must have real empty states — verify
  `DuaReader`/`HadithReader`/`HadithBook` during implementation).
- Resolve-once (§1.4): `ResolveAnnouncementRouteUseCase` takes `resolveFeatureKey: (String) -> Route?`
  (not a boolean probe); `NavigateToFeature` carries both `routeKey: String` (analytics) and
  `route: Route` (navigation). Update `AnnouncementModule` binding to `::announcementRoute`.
  `MainActivity.pendingAnnouncementRoute` stays `String?` (survives process death).
- `announcement_route_rejected` analytics (§1.5) on the non-empty `None` branch.
- Extend the existing `AnnouncementRoutesTest` (§1.6) — do not start a new file.

---

## 5. `celebration` type (source spec §2)

Unchanged from source spec:
- `AnnouncementType.CELEBRATION("celebration")`; new `CelebrationEvent` enum whose keys match
  `IslamicEvents.events` ids (`eid_al_fitr`, `day_of_arafah`, `islamic_new_year`, …) so §7's merge
  is an id comparison. `CelebrationEvent.fromKey` degrades unknowns to `GENERIC` (never null).
- New nullable `Announcement` fields: `event, arabic, transliteration, proofRef, proofText,
  cta2Label, route2, startsAtMillis`. `isActiveFor` gains the `startsAtMillis` start gate.
- Mapper: add the 8 keys **and add them to `PAYLOAD_KEYS`** (the single easiest thing to get wrong
  — missing here means silent loss on tray-notification taps). Parse discipline per §2.2:
  `event` only when `type == CELEBRATION`; `starts_at` via `runCatching`/`return null`; drop **both**
  proof fields if only one present (local degrade, not payload discard); `arabic` trimmed
  `ifEmpty { null }`.
- Payload contract per §2.3. `title`/`body` remain required (tray fallback). `route2` validated
  against the same allowlist. Never push prayer times / countdowns / Hijri dates.

---

## 6. Hijri day-offset preference (source spec §3.5)

Unchanged: `hijri_day_offset: Int` (−2..+2, default 0) in `PreferencesDataStore`; surfaced in
`SettingsScreen` near calendar settings labelled "Adjust Hijri date"; `HijriDateCalculator.today()`
gains `offsetDays: Int = 0`; the two Hijri widgets read the same preference. Prerequisite for
shipping Eid cards — without it a slice of users get Eid on the wrong morning.

---

## 7. Two-source merge (source spec §3.4)

`domain/usecase/ObserveEventCardUseCase` combines local (`HijriDateCalculator.today(offset)` matched
against `IslamicEvents.events` by `(hijriMonth, hijriDay)`, plus `isFriday` for Jumu'ah) with pushed
celebrations (`ObserveActiveAnnouncementUseCase` filtered to `type == CELEBRATION`). Merge: if
`pushed.event.key == local.id` → one merged card (pushed fills what it has, local fills the rest);
else both render, sorted by priority. Output is the `List<EventCardUi>` the carousel consumes.
Local-first guarantees a card on Eid morning regardless of push delivery.

---

## 8. Cleanup / debt recorded

- **`IslamicEventCard` overlap.** After this work, `IslamicEventCard` (molecule) and `EventCard`
  (organism) both render occasion cards. Future task: migrate `IslamicEventCard` call sites onto
  `EventCard` and retire the molecule, and reconcile `HijriDateCalculator.EventType` with
  `CelebrationEvent`. Not in scope here per decision §0.2.
- **Prune dismissed ids past expiry** (source spec §0.1 item 5) — final step.

---

## 9. Sequencing (each independently shippable/revertible)

1. `feat(nav): parameterised announcement route grammar` — §4 grammar + tests. Fixes the dead CTA.
2. `refactor(nav): resolve announcement routes once` — §4 resolve-once.
3. `feat(analytics): announcement_route_rejected` — §4.
4. `feat(ui): EventCard organism` — §2, previews light+dark, every occasion.
5. `refactor(home): rebuild JumuahCard on EventCard + EventsCarousel` — §2–3; Jumu'ah becomes a
   carousel page; Home wiring swapped from vertical item to carousel.
6. `feat(calendar): hijri day offset preference` — §6, incl. widgets.
7. `feat(events): local event source from IslamicEvents` — §7 local half.
8. `feat(fcm): celebration announcement type` — §5, incl. `PAYLOAD_KEYS`.
9. `feat(events): merge pushed celebrations with local events` — §7 merge half.
10. `chore(announcements): prune dismissed ids past expiry` — §8.

Steps 1–5 improve the app with zero FCM involvement. If the celebration work stalls, nothing is
left half-built.

---

## 10. Verification before broadcast (source spec §5)

The source spec's §5 table stands in full (bad-type discard, `min_version_code` gating,
`PAYLOAD_KEYS` cold-start proof, route2 invalid, proof half-chip, unknown event → GENERIC,
`starts_at` future, patterns off, 200% font, dark theme, airplane-mode local Eid, Eid-on-Friday).
Plus for the carousel: **Eid on a Friday shows two carousel pages (Eid + Jumu'ah), swipeable, one
fixed height** — no vertical stacking. Always send FCM to your own token first; topic sends can't
be recalled.

## Docs to update as part of the work (per CLAUDE.md)

- `docs/NAVIGATION.md` — new parameterised announcement route keys.
- `docs/SUBSYSTEMS.md` — notifications/announcements pipeline (celebration type, `PAYLOAD_KEYS`,
  `startsAtMillis`), preferences (`hijri_day_offset`), prayer-time/calendar (offset), widgets.
- `docs/ARCHITECTURE.md` §9 — record the `IslamicEventCard`/`EventCard` overlap as a known
  deviation until §8 cleanup.
- `docs/CLEAN_ARCHITECTURE_CHECKLIST.md` — tick JumuahCard's hand-rolled well/divider removal.
