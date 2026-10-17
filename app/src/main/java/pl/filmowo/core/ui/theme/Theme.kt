package pl.filmowo.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalFilmowoColors = staticCompositionLocalOf { LightFilmowoColors }

/** Dostęp do pełnej palety: `FilmowoTheme.colors.peach` itd. */
object FilmowoTheme {
    val colors: FilmowoColors
        @Composable @ReadOnlyComposable
        get() = LocalFilmowoColors.current
}

private fun FilmowoColors.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = ink,
        onPrimary = onInk,
        primaryContainer = lilac,
        onPrimaryContainer = onLilac,
        secondary = brand,
        onSecondary = onBrand,
        secondaryContainer = peach,
        onSecondaryContainer = onPeach,
        tertiary = onButter,
        onTertiary = butter,
        tertiaryContainer = mint,
        onTertiaryContainer = onMint,
        background = surface,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceSunken,
        onSurfaceVariant = inkMuted,
        surfaceContainerLowest = surfaceRaised,
        surfaceContainerLow = surfaceRaised,
        surfaceContainer = surfaceRaised,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceSunken,
        surfaceBright = surfaceRaised,
        surfaceDim = surfaceSunken,
        inverseSurface = inverse,
        inverseOnSurface = onInverse,
        inversePrimary = brand,
        error = danger,
        onError = surfaceRaised,
        errorContainer = dangerSoft,
        onErrorContainer = onDangerSoft,
        outline = lineStrong,
        outlineVariant = line,
        scrim = scrim,
    )
}

/**
 * Motyw aplikacji według design systemu Filmowo. Kolory dynamiczne (z tapety)
 * są wyłączone, bo rozmyłyby stałe role pasteli. Tryb ciemny podąża za systemem.
 */
@Composable
fun FilmowoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkFilmowoColors else LightFilmowoColors
    CompositionLocalProvider(LocalFilmowoColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(darkTheme),
            typography = FilmowoTypography,
            shapes = FilmowoShapes,
            content = content,
        )
    }
}
