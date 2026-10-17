package pl.filmowo.feature.details

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import pl.filmowo.core.network.ConnectivityObserver
import pl.filmowo.data.repository.LibraryRepository
import pl.filmowo.data.repository.TmdbRepository
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.MovieDetails
import pl.filmowo.domain.model.toLibraryMetadata
import pl.filmowo.domain.model.toMovieDetails
import javax.inject.Inject

@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tmdbRepository: TmdbRepository,
    libraryRepository: LibraryRepository,
    connectivity: ConnectivityObserver,
) : LibraryDetailsViewModel<MovieDetails>(
    savedStateHandle = savedStateHandle,
    mediaType = MediaType.MOVIE,
    libraryRepository = libraryRepository,
    connectivity = connectivity,
    fetch = { tmdbRepository.movieDetails(it) },
    toMetadata = { it.toLibraryMetadata() },
    fromLocal = { it.toMovieDetails() },
    fetchRecommendations = { tmdbRepository.recommendations(it, MediaType.MOVIE) },
)
