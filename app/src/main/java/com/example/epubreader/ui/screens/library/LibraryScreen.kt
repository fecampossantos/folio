package com.example.epubreader.ui.screens.library

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.epubreader.R
import com.example.epubreader.data.model.BookMetadata
import com.example.epubreader.ui.theme.AppThemeMode
import com.example.epubreader.util.CoverManager
import kotlinx.coroutines.launch

/**
 * Main composable screen displaying the library of EPUB books with search, sorting, statistics, and JSON backup controls.
 *
 * @param uiState State object for the library UI.
 * @param onSelectFolderClick Callback triggered when user clicks select folder button.
 * @param onSearchQueryChanged Callback triggered when user types in search query bar.
 * @param onSortOptionChanged Callback triggered when user selects a sorting criterion.
 * @param onAppThemeChanged Callback triggered when user changes app theme mode.
 * @param onExportBackupClick Callback triggered when user chooses to export JSON history.
 * @param onImportBackupClick Callback triggered when user chooses to import JSON history.
 * @param onBookClick Callback triggered when user clicks a book to start reading.
 * @param onChangeCoverClick Callback triggered when user clicks to select or change a custom cover image.
 * @param onClearMessage Callback triggered to dismiss feedback message.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onSelectFolderClick: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSortOptionChanged: (SortOption) -> Unit,
    onAppThemeChanged: (AppThemeMode) -> Unit,
    onExportBackupClick: () -> Unit,
    onImportBackupClick: () -> Unit,
    onBookClick: (BookMetadata) -> Unit,
    onChangeCoverClick: (BookMetadata) -> Unit,
    onClearMessage: () -> Unit,
    onDeleteSnippet: (String) -> Unit,
    onEditSnippetNote: (String, String) -> Unit,
    hardcoverToken: String?,
    onSaveHardcoverToken: (String) -> Unit,
    onSyncHardcover: (BookMetadata, Int, Float?) -> Unit,
    onUpdateMetadata: (BookMetadata, String, String) -> Unit,
    onLinkHardcover: (BookMetadata, Int?) -> Unit,
    searchHardcoverBooks: suspend (String) -> List<com.example.epubreader.data.repository.HardcoverBook>
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showHardcoverSettings by remember { mutableStateOf(false) }
    var bookToSync by remember { mutableStateOf<BookMetadata?>(null) }
    var bookToLink by remember { mutableStateOf<BookMetadata?>(null) }
    var bookToEditMetadata by remember { mutableStateOf<BookMetadata?>(null) }
    var selectedTab by remember { mutableStateOf("Books") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "Folio Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Text("Folio Library", fontWeight = FontWeight.Bold)
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showStatsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Insights,
                            contentDescription = "Reading Statistics"
                        )
                    }

                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort Books"
                        )
                    }

                    IconButton(onClick = onSelectFolderClick) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Select EPUB Folder"
                        )
                    }

                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options"
                        )
                    }

                    // Sort Dropdown Menu
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    onSortOptionChanged(option)
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (uiState.sortOption == option) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )
                        }
                    }

                    // More Options Dropdown Menu
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Hardcover API Settings") },
                            onClick = {
                                showHardcoverSettings = true
                                showMoreMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Theme Settings") },
                            onClick = {
                                showThemeDialog = true
                                showMoreMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export History Backup (JSON)") },
                            onClick = {
                                onExportBackupClick()
                                showMoreMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import History Backup (JSON)") },
                            onClick = {
                                onImportBackupClick()
                                showMoreMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == "Books",
                    onClick = { selectedTab = "Books" },
                    icon = { Icon(Icons.Default.LibraryBooks, contentDescription = "Books") },
                    label = { Text("Books") }
                )
                NavigationBarItem(
                    selected = selectedTab == "Snippets",
                    onClick = { selectedTab = "Snippets" },
                    icon = { Icon(Icons.Default.FormatQuote, contentDescription = "Snippets") },
                    label = { Text("Snippets") }
                )
            }
        }
    ) { paddingValues ->
        if (showThemeDialog) {
            ThemeSettingsDialog(
                currentMode = uiState.appThemeMode,
                onThemeSelected = { mode ->
                    onAppThemeChanged(mode)
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }
        if (showHardcoverSettings) {
            HardcoverSettingsDialog(
                initialToken = hardcoverToken,
                onSaveToken = { token ->
                    onSaveHardcoverToken(token)
                    showHardcoverSettings = false
                },
                onDismiss = { showHardcoverSettings = false }
            )
        }
        bookToSync?.let { book ->
            HardcoverSyncDialog(
                bookTitle = book.title,
                onSync = { statusId, rating ->
                    onSyncHardcover(book, statusId, rating)
                    bookToSync = null
                },
                onDismiss = { bookToSync = null }
            )
        }
        bookToLink?.let { book ->
            HardcoverLinkDialog(
                initialQuery = book.title,
                onSearch = searchHardcoverBooks,
                onLink = { id ->
                    onLinkHardcover(book, id)
                    bookToLink = null
                    // After linking, immediately prompt for sync
                    bookToSync = book.copy(hardcoverBookId = id)
                },
                onDismiss = { bookToLink = null }
            )
        }
        bookToEditMetadata?.let { book ->
            EditMetadataDialog(
                book = book,
                onSave = { newTitle, newAuthor ->
                    onUpdateMetadata(book, newTitle, newAuthor)
                    bookToEditMetadata = null
                },
                onDismiss = { bookToEditMetadata = null }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedTab == "Snippets") {
                SnippetsScreen(
                    snippets = uiState.snippets,
                    onDelete = onDeleteSnippet,
                    onEditNote = onEditSnippetNote
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                // Search Input Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { 
                        Text(
                            text = "Search by title, author, or filename...",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Search"
                                )
                            }
                        }
                    },
                    singleLine = true
                )



                // Main Library Content List
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        uiState.isLoading -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Scanning folder for EPUB files...")
                            }
                        }
                        uiState.selectedFolderUri == null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Select your EPUB books folder",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Pick a folder containing your .epub books to start reading.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(onClick = onSelectFolderClick) {
                                    Text("Choose Folder")
                                }
                            }
                        }
                        uiState.allBooks.isEmpty() -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "No .epub files found in selected folder",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = onSelectFolderClick) {
                                    Text("Select another folder")
                                }
                            }
                        }
                        uiState.displayedBooks.isEmpty() -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "No books match \"${uiState.searchQuery}\"",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.displayedBooks, key = { it.uri.toString() }) { book ->
                                    BookItemCard(
                                        book = book,
                                        onClick = { onBookClick(book) },
                                        onChangeCoverClick = { onChangeCoverClick(book) },
                                        onSyncClick = { 
                                            if (book.hardcoverBookId != null) {
                                                bookToSync = book
                                            } else {
                                                bookToLink = book
                                            }
                                        },
                                        onEditMetadataClick = { bookToEditMetadata = book }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            uiState.message?.let { msg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = onClearMessage) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(msg)
                }
            }

            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = onClearMessage) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
            }
        }
    }

    // Reading Statistics Modal Dialog
    if (showStatsDialog) {
        AlertDialog(
            onDismissRequest = { showStatsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reading Statistics")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Total Reading Time",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatTimeSpan(uiState.totalReadingTimeSeconds),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Books Completed: ${uiState.completedBooksCount}")
                                Text("Total Books: ${uiState.allBooks.size}")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Per Book Breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    uiState.allBooks.forEach { book ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = book.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatTimeSpan(book.totalReadingTimeSeconds),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Single book item card displaying cover thumbnail, title, author, reading progress, and action buttons.
 *
 * @param book Metadata for the book item.
 * @param onClick Callback when book card or Read button is clicked.
 * @param onChangeCoverClick Callback when user taps to pick/change cover image.
 */
