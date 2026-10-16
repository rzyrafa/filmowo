package pl.filmowo.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import pl.filmowo.core.network.ConnectivityObserver
import pl.filmowo.core.ui.UiState
import pl.filmowo.data.repository.TmdbRepository
import pl.filmowo.domain.model.MovieDetails
import javax.inject.Inject

@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: TmdbRepository,
    connectivity: ConnectivityObserver,
) : ViewModel() {

    private val movieId: Int = checkNotNull(savedStateHandle["id"])

    private val loader = DetailsLoader(viewModelScope, connectivity) { repository.movieDetails(movieId) }

    val state: StateFlow<UiState<MovieDetails>> = loader.state

    fun retry() = loader.load()
}
