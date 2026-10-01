package com.example.epubreader.util

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor

/**
 * Parser for PDF documents utilizing Android's native PdfRenderer.
 */
object PdfParser : DocumentParser {

    /**
     * Extracts basic metadata for a PDF file. Native PDF metadata extraction requires
     * extra libraries, so we fallback to the file name as the title.
     *
     * @param context Application context.
     * @param uri Content URI pointing to the PDF file.
     * @param fallbackFileName Fallback title.
     * @return Pair of (Title, Author).
     */
    override fun parseMetadata(context: Context, uri: Uri, fallbackFileName: String): Pair<String, String> {
        val title = fallbackFileName.removeSuffix(".pdf").replace("_", " ")
        return Pair(title, "Unknown Author")
    }

    /**
     * Fully parses a PDF file into a ParsedDocument containing its pages as chapters.
     *
     * @param context Application context used to open input stream.
     * @param uri Content URI pointing to the PDF file.
     * @param fallbackFileName Fallback title.
     * @return Parsed [ParsedDocument] instance.
     */
    override fun parseDocument(context: Context, uri: Uri, fallbackFileName: String): ParsedDocument {
        val chapters = mutableListOf<DocumentChapter>()
        val (title, author) = parseMetadata(context, uri, fallbackFileName)
        
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                val pageCount = renderer.pageCount
                
                for (i in 0 until pageCount) {
                    chapters.add(
                        DocumentChapter(
                            index = i,
                            title = "Page ${i + 1}",
                            content = "PDF_PAGE_$i"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            chapters.add(DocumentChapter(0, "Error", "Could not parse PDF contents."))
        } finally {
            renderer?.close()
            pfd?.close()
        }

        if (chapters.isEmpty()) {
            chapters.add(DocumentChapter(0, "Empty", "No pages found in this PDF."))
        }

        return ParsedDocument(
            title = title,
            author = author,
            chapters = chapters
        )
    }
}
