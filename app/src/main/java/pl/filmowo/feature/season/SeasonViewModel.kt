package pl.filmowo.feature.season

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.filmowo.core.TimeProvider
import pl.filmowo.core.network.AppError
import pl.filmowo.core.network.ConnectivityObserver
import pl.filmowo.core.today
import pl.filmowo.core.ui.UiState
import pl.filmowo.core.ui.toUiState
import pl.filmowo.data.repository.EpisodeRepository
import pl.filmowo.data.repository.LibraryRepository
import pl.filmowo.data.repository.TmdbRepository
import pl.filmowo.domain.model.EpisodeInfo
import pl.filmowo.domain.model.EpisodeProgressInput
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.SeasonDetails
import pl.filmowo.domain.model.SeasonProgress
import pl.filmowo.domain.model.computeSeasonProgress
import pl.filmowo.domain.model.isAired
import javax.inject.Inject

data class EpisodeRow(
    val info: EpisodeInfo,
    val isWatched: Boolean,
    val isAired: Boolean,
)

data class SeasonUiState(
    val episodes: UiState<List<EpisodeRow>> = UiState.Loading,
    /** Odcinki można odhaczać tylko w serialu z biblioteki, gdy są zapisane lokalnie. */
    val isTracked: Boolean = false,
    val inLibrary: Boolean = false,
    val progress: SeasonProgress? = null,
)

/**
 * Lista odcinków sezonu. Dla seriali z biblioteki dane pochodzą z Room (działa offline);
 * w pozostałych przypadkach – z TMDB, tylko do podglądu.
 */
@HiltViewModel
class SeasonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tmdbRepository: TmdbRepository,
    libraryRepository: LibraryRepository,
    private val episodeRepository: EpisodeRepository,
    connectivity: ConnectivityObserver,
    private val time: TimeProvider,
) : ViewModel() {

    private val showId: Int = checkNotNull(savedStateHandle["showId"])
    private val seasonNumber: Int = checkNotNull(savedStateHandle["seasonNumber"])

    private val remote = MutableStateFlow<UiState<SeasonDetails>?>(null)
    private var remoteJob: Job? = null

    private val localEpisodes = episodeRepository.observeEpisodes(showId, seasonNumber)

    val state: StateFlow<SeasonUiState> = combine(
        libraryRepository.observeItem(showId, MediaType.TV),
        localEpisodes,
        remote,
    ) { item, local, remoteState ->
        val today = time.today()
        if (local.isNotEmpty()) {
            val rows = local.map { EpisodeRow(it.info, it.isWatched, isAired(it.info.airDate, today)) }
            SeasonUiState(
                episodes = UiState.Success(rows),
                isTracked = item != null,
                inLibrary = item != null,
                progress = computeSeasonProgress(
                    local.map { EpisodeProgressInput(it.info.seasonNumber, it.info.episodeNumber, it.info.airDate, it.isWatched) },
                    today,
                ),
            )
        } else {
            val episodes = when (remoteState) {
                null, UiState.Loading -> UiState.Loading
                is UiState.Error -> remoteState
                is UiState.Success -> UiState.Success(
                    remoteState.data.episodes.map { EpisodeRow(it, isWatched = false, isAired = isAired(it.airDate, today)) },
                )
            }
            SeasonUiState(episodes = episodes, inLibrary = item != null)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SeasonUiState())

    init {
        viewModelScope.launch {
            if (localEpisodes.first().isEmpty()) {
                loadRemote()
                // Serial jest w bibliotece, ale odcinków jeszcze nie ma – pobierz je do śledzenia.
                if (libraryRepository.observeItem(showId, MediaType.TV).first() != null) {
                    episodeRepository.sync(showId)
                }
            }
        }
        connectivity.isOnline
            .filter { it }
            .onEach {
                val current = remote.value
                if (current is UiState.Error && current.error == AppError.NETWORK) loadRemote()
            }
            .launchIn(viewModelScope)
    }

    fun retry() = loadRemote()

    fun onEpisodeWatchedChange(episodeNumber: Int, watched: Boolean) {
        if (!state.value.isTracked) return
        viewModelScope.launch {
            episodeRepository.setEpisodeWatched(showId, seasonNumber, episodeNumber, watched)
        }
    }

    fun onSeasonWatchedChange(watched: Boolean) {
        if (!state.value.isTracked) return
        viewModelScope.launch { episodeRepository.setSeasonWatched(showId, seasonNumber, watched) }
    }

    private fun loadRemote() {
        remoteJob?.cancel()
        remote.value = UiState.Loading
        remoteJob = viewModelScope.launch {
            remote.value = tmdbRepository.seasonDetails(showId, seasonNumber).toUiState()
        }
    }
}
