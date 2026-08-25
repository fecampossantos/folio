package com.example.epubreader.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.epubreader.data.model.BookState
import com.example.epubreader.data.model.ReadingHistory
import com.example.epubreader.data.model.TextSnippet
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

    /**
     * Gets the saved Hardcover API token.
     *
     * @return Hardcover API token string, or null if not set.
     */
    fun getHardcoverToken(): String? {
        return prefs.getString("hardcover_token", null)
    }

    /**
     * Saves the Hardcover API token.
     *
     * @param token Hardcover API token string to store.
     */
    fun saveHardcoverToken(token: String) {
        prefs.edit().putString("hardcover_token", token).apply()
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private fun getHistoryDocumentFile(overrideFolderUriString: String? = null): DocumentFile? {
        val folderUriString = overrideFolderUriString ?: prefs.getString("selected_folder_uri", null) ?: return null
        return try {
            val folderUri = Uri.parse(folderUriString)
            val rootDoc = DocumentFile.fromTreeUri(context, folderUri) ?: return null
            var folioDir = rootDoc.findFile(".folio")
            if (folioDir == null) {
                folioDir = rootDoc.createDirectory(".folio")
            }
            var historyFile = folioDir?.findFile("reading_history.json")
            if (historyFile == null) {
                historyFile = folioDir?.createFile("application/json", "reading_history.json")
            }
            historyFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Loads the stored reading history from the JSON file.
     *
     * @param overrideFolderUri Optional SAF folder URI string to override SharedPreferences lookup.
     * @return The parsed [ReadingHistory] instance or a default empty structure if file does not exist.
     */
    suspend fun loadHistory(overrideFolderUri: String? = null): ReadingHistory = withContext(Dispatchers.IO) {
        try {
            val docFile = getHistoryDocumentFile(overrideFolderUri)
            if (docFile == null || !docFile.exists()) {
                val savedUri = overrideFolderUri ?: prefs.getString("selected_folder_uri", null)
                return@withContext ReadingHistory(selectedFolderUri = savedUri)
            }
            context.contentResolver.openInputStream(docFile.uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().use { it.readText() }
                if (content.isBlank()) {
                    val savedUri = overrideFolderUri ?: prefs.getString("selected_folder_uri", null)
                    return@withContext ReadingHistory(selectedFolderUri = savedUri)
                }
                val history = json.decodeFromString<ReadingHistory>(content)
                val savedUri = overrideFolderUri ?: prefs.getString("selected_folder_uri", null)
                return@withContext history.copy(selectedFolderUri = savedUri)
            } ?: ReadingHistory(selectedFolderUri = overrideFolderUri ?: prefs.getString("selected_folder_uri", null))
        } catch (e: Exception) {
            e.printStackTrace()
            ReadingHistory(selectedFolderUri = overrideFolderUri ?: prefs.getString("selected_folder_uri", null))
        }
    }

    /**
     * Saves the selected folder URI string into the persistent JSON history.
     *
     * @param folderUriString SAF directory tree URI string.
     */
    suspend fun saveSelectedFolderUri(folderUriString: String) = withContext(Dispatchers.IO) {
        try {
            prefs.edit().putString("selected_folder_uri", folderUriString).commit()
            val currentHistory = loadHistory(overrideFolderUri = folderUriString)
            val newHistory = currentHistory.copy(selectedFolderUri = folderUriString)
            saveHistory(newHistory, overrideFolderUri = folderUriString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Saves reading history object to JSON file.
     *
     * @param history ReadingHistory instance to write.
     * @param overrideFolderUri Optional SAF folder URI string.
     */
    private suspend fun saveHistory(history: ReadingHistory, overrideFolderUri: String? = null) = withContext(Dispatchers.IO) {
        val docFile = getHistoryDocumentFile(overrideFolderUri)
        if (docFile != null && docFile.exists()) {
            context.contentResolver.openOutputStream(docFile.uri, "wt")?.use { outputStream ->
                outputStream.bufferedWriter().use { it.write(json.encodeToString(history)) }
            }
        }
    }

    /**
     * Retrieves the saved selected folder URI string from the JSON history.
     *
     * @return Folder URI string or null if none saved.
     */
    suspend fun getSelectedFolderUri(): String? = withContext(Dispatchers.IO) {
        return@withContext prefs.getString("selected_folder_uri", null)
    }

    /**
     * Retrieves the saved book state for a specific EPUB URI string or file name.
     *
     * @param uriString Unique URI identifier for the EPUB file.
     * @param fileName Optional EPUB file name for cross-device matching.
     * @return The [BookState] matching the URI or file name, or null if not found.
     */
    suspend fun getBookState(uriString: String, fileName: String? = null): BookState? = withContext(Dispatchers.IO) {
        val history = loadHistory()
        return@withContext history.books.find {
            it.uriString == uriString || (fileName != null && fileName.isNotEmpty() && it.fileName == fileName)
        }
    }

    /**
     * Updates the custom or extracted cover image file path for a book in JSON history.
     *
     * @param uriString Unique EPUB URI string.
     * @param coverPath Absolute file path to cover image.
     * @param fileName Optional file name of the EPUB book.
     */
    suspend fun updateBookCoverPath(uriString: String, coverPath: String, fileName: String = "") = withContext(Dispatchers.IO) {
        val existing = getBookState(uriString, fileName)
        if (existing != null) {
            saveBookState(existing.copy(uriString = uriString, coverImagePath = coverPath))
        } else {
            saveBookState(BookState(uriString = uriString, fileName = fileName, coverImagePath = coverPath))
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
            val existingIndex = updatedBooks.indexOfFirst {
                it.uriString == state.uriString || (state.fileName.isNotEmpty() && it.fileName == state.fileName)
            }

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

            saveHistory(newHistory)
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
        val existing = getBookState(uriString, fileName)
        val now = System.currentTimeMillis()
        val newState = existing?.copy(
            uriString = uriString,
            fileName = if (fileName.isNotBlank()) fileName else existing.fileName,
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
                books = mergedBooksMap.values.toList(),
                snippets = (currentHistory.snippets + importedHistory.snippets).distinctBy { it.id }
            )

            saveHistory(mergedHistory)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun saveSnippet(snippet: TextSnippet) = withContext(Dispatchers.IO) {
        try {
            val currentHistory = loadHistory()
            val newSnippets = currentHistory.snippets + snippet
            val newHistory = currentHistory.copy(snippets = newSnippets)
            saveHistory(newHistory)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateSnippetNote(id: String, note: String) = withContext(Dispatchers.IO) {
        try {
            val currentHistory = loadHistory()
            val newSnippets = currentHistory.snippets.map {
                if (it.id == id) it.copy(note = note) else it
            }
            val newHistory = currentHistory.copy(snippets = newSnippets)
            saveHistory(newHistory)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteSnippet(id: String) = withContext(Dispatchers.IO) {
        try {
            val currentHistory = loadHistory()
            val newSnippets = currentHistory.snippets.filter { it.id != id }
            val newHistory = currentHistory.copy(snippets = newSnippets)
            saveHistory(newHistory)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
