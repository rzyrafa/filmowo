package pl.filmowo.feature.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.filmowo.R
import pl.filmowo.core.ui.UiState
import pl.filmowo.core.ui.components.ErrorContent
import pl.filmowo.core.ui.components.LoadingContent
import pl.filmowo.core.ui.components.StateTransition
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.MovieDetails
import pl.filmowo.domain.model.SearchResult

@Composable
fun MovieDetailsScreen(
    onBack: () -> Unit,
    onOpenDetails: (id: Int, type: MediaType) -> Unit,
    viewModel: MovieDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { DetailsTopBar((state.content as? UiState.Success)?.data?.title, onBack) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        StateTransition(state.content, contentKey = { it::class }) { content ->
            when (content) {
                UiState.Loading -> LoadingContent(modifier)
                is UiState.Error -> ErrorContent(content.error, viewModel::retry, modifier)
                is UiState.Success -> MovieDetailsContent(
                    movie = content.data,
                    entry = state.library,
                    recommendations = recommendations,
                    viewModel = viewModel,
                    onOpenDetails = onOpenDetails,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun MovieDetailsContent(
    movie: MovieDetails,
    entry: LibraryEntryState?,
    recommendations: List<SearchResult>,
    viewModel: MovieDetailsViewModel,
    onOpenDetails: (id: Int, type: MediaType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        DetailsHeader(
            title = movie.title,
            originalTitle = movie.originalTitle,
            posterModel = posterModel(entry, movie.posterPath),
            metaLines = listOfNotNull(
                listOfNotNull(stringResource(R.string.media_movie), movie.year?.toString()).joinToString(" · "),
                movie.runtimeMinutes?.let { formatRuntime(it) },
                movie.genres.joinToString(", "),
            ),
        )
        LibraryActions(
            entry = entry,
            onStatusSelected = viewModel::onStatusSelected,
            onRemove = viewModel::onRemove,
            onRatingChange = viewModel::onRatingChange,
            onRewatchChange = viewModel::onRewatchChange,
        )
        OverviewSection(movie.overview)
        if (entry != null) NoteSection(entry.note, viewModel::onNoteChange)
        RecommendationsSection(recommendations, onOpenDetails)
    }
}
