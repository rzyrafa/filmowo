package pl.filmowo.feature.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.filmowo.R
import pl.filmowo.core.ui.components.MediaTile
import pl.filmowo.core.ui.components.Poster
import pl.filmowo.core.ui.components.filmowoTopBarColors
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme
import pl.filmowo.data.remote.TmdbImages
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.SearchResult
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
        colors = filmowoTopBarColors(),
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
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
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
        Text(stringResource(R.string.details_overview), style = MaterialTheme.typography.titleLarge)
        Text(
            text = overview.ifBlank { stringResource(R.string.details_no_overview) },
            style = MaterialTheme.typography.bodyLarge,
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

/** Notatka użytkownika – tylko dla pozycji z biblioteki; edycja w oknie dialogowym. */
@Composable
internal fun NoteSection(note: String?, onSave: (String?) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    val colors = FilmowoTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.note_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { editing = true }) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text(stringResource(if (note == null) R.string.note_add else R.string.note_edit))
            }
        }
        Text(
            text = note ?: stringResource(R.string.note_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = if (note == null) colors.inkMuted else colors.ink,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceRaised, FilmowoRadius.Md)
                .padding(16.dp),
        )
    }
    if (editing) {
        var text by rememberSaveable { mutableStateOf(note.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(R.string.note_title)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= NOTE_MAX_LENGTH) text = it },
                    label = { Text(stringResource(R.string.note_hint)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    editing = false
                    onSave(text)
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

private const val NOTE_MAX_LENGTH = 2000

/** Pozioma lista polecanych tytułów; ukryta, gdy nic nie ma. */
@Composable
internal fun RecommendationsSection(items: List<SearchResult>, onOpen: (id: Int, type: MediaType) -> Unit) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.details_recommendations), style = MaterialTheme.typography.titleLarge)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { "${it.mediaType}-${it.id}" }) { item ->
                MediaTile(
                    title = item.title,
                    mediaType = item.mediaType,
                    year = item.year,
                    posterModel = TmdbImages.posterSmall(item.posterPath),
                    onClick = { onOpen(item.id, item.mediaType) },
                    modifier = Modifier.width(120.dp),
                )
            }
        }
    }
}
