package pl.filmowo.feature.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pl.filmowo.core.TimeProvider
import pl.filmowo.core.today
import pl.filmowo.data.repository.EpisodeRepository
import pl.filmowo.domain.model.UpcomingEpisode
import javax.inject.Inject

/** Odcinki z premierą tego samego dnia. */
data class UpcomingDay(
    /** Data ISO (yyyy-MM-dd). */
    val date: String,
    val episodes: List<UpcomingEpisode>,
)

data class UpcomingUiState(
    val isLoading: Boolean = true,
    /** Dzisiejsza data ISO – do podpisów „Dziś” / „Jutro”. */
    val today: String = "",
    val days: List<UpcomingDay> = emptyList(),
)

/** Zapowiedziane odcinki seriali z biblioteki, pogrupowane po dniach premiery. */
@HiltViewModel
class UpcomingViewModel @Inject constructor(
    episodeRepository: EpisodeRepository,
    private val time: TimeProvider,
) : ViewModel() {

    val state: StateFlow<UpcomingUiState> = episodeRepository.observeUpcoming()
        .map { episodes ->
            UpcomingUiState(
                isLoading = false,
                today = time.today(),
                days = episodes.groupBy { it.airDate }.map { (date, list) -> UpcomingDay(date, list) },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UpcomingUiState())
}
