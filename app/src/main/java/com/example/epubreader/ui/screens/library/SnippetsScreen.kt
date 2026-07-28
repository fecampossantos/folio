package com.example.epubreader.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.epubreader.data.model.TextSnippet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetsScreen(
    snippets: List<TextSnippet>,
    onDelete: (String) -> Unit,
    onEditNote: (String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedBookFilter by remember { mutableStateOf<String?>("All Books") }

    val allBooks = remember(snippets) {
        listOf("All Books") + snippets.map { it.bookTitle }.distinct().sorted()
    }

    val filteredSnippets = remember(snippets, searchQuery, selectedBookFilter) {
        snippets.filter { snippet ->
            val matchesBook = selectedBookFilter == "All Books" || snippet.bookTitle == selectedBookFilter
            val matchesSearch = searchQuery.isBlank() ||
                    snippet.text.contains(searchQuery, ignoreCase = true) ||
                    snippet.note.contains(searchQuery, ignoreCase = true)
            matchesBook && matchesSearch
        }.sortedByDescending { it.createdTimestamp }
    }

    var editSnippetId by remember { mutableStateOf<String?>(null) }
    var editNoteText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search snippets and notes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        ScrollableTabRow(
            selectedTabIndex = allBooks.indexOf(selectedBookFilter).coerceAtLeast(0),
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            allBooks.forEach { book ->
                Tab(
                    selected = selectedBookFilter == book,
                    onClick = { selectedBookFilter = book },
                    text = { Text(book) }
                )
            }
        }

        if (filteredSnippets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No snippets found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredSnippets) { snippet ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = snippet.bookTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "\"${snippet.text}\"",
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = FontStyle.Italic
                            )
                            if (snippet.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Note: ${snippet.note}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = {
                                    editSnippetId = snippet.id
                                    editNoteText = snippet.note
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Note")
                                }
                                IconButton(onClick = { onDelete(snippet.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Snippet")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editSnippetId != null) {
        AlertDialog(
            onDismissRequest = { editSnippetId = null },
            title = { Text("Edit Note") },
            text = {
                OutlinedTextField(
                    value = editNoteText,
                    onValueChange = { editNoteText = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                Button(onClick = {
                    onEditNote(editSnippetId!!, editNoteText)
                    editSnippetId = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editSnippetId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
