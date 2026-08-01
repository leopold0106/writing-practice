package com.example.writingpractice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.writingpractice.data.repository.ThemeMode

private val LightColors = lightColorScheme(
    primary = InkNavy,
    onPrimary = PaperSurface,
    primaryContainer = InkNavyLight,
    onPrimaryContainer = InkNavyDeep,
    secondary = Amber,
    onSecondary = PaperSurface,
    secondaryContainer = AmberLight,
    onSecondaryContainer = AmberDeep,
    tertiary = Teal,
    onTertiary = PaperSurface,
    tertiaryContainer = TealLight,
    onTertiaryContainer = TealDeep,
    error = Brick,
    onError = PaperSurface,
    errorContainer = BrickLight,
    onErrorContainer = BrickDeep,
    background = Paper,
    onBackground = InkText,
    surface = PaperSurface,
    onSurface = InkText,
    surfaceVariant = PaperVariant,
    onSurfaceVariant = InkTextSoft,
    outline = OutlineWarm,
    outlineVariant = OutlineWarmSoft
)

private val DarkColors = darkColorScheme(
    primary = InkNavyDark,
    onPrimary = InkNavyDeep,
    primaryContainer = InkNavyContainerDark,
    onPrimaryContainer = InkNavyLight,
    secondary = AmberDark,
    onSecondary = AmberDeep,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = AmberLight,
    tertiary = TealDark,
    onTertiary = TealDeep,
    tertiaryContainer = TealContainerDark,
    onTertiaryContainer = TealLight,
    error = BrickDark,
    onError = BrickDeep,
    errorContainer = BrickContainerDark,
    onErrorContainer = BrickLight,
    background = NightBackground,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightTextSoft,
    outline = OutlineNight,
    outlineVariant = OutlineNightSoft
)

@Composable
fun WritingPracticeTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val wpColors = if (darkTheme) DarkWpColors else LightWpColors

    CompositionLocalProvider(LocalWpColors provides wpColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WpTypography,
            shapes = WpShapes,
            content = content
        )
    }
}
