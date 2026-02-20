package com.downtomark.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class MarkdownColors(
    val codeBg: Color,
    val codeText: Color,
    val blockquoteBar: Color,
    val blockquoteBg: Color,
    val linkColor: Color,
    val headingColor: Color,
    val highlightYellow: Color,
    val highlightGreen: Color,
    val highlightBlue: Color,
    val highlightPink: Color,
    val highlightOrange: Color,
    val hrColor: Color
)

val LocalMarkdownColors = staticCompositionLocalOf {
    MarkdownColors(
        codeBg = Color.Gray,
        codeText = Color.White,
        blockquoteBar = Color.Gray,
        blockquoteBg = Color.Transparent,
        linkColor = Color.Blue,
        headingColor = Color.White,
        highlightYellow = Color.Yellow.copy(alpha = 0.3f),
        highlightGreen = Color.Green.copy(alpha = 0.3f),
        highlightBlue = Color.Blue.copy(alpha = 0.3f),
        highlightPink = Color.Magenta.copy(alpha = 0.3f),
        highlightOrange = Color(0xFFFFA500).copy(alpha = 0.3f),
        hrColor = Color.Gray
    )
}
