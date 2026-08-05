package com.example.epubreader.ui.screens.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.epubreader.ui.theme.AppThemeMode
import com.example.epubreader.ui.theme.EpubReaderTheme
import com.example.epubreader.util.EpubParser
import kotlinx.coroutines.launch

/**
 * Data representation of a single rendered page location across the entire book.
 *
 * @property chapterIndex Zero-based chapter index.
 * @property pageIndexInChapter Zero-based page index inside the chapter.
 * @property chapterTitle Display title of the chapter.
 * @property text Clean text content of the page.
 */
data class BookPageLocation(
    val chapterIndex: Int,
    val pageIndexInChapter: Int,
    val chapterTitle: String,
    val text: String
)

/**
 * Main composable reader screen displaying continuous paginated content, customization controls, TTS, and bookmarks.
 *
 * @param uiState Current reader UI state.
 * @param onBackClick Callback when back arrow is pressed.
 * @param onPagePositionUpdated Callback when page or chapter index changes during horizontal scrolling.
 * @param onThemeChanged Callback when user selects a theme mode.
 * @param onIncreaseFontSize Callback when user taps font size increase.
 * @param onDecreaseFontSize Callback when user taps font size decrease.
 * @param onToggleBookmark Callback when user taps bookmark icon.
 * @param onDeleteBookmark Callback when user deletes a bookmark.
 * @param onToggleTts Callback when user taps Text-To-Speech play/stop button.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    uiState: ReaderUiState,
    onBackClick: () -> Unit,
    onPagePositionUpdated: (Int, Int) -> Unit,
    onThemeChanged: (ReaderThemeMode) -> Unit,
    onIncreaseFontSize: () -> Unit,
    onDecreaseFontSize: () -> Unit,
    onToggleBookmark: () -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onToggleTts: () -> Unit,
    onSaveSnippet: (String, String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isFullscreen by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }
    var showMenuModal by remember { mutableStateOf(false) }
    
    var selectedSnippetText by remember { mutableStateOf<String?>(null) }
    var showSaveSnippetDialog by remember { mutableStateOf(false) }
    var snippetNoteText by remember { mutableStateOf("") }

    val isCurrentChapterBookmarked = uiState.bookmarks.any { it.chapterIndex == uiState.currentChapterIndex }

    val appThemeMode = when (uiState.themeMode) {
        ReaderThemeMode.LIGHT -> AppThemeMode.LIGHT
        ReaderThemeMode.DARK -> AppThemeMode.DARK
    }

    EpubReaderTheme(appThemeMode = appThemeMode) {
        val backgroundColor = MaterialTheme.colorScheme.background
        val textColor = MaterialTheme.colorScheme.onBackground

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthDp = with(density) { constraints.maxWidth.toDp().value }
        val heightDp = with(density) { constraints.maxHeight.toDp().value }

        val fontSizeSp = uiState.fontSizeSp
        val textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * 1.5f).sp
        )

        val availableHeightDp = (heightDp - 140f).coerceAtLeast(100f)
        val availableWidthDp = (widthDp - 48f).coerceAtLeast(100f)
        
        val exactWidthPx = with(density) { availableWidthDp.dp.toPx().toInt() }.coerceAtLeast(1)
        val exactHeightPx = with(density) { availableHeightDp.dp.toPx().toInt() }.coerceAtLeast(1)

        var allBookPages by remember { mutableStateOf<List<BookPageLocation>>(emptyList()) }
        var isPaginating by remember { mutableStateOf(false) }
        val textMeasurer = rememberTextMeasurer()

        LaunchedEffect(uiState.chapters, fontSizeSp, exactWidthPx, exactHeightPx) {
            if (uiState.chapters.isEmpty()) return@LaunchedEffect
            
            isPaginating = true
            val pageConstraints = Constraints(maxWidth = exactWidthPx, maxHeight = exactHeightPx)
            
            val newPages = withContext(Dispatchers.Default) {
                val list = mutableListOf<BookPageLocation>()
                val chunkSize = 4000
                
                uiState.chapters.forEachIndexed { chIdx, chapter ->
                    var remainingText = chapter.content
                    var pageIdx = 0
                    
                    while (remainingText.isNotEmpty()) {
                        var attemptSize = chunkSize
                        var chunk = if (remainingText.length > attemptSize) remainingText.substring(0, attemptSize) else remainingText
                        
                        var result = textMeasurer.measure(
                            text = AnnotatedString(chunk),
                            style = textStyle,
                            constraints = pageConstraints
                        )
                        
                        while (result.lineCount > 0 && result.getLineBottom(result.lineCount - 1) <= pageConstraints.maxHeight && chunk.length < remainingText.length) {
                            attemptSize += 2000
                            chunk = if (remainingText.length > attemptSize) remainingText.substring(0, attemptSize) else remainingText
                            result = textMeasurer.measure(
                                text = AnnotatedString(chunk),
                                style = textStyle,
                                constraints = pageConstraints
                            )
                        }

                        var lastLineIndex = result.lineCount - 1
                        while (lastLineIndex >= 0 && result.getLineBottom(lastLineIndex) > pageConstraints.maxHeight) {
                            lastLineIndex--
                        }
                        
                        if (lastLineIndex < 0) break
                        
                        val endIndex = result.getLineEnd(lastLineIndex, visibleEnd = true)
                        if (endIndex <= 0) break
                        
                        val pageText = remainingText.substring(0, endIndex).trim()
                        
                        list.add(
                            BookPageLocation(
                                chapterIndex = chIdx,
                                pageIndexInChapter = pageIdx,
                                chapterTitle = chapter.title,
                                text = pageText
                            )
                        )
                        remainingText = remainingText.substring(endIndex).trimStart()
                        pageIdx++
                    }
                }
                if (list.isEmpty()) {
                    list.add(BookPageLocation(0, 0, "Content", "No content available"))
                }
                list
            }
            
            allBookPages = newPages
            isPaginating = false
        }
        
        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { allBookPages.size.coerceAtLeast(1) }
        )

        LaunchedEffect(allBookPages) {
            if (allBookPages.isNotEmpty() && !isPaginating) {
                val targetGlobalIdx = allBookPages.indexOfFirst {
                    it.chapterIndex == uiState.currentChapterIndex && it.pageIndexInChapter == uiState.currentPageIndex
                }
                val finalIdx = if (targetGlobalIdx >= 0) targetGlobalIdx else {
                    allBookPages.indexOfFirst { it.chapterIndex == uiState.currentChapterIndex }.coerceAtLeast(0)
                }
                if (pagerState.currentPage != finalIdx) {
                    pagerState.scrollToPage(finalIdx)
                }
            }
        }
        
        val activeLocation = allBookPages.getOrNull(pagerState.currentPage) ?: BookPageLocation(0, 0, "Content", "")

        // Notify page and chapter changes for exact JSON persistence whenever user scrolls horizontally
        LaunchedEffect(pagerState.currentPage) {
            val loc = allBookPages.getOrNull(pagerState.currentPage)
            if (loc != null) {
                onPagePositionUpdated(loc.chapterIndex, loc.pageIndexInChapter)
            }
            selectedSnippetText = null // Clear selection on page change
        }

        Scaffold(
            floatingActionButton = {
                AnimatedVisibility(
                    visible = selectedSnippetText != null && !isFullscreen,
                    enter = slideInVertically(initialOffsetY = { it * 2 }),
                    exit = slideOutVertically(targetOffsetY = { it * 2 })
                ) {
                    ExtendedFloatingActionButton(
                        text = { Text("Save Snippet") },
                        icon = { Icon(Icons.Default.ContentCut, contentDescription = null) },
                        onClick = {
                            snippetNoteText = ""
                            showSaveSnippetDialog = true 
                        }
                    )
                }
            },
            topBar = {
                AnimatedVisibility(
                    visible = !isFullscreen,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it })
                ) {
                    TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = uiState.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = activeLocation.chapterTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Library"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onToggleTts) {
                            Icon(
                                imageVector = if (uiState.isTtsSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (uiState.isTtsSpeaking) "Stop TTS" else "Read Aloud (TTS)",
                                tint = if (uiState.isTtsSpeaking) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = onToggleBookmark) {
                            Icon(
                                imageVector = if (isCurrentChapterBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark Chapter",
                                tint = if (isCurrentChapterBookmarked) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = { showSettingsModal = true }) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "Appearance Settings"
                            )
                        }
                        IconButton(onClick = { showMenuModal = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "Table of Contents & Bookmarks"
                            )
                        }
                    }
                )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = !isFullscreen,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { (pagerState.currentPage + 1).toFloat() / allBookPages.size.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (pagerState.currentPage > 0) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                },
                                enabled = pagerState.currentPage > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "Previous Page"
                                )
                            }

                            Text(
                                text = "Page ${pagerState.currentPage + 1} of ${allBookPages.size} • Ch ${activeLocation.chapterIndex + 1}/${uiState.chapters.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            IconButton(
                                onClick = {
                                    if (pagerState.currentPage < allBookPages.size - 1) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                },
                                enabled = pagerState.currentPage < allBookPages.size - 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Next Page"
                                )
                            }
                        }
                    }
                }
                }
            }
) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(backgroundColor)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            var downTime = 0L
                            var downPos = androidx.compose.ui.geometry.Offset.Zero
                            while (true) {
                                val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                if (event.changes.size == 1) {
                                    val change = event.changes.first()
                                    if (change.pressed && !change.previousPressed) {
                                        // Down
                                        downTime = System.currentTimeMillis()
                                        downPos = change.position
                                    } else if (!change.pressed && change.previousPressed) {
                                        // Up
                                        val duration = System.currentTimeMillis() - downTime
                                        val distance = (change.position - downPos).getDistance()
                                        if (duration < 300 && distance < 30f) {
                                            val width = size.width
                                            val x = change.position.x
                                            if (x < width * 0.2f) {
                                                if (pagerState.currentPage > 0) {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                    }
                                                }
                                            } else if (x > width * 0.8f) {
                                                if (pagerState.currentPage < allBookPages.size - 1) {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                    }
                                                }
                                            } else {
                                                isFullscreen = !isFullscreen
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                if (uiState.isLoading || isPaginating) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(if (uiState.isLoading) "Loading book contents..." else "Formatting pages...", color = textColor)
                    }
                } else if (allBookPages.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        val pageLoc = allBookPages.getOrNull(pageIndex)
                        var textFieldValue by remember(pageLoc?.text) {
                            mutableStateOf(TextFieldValue(pageLoc?.text ?: ""))
                        }
                        
                        LaunchedEffect(textFieldValue.selection) {
                            if (!textFieldValue.selection.collapsed) {
                                val selected = textFieldValue.annotatedString.substring(textFieldValue.selection.start, textFieldValue.selection.end)
                                selectedSnippetText = selected
                            } else {
                                selectedSnippetText = null
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = { 
                                    textFieldValue = it.copy(text = pageLoc?.text ?: "") 
                                },
                                readOnly = true,
                                textStyle = textStyle.copy(color = textColor),
                                cursorBrush = SolidColor(Color.Transparent),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No chapter content available.", color = textColor)
                    }
                }
            }
        }

        // Modal Dialog for Appearance / Theme & Typography Settings
        if (showSettingsModal) {
            AlertDialog(
                onDismissRequest = { showSettingsModal = false },
                title = { Text("Reader Customization") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilterChip(
                                selected = uiState.themeMode == ReaderThemeMode.LIGHT,
                                onClick = { onThemeChanged(ReaderThemeMode.LIGHT) },
                                label = { Text("Light") }
                            )
                            FilterChip(
                                selected = uiState.themeMode == ReaderThemeMode.DARK,
                                onClick = { onThemeChanged(ReaderThemeMode.DARK) },
                                label = { Text("Dark") }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Font Size (${uiState.fontSizeSp} sp)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(onClick = onDecreaseFontSize) {
                                Text("A-")
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "${uiState.fontSizeSp} sp",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            OutlinedButton(onClick = onIncreaseFontSize) {
                                Text("A+")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsModal = false }) {
                        Text("Done")
                    }
                }
            )
        }

        // Modal Dialog for Table of Contents & Bookmarks
        if (showMenuModal) {
            var selectedTab by remember { mutableIntStateOf(0) }

            AlertDialog(
                onDismissRequest = { showMenuModal = false },
                title = {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Chapters (${uiState.chapters.size})") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Bookmarks (${uiState.bookmarks.size})") }
                        )
                    }
                },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                    ) {
                        if (selectedTab == 0) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                uiState.chapters.forEach { chapter ->
                                    TextButton(
                                        onClick = {
                                            val targetGlobalIdx = allBookPages.indexOfFirst { it.chapterIndex == chapter.index }.coerceAtLeast(0)
                                            coroutineScope.launch {
                                                pagerState.scrollToPage(targetGlobalIdx)
                                            }
                                            showMenuModal = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = chapter.title,
                                                fontWeight = if (chapter.index == activeLocation.chapterIndex) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (uiState.bookmarks.any { it.chapterIndex == chapter.index }) {
                                                Icon(
                                                    imageVector = Icons.Default.Bookmark,
                                                    contentDescription = "Bookmarked",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            if (uiState.bookmarks.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No bookmarks added yet.")
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    uiState.bookmarks.forEach { bookmark ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            val targetGlobalIdx = allBookPages.indexOfFirst { it.chapterIndex == bookmark.chapterIndex }.coerceAtLeast(0)
                                                            coroutineScope.launch {
                                                                pagerState.scrollToPage(targetGlobalIdx)
                                                            }
                                                            showMenuModal = false
                                                        }
                                                ) {
                                                    Text(
                                                        text = bookmark.chapterTitle,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = bookmark.snippet,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                IconButton(onClick = { onDeleteBookmark(bookmark.id) }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Bookmark",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMenuModal = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }

    if (showSaveSnippetDialog && selectedSnippetText != null) {
        AlertDialog(
            onDismissRequest = { showSaveSnippetDialog = false },
            title = { Text("Save Snippet") },
            text = {
                Column {
                    Text(
                        text = "\"${selectedSnippetText}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = snippetNoteText,
                        onValueChange = { snippetNoteText = it },
                        label = { Text("Note (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    selectedSnippetText?.let { text ->
                        onSaveSnippet(text, snippetNoteText)
                    }
                    showSaveSnippetDialog = false
                    selectedSnippetText = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveSnippetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

}
