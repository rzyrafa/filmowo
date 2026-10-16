package pl.filmowo.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pl.filmowo.data.repository.StatsRepository
import pl.filmowo.domain.model.WatchStats
import javax.inject.Inject

data class StatsUiState(
    val isLoading: Boolean = true,
    val stats: WatchStats? = null,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    repository: StatsRepository,
) : ViewModel() {

    val state: StateFlow<StatsUiState> = repository.observeStats()
        .map { StatsUiState(isLoading = false, stats = it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StatsUiState())
}
