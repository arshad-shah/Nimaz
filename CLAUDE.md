# Nimaz — agent guide

Nimaz is an offline-first Android Islamic companion app: **Kotlin + Jetpack Compose**,
**Clean Architecture** (`presentation → domain → data`) with **MVVM + UDF**, Hilt DI, Room,
DataStore, type-safe Navigation Compose.

## Read this first

**[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) is the source of truth** for how this app is
structured (layer patterns, DI, navigation, theming, diagrams, a new-feature recipe, and a
tech-debt registry of known deviations). Follow it so the architecture does not drift. When
adding a feature, copy an existing one that follows the patterns — good references:
`AsmaUlHusna`, `Prophet`, `Khatam`, `Quran`.

## Non-negotiable rules

1. Dependencies point inward: **domain never imports `data`** (no Room entity/DAO/DataStore);
   presentation never imports entities/DAOs.
2. ViewModels inject **`XxxUseCases`**, not repositories or DAOs.
3. ViewModels expose `StateFlow<XxxUiState>` (immutable `data class`) + a single
   `onEvent(event: XxxEvent)` (sealed interface). No exposed `MutableStateFlow`/`LiveData`.
4. Repositories return **domain models**; map at the data layer (`Entity.toDomain()` /
   `Model.toEntity()`).
5. DI lives in `core/di`: `@Binds` for interface→impl, `@Provides` for `XxxUseCases`,
   `@Singleton` in `SingletonComponent`.
6. Navigation is type-safe: add a `@Serializable` `Route` + `composable<Route.X>` in
   `NavGraph`. (Not every `Route` is a screen — some features are tabs inside a parent
   screen; validate before wiring.)
7. No hardcoded `Color(0xFF…)` in screens — use `MaterialTheme.colorScheme.*` / `NimazColors.*`
   and reuse `presentation/components` (atoms/molecules/organisms).

## Verify before finishing

```bash
./gradlew :app:compileDebugKotlin     # runs KSP → validates Hilt + Room wiring
./gradlew :app:testDebugUnitTest
```

Requires JDK 21 + Android SDK (compileSdk 36); set `sdk.dir` in `local.properties` or
`ANDROID_HOME`. Develop on a feature branch; do not push to `dev` without explicit approval.

## Known deviations

Some areas intentionally diverge from the canonical patterns (use-case gaps, a Zakat
clean-arch leak, Calendar's direct DAO use, QaidaReader's missing `onEvent`, etc.). They are
catalogued in **§9 of `docs/ARCHITECTURE.md`** — do not copy them; fix them per the table.
</content>
