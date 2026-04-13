package com.example.apzolife.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = ApzoPrimary,
    onPrimary = Color.White,
    primaryContainer = ApzoPrimaryContainer,
    onPrimaryContainer = OnApzoPrimaryContainer,
    secondary = ApzoSecondary,
    onSecondary = Color.White,
    secondaryContainer = ApzoSecondaryContainer,
    onSecondaryContainer = OnApzoSecondaryContainer,
    tertiary = ApzoTertiary,
    onTertiary = Color.White,
    tertiaryContainer = ApzoTertiaryContainer,
    onTertiaryContainer = OnApzoTertiaryContainer,
    // Slightly warmer background for depth
    background = Color(0xFFF2F6F9),
    onBackground = Color(0xFF0A1628),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A1628),
    surfaceVariant = Color(0xFFEBF2F6),
    onSurfaceVariant = Color(0xFF3A5260),
    outline = Color(0xFF8FA5B2),
    outlineVariant = Color(0xFFCDD8DE),
    error = ApzoError,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = ApzoPrimaryDarkTheme,
    onPrimary = ApzoOnPrimaryDark,
    primaryContainer = ApzoPrimaryDark,
    onPrimaryContainer = ApzoPrimaryContainer,
    secondary = Color(0xFF9ECFDF),
    onSecondary = Color(0xFF003546),
    secondaryContainer = Color(0xFF1A3F50),
    onSecondaryContainer = ApzoSecondaryContainer,
    tertiary = Color(0xFF80DFCB),
    onTertiary = Color(0xFF003829),
    tertiaryContainer = Color(0xFF00503D),
    onTertiaryContainer = ApzoTertiaryContainer,
    background = Color(0xFF0A1628),
    onBackground = Color(0xFFD8EBF2),
    surface = Color(0xFF1A2F38),
    onSurface = Color(0xFFD8EBF2),
    surfaceVariant = Color(0xFF1E3A47),
    onSurfaceVariant = Color(0xFFAFC8D5),
    outline = Color(0xFF52717E),
    outlineVariant = Color(0xFF2A4555),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

object ThemeManager {
    var isDarkMode by mutableStateOf(false)

    fun toggle() {
        isDarkMode = !isDarkMode
    }

    fun setDark(dark: Boolean) {
        isDarkMode = dark
    }
}

@Composable
fun ApzoLifeTheme(
    darkTheme: Boolean = ThemeManager.isDarkMode,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ApzoTypography,
        content = content
    )
}