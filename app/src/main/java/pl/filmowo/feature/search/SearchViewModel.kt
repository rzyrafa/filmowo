package pl.filmowo.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.filmowo.core.network.AppError
import pl.filmowo.core.network.AppResult
import pl.filmowo.core.network.ConnectivityObserver
import pl.filmowo.core.ui.UiState
import pl.filmowo.core.ui.toUiState
import pl.filmowo.data.repository.TmdbRepository
import pl.filmowo.domain.model.MediaFilter
import pl.filmowo.domain.model.SearchResult
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val filter: MediaFilter = MediaFilter.ALL,
    val results: SearchResultsState = SearchResultsState.Idle,
    /** Popularne tytuły pokazywane przy pustym polu wyszukiwania. */
    val trending: UiState<List<SearchResult>> = UiState.Loading,
)

sealed interface SearchResultsState {
    /** Puste pole wyszukiwania. */
    data object Idle : SearchResultsState
    data object Loading : SearchResultsState
    data object Empty : SearchResultsState
    data class Error(val error: AppError) : SearchResultsState
    data class Success(
        val items: List<SearchResult>,
        val page: Int,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
        val loadMoreError: AppError? = null,
    ) : SearchResultsState
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: TmdbRepository,
    connectivity: ConnectivityObserver,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val queryInput = MutableStateFlow("")
    private var searchJob: Job? = null
    private var trendingJob: Job? = null

    init {
        queryInput
            .debounce(DEBOUNCE_MS)
            .map { it.trim() }
            .distinctUntilChanged()
            .onEach { search(it) }
            .launchIn(viewModelScope)

        loadTrending()

        // Po odzyskaniu internetu ponów zapytanie, które nie powiodło się z powodu braku sieci.
        connectivity.isOnline
            .filter { it }
            .onEach {
                if (_state.value.trending is UiState.Error) loadTrending()
                when (val results = _state.value.results) {
                    is SearchResultsState.Error -> if (results.error == AppError.NETWORK) retry()
                    is SearchResultsState.Success -> if (results.loadMoreError == AppError.NETWORK) loadMore()
                    else -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    fun retryTrending() = loadTrending()

    private fun loadTrending() {
        if (trendingJob?.isActive == true) return
        _state.update { it.copy(trending = UiState.Loading) }
        trendingJob = viewModelScope.launch {
            val trending = repository.trending().toUiState()
            _state.update { it.copy(trending = trending) }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        queryInput.value = query
    }

    fun onFilterChange(filter: MediaFilter) {
        if (filter == _state.value.filter) return
        _state.update { it.copy(filter = filter) }
        search(currentQuery())
    }

    fun retry() = search(currentQuery())

    fun loadMore() {
        val current = _state.value.results as? SearchResultsState.Success ?: return
        if (!current.hasMore || current.isLoadingMore) return
        val query = currentQuery()
        val filter = _state.value.filter

        _state.update { it.copy(results = current.copy(isLoadingMore = true, loadMoreError = null)) }
        searchJob = viewModelScope.launch {
            val next = when (val result = repository.search(query, filter, current.page + 1)) {
                is AppResult.Success -> current.copy(
                    items = (current.items + result.data.items).distinctBy { it.mediaType to it.id },
                    page = result.data.page,
                    hasMore = result.data.hasMore,
                    isLoadingMore = false,
                )
                is AppResult.Failure -> current.copy(isLoadingMore = false, loadMoreError = result.error)
            }
            _state.update { it.copy(results = next) }
        }
    }

    private fun currentQuery() = _state.value.query.trim()

    private fun search(query: String) {
        searchJob?.cancel()
        if (query.isEmpty()) {
            _state.update { it.copy(results = SearchResultsState.Idle) }
            return
        }
        val filter = _state.value.filter
        _state.update { it.copy(results = SearchResultsState.Loading) }
        searchJob = viewModelScope.launch {
            val results = when (val result = repository.search(query, filter, page = 1)) {
                is AppResult.Success -> if (result.data.items.isEmpty()) {
                    SearchResultsState.Empty
                } else {
                    SearchResultsState.Success(
                        // TMDB potrafi zwrócić duplikaty – klucze listy muszą być unikalne.
                        items = result.data.items.distinctBy { it.mediaType to it.id },
                        page = result.data.page,
                        hasMore = result.data.hasMore,
                    )
                }
                is AppResult.Failure -> SearchResultsState.Error(result.error)
            }
            _state.update { it.copy(results = results) }
        }
    }

    companion object {
        const val DEBOUNCE_MS = 400L
    }
}
