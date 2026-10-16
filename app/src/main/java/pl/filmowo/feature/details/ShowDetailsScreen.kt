package pl.filmowo.feature.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.filmowo.R
import pl.filmowo.core.ui.UiState
import pl.filmowo.core.ui.components.ErrorContent
import pl.filmowo.core.ui.components.LoadingContent
import pl.filmowo.core.ui.components.messageRes
import pl.filmowo.core.ui.components.progressLabel
import pl.filmowo.domain.model.SeasonProgress
import pl.filmowo.domain.model.ShowDetails

@Composable
fun ShowDetailsScreen(
    onBack: () -> Unit,
    onOpenSeason: (showId: Int, seasonNumber: Int, title: String) -> Unit,
    viewModel: ShowDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tracking by viewModel.tracking.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val refreshError = tracking.refreshError
    val refreshErrorText = refreshError?.let {
        stringResource(R.string.refresh_failed, stringResource(it.messageRes))
    }
    LaunchedEffect(refreshErrorText) {
        if (refreshErrorText != null) {
            snackbarHostState.showSnackbar(refreshErrorText)
            viewModel.onRefreshErrorShown()
        }
    }

    Scaffold(
        topBar = { DetailsTopBar((state.content as? UiState.Success)?.data?.title, onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (val content = state.content) {
            UiState.Loading -> LoadingContent(modifier)
            is UiState.Error -> ErrorContent(content.error, viewModel::retry, modifier)
            is UiState.Success -> ShowDetailsContent(
                show = content.data,
                entry = state.library,
                tracking = tracking,
                viewModel = viewModel,
                onOpenSeason = { number, title -> onOpenSeason(content.data.id, number, title) },
                modifier = modifier,
            )
        }
    }
}

/** Wiersz sezonu do wyświetlenia – z danych lokalnych (z postępem) albo z TMDB. */
private data class SeasonRow(
    val seasonNumber: Int,
    val name: String?,
    val year: Int?,
    val episodeCount: Int,
    val progress: SeasonProgress?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShowDetailsContent(
    show: ShowDetails,
    entry: LibraryEntryState?,
    tracking: ShowTrackingState,
    viewModel: ShowDetailsViewModel,
    onOpenSeason: (seasonNumber: Int, title: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val seasons = if (tracking.seasons.isNotEmpty()) {
        tracking.seasons.map { SeasonRow(it.seasonNumber, it.name, it.year, it.progress.totalCount, it.progress) }
    } else {
        show.seasons.map { SeasonRow(it.seasonNumber, it.name, it.year, it.episodeCount, null) }
    }
    val isTracked = entry != null && tracking.seasons.isNotEmpty()

    PullToRefreshBox(
        isRefreshing = tracking.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            DetailsHeader(
                title = show.title,
                originalTitle = show.originalTitle,
                posterModel = posterModel(entry, show.posterPath),
                metaLines = listOfNotNull(
                    listOfNotNull(stringResource(R.string.media_show), show.year?.toString()).joinToString(" · "),
                    listOf(
                        pluralStringResource(R.plurals.seasons_count, show.seasonCount, show.seasonCount),
                        pluralStringResource(R.plurals.episodes_count, show.episodeCount, show.episodeCount),
                    ).joinToString(" · "),
                    show.episodeRuntimeMinutes?.let { stringResource(R.string.episode_runtime, formatRuntime(it)) },
                    show.genres.joinToString(", "),
                ),
            )
            LibraryActions(
                entry = entry,
                onStatusSelected = viewModel::onStatusSelected,
                onRemove = viewModel::onRemove,
                onRatingChange = viewModel::onRatingChange,
            )
            tracking.progress?.takeIf { entry != null }?.let { progress ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.progress_title), style = MaterialTheme.typography.titleMedium)
                    Text(progressLabel(progress), style = MaterialTheme.typography.bodyLarge)
                    LinearProgressIndicator(
                        progress = { progress.percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(R.string.progress_episodes, progress.watchedCount, progress.airedCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OverviewSection(show.overview)
            if (seasons.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.details_seasons), style = MaterialTheme.typography.titleMedium)
                    if (entry == null) {
                        Text(
                            stringResource(R.string.tracking_requires_library),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    seasons.forEach { season ->
                        val title = seasonTitle(season.seasonNumber, season.name)
                        SeasonCard(
                            title = title,
                            season = season,
                            tracked = isTracked,
                            onClick = { onOpenSeason(season.seasonNumber, title) },
                            onWatchedChange = { viewModel.onSeasonWatchedChange(season.seasonNumber, it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonCard(
    title: String,
    season: SeasonRow,
    tracked: Boolean,
    onClick: () -> Unit,
    onWatchedChange: (Boolean) -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                val progress = season.progress
                Text(
                    text = listOfNotNull(
                        if (tracked && progress != null) {
                            stringResource(R.string.season_progress, progress.watchedCount, progress.totalCount)
                        } else {
                            pluralStringResource(R.plurals.episodes_count, season.episodeCount, season.episodeCount)
                        },
                        season.year?.toString(),
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (tracked && progress != null && progress.totalCount > 0) {
                    LinearProgressIndicator(
                        progress = { progress.watchedCount.toFloat() / progress.totalCount },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (tracked && season.progress != null && season.progress.airedCount > 0) {
                Checkbox(
                    checked = season.progress.isComplete,
                    onCheckedChange = onWatchedChange,
                )
            }
        }
    }
}

@Composable
internal fun seasonTitle(seasonNumber: Int, name: String?): String = name ?: if (seasonNumber == 0) {
    stringResource(R.string.season_specials)
} else {
    stringResource(R.string.season_number, seasonNumber)
}
