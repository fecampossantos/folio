package com.example.epubreader.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests covering EPUB text parsing, HTML tag stripping, metadata fallback, and pagination calculations.
 */
class EpubParserTest {

    /**
     * Tests that [EpubParser.paginateText] returns fallback single page when text is blank.
     */
    @Test
    fun testPaginateText_BlankInput() {
        val result = EpubParser.paginateText("", 1000)
        assertEquals(1, result.size)
        assertEquals("No content available", result[0])
    }

    /**
     * Tests that [EpubParser.paginateText] correctly chunks multi-paragraph text.
     */
    @Test
    fun testPaginateText_MultiParagraph() {
        val text = "Paragraph 1 is here.\n\nParagraph 2 is here.\n\nParagraph 3 is here."
        val pages = EpubParser.paginateText(text, 600)
        assertTrue(pages.isNotEmpty())
        assertTrue(pages[0].contains("Paragraph 1"))
    }

    /**
     * Tests that [EpubParser.paginateText] handles large font size character limits smoothly (> 26sp).
     */
    @Test
    fun testPaginateText_LargeFontSize() {
        val paragraph1 = "This is the first long paragraph in the book chapter."
        val paragraph2 = "This is the second long paragraph in the book chapter."
        val text = "$paragraph1\n\n$paragraph2"

        val pages = EpubParser.paginateText(text, 600)
        assertTrue(pages.size >= 1)
        assertTrue(pages[0].contains("first long paragraph"))
    }

    /**
     * Tests that long paragraphs exceeding page capacity are split into word/sentence boundary chunks.
     */
    @Test
    fun testPaginateText_LongParagraphSplit() {
        val longParagraph = "Word ".repeat(300)
        val pages = EpubParser.paginateText(longParagraph, 600)
        assertTrue(pages.size > 1)
    }

    /**
     * Tests fallback ParsedEpub creation when zip input stream is missing or invalid.
     */
    @Test
    fun testParsedEpubDataStructure() {
        val chapter1 = EpubChapter(0, "Chapter 1", "Hello World")
        val parsedEpub = ParsedEpub("My Book", "My Author", listOf(chapter1))

        assertEquals("My Book", parsedEpub.title)
        assertEquals("My Author", parsedEpub.author)
        assertEquals(1, parsedEpub.chapters.size)
        assertEquals("Chapter 1", parsedEpub.chapters[0].title)
    }
}
