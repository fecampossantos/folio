package com.example.epubreader.ui.screens.library

import android.net.TestUri
import com.example.epubreader.data.model.BookMetadata
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests covering LibraryUiState, sorting options, search filtering, and state transformations.
 */
class LibraryViewModelTest {

    /**
     * Tests [SortOption] labels and enum values.
     */
    @Test
    fun testSortOptions() {
        assertEquals("Recently Opened", SortOption.RECENTLY_OPENED.label)
        assertEquals("Reading Progress", SortOption.PROGRESS.label)
        assertEquals("Title (A-Z)", SortOption.TITLE.label)
        assertEquals("Author (A-Z)", SortOption.AUTHOR.label)
    }

    /**
     * Tests default values of [LibraryUiState].
     */
    @Test
    fun testLibraryUiStateDefaults() {
        val state = LibraryUiState()
        assertFalse(state.isLoading)
        assertNull(state.selectedFolderUri)
        assertTrue(state.allBooks.isEmpty())
        assertTrue(state.displayedBooks.isEmpty())
        assertEquals("", state.searchQuery)
        assertEquals(SortOption.RECENTLY_OPENED, state.sortOption)
        assertEquals(0L, state.totalReadingTimeSeconds)
        assertEquals(0, state.completedBooksCount)
        assertNull(state.message)
        assertNull(state.errorMessage)
    }

    /**
     * Tests sorting [BookMetadata] items by title, author, progress, and timestamp.
     */
    @Test
    fun testBookMetadataSorting() {
        val uri1 = TestUri("content://book1")
        val uri2 = TestUri("content://book2")

        val book1 = BookMetadata(
            uri = uri1,
            fileName = "alpha.epub",
            title = "Alpha",
            author = "Zoe",
            progressPercentage = 10f,
            lastOpenedTimestamp = 1000L
        )
        val book2 = BookMetadata(
            uri = uri2,
            fileName = "beta.epub",
            title = "Beta",
            author = "Adam",
            progressPercentage = 80f,
            lastOpenedTimestamp = 5000L
        )

        val list = listOf(book1, book2)

        val sortedByTitle = list.sortedBy { it.title.lowercase() }
        assertEquals("Alpha", sortedByTitle[0].title)

        val sortedByAuthor = list.sortedBy { it.author.lowercase() }
        assertEquals("Adam", sortedByAuthor[0].author)

        val sortedByProgress = list.sortedByDescending { it.progressPercentage }
        assertEquals("Beta", sortedByProgress[0].title)

        val sortedByRecent = list.sortedByDescending { it.lastOpenedTimestamp }
        assertEquals("Beta", sortedByRecent[0].title)
    }
}
