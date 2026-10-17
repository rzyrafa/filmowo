package pl.filmowo.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Pełna paleta Filmowo (design system „Filmowo”). Pastele zawsze w parze
 * z własnym kolorem tekstu `on…`, więc ta sama para działa w obu motywach.
 */
@Immutable
data class FilmowoColors(
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,
    val line: Color,
    val lineStrong: Color,
    val ink: Color,
    val inkMuted: Color,
    val onInk: Color,
    val inverse: Color,
    val inverseRaised: Color,
    val onInverse: Color,
    val onInverseMuted: Color,
    val brand: Color,
    val onBrand: Color,
    val peach: Color,
    val onPeach: Color,
    val sky: Color,
    val onSky: Color,
    val lilac: Color,
    val onLilac: Color,
    val butter: Color,
    val onButter: Color,
    val mint: Color,
    val onMint: Color,
    val danger: Color,
    val dangerSoft: Color,
    val onDangerSoft: Color,
    val scrim: Color,
)

internal val LightFilmowoColors = FilmowoColors(
    surface = Color(0xFFEBE8E1),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFDEDAD2),
    line = Color(0xFFD6D1C8),
    lineStrong = Color(0xFF7F7987),
    ink = Color(0xFF1F1C27),
    inkMuted = Color(0xFF5A5563),
    onInk = Color(0xFFFFFFFF),
    inverse = Color(0xFF1F1C27),
    inverseRaised = Color(0xFF2E2A39),
    onInverse = Color(0xFFF6F3EE),
    onInverseMuted = Color(0xFFAAA4B5),
    brand = Color(0xFFFF7F5C),
    onBrand = Color(0xFF1F1C27),
    peach = Color(0xFFFFC6B0),
    onPeach = Color(0xFF6E2B10),
    sky = Color(0xFFB8D5F5),
    onSky = Color(0xFF173F68),
    lilac = Color(0xFFD1C3F6),
    onLilac = Color(0xFF3F2E7A),
    butter = Color(0xFFF8DA7F),
    onButter = Color(0xFF594200),
    mint = Color(0xFFADE3C8),
    onMint = Color(0xFF174D36),
    danger = Color(0xFFB3321F),
    dangerSoft = Color(0xFFFDE1DC),
    onDangerSoft = Color(0xFF8A2414),
    scrim = Color(0x661F1C27),
)

internal val DarkFilmowoColors = FilmowoColors(
    surface = Color(0xFF121117),
    surfaceRaised = Color(0xFF1D1C24),
    surfaceSunken = Color(0xFF0B0A0F),
    line = Color(0xFF2E2C36),
    lineStrong = Color(0xFF6C6877),
    ink = Color(0xFFF3F1F6),
    inkMuted = Color(0xFFA8A3B2),
    onInk = Color(0xFF14131A),
    inverse = Color(0xFF2A2833),
    inverseRaised = Color(0xFF383541),
    onInverse = Color(0xFFF3F1F6),
    onInverseMuted = Color(0xFFB3AEBD),
    brand = Color(0xFFFF9F85),
    onBrand = Color(0xFF1F1C27),
    peach = Color(0xFF5A3326),
    onPeach = Color(0xFFFFCDB9),
    sky = Color(0xFF213A58),
    onSky = Color(0xFFBCD8F6),
    lilac = Color(0xFF382E57),
    onLilac = Color(0xFFD6CAF8),
    butter = Color(0xFF4A3E18),
    onButter = Color(0xFFF6E09A),
    mint = Color(0xFF22493A),
    onMint = Color(0xFFB5EAD0),
    danger = Color(0xFFFF9C8A),
    dangerSoft = Color(0xFF4A1C16),
    onDangerSoft = Color(0xFFFFC2B8),
    scrim = Color(0x99000000),
)

/** Pastelowy ton: wypełnienie i kolor treści na nim. */
@Immutable
data class Tone(val container: Color, val content: Color)

enum class ToneName { PEACH, SKY, LILAC, BUTTER, MINT, BRAND, INVERSE, NEUTRAL }

fun FilmowoColors.tone(name: ToneName): Tone = when (name) {
    ToneName.PEACH -> Tone(peach, onPeach)
    ToneName.SKY -> Tone(sky, onSky)
    ToneName.LILAC -> Tone(lilac, onLilac)
    ToneName.BUTTER -> Tone(butter, onButter)
    ToneName.MINT -> Tone(mint, onMint)
    ToneName.BRAND -> Tone(brand, onBrand)
    ToneName.INVERSE -> Tone(inverse, onInverse)
    ToneName.NEUTRAL -> Tone(surfaceSunken, inkMuted)
}
