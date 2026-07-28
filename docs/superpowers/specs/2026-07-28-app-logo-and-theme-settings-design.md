# Folio App Logo and Material 3 Theme Settings Design Document

## Goal
Integrate the official Folio app logo (`logo.png`) into the Android launcher icons and main top app bar, and implement full Material 3 Light/Dark/System theme switching based on design specifications (`DESIGN-light.md` and `DESIGN-dark.md`).

## 1. App Identity & Logo Assets
- **Launcher Icon**: Convert `docs/ui/logo.png` into Android mipmap launcher icons (`ic_launcher.png`, `ic_launcher_round.png`) across all standard density buckets (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`).
- **In-App Branding**: Copy `logo.png` into `app/src/main/res/drawable/ic_app_logo.png` and render it as a 32dp composable icon alongside "Folio Library" in the `LibraryScreen` top app bar.

## 2. Material 3 Sanctuary Theme Tokens
- **`Color.kt`**:
  - Add light theme color tokens:
    - Primary: `#475949` (Sage Green)
    - OnPrimary: `#ffffff`
    - PrimaryContainer: `#5f7161`
    - Background/Surface: `#fff8f1` (Parchment)
    - OnBackground/OnSurface: `#1f1b12` (Warm Charcoal)
    - SurfaceContainer: `#f6edde`
    - Secondary: `#625e56` (Muted Stone)
  - Add dark theme color tokens:
    - Primary: `#c3e2ba` (Soft Sage)
    - OnPrimary: `#1d361b`
    - PrimaryContainer: `#a8c6a0`
    - Background/Surface: `#121412` (Olive Charcoal)
    - OnBackground/OnSurface: `#e2e3df`
    - SurfaceContainer: `#1e201e`
    - Secondary: `#c0c9c1`
- **`Theme.kt`**:
  - Introduce `AppThemeMode` enum: `LIGHT`, `DARK`, `SYSTEM`.
  - Update `EpubReaderTheme` composable to accept `appThemeMode: AppThemeMode = AppThemeMode.LIGHT`.
  - Calculate `darkTheme` boolean based on mode:
    - `LIGHT` -> `false`
    - `DARK` -> `true`
    - `SYSTEM` -> `isSystemInDarkTheme()`

## 3. User Theme Settings & Persistence
- **State Persistence**:
  - Store `appThemeMode` string key (`"LIGHT"`, `"DARK"`, `"SYSTEM"`) in `ReadingStateRepository` via `SharedPreferences`.
  - Default value is `"LIGHT"`.
- **UI Interaction**:
  - Add "Theme Settings" item in the top bar "More Options" menu in `LibraryScreen.kt`.
  - Show a `ThemeSettingsDialog` with selectable radio options for Light, Dark, and Follow System.
  - Callback updates `LibraryViewModel` and `ReadingStateRepository`, propagating the change to `MainActivity` so the app re-themes instantly.

## Verification Plan
1. **App Build**: Compile the project with `./gradlew assembleDebug` to verify no compilation errors.
2. **Theme Testing**: Verify Light mode (default), Dark mode, and System mode rendering.
3. **Logo Verification**: Verify `ic_app_logo` displays clearly in the top app bar and launcher icon build.
