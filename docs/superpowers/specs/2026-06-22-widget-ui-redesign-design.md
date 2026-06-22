# Widget UI Redesign — "Refined Minimal"

**Date:** 2026-06-22
**Status:** Design approved, pending spec review
**Scope:** UI layer only for all five Glance home-screen widgets. No changes to data
loading, workers, state definitions, or the `provideGlance` plumbing.

## 1. Goal

Give the five widgets one coherent, modern visual language and **eliminate every
emoji / ASCII / unicode-glyph crutch** in favour of real vector iconography. Today
the widgets are text-only and lean on glyphs:

- Prayer Tracker: single-letter labels `F/D/A/M/I` and a unicode checkmark `✓` (U+2713).
- Em-dash `—` (U+2014) used as empty-state fallbacks across widgets.

These are replaced with proper vector drawables and full words.

## 2. Design language — "Refined Minimal"

The agreed direction (chosen over a tonal-card and a bold-hero alternative):

- **Solid surface.** Opaque card — `widget_background` (white in light, `#1C1917` in
  dark), `16dp` corner radius. No translucency/glass (legibility first).
- **One accent.** Teal `widget_primary` `#14B8A6` for highlights, the next prayer, the
  checkbox "done" state, the calendar "today", and icon tints.
- **Real vector icons**, tinted via `ColorFilter.tint(ColorProvider)` so they follow
  light/dark + accent automatically. No glyphs anywhere.
- **Crisp type scale, airy spacing.** Content-first, system-native feel.
- Every widget keeps a matching **dark variant** via the existing `values-night`
  color resources — no per-widget dark logic needed.

## 3. Iconography & assets

Glance renders to RemoteViews and **cannot** consume the app's Compose
`Icons.*` (`material-icons-extended`). All widget icons must ship as **vector
drawable XML** in `res/drawable/`. minSdk 29 + Glance 1.2.0 render vector drawables
in widgets correctly.

New drawables (all `24dp` viewport, stroke-based line icons matching the line weight
shown in mockups; rendered monochrome and tinted at runtime):

| Drawable | Used by | Meaning |
|---|---|---|
| `ic_widget_fajr.xml` | Next Prayer | dawn — horizon + rising sun + up chevron |
| `ic_widget_dhuhr.xml` | Next Prayer | zenith — full sun, all rays |
| `ic_widget_asr.xml` | Next Prayer | low sun above horizon |
| `ic_widget_maghrib.xml` | Next Prayer | sunset — sun on horizon + down chevron |
| `ic_widget_isha.xml` | Next Prayer | night — crescent moon |
| `ic_widget_check.xml` | Prayer Tracker | checkmark stroke (replaces `✓`) |
| `ic_widget_crescent.xml` | Hijri Date | crescent accent next to weekday |
| `ic_widget_event.xml` | Hijri Calendar | calendar/event marker |
| `ic_widget_star.xml` | Hijri Calendar | special-day / fast marker |

A single mapping helper resolves a prayer name → its celestial drawable res id
(see §6). The existing unused `ic_dua.xml` is left untouched.

> Note: the **Prayer Times** widget intentionally uses **no per-cell icons** (that was
> the clutter we removed). The celestial icons are only needed by Next Prayer.

## 4. Color tokens

Reuse the current palette in `res/values/widget_colors.xml` +
`res/values-night/widget_colors.xml`. No new colors are strictly required; the
"empty checkbox ring" uses `widget_unchecked`, "past prayer" uses
`widget_text_secondary`, the "next prayer pill" uses `widget_primary` with white text,
and the soft pill/badge backgrounds use `widget_primary_dim`.

If a dedicated faint divider/ring reads better than `widget_unchecked` we may add one
token (`widget_outline`), but the default is to reuse existing tokens.

## 5. Shared core (`widget/core/`)

Centralize the repeated UI atoms in `WidgetUi.kt` (consistent with the existing
`WidgetPalette`, `WidgetMessageBox`, `WidgetLoadingBox`). Add small, single-purpose,
reusable composables so each widget file shrinks to layout + data mapping:

- `WidgetCard(palette, onClick, padding, content)` — the standard solid, rounded,
  tappable surface (replaces the hand-rolled `Box(...).background().cornerRadius()`
  repeated in every widget).
- `WidgetIcon(resId, tint, size)` — `Image(ImageProvider(resId), colorFilter =
  ColorFilter.tint(tint))`, the one place icons are drawn.
- `WidgetLabel(text, palette)` — the uppercase 10sp tracking-wide caption.
- `WidgetPill(text, container, content)` — the rounded badge used for countdowns and
  the "next" highlight.
- `prayerIconRes(prayerName): Int` — name → celestial drawable mapping.

