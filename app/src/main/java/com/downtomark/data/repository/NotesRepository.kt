package com.downtomark.data.repository

import android.content.Context
import com.downtomark.data.model.FileEntry
import com.downtomark.data.model.FileNotes
import com.downtomark.data.model.NoteIndex
import com.downtomark.ui.theme.AppTheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

class NotesRepository(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val notesDir: File
        get() = File(context.filesDir, "notes").also { it.mkdirs() }

    private val indexFile: File
        get() = File(notesDir, "index.json")

    private val prefsFile: File
        get() = File(notesDir, "prefs.json")

    fun loadIndex(): NoteIndex {
        if (!indexFile.exists()) return NoteIndex()
        return try {
            json.decodeFromString<NoteIndex>(indexFile.readText())
        } catch (e: Exception) {
            NoteIndex()
        }
    }

    fun saveIndex(index: NoteIndex) {
        indexFile.writeText(json.encodeToString(index))
    }

    fun loadFileNotes(notesFileName: String): FileNotes? {
        val file = File(notesDir, notesFileName)
        if (!file.exists()) return null
        return try {
            json.decodeFromString<FileNotes>(file.readText())
        } catch (e: Exception) {
            null
        }
    }

    fun saveFileNotes(notesFileName: String, notes: FileNotes) {
        File(notesDir, notesFileName).writeText(json.encodeToString(notes))
    }

    fun notesFileNameForUri(uri: String): String {
        val hash = md5(uri)
        return "$hash.json"
    }

    fun contentHash(content: String): String = md5(content)

    fun updateIndex(index: NoteIndex, entry: FileEntry): NoteIndex {
        val existing = index.files.indexOfFirst { it.uri == entry.uri }
        val updatedFiles = if (existing >= 0) {
            index.files.toMutableList().apply { set(existing, entry) }
        } else {
            index.files + entry
        }
        return index.copy(files = updatedFiles)
    }

    fun loadThemePreference(): AppTheme {
        if (!prefsFile.exists()) return AppTheme.EVERFOREST_DARK
        return try {
            val name = json.decodeFromString<String>(prefsFile.readText())
            AppTheme.valueOf(name)
        } catch (e: Exception) {
            AppTheme.EVERFOREST_DARK
        }
    }

    fun saveThemePreference(theme: AppTheme) {
        prefsFile.writeText(json.encodeToString(theme.name))
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
