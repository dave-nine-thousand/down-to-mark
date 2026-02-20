package com.downtomark.ui.reader

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.downtomark.DownToMarkApp
import com.downtomark.data.model.Bookmark
import com.downtomark.data.model.FileEntry
import com.downtomark.data.model.FileNotes
import com.downtomark.data.model.Highlight
import com.downtomark.data.model.NoteIndex
import com.downtomark.util.MarkdownBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as DownToMarkApp).repository
    private var notesFileName: String = ""
    private var currentUri: String = ""

    private val _blocks = MutableStateFlow<List<MarkdownBlock>>(emptyList())
    val blocks: StateFlow<List<MarkdownBlock>> = _blocks

    private val _fileNotes = MutableStateFlow<FileNotes?>(null)
    val fileNotes: StateFlow<FileNotes?> = _fileNotes

    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName

    private val _contentHashMismatch = MutableStateFlow(false)
    val contentHashMismatch: StateFlow<Boolean> = _contentHashMismatch

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadFile(uri: String) {
        currentUri = uri
        _error.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<DownToMarkApp>()
                val contentUri = Uri.parse(uri)
                val content = context.contentResolver.openInputStream(contentUri)
                    ?.bufferedReader()?.use { it.readText() }
                if (content == null) {
                    _error.value = "Could not read file — permission may have been revoked"
                    return@launch
                }

                val name = resolveFileName(contentUri)
                _fileName.value = name

                val parser = MarkdownParser()
                _blocks.value = parser.parse(content)

                notesFileName = repository.notesFileNameForUri(uri)
                val contentHash = repository.contentHash(content)

                val existingNotes = repository.loadFileNotes(notesFileName)
                if (existingNotes != null) {
                    _fileNotes.value = existingNotes
                    _contentHashMismatch.value = existingNotes.contentHash != contentHash
                } else {
                    val newNotes = FileNotes(
                        fileUri = uri,
                        fileName = name,
                        contentHash = contentHash
                    )
                    _fileNotes.value = newNotes
                    repository.saveFileNotes(notesFileName, newNotes)
                }

                val index = repository.loadIndex()
                val entry = FileEntry(
                    uri = uri,
                    name = name,
                    notesFileName = notesFileName,
                    highlightCount = _fileNotes.value?.highlights?.size ?: 0,
                    bookmarkCount = _fileNotes.value?.bookmarks?.size ?: 0,
                    lastOpened = System.currentTimeMillis()
                )
                repository.saveIndex(repository.updateIndex(index, entry))
            } catch (e: Exception) {
                _error.value = "Error loading file: ${e.message}"
                android.util.Log.e("ReaderViewModel", "Failed to load $uri", e)
            }
        }
    }

    fun toggleBookmark(blockIndex: Int) {
        val notes = _fileNotes.value ?: return
        val existing = notes.bookmarks.find { it.blockIndex == blockIndex }
        val updatedBookmarks = if (existing != null) {
            notes.bookmarks.filter { it.id != existing.id }
        } else {
            notes.bookmarks + Bookmark(
                id = UUID.randomUUID().toString(),
                blockIndex = blockIndex
            )
        }
        val updated = notes.copy(bookmarks = updatedBookmarks)
        _fileNotes.value = updated
        saveNotes(updated)
    }

    fun addHighlight(highlight: Highlight) {
        val notes = _fileNotes.value ?: return
        val updated = notes.copy(highlights = notes.highlights + highlight)
        _fileNotes.value = updated
        saveNotes(updated)
        updateTagsInIndex(updated)
    }

    fun updateHighlight(highlight: Highlight) {
        val notes = _fileNotes.value ?: return
        val updated = notes.copy(
            highlights = notes.highlights.map {
                if (it.id == highlight.id) highlight else it
            }
        )
        _fileNotes.value = updated
        saveNotes(updated)
        updateTagsInIndex(updated)
    }

    fun deleteHighlight(highlightId: String) {
        val notes = _fileNotes.value ?: return
        val updated = notes.copy(
            highlights = notes.highlights.filter { it.id != highlightId }
        )
        _fileNotes.value = updated
        saveNotes(updated)
        updateTagsInIndex(updated)
    }

    private fun saveNotes(notes: FileNotes) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveFileNotes(notesFileName, notes)
            val index = repository.loadIndex()
            val entry = FileEntry(
                uri = currentUri,
                name = _fileName.value,
                notesFileName = notesFileName,
                highlightCount = notes.highlights.size,
                bookmarkCount = notes.bookmarks.size,
                lastOpened = System.currentTimeMillis()
            )
            repository.saveIndex(repository.updateIndex(index, entry))
        }
    }

    private fun updateTagsInIndex(notes: FileNotes) {
        viewModelScope.launch(Dispatchers.IO) {
            val index = repository.loadIndex()
            val allTags = notes.highlights.flatMap { it.tags }.distinct()
            val existingTagNames = index.tags.map { it.name }.toSet()
            val newTags = allTags.filter { it !in existingTagNames }
                .map { com.downtomark.data.model.TagInfo(name = it) }
            if (newTags.isNotEmpty()) {
                repository.saveIndex(index.copy(tags = index.tags + newTags))
            }
        }
    }

    private fun resolveFileName(uri: Uri): String {
        val context = getApplication<DownToMarkApp>()
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                return cursor.getString(nameIndex)
            }
        }
        return uri.lastPathSegment ?: "unknown.md"
    }
}
