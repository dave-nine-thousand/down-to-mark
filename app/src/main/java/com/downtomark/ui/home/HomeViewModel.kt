package com.downtomark.ui.home

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.downtomark.DownToMarkApp
import com.downtomark.data.model.FileEntry
import com.downtomark.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as DownToMarkApp).repository

    private val _recentFiles = MutableStateFlow<List<FileEntry>>(emptyList())
    val recentFiles: StateFlow<List<FileEntry>> = _recentFiles

    init {
        loadRecentFiles()
    }

    fun loadRecentFiles() {
        val index = repository.loadIndex()
        _recentFiles.value = index.files.sortedByDescending { it.lastOpened }
    }

    fun saveTheme(theme: AppTheme) {
        repository.saveThemePreference(theme)
    }

    fun renameFile(entry: FileEntry, newName: String): Boolean {
        val context = getApplication<DownToMarkApp>()
        return try {
            val uri = Uri.parse(entry.uri)
            val newUri = DocumentsContract.renameDocument(
                context.contentResolver, uri, newName
            )
            if (newUri != null) {
                val index = repository.loadIndex()
                val updatedEntry = entry.copy(
                    uri = newUri.toString(),
                    name = newName,
                    notesFileName = repository.notesFileNameForUri(newUri.toString())
                )
                // Migrate notes file if URI changed
                if (newUri.toString() != entry.uri) {
                    val oldNotes = repository.loadFileNotes(entry.notesFileName)
                    if (oldNotes != null) {
                        repository.saveFileNotes(
                            updatedEntry.notesFileName,
                            oldNotes.copy(fileUri = newUri.toString(), fileName = newName)
                        )
                    }
                }
                val updatedFiles = index.files.map {
                    if (it.uri == entry.uri) updatedEntry else it
                }
                repository.saveIndex(index.copy(files = updatedFiles))
                loadRecentFiles()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "Rename failed", e)
            false
        }
    }

    fun duplicateFile(entry: FileEntry, destinationUri: Uri) {
        val context = getApplication<DownToMarkApp>()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sourceUri = Uri.parse(entry.uri)
                val input = context.contentResolver.openInputStream(sourceUri)
                val output = context.contentResolver.openOutputStream(destinationUri)
                if (input != null && output != null) {
                    input.use { src -> output.use { dst -> src.copyTo(dst) } }

                    // Resolve the new file name
                    var newName = entry.name
                    context.contentResolver.query(
                        destinationUri, null, null, null, null
                    )?.use { cursor ->
                        val col = cursor.getColumnIndex(
                            android.provider.OpenableColumns.DISPLAY_NAME
                        )
                        if (cursor.moveToFirst() && col >= 0) {
                            newName = cursor.getString(col)
                        }
                    }

                    val index = repository.loadIndex()
                    val newEntry = FileEntry(
                        uri = destinationUri.toString(),
                        name = newName,
                        notesFileName = repository.notesFileNameForUri(
                            destinationUri.toString()
                        ),
                        lastOpened = System.currentTimeMillis()
                    )
                    repository.saveIndex(repository.updateIndex(index, newEntry))
                    loadRecentFiles()
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Duplicate failed", e)
            }
        }
    }

    fun removeFromRecents(entry: FileEntry) {
        val index = repository.loadIndex()
        val updatedFiles = index.files.filter { it.uri != entry.uri }
        repository.saveIndex(index.copy(files = updatedFiles))
        loadRecentFiles()
    }
}
