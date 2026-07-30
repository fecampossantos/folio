# Folio

> **A modern, privacy-first, open-source EPUB reader for Android built with Kotlin & Jetpack Compose.**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)](LICENSE)

**Folio** is a fast, offline-first native Android ebook reader designed for users who value privacy, customization, and seamless reading state portability.

---

## Features

- **Continuous Multi-Chapter Horizontal Reader (Kindle-style)**
  - Smooth page-flipping continuous horizontal pager across all chapters.
  - Manual swiping or navigation arrows seamlessly transition across chapter boundaries.
  - Dynamic page-budgeting engine ensuring multi-paragraph text fitting even at large font sizes (> 26sp).

- **Book Cover Extraction & Custom Cover Picker**
  - Automatic extraction of cover images (`.jpg`, `.png`, `.webp`) embedded inside EPUB containers.
  - Custom image picker allows assigning downloaded covers to books without embedded images.

- **Folder Storage Access Framework (SAF)**
  - Pick any local directory containing EPUB files using Android SAF tree picker.
  - Automatically scans and indexes all `.epub` books without duplicating or copying files.

- **Portable JSON Reading State Persistence**
  - All reading progress, exact chapter and page locations, reading time, bookmarks, and theme preferences are saved in human-readable `reading_history.json`.
  - Auto-resumes reading at your exact page location on launch.

- **Reading Customization**
  - **Themes**: Light (`#FFFFFF`), Dark (`#121212`), and Sepia (`#FBF0D9` warm paper theme).
  - **Typography**: Dynamic font resizing (12sp to 32sp) with responsive line-height calculation.

- **Text-To-Speech (TTS) Narration**
  - Built-in Android TTS narration to listen to chapters on the go.

- **Table of Contents & Bookmarks**
  - Instant chapter navigation drawer with full Table of Contents.
  - Create, manage, and delete chapter bookmarks with text snippet previews.

- **Library Search, Sorting & Statistics**
  - Search books by title, author, or file name.
  - Sort by _Recently Opened_, _Reading Progress_, _Title_, or _Author_.
  - View total reading time metrics and completed book counters.

- **JSON Backup Export & Import**
  - Export and import your reading history backup to migrate data between devices.

---

## Installation

1. Download the latest `app-release.apk` from the [Releases](https://github.com/your-username/epub-reader/releases) tab.
2. Install the APK on your Android device (Android 7.0 / API 24 or higher).
3. Open **Folio**, tap **Choose Folder**, and select your local EPUB directory.

---

## Building from Source

### Prerequisites

- **JDK 17** or higher (JDK 24 supported).
- **Android SDK** (API Level 34).
- Set `ANDROID_HOME` / `ANDROID_SDK_ROOT` environment variable.

### Building on Linux

```bash
# 1. Set environment variables
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=$HOME/Android/Sdk

# 2. Grant execution permissions
chmod +x gradlew

# 3. Run unit tests
./gradlew test

# 4. Build Release APK
./gradlew assembleRelease
```

### Building on Windows

```powershell
# 1. Set environment variables (PowerShell)
$env:JAVA_HOME="C:\Program Files\Java\jdk-24"
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"

# 2. Run unit tests
.\gradlew.bat test

# 3. Build Release APK
.\gradlew.bat assembleRelease
```

**APK Output Path**:

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

---

## Cross-Device Synchronization

You can sync your book library and reading history across Android devices, PCs, or NAS servers using various methods:

### Using GitHub

You can use GitHub to sync your library and reading history using a Git client for Android (like Termux or MGit) and a GitHub repository:

1. **Setup Repository**:
   - Create a private repository on GitHub to store your books.
   - Clone the repository to a local folder on your devices using a Git client.
2. **Sync Books Folder**:
   - Place your EPUB files in the cloned folder and commit/push them to GitHub.
   - Pull the changes on your other devices.
3. **Sync Reading State**:
   - In Folio on Device A, tap the overflow menu (`⋮`) -> **Export History Backup (JSON)** and save `reading_history_backup.json` in your Git folder.
   - Commit and push the changes to GitHub.
   - On Device B, pull the latest changes, tap the overflow menu (`⋮`) -> **Import History Backup (JSON)** to instantly restore your exact reading position.

### Using Syncthing

You can pair Folio with **[Syncthing](https://syncthing.net/)** to sync your book library and reading history:

1. **Sync Books Folder**:
   - Share your EPUB directory across devices using Syncthing.
2. **Sync Reading State**:
   - In Folio on Device A, tap the overflow menu (`⋮`) -> **Export History Backup (JSON)** and save `reading_history_backup.json` in your synced folder.
   - On Device B, tap the overflow menu (`⋮`) -> **Import History Backup (JSON)** to instantly restore your exact reading position.

---

## Architecture & Technology Stack

- **Language**: Kotlin 1.9+
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM + Repository Pattern
- **Storage**: Storage Access Framework (SAF) & `kotlinx.serialization`
- **Concurrency**: Kotlin Coroutines & `StateFlow`
- **Testing**: JUnit 4, Kotlin Coroutines Test

---

## Contributing

Contributions are welcome! Feel free to report issues, suggest feature ideas, or submit pull requests.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Run unit tests (`./gradlew test`)
5. Push to the branch (`git push origin feature/amazing-feature`)
6. Open a Pull Request

---

## License

Distributed under the **MIT License**. See `LICENSE` for more information.
