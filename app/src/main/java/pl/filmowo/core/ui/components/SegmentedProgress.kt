package pl.filmowo.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pl.filmowo.core.ui.theme.FilmowoMotion
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme

private const val MAX_SEGMENTS = 20

/**
 * Segmentowy pasek postępu: jeden segment na odcinek, obejrzane w kolorze brand.
 * Powyżej 20 odcinków rysuje 20 segmentów proporcjonalnie – dokładne liczby
 * podaje etykieta, która jest obowiązkowa (segmenty mają niski kontrast z tłem).
 */
@Composable
fun SegmentedProgress(
    done: Int,
    total: Int,
    label: String,
    modifier: Modifier = Modifier,
    trackColor: Color = FilmowoTheme.colors.surfaceSunken,
    labelColor: Color = FilmowoTheme.colors.inkMuted,
) {
    val colors = FilmowoTheme.colors
    val safeTotal = total.coerceAtLeast(1)
    val safeDone = done.coerceIn(0, safeTotal)
    val segments = safeTotal.coerceAtMost(MAX_SEGMENTS)
    val filled = if (safeTotal > MAX_SEGMENTS) Math.round(safeDone.toFloat() / safeTotal * segments) else safeDone
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = label
            progressBarRangeInfo = ProgressBarRangeInfo(safeDone.toFloat(), 0f..safeTotal.toFloat())
        },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(segments) { index ->
                // Segmenty zapalają się falą od lewej – każdy z małym opóźnieniem.
                val color by animateColorAsState(
                    targetValue = if (index < filled) colors.brand else trackColor,
                    animationSpec = tween(FilmowoMotion.MEDIUM, delayMillis = index * 25, easing = FilmowoMotion.Standard),
                    label = "segment",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(color, FilmowoRadius.Pill),
                )
            }
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = labelColor, maxLines = 1)
    }
}
