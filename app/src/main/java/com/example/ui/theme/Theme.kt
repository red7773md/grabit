package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GrabItCoralLight,
    onPrimary = Color.White,
    primaryContainer = GrabItCoralDark,
    onPrimaryContainer = Color(0xFFFFD2D7),
    secondary = GrabItIndigoLight,
    onSecondary = Color.White,
    secondaryContainer = GrabItIndigoDark,
    onSecondaryContainer = Color(0xFFDCDDFF),
    tertiary = GrabItTealLight,
    onTertiary = Color(0xFF00381B),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = GrabItCoral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE5E8),
    onPrimaryContainer = GrabItCoralDark,
    secondary = GrabItIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8FF),
    onSecondaryContainer = GrabItIndigoDark,
    tertiary = GrabItTeal,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep distinct brand identity for GrabIt
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
