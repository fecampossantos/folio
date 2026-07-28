package com.example.epubreader.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests covering cover image loading, null safety handling, and file path utilities.
 */
class CoverManagerTest {

    /**
     * Tests that [CoverManager.loadBitmapFromFile] returns null safely when path is null or blank.
     */
    @Test
    fun testLoadBitmapFromFile_NullOrBlankPath() {
        assertNull(CoverManager.loadBitmapFromFile(null))
        assertNull(CoverManager.loadBitmapFromFile(""))
        assertNull(CoverManager.loadBitmapFromFile("   "))
    }

    /**
     * Tests that [CoverManager.loadBitmapFromFile] returns null safely when target file does not exist.
     */
    @Test
    fun testLoadBitmapFromFile_NonExistentFile() {
        assertNull(CoverManager.loadBitmapFromFile("/path/to/non_existent_image_123.jpg"))
    }
}
