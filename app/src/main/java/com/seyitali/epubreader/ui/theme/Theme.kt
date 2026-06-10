package com.seyitali.epubreader.ui.theme

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

private val LightColors = lightColorScheme(
    primary = Color(0xFF4A5C92),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDBE1FF),
    secondary = Color(0xFF585E72),
    secondaryContainer = Color(0xFFDDE1F9),
    tertiary = Color(0xFF745471),
    background = Color(0xFFFAF8FF),
    surface = Color(0xFFFAF8FF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB3C5FF),
    onPrimary = Color(0xFF1A2D60),
    primaryContainer = Color(0xFF324478),
    secondary = Color(0xFFC1C6DD),
    secondaryContainer = Color(0xFF404659),
    tertiary = Color(0xFFE2BBDC),
    background = Color(0xFF121318),
    surface = Color(0xFF121318)
)

/**
 * @param themeMode 0 = Sistem, 1 = Açık, 2 = Koyu
 */
@Composable
fun EpubReaderTheme(themeMode: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    // Android 12+ için Material You dinamik renkler
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) DarkColors else LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
