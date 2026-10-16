package pl.filmowo.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pl.filmowo.data.repository.LibraryRepository
import pl.filmowo.domain.model.LibraryItem
import pl.filmowo.domain.model.MediaFilter
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStatus
import java.text.Collator
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

data class LibraryUiState(
    val tab: LibraryTab = LibraryTab.WATCHING,
    val filter: MediaFilter = MediaFilter.ALL,
    val sort: LibrarySort = LibrarySort.RECENT,
    val items: List<LibraryItem> = emptyList(),
    /** Liczba pozycji w każdej zakładce (z uwzględnieniem filtra). */
    val counts: Map<LibraryTab, Int> = emptyMap(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    repository: LibraryRepository,
) : ViewModel() {

    private val tab = MutableStateFlow(LibraryTab.WATCHING)
    private val filter = MutableStateFlow(MediaFilter.ALL)
    private val sort = MutableStateFlow(LibrarySort.RECENT)

    private val titleCollator = Collator.getInstance(Locale.forLanguageTag("pl-PL"))

    val state: StateFlow<LibraryUiState> =
        combine(repository.observeLibrary(), tab, filter, sort) { all, tab, filter, sort ->
            val filtered = all.filter { it.matches(filter) }
            LibraryUiState(
                tab = tab,
                filter = filter,
                sort = sort,
                items = filtered.filter { it.status == tab.status }.sortedWith(comparator(sort)),
                counts = LibraryTab.entries.associateWith { t -> filtered.count { it.status == t.status } },
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, LibraryUiState())

    fun onTabSelected(value: LibraryTab) {
        tab.value = value
    }

    fun onFilterSelected(value: MediaFilter) {
        filter.value = value
    }

    fun onSortSelected(value: LibrarySort) {
        sort.value = value
    }

    private fun LibraryItem.matches(filter: MediaFilter) = when (filter) {
        MediaFilter.ALL -> true
        MediaFilter.MOVIES -> mediaType == MediaType.MOVIE
        MediaFilter.SHOWS -> mediaType == MediaType.TV
    }

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
}
