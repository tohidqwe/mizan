package com.mizan.civilleitner.product

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LuxuryAcademicDark = darkColorScheme(
    primary = Color(0xFFD4AF37),
    onPrimary = Color(0xFF15120A),
    secondary = Color(0xFF5CA89A),
    onSecondary = Color(0xFF071D18),
    background = Color(0xFF0C1118),
    onBackground = Color(0xFFF1EEE6),
    surface = Color(0xFF141B24),
    onSurface = Color(0xFFF1EEE6),
    surfaceVariant = Color(0xFF1D2733),
    onSurfaceVariant = Color(0xFFC8D1DC),
    error = Color(0xFFFF6B6B),
)

@Composable
fun ProductTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LuxuryAcademicDark,
        content = content,
    )
}
