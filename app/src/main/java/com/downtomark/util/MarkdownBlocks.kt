package com.downtomark.util

import androidx.compose.ui.text.AnnotatedString

sealed class MarkdownBlock {
    data class Heading(
        val level: Int,
        val content: AnnotatedString
    ) : MarkdownBlock()

    data class Paragraph(
        val content: AnnotatedString
    ) : MarkdownBlock()

    data class CodeBlock(
        val code: String,
        val language: String? = null
    ) : MarkdownBlock()

    data class BlockQuote(
        val children: List<MarkdownBlock>
    ) : MarkdownBlock()

    data class OrderedList(
        val items: List<ListItem>,
        val startNumber: Int = 1
    ) : MarkdownBlock()

    data class UnorderedList(
        val items: List<ListItem>
    ) : MarkdownBlock()

    data class ListItem(
        val content: AnnotatedString,
        val children: List<MarkdownBlock> = emptyList()
    )

    data object HorizontalRule : MarkdownBlock()
}
