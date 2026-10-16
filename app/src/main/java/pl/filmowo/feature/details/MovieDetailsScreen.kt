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
import pl.filmowo.domain.model.MovieDetails

@Composable
fun MovieDetailsScreen(
    onBack: () -> Unit,
    viewModel: MovieDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { DetailsTopBar((state as? UiState.Success)?.data?.title, onBack) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (val s = state) {
            UiState.Loading -> LoadingContent(modifier)
            is UiState.Error -> ErrorContent(s.error, viewModel::retry, modifier)
            is UiState.Success -> MovieDetailsContent(s.data, modifier)
        }
    }
}

@Composable
private fun MovieDetailsContent(movie: MovieDetails, modifier: Modifier = Modifier) {
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
            posterPath = movie.posterPath,
            metaLines = listOfNotNull(
                listOfNotNull(stringResource(R.string.media_movie), movie.year?.toString()).joinToString(" · "),
                movie.runtimeMinutes?.let { formatRuntime(it) },
                movie.genres.joinToString(", "),
            ),
        )
        OverviewSection(movie.overview)
    }
}
