package com.example.epubreader.ui.screens.reader

import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.epubreader.data.model.Bookmark
import com.example.epubreader.data.model.BookState
import com.example.epubreader.data.repository.ReadingStateRepository
import com.example.epubreader.util.EpubChapter
import com.example.epubreader.util.EpubParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Reader UI theme options.
 */
enum class ReaderThemeMode {
    LIGHT, DARK
}

/**
 * UI state for the Reader screen.
 *
 * @property isLoading True while parsing EPUB content.
 * @property title Book title.
 * @property author Book author.
 * @property chapters List of chapters in the book.
 * @property currentChapterIndex Current chapter index being read.
 * @property currentPageIndex Current page index within the chapter.
 * @property totalPagesInCurrentChapter Total calculated pages for the current chapter.
 * @property progressPercentage Calculated total progress percentage.
 * @property themeMode Active theme mode (LIGHT, DARK, SEPIA).
 * @property fontSizeSp Active font size in SP.
 * @property bookmarks List of saved bookmarks for this book.
 * @property isTtsSpeaking True if Text-To-Speech is currently narrating chapter text.
 * @property totalReadingTimeSeconds Total accumulated reading time for this book.
 * @property isCompleted True if user finished reading book.
 * @property errorMessage Error message if parsing fails.
 */
data class ReaderUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val author: String = "",
    val chapters: List<EpubChapter> = emptyList(),
    val currentChapterIndex: Int = 0,
    val currentPageIndex: Int = 0,
    val totalPagesInCurrentChapter: Int = 1,
    val progressPercentage: Float = 0f,
    val themeMode: ReaderThemeMode = ReaderThemeMode.LIGHT,
    val fontSizeSp: Int = 18,
    val bookmarks: List<Bookmark> = emptyList(),
    val isTtsSpeaking: Boolean = false,
    val totalReadingTimeSeconds: Long = 0L,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for the EPUB Reader screen managing parsing, theme customization, TTS, timer tracking, and state saving.
 *
 * @param stateRepository Repository saving reading state to JSON file.
 */
