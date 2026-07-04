# Location Screen Redesign — Design Spec

**Date:** 2026-07-04
**Status:** Approved for planning
**Feature:** Redesign the Location settings screen (`Route.SettingsLocation`) in the Nimaz
design language, expanding the curated city list and grouping it by region.

---

## 1. Goal

Rework `LocationScreen` so that:

1. It reads as a first-class, native Nimaz screen (region-grouped "Direction A" layout).
2. The curated city list grows from **8 → ~40** notable cities, **grouped and filterable by
   region** so an expanded list stays browseable offline.
3. Each curated city shows its **country flag as an emoji** — a deliberate, documented exception
   to the project-wide "no emoji / Material icons only" rule (see §7).

Non-goals: no bundled offline city database, no map picker, no change to GPS/Geocoder mechanics,
no change to how a selected location is persisted (DataStore + Room stay as-is).

---

## 2. Layout (top → bottom)

```
Scaffold(background)
 └─ NimazBackTopAppBar(title = "Location")
     └─ LazyColumn(contentPadding = 20dp h / 8dp v, spacedBy 12dp)
        1. Current-location HERO card   — gradient, selected city + coords + CURRENT badge
        2. NimazSearchBar               — city / place search (Geocoder-backed, unchanged)
        3. "Use my current location"    — GPS action card (tonal)
        4. [Search results section]     — shown ONLY while a query is active; when shown,
                                          the Recent + Browse sections are hidden
        5. Recent section               — header + up to 5 rows; shown only if non-empty
        6. Browse by region
           a. Region filter chip row    — [All · Middle East · Asia · Europe · Americas · Africa]
           b. City list:
              • "All" selected     → cities grouped, a small region sub-header before each group
              • a region selected  → flat list of just that region's cities
```

Behavior notes:
- Selecting **All** groups by `CityRegion.order`, each group preceded by a section sub-header
  (region label). Selecting a specific region filters to that region only (no sub-header).
- The default selected chip is **All**.
- Search remains live/auto (≥2 chars) exactly as today; while `searchResults` is non-empty the
  region browse + recent sections collapse to keep focus on results.

---

## 3. Components (all existing — no new primitives)

| Element | Component |
|---|---|
| App bar | `NimazBackTopAppBar` |
| Hero card | `NimazCard(style = GRADIENT)` (existing `CurrentLocationCard`, restyled to tokens) |
| Search | `NimazSearchBar` |
| GPS button | `NimazCard` tonal (existing `UseCurrentLocationButton`) |
| Section headers | `NimazSectionTitle` / `NimazSectionHeader` |
| Region filter row | `NimazChip(variant = FILTER)` in a scrollable `Row` — mirrors the Asma "All/Favorites" pattern |
| City / recent row | `LocationListItem` (existing, extended to render a flag) |
| Selection check | existing check affordance in `LocationListItem` |

Spacing/shape/color use `NimazSpacing.*`, `NimazCornerRadius.*`, `MaterialTheme.colorScheme.*` /
`NimazColors.*` only — no magic numbers, no `Color(0xFF…)`.

---

## 4. Data model changes

Add a region enum and extend `SearchLocation` (in `LocationViewModel.kt`):

```kotlin
enum class CityRegion(val label: String, val order: Int) {
    MIDDLE_EAST("Middle East", 0),
    ASIA("Asia", 1),
    EUROPE("Europe", 2),
    AMERICAS("Americas", 3),
    AFRICA("Africa", 4),
}

data class SearchLocation(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val region: CityRegion? = null,   // set for curated cities; null for Geocoder & recent results
    val flag: String? = null,         // country-flag emoji for curated cities; null otherwise
)
```

- `region == null` / `flag == null` for Geocoder search results and DB-derived recent locations —
  those keep the `NimazIcon` (`Public` for recents, `LocationOn` for search) fallback.
- No database, entity, DAO, repository, or DataStore change. `region`/`flag` are presentation-only
  fields on the in-memory `SearchLocation`; persistence continues via the existing `Location`
  domain model.

### Curated city list (~40)

