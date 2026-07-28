package com.example.epubreader.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Utility managing extraction, caching, custom selection, and loading of book cover images.
 */
object CoverManager {

    /**
     * Retrieves the file handle for a book's cached cover image.
     *
     * @param context Application context.
     * @param uriString Unique string identifier of the EPUB file.
     * @return File object pointing to the cached cover image path.
     */
    fun getCoverFile(context: Context, uriString: String): File {
        val coversDir = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }
        val fileName = "${hashString(uriString)}.jpg"
        return File(coversDir, fileName)
    }

    /**
     * Automatically extracts the cover image from an EPUB ZIP container if present.
     *
     * @param context Application context.
     * @param epubUri Content URI pointing to the EPUB file.
     * @return Absolute file path to saved cover image, or null if no cover found in EPUB.
     */
    fun extractAndSaveEpubCover(context: Context, epubUri: Uri): String? {
        val targetFile = getCoverFile(context, epubUri.toString())
        if (targetFile.exists() && targetFile.length() > 0) {
            return targetFile.absolutePath
        }

        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(epubUri) ?: return null
            var extractedBytes: ByteArray? = null

            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                val candidateEntries = mutableMapOf<String, ByteArray>()

                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.lowercase()
                        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")) {
                            val bytes = zip.readBytes()
                            if (name.contains("cover")) {
                                extractedBytes = bytes
                                break
                            }
                            candidateEntries[name] = bytes
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }

                if (extractedBytes == null && candidateEntries.isNotEmpty()) {
                    extractedBytes = candidateEntries.values.firstOrNull()
                }
            }

            val finalBytes = extractedBytes
            if (finalBytes != null && finalBytes.isNotEmpty()) {
                targetFile.writeBytes(finalBytes)
                return targetFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Saves a user-selected custom cover image file to local storage.
     *
     * @param context Application context.
     * @param epubUri Content URI of the EPUB file.
     * @param sourceImageUri Content URI of user-selected image.
     * @return Absolute file path of saved cover image or null if failed.
     */
    fun saveCustomCover(context: Context, epubUri: Uri, sourceImageUri: Uri): String? {
        return try {
            val targetFile = getCoverFile(context, epubUri.toString())
            context.contentResolver.openInputStream(sourceImageUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (targetFile.exists() && targetFile.length() > 0) {
                targetFile.absolutePath
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Loads a [Bitmap] instance from a local file path.
     *
     * @param filePath Absolute path string of image file.
     * @return Decoded Bitmap or null if file invalid.
     */
    fun loadBitmapFromFile(filePath: String?): Bitmap? {
        if (filePath.isNullOrEmpty()) return null
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generates SHA-256 hash string for unique file naming.
     *
     * @param input Raw string to hash.
     * @return Hexadecimal hash string.
     */
    private fun hashString(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input.hashCode().toString()
        }
    }
}
