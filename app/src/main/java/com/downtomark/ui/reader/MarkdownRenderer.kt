package com.downtomark.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.downtomark.data.model.Highlight
import com.downtomark.data.model.HighlightColor
import com.downtomark.ui.theme.LocalMarkdownColors
import com.downtomark.util.MarkdownBlock

@Composable
fun RenderBlock(
    block: MarkdownBlock,
    blockIndex: Int,
    highlights: List<Highlight> = emptyList(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    onTextTap: ((Int) -> Unit)? = null,
    onHighlightTap: ((Highlight) -> Unit)? = null
) {
    val mdColors = LocalMarkdownColors.current
    when (block) {
        is MarkdownBlock.Heading -> {
            val style = when (block.level) {
                1 -> MaterialTheme.typography.headlineLarge
                2 -> MaterialTheme.typography.headlineMedium
                3 -> MaterialTheme.typography.headlineSmall
                4 -> MaterialTheme.typography.titleLarge
                5 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            }
            val styledContent = applyLinkColors(block.content, mdColors.linkColor)
            val contentWithHighlights = applyHighlights(styledContent, highlights, mdColors)
            Text(
                text = contentWithHighlights,
                style = style.copy(color = mdColors.headingColor),
                modifier = Modifier.padding(bottom = 8.dp),
                onTextLayout = onTextLayout
            )
        }

        is MarkdownBlock.Paragraph -> {
            val styledContent = applyLinkColors(block.content, mdColors.linkColor)
            val contentWithHighlights = applyHighlights(styledContent, highlights, mdColors)
            Text(
                text = contentWithHighlights,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.padding(bottom = 12.dp),
                onTextLayout = onTextLayout
            )
        }

        is MarkdownBlock.CodeBlock -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(mdColors.codeBg)
                    .padding(12.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = block.code,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = mdColors.codeText,
                        lineHeight = 20.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        is MarkdownBlock.BlockQuote -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            mdColors.blockquoteBar,
                            RoundedCornerShape(2.dp)
                        )
                )
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .background(
                            mdColors.blockquoteBg,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                ) {
                    block.children.forEachIndexed { i, child ->
                        RenderBlock(
                            block = child,
                            blockIndex = blockIndex,
                            highlights = highlights
                        )
                    }
                }
            }
        }

        is MarkdownBlock.OrderedList -> {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                block.items.forEachIndexed { index, item ->
                    Row(modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)) {
                        Text(
                            text = "${block.startNumber + index}. ",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Column {
                            Text(
                                text = applyLinkColors(item.content, mdColors.linkColor),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            item.children.forEach { child ->
                                RenderBlock(block = child, blockIndex = blockIndex)
                            }
                        }
                    }
                }
            }
        }

        is MarkdownBlock.UnorderedList -> {
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                block.items.forEach { item ->
                    Row(modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)) {
                        Text(
                            text = "\u2022 ",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Column {
                            Text(
                                text = applyLinkColors(item.content, mdColors.linkColor),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            item.children.forEach { child ->
                                RenderBlock(block = child, blockIndex = blockIndex)
                            }
                        }
                    }
                }
            }
        }

        is MarkdownBlock.HorizontalRule -> {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = mdColors.hrColor
            )
        }
    }
}

private fun applyLinkColors(
    text: AnnotatedString,
    linkColor: androidx.compose.ui.graphics.Color
): AnnotatedString {
    val urlAnnotations = text.getStringAnnotations("URL", 0, text.length)
    if (urlAnnotations.isEmpty()) return text
    return buildAnnotatedString {
        append(text)
        urlAnnotations.forEach { annotation ->
            addStyle(
                SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                annotation.start,
                annotation.end
            )
        }
    }
}

private fun applyHighlights(
    text: AnnotatedString,
    highlights: List<Highlight>,
    mdColors: com.downtomark.ui.theme.MarkdownColors
): AnnotatedString {
    if (highlights.isEmpty()) return text
    return buildAnnotatedString {
        append(text)
        highlights.forEach { highlight ->
            val start = highlight.startOffset.coerceAtMost(text.length)
            val end = highlight.endOffset.coerceAtMost(text.length)
            if (start < end) {
                val color = when (highlight.color) {
                    HighlightColor.YELLOW -> mdColors.highlightYellow
                    HighlightColor.GREEN -> mdColors.highlightGreen
                    HighlightColor.BLUE -> mdColors.highlightBlue
                    HighlightColor.PINK -> mdColors.highlightPink
                    HighlightColor.ORANGE -> mdColors.highlightOrange
                }
                addStyle(SpanStyle(background = color), start, end)
            }
        }
    }
}
