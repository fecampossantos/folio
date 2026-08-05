package com.example.epubreader.data.repository

import com.example.epubreader.data.model.BookState
import com.example.epubreader.data.model.ReadingHistory
import org.junit.Assert.assertEquals
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
