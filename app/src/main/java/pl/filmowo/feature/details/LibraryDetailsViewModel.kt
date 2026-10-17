package pl.filmowo.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.filmowo.core.network.AppError
import pl.filmowo.core.network.AppResult
import pl.filmowo.core.network.ConnectivityObserver
import pl.filmowo.core.ui.UiState
import pl.filmowo.data.repository.LibraryRepository
import pl.filmowo.domain.model.LibraryMetadata
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.SearchResult
import pl.filmowo.domain.model.WatchStatus

/** Stan pozycji w bibliotece widoczny na ekranie szczegółów. */
data class LibraryEntryState(
    val status: WatchStatus,
    val rating: Int?,
    val localPosterFile: String?,
    val note: String? = null,
    val rewatchCount: Int = 0,
)

data class DetailsUiState<T>(
    val content: UiState<T> = UiState.Loading,
    /** null – pozycji nie ma w bibliotece. */
    val library: LibraryEntryState? = null,
    /** Błąd pracy po zmianie statusu (np. pobierania odcinków) – do jednorazowego komunikatu. */
    val actionError: AppError? = null,
)

/**
 * Wspólna logika szczegółów filmu i serialu.
 *
 * Pozycje z biblioteki pokazujemy od razu z Room (działa offline); gdy jest internet,
 * dane z TMDB mają pierwszeństwo i odświeżają zapis lokalny.
 *
 * Zależności przychodzą jako lambdy, bo konstruktor klasy bazowej uruchamia ładowanie
 * zanim pola klasy pochodnej zostaną zainicjalizowane.
 */
abstract class LibraryDetailsViewModel<T : Any>(
    savedStateHandle: SavedStateHandle,
    private val mediaType: MediaType,
    private val libraryRepository: LibraryRepository,
    connectivity: ConnectivityObserver,
    fetch: suspend (id: Int) -> AppResult<T>,
    private val toMetadata: (T) -> LibraryMetadata,
    fromLocal: (LibraryMetadata) -> T,
    private val fetchRecommendations: suspend (id: Int) -> AppResult<List<SearchResult>> = { AppResult.Success(emptyList()) },
    /** Dodatkowa praca po zmianie statusu (np. pobranie odcinków serialu); zwraca błąd do pokazania. */
    private val afterStatusChange: suspend (WatchStatus) -> AppError? = { null },
) : ViewModel() {

    protected val tmdbId: Int = checkNotNull(savedStateHandle["id"])

    internal val loader = DetailsLoader(viewModelScope, connectivity) { fetch(tmdbId) }

    private val actionError = MutableStateFlow<AppError?>(null)

    private val _recommendations = MutableStateFlow<List<SearchResult>>(emptyList())

    /** Polecane tytuły; pusta lista, gdy brak danych lub internetu (sekcja jest wtedy ukryta). */
    val recommendations: StateFlow<List<SearchResult>> = _recommendations.asStateFlow()
    private var recommendationsLoaded = false
    private var recommendationsJob: Job? = null

    val state: StateFlow<DetailsUiState<T>> =
        combine(loader.state, libraryRepository.observeItem(tmdbId, mediaType), actionError) { remote, item, error ->
            val content = when {
                remote is UiState.Success -> remote
                item != null -> UiState.Success(fromLocal(item.metadata))
                else -> remote
            }
            DetailsUiState(
                content = content,
                library = item?.let { LibraryEntryState(it.status, it.rating, it.localPosterFile, it.note, it.rewatchCount) },
                actionError = error,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, DetailsUiState())

    init {
        loader.state
            .filterIsInstance<UiState.Success<T>>()
            .onEach { libraryRepository.refreshMetadata(toMetadata(it.data)) }
            .launchIn(viewModelScope)

        // isOnline podaje od razu bieżący stan, więc to też pierwsze ładowanie.
        connectivity.isOnline
            .filter { it && !recommendationsLoaded }
            .onEach { loadRecommendations() }
            .launchIn(viewModelScope)
    }

    fun retry() = loader.load()

    /** Dodaje do biblioteki albo zmienia status. Usunięcie – przez [onRemove]. */
    fun onStatusSelected(status: WatchStatus) {
        val details = (state.value.content as? UiState.Success)?.data ?: return
        if (state.value.library?.status == status) return
        viewModelScope.launch {
            libraryRepository.setStatus(toMetadata(details), status)
            afterStatusChange(status)?.let { actionError.value = it }
        }
    }

    fun onActionErrorShown() {
        actionError.value = null
    }

    fun onRemove() {
        viewModelScope.launch { libraryRepository.remove(tmdbId, mediaType) }
    }

    fun onRatingChange(rating: Int?) {
        if (state.value.library == null) return
        viewModelScope.launch { libraryRepository.setRating(tmdbId, mediaType, rating) }
    }

    fun onNoteChange(note: String?) {
        if (state.value.library == null) return
        viewModelScope.launch { libraryRepository.setNote(tmdbId, mediaType, note) }
    }

    /** Dodaje (+1) albo odejmuje (-1) powtórne obejrzenie; tylko dla obejrzanych pozycji. */
    fun onRewatchChange(delta: Int) {
        val entry = state.value.library ?: return
        if (entry.status != WatchStatus.WATCHED) return
        val count = (entry.rewatchCount + delta).coerceAtLeast(0)
        if (count == entry.rewatchCount) return
        viewModelScope.launch { libraryRepository.setRewatchCount(tmdbId, mediaType, count) }
    }

    private fun loadRecommendations() {
        if (recommendationsJob?.isActive == true) return
        recommendationsJob = viewModelScope.launch {
            val result = fetchRecommendations(tmdbId)
            if (result is AppResult.Success) {
                recommendationsLoaded = true
                _recommendations.value = result.data.filterNot { it.id == tmdbId && it.mediaType == mediaType }
            }
        }
    }
}
