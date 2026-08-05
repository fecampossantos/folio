# Instant Library Load with Background Directory Scan

## Overview
Currently, on every app launch, Folio performs a full recursive Storage Access Framework (SAF) folder traversal to scan for `.epub` files before displaying anything on screen. This causes an unnecessary loading screen delay for the user.

This design introduces a two-phase loading mechanism:
1. **Instant Phase**: Immediately load and render the library from the persisted `.folio/reading_history.json` cache (0 ms delay).
2. **Background Refresh Phase**: Perform the SAF directory scan asynchronously in the background. Update the book list silently when changes are detected and display a small spinner in the header during scanning.

---

## Proposed Changes

### 1. `LibraryUiState`
Path: [`LibraryUiState.kt`](file:///c:/Users/fecam/Documents/personal/folio/app/src/main/java/com/example/epubreader/ui/screens/library/LibraryViewModel.kt)
- Add property `isRefreshing: Boolean = false` to track background directory scanning without triggering full screen loading state (`isLoading`).

```kotlin
data class LibraryUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedFolderUri: String? = null,
    val allBooks: List<BookMetadata> = emptyList(),
    val displayedBooks: List<BookMetadata> = emptyList(),
    // ...
)
```

### 2. `LibraryRepository`
Path: [`LibraryRepository.kt`](file:///c:/Users/fecam/Documents/personal/folio/app/src/main/java/com/example/epubreader/data/repository/LibraryRepository.kt)
- Add method `loadCachedBooks(treeUri: Uri): List<BookMetadata>`:
  - Reads `stateRepository.loadHistory(overrideFolderUri = treeUri.toString())`.
  - Converts each `BookState` in history into a `BookMetadata` instance using stored metadata (`title`, `author`, `coverImagePath`, `lastOpenedTimestamp`, `progressPercentage`, `totalReadingTimeSeconds`, etc.).
  - Returns cached list immediately without iterating SAF files.

### 3. `LibraryViewModel`
Path: [`LibraryViewModel.kt`](file:///c:/Users/fecam/Documents/personal/folio/app/src/main/java/com/example/epubreader/ui/screens/library/LibraryViewModel.kt)
- Update `loadSavedFolder()`:
  - Phase 1: Load cached books using `loadCachedBooks(uri)` and update `_uiState` with cached books immediately (`isLoading = false`).
  - Phase 2: Set `isRefreshing = true` and call `refreshBooks(uri)`.
  - When `refreshBooks(uri)` finishes scanning, update `_uiState` with the refreshed book list and set `isRefreshing = false`.

### 4. `LibraryScreen`
Path: [`LibraryScreen.kt`](file:///c:/Users/fecam/Documents/personal/folio/app/src/main/java/com/example/epubreader/ui/screens/library/LibraryScreen.kt)
- In `TopAppBar` title row:
  - If `uiState.isRefreshing == true`, render a small `CircularProgressIndicator` (size `18.dp`, strokeWidth `2.dp`) next to "Folio Library".
  - This informs the user that a background scan is active without blocking UI interaction.

---

## Verification Plan

### Automated Tests
- Add unit tests in `LibraryViewModelTest.kt` verifying:
  - Initial state populates cached books immediately.
  - `isRefreshing` transitions to `true` during background scan and returns to `false` on completion.
  - Newly discovered books are merged into state.

### Manual Verification
- Run `./gradlew.bat test` to verify all unit tests pass.
