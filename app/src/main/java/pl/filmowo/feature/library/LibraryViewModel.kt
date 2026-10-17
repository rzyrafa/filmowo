package pl.filmowo.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.filmowo.data.preferences.UserPreferencesRepository
import pl.filmowo.data.repository.EpisodeRepository
import pl.filmowo.data.repository.LibraryRepository
import pl.filmowo.domain.model.EpisodeRef
import pl.filmowo.domain.model.LibraryItem
import pl.filmowo.domain.model.MediaFilter
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.ShowProgress
import pl.filmowo.domain.model.WatchStatus
import java.text.Collator
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject

enum class LibraryTab(val status: WatchStatus) {
    WATCHING(WatchStatus.WATCHING),
    PLANNED(WatchStatus.PLANNED),
    WATCHED(WatchStatus.WATCHED),
}

enum class LibrarySort {
    /** Ostatnio dodane lub zmienione. */
    RECENT,
    TITLE,
    YEAR,
    RATING,
}

/** Pozycja na liście; [progress] tylko dla seriali z pobranymi odcinkami. */
data class LibraryEntry(
    val item: LibraryItem,
    val progress: ShowProgress?,
)

data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.WATCHING,
    val filter: MediaFilter = MediaFilter.ALL,
    val sort: LibrarySort = LibrarySort.RECENT,
    /** Fraza wyszukiwania w bibliotece; pusta – bez filtrowania. */
    val query: String = "",
    val entries: List<LibraryEntry> = emptyList(),
    /** Liczba pozycji w każdej zakładce (z uwzględnieniem filtra i wyszukiwania). */
    val counts: Map<LibraryTab, Int> = emptyMap(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    repository: LibraryRepository,
    private val episodeRepository: EpisodeRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    // Zakładka i fraza przetrwają zabicie procesu; sortowanie i filtr – także ponowne uruchomienie (DataStore).
    private val tab = savedStateHandle.getStateFlow(KEY_TAB, LibraryTab.WATCHING)
    private val query = MutableStateFlow(savedStateHandle[KEY_QUERY] ?: "")

    private val titleCollator = Collator.getInstance(Locale.forLanguageTag("pl-PL"))

    val state: StateFlow<LibraryUiState> = combine(
        repository.observeLibrary(),
        episodeRepository.observeAllProgress(),
        preferences.library,
        tab,
        query,
    ) { all, progress, prefs, tab, query ->
        val sort = LibrarySort.entries.firstOrNull { it.name == prefs.sort } ?: LibrarySort.RECENT
        val needle = query.normalizedForSearch()
        val filtered = all.filter { it.matches(prefs.filter) && (needle.isEmpty() || it.matches(needle)) }
        LibraryUiState(
            tab = tab,
            filter = prefs.filter,
            sort = sort,
            query = query,
            entries = filtered
                .filter { it.status == tab.status }
                .sortedWith(comparator(sort))
                .map { item ->
                    LibraryEntry(item, progress[item.tmdbId].takeIf { item.mediaType == MediaType.TV })
                },
            counts = LibraryTab.entries.associateWith { t -> filtered.count { it.status == t.status } },
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), LibraryUiState())

    fun onTabSelected(value: LibraryTab) {
        savedStateHandle[KEY_TAB] = value
    }

    fun onFilterSelected(value: MediaFilter) {
        viewModelScope.launch { preferences.setLibraryFilter(value) }
    }

    fun onSortSelected(value: LibrarySort) {
        viewModelScope.launch { preferences.setLibrarySort(value.name) }
    }

    fun onQueryChange(value: String) {
        query.value = value
        savedStateHandle[KEY_QUERY] = value
    }

    /** Szybkie odhaczenie następnego odcinka z kafelka biblioteki. */
    fun onMarkEpisodeWatched(showId: Int, episode: EpisodeRef) {
        viewModelScope.launch {
            episodeRepository.setEpisodeWatched(showId, episode.seasonNumber, episode.episodeNumber, watched = true)
        }
    }

    private fun LibraryItem.matches(filter: MediaFilter) = when (filter) {
        MediaFilter.ALL -> true
        MediaFilter.MOVIES -> mediaType == MediaType.MOVIE
        MediaFilter.SHOWS -> mediaType == MediaType.TV
    }

    private fun LibraryItem.matches(needle: String) =
        metadata.title.normalizedForSearch().contains(needle) ||
            metadata.originalTitle?.normalizedForSearch()?.contains(needle) == true

    private fun comparator(sort: LibrarySort): Comparator<LibraryItem> {
        val byTitle = Comparator<LibraryItem> { a, b -> titleCollator.compare(a.metadata.title, b.metadata.title) }
        return when (sort) {
            LibrarySort.RECENT -> compareByDescending<LibraryItem> { it.updatedAt }.then(byTitle)
            LibrarySort.TITLE -> byTitle
            // Pozycje bez roku / bez oceny na końcu.
            LibrarySort.YEAR -> compareByDescending<LibraryItem, Int?>(nullsFirst()) { it.metadata.year }.then(byTitle)
            LibrarySort.RATING -> compareByDescending<LibraryItem, Int?>(nullsFirst()) { it.rating }.then(byTitle)
        }
    }

    private companion object {
        const val KEY_TAB = "tab"
        const val KEY_QUERY = "query"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Małe litery bez polskich znaków i akcentów – „Łódź” pasuje do „lodz”. */
internal fun String.normalizedForSearch(): String =
    Normalizer.normalize(trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('ł', 'l')
