package com.example.epubreader.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.epubreader.data.model.BookMetadata
import com.example.epubreader.util.CoverManager
import com.example.epubreader.util.EpubParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository for scanning and managing EPUB files from user-selected storage folders.
 *
 * @param context Android Application Context.
 * @param stateRepository Repository for accessing stored reading history JSON.
 */
class LibraryRepository(
    private val context: Context,
    private val stateRepository: ReadingStateRepository
) {

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


    /**
     * Scans the directory tree at the given SAF [treeUri] for `.epub` files.
     *
     * @param treeUri Content URI representing the user-selected folder.
     * @return List of discovered [BookMetadata] objects enriched with reading history.
     */
    suspend fun scanFolder(treeUri: Uri): List<BookMetadata> = withContext(Dispatchers.IO) {
        val rootDir = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val history = stateRepository.loadHistory(overrideFolderUri = treeUri.toString())

        val epubFiles = mutableListOf<DocumentFile>()
        findEpubFiles(rootDir, epubFiles)

        val result = mutableListOf<BookMetadata>()
        for (file in epubFiles) {
            val fileUri = file.uri
            val uriString = fileUri.toString()
            val fileName = file.name ?: "Unknown.epub"

            val savedState = history.books.find {
                it.uriString == uriString || (fileName.isNotEmpty() && it.fileName == fileName)
            }
            val (parsedTitle, parsedAuthor) = EpubParser.parseMetadata(context, fileUri, fileName)

            val title = savedState?.title?.takeIf { it.isNotBlank() } ?: parsedTitle
            val author = savedState?.author?.takeIf { it.isNotBlank() } ?: parsedAuthor

            // Cover image resolution (Saved custom cover -> Automatically extracted EPUB cover)
            var coverPath = savedState?.coverImagePath
            if (coverPath.isNullOrEmpty()) {
                coverPath = CoverManager.extractAndSaveEpubCover(context, fileUri)
                if (coverPath != null) {
                    stateRepository.updateBookCoverPath(uriString, coverPath, fileName)
                }
            }

            val lastOpenedFormatted = if (savedState != null && savedState.lastOpenedTimestamp > 0) {
                formatTimestamp(savedState.lastOpenedTimestamp)
            } else {
                "Never opened"
            }

            result.add(
                BookMetadata(
                    uri = fileUri,
                    fileName = fileName,
                    title = title,
                    author = author,
                    coverImagePath = coverPath,
                    lastOpenedFormatted = lastOpenedFormatted,
                    lastOpenedTimestamp = savedState?.lastOpenedTimestamp ?: 0L,
                    currentChapterIndex = savedState?.currentChapterIndex ?: 0,
                    currentPageIndex = savedState?.currentPageIndex ?: 0,
                    totalChapters = savedState?.totalChapters ?: 1,
                    progressPercentage = savedState?.progressPercentage ?: 0f,
                    bookmarksCount = savedState?.bookmarks?.size ?: 0,
                    totalReadingTimeSeconds = savedState?.totalReadingTimeSeconds ?: 0L,
                    isCompleted = savedState?.isCompleted ?: false
                )
            )
        }

        // Sort books: recently opened first, then alphabetically by title
        result.sortedWith(
            compareByDescending<BookMetadata> { it.lastOpenedTimestamp }
                .thenBy { it.title }
        )
    }

    /**
     * Recursively traverses document tree files looking for `.epub` extension.
     *
     * @param dir DocumentFile directory node.
     * @param outList List to accumulate found EPUB files.
     */
    private fun findEpubFiles(dir: DocumentFile, outList: MutableList<DocumentFile>) {
        if (!dir.canRead()) return
        val files = dir.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                findEpubFiles(file, outList)
            } else if (file.isFile && (file.name?.endsWith(".epub", ignoreCase = true) == true)) {
                outList.add(file)
            }
        }
    }

    /**
     * Formats an epoch timestamp in millis into a user-friendly date string.
     *
     * @param timestamp Epoch timestamp in milliseconds.
     * @return Formatted date string.
     */
    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
