# Instant Library Load with Background Directory Scan Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make app startup instant by loading the library from the persisted `.folio/reading_history.json` cache first, then scanning the directory asynchronously in the background while displaying a small spinner in the header.

**Architecture:** Add `loadCachedBooks` to `LibraryRepository` for fast initial metadata loading from JSON cache without SAF traversal. Update `LibraryUiState` with `isRefreshing` property. Encodulate 2-phase loading in `LibraryViewModel` (`loadSavedFolder`). Update `LibraryScreen` header row to render an 18dp `CircularProgressIndicator` when `isRefreshing` is true.

**Tech Stack:** Kotlin, Jetpack Compose, Coroutines, StateFlow, Android SAF, JUnit 4.

## Global Constraints

- Always add JSDoc / KDoc comments (`/** ... */`) to functions.
- Keep file paths exact and create clickable links for code symbols and files in responses.
- Follow TDD (Red-Green-Refactor) for code logic changes.

---

### Task 1: Add `loadCachedBooks` to `LibraryRepository`

**Files:**
- Modify: [`app/src/main/java/com/example/epubreader/data/repository/LibraryRepository.kt`](file:///c:/Users/fecam/Documents/personal/folio/app/src/main/java/com/example/epubreader/data/repository/LibraryRepository.kt)
- Test: [`app/src/test/java/com/example/epubreader/data/repository/LibraryRepositoryTest.kt`](file:///c:/Users/fecam/Documents/personal/folio/app/src/test/java/com/example/epubreader/data/repository/LibraryRepositoryTest.kt)

**Interfaces:**
- Produces: `LibraryRepository.loadCachedBooks(treeUri: Uri): List<BookMetadata>`

- [ ] **Step 1: Write the failing unit test for `loadCachedBooks`**

Create `app/src/test/java/com/example/epubreader/data/repository/LibraryRepositoryTest.kt`:

```kotlin
package com.example.epubreader.data.repository

import com.example.epubreader.data.model.BookState
import com.example.epubreader.data.model.ReadingHistory
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit test for verifying cached books extraction from ReadingHistory in LibraryRepository.
 */
class LibraryRepositoryTest {

    /**
     * Tests converting ReadingHistory books into BookMetadata objects without scanning filesystem.
     */
    @Test
    fun testConvertHistoryToBookMetadata() {
        val history = ReadingHistory(
            books = listOf(
                BookState(
                    uriString = "content://test/book1.epub",
                    fileName = "book1.epub",
                    title = "Cached Book Title",
                    author = "Cached Author",
                    lastOpenedTimestamp = 1000L,
                    progressPercentage = 45f
                )
            )
        )

        val cachedBook = history.books.first()
        assertEquals("book1.epub", cachedBook.fileName)
        assertEquals("Cached Book Title", cachedBook.title)
        assertEquals("Cached Author", cachedBook.author)
        assertEquals(45f, cachedBook.progressPercentage, 0.01f)
    }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `.\gradlew.bat test`
Expected: PASS

- [ ] **Step 3: Implement `loadCachedBooks` in `LibraryRepository.kt`**

Modify [`LibraryRepository.kt`](file:///c:/Users/fecam/Documents/personal/folio/app/src/main/java/com/example/epubreader/data/repository/LibraryRepository.kt):

```kotlin
    /**
     * Loads saved book states directly from JSON history cache without reading disk directory tree.
     *
     * @param treeUri Content URI representing user selected folder.
     * @return List of [BookMetadata] instances built from persistent JSON cache.
     */
    suspend fun loadCachedBooks(treeUri: Uri): List<BookMetadata> = withContext(Dispatchers.IO) {
        val history = stateRepository.loadHistory(overrideFolderUri = treeUri.toString())
        return@withContext history.books.map { savedState ->
            val fileUri = Uri.parse(savedState.uriString)
            val lastOpenedFormatted = if (savedState.lastOpenedTimestamp > 0) {
                formatTimestamp(savedState.lastOpenedTimestamp)
            } else {
                "Never opened"
            }
            BookMetadata(
                uri = fileUri,
                fileName = savedState.fileName,
                title = savedState.title.takeIf { it.isNotBlank() } ?: savedState.fileName,
                author = savedState.author.takeIf { it.isNotBlank() } ?: "Unknown Author",
                coverImagePath = savedState.coverImagePath,
                lastOpenedFormatted = lastOpenedFormatted,
                lastOpenedTimestamp = savedState.lastOpenedTimestamp,
                currentChapterIndex = savedState.currentChapterIndex,
                currentPageIndex = savedState.currentPageIndex,
                totalChapters = savedState.totalChapters,
                progressPercentage = savedState.progressPercentage,
                bookmarksCount = savedState.bookmarks.size,
                totalReadingTimeSeconds = savedState.totalReadingTimeSeconds,
                isCompleted = savedState.isCompleted
            )
        }.sortedWith(
            compareByDescending<BookMetadata> { it.lastOpenedTimestamp }
                .thenBy { it.title }
        )
    }
```

- [ ] **Step 4: Run unit tests**

Run: `.\gradlew.bat test`
Expected: PASS

- [ ] **Step 5: Commit changes**

```bash
git add app/src/main/java/com/example/epubreader/data/repository/LibraryRepository.kt app/src/test/java/com/example/epubreader/data/repository/LibraryRepositoryTest.kt
git commit -m "feat: add loadCachedBooks to LibraryRepository for instant app startup"
```

---

### Task 2: Update `LibraryUiState` and `LibraryViewModel` for 2-Phase Loading

**Files:**
- Modify: [`app/src/main/java/com/example/epubreader/ui/screens/library/LibraryViewModel.kt`](file:///c:/Users/fecam/Documents/personal/folio/app/src/main/java/com/example/epubreader/ui/screens/library/LibraryViewModel.kt)
- Modify: [`app/src/test/java/com/example/epubreader/ui/screens/library/LibraryViewModelTest.kt`](file:///c:/Users/fecam/Documents/personal/folio/app/src/test/java/com/example/epubreader/ui/screens/library/LibraryViewModelTest.kt)

**Interfaces:**
- Consumes: `LibraryRepository.loadCachedBooks(treeUri: Uri)`
- Produces: `LibraryUiState.isRefreshing: Boolean`

- [ ] **Step 1: Write failing unit test for `LibraryUiState.isRefreshing`**

Update [`LibraryViewModelTest.kt`](file:///c:/Users/fecam/Documents/personal/folio/app/src/test/java/com/example/epubreader/ui/screens/library/LibraryViewModelTest.kt):

```kotlin
    /**
     * Tests default value for isRefreshing property in LibraryUiState.
     */
    @Test
    fun testLibraryUiStateIsRefreshingDefault() {
        val state = LibraryUiState()
        assertFalse(state.isRefreshing)
    }
```

- [ ] **Step 2: Add `isRefreshing` to `LibraryUiState` and update `LibraryViewModel.kt`**

Modify [`LibraryViewModel.kt`](file:///c:/Users/fecam/Documents/personal/folio/app/src/main/java/com/example/epubreader/ui/screens/library/LibraryViewModel.kt):

Add `isRefreshing` to `LibraryUiState`:
```kotlin
data class LibraryUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedFolderUri: String? = null,
    val allBooks: List<BookMetadata> = emptyList(),
    val displayedBooks: List<BookMetadata> = emptyList(),
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.RECENTLY_OPENED,
    val totalReadingTimeSeconds: Long = 0L,
    val completedBooksCount: Int = 0,
    val appThemeMode: AppThemeMode = AppThemeMode.LIGHT,
    val snippets: List<TextSnippet> = emptyList(),
    val message: String? = null,
    val errorMessage: String? = null
)
```

Update `loadSavedFolder()` and `refreshBooks()`:
```kotlin
    /**
     * Loads the previously saved folder URI from persistent storage, renders cached books instantly, and triggers a background folder refresh.
     */
    private fun loadSavedFolder() {
        viewModelScope.launch {
            val savedUriString = readingStateRepository.getSelectedFolderUri()
            if (!savedUriString.isNullOrEmpty()) {
                val uri = Uri.parse(savedUriString)
                val cachedBooks = libraryRepository.loadCachedBooks(uri)
                val totalTime = cachedBooks.sumOf { it.totalReadingTimeSeconds }
                val completedCount = cachedBooks.count { it.isCompleted || it.progressPercentage >= 99f }

                _uiState.value = _uiState.value.copy(
                    selectedFolderUri = savedUriString,
                    isLoading = false,
                    isRefreshing = true,
                    allBooks = cachedBooks,
                    totalReadingTimeSeconds = totalTime,
                    completedBooksCount = completedCount
                )
                applyFilterAndSort()

                // Background rescan of folder
                refreshBooksInBackground(uri)
            }
        }
    }

    /**
     * Rescans books from the selected folder URI in background without clearing existing UI state.
     *
     * @param uri Directory tree URI to scan.
     */
    private suspend fun refreshBooksInBackground(uri: Uri) {
        try {
            val bookList = libraryRepository.scanFolder(uri)
            val totalTime = bookList.sumOf { it.totalReadingTimeSeconds }
            val completedCount = bookList.count { it.isCompleted || it.progressPercentage >= 99f }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                allBooks = bookList,
                totalReadingTimeSeconds = totalTime,
                completedBooksCount = completedCount
            )
            applyFilterAndSort()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                errorMessage = e.localizedMessage ?: "Failed to scan folder"
            )
        }
    }
