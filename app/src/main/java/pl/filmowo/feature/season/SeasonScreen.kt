package pl.filmowo.feature.season

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.filmowo.R
import pl.filmowo.core.ui.UiState
import pl.filmowo.core.ui.components.ErrorContent
import pl.filmowo.core.ui.components.LoadingContent
import pl.filmowo.core.ui.components.PlaceholderContent
import pl.filmowo.feature.details.DetailsTopBar
import pl.filmowo.feature.details.formatRuntime

@Composable
fun SeasonScreen(
    title: String,
    onBack: () -> Unit,
    viewModel: SeasonViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { DetailsTopBar(title, onBack) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (val episodes = state.episodes) {
            UiState.Loading -> LoadingContent(modifier)
            is UiState.Error -> ErrorContent(episodes.error, viewModel::retry, modifier)
            is UiState.Success -> if (episodes.data.isEmpty()) {
                PlaceholderContent(R.drawable.ic_movie, stringResource(R.string.season_no_episodes), modifier)
            } else {
                EpisodeList(
                    rows = episodes.data,
                    state = state,
                    onEpisodeWatchedChange = viewModel::onEpisodeWatchedChange,
                    onSeasonWatchedChange = viewModel::onSeasonWatchedChange,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun EpisodeList(
    rows: List<EpisodeRow>,
    state: SeasonUiState,
    onEpisodeWatchedChange: (Int, Boolean) -> Unit,
    onSeasonWatchedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize()) {
        item(key = "header") {
            SeasonHeader(state, onSeasonWatchedChange)
            HorizontalDivider()
        }
        items(rows, key = { it.info.episodeNumber }) { row ->
            EpisodeItem(
                row = row,
                enabled = state.isTracked && row.isAired,
                onWatchedChange = { onEpisodeWatchedChange(row.info.episodeNumber, it) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun SeasonHeader(state: SeasonUiState, onSeasonWatchedChange: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val progress = state.progress
        when {
            state.isTracked && progress != null -> {
                Text(
                    stringResource(R.string.season_progress, progress.watchedCount, progress.totalCount),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (progress.totalCount > 0) {
                    LinearProgressIndicator(
                        progress = { progress.watchedCount.toFloat() / progress.totalCount },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (progress.airedCount > 0) {
                    FilledTonalButton(onClick = { onSeasonWatchedChange(!progress.isComplete) }) {
                        Text(
                            stringResource(
                                if (progress.isComplete) R.string.season_unmark_all else R.string.season_mark_all,
                            ),
                        )
                    }
                }
            }
            state.inLibrary -> Text(
                stringResource(R.string.tracking_syncing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> Text(
                stringResource(R.string.tracking_requires_library),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EpisodeItem(
    row: EpisodeRow,
    enabled: Boolean,
    onWatchedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onWatchedChange(!row.isWatched) }
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val number = row.info.episodeNumber
            Text(
                text = row.info.name?.let { stringResource(R.string.episode_title, number, it) }
                    ?: stringResource(R.string.episode_number, number),
                style = MaterialTheme.typography.titleSmall,
            )
            val meta = listOfNotNull(
                row.info.airDate?.let {
                    if (row.isAired) it else stringResource(R.string.episode_premiere, it)
                } ?: stringResource(R.string.episode_no_date),
                row.info.runtimeMinutes?.let { formatRuntime(it) },
            ).joinToString(" · ")
            Text(
                meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (row.info.overview.isNotBlank()) {
                Text(
                    row.info.overview,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Checkbox(
            checked = row.isWatched,
            onCheckedChange = onWatchedChange,
            enabled = enabled,
        )
    }
}
