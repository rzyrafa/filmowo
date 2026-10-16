package pl.filmowo.feature.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import pl.filmowo.domain.model.SeasonSummary
import pl.filmowo.domain.model.ShowDetails

@Composable
fun ShowDetailsScreen(
    onBack: () -> Unit,
    viewModel: ShowDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { DetailsTopBar((state as? UiState.Success)?.data?.title, onBack) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (val s = state) {
            UiState.Loading -> LoadingContent(modifier)
            is UiState.Error -> ErrorContent(s.error, viewModel::retry, modifier)
            is UiState.Success -> ShowDetailsContent(s.data, modifier)
        }
    }
}

@Composable
private fun ShowDetailsContent(show: ShowDetails, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        DetailsHeader(
            title = show.title,
            originalTitle = show.originalTitle,
            posterPath = show.posterPath,
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
        OverviewSection(show.overview)
        if (show.seasons.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.details_seasons), style = MaterialTheme.typography.titleMedium)
                show.seasons.forEach { SeasonCard(it) }
            }
        }
    }
}

@Composable
private fun SeasonCard(season: SeasonSummary) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = season.name ?: if (season.isSpecials) {
                    stringResource(R.string.season_specials)
                } else {
                    stringResource(R.string.season_number, season.seasonNumber)
                },
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = listOfNotNull(
                    pluralStringResource(R.plurals.episodes_count, season.episodeCount, season.episodeCount),
                    season.year?.toString(),
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
