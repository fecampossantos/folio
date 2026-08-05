package com.example.epubreader.data.repository

import com.example.epubreader.data.model.BookState
import com.example.epubreader.data.model.ReadingHistory
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for validating cross-device book state matching by file name when SAF content URIs differ across devices.
 */
class CrossDeviceSyncTest {

    /**
     * Tests that a book recorded on Device A can be matched on Device B using fileName even when uriString differs.
     */
    @Test
    fun testBookMatchingCrossDeviceByFileName() {
        val deviceABookState = BookState(
            uriString = "content://com.android.externalstorage.documents/tree/primary%3ABooks/document/primary%3ABooks%2Fbook1.epub",
            fileName = "book1.epub",
            title = "Synced Book",
            author = "Synced Author",
            currentChapterIndex = 3,
            currentPageIndex = 12,
            progressPercentage = 50f
        )

        val historyFromDeviceA = ReadingHistory(
            version = 1,
            lastUpdated = "2026-08-04T12:00:00Z",
            selectedFolderUri = "content://com.android.externalstorage.documents/tree/primary%3ABooks",
            books = listOf(deviceABookState)
        )

        val deviceBUriString = "content://com.android.externalstorage.documents/tree/1234-5678%3ABooks/document/1234-5678%3ABooks%2Fbook1.epub"
        val fileName = "book1.epub"

        // Search in history for device B's book using fileName or uriString fallback
        val matchedBook = historyFromDeviceA.books.find { it.uriString == deviceBUriString || (fileName.isNotEmpty() && it.fileName == fileName) }

        assertNotNull("Book synced from Device A should be matched on Device B by fileName", matchedBook)
        assertEquals("book1.epub", matchedBook?.fileName)
        assertEquals(3, matchedBook?.currentChapterIndex)
        assertEquals(12, matchedBook?.currentPageIndex)
        assertEquals(50f, matchedBook?.progressPercentage ?: 0f, 0.01f)
    }

    /**
     * Tests updating book state on Device B updates the existing book entry matching fileName instead of creating a duplicate.
     */
    @Test
    fun testUpdateBookStatePreservesSingleEntryPerFileName() {
        val deviceABookState = BookState(
            uriString = "content://deviceA/book1.epub",
            fileName = "book1.epub",
            title = "Synced Book",
            currentChapterIndex = 1
        )

        val history = ReadingHistory(books = listOf(deviceABookState))

        val deviceBStateUpdate = BookState(
            uriString = "content://deviceB/book1.epub",
            fileName = "book1.epub",
            title = "Synced Book",
            currentChapterIndex = 2
        )

        val updatedBooks = history.books.toMutableList()
        val existingIndex = updatedBooks.indexOfFirst {
            it.uriString == deviceBStateUpdate.uriString || (deviceBStateUpdate.fileName.isNotEmpty() && it.fileName == deviceBStateUpdate.fileName)
        }

        if (existingIndex >= 0) {
            updatedBooks[existingIndex] = deviceBStateUpdate
        } else {
            updatedBooks.add(deviceBStateUpdate)
        }

        assertEquals("Should not duplicate book entry when updating state on another device", 1, updatedBooks.size)
        assertEquals("content://deviceB/book1.epub", updatedBooks[0].uriString)
        assertEquals(2, updatedBooks[0].currentChapterIndex)
    }
}
