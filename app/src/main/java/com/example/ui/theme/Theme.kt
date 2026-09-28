package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = DroidGreen,
    secondary = DroidGreenDark,
    tertiary = Blue40,
    background = BgColor,
    surface = OffWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = PureWhite,
    outline = CardBorderColor
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // We want to force a beautiful clean pure white interface as requested by user
    dynamicColor: Boolean = false, // Force our premium curated color palette
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
