package pl.filmowo.feature.season

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.filmowo.R
import pl.filmowo.core.ui.UiState
import pl.filmowo.core.ui.components.ErrorContent
import pl.filmowo.core.ui.components.LoadingContent
import pl.filmowo.core.ui.components.PlaceholderContent
import pl.filmowo.core.ui.components.SegmentedProgress
import pl.filmowo.core.ui.components.StateTransition
import pl.filmowo.core.ui.components.WatchedCheck
import pl.filmowo.core.ui.theme.FilmowoMotion
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme
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
        StateTransition(state.episodes, contentKey = { it::class }) { episodes ->
            when (episodes) {
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
}

@Composable
private fun EpisodeList(
    rows: List<EpisodeRow>,
    state: SeasonUiState,
    onEpisodeWatchedChange: (Int, Boolean) -> Unit,
    onSeasonWatchedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "header") {
            SeasonHeader(state, onSeasonWatchedChange)
        }
        items(rows, key = { it.info.episodeNumber }) { row ->
            EpisodeItem(
                modifier = Modifier.animateItem(),
                row = row,
                enabled = state.isTracked && row.isAired,
                onWatchedChange = { onEpisodeWatchedChange(row.info.episodeNumber, it) },
            )
        }
    }
}

@Composable
private fun SeasonHeader(state: SeasonUiState, onSeasonWatchedChange: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val progress = state.progress
        when {
            state.isTracked && progress != null -> {
                if (progress.totalCount > 0) {
                    SegmentedProgress(
                        done = progress.watchedCount,
                        total = progress.totalCount,
                        label = stringResource(R.string.season_progress, progress.watchedCount, progress.totalCount),
                    )
                }
                if (progress.airedCount > 0) {
                    val colors = FilmowoTheme.colors
                    Button(
                        onClick = { onSeasonWatchedChange(!progress.isComplete) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.peach, contentColor = colors.onPeach),
                    ) {
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

/** Wiersz odcinka jako biała karta; po prawej pole odhaczenia – miętowe po obejrzeniu. */
@Composable
private fun EpisodeItem(
    row: EpisodeRow,
    enabled: Boolean,
    onWatchedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FilmowoTheme.colors
    val titleColor by animateColorAsState(if (row.isWatched) colors.inkMuted else colors.ink, FilmowoMotion.color(), label = "episodeTitle")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(FilmowoRadius.Md)
            .background(colors.surfaceRaised)
            .toggleable(value = row.isWatched, enabled = enabled, role = Role.Checkbox, onValueChange = onWatchedChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val number = row.info.episodeNumber
            Text(
                text = row.info.name?.let { stringResource(R.string.episode_title, number, it) }
                    ?: stringResource(R.string.episode_number, number),
                style = MaterialTheme.typography.titleSmall,
                color = titleColor,
            )
            val meta = listOfNotNull(
                row.info.airDate?.let {
                    if (row.isAired) it else stringResource(R.string.episode_premiere, it)
                } ?: stringResource(R.string.episode_no_date),
                row.info.runtimeMinutes?.let { formatRuntime(it) },
            ).joinToString(" · ")
            Text(meta, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
            if (row.info.overview.isNotBlank()) {
                Text(
                    row.info.overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        WatchedCheck(checked = row.isWatched, enabled = enabled)
    }
}
