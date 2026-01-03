[![better commits is enabled](https://img.shields.io/badge/better--commits-enabled?style=for-the-badge&logo=git&color=a6e3a1&logoColor=D9E0EE&labelColor=302D41)](https://github.com/Everduin94/better-commits)

---

# Nimaz - Your Complete Islamic Companion

<p align="center">
  <img src="https://img.shields.io/badge/Version-2.6.32-blue?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/Min%20SDK-28-orange?style=for-the-badge" alt="Min SDK">
  <img src="https://img.shields.io/badge/Target%20SDK-36-orange?style=for-the-badge" alt="Target SDK">
  <img src="https://img.shields.io/badge/License-MIT-red?style=for-the-badge" alt="License">
</p>

Nimaz is a modern, comprehensive Islamic companion app for Android that helps Muslims maintain their spiritual practices with elegance and ease. Built with **Jetpack Compose** and **Material 3 Design**, Nimaz provides a beautiful, intuitive interface that adapts to your preferences while keeping you connected with your faith.

---

## 📱 Overview

Nimaz is more than just a prayer times app—it's your complete spiritual companion. Whether you're tracking your daily prayers, reading the Quran with translations, calculating Zakat, or finding the Qibla direction, Nimaz provides all the tools you need in one beautifully designed application.

### Why Nimaz?

- 🎨 **Modern Material 3 UI** - Beautiful, adaptive interface with dynamic theming
- 🌙 **Offline First** - Works seamlessly without internet connection
- 📍 **Smart Location** - Automatic and manual location settings for accurate prayer times
- 🎯 **Comprehensive** - All-in-one solution for Islamic practices
- 🌍 **Multi-language** - Support for English and Urdu translations
- 🔔 **Smart Notifications** - Customizable prayer time reminders
- 🎭 **Flexible Theming** - Multiple theme options with light and dark modes

---

## ✨ Features

### 🕌 Prayer Times & Tracking
- **Accurate Prayer Times** - Location-based calculation for all five daily prayers
- **Next Prayer Timer** - Countdown to the next prayer with visual indicators
- **Prayer Tracker** - Track your daily prayers with an elegant calendar interface
- **Custom Adjustments** - Fine-tune prayer times based on your local mosque or preferences
- **Smart Notifications** - Configurable alarms and notifications for each prayer
- **Fasting Tracker** - Keep track of your fasting days and maintain consistency

### 📖 Complete Quran Experience
- **Full Quran Text** - Complete Arabic text with diacritical marks
- **Multi-language Translations** - English and Urdu translations available
- **Advanced Search** - Search across Quran text with multiple filters:
  - Search by Surah, Juz, or specific Aya
  - Filter by language (Arabic, English, Urdu)
  - Search within favorites and bookmarks
  - Real-time search with text highlighting
- **Reading Modes**:
  - Traditional continuous reading
  - Page-by-page pagination mode
  - Jump to specific Surah, Juz, or Page
- **Audio Recitation** - Listen to Quran recitations from multiple reciters
- **Bookmarks & Favorites** - Save and organize your favorite verses
- **Reading Progress** - Track your reading progress and Khatam sessions
- **Notes & Annotations** - Add personal notes to specific Ayat
- **Tafsir (Exegesis)** - In-depth explanations and commentary

### 🎯 Navigation & Orientation
- **Qibla Compass** - Accurate Qibla direction finder using device sensors
- **Visual Indicators** - Clear compass with degree measurements
- **Location-based** - Automatic calculation based on your GPS location

### 📿 Dhikr & Tasbih
- **Digital Tasbih Counter** - Count your dhikr with haptic feedback
- **Pre-loaded Tasbihs** - Common dhikr phrases ready to use
- **Custom Tasbihs** - Create and save your own dhikr lists
- **Tasbih Categories** - Organized by different occasions and purposes
- **Progress Tracking** - Monitor your daily dhikr goals

### 📅 Islamic Calendar
- **Dual Calendar System** - View both Gregorian and Hijri dates
- **Date Converter** - Convert between Gregorian and Hijri calendars
- **Islamic Events** - Important dates and occasions marked
- **Prayer & Fasting History** - Visual calendar showing your spiritual progress

### 📚 Additional Features
- **Hadith Collections** - Browse authentic Hadith from major collections:
  - Organized by books and chapters
  - Favorite Hadith for quick access
  - Search within Hadith text
- **99 Names of Allah** - Beautiful presentation with meanings
- **Duas** - Collection of daily supplications organized by category
- **Zakat Calculator** - Calculate your Zakat obligations accurately
- **Shahadah** - Declaration of faith with transliteration

### ⚙️ Customization & Settings
- **Multiple Themes** - Choose from various color schemes
- **Dark Mode** - Full dark mode support with multiple variants
- **Material You** - Dynamic colors based on your Android theme (Android 12+)
- **Prayer Time Calculation Methods** - Multiple calculation methods for different regions
- **Notification Settings** - Customize when and how you receive notifications
- **Language Preferences** - Set your preferred translation language
- **Debug Tools** - Advanced settings for developers and power users

---

## 🏗️ Technical Architecture

### Technology Stack

#### Frontend
- **Jetpack Compose** - Modern declarative UI toolkit for Android
- **Material 3 Design** - Latest Material Design components and theming
- **Navigation Compose** - Type-safe navigation with Compose
- **Kotlin** - Modern, concise, and safe programming language

#### Architecture Pattern
- **MVVM (Model-View-ViewModel)** - Clean separation of concerns
- **Repository Pattern** - Abstraction layer for data sources
- **Clean Architecture** - Maintainable and testable codebase
- **Unidirectional Data Flow** - Predictable state management

#### Dependency Injection
- **Hilt** - Compile-time dependency injection built on Dagger

#### Database
- **Room** - SQLite abstraction with compile-time verification
- **DataStore** - Modern key-value storage for preferences

#### Asynchronous Operations
- **Kotlin Coroutines** - Lightweight concurrency
- **StateFlow** - State management with Flow

#### Backend Services
- **Firebase Crashlytics** - Crash reporting and analytics
- **Firebase Performance** - Performance monitoring
- **WorkManager** - Reliable background task scheduling

#### External Integrations
- **AWS S3** - Audio file storage and delivery
- **Google Location Services** - GPS and location-based features

### Project Structure

```
app/
├── src/main/java/com/arshadshah/nimaz/
│   ├── activities/          # Application activities
│   ├── constants/           # App-wide constants
│   ├── data/               # Data layer
│   │   ├── local/          # Local data sources
│   │   │   ├── dao/        # Room DAOs
│   │   │   └── models/     # Data models
│   │   └── remote/         # Remote data sources
│   ├── modules/            # Hilt dependency injection modules
│   ├── repositories/       # Repository implementations
│   ├── services/           # Background services
│   ├── ui/                 # Presentation layer
│   │   ├── components/     # Reusable UI components
│   │   ├── navigation/     # Navigation setup
│   │   ├── screens/        # Screen composables
│   │   └── theme/          # Theming and styling
│   ├── utils/              # Utility classes
│   ├── viewModel/          # ViewModels
│   └── widgets/            # App widgets
├── res/                    # Resources
└── build.gradle           # App-level Gradle configuration
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK** 11 or higher
- **Android SDK** with minimum API level 28
- **Gradle** 8.13 or newer (included with Android Studio)

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/arshad-shah/Nimaz.git
   cd Nimaz
   ```

2. **Create a `secrets.properties` file** in the root directory (optional for full functionality):
   ```properties
   DO_ACCESS_KEY=your_digitalocean_access_key
   DO_SECRET_KEY=your_digitalocean_secret_key
   METAL_API_KEY=your_metal_api_key
   ```
   *Note: The app will work without these keys, but some features like audio downloads may be limited.*

3. **Open the project in Android Studio:**
   - Launch Android Studio
   - Select **"Open an existing project"**
   - Navigate to the cloned repository and select it
   - Wait for Gradle sync to complete

4. **Configure Firebase** (optional for analytics):
   - Add your `google-services.json` file to the `app/` directory
   - Or disable Firebase dependencies if not needed

5. **Build and run:**
   - Select a physical device or emulator (API 28+)
   - Click the **Run** button (▶️) or press `Shift + F10`

### Building from Command Line

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing configuration)
./gradlew assembleRelease

# Run tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

---

## 🧪 Testing

The project includes comprehensive testing:

- **Unit Tests** - ViewModels and business logic
- **Instrumentation Tests** - UI and integration tests
- **Navigation Tests** - Screen navigation flows

Run tests:
```bash
# Unit tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest
```

---

## 📖 Documentation

Additional documentation is available in the repository:

- **[Design System](Design_system.md)** - UI/UX design patterns and guidelines
- **[Quran Search Documentation](QURAN_SEARCH_DOCS.md)** - Technical details of the search implementation

---

## 🤝 Contributing

We welcome contributions from the community! Whether it's bug fixes, new features, translations, or documentation improvements, your help is appreciated.

### How to Contribute

1. **Fork the repository** on GitHub
2. **Create a feature branch** from `main`:
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **Make your changes** following the coding standards:
   - Follow Kotlin coding conventions
   - Use meaningful commit messages (better-commits enabled)
   - Add tests for new features
   - Update documentation as needed
4. **Test your changes** thoroughly
5. **Commit your changes:**
   ```bash
   git commit -m 'feat: Add amazing feature'
   ```
6. **Push to your branch:**
   ```bash
   git push origin feature/amazing-feature
   ```
7. **Open a Pull Request** with a clear description of your changes

### Coding Standards

- Follow the existing code style and architecture patterns
- Use Jetpack Compose best practices
- Write clean, maintainable, and well-documented code
- Add comments for complex logic
- Ensure all tests pass before submitting PR

### Reporting Issues

Found a bug or have a feature request? Please open an issue on GitHub with:
- Clear description of the problem/feature
- Steps to reproduce (for bugs)
- Expected behavior
- Screenshots if applicable
- Device and Android version

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- **Quran Data** - Tanzil Project and other open-source Quran databases
- **Prayer Times Calculation** - Various Islamic organizations and calculation methods
- **Hadith Collections** - Authentic hadith sources
- **Open Source Libraries** - See [About Libraries](app/src/main/assets/aboutlibraries.json) in the app

---

## 📞 Contact & Support

- **Developer:** Arshad Shah
- **GitHub:** [@arshad-shah](https://github.com/arshad-shah)
- **Issues:** [GitHub Issues](https://github.com/arshad-shah/Nimaz/issues)

---

## 🗺️ Roadmap

Future enhancements planned:
- 📱 iOS version
- 🌐 More language translations
- 🎙️ More Quran reciters
- 📚 Additional Hadith collections
- 🤲 More Dua categories
- 🎨 More theme options
- ⌚ Wear OS support

---

<p align="center">
  <b>May this app help you in your spiritual journey. Ameen.</b>
</p>

<p align="center">
  Made with ❤️ by <a href="https://github.com/arshad-shah">Arshad Shah</a>
</p>

---

