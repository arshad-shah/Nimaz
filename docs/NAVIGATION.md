# Nimaz — Navigation Map

> **Single source of truth for the app's navigation graph.** Whenever you add, remove, or
> rename a `Route`, **update this file in the same change** (the diagram *and* the route table).
> See [`ARCHITECTURE.md` §7](ARCHITECTURE.md) for the navigation patterns and conventions.

Navigation is **type-safe**: a `@Serializable sealed interface Route`
(`core/navigation/Routes.kt`) plus `composable<Route.X>` wiring in
`core/navigation/NavGraph.kt`. Typed arguments are read with
`backStackEntry.toRoute<Route.X>()`. Bottom navigation is the `BottomNavDestination` enum.

## Graph

```mermaid
flowchart LR
    Onboard([Onboarding]) --> Home
    subgraph BN["Bottom navigation"]
        Home[Home]
        QuranHub[Quran]
        TasbihHub[Tasbih]
        Qibla[Qibla]
        More[More]
    end

    subgraph Q["Quran"]
        QuranHub --> QuranReader & QuranPage & QuranJuz & QuranSearch & QuranBookmarks
        QuranReader --> SurahInfo & Tafseer & SelectReciter
    end

    subgraph Names["Names & Prophets"]
        AsmaUlHusnaList --> AsmaUlHusnaDetail
        AsmaUnNabiList --> AsmaUnNabiDetail
        ProphetsList --> ProphetDetail
    end

    subgraph Kh["Khatam"]
        KhatamList --> KhatamDetail & KhatamCreate
    end

    subgraph Qaida["Qaida"]
        QaidaHome --> QaidaReader & QaidaLetters
    end

    subgraph H["Hadith"]
        HadithHome --> HadithBook --> HadithChapter --> HadithReader
        HadithHome --> HadithSearch & HadithBookmarks & HadithSettings
    end

    subgraph D["Dua"]
        DuaHome --> DuaCategory --> DuaReader
        DuaHome --> DuaFavorites & DuaSearch & DuaSettings
    end

    subgraph P["Prayer"]
        PrayerTimes --> PrayerTracker & PrayerStats & QadaPrayers & MonthlyPrayerTimes
    end

    subgraph F["Fasting"]
        FastingHome --> FastingTracker & FastingStats
    end

    subgraph T["Tasbih screens"]
        TasbihHub --> TasbihCounter & TasbihPresets & TasbihStats & TasbihHistory & TasbihAddPreset
    end

    subgraph Z["Zakat / Calendar"]
        ZakatCalculator --> ZakatHistory
        IslamicCalendar --> IslamicMonth
    end

    subgraph S["Settings"]
        Settings --> SettingsPrayerCalculation & SettingsNotifications & SettingsAppearance & SettingsLanguage & SettingsLocation & SettingsQuran & SettingsWidgets & SettingsAbout
        Settings --> SettingsHelp & SettingsSync
        SettingsHelp --> HelpTopicDetail & HelpGuide
        SettingsAbout --> Licenses --> LicenseDetail
    end

    More --> HadithHome & DuaHome & PrayerTimes & FastingHome & ZakatCalculator & IslamicCalendar
    More --> AsmaUlHusnaList & AsmaUnNabiList & ProphetsList & KhatamList & QaidaHome
    More --> AllBookmarks & GlobalSearch & Settings
    Qibla --> Qibla
```

> The graph shows the primary reachability, not every edge (most screens can also be reached
> via deep links and cross-feature actions). `Home`, `Quran`, `Tasbih`, `Qibla`, and `More` are
> the five bottom-nav roots; `More` is the hub for everything else.

## Route reference

All routes live in `core/navigation/Routes.kt` and are wired in `core/navigation/NavGraph.kt`
(75 `composable<Route.X>` destinations). `data object` = no args; `data class` = typed args.

### Bottom navigation (`BottomNavDestination`)
| Route | Screen |
|-------|--------|
| `Home` | HomeScreen |
| `Quran` | QuranHomeScreen |
| `Tasbih` | TasbihHomeScreen |
| `QiblaNav` → `Qibla` | QiblaScreen |
| `More` | MoreScreen (hub) |

### Quran
| Route | Args | Screen |
|-------|------|--------|
| `QuranReader` | `surahNumber: Int, ayahNumber: Int = 1` | QuranReaderScreen |
| `QuranPage` | `pageNumber: Int` | QuranPageScreen (mushaf) |
| `QuranJuz` | `juzNumber: Int` | QuranJuzScreen |
| `QuranSearch` | — | QuranSearchScreen |
| `QuranBookmarks` | — | QuranBookmarksScreen |
| `SurahInfo` | `surahNumber: Int` | SurahInfoScreen |
| `Tafseer` | `surahNumber: Int, ayahNumber: Int = 1` | TafseerScreen |
| `SelectReciter` | — | SelectReciterScreen |

### Hadith
| Route | Args | Screen |
|-------|------|--------|
| `HadithHome` | — | HadithCollectionScreen |
| `HadithBook` | `bookId: String` | HadithBookScreen |
| `HadithChapter` | `bookId: String, chapterId: String` | HadithChapterScreen |
| `HadithReader` | `hadithId: String` | HadithReaderScreen |
| `HadithSearch` | — | HadithSearchScreen |
| `HadithBookmarks` | — | HadithBookmarksScreen |
| `HadithSettings` | — | HadithSettingsScreen |

