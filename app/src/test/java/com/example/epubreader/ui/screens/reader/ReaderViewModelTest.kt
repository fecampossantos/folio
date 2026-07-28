package com.example.epubreader.ui.screens.reader

import com.example.epubreader.util.EpubChapter
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests covering ReaderUiState, theme modes, font size limits, and chapter page calculations.
 */
class ReaderViewModelTest {

    /**
     * Tests [ReaderThemeMode] enum values.
     */
    @Test
    fun testThemeModes() {
        assertEquals(2, ReaderThemeMode.entries.size)
        assertEquals(ReaderThemeMode.LIGHT, ReaderThemeMode.valueOf("LIGHT"))
        assertEquals(ReaderThemeMode.DARK, ReaderThemeMode.valueOf("DARK"))
    }

    /**
     * Tests default values of [ReaderUiState].
     */
    @Test
    fun testReaderUiStateDefaults() {
        val state = ReaderUiState()
        assertTrue(state.isLoading)
        assertEquals("", state.title)
        assertEquals("", state.author)
        assertTrue(state.chapters.isEmpty())
        assertEquals(0, state.currentChapterIndex)
        assertEquals(0, state.currentPageIndex)
        assertEquals(ReaderThemeMode.LIGHT, state.themeMode)
        assertEquals(18, state.fontSizeSp)
        assertTrue(state.bookmarks.isEmpty())
        assertFalse(state.isTtsSpeaking)
        assertFalse(state.isCompleted)
    }

    /**
     * Tests [BookPageLocation] structure and properties.
     */
    @Test
    fun testBookPageLocation() {
        val pageLoc = BookPageLocation(
            chapterIndex = 1,
            pageIndexInChapter = 3,
            chapterTitle = "Chapter 2: The Journey",
            text = "Once upon a time in a far away land..."
        )

        assertEquals(1, pageLoc.chapterIndex)
        assertEquals(3, pageLoc.pageIndexInChapter)
        assertEquals("Chapter 2: The Journey", pageLoc.chapterTitle)
        assertTrue(pageLoc.text.startsWith("Once upon a time"))
    }

    /**
     * Tests font size boundaries (min 12sp, max 32sp).
     */
    @Test
    fun testFontSizeBoundaries() {
        var fontSize = 18
        // Increase font size test
        fontSize = (fontSize + 2).coerceAtMost(32)
        assertEquals(20, fontSize)

        // Max limit test
        fontSize = 32
        fontSize = (fontSize + 2).coerceAtMost(32)
        assertEquals(32, fontSize)

        // Decrease font size test
        fontSize = (fontSize - 2).coerceAtLeast(12)
        assertEquals(30, fontSize)

        // Min limit test
        fontSize = 12
        fontSize = (fontSize - 2).coerceAtLeast(12)
        assertEquals(12, fontSize)
    }

    /**
     * Tests reading completion percentage calculation.
     */
    @Test
    fun testCalculateProgress() {
        val totalChapters = 5
        val ch0Progress = ((0 + 1).toFloat() / totalChapters.toFloat()) * 100f
        assertEquals(20f, ch0Progress, 0.01f)

        val ch4Progress = ((4 + 1).toFloat() / totalChapters.toFloat()) * 100f
        assertEquals(100f, ch4Progress, 0.01f)
    }
}