Error/empty states keep using `WidgetMessageBox`; the em-dash fallbacks stay **only**
as last-resort empty-data placeholders inside text (they are typographic, not
decorative glyphs) — acceptable, but where a value is structurally absent we prefer a
short word ("—" stays only for "no time available").

## 6. Per-widget specs

All five keep their current `provideGlance`/state handling; only the `Success`
composable changes. Sizes unchanged (see `res/xml/*_widget_info.xml`).

### 6.1 Next Prayer (2×2)
- `WidgetCard`, vertical layout, space-between.
- Top: `WidgetIcon(prayerIconRes(name), tint=primary, 16dp)` + `WidgetLabel("Next Prayer")`.
- Prayer name — 20sp Bold, primary.
- Time — 32–33sp ExtraBold, `widget_text`.
- Countdown — `WidgetPill`, primary text on `widget_primary_dim`, bottom-left.

### 6.2 Hijri Date (2×2)
- `WidgetCard`, centered column.
- Weekday row: `WidgetIcon(ic_widget_crescent, primary, 13dp)` + weekday (11sp, secondary).
- Hijri day — large, ~54sp ExtraBold, primary.
- Month + year — 14sp Bold, `widget_text`.
- Gregorian date — 11sp, secondary.

### 6.3 Prayer Times (4×1, resizable taller) — **Clean Pills**
- `WidgetCard`, column.
- Header row: location (12sp Bold) on the left; on the right a compact
  "`<hijri> · <NextPrayer> in <countdown>`" (10sp, secondary).
- Grid row: five equal-weight cells, **no icons**. Each cell = name (10sp) over time
  (15sp Bold).
  - Past prayers: name + time in `widget_text_secondary`/dimmed.
  - **Next prayer cell**: solid `widget_primary` rounded pill, white name + time.
  - Upcoming (not-yet, not-next): normal `widget_text`.
- Drives off existing `PrayerTimesData` (`*Passed` flags + `nextPrayerName`); no data
  change.

### 6.4 Prayer Tracker (4×1) — **custom checkbox**
- `WidgetCard`, column.
- Header: date label (11sp, secondary) + count badge `N / 5` (12sp Bold, primary).
- Five equal-weight cells, each a tappable column:
  - **Prayed**: filled `widget_primary` disc (`28dp`) containing
    `WidgetIcon(ic_widget_check, white, 16dp)`.
  - **Not prayed**: empty `2dp` outline ring (`widget_unchecked`), no inner content.
  - Full prayer name below (Fajr/Dhuhr/Asr/Maghrib/Isha) — 9–10sp; bold + `widget_text`
    when prayed, secondary when not.
- Tap behaviour (`togglePrayerStatus` + `enqueueImmediateWork`) is **unchanged**.

### 6.5 Hijri Calendar (4×2) — minor refresh
- Keep the two-panel layout (month grid left, today panel right) — it was approved.
- Left: header (month/year + gregorian), weekday strip (Friday tinted primary), month
  grid with **today as a filled primary circle**.
- Right: "TODAY" label + large day number + divider, then events.
  - **Events use real icons** instead of the tiny drawn accent dots:
    `WidgetIcon(ic_widget_event, primary, 13dp)` (or `ic_widget_star` for
    recommended-fast/special days) + event name (max 2 lines).
- Existing deep-link action (`ACTION_OPEN_ISLAMIC_CALENDAR`) unchanged.

## 7. Preview layouts (RemoteViews)

Each widget ships a static `res/layout/widget_*_preview.xml` used by the picker. These
must be updated to visually match the new design (they are plain `LinearLayout`/
`TextView` RemoteViews, not Glance). Where the preview should show an icon, reference
the new drawables. `glance_default_loading.xml` stays as-is.

## 8. Out of scope (YAGNI)

- No new widgets, no resize-aware (`SizeMode.Responsive`) layout variants beyond what
  exists.
- No data/worker/state-definition changes.
- No theming system beyond the existing `widget_colors.xml` light/dark resources.
- No animation (Glance can't).

## 9. Verification

- `./gradlew :app:compileDebugKotlin` (KSP/Hilt/Room wiring intact).
- `./gradlew :app:testDebugUnitTest`.
- Manual: add each widget to the launcher in light **and** dark mode; confirm no
  glyphs render, icons tint correctly, Tracker toggle still writes + refreshes, and
  the calendar deep-link still opens.

## 10. Docs to update (part of the work)

- `docs/SUBSYSTEMS.md` — widgets section: new shared `widget/core` atoms, the
  drawable-based icon approach, per-widget visual description.
- No route/DB/schema changes, so `NAVIGATION.md` / data guide are untouched.
