# Nimaz

Nimaz is an Android Islamic companion app focused on offline-first usage, accurate prayer tooling, and a broad set of daily-use Islamic features.

## Overview

The app is built with modern Android tooling:

- Kotlin + Jetpack Compose UI
- Clean architecture (data, domain, presentation)
- MVVM with StateFlow-driven state
- Hilt for dependency injection
- Room + prepopulated local database for core content
- DataStore for user settings
- WorkManager and Glance widgets

Core content and functionality are designed to remain available without constant network access.

> **Documentation:** [`docs/README.md`](docs/README.md) is the index — it says which doc owns
> which area and where to start. [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) is the source of
> truth for layering, patterns, DI, navigation, theming, diagrams, and the new-feature recipe.
> Before changing anything, read [`docs/DOCUMENTATION.md`](docs/DOCUMENTATION.md) — the contract
> for keeping the docs true, enforced on every PR by `python3 scripts/check_docs.py`.
> Agents should also read [`CLAUDE.md`](CLAUDE.md).

## Main Features

- Prayer times and prayer tracking
- Quran reading, tafseer, bookmarks, and audio playback
- Hadith collections and reader
- Duas and adhkar browsing
- Tasbih counter with presets and history
- Qibla direction tools
- Hijri calendar and Islamic events
- Fasting tracker
- Zakat calculator
- Widgets for prayer and date information
- Global search and unified bookmarks
- Onboarding and in-app settings for appearance, language, notifications, and Quran options

## Tech Stack

- Android Gradle Plugin 8.12.0
- Kotlin 2.3.0
- Jetpack Compose BOM 2026.01.00
- Hilt 2.57.1
- Room 2.8.4
- Navigation Compose 2.9.6
- Coroutines 1.10.2
- Media3 1.9.0
- WorkManager 2.11.0

For the full dependency catalog, see:

- `gradle/libs.versions.toml`

## Repository Structure

```text
app/
  src/main/java/com/arshadshah/nimaz/
    core/          navigation, DI, utilities, initialization, monitoring
    data/          Room, repositories, services, local data
    domain/        models, repository contracts, use cases
    presentation/  Compose screens, components, theme, viewmodels
    widget/        app widgets
docs/             the source of truth — start at docs/README.md
                  (content data and the nz console live in arshad-shah/nimaz-data)
scripts/          repo checks that need no Android toolchain (docs drift, contrast)
fastlane/         CI and release lanes
```

## Prerequisites

- JDK 21
- Android SDK configured (compileSdk 36, minSdk 29)
- Ruby + Bundler (for Fastlane commands)

## Getting Started

From the repository root:

1. Install Ruby gems:

   ```bash
   bundle install
   ```

2. Run tests and lint (same lane used in CI):

   ```bash
   bundle exec fastlane android test
   ```

3. Build debug APK:

   ```bash
   ./gradlew assembleDebug
   ```

## Development Commands

- Run unit tests:

  ```bash
  ./gradlew test
  ```

- Run lint:

  ```bash
  ./gradlew lint
  ```

- Generate JaCoCo report:

  ```bash
  ./gradlew jacocoTestReport
  ```

## Data and Assets

- Content data and the tooling that compiles it live in a separate private repository,
  **arshad-shah/nimaz-data**. The prepopulated database is a build output fetched from a
  release there and pinned by sha256 in `data.lock.json` — it is no longer tracked here.
- How a content correction reaches devices that already have the app:
  `docs/DATA_RETIREMENT.md`
- Historical data specification reference: `docs/nimaz-pro-data-guide.md`

## CI

Pull requests run Android checks through:

- `.github/workflows/pr_checks.yml`

Primary CI lane:

- `bundle exec fastlane android test` (runs Gradle tests and lint)

## Release and Deployment

Fastlane lanes in `fastlane/Fastfile` include internal and beta deployment workflows. These lanes handle version bumping and signed artifact builds for Play Store publishing.

## License

If a license file is added to the repository, reference it here. Currently, no top-level `LICENSE` file is present.
