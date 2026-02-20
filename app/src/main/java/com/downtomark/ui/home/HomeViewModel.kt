package com.downtomark.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.downtomark.DownToMarkApp
import com.downtomark.data.model.FileEntry
import com.downtomark.data.model.NoteIndex
import com.downtomark.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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
}
