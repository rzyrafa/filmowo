package pl.filmowo.feature.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.filmowo.R
import pl.filmowo.core.ui.UiState
import pl.filmowo.core.ui.components.ErrorContent
import pl.filmowo.core.ui.components.LoadingContent
import pl.filmowo.core.ui.components.SegmentedProgress
import pl.filmowo.core.ui.components.StateTransition
import pl.filmowo.core.ui.components.WatchedCheck
import pl.filmowo.core.ui.components.messageRes
import pl.filmowo.core.ui.components.progressLabel
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme
import pl.filmowo.core.ui.theme.pressScale
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.SearchResult
import pl.filmowo.domain.model.SeasonProgress
import pl.filmowo.domain.model.ShowDetails

@Composable
fun ShowDetailsScreen(
    onBack: () -> Unit,
    onOpenSeason: (showId: Int, seasonNumber: Int, title: String) -> Unit,
    onOpenDetails: (id: Int, type: MediaType) -> Unit,
    viewModel: ShowDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tracking by viewModel.tracking.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
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
    val actionErrorText = state.actionError?.let {
        stringResource(R.string.details_sync_failed, stringResource(it.messageRes))
    }
    LaunchedEffect(actionErrorText) {
        if (actionErrorText != null) {
            snackbarHostState.showSnackbar(actionErrorText)
            viewModel.onActionErrorShown()
        }
    }

    Scaffold(
        topBar = { DetailsTopBar((state.content as? UiState.Success)?.data?.title, onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        StateTransition(state.content, contentKey = { it::class }) { content ->
            when (content) {
                UiState.Loading -> LoadingContent(modifier)
                is UiState.Error -> ErrorContent(content.error, viewModel::retry, modifier)
                is UiState.Success -> ShowDetailsContent(
                    show = content.data,
                    entry = state.library,
                    tracking = tracking,
                    recommendations = recommendations,
                    viewModel = viewModel,
                    onOpenDetails = onOpenDetails,
                    onOpenSeason = { number, title -> onOpenSeason(content.data.id, number, title) },
                    modifier = modifier,
                )
            }
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
    recommendations: List<SearchResult>,
    viewModel: ShowDetailsViewModel,
    onOpenDetails: (id: Int, type: MediaType) -> Unit,
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
                confirmPlanned = (tracking.progress?.watchedCount ?: 0) > 0,
            )
            tracking.progress?.takeIf { entry != null }?.let { progress ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.progress_title), style = MaterialTheme.typography.titleLarge)
                    SegmentedProgress(
                        done = progress.watchedCount,
                        total = progress.airedCount,
                        label = progressLabel(progress),
                    )
                    Text(
                        stringResource(R.string.progress_episodes, progress.watchedCount, progress.airedCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OverviewSection(show.overview)
            if (entry != null) NoteSection(entry.note, viewModel::onNoteChange)
            if (seasons.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.details_seasons), style = MaterialTheme.typography.titleLarge)
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
            RecommendationsSection(recommendations, onOpenDetails)
        }
    }
}

/** Biała karta sezonu: tytuł, liczba odcinków, segmentowy postęp i odhaczenie całego sezonu. */
@Composable
private fun SeasonCard(
    title: String,
    season: SeasonRow,
    tracked: Boolean,
    onClick: () -> Unit,
    onWatchedChange: (Boolean) -> Unit,
) {
    val colors = FilmowoTheme.colors
    val progress = season.progress
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.98f)
            .clip(FilmowoRadius.Lg)
            .background(colors.surfaceRaised)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            val episodes = pluralStringResource(R.plurals.episodes_count, season.episodeCount, season.episodeCount)
            if (tracked && progress != null && progress.totalCount > 0) {
                SegmentedProgress(
                    done = progress.watchedCount,
                    total = progress.totalCount,
                    label = stringResource(R.string.season_progress, progress.watchedCount, progress.totalCount),
                )
            } else {
                Text(
                    text = listOfNotNull(episodes, season.year?.toString()).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                )
            }
        }
        if (tracked && progress != null && progress.airedCount > 0) {
            WatchedCheck(
                checked = progress.isComplete,
                enabled = true,
                modifier = Modifier
                    .clip(FilmowoRadius.Sm)
                    .toggleable(value = progress.isComplete, role = Role.Checkbox, onValueChange = onWatchedChange),
            )
        }
    }
}

@Composable
internal fun seasonTitle(seasonNumber: Int, name: String?): String = name ?: if (seasonNumber == 0) {
    stringResource(R.string.season_specials)
} else {
    stringResource(R.string.season_number, seasonNumber)
}
