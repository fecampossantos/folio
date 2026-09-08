package com.example.epubreader.util

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Data representation of a parsed EPUB document.
 *
 * @property title Extracted title of the book.
 * @property author Extracted author of the book.
 * @property chapters List of extracted text contents for each chapter in spine order.
 */
data class ParsedEpub(
    val title: String,
    val author: String,
    val chapters: List<EpubChapter>
)

/**
 * Data representation of a single EPUB chapter.
 *
 * @property index Zero-based index of the chapter.
 * @property title Chapter header or fallback title.
 * @property content Cleaned text content of the chapter.
 */
data class EpubChapter(
    val index: Int,
    val title: String,
    val content: String
)

/**
 * Utility object for extracting metadata and text content from EPUB files.
 */
object EpubParser {

    /**
     * Splits chapter content into readable page chunks based on paragraph breaks and dynamic page character capacity.
     *
     * @param text Full text content of the chapter.
     * @param targetPageChars Target character count limit per page based on screen dimensions and font size.
     * @return List of page text content strings.
     */
    fun paginateText(text: String, targetPageChars: Int): List<String> {
        if (text.isBlank()) return listOf("No content available")

        val maxChars = targetPageChars.coerceAtLeast(600)
        val pages = mutableListOf<String>()
        val paragraphs = text.split("\n\n")
        var currentChunk = StringBuilder()

        for (paragraph in paragraphs) {
            val p = paragraph.trim()
            if (p.isEmpty()) continue

            // Flush chunk if adding paragraph exceeds page target
            if (currentChunk.isNotEmpty() && currentChunk.length + p.length > maxChars) {
                pages.add(currentChunk.toString().trim())
                currentChunk = StringBuilder()
            }

            // If a single paragraph is longer than maxChars, break it down into sentence/word chunks
            if (p.length > maxChars) {
                var remaining = p
                while (remaining.length > maxChars) {
                    var splitIdx = remaining.substring(0, maxChars).lastIndexOf(". ")
                    if (splitIdx < 100) {
                        splitIdx = remaining.substring(0, maxChars).lastIndexOf(' ')
                    }
                    if (splitIdx < 50) splitIdx = maxChars

                    val chunkText = remaining.substring(0, splitIdx + 1).trim()
                    if (currentChunk.isNotEmpty()) {
                        pages.add(currentChunk.toString().trim())
                        currentChunk = StringBuilder()
                    }
                    pages.add(chunkText)
                    remaining = remaining.substring(splitIdx + 1).trim()
                }
                if (remaining.isNotEmpty()) {
                    currentChunk.append(remaining)
                }
            } else {
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append("\n\n")
                }
                currentChunk.append(p)
            }
        }

        if (currentChunk.isNotEmpty()) {
            pages.add(currentChunk.toString().trim())
        }

