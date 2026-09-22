package com.abhishek.zerodroid.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val ZeroDroidColorScheme = darkColorScheme(
    primary = ZdColors.Accent,
    onPrimary = ZdColors.OnAccent,
    primaryContainer = ZdColors.AccentBg,
    onPrimaryContainer = ZdColors.Accent,
    secondary = ZdColors.Info,
    onSecondary = ZdColors.Bg,
    secondaryContainer = ZdColors.InfoBg,
    onSecondaryContainer = ZdColors.Info,
    tertiary = ZdColors.Medium,
    onTertiary = ZdColors.Bg,
    tertiaryContainer = ZdColors.MediumBg,
    onTertiaryContainer = ZdColors.Medium,
    error = ZdColors.Critical,
    onError = ZdColors.OnCritical,
    errorContainer = ZdColors.CriticalBg,
    onErrorContainer = ZdColors.Critical,
    background = ZdColors.Bg,
    onBackground = ZdColors.Text,
    surface = ZdColors.Surface,
    onSurface = ZdColors.Text,
    surfaceVariant = ZdColors.Surface2,
    onSurfaceVariant = ZdColors.Text2,
    surfaceContainerLowest = ZdColors.Bg,
    surfaceContainerLow = ZdColors.Surface,
    surfaceContainer = ZdColors.Surface,
    surfaceContainerHigh = ZdColors.Surface2,
    surfaceContainerHighest = ZdColors.Surface3,
    outline = ZdColors.BorderStrong,
    outlineVariant = ZdColors.Border,
    scrim = ZdColors.Scrim
)

private val ZeroDroidShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(22.dp)
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
