package com.example.epubreader.data.repository

import android.content.Context
import android.net.Uri
import com.example.epubreader.data.model.BookState
import com.example.epubreader.data.model.ReadingHistory
import com.example.epubreader.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository responsible for persisting, loading, exporting, and importing reading history and folder selection from a JSON file.
 *
 * @param context Android Application Context used to locate internal files directory.
 */
class ReadingStateRepository(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences("folio_app_prefs", Context.MODE_PRIVATE)
    }

    /**
     * Gets the saved app theme mode selection. Defaults to LIGHT theme.
     *
     * @return Stored [AppThemeMode] instance.
     */
    fun getAppThemeMode(): AppThemeMode {
        val name = prefs.getString("app_theme_mode", AppThemeMode.LIGHT.name)
        return try {
            AppThemeMode.valueOf(name ?: AppThemeMode.LIGHT.name)
        } catch (e: Exception) {
            AppThemeMode.LIGHT
        }
    }

    /**
     * Saves the user selected app theme mode.
     *
     * @param mode Target [AppThemeMode] instance to store.
     */
    fun saveAppThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("app_theme_mode", mode.name).apply()
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val historyFile: File
        get() = File(context.filesDir, "reading_history.json")

    /**
     * Loads the stored reading history from the JSON file.
     *
     * @return The parsed [ReadingHistory] instance or a default empty structure if file does not exist.
     */
    suspend fun loadHistory(): ReadingHistory = withContext(Dispatchers.IO) {
        try {
            if (!historyFile.exists()) {
                return@withContext ReadingHistory()
            }
            val content = historyFile.readText()
            if (content.isBlank()) {
                return@withContext ReadingHistory()
            }
            json.decodeFromString<ReadingHistory>(content)
        } catch (e: Exception) {
            e.printStackTrace()
            ReadingHistory()
        }
    }

    /**
     * Saves the selected folder URI string into the persistent JSON history.
     *
     * @param folderUriString SAF directory tree URI string.
     */
    suspend fun saveSelectedFolderUri(folderUriString: String) = withContext(Dispatchers.IO) {
        try {
            val currentHistory = loadHistory()
            val newHistory = currentHistory.copy(selectedFolderUri = folderUriString)
            historyFile.writeText(json.encodeToString(newHistory))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Retrieves the saved selected folder URI string from the JSON history.
     *
     * @return Folder URI string or null if none saved.
     */
    suspend fun getSelectedFolderUri(): String? = withContext(Dispatchers.IO) {
        val history = loadHistory()
        return@withContext history.selectedFolderUri
    }

    /**
     * Retrieves the saved book state for a specific EPUB URI string.
     *
     * @param uriString Unique URI identifier for the EPUB file.
     * @return The [BookState] matching the URI, or null if not found.
     */
    suspend fun getBookState(uriString: String): BookState? = withContext(Dispatchers.IO) {
        val history = loadHistory()
        return@withContext history.books.find { it.uriString == uriString }
    }

    /**
     * Updates the custom or extracted cover image file path for a book in JSON history.
     *
     * @param uriString Unique EPUB URI string.
     * @param coverPath Absolute file path to cover image.
     */
    suspend fun updateBookCoverPath(uriString: String, coverPath: String) = withContext(Dispatchers.IO) {
        val existing = getBookState(uriString)
        if (existing != null) {
            saveBookState(existing.copy(coverImagePath = coverPath))
        } else {
            saveBookState(BookState(uriString = uriString, fileName = "", coverImagePath = coverPath))
        }
    }

    /**
     * Saves or updates the reading state of an EPUB book in the JSON file.
     *
     * @param state The [BookState] containing updated reading position and timestamp.
     */
    suspend fun saveBookState(state: BookState) = withContext(Dispatchers.IO) {
        try {
            val currentHistory = loadHistory()
            val updatedBooks = currentHistory.books.toMutableList()
            val existingIndex = updatedBooks.indexOfFirst { it.uriString == state.uriString }

            if (existingIndex >= 0) {
                val currentCover = updatedBooks[existingIndex].coverImagePath
                updatedBooks[existingIndex] = if (state.coverImagePath == null && currentCover != null) {
                    state.copy(coverImagePath = currentCover)
                } else {
                    state
                }
            } else {
                updatedBooks.add(state)
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val newHistory = currentHistory.copy(
                lastUpdated = timestamp,
                books = updatedBooks
            )

            val jsonString = json.encodeToString(newHistory)
            historyFile.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Updates only the last opened timestamp for a given EPUB book in the JSON history.
     *
     * @param uriString Unique URI string of the EPUB file.
     * @param fileName File name of the EPUB file.
     * @param title Title of the book.
     * @param author Author of the book.
     */
    suspend fun recordBookOpened(
        uriString: String,
        fileName: String,
        title: String,
        author: String
    ) = withContext(Dispatchers.IO) {
        val existing = getBookState(uriString)
        val now = System.currentTimeMillis()
        val newState = existing?.copy(
            lastOpenedTimestamp = now,
            title = if (title.isNotBlank()) title else existing.title,
            author = if (author.isNotBlank()) author else existing.author
        ) ?: BookState(
            uriString = uriString,
            fileName = fileName,
            title = title,
            author = author,
            lastOpenedTimestamp = now
        )
        saveBookState(newState)
    }

    /**
     * Exports current reading history JSON to a user-selected destination URI.
     *
     * @param targetUri Content URI where export JSON should be written.
     * @return True if export succeeded, false otherwise.
     */
    suspend fun exportHistoryToUri(targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val history = loadHistory()
            val jsonString = json.encodeToString(history)
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                output.write(jsonString.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Imports reading history JSON from a user-selected source URI and merges with current state.
     *
     * @param sourceUri Content URI of imported JSON backup.
     * @return True if import succeeded, false otherwise.
     */
    suspend fun importHistoryFromUri(sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(sourceUri)?.bufferedReader()?.use { it.readText() }
                ?: return@withContext false

            val importedHistory = json.decodeFromString<ReadingHistory>(content)
            val currentHistory = loadHistory()

            val mergedBooksMap = currentHistory.books.associateBy { it.uriString }.toMutableMap()
            for (importedBook in importedHistory.books) {
                val existing = mergedBooksMap[importedBook.uriString]
                if (existing == null || importedBook.lastOpenedTimestamp > existing.lastOpenedTimestamp) {
                    mergedBooksMap[importedBook.uriString] = importedBook
                }
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val mergedHistory = ReadingHistory(
                version = 1,
                lastUpdated = timestamp,
                selectedFolderUri = importedHistory.selectedFolderUri ?: currentHistory.selectedFolderUri,
                books = mergedBooksMap.values.toList()
            )

            val jsonString = json.encodeToString(mergedHistory)
            historyFile.writeText(jsonString)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
