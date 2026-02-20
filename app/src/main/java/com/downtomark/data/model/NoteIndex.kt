package com.downtomark.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NoteIndex(
    val version: Int = 1,
    val files: List<FileEntry> = emptyList(),
    val tags: List<TagInfo> = emptyList()
)

@Serializable
data class FileEntry(
    val uri: String,
    val name: String,
    val notesFileName: String,
    val highlightCount: Int = 0,
    val bookmarkCount: Int = 0,
    val lastOpened: Long = System.currentTimeMillis()
)

@Serializable
data class TagInfo(
    val name: String,
    val color: String? = null
)
