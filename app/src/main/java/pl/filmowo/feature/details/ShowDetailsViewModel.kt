package pl.filmowo.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import pl.filmowo.core.network.ConnectivityObserver
import pl.filmowo.core.ui.UiState
import pl.filmowo.data.repository.TmdbRepository
import pl.filmowo.domain.model.ShowDetails
import javax.inject.Inject

@HiltViewModel
class ShowDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: TmdbRepository,
    connectivity: ConnectivityObserver,
) : ViewModel() {

    private val showId: Int = checkNotNull(savedStateHandle["id"])

    private val loader = DetailsLoader(viewModelScope, connectivity) { repository.showDetails(showId) }

    val state: StateFlow<UiState<ShowDetails>> = loader.state

    fun retry() = loader.load()
}
