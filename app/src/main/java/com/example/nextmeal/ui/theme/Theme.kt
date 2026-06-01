package com.example.nextmeal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GreenMain,
    secondary = AccentTeal,

    background = GreenDark,
    surface = GreenDarkest,

    onPrimary = TextLight,
    onSecondary = TextLight,

    onBackground = TextLight,
    onSurface = TextLight
)

private val LightColorScheme = lightColorScheme(
    primary = GreenMain,
    secondary = AccentTeal,
    tertiary = GreenDark,

    background = GreenLight,
    surface = SurfaceWhite,

    onPrimary = TextLight,
    onSecondary = TextLight,

    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun NextMealTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (darkTheme) DarkColorScheme
        else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}