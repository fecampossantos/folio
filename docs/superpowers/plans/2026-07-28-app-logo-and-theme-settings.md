# App Logo and Material 3 Theme Settings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the official app logo (`logo.png`) as the Android launcher icon and top app bar branding, and implement persistent Material 3 Light/Dark/System theme switching defaulting to Light theme.

**Architecture:** M3 color tokens in `Color.kt` / `Theme.kt` are driven by an `AppThemeMode` enum (`LIGHT`, `DARK`, `SYSTEM`). User choice is saved in `ReadingStateRepository` (SharedPreferences) and exposed via `LibraryViewModel` to `MainActivity`, which wraps the Compose UI in `EpubReaderTheme(appThemeMode)`. `logo.png` is placed in `res/drawable/ic_app_logo.png` and standard launcher mipmap directories.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Android SAF / SharedPreferences, Gradle.

## Global Constraints
- Android app logo image source: `docs/ui/logo.png`.
- Default theme: `LIGHT`.
- Theme options: `LIGHT`, `DARK`, `SYSTEM`.
- Maintain KDoc / JSDoc comments on all created/modified functions.

---

### Task 1: Add App Logo Assets to Android Resources

**Files:**
- Create: `app/src/main/res/drawable/ic_app_logo.png` (copy from `docs/ui/logo.png`)
- Modify: `app/src/main/AndroidManifest.xml` (ensure launcher icon references `@mipmap/ic_launcher`)

**Interfaces:**
- Consumes: `docs/ui/logo.png`
- Produces: `@drawable/ic_app_logo` resource for Compose UI and mipmap launcher icons.

- [ ] **Step 1: Copy `logo.png` to `res/drawable/ic_app_logo.png`**

Copy `docs/ui/logo.png` into `app/src/main/res/drawable/ic_app_logo.png`.

- [ ] **Step 2: Generate/Copy logo to launcher mipmap resource folders**

Copy `logo.png` to `app/src/main/res/mipmap-xxxhdpi/ic_launcher.png`, `ic_launcher_round.png`, `hdpi`, `mdpi`, `xhdpi`, `xxhdpi`.

- [ ] **Step 3: Verify AndroidManifest.xml uses `@mipmap/ic_launcher`**

Ensure `AndroidManifest.xml` retains `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"`.

- [ ] **Step 4: Commit assets**

```bash
git add app/src/main/res/
git commit -m "feat: add app logo drawable and launcher icons"
```

---

### Task 2: Implement Material 3 Sanctuary Light and Dark Color Schemes

**Files:**
- Modify: `app/src/main/java/com/example/epubreader/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/example/epubreader/ui/theme/Theme.kt`

**Interfaces:**
- Consumes: M3 design tokens from `DESIGN-light.md` and `DESIGN-dark.md`.
- Produces: `enum class AppThemeMode { LIGHT, DARK, SYSTEM }` and updated `EpubReaderTheme(appThemeMode: AppThemeMode, content: @Composable () -> Unit)`.

- [ ] **Step 1: Update `Color.kt` with Sanctuary palette colors**

```kotlin
package com.example.epubreader.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Tokens (DESIGN-light.md)
val LightPrimary = Color(0xFF475949)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFF5F7161)
val LightOnPrimaryContainer = Color(0xFFE1F5E1)
val LightSecondary = Color(0xFF625E56)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFE6DFD5)
val LightOnSecondaryContainer = Color(0xFF67625B)
val LightBackground = Color(0xFFFFF8F1)
val LightOnBackground = Color(0xFF1F1B12)
val LightSurface = Color(0xFFFFF8F1)
val LightOnSurface = Color(0xFF1F1B12)
val LightSurfaceVariant = Color(0xFFEAE1D2)
val LightOnSurfaceVariant = Color(0xFF434843)
val LightOutline = Color(0xFF737872)

// Dark Theme Tokens (DESIGN-dark.md)
val DarkPrimary = Color(0xFFC3E2BA)
val DarkOnPrimary = Color(0xFF1D361B)
val DarkPrimaryContainer = Color(0xFFA8C6A0)
val DarkOnPrimaryContainer = Color(0xFF395335)
val DarkSecondary = Color(0xFFC0C9C1)
val DarkOnSecondary = Color(0xFF2A322D)
val DarkSecondaryContainer = Color(0xFF404943)
val DarkOnSecondaryContainer = Color(0xFFAEB7B0)
val DarkBackground = Color(0xFF121412)
val DarkOnBackground = Color(0xFFE2E3DF)
val DarkSurface = Color(0xFF121412)
val DarkOnSurface = Color(0xFFE2E3DF)
val DarkSurfaceVariant = Color(0xFF333533)
val DarkOnSurfaceVariant = Color(0xFFC3C8BD)
val DarkOutline = Color(0xFF8D9289)

val SepiaBackground = Color(0xFFFBF0D9)
val SepiaText = Color(0xFF5F4B32)
```

