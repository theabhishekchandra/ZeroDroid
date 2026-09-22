package com.abhishek.zerodroid.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Refined Terminal" design tokens (phosphor / dark).
 *
 * New code should use these. The legacy names further down are kept as aliases so
 * existing screens pick up the new palette until each one is migrated.
 */
object ZdColors {
    // Grounds
    val Bg = Color(0xFF0B0D0C)
    val Surface = Color(0xFF121614)
    val Surface2 = Color(0xFF182019)
    val Surface3 = Color(0xFF1E2822)
    val Border = Color(0xFF25302A)
    val BorderStrong = Color(0xFF33413A)
    val Scrim = Color(0x99000000)

    // Text
    val Text = Color(0xFFE6ECE8)
    val Text2 = Color(0xFFA3B0A8)
    val Text3 = Color(0xFF7C8982)

    // Accent
    val Accent = Color(0xFF52E08A)
    val AccentHover = Color(0xFF8AF0B2)
    val OnAccent = Color(0xFF06120B)
    val AccentBg = Color(0xFF13301F)
    val AccentBorder = Color(0xFF1F4A30)

    // Severity: always shown with an icon and a word, never colour alone
    val Critical = Color(0xFFFF6B61)
    val CriticalBg = Color(0xFF3D1714)
    val CriticalBorder = Color(0xFF5A2420)
    val OnCritical = Color(0xFF1A0604)
    val High = Color(0xFFFF8A4C)
    val HighBg = Color(0xFF3A1F10)
    val Medium = Color(0xFFF2B53C)
    val MediumBg = Color(0xFF3A2C10)
    val MediumBorder = Color(0xFF5A4418)
    val Info = Color(0xFF5CC8FF)
    val InfoBg = Color(0xFF0F2C3B)
    val InfoBorder = Color(0xFF1B4458)
    val Violet = Color(0xFFC9A0FF)
}

// ── Legacy aliases, remapped onto the new tokens ─────────────────────────────

val TerminalGreen = ZdColors.Accent
val TerminalGreenDim = Color(0xFF3DAE6C)
val TerminalGreenDark = ZdColors.AccentBg
val TerminalGreenGlow = ZdColors.Accent.copy(alpha = 0.25f)

val TerminalAmber = ZdColors.Medium
val TerminalAmberGlow = ZdColors.Medium.copy(alpha = 0.25f)
val TerminalRed = ZdColors.Critical
val TerminalRedGlow = ZdColors.Critical.copy(alpha = 0.25f)
val TerminalCyan = ZdColors.Info
val TerminalCyanGlow = ZdColors.Info.copy(alpha = 0.25f)
val TerminalBlue = Color(0xFF5C9DFF)

val SeverityCritical = ZdColors.Critical
val SeverityHigh = ZdColors.High
val SeverityMedium = ZdColors.Medium
val SeverityLow = ZdColors.Info
val SeverityInfo = ZdColors.Accent

val BackgroundDark = ZdColors.Bg
val SurfaceDark = ZdColors.Surface
val SurfaceVariantDark = ZdColors.Surface2
val SurfaceElevated = ZdColors.Surface3
val CardBorderGreen = ZdColors.Border
val CardBorderGlow = ZdColors.Accent

val TextPrimary = ZdColors.Text
val TextSecondary = ZdColors.Text2
val TextGreen = ZdColors.Accent
val TextDim = ZdColors.Text3

val GradientGreenStart = ZdColors.Accent
val GradientGreenEnd = ZdColors.Info
val GradientAmberStart = ZdColors.Medium
val GradientAmberEnd = ZdColors.High
