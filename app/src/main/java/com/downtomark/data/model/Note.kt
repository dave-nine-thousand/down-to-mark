package com.downtomark.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FileNotes(
    val fileUri: String,
    val fileName: String,
    val contentHash: String,
    val highlights: List<Highlight> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList()
)

@Serializable
data class Highlight(
    val id: String,
    val blockIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val highlightedText: String,
    val color: HighlightColor = HighlightColor.YELLOW,
    val comment: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class HighlightColor {
    YELLOW, GREEN, BLUE, PINK, ORANGE
}

@Serializable
data class Bookmark(
    val id: String,
    val blockIndex: Int,
    val label: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
