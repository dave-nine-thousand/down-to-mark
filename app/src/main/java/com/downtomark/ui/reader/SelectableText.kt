package com.downtomark.ui.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.downtomark.data.model.Highlight
import com.downtomark.ui.theme.LocalMarkdownColors
import com.downtomark.util.MarkdownBlock

data class TextSelection(
    val blockIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String
)

@Composable
fun SelectableBlock(
    block: MarkdownBlock,
    blockIndex: Int,
    highlights: List<Highlight>,
    selectionState: SelectionState,
    onSelectionComplete: (TextSelection) -> Unit,
    onHighlightTap: (Highlight) -> Unit,
    modifier: Modifier = Modifier
) {
    // Only paragraph and heading blocks are selectable
    val content = when (block) {
        is MarkdownBlock.Paragraph -> block.content
        is MarkdownBlock.Heading -> block.content
        else -> null
    }

    if (content == null) {
        RenderBlock(block = block, blockIndex = blockIndex, highlights = highlights)
        return
    }

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val mdColors = LocalMarkdownColors.current
    val isHeading = block is MarkdownBlock.Heading

    val style = if (isHeading) {
        val level = (block as MarkdownBlock.Heading).level
        when (level) {
            1 -> MaterialTheme.typography.headlineLarge
            2 -> MaterialTheme.typography.headlineMedium
            3 -> MaterialTheme.typography.headlineSmall
            4 -> MaterialTheme.typography.titleLarge
            5 -> MaterialTheme.typography.titleMedium
            else -> MaterialTheme.typography.titleSmall
        }.copy(color = mdColors.headingColor)
    } else {
        MaterialTheme.typography.bodyLarge.copy(
            lineHeight = 24.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }

    val displayText = buildStyledText(content, highlights, selectionState, blockIndex, mdColors)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = if (isHeading) 8.dp else 12.dp)
            .pointerInput(blockIndex, highlights, selectionState.firstTapOffset) {
                detectTapGestures(
                    onLongPress = { offset ->
                        val layout = textLayoutResult ?: return@detectTapGestures
                        val charOffset = layout.getOffsetForPosition(offset)
                        val (wordStart, wordEnd) = findWordBoundary(content.text, charOffset)
                        selectionState.blockIndex = blockIndex
                        selectionState.firstTapOffset = wordStart
                        selectionState.firstTapEnd = wordEnd
                    },
                    onTap = { offset ->
                        val layout = textLayoutResult ?: return@detectTapGestures
                        val charOffset = layout.getOffsetForPosition(offset)

                        // Check if tapping an existing highlight
                        val tappedHighlight = highlights.find { h ->
                            charOffset in h.startOffset until h.endOffset
                        }
                        if (tappedHighlight != null) {
                            onHighlightTap(tappedHighlight)
                            return@detectTapGestures
                        }

                        // Complete selection if long-press started on this block
                        if (selectionState.blockIndex == blockIndex && selectionState.firstTapOffset != null) {
                            val firstStart = selectionState.firstTapOffset!!
                            val (_, wordEnd) = findWordBoundary(content.text, charOffset)
                            val selStart = minOf(firstStart, charOffset)
                            val selEnd = maxOf(selectionState.firstTapEnd ?: firstStart, wordEnd)
                            val selectedText = content.text.substring(
                                selStart.coerceIn(0, content.text.length),
                                selEnd.coerceIn(0, content.text.length)
                            )
                            if (selectedText.isNotBlank()) {
                                onSelectionComplete(
                                    TextSelection(
                                        blockIndex = blockIndex,
                                        startOffset = selStart,
                                        endOffset = selEnd,
                                        selectedText = selectedText
                                    )
                                )
                            }
                            selectionState.reset()
                        } else {
                            // Tap with no active selection — clear any stale state
                            selectionState.reset()
                        }
                    }
                )
            }
    ) {
        Text(
            text = displayText,
            style = style,
            onTextLayout = { textLayoutResult = it }
        )
    }
}

class SelectionState {
    var blockIndex by mutableStateOf(-1)
    var firstTapOffset by mutableStateOf<Int?>(null)
    var firstTapEnd by mutableStateOf<Int?>(null)

    fun reset() {
        blockIndex = -1
        firstTapOffset = null
        firstTapEnd = null
    }
}

private fun findWordBoundary(text: String, offset: Int): Pair<Int, Int> {
    if (text.isEmpty() || offset < 0 || offset >= text.length) {
        return offset to offset
    }
    var start = offset
    var end = offset
    while (start > 0 && !text[start - 1].isWhitespace()) start--
    while (end < text.length && !text[end].isWhitespace()) end++
    return start to end
}

private fun buildStyledText(
    content: AnnotatedString,
    highlights: List<Highlight>,
    selectionState: SelectionState,
    blockIndex: Int,
    mdColors: com.downtomark.ui.theme.MarkdownColors
): AnnotatedString {
    return buildAnnotatedString {
        append(content)

        // Apply link colors
        content.getStringAnnotations("URL", 0, content.length).forEach { annotation ->
            addStyle(
                SpanStyle(
                    color = mdColors.linkColor,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                ),
                annotation.start,
                annotation.end
            )
        }

        // Apply saved highlights
        highlights.forEach { highlight ->
            val start = highlight.startOffset.coerceAtMost(content.length)
            val end = highlight.endOffset.coerceAtMost(content.length)
            if (start < end) {
                val color = when (highlight.color) {
                    com.downtomark.data.model.HighlightColor.YELLOW -> mdColors.highlightYellow
                    com.downtomark.data.model.HighlightColor.GREEN -> mdColors.highlightGreen
                    com.downtomark.data.model.HighlightColor.BLUE -> mdColors.highlightBlue
                    com.downtomark.data.model.HighlightColor.PINK -> mdColors.highlightPink
                    com.downtomark.data.model.HighlightColor.ORANGE -> mdColors.highlightOrange
                }
                addStyle(SpanStyle(background = color), start, end)
            }
        }

        // Apply long-press selection preview
        if (selectionState.blockIndex == blockIndex && selectionState.firstTapOffset != null) {
            val start = selectionState.firstTapOffset!!.coerceAtMost(content.length)
            val end = (selectionState.firstTapEnd ?: start).coerceAtMost(content.length)
            if (start < end) {
                addStyle(
                    SpanStyle(
                        background = mdColors.highlightBlue.copy(alpha = 0.5f)
                    ),
                    start,
                    end
                )
            }
        }
    }
}