- [ ] **Step 2: Update `Theme.kt` with `AppThemeMode` enum and ColorSchemes**

```kotlin
package com.example.epubreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * App-wide UI theme options.
 */
enum class AppThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("Follow System")
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

/**
 * Main application composable theme wrapper.
 *
 * @param appThemeMode Target theme selection mode (LIGHT, DARK, SYSTEM). Defaults to LIGHT.
 * @param content Composable content block to wrap.
 */
@Composable
fun EpubReaderTheme(
    appThemeMode: AppThemeMode = AppThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val isDark = when (appThemeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
```

- [ ] **Step 3: Commit theme updates**

```bash
git add app/src/main/java/com/example/epubreader/ui/theme/
git commit -m "feat: implement Material 3 Sanctuary theme palettes and AppThemeMode enum"
```

---

### Task 3: Persist App Theme Settings in ReadingStateRepository

**Files:**
- Modify: `app/src/main/java/com/example/epubreader/data/repository/ReadingStateRepository.kt`
- Modify: `app/src/main/java/com/example/epubreader/ui/screens/library/LibraryViewModel.kt`

**Interfaces:**
- Consumes: `AppThemeMode` enum.
- Produces: `getAppThemeMode(): AppThemeMode` and `saveAppThemeMode(mode: AppThemeMode)` in `ReadingStateRepository`, exposed in `LibraryUiState.appThemeMode`.

- [ ] **Step 1: Add app theme methods to `ReadingStateRepository.kt`**

Add `PREF_KEY_APP_THEME = "app_theme_mode"` to `ReadingStateRepository`.
Implement `getAppThemeMode()` returning `AppThemeMode.LIGHT` as default.
Implement `saveAppThemeMode(mode: AppThemeMode)`.

- [ ] **Step 2: Expose `appThemeMode` in `LibraryUiState` and `LibraryViewModel`**

Add `appThemeMode: AppThemeMode = AppThemeMode.LIGHT` to `LibraryUiState`.
Initialize `appThemeMode` from repository in `LibraryViewModel`.
Add `onAppThemeChanged(mode: AppThemeMode)` method in `LibraryViewModel` to save theme and update `uiState`.

- [ ] **Step 3: Commit state persistence logic**

```bash
git add app/src/main/java/com/example/epubreader/
git commit -m "feat: persist AppThemeMode preference in repository and ViewModel state"
```

---

### Task 4: Integrate Top Bar Logo and Theme Settings Dialog in UI

**Files:**
- Modify: `app/src/main/java/com/example/epubreader/ui/screens/library/LibraryScreen.kt`
- Modify: `app/src/main/java/com/example/epubreader/MainActivity.kt`

**Interfaces:**
- Consumes: `ic_app_logo` drawable resource, `AppThemeMode` enum, `LibraryViewModel.onAppThemeChanged`.
- Produces: Top app bar displaying app logo and "Folio Library", Theme Settings dialog in "More Options" menu, and dynamic root re-theming in `MainActivity`.

- [ ] **Step 1: Add App Logo and Theme Settings Dialog in `LibraryScreen.kt`**

Update `LibraryScreen` top app bar:
- Add `Image(painter = painterResource(id = R.drawable.ic_app_logo), contentDescription = "Folio Logo", modifier = Modifier.size(32.dp))` next to title "Folio Library".
- Add "Theme Settings" option in `DropdownMenu(expanded = showMoreMenu)` which opens `showThemeDialog = true`.
- Create `@Composable fun ThemeSettingsDialog` with radio options for `AppThemeMode.LIGHT`, `AppThemeMode.DARK`, `AppThemeMode.SYSTEM`.

- [ ] **Step 2: Wire theme state in `MainActivity.kt`**

Pass `libraryState.appThemeMode` into `EpubReaderTheme(appThemeMode = libraryState.appThemeMode)` inside `setContent` in `MainActivity.kt`.

- [ ] **Step 3: Build and test application**

Run: `gradlew.bat assembleDebug`
Verify build succeeds with zero errors.

- [ ] **Step 4: Commit UI integration**

```bash
git add app/src/main/java/com/example/epubreader/
git commit -m "feat: add top app bar logo and Theme Settings dialog"
```
