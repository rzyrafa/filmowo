package pl.filmowo.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Promienie z design systemu: sm 8 · md 16 · lg 24 · xl 32 · pill. */
object FilmowoRadius {
    val Sm = RoundedCornerShape(8.dp)
    val Md = RoundedCornerShape(16.dp)
    val Lg = RoundedCornerShape(24.dp)
    val Xl = RoundedCornerShape(32.dp)
    val Pill = RoundedCornerShape(percent = 50)
}

/** Siatka 4dp: space-1 … space-8. */
object FilmowoSpacing {
    val S1 = 4.dp
    val S2 = 8.dp
    val S3 = 12.dp
    val S4 = 16.dp
    val S6 = 24.dp
    val S8 = 32.dp
}

internal val FilmowoShapes = Shapes(
    extraSmall = FilmowoRadius.Sm,
    small = FilmowoRadius.Md,
    medium = FilmowoRadius.Lg,
    large = FilmowoRadius.Lg,
    extraLarge = FilmowoRadius.Xl,
)
