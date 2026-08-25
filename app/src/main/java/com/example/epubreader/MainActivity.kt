package com.example.epubreader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.epubreader.data.model.BookMetadata
import com.example.epubreader.data.repository.LibraryRepository
import com.example.epubreader.data.repository.ReadingStateRepository
import com.example.epubreader.ui.screens.library.LibraryScreen
import com.example.epubreader.ui.screens.library.LibraryViewModel
import com.example.epubreader.ui.screens.reader.ReaderScreen
import com.example.epubreader.ui.screens.reader.ReaderViewModel
import com.example.epubreader.ui.theme.EpubReaderTheme

/**
 * Main Activity host for the EPUB Reader Android application.
 */
class MainActivity : ComponentActivity() {

    private lateinit var stateRepository: ReadingStateRepository
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var libraryViewModel: LibraryViewModel
    private lateinit var readerViewModel: ReaderViewModel
    private var pendingCoverBookUri: Uri? = null

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            persistFolderPermission(it)
            libraryViewModel.onFolderSelected(it)
        }
    }

    private val exportBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { libraryViewModel.exportBackup(it) }
    }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { libraryViewModel.importBackup(it) }
    }

    private val customCoverPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { imageUri: Uri? ->
        val bookUri = pendingCoverBookUri
        if (imageUri != null && bookUri != null) {
            libraryViewModel.onCustomCoverSelected(applicationContext, bookUri, imageUri)
        }
        pendingCoverBookUri = null
    }

    /**
     * Called when Activity is starting. Initializes repositories, view models, and sets Compose UI.
     *
     * @param savedInstanceState Saved activity state bundle.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stateRepository = ReadingStateRepository(applicationContext)
        libraryRepository = LibraryRepository(applicationContext, stateRepository)
        libraryViewModel = LibraryViewModel(libraryRepository, stateRepository)
        readerViewModel = ReaderViewModel(stateRepository)

        setContent {
            val libraryState by libraryViewModel.uiState.collectAsState()
            EpubReaderTheme(appThemeMode = libraryState.appThemeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    /**
     * Top-level composable managing navigation state between Library and Reader screens.
     */
    @Composable
    private fun AppNavigation() {
        val libraryState by libraryViewModel.uiState.collectAsState()
        val readerState by readerViewModel.uiState.collectAsState()
        var activeBook by remember { mutableStateOf<BookMetadata?>(null) }

        if (activeBook == null) {
            LibraryScreen(
                uiState = libraryState,
                onSelectFolderClick = { launchFolderPicker() },
                onSearchQueryChanged = { query -> libraryViewModel.onSearchQueryChanged(query) },
                onSortOptionChanged = { option -> libraryViewModel.onSortOptionChanged(option) },
                onAppThemeChanged = { mode -> libraryViewModel.onAppThemeChanged(mode) },
                onExportBackupClick = { exportBackupLauncher.launch("reading_history_backup.json") },
                onImportBackupClick = { importBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                onBookClick = { book ->
                    libraryViewModel.onBookOpened(book)
                    activeBook = book
                    readerViewModel.loadBook(
                        context = applicationContext,
                        bookUri = book.uri,
                        bookFileName = book.fileName
                    )
                },
                onChangeCoverClick = { book ->
                    pendingCoverBookUri = book.uri
                    customCoverPickerLauncher.launch("image/*")
                },
                onClearMessage = { libraryViewModel.clearMessage() },
                onDeleteSnippet = { id -> libraryViewModel.deleteSnippet(id) },
                onEditSnippetNote = { id, note -> libraryViewModel.updateSnippetNote(id, note) },
                hardcoverToken = libraryViewModel.getHardcoverToken(),
                onSaveHardcoverToken = { token -> libraryViewModel.saveHardcoverToken(token) },
                onSyncHardcover = { book, statusId, rating -> libraryViewModel.syncToHardcover(book, statusId, rating) },
                onUpdateMetadata = { book, title, author -> libraryViewModel.updateBookMetadata(book, title, author) },
                onLinkHardcover = { book, hardcoverBookId -> libraryViewModel.linkHardcoverBook(book, hardcoverBookId) },
                searchHardcoverBooks = { query -> libraryViewModel.searchHardcoverBooks(query) }
            )
        } else {
            ReaderScreen(
                uiState = readerState,
                onBackClick = {
                    readerViewModel.stopTts()
                    readerViewModel.persistState()
                    activeBook = null
                    // Refresh library listing to reflect newly saved timestamp, bookmarks & progress
                    libraryState.selectedFolderUri?.let { uriString ->
                        libraryViewModel.refreshBooks(Uri.parse(uriString))
                    }
                },
                onPagePositionUpdated = { chapterIndex, pageIndex ->
                    readerViewModel.updatePagePosition(chapterIndex, pageIndex)
                },
                onThemeChanged = { mode -> readerViewModel.setThemeMode(mode) },
                onIncreaseFontSize = { readerViewModel.increaseFontSize() },
                onDecreaseFontSize = { readerViewModel.decreaseFontSize() },
                onToggleBookmark = { readerViewModel.toggleBookmark() },
                onDeleteBookmark = { id -> readerViewModel.removeBookmark(id) },
                onToggleTts = { readerViewModel.toggleTts(applicationContext) },
                onSaveSnippet = { text, note -> readerViewModel.saveSnippet(text, note) }
            )
        }
    }

    /**
     * Launches the Storage Access Framework directory tree picker.
     */
    private fun launchFolderPicker() {
        folderPickerLauncher.launch(null)
    }

    /**
     * Requests and persists read access permission for a selected SAF URI tree.
     *
     * @param uri Directory tree URI returned by SAF picker.
     */
    private fun persistFolderPermission(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
