package pl.filmowo.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.filmowo.R
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme
import pl.filmowo.core.ui.theme.pressScale
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStatus

/** Postęp serialu pod kafelkiem, np. 5 z 8 odcinków z etykietą „S2E5 · 60%”. */
data class TileProgress(val done: Int, val total: Int, val label: String)

/**
 * Pionowy kafelek tytułu do siatki (2 kolumny na telefonie): plakat z oceną
 * i statusem, tytuł do 2 linii, „Film · 2024” i – dla oglądanych seriali – postęp.
 */
@Composable
fun MediaTile(
    title: String,
    mediaType: MediaType,
    year: Int?,
    posterModel: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rating: Int? = null,
    status: WatchStatus? = null,
    progress: TileProgress? = null,
    /** Dodatkowa akcja pod kafelkiem (np. odhaczenie następnego odcinka). */
    footer: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Poster(
            model = posterModel,
            contentDescription = null,
            mediaType = mediaType,
            rating = rating,
            status = status,
            shape = FilmowoRadius.Md,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = FilmowoTheme.colors.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = mediaMeta(mediaType, year),
            style = MaterialTheme.typography.bodySmall,
            color = FilmowoTheme.colors.inkMuted,
        )
        if (progress != null && progress.total > 0) {
            Spacer(Modifier.height(4.dp))
            SegmentedProgress(done = progress.done, total = progress.total, label = progress.label)
        }
        footer?.invoke()
    }
}

/** „Film · 2024” albo „Serial”, gdy rok nieznany. */
@Composable
fun mediaMeta(type: MediaType, year: Int?): String {
    val typeLabel = stringResource(if (type == MediaType.MOVIE) R.string.media_movie else R.string.media_show)
    return if (year != null) "$typeLabel · $year" else typeLabel
}
