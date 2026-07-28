package com.example.epubreader.ui.screens.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.epubreader.data.model.BookMetadata
import com.example.epubreader.data.model.TextSnippet
import com.example.epubreader.data.repository.LibraryRepository
import com.example.epubreader.data.repository.ReadingStateRepository
import com.example.epubreader.ui.theme.AppThemeMode
import com.example.epubreader.util.CoverManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Options for sorting the library book list.
 */
enum class SortOption(val label: String) {
    RECENTLY_OPENED("Recently Opened"),
    PROGRESS("Reading Progress"),
    TITLE("Title (A-Z)"),
    AUTHOR("Author (A-Z)")
}

/**
 * UI State for the Library screen.
 *
 * @property isLoading True if folder is currently being scanned.
 * @property selectedFolderUri Currently selected SAF folder URI string.
 * @property allBooks List of all discovered EPUB books.
 * @property displayedBooks Filtered and sorted list of books to display.
 * @property searchQuery Active search text filter.
 * @property sortOption Active sort criteria.
 * @property totalReadingTimeSeconds Total accumulated reading time across all books.
 * @property completedBooksCount Count of books completed (100% read).
 * @property appThemeMode Active app theme mode (LIGHT, DARK, SYSTEM).
 * @property message Toast/Snackbar message notification.
 * @property errorMessage Error message if scan failed.
 */
