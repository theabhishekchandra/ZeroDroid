package com.abhishek.zerodroid.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val ZeroDroidColorScheme = darkColorScheme(
    primary = TerminalGreen,
    onPrimary = BackgroundDark,
    primaryContainer = TerminalGreenDark,
    onPrimaryContainer = TerminalGreen,
    secondary = TerminalCyan,
    onSecondary = BackgroundDark,
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = TerminalCyan,
    tertiary = TerminalAmber,
    onTertiary = BackgroundDark,
    error = TerminalRed,
    onError = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = CardBorderGreen,
    outlineVariant = Color(0xFF2E2E2E)
)

private val ZeroDroidShapes = Shapes(
    extraSmall = CutCornerShape(2.dp),
    small = CutCornerShape(4.dp),
    medium = CutCornerShape(6.dp),
    large = CutCornerShape(8.dp),
    extraLarge = CutCornerShape(12.dp)
)

@Composable
fun ZeroDroidTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = BackgroundDark.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = BackgroundDark.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = ZeroDroidColorScheme,
        typography = Typography,
        shapes = ZeroDroidShapes,
        content = content
    )
}
