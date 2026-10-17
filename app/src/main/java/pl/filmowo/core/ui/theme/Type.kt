package pl.filmowo.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import pl.filmowo.R

// Fonty zmienne z res/font (Google Fonts, licencja OFL) – grubość ustawiamy osią wght.
@OptIn(ExperimentalTextApi::class)
private fun bricolage(weight: FontWeight) = Font(
    R.font.bricolage_grotesque,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight), FontVariation.Setting("opsz", 24f)),
)

@OptIn(ExperimentalTextApi::class)
private fun figtree(weight: FontWeight) = Font(
    R.font.figtree,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Bricolage Grotesque – nagłówki, tytuły w kartach i liczby. */
val DisplayFontFamily = FontFamily(bricolage(FontWeight.Medium), bricolage(FontWeight.SemiBold), bricolage(FontWeight.Bold))

/** Figtree – cały pozostały tekst. */
val SansFontFamily =
    FontFamily(figtree(FontWeight.Normal), figtree(FontWeight.Medium), figtree(FontWeight.SemiBold), figtree(FontWeight.Bold))

private fun display(size: Int, line: Int, weight: FontWeight, tracking: Double) = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

private fun sans(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = SansFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
)

// display 40/44 · headline 28/34 · title 20/26 · body-lg 16/24 · body 14/20 · label 14/20 · caption 12/16
internal val FilmowoTypography = Typography(
    displayLarge = display(48, 52, FontWeight.Bold, -0.02),
    displayMedium = display(44, 48, FontWeight.Bold, -0.02),
    displaySmall = display(40, 44, FontWeight.Bold, -0.02),
    headlineLarge = display(32, 38, FontWeight.Bold, -0.015),
    headlineMedium = display(28, 34, FontWeight.Bold, -0.015),
    headlineSmall = display(24, 30, FontWeight.Bold, -0.015),
    titleLarge = display(20, 26, FontWeight.SemiBold, -0.01),
    titleMedium = display(16, 22, FontWeight.SemiBold, -0.01),
    titleSmall = sans(14, 20, FontWeight.SemiBold),
    bodyLarge = sans(16, 24, FontWeight.Normal),
    bodyMedium = sans(14, 20, FontWeight.Normal),
    bodySmall = sans(13, 18, FontWeight.Normal),
    labelLarge = sans(14, 20, FontWeight.SemiBold),
    labelMedium = sans(12, 16, FontWeight.SemiBold, 0.01),
    labelSmall = sans(11, 16, FontWeight.SemiBold, 0.01),
)
