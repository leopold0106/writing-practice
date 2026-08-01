package com.example.writingpractice.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Material3 colorScheme이 커버하지 않는 앱 고유 semantic 색상.
 * 라이트/다크 각각의 인스턴스가 [LocalWpColors]로 주입되며,
 * 화면 코드는 `WpTheme.colors.success` 형태로 접근한다.
 */
@Immutable
data class WpColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val scoreHigh: Color,
    val scoreMid: Color,
    val scoreLow: Color,
    val grammar: Color,
    val vocabulary: Color,
    val structure: Color,
    val punctuation: Color,
    val spelling: Color,
    val levelColors: List<Color>
)

val LightWpColors = WpColors(
    success = Color(0xFF2F7D4F),
    successContainer = Color(0xFFDEEFE2),
    onSuccessContainer = Color(0xFF0F3B22),
    warning = Color(0xFF9A6A1B),
    warningContainer = Color(0xFFF6E8CE),
    onWarningContainer = Color(0xFF3C2A05),
    info = Color(0xFF3F638B),
    infoContainer = Color(0xFFE0EAF4),
    onInfoContainer = Color(0xFF16324E),
    scoreHigh = Color(0xFF2F7D4F),
    scoreMid = Color(0xFF9A6A1B),
    scoreLow = Color(0xFFA63D2F),
    grammar = Color(0xFFA65A52),
    vocabulary = Color(0xFF4A7BA6),
    structure = Color(0xFF5B8A6B),
    punctuation = Color(0xFFC08A3E),
    spelling = Color(0xFF8A6B9E),
    levelColors = listOf(
        Color(0xFF6E8DAD),
        Color(0xFF56779B),
        Color(0xFF41658D),
        Color(0xFF3B5B80),
        Color(0xFF7A6A55),
        Color(0xFF9C7E3A),
        Color(0xFFB7791F)
    )
)

val DarkWpColors = WpColors(
    success = Color(0xFF8FCBA4),
    successContainer = Color(0xFF1F4630),
    onSuccessContainer = Color(0xFFDEEFE2),
    warning = Color(0xFFE3C27C),
    warningContainer = Color(0xFF46350F),
    onWarningContainer = Color(0xFFF6E8CE),
    info = Color(0xFF9EC0DF),
    infoContainer = Color(0xFF24425E),
    onInfoContainer = Color(0xFFE0EAF4),
    scoreHigh = Color(0xFF8FCBA4),
    scoreMid = Color(0xFFE3C27C),
    scoreLow = Color(0xFFE8A79C),
    grammar = Color(0xFFD99C94),
    vocabulary = Color(0xFF92B8D9),
    structure = Color(0xFF9BC4A8),
    punctuation = Color(0xFFDDB679),
    spelling = Color(0xFFBFA6D4),
    levelColors = listOf(
        Color(0xFF9AB4CC),
        Color(0xFF86A3C2),
        Color(0xFF7394B5),
        Color(0xFF6D8CAC),
        Color(0xFFB0A186),
        Color(0xFFCBAD69),
        Color(0xFFE5BA5F)
    )
)

val LocalWpColors = staticCompositionLocalOf { LightWpColors }

object WpTheme {
    val colors: WpColors
        @Composable
        @ReadOnlyComposable
        get() = LocalWpColors.current
}
