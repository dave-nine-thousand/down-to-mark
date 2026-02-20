package com.downtomark.ui.reader

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.downtomark.util.MarkdownBlock
import org.commonmark.node.*
import org.commonmark.parser.Parser

class MarkdownParser {

    private val parser: Parser = Parser.builder().build()

    fun parse(markdown: String): List<MarkdownBlock> {
        val document = parser.parse(markdown)
        return processChildren(document)
    }

    private fun processChildren(parent: Node): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        var child = parent.firstChild
        while (child != null) {
            processNode(child)?.let { blocks.add(it) }
            child = child.next
        }
        return blocks
    }

    private fun processNode(node: Node): MarkdownBlock? {
        return when (node) {
            is Heading -> MarkdownBlock.Heading(
                level = node.level,
                content = flattenInlines(node)
            )
            is Paragraph -> MarkdownBlock.Paragraph(
                content = flattenInlines(node)
            )
            is FencedCodeBlock -> MarkdownBlock.CodeBlock(
                code = node.literal.trimEnd('\n'),
                language = node.info.takeIf { it.isNotBlank() }
            )
            is IndentedCodeBlock -> MarkdownBlock.CodeBlock(
                code = node.literal.trimEnd('\n')
            )
            is BlockQuote -> MarkdownBlock.BlockQuote(
                children = processChildren(node)
            )
            is OrderedList -> MarkdownBlock.OrderedList(
                items = processListItems(node),
                startNumber = node.markerStartNumber
            )
            is BulletList -> MarkdownBlock.UnorderedList(
                items = processListItems(node)
            )
            is ThematicBreak -> MarkdownBlock.HorizontalRule
            else -> null
        }
    }

    private fun processListItems(list: Node): List<MarkdownBlock.ListItem> {
        val items = mutableListOf<MarkdownBlock.ListItem>()
        var child = list.firstChild
        while (child != null) {
            if (child is ListItem) {
                val firstChild = child.firstChild
                val content = if (firstChild is Paragraph) {
                    flattenInlines(firstChild)
                } else {
                    flattenInlines(child)
                }
                val childBlocks = mutableListOf<MarkdownBlock>()
                var listChild = child.firstChild
                // Skip the first paragraph (already used for content)
                if (listChild is Paragraph) listChild = listChild.next
                while (listChild != null) {
                    processNode(listChild)?.let { childBlocks.add(it) }
                    listChild = listChild.next
                }
                items.add(MarkdownBlock.ListItem(content = content, children = childBlocks))
            }
            child = child.next
        }
        return items
    }

    private fun flattenInlines(node: Node): AnnotatedString {
        return buildAnnotatedString {
            appendInlineChildren(node)
        }
    }

    private fun AnnotatedString.Builder.appendInlineChildren(node: Node) {
        var child = node.firstChild
        while (child != null) {
            appendInline(child)
            child = child.next
        }
    }

    private fun AnnotatedString.Builder.appendInline(node: Node) {
        when (node) {
            is Text -> append(node.literal)
            is SoftLineBreak -> append(" ")
            is HardLineBreak -> append("\n")
            is Code -> {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
                    append(node.literal)
                }
            }
            is Emphasis -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendInlineChildren(node)
                }
            }
            is StrongEmphasis -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendInlineChildren(node)
                }
            }
            is Link -> {
                pushStringAnnotation(tag = "URL", annotation = node.destination)
                withStyle(SpanStyle()) { // Color applied at render time
                    appendInlineChildren(node)
                }
                pop()
            }
            is Image -> {
                append("[image: ${node.title ?: node.destination}]")
            }
            else -> {
                appendInlineChildren(node)
            }
        }
    }
}
