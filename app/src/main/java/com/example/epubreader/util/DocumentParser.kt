package com.example.epubreader.util

import android.content.Context
import android.net.Uri

/**
 * Data representation of a parsed document (e.g. EPUB, PDF).
 *
 * @property title Extracted title of the document.
 * @property author Extracted author of the document.
 * @property chapters List of extracted text contents for each chapter in spine order.
 */
data class ParsedDocument(
    val title: String,
    val author: String,
    val chapters: List<DocumentChapter>
)

/**
 * Data representation of a single document chapter.
 *
 * @property index Zero-based index of the chapter.
 * @property title Chapter header or fallback title.
 * @property content Cleaned text content of the chapter.
 */
data class DocumentChapter(
    val index: Int,
    val title: String,
    val content: String
)

/**
 * Interface for document parsers to support multiple formats (EPUB, PDF, etc.).
 */
interface DocumentParser {
    /**
     * Parses metadata (Title & Author) from a document file.
     *
     * @param context Application context used to open input stream.
     * @param uri Content URI pointing to the file.
     * @param fallbackFileName Fallback title if metadata title is missing.
     * @return Pair of (Title, Author).
     */
    fun parseMetadata(context: Context, uri: Uri, fallbackFileName: String): Pair<String, String>

    /**
     * Fully parses a document into structured chapters and metadata.
     *
     * @param context Application context used to open input stream.
     * @param uri Content URI pointing to the file.
     * @param fallbackFileName Fallback title if metadata title is missing.
     * @return Parsed [ParsedDocument] instance.
     */
    fun parseDocument(context: Context, uri: Uri, fallbackFileName: String): ParsedDocument
}
