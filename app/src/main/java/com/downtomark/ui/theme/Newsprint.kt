package com.downtomark.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

val NewsprintScheme = lightColorScheme(
    primary = Color(0xFF8B4513),
    onPrimary = Color(0xFFFAF8F0),
    primaryContainer = Color(0xFFE8E0D0),
    onPrimaryContainer = Color(0xFF2C2C2C),
    secondary = Color(0xFF5B7065),
    onSecondary = Color(0xFFFAF8F0),
    secondaryContainer = Color(0xFFE8E0D0),
    onSecondaryContainer = Color(0xFF5B7065),
    tertiary = Color(0xFF6B4C71),
    onTertiary = Color(0xFFFAF8F0),
    background = Color(0xFFFAF8F0),
    onBackground = Color(0xFF2C2C2C),
    surface = Color(0xFFF2EEE4),
    onSurface = Color(0xFF2C2C2C),
    surfaceVariant = Color(0xFFE8E0D0),
    onSurfaceVariant = Color(0xFF6B6B6B),
    outline = Color(0xFFAAAAAA),
    error = Color(0xFFC62828),
    onError = Color(0xFFFAF8F0)
)

val NewsprintBodyFontFamily = FontFamily.Serif

val NewsprintMarkdownColors = MarkdownColors(
    codeBg = Color(0xFFEDE8DC),
    codeText = Color(0xFF6B4C71),
    blockquoteBar = Color(0xFF8B4513),
    blockquoteBg = Color(0xFFF5F0E5),
    linkColor = Color(0xFF1A5276),
    headingColor = Color(0xFF2C2C2C),
    highlightYellow = Color(0xFFFFD700).copy(alpha = 0.25f),
    highlightGreen = Color(0xFF5B7065).copy(alpha = 0.25f),
    highlightBlue = Color(0xFF1A5276).copy(alpha = 0.25f),
    highlightPink = Color(0xFF6B4C71).copy(alpha = 0.25f),
    highlightOrange = Color(0xFFCC5500).copy(alpha = 0.25f),
    hrColor = Color(0xFFCCCCCC)
)
