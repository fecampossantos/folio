package com.example.epubreader.data.model

import kotlinx.serialization.Serializable

/**
 * Root data structure for reading history stored in JSON file.
 *
 * @property version Schema version number.
 * @property lastUpdated ISO timestamp string when history was last saved.
 * @property selectedFolderUri Saved SAF folder URI string selected by user.
 * @property books Map or list of tracked books and their saved reading states.
 */
@Serializable
data class ReadingHistory(
    val version: Int = 1,
    val lastUpdated: String = "",
    val selectedFolderUri: String? = null,
    val books: List<BookState> = emptyList()
)

/**
 * Data structure representing a user bookmark saved in a book.
 *
 * @property id Unique bookmark identifier.
 * @property chapterIndex Chapter index where bookmark was created.
 * @property chapterTitle Title of chapter bookmarked.
 * @property snippet Text snippet preview of bookmarked location.
 * @property createdTimestamp Timestamp when bookmark was created.
 */
@Serializable
data class Bookmark(
    val id: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val snippet: String = "",
    val createdTimestamp: Long = System.currentTimeMillis()
)

/**
 * Data structure representing saved state for a single EPUB book.
 *
 * @property uriString Unique content URI string identifying the EPUB file.
 * @property fileName Name of the EPUB file.
 * @property title Extracted or fallback title of the book.
 * @property author Extracted or fallback author of the book.
 * @property coverImagePath Absolute local path string to saved cover image file.
 * @property lastOpenedTimestamp Epoch timestamp in millis when book was last opened.
 * @property currentChapterIndex Zero-based index of last read chapter.
 * @property currentPageIndex Zero-based index of last read page within chapter.
 * @property totalChapters Total count of chapters in the EPUB.
 * @property progressPercentage Calculated reading completion percentage (0 to 100).
 * @property readerThemeMode Reader theme palette name ("LIGHT", "DARK", "SEPIA").
 * @property fontSizeSp Reader font size setting in SP.
 * @property bookmarks List of saved user bookmarks for this book.
 * @property totalReadingTimeSeconds Accumulated reading time in seconds.
 * @property isCompleted True if user has reached the end of the book.
 */
@Serializable
data class BookState(
    val uriString: String,
    val fileName: String,
    val title: String = "",
    val author: String = "",
    val coverImagePath: String? = null,
    val lastOpenedTimestamp: Long = 0L,
    val currentChapterIndex: Int = 0,
    val currentPageIndex: Int = 0,
    val totalChapters: Int = 1,
    val progressPercentage: Float = 0f,
    val readerThemeMode: String = "LIGHT",
    val fontSizeSp: Int = 18,
    val bookmarks: List<Bookmark> = emptyList(),
    val totalReadingTimeSeconds: Long = 0L,
    val isCompleted: Boolean = false
)
