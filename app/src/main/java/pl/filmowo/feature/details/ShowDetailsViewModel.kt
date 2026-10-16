package pl.filmowo.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.filmowo.core.network.AppError
import pl.filmowo.core.network.AppResult
import pl.filmowo.core.network.ConnectivityObserver
import pl.filmowo.data.repository.EpisodeRepository
import pl.filmowo.data.repository.LibraryRepository
import pl.filmowo.data.repository.TmdbRepository
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.ShowDetails
import pl.filmowo.domain.model.ShowProgress
import pl.filmowo.domain.model.TrackedSeason
import pl.filmowo.domain.model.WatchStatus
import pl.filmowo.domain.model.toLibraryMetadata
import pl.filmowo.domain.model.toShowDetails
import javax.inject.Inject

/** Śledzenie odcinków na ekranie serialu (tylko dla seriali z biblioteki). */
data class ShowTrackingState(
    /** Sezony zapisane lokalnie z postępem; puste, gdy odcinków jeszcze nie pobrano. */
    val seasons: List<TrackedSeason> = emptyList(),
    val progress: ShowProgress? = null,
    val isRefreshing: Boolean = false,
    /** Błąd ostatniego odświeżenia – do jednorazowego komunikatu. */
    val refreshError: AppError? = null,
)

@HiltViewModel
class ShowDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tmdbRepository: TmdbRepository,
    private val libraryRepository: LibraryRepository,
    private val episodeRepository: EpisodeRepository,
    connectivity: ConnectivityObserver,
) : LibraryDetailsViewModel<ShowDetails>(
    savedStateHandle = savedStateHandle,
    mediaType = MediaType.TV,
    libraryRepository = libraryRepository,
    connectivity = connectivity,
    fetch = { tmdbRepository.showDetails(it) },
    toMetadata = { it.toLibraryMetadata() },
    fromLocal = { it.toShowDetails() },
    afterStatusChange = { status ->
        val showId: Int = checkNotNull(savedStateHandle["id"])
        if (status == WatchStatus.WATCHED) {
            episodeRepository.markShowWatched(showId)
        } else if (!episodeRepository.hasEpisodes(showId)) {
            episodeRepository.sync(showId)
        }
    },
) {

    private val refreshState = MutableStateFlow(ShowTrackingState())

    val tracking: StateFlow<ShowTrackingState> = combine(
        episodeRepository.observeSeasons(tmdbId),
        episodeRepository.observeProgress(tmdbId),
        refreshState,
    ) { seasons, progress, refresh ->
        refresh.copy(seasons = seasons, progress = progress.takeIf { seasons.isNotEmpty() })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ShowTrackingState())

    init {
        // Serial z biblioteki bez zapisanych odcinków (np. dodany offline) – dociągnij je.
        viewModelScope.launch {
            if (libraryRepository.observeItem(tmdbId, MediaType.TV).first() != null &&
                !episodeRepository.hasEpisodes(tmdbId)
            ) {
                episodeRepository.sync(tmdbId)
            }
        }
    }

    fun refresh() {
        if (refreshState.value.isRefreshing) return
        refreshState.update { it.copy(isRefreshing = true, refreshError = null) }
        viewModelScope.launch {
            var error = loader.refresh()
            if (state.value.library != null) {
                val sync = episodeRepository.sync(tmdbId)
                if (sync is AppResult.Failure) error = error ?: sync.error
            }
            refreshState.update { it.copy(isRefreshing = false, refreshError = error) }
        }
    }

    fun onRefreshErrorShown() {
        refreshState.update { it.copy(refreshError = null) }
    }

    fun onSeasonWatchedChange(seasonNumber: Int, watched: Boolean) {
        viewModelScope.launch { episodeRepository.setSeasonWatched(tmdbId, seasonNumber, watched) }
    }
}
