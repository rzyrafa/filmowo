package pl.filmowo.feature.details

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import pl.filmowo.core.network.ConnectivityObserver
import pl.filmowo.data.repository.LibraryRepository
import pl.filmowo.data.repository.TmdbRepository
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.ShowDetails
import pl.filmowo.domain.model.toLibraryMetadata
import pl.filmowo.domain.model.toShowDetails
import javax.inject.Inject

@HiltViewModel
class ShowDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tmdbRepository: TmdbRepository,
    libraryRepository: LibraryRepository,
    connectivity: ConnectivityObserver,
) : LibraryDetailsViewModel<ShowDetails>(
    savedStateHandle = savedStateHandle,
    mediaType = MediaType.TV,
    libraryRepository = libraryRepository,
    connectivity = connectivity,
    fetch = { tmdbRepository.showDetails(it) },
    toMetadata = { it.toLibraryMetadata() },
    fromLocal = { it.toShowDetails() },
)
