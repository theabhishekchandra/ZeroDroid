package com.abhishek.zerodroid.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.abhishek.zerodroid.R

/** Identity and data: titles, labels, values, addresses, dBm. */
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_light, FontWeight.Light),
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)

/** Anything people read as sentences: explanations, guidance, why something was flagged. */
val PlexSans = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold)
)

/** Named styles from the design system board, for components that need an exact match. */
object ZdType {
    val Display = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp)
    val Title = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.3).sp)
    val Heading = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp)
    val Label = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp)
    val Mono = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
    val Section = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.4.sp)
    val Path = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.3.sp)
    val Tag = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.6.sp)
    val Button = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp)
    val Body = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    val BodySmall = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp)
    val Caption = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp)
}

val Typography = Typography(
    displayLarge = ZdType.Display,
    displayMedium = ZdType.Display.copy(fontSize = 26.sp, lineHeight = 32.sp),
    displaySmall = ZdType.Display.copy(fontSize = 22.sp, lineHeight = 28.sp),
    headlineLarge = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineMedium = ZdType.Title,
    headlineSmall = ZdType.Heading.copy(fontSize = 18.sp),
    titleLarge = ZdType.Heading.copy(fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp),
    titleSmall = ZdType.Label,
    bodyLarge = ZdType.Body,
    bodyMedium = TextStyle(fontFamily = PlexSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = ZdType.Caption,
    labelLarge = ZdType.Button,
    labelMedium = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)
