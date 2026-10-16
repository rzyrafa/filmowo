package pl.filmowo.feature.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.filmowo.R
import pl.filmowo.core.ui.components.Poster
import pl.filmowo.data.remote.TmdbImages
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailsTopBar(title: String?, onBack: () -> Unit) {
    TopAppBar(
        title = {
            if (title != null) Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
            }
        },
    )
}

/** Plakat z tytułem i krótkimi informacjami (typ, rok, czas trwania, gatunki). */
@Composable
internal fun DetailsHeader(
    title: String,
    originalTitle: String?,
    posterModel: Any?,
    metaLines: List<String>,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Poster(
            model = posterModel,
            contentDescription = stringResource(R.string.poster_description, title),
            modifier = Modifier.width(128.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            if (originalTitle != null) {
                Text(
                    originalTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            metaLines.filter { it.isNotBlank() }.forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun OverviewSection(overview: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.details_overview), style = MaterialTheme.typography.titleMedium)
        Text(
            text = overview.ifBlank { stringResource(R.string.details_no_overview) },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
internal fun formatRuntime(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> stringResource(R.string.runtime_minutes, rest)
        rest == 0 -> stringResource(R.string.runtime_hours, hours)
        else -> stringResource(R.string.runtime_hours_minutes, hours, rest)
    }
}

/** Plakat zapisany lokalnie (offline) ma pierwszeństwo przed adresem z TMDB. */
internal fun posterModel(entry: LibraryEntryState?, posterPath: String?): Any? =
    entry?.localPosterFile?.let(::File) ?: TmdbImages.posterLarge(posterPath)
