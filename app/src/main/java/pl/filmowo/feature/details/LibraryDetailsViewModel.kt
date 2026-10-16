package pl.filmowo.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.filmowo.core.network.AppResult
import pl.filmowo.core.network.ConnectivityObserver
import pl.filmowo.core.ui.UiState
import pl.filmowo.data.repository.LibraryRepository
import pl.filmowo.domain.model.LibraryMetadata
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStatus

/** Stan pozycji w bibliotece widoczny na ekranie szczegółów. */
data class LibraryEntryState(
    val status: WatchStatus,
    val rating: Int?,
    val localPosterFile: String?,
)

data class DetailsUiState<T>(
    val content: UiState<T> = UiState.Loading,
    /** null – pozycji nie ma w bibliotece. */
    val library: LibraryEntryState? = null,
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
    /** Dodatkowa praca po zmianie statusu (np. pobranie odcinków serialu). */
    private val afterStatusChange: suspend (WatchStatus) -> Unit = {},
) : ViewModel() {

    protected val tmdbId: Int = checkNotNull(savedStateHandle["id"])

    internal val loader = DetailsLoader(viewModelScope, connectivity) { fetch(tmdbId) }

    val state: StateFlow<DetailsUiState<T>> =
        combine(loader.state, libraryRepository.observeItem(tmdbId, mediaType)) { remote, item ->
            val content = when {
                remote is UiState.Success -> remote
                item != null -> UiState.Success(fromLocal(item.metadata))
                else -> remote
            }
            DetailsUiState(
                content = content,
                library = item?.let { LibraryEntryState(it.status, it.rating, it.localPosterFile) },
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, DetailsUiState())

    init {
        loader.state
            .filterIsInstance<UiState.Success<T>>()
            .onEach { libraryRepository.refreshMetadata(toMetadata(it.data)) }
            .launchIn(viewModelScope)
    }

    fun retry() = loader.load()

    /** Dodaje do biblioteki albo zmienia status. Usunięcie – przez [onRemove]. */
    fun onStatusSelected(status: WatchStatus) {
        val details = (state.value.content as? UiState.Success)?.data ?: return
        if (state.value.library?.status == status) return
        viewModelScope.launch {
            libraryRepository.setStatus(toMetadata(details), status)
            afterStatusChange(status)
        }
    }

    fun onRemove() {
        viewModelScope.launch { libraryRepository.remove(tmdbId, mediaType) }
    }

    fun onRatingChange(rating: Int?) {
        if (state.value.library == null) return
        viewModelScope.launch { libraryRepository.setRating(tmdbId, mediaType, rating) }
    }
}