data class LibraryUiState(
    val isLoading: Boolean = false,
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

/**
 * ViewModel managing state, search filtering, sorting, backup export/import, and statistics for the Library screen.
 *
 * @param libraryRepository Repository scanning EPUB files in directory tree.
 * @param readingStateRepository Repository recording reading history.
 */
class LibraryViewModel(
    private val libraryRepository: LibraryRepository,
    private val readingStateRepository: ReadingStateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadSavedTheme()
        loadSavedFolder()
        loadSnippets()
    }

    /**
     * Loads the saved app theme selection from persistent repository storage.
     */
    private fun loadSavedTheme() {
        val themeMode = readingStateRepository.getAppThemeMode()
        _uiState.value = _uiState.value.copy(appThemeMode = themeMode)
    }

    /**
     * Updates the active app theme selection and persists choice in repository storage.
     *
     * @param mode Target [AppThemeMode] instance.
     */
    fun onAppThemeChanged(mode: AppThemeMode) {
        _uiState.value = _uiState.value.copy(appThemeMode = mode)
        readingStateRepository.saveAppThemeMode(mode)
    }

    /**
     * Loads the previously saved folder URI from persistent storage and scans it automatically on startup.
     */
    private fun loadSavedFolder() {
        viewModelScope.launch {
            val savedUriString = readingStateRepository.getSelectedFolderUri()
            if (!savedUriString.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(
                    selectedFolderUri = savedUriString,
                    isLoading = true
                )
                try {
                    val uri = Uri.parse(savedUriString)
                    refreshBooks(uri)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
        }
    }

    /**
     * Called when user picks a folder URI from SAF folder picker.
     *
     * @param uri SAF directory tree URI selected by user.
     */
    fun onFolderSelected(uri: Uri) {
        val uriString = uri.toString()
        _uiState.value = _uiState.value.copy(
            selectedFolderUri = uriString,
            isLoading = true,
            errorMessage = null
        )
        viewModelScope.launch {
            readingStateRepository.saveSelectedFolderUri(uriString)
        }
        refreshBooks(uri)
    }

    /**
     * Saves a user-selected custom cover image for a book and updates stored JSON state.
     *
     * @param context Application context.
     * @param bookUri Content URI of the EPUB file.
     * @param imageUri Content URI of user-selected cover image.
     */
    fun onCustomCoverSelected(context: Context, bookUri: Uri, imageUri: Uri) {
        viewModelScope.launch {
            val coverPath = CoverManager.saveCustomCover(context, bookUri, imageUri)
            if (coverPath != null) {
                readingStateRepository.updateBookCoverPath(bookUri.toString(), coverPath)
                _uiState.value.selectedFolderUri?.let { refreshBooks(Uri.parse(it)) }
            }
        }
    }

    /**
     * Rescans books from the selected folder URI.
     *
     * @param uri Directory tree URI to scan.
     */
    fun refreshBooks(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val bookList = libraryRepository.scanFolder(uri)
                val totalTime = bookList.sumOf { it.totalReadingTimeSeconds }
                val completedCount = bookList.count { it.isCompleted || it.progressPercentage >= 99f }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    allBooks = bookList,
                    totalReadingTimeSeconds = totalTime,
                    completedBooksCount = completedCount
                )
                applyFilterAndSort()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to scan folder"
                )
            }
        }
    }

    /**
     * Exports current reading history JSON to user-selected file URI.
     *
     * @param targetUri Content URI where JSON backup should be written.
     */
    fun exportBackup(targetUri: Uri) {
        viewModelScope.launch {
            val success = readingStateRepository.exportHistoryToUri(targetUri)
            _uiState.value = _uiState.value.copy(
                message = if (success) "Backup exported successfully!" else "Failed to export backup."
            )
        }
    }

    /**
     * Imports reading history JSON from user-selected file URI.
     *
     * @param sourceUri Content URI of JSON backup to restore.
     */
    fun importBackup(sourceUri: Uri) {
        viewModelScope.launch {
            val success = readingStateRepository.importHistoryFromUri(sourceUri)
            if (success) {
                _uiState.value = _uiState.value.copy(message = "Backup imported successfully!")
                val savedUri = readingStateRepository.getSelectedFolderUri() ?: _uiState.value.selectedFolderUri
                if (!savedUri.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(selectedFolderUri = savedUri)
                    refreshBooks(Uri.parse(savedUri))
                }
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to import backup file.")
            }
        }
    }

    /**
     * Clears user feedback messages from state.
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, errorMessage = null)
    }

    /**
     * Loads snippets from reading history.
     */
    fun loadSnippets() {
        viewModelScope.launch {
            val history = readingStateRepository.loadHistory()
            _uiState.value = _uiState.value.copy(snippets = history.snippets)
        }
    }

    /**
     * Deletes a snippet.
     */
    fun deleteSnippet(id: String) {
        viewModelScope.launch {
            readingStateRepository.deleteSnippet(id)
            loadSnippets()
        }
    }

    /**
     * Updates a snippet note.
     */
    fun updateSnippetNote(id: String, note: String) {
        viewModelScope.launch {
            readingStateRepository.updateSnippetNote(id, note)
            loadSnippets()
        }
    }

    /**
     * Updates search query string and reapplies filtering and sorting.
     *
     * @param query New search query text.
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilterAndSort()
    }

    /**
     * Updates active sorting option and reapplies ordering.
     *
     * @param option Target sort option.
     */
    fun onSortOptionChanged(option: SortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
        applyFilterAndSort()
    }

    /**
     * Filters and sorts the book list based on current query and sort option.
     */
    private fun applyFilterAndSort() {
        val currentState = _uiState.value
        val query = currentState.searchQuery.trim().lowercase()

        val filtered = if (query.isEmpty()) {
            currentState.allBooks
        } else {
            currentState.allBooks.filter { book ->
                book.title.lowercase().contains(query) ||
                        book.author.lowercase().contains(query) ||
                        book.fileName.lowercase().contains(query)
            }
        }

        val sorted = when (currentState.sortOption) {
            SortOption.RECENTLY_OPENED -> filtered.sortedByDescending { it.lastOpenedTimestamp }
            SortOption.PROGRESS -> filtered.sortedByDescending { it.progressPercentage }
            SortOption.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortOption.AUTHOR -> filtered.sortedBy { it.author.lowercase() }
        }

        _uiState.value = _uiState.value.copy(displayedBooks = sorted)
    }

    /**
     * Records that a book was opened by user and updates timestamp in stored JSON history.
     *
     * @param book Metadata of the opened book.
     */
    fun onBookOpened(book: BookMetadata) {
        viewModelScope.launch {
            readingStateRepository.recordBookOpened(
                uriString = book.uri.toString(),
                fileName = book.fileName,
                title = book.title,
                author = book.author
            )
        }
    }
}
