package com.example.epubreader.data.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests covering JSON serialization, deserialization, and data model state defaults for ReadingHistory.
 */
class ReadingHistoryTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    /**
     * Tests default values for [BookState] data structure.
     */
    @Test
    fun testBookStateDefaults() {
        val bookState = BookState(
            uriString = "content://com.android.externalstorage.documents/tree/book.epub",
            fileName = "book.epub",
            title = "Test Title",
            author = "Test Author"
        )

        assertEquals("content://com.android.externalstorage.documents/tree/book.epub", bookState.uriString)
        assertEquals("book.epub", bookState.fileName)
        assertEquals("Test Title", bookState.title)
        assertEquals("Test Author", bookState.author)
        assertEquals(0, bookState.currentChapterIndex)
        assertEquals(0, bookState.currentPageIndex)
        assertEquals("LIGHT", bookState.readerThemeMode)
        assertEquals(18, bookState.fontSizeSp)
        assertNull(bookState.coverImagePath)
        assertFalse(bookState.isCompleted)
    }

    /**
     * Tests JSON encoding and decoding of [ReadingHistory] object.
     */
    @Test
    fun testReadingHistoryJsonSerialization() {
        val bookmark = Bookmark(
            id = "bm-1",
            chapterIndex = 2,
            chapterTitle = "Chapter 3",
            snippet = "Some bookmarked snippet..."
        )

        val bookState = BookState(
            uriString = "content://epub/book1",
            fileName = "sample.epub",
            title = "Sample Book",
            author = "Jane Doe",
            coverImagePath = "/data/user/0/com.example.epubreader/files/covers/sample.jpg",
            currentChapterIndex = 2,
            currentPageIndex = 5,
            progressPercentage = 45f,
            bookmarks = listOf(bookmark)
        )

        val history = ReadingHistory(
            version = 1,
            lastUpdated = "2026-07-28T12:00:00Z",
            selectedFolderUri = "content://com.android.externalstorage.documents/tree/epubs",
            books = listOf(bookState)
        )

        val jsonString = json.encodeToString(history)
        assertTrue(jsonString.contains("sample.epub"))
        assertTrue(jsonString.contains("Jane Doe"))
        assertTrue(jsonString.contains("covers/sample.jpg"))

        val deserialized = json.decodeFromString<ReadingHistory>(jsonString)
        assertEquals(1, deserialized.version)
        assertEquals("content://com.android.externalstorage.documents/tree/epubs", deserialized.selectedFolderUri)
        assertEquals(1, deserialized.books.size)

        val restoredBook = deserialized.books[0]
        assertEquals("Sample Book", restoredBook.title)
        assertEquals("/data/user/0/com.example.epubreader/files/covers/sample.jpg", restoredBook.coverImagePath)
        assertEquals(1, restoredBook.bookmarks.size)
        assertEquals("Chapter 3", restoredBook.bookmarks[0].chapterTitle)
    }

    /**
     * Tests [Bookmark] data model initialization.
     */
    @Test
    fun testBookmarkInitialization() {
        val bookmark = Bookmark(
            id = "uuid-123",
            chapterIndex = 4,
            chapterTitle = "Chapter 5",
            snippet = "Beginning of chapter 5..."
        )

        assertEquals("uuid-123", bookmark.id)
        assertEquals(4, bookmark.chapterIndex)
        assertEquals("Chapter 5", bookmark.chapterTitle)
        assertEquals("Beginning of chapter 5...", bookmark.snippet)
        assertTrue(bookmark.createdTimestamp > 0L)
    }
}