class ReaderViewModel(
    private val stateRepository: ReadingStateRepository
) : ViewModel(), TextToSpeech.OnInitListener {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var currentBookUri: Uri? = null
    private var fileName: String = ""
    private var tts: TextToSpeech? = null
    private var ttsInitialized = false
    private var timerJob: Job? = null

    /**
     * Initializes and loads the EPUB file from the given content URI and starts reading timer.
     *
     * @param context Application context.
     * @param bookUri Content URI of the EPUB file.
     * @param bookFileName File name of the EPUB file.
     */
    fun loadBook(context: Context, bookUri: Uri, bookFileName: String) {
        currentBookUri = bookUri
        fileName = bookFileName

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val parsedEpub = EpubParser.parseEpub(context, bookUri, bookFileName)
            val savedState = stateRepository.getBookState(bookUri.toString())

            val initialChapter = savedState?.currentChapterIndex?.coerceIn(0, (parsedEpub.chapters.size - 1).coerceAtLeast(0)) ?: 0
            val initialPage = savedState?.currentPageIndex ?: 0
            val themeMode = savedState?.readerThemeMode?.let { name ->
                try { ReaderThemeMode.valueOf(name) } catch (e: Exception) { ReaderThemeMode.LIGHT }
            } ?: ReaderThemeMode.LIGHT
            val fontSizeSp = savedState?.fontSizeSp ?: 18
            val bookmarks = savedState?.bookmarks ?: emptyList()
            val savedTime = savedState?.totalReadingTimeSeconds ?: 0L
            val isCompleted = savedState?.isCompleted ?: false

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                title = parsedEpub.title,
                author = parsedEpub.author,
                chapters = parsedEpub.chapters,
                currentChapterIndex = initialChapter,
                currentPageIndex = initialPage,
                themeMode = themeMode,
                fontSizeSp = fontSizeSp,
                bookmarks = bookmarks,
                totalReadingTimeSeconds = savedTime,
                isCompleted = isCompleted,
                progressPercentage = calculateProgress(initialChapter, parsedEpub.chapters.size)
            )

            // Update opened timestamp in JSON
            stateRepository.recordBookOpened(
                uriString = bookUri.toString(),
                fileName = bookFileName,
                title = parsedEpub.title,
                author = parsedEpub.author
            )

            // Start reading time counter ticker
            startReadingTimer()
        }
    }

    /**
     * Starts a periodic timer job that increments total reading time every second.
     */
    private fun startReadingTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000L)
                val newTime = _uiState.value.totalReadingTimeSeconds + 1
                _uiState.value = _uiState.value.copy(totalReadingTimeSeconds = newTime)

                // Periodically persist state every 30 seconds
                if (newTime % 30 == 0L) {
                    persistState()
                }
            }
        }
    }

    /**
     * Toggles Text-To-Speech playback for the current chapter text.
     *
     * @param context Application context to initialize TTS engine.
     */
    fun toggleTts(context: Context) {
        if (_uiState.value.isTtsSpeaking) {
            stopTts()
        } else {
            startTts(context)
        }
    }

    /**
     * Initializes and starts reading current chapter content aloud via Android TextToSpeech.
     *
     * @param context Application context.
     */
    private fun startTts(context: Context) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        } else if (ttsInitialized) {
            speakCurrentChapter()
        }
    }

    /**
     * Callback invoked when TextToSpeech engine completes initialization.
     *
     * @param status Init status code.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            ttsInitialized = true
            speakCurrentChapter()
        } else {
            _uiState.value = _uiState.value.copy(isTtsSpeaking = false)
        }
    }

    /**
     * Sends current chapter text content to the TextToSpeech engine queue.
     */
    private fun speakCurrentChapter() {
        val chapter = _uiState.value.chapters.getOrNull(_uiState.value.currentChapterIndex) ?: return
        val text = chapter.content.take(4000) // Speak current chapter chunk
        if (text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "EPUB_TTS_ID")
            _uiState.value = _uiState.value.copy(isTtsSpeaking = true)
        }
    }

    /**
     * Stops active Text-To-Speech speech playback.
     */
    fun stopTts() {
        tts?.stop()
        _uiState.value = _uiState.value.copy(isTtsSpeaking = false)
    }

    /**
     * Updates chapter and page position state and persists to state.
     *
     * @param chapterIndex Zero-based chapter index.
     * @param pageIndex Zero-based page index inside the chapter.
     */
    fun updatePagePosition(chapterIndex: Int, pageIndex: Int) {
        val currentState = _uiState.value
        if (currentState.currentChapterIndex != chapterIndex || currentState.currentPageIndex != pageIndex) {
            val totalChapters = currentState.chapters.size.coerceAtLeast(1)
            val newProgress = calculateProgress(chapterIndex, totalChapters)
            val isCompleted = newProgress >= 100f || (chapterIndex == totalChapters - 1)

            _uiState.value = currentState.copy(
                currentChapterIndex = chapterIndex,
                currentPageIndex = pageIndex,
                progressPercentage = newProgress,
                isCompleted = isCompleted
            )
            persistState()
        }
    }

    /**
     * Changes reader theme color mode and persists to JSON storage.
     *
     * @param mode Target theme mode (LIGHT, DARK, SEPIA).
     */
    fun setThemeMode(mode: ReaderThemeMode) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
        persistState()
    }

    /**
     * Increases reader font size by 2sp (max 32sp).
     */
    fun increaseFontSize() {
        val current = _uiState.value.fontSizeSp
        if (current < 32) {
            _uiState.value = _uiState.value.copy(fontSizeSp = current + 2)
            persistState()
        }
    }

    /**
     * Decreases reader font size by 2sp (min 12sp).
     */
    fun decreaseFontSize() {
        val current = _uiState.value.fontSizeSp
        if (current > 12) {
            _uiState.value = _uiState.value.copy(fontSizeSp = current - 2)
            persistState()
        }
    }

    /**
     * Toggles bookmark for the current chapter location.
     */
    fun toggleBookmark() {
        val currentState = _uiState.value
        val currentChapterIndex = currentState.currentChapterIndex
        val currentChapter = currentState.chapters.getOrNull(currentChapterIndex) ?: return

        val existing = currentState.bookmarks.find { it.chapterIndex == currentChapterIndex }
        val updatedBookmarks = if (existing != null) {
            currentState.bookmarks.filter { it.chapterIndex != currentChapterIndex }
        } else {
            val snippet = currentChapter.content.take(60).replace("\n", " ") + "..."
            val newBookmark = Bookmark(
                id = UUID.randomUUID().toString(),
                chapterIndex = currentChapterIndex,
                chapterTitle = currentChapter.title,
                snippet = snippet
            )
            currentState.bookmarks + newBookmark
        }

        _uiState.value = _uiState.value.copy(bookmarks = updatedBookmarks)
        persistState()
    }

    /**
     * Removes a bookmark by its ID.
     *
     * @param bookmarkId Unique ID of the bookmark to delete.
     */
    fun removeBookmark(bookmarkId: String) {
        val updated = _uiState.value.bookmarks.filter { it.id != bookmarkId }
        _uiState.value = _uiState.value.copy(bookmarks = updated)
        persistState()
    }

    /**
     * Persists the active UI state to the JSON history file.
     */
    fun persistState() {
        val uri = currentBookUri ?: return
        val state = _uiState.value
        val totalChapters = state.chapters.size.coerceAtLeast(1)

        viewModelScope.launch(Dispatchers.IO) {
            stateRepository.saveBookState(
                BookState(
                    uriString = uri.toString(),
                    fileName = fileName,
                    title = state.title,
                    author = state.author,
                    lastOpenedTimestamp = System.currentTimeMillis(),
                    currentChapterIndex = state.currentChapterIndex,
                    currentPageIndex = state.currentPageIndex,
                    totalChapters = totalChapters,
                    progressPercentage = state.progressPercentage,
                    readerThemeMode = state.themeMode.name,
                    fontSizeSp = state.fontSizeSp,
                    bookmarks = state.bookmarks,
                    totalReadingTimeSeconds = state.totalReadingTimeSeconds,
                    isCompleted = state.isCompleted
                )
            )
        }
    }

    /**
     * Calculates percentage completion based on current chapter index and total chapters.
     *
     * @param chapterIndex Current zero-based chapter index.
     * @param totalChapters Total chapter count.
     * @return Progress percentage float (0.0 to 100.0).
     */
    private fun calculateProgress(chapterIndex: Int, totalChapters: Int): Float {
        if (totalChapters <= 1) return 100f
        return ((chapterIndex + 1).toFloat() / totalChapters.toFloat()) * 100f
    }

    /**
     * Cleans up TextToSpeech resources and timer job when ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        stopTts()
        tts?.shutdown()
        tts = null
    }
}
