package com.downtomark.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.downtomark.MainActivity
import com.downtomark.data.model.Highlight
import com.downtomark.data.model.HighlightColor
import com.downtomark.ui.notes.NotesSheet
import com.downtomark.ui.theme.AppTheme
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    fileUri: String,
    onNavigateBack: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val blocks by viewModel.blocks.collectAsState()
    val fileNotes by viewModel.fileNotes.collectAsState()
    val fileName by viewModel.fileName.collectAsState()
    val contentHashMismatch by viewModel.contentHashMismatch.collectAsState()

    var showThemeMenu by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var showAnnotationSheet by remember { mutableStateOf(false) }
    var currentSelection by remember { mutableStateOf<TextSelection?>(null) }
    var editingHighlight by remember { mutableStateOf<Highlight?>(null) }
    val selectionState = remember { SelectionState() }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(fileUri) {
        viewModel.loadFile(fileUri)
    }

    LaunchedEffect(contentHashMismatch) {
        if (contentHashMismatch) {
            snackbarHostState.showSnackbar("File content has changed since notes were created")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = fileName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (contentHashMismatch) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Content changed",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { showNotesSheet = true }) {
                        Icon(Icons.Default.EditNote, contentDescription = "Notes")
                    }
                    IconButton(onClick = { showThemeMenu = true }) {
                        Icon(Icons.Default.Palette, contentDescription = "Theme")
                    }
                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = { showThemeMenu = false }
                    ) {
                        AppTheme.entries.forEach { theme ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (theme) {
                                            AppTheme.EVERFOREST_DARK -> "Everforest Dark"
                                            AppTheme.EVERFOREST_LIGHT -> "Everforest Light"
                                            AppTheme.NEWSPRINT -> "Newsprint"
                                        }
                                    )
                                },
                                onClick = {
                                    MainActivity.currentTheme = theme
                                    showThemeMenu = false
                                }
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            itemsIndexed(blocks) { index, block ->
                val blockHighlights = fileNotes?.highlights?.filter {
                    it.blockIndex == index
                } ?: emptyList()
                val isBookmarked = fileNotes?.bookmarks?.any {
                    it.blockIndex == index
                } ?: false

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Bookmark gutter
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .padding(top = 2.dp)
                            .clickable { viewModel.toggleBookmark(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) {
                                Icons.Default.Bookmark
                            } else {
                                Icons.Default.BookmarkBorder
                            },
                            contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                            tint = if (isBookmarked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Block content
                    SelectableBlock(
                        block = block,
                        blockIndex = index,
                        highlights = blockHighlights,
                        selectionState = selectionState,
                        onSelectionComplete = { selection ->
                            currentSelection = selection
                            editingHighlight = null
                            showAnnotationSheet = true
                        },
                        onHighlightTap = { highlight ->
                            editingHighlight = highlight
                            currentSelection = null
                            showAnnotationSheet = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Annotation sheet
    if (showAnnotationSheet) {
        val allTags = fileNotes?.highlights?.flatMap { it.tags }?.distinct() ?: emptyList()
        AnnotationSheet(
            selection = currentSelection,
            existingHighlight = editingHighlight,
            allTags = allTags,
            onSave = { color, comment, tags ->
                if (editingHighlight != null) {
                    viewModel.updateHighlight(
                        editingHighlight!!.copy(
                            color = color,
                            comment = comment,
                            tags = tags
                        )
                    )
                } else if (currentSelection != null) {
                    viewModel.addHighlight(
                        Highlight(
                            id = UUID.randomUUID().toString(),
                            blockIndex = currentSelection!!.blockIndex,
                            startOffset = currentSelection!!.startOffset,
                            endOffset = currentSelection!!.endOffset,
                            highlightedText = currentSelection!!.selectedText,
                            color = color,
                            comment = comment,
                            tags = tags
                        )
                    )
                }
                showAnnotationSheet = false
                currentSelection = null
                editingHighlight = null
            },
            onDelete = if (editingHighlight != null) {
                {
                    viewModel.deleteHighlight(editingHighlight!!.id)
                    showAnnotationSheet = false
                    editingHighlight = null
                }
            } else null,
            onDismiss = {
                showAnnotationSheet = false
                currentSelection = null
                editingHighlight = null
            }
        )
    }

    // Notes sheet
    if (showNotesSheet && fileNotes != null) {
        NotesSheet(
            fileNotes = fileNotes!!,
            onScrollToBlock = { blockIndex ->
                scope.launch {
                    listState.animateScrollToItem(blockIndex)
                }
            },
            onHighlightTap = { highlight ->
                editingHighlight = highlight
                showNotesSheet = false
                showAnnotationSheet = true
            },
            onDismiss = { showNotesSheet = false }
        )
    }
}