@Composable
fun BookItemCard(
    book: BookMetadata,
    onClick: () -> Unit,
    onChangeCoverClick: () -> Unit,
    onSyncClick: () -> Unit,
    onEditMetadataClick: () -> Unit
) {
    val bitmap = remember(book.coverImagePath) {
        CoverManager.loadBitmapFromFile(book.coverImagePath)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book Cover Image Thumbnail / Cover Placeholder
            Surface(
                modifier = Modifier
                    .width(56.dp)
                    .height(80.dp)
                    .clickable(onClick = onChangeCoverClick),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Cover for ${book.title}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Add Cover Image",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Cover",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (book.isCompleted || book.progressPercentage >= 99f) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Time: ${formatTimeSpan(book.totalReadingTimeSeconds)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (book.bookmarksCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Bookmarks",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${book.bookmarksCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (book.progressPercentage > 0f) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { book.progressPercentage / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onSyncClick) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Sync to Hardcover",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Button(onClick = onClick) {
                    Text(if (book.lastOpenedTimestamp > 0L) "Continue" else "Read")
                }
                TextButton(onClick = onChangeCoverClick) {
                    Text(if (bitmap != null) "Edit Cover" else "+ Cover", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onEditMetadataClick) {
                    Text("Edit Info", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * Formats a duration in seconds into a human-readable HH:mm:ss or mm:ss string.
 *
 * @param totalSeconds Duration in seconds.
 * @return Formatted time string.
 */
fun formatTimeSpan(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%dh %02dm", hours, minutes)
    } else {
        String.format("%dm %02ds", minutes, seconds)
    }
}

/**
 * Dialog enabling the user to choose between Light, Dark, or Follow System themes.
 *
 * @param currentMode Currently active [AppThemeMode].
 * @param onThemeSelected Callback triggered when user selects a theme option.
 * @param onDismiss Callback triggered to close dialog.
 */
@Composable
fun ThemeSettingsDialog(
    currentMode: AppThemeMode,
    onThemeSelected: (AppThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("App Theme") },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                AppThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = (mode == currentMode),
                                onClick = { onThemeSelected(mode) },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (mode == currentMode),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = mode.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun HardcoverSettingsDialog(
    initialToken: String?,
    onSaveToken: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var token by remember { mutableStateOf(initialToken ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hardcover API Settings") },
        text = {
            Column {
                Text("Enter your Hardcover API token:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSaveToken(token) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HardcoverSyncDialog(
    bookTitle: String,
    onSync: (Int, Float?) -> Unit,
    onDismiss: () -> Unit
) {
    var statusId by remember { mutableStateOf(2) } // Default: Currently Reading
    var rating by remember { mutableStateOf<Float?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sync '${bookTitle}' to Hardcover") },
        text = {
            Column {
                Text("Status:")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = statusId == 1, onClick = { statusId = 1 })
                    Text("Want to Read", modifier = Modifier.clickable { statusId = 1 })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = statusId == 2, onClick = { statusId = 2 })
                    Text("Currently Reading", modifier = Modifier.clickable { statusId = 2 })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = statusId == 3, onClick = { statusId = 3 })
                    Text("Read", modifier = Modifier.clickable { statusId = 3 })
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Rating (1-5, Optional):")
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    for (i in 1..5) {
                        val isSelected = rating != null && rating!! >= i.toFloat()
                        Icon(
                            imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = "Rate $i stars",
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { 
                                    if (rating == i.toFloat()) rating = null else rating = i.toFloat() 
                                },
                            tint = if (isSelected) Color(0xFFFFD700) else Color.Gray
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSync(statusId, rating) }) {
                Text("Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditMetadataDialog(
    book: BookMetadata,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Metadata") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Author") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, author) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun HardcoverLinkDialog(
    initialQuery: String,
    onSearch: suspend (String) -> List<com.example.epubreader.data.repository.HardcoverBook>,
    onLink: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<com.example.epubreader.data.repository.HardcoverBook>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialQuery) {
        isLoading = true
        results = onSearch(initialQuery)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link to Hardcover") },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Search title...") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        scope.launch {
                            isLoading = true
                            results = onSearch(query)
                            isLoading = false
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (results != null) {
                    if (results!!.isEmpty()) {
                        Text("No books found.")
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn {
                            items(results!!) { book ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onLink(book.id) }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text(book.title, style = MaterialTheme.typography.bodyLarge)
                                    Text(book.author, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                }
                                androidx.compose.material3.Divider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