Replace `defaultPopularCities` with a regioned list (each entry carries `region` + `flag`).
Emphasis on Muslim-majority and major-diaspora cities. Target set:

- **Middle East:** Mecca 🇸🇦, Madinah 🇸🇦, Riyadh 🇸🇦, Jeddah 🇸🇦, Dubai 🇦🇪, Abu Dhabi 🇦🇪,
  Doha 🇶🇦, Kuwait City 🇰🇼, Manama 🇧🇭, Muscat 🇴🇲, Jerusalem 🇵🇸, Amman 🇯🇴, Baghdad 🇮🇶, Tehran 🇮🇷
- **Asia:** Istanbul 🇹🇷, Karachi 🇵🇰, Lahore 🇵🇰, Islamabad 🇵🇰, Dhaka 🇧🇩, Delhi 🇮🇳,
  Hyderabad 🇮🇳, Jakarta 🇮🇩, Kuala Lumpur 🇲🇾, Singapore 🇸🇬
- **Europe:** London 🇬🇧, Birmingham 🇬🇧, Paris 🇫🇷, Berlin 🇩🇪, Amsterdam 🇳🇱, Brussels 🇧🇪, Sarajevo 🇧🇦
- **Americas:** New York 🇺🇸, Chicago 🇺🇸, Dearborn 🇺🇸, Toronto 🇨🇦
- **Africa:** Cairo 🇪🇬, Casablanca 🇲🇦, Lagos 🇳🇬, Kano 🇳🇬, Khartoum 🇸🇩, Nairobi 🇰🇪

Coordinates: decimal lat/lng per city (well-known values). Exact list may be trimmed/adjusted
during implementation, but region + flag are mandatory for every curated entry.

---

## 5. ViewModel / state changes

`LocationUiState` gains region-filter state:

```kotlin
val selectedRegion: CityRegion? = null   // null = "All"
```

New event:

```kotlin
data class SelectRegion(val region: CityRegion?) : LocationEvent
```

Derivation (in the screen or a small state helper):
- `groupedCities: Map<CityRegion, List<SearchLocation>>` from `popularCities` (for "All").
- `visibleCities` = all-grouped when `selectedRegion == null`, else the single region's list.

No use-case or repository changes.

---

## 6. Bug fix folded into this work

`CurrentLocationCard` currently hardcodes the coordinate hemisphere as `° N, ° W`
(`LocationScreen.kt:282`). Replace with sign-derived hemispheres:

```kotlin
val ns = if (lat >= 0) "N" else "S"
val ew = if (lng >= 0) "E" else "W"
// format |lat|, |lng| with the correct suffix
```

---

## 7. Documented emoji exception

The project rule (CLAUDE.md rule 7, ARCHITECTURE.md §7) is "no emoji — Material icons via
`NimazIcon`". This screen introduces a **single, explicit exception**: **country flags on the
Location screen may be rendered as emoji.** No other emoji are permitted, on this screen or
elsewhere.

This exception must be recorded in:
- `docs/ARCHITECTURE.md` — §7 (theming/iconography rule) and the §9 deviation registry.

---

## 8. Documentation updates (part of the work)

- `docs/ARCHITECTURE.md` — §7 iconography note + §9 registry entry for the flag-emoji exception.
- `docs/SUBSYSTEMS.md` — if the location/preferences section describes the city list, note the
  curated-region expansion.
- No `docs/NAVIGATION.md` change (route unchanged). No schema change → no data-guide change.

---

## 9. Testing

- Unit-test the region grouping/filter derivation (`groupedCities`, `visibleCities` for
  `selectedRegion == null` vs a specific region).
- Unit-test the hemisphere formatting helper (N/S/E/W across the four sign combinations).
- Verify build: `./gradlew :app:compileDebugKotlin` (KSP/Hilt/Room wiring) and
  `./gradlew :app:testDebugUnitTest`.

---

## 10. Out of scope

Bundled offline city DB, map/geo picker, favouriting cities from this screen, per-city prayer
calculation presets, reordering recents. These remain future work.