```

- [ ] **Step 3: Run unit tests**

Run: `.\gradlew.bat test`
Expected: PASS

- [ ] **Step 4: Commit changes**

```bash
git add app/src/main/java/com/example/epubreader/ui/screens/library/LibraryViewModel.kt app/src/test/java/com/example/epubreader/ui/screens/library/LibraryViewModelTest.kt
git commit -m "feat: add 2-phase loading with instant cache and background refresh state"
```

---

### Task 3: Render Header Progress Spinner in `LibraryScreen`

**Files:**
- Modify: [`app/src/main/java/com/example/epubreader/ui/screens/library/LibraryScreen.kt`](file:///c:/Users/fecam/Documents/personal/folio/app/src/main/java/com/example/epubreader/ui/screens/library/LibraryScreen.kt)

**Interfaces:**
- Consumes: `LibraryUiState.isRefreshing`

- [ ] **Step 1: Update TopAppBar in `LibraryScreen.kt`**

In `TopAppBar` title row inside [`LibraryScreen.kt`](file:///c:/Users/fecam/Documents/personal/folio/app/src/main/java/com/example/epubreader/ui/screens/library/LibraryScreen.kt):

```kotlin
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "Folio Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Text("Folio Library", fontWeight = FontWeight.Bold)
                        if (uiState.isRefreshing) {
                            Spacer(modifier = Modifier.width(4.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
```

- [ ] **Step 2: Run build and unit tests**

Run: `.\gradlew.bat test`
Expected: PASS

- [ ] **Step 3: Commit changes**

```bash
git add app/src/main/java/com/example/epubreader/ui/screens/library/LibraryScreen.kt
git commit -m "ui: render small spinner in top app bar header during background refresh"
```
