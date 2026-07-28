package com.example.epubreader.ui.screens.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.epubreader.ui.theme.SepiaBackground
import com.example.epubreader.ui.theme.SepiaText
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
    onToggleTts: () -> Unit
) {
    var showMenuModal by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val isCurrentChapterBookmarked = uiState.bookmarks.any { it.chapterIndex == uiState.currentChapterIndex }

    // Determine reader background & text colors strictly from ThemeMode
    val (backgroundColor, textColor) = when (uiState.themeMode) {
        ReaderThemeMode.LIGHT -> Pair(Color(0xFFFFFFFF), Color(0xFF1C1B1F))
        ReaderThemeMode.DARK -> Pair(Color(0xFF121212), Color(0xFFE0E0E0))
        ReaderThemeMode.SEPIA -> Pair(SepiaBackground, SepiaText)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthDp = with(density) { constraints.maxWidth.toDp().value }
        val heightDp = with(density) { constraints.maxHeight.toDp().value }

        val fontSizeSp = uiState.fontSizeSp
        val fontHeightDp = fontSizeSp * 1.4f
        val fontWidthDp = fontSizeSp * 0.5f

        val availableHeightDp = (heightDp - 140f).coerceAtLeast(100f)
        val availableWidthDp = (widthDp - 48f).coerceAtLeast(100f)

        val linesPerPage = (availableHeightDp / fontHeightDp).toInt().coerceAtLeast(5)
        val charsPerLine = (availableWidthDp / fontWidthDp).toInt().coerceAtLeast(20)
        val targetPageChars = (linesPerPage * charsPerLine * 1.25f).toInt().coerceIn(600, 4500)

        // Paginate ALL chapters across the entire book into a single seamless continuous HorizontalPager
        val allBookPages = remember(uiState.chapters, targetPageChars) {
            val list = mutableListOf<BookPageLocation>()
            uiState.chapters.forEachIndexed { chIdx, chapter ->
                val chapterPages = EpubParser.paginateText(chapter.content, targetPageChars)
                chapterPages.forEachIndexed { pageIdx, pageText ->
                    list.add(
                        BookPageLocation(
                            chapterIndex = chIdx,
                            pageIndexInChapter = pageIdx,
                            chapterTitle = chapter.title,
                            text = pageText
                        )
                    )
                }
            }
            if (list.isEmpty()) {
                list.add(BookPageLocation(0, 0, "Content", "No content available"))
            }
            list
        }

        // Find initial global page index matching saved chapter and page index
        val initialGlobalPage = remember(allBookPages, uiState.currentChapterIndex, uiState.currentPageIndex) {
            val idx = allBookPages.indexOfFirst {
                it.chapterIndex == uiState.currentChapterIndex && it.pageIndexInChapter == uiState.currentPageIndex
            }
            if (idx >= 0) idx else allBookPages.indexOfFirst { it.chapterIndex == uiState.currentChapterIndex }.coerceAtLeast(0)
        }

        val pagerState = rememberPagerState(
            initialPage = initialGlobalPage.coerceIn(0, (allBookPages.size - 1).coerceAtLeast(0)),
            pageCount = { allBookPages.size }
        )

        val activeLocation = allBookPages.getOrNull(pagerState.currentPage) ?: allBookPages[0]

        // Notify page and chapter changes for exact JSON persistence whenever user scrolls horizontally
        LaunchedEffect(pagerState.currentPage) {
            val loc = allBookPages.getOrNull(pagerState.currentPage)
            if (loc != null) {
                onPagePositionUpdated(loc.chapterIndex, loc.pageIndexInChapter)
            }
        }

        Scaffold(
            topBar = {
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
            },
            bottomBar = {
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
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(backgroundColor)
            ) {
                if (uiState.isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading book contents...", color = textColor)
                    }
                } else if (allBookPages.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        val pageLoc = allBookPages.getOrNull(pageIndex)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Text(
                                text = pageLoc?.text ?: "",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = uiState.fontSizeSp.sp,
                                    lineHeight = (uiState.fontSizeSp * 1.5f).sp
                                ),
                                color = textColor
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
                            FilterChip(
                                selected = uiState.themeMode == ReaderThemeMode.SEPIA,
                                onClick = { onThemeChanged(ReaderThemeMode.SEPIA) },
                                label = { Text("Sepia") }
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
}