### Dua
| Route | Args | Screen |
|-------|------|--------|
| `DuaHome` | — | DuasCollectionScreen |
| `DuaCategory` | `categoryId: String` | DuaCategoryScreen |
| `DuaReader` | `duaId: String` | DuaReaderScreen |
| `DuaFavorites` | — | DuaFavoritesScreen |
| `DuaSearch` | — | DuaSearchScreen |
| `DuaSettings` | — | DuaSettingsScreen |

### Prayer
| Route | Args | Screen |
|-------|------|--------|
| `PrayerTimes` | — | PrayerTimesScreen |
| `PrayerTracker` | `initialTab: Int = 0` | PrayerTrackerScreen |
| `PrayerStats` | — | PrayerStatsScreen |
| `QadaPrayers` | — | QadaPrayersScreen |
| `MonthlyPrayerTimes` | — | MonthlyPrayerTimesScreen |

### Fasting
| Route | Args | Screen |
|-------|------|--------|
| `FastingHome` | — | FastTrackerScreen |
| `FastingTracker` | — | FastTrackerScreen |
| `FastingStats` | — | (fasting stats) |

> **Makeup fasts is a tab inside `FastTrackerScreen`** (driven by `FastingEvent.LoadMakeupFasts`),
> not a standalone route. There is intentionally **no** `Route.MakeupFasts`.

### Tasbih
| Route | Args | Screen |
|-------|------|--------|
| `TasbihHome` | — | TasbihHomeScreen |
| `TasbihCounter` | `presetId: Long? = null` | TasbihCounterScreen |
| `TasbihPresets` | — | TasbihPresetsScreen |
| `TasbihStats` | — | TasbihStatsScreen |
| `TasbihHistory` | — | TasbihHistoryScreen |
| `TasbihAddPreset` | — | TasbihAddPresetScreen |

### Zakat, Qibla, Calendar
| Route | Args | Screen |
|-------|------|--------|
| `ZakatCalculator` | — | ZakatCalculatorScreen |
| `ZakatHistory` | — | ZakatHistoryScreen |
| `Qibla` | — | QiblaScreen |
| `IslamicCalendar` | — | IslamicCalendarScreen |
| `IslamicMonth` | `month: Int, year: Int` | IslamicMonthScreen |

### Qaida (children's Arabic reader)
| Route | Args | Screen |
|-------|------|--------|
| `QaidaHome` | — | QaidaHomeScreen |
| `QaidaReader` | `lessonId: Int` | QaidaReaderScreen |
| `QaidaLetters` | — | QaidaLettersScreen |

### Names & Prophets
| Route | Args | Screen |
|-------|------|--------|
| `AsmaUlHusnaList` | — | AsmaUlHusnaListScreen |
| `AsmaUlHusnaDetail` | `nameId: Int` | AsmaUlHusnaDetailScreen |
| `AsmaUnNabiList` | — | AsmaUnNabiListScreen |
| `AsmaUnNabiDetail` | `nameId: Int` | AsmaUnNabiDetailScreen |
| `ProphetsList` | — | ProphetsListScreen |
| `ProphetDetail` | `prophetId: Int` | ProphetDetailScreen |

### Khatam
| Route | Args | Screen |
|-------|------|--------|
| `KhatamList` | — | KhatamListScreen |
| `KhatamDetail` | `khatamId: Long` | KhatamDetailScreen |
| `KhatamCreate` | — | KhatamCreateScreen |

### Settings & misc
| Route | Args | Screen |
|-------|------|--------|
| `Settings` | — | SettingsScreen |
| `SettingsPrayerCalculation` | — | PrayerCalculationSettingsScreen |
| `SettingsNotifications` | — | NotificationSettingsScreen |
| `SettingsAppearance` | — | AppearanceSettingsScreen |
| `SettingsLanguage` | — | LanguageSettingsScreen |
| `SettingsLocation` | — | LocationSettingsScreen |
| `SettingsQuran` | — | QuranSettingsScreen |
| `SettingsWidgets` | — | WidgetsScreen |
| `SettingsAbout` | — | AboutScreen |
| `SettingsHelp` | — | HelpScreen |
| `HelpTopicDetail` | `topicId: String` | HelpTopicDetailScreen |
| `HelpGuide` | `guideId: String` | HelpGuideScreen |
| `SettingsSync` | — | SyncScreen |
| `Licenses` | — | LicensesScreen |
| `LicenseDetail` | `libraryHashCode: Int` | LicenseDetailScreen |
| `Onboarding` | — | OnboardingScreen |
| `AllBookmarks` | — | BookmarksScreen |
| `GlobalSearch` | — | SearchScreen |

## Adding / changing a route — checklist

1. Add a `@Serializable` entry to `Route` in `core/navigation/Routes.kt` (`data object` for
   no-arg, `data class` for typed args; give args sensible defaults).
2. Add a `composable<Route.X> { backStackEntry -> ... }` in `core/navigation/NavGraph.kt`; read
   typed args with `backStackEntry.toRoute<Route.X>()`.
3. Put the screen under `presentation/screens/<feature>/`.
4. Navigate via the typed route object (`navController.navigate(Route.X(...))`).
5. If it's a deep-link target, wire it in `core/navigation/HelpDeepLink.kt`.
6. **Update this file** — add the route to the table and (if it changes the high-level map) the
   diagram. Validate the diagram (Mermaid).
7. Remember: **not every `Route` is a full screen** — some features are tabs/sections inside a
   parent screen (e.g. makeup fasts inside `FastTrackerScreen`). Don't add a `composable` for
   those, and don't leave an orphaned `Route` declaration either.

*Keep this file honest — a route that isn't in this table is a documentation bug.*
</content>
