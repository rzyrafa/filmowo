package pl.filmowo.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.filmowo.R
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme
import pl.filmowo.core.ui.theme.ToneName
import pl.filmowo.core.ui.theme.tone
import pl.filmowo.domain.model.WatchStatus

/** Stały kolor każdego statusu: Oglądam – lilac, Do obejrzenia – butter, Obejrzane – mint. */
val WatchStatus.toneName: ToneName
    get() = when (this) {
        WatchStatus.WATCHING -> ToneName.LILAC
        WatchStatus.PLANNED -> ToneName.BUTTER
        WatchStatus.WATCHED -> ToneName.MINT
    }

@Composable
fun WatchStatus.label(): String = stringResource(
    when (this) {
        WatchStatus.WATCHING -> R.string.library_tab_watching
        WatchStatus.PLANNED -> R.string.library_tab_planned
        WatchStatus.WATCHED -> R.string.library_tab_watched
    },
)

/**
 * Pastelowy znacznik. Na obrazie ([onImage]) staje się białą pigułką z cieniem
 * i kropką w kolorze tonu, żeby był czytelny na dowolnym kadrze.
 */
@Composable
fun Badge(
    text: String,
    tone: ToneName,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    onImage: Boolean = false,
) {
    val colors = FilmowoTheme.colors
    val t = colors.tone(tone)
    val base = if (onImage) {
        modifier.shadow(4.dp, FilmowoRadius.Pill).background(colors.surfaceRaised, FilmowoRadius.Pill)
    } else {
        modifier.background(t.container, FilmowoRadius.Pill)
    }
    Row(
        modifier = base
            .height(24.dp)
            .padding(horizontal = if (onImage) 8.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            iconRes != null -> Icon(
                painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = t.content,
            )
            onImage -> Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(t.content),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (onImage) colors.ink else t.content,
            maxLines = 1,
        )
    }
}

@Composable
fun StatusBadge(status: WatchStatus, modifier: Modifier = Modifier, onImage: Boolean = false) {
    Badge(
        text = status.label(),
        tone = status.toneName,
        modifier = modifier,
        iconRes = if (status == WatchStatus.WATCHED) R.drawable.ic_check else null,
        onImage = onImage,
    )
}