        return if (pages.isEmpty()) listOf(text) else pages
    }

    /**
     * Parses metadata (Title & Author) from an EPUB file given its content URI.
     *
     * @param context Application context used to open input stream.
     * @param uri Content URI pointing to the EPUB file.
     * @param fallbackFileName Fallback title if metadata title is missing.
     * @return Pair of (Title, Author).
     */
    fun parseMetadata(context: Context, uri: Uri, fallbackFileName: String): Pair<String, String> {
        return try {
            val fileMap = readZipEntries(context.contentResolver.openInputStream(uri))
            val opfPath = findOpfPath(fileMap) ?: return Pair(fallbackFileName.removeSuffix(".epub"), "Unknown Author")
            val opfContent = fileMap[opfPath] ?: return Pair(fallbackFileName.removeSuffix(".epub"), "Unknown Author")
            extractMetadataFromOpf(opfContent, fallbackFileName)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(fallbackFileName.removeSuffix(".epub"), "Unknown Author")
        }
    }

    /**
     * Fully parses an EPUB file into structured chapters and metadata.
     *
     * @param context Application context used to open input stream.
     * @param uri Content URI pointing to the EPUB file.
     * @param fallbackFileName Fallback title if metadata title is missing.
     * @return Parsed [ParsedEpub] instance containing chapters and metadata.
     */
    fun parseEpub(context: Context, uri: Uri, fallbackFileName: String): ParsedEpub {
        try {
            val fileMap = readZipEntries(context.contentResolver.openInputStream(uri))
            val opfPath = findOpfPath(fileMap) ?: return createFallbackEpub(fallbackFileName)
            val opfContent = fileMap[opfPath] ?: return createFallbackEpub(fallbackFileName)

            val (title, author) = extractMetadataFromOpf(opfContent, fallbackFileName)
            val spineHrefList = extractSpineHrefsFromOpf(opfContent, opfPath)

            val chapters = mutableListOf<EpubChapter>()
            spineHrefList.forEachIndexed { _, href ->
                val chapterContent = fileMap[href] ?: fileMap[normalizePath(href)] ?: ""
                val cleanText = stripHtmlTags(chapterContent)
                if (cleanText.isNotBlank()) {
                    chapters.add(
                        EpubChapter(
                            index = chapters.size,
                            title = "Chapter ${chapters.size + 1}",
                            content = cleanText
                        )
                    )
                }
            }

            if (chapters.isEmpty()) {
                chapters.add(EpubChapter(0, "Content", "No readable chapters found in this EPUB file."))
            }

            return ParsedEpub(
                title = title,
                author = author,
                chapters = chapters
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return createFallbackEpub(fallbackFileName)
        }
    }

    /**
     * Reads all uncompressed text entries from a ZIP input stream.
     *
     * @param inputStream Open input stream of the ZIP/EPUB archive.
     * @return Map of entry path to string content.
     */
    private fun readZipEntries(inputStream: InputStream?): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (inputStream == null) return map

        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = normalizePath(entry.name)
                    if (name.endsWith(".xml") || name.endsWith(".opf") || name.endsWith(".html") ||
                        name.endsWith(".xhtml") || name.endsWith(".htm") || name.contains("container")
                    ) {
                        val content = zip.readBytes().toString(Charsets.UTF_8)
                        map[name] = content
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return map
    }

    /**
     * Finds the path to the main OPF file by reading container.xml.
     *
     * @param fileMap Extracted map of zip file entries.
     * @return Normalized path to OPF file or null.
     */
    private fun findOpfPath(fileMap: Map<String, String>): String? {
        val containerContent = fileMap.entries.find { it.key.endsWith("container.xml") }?.value ?: return null
        val match = Regex("full-path=\"([^\"]+)\"").find(containerContent)
        return match?.groupValues?.get(1)?.let { normalizePath(it) }
    }

    /**
     * Extracts title and author metadata from OPF content string.
     *
     * @param opfContent Raw XML text of OPF file.
     * @param fallbackName Default title fallback.
     * @return Pair of Title to Author.
     */
    private fun extractMetadataFromOpf(opfContent: String, fallbackName: String): Pair<String, String> {
        val titleMatch = Regex("<dc:title[^>]*>(.*?)</dc:title>", RegexOption.DOT_MATCHES_ALL).find(opfContent)
        val authorMatch = Regex("<dc:creator[^>]*>(.*?)</dc:creator>", RegexOption.DOT_MATCHES_ALL).find(opfContent)

        val title = titleMatch?.groupValues?.get(1)?.trim()?.let { stripHtmlTags(it) }
            .takeIf { !it.isNullOrBlank() } ?: fallbackName.removeSuffix(".epub")

        val author = authorMatch?.groupValues?.get(1)?.trim()?.let { stripHtmlTags(it) }
            .takeIf { !it.isNullOrBlank() } ?: "Unknown Author"

        return Pair(title, author)
    }

    /**
     * Extracts chapter file paths in spine order from OPF content.
     *
     * @param opfContent Raw XML text of OPF file.
     * @param opfPath Root path of OPF file inside zip.
     * @return List of normalized relative paths to chapter HTML files.
     */
    private fun extractSpineHrefsFromOpf(opfContent: String, opfPath: String): List<String> {
        val parentDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") else ""

        val manifestMap = mutableMapOf<String, String>()
        val itemMatches = Regex("<item\\s+[^>]*id=\"([^\"]+)\"[^>]*href=\"([^\"]+)\"[^>]*>|<item\\s+[^>]*href=\"([^\"]+)\"[^>]*id=\"([^\"]+)\"[^>]*>").findAll(opfContent)

        itemMatches.forEach { match ->
            val id = match.groupValues[1].ifEmpty { match.groupValues[4] }
            val href = match.groupValues[2].ifEmpty { match.groupValues[3] }
            if (id.isNotEmpty() && href.isNotEmpty()) {
                manifestMap[id] = href
            }
        }

        val spineHrefs = mutableListOf<String>()
        val itemrefMatches = Regex("<itemref\\s+[^>]*idref=\"([^\"]+)\"[^>]*>").findAll(opfContent)

        itemrefMatches.forEach { match ->
            val idref = match.groupValues[1]
            val href = manifestMap[idref]
            if (href != null) {
                val fullPath = if (parentDir.isNotEmpty()) "$parentDir/$href" else href
                spineHrefs.add(normalizePath(fullPath))
            }
        }

        return spineHrefs
    }

    /**
     * Removes HTML/XHTML tags and decodes common entities from content string.
     *
     * @param html Raw HTML/XHTML content.
     * @return Plain text content with preserved paragraph line breaks.
     */
    private fun stripHtmlTags(html: String): String {
        val bodyMatch = Regex("<body[^>]*>(.*?)</body>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)
        val content = bodyMatch?.groupValues?.get(1) ?: html

        return content
            .replace(Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("</h[1-6]>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("</div[^>]*>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("^[ \\t]+", RegexOption.MULTILINE), "")
            .replace(Regex("[ \\t]+$", RegexOption.MULTILINE), "")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    /**
     * Normalizes zip file entry paths by replacing backslashes and trimming slashes.
     *
     * @param path Raw file entry path.
     * @return Cleaned relative path.
     */
    private fun normalizePath(path: String): String {
        return path.replace("\\", "/").trimStart('/')
    }

    /**
     * Helper to build a fallback [ParsedEpub] when parsing fails.
     *
     * @param fallbackFileName File name to display as title.
     * @return Fallback epub document.
     */
    private fun createFallbackEpub(fallbackFileName: String): ParsedEpub {
        return ParsedEpub(
            title = fallbackFileName.removeSuffix(".epub"),
            author = "Unknown Author",
            chapters = listOf(EpubChapter(0, "Error", "Could not parse EPUB contents."))
        )
    }
}
