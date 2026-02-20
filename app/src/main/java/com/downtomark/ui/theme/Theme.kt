package com.downtomark.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

enum class AppTheme {
    EVERFOREST_DARK,
    EVERFOREST_LIGHT,
    NEWSPRINT
}

val LocalBodyFontFamily = staticCompositionLocalOf { FontFamily.Default }

@Composable
fun DownToMarkTheme(
    appTheme: AppTheme = AppTheme.EVERFOREST_DARK,
    content: @Composable () -> Unit
) {
    val (colorScheme, mdColors, bodyFont) = when (appTheme) {
        AppTheme.EVERFOREST_DARK -> Triple(
            EverforestDarkScheme, EverforestDarkMarkdownColors, FontFamily.Default
        )
        AppTheme.EVERFOREST_LIGHT -> Triple(
            EverforestLightScheme, EverforestLightMarkdownColors, FontFamily.Default
        )
        AppTheme.NEWSPRINT -> Triple(
            NewsprintScheme, NewsprintMarkdownColors, NewsprintBodyFontFamily
        )
    }

    val typography = Typography().withBodyFont(bodyFont)

    CompositionLocalProvider(
        LocalMarkdownColors provides mdColors,
        LocalBodyFontFamily provides bodyFont
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

private fun Typography.withBodyFont(fontFamily: FontFamily): Typography {
    if (fontFamily == FontFamily.Default) return this
    return copy(
        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = bodySmall.copy(fontFamily = fontFamily)
    )
}
