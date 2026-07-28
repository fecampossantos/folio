package com.example.epubreader.data.model

import android.net.Uri

/**
 * Model representing an EPUB book for display in the library UI.
 *
 * @property uri Content URI pointing to the EPUB file.
 * @property fileName File name of the EPUB file.
 * @property title Display title of the book.
 * @property author Display author of the book.
 * @property coverImagePath Path string pointing to saved cover image file.
 * @property lastOpenedFormatted Formatted string indicating when book was last opened.
 * @property lastOpenedTimestamp Epoch timestamp when last opened.
 * @property currentChapterIndex Saved chapter index position.
 * @property currentPageIndex Saved page index position.
 * @property totalChapters Total chapter count.
 * @property progressPercentage Saved progress percentage.
 * @property bookmarksCount Total bookmarks saved for this book.
 * @property totalReadingTimeSeconds Total reading time accumulated in seconds.
 * @property isCompleted True if user finished reading book.
 */
data class BookMetadata(
    val uri: Uri,
    val fileName: String,
    val title: String,
    val author: String,
    val coverImagePath: String? = null,
    val lastOpenedFormatted: String = "Never",
    val lastOpenedTimestamp: Long = 0L,
    val currentChapterIndex: Int = 0,
    val currentPageIndex: Int = 0,
    val totalChapters: Int = 1,
    val progressPercentage: Float = 0f,
    val bookmarksCount: Int = 0,
    val totalReadingTimeSeconds: Long = 0L,
    val isCompleted: Boolean = false
)
