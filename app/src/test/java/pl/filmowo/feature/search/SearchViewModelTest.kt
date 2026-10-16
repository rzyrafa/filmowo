package pl.filmowo.feature.search

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.filmowo.core.network.AppError
import pl.filmowo.core.network.AppResult
import pl.filmowo.domain.model.MediaFilter
import pl.filmowo.domain.model.SearchPage
import pl.filmowo.testutil.FakeConnectivityObserver
import pl.filmowo.testutil.FakeTmdbRepository
import pl.filmowo.testutil.MainDispatcherRule
import pl.filmowo.testutil.searchResult

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeTmdbRepository()
    private val connectivity = FakeConnectivityObserver()

    private fun createViewModel() = SearchViewModel(repository, connectivity)

    private fun TestScope.typeAndWait(viewModel: SearchViewModel, query: String) {
        viewModel.onQueryChange(query)
        advanceTimeBy(SearchViewModel.DEBOUNCE_MS + 1)
        runCurrent()
    }

    @Test
    fun `initial state is idle`() = runTest(mainRule.dispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        assertEquals(SearchResultsState.Idle, viewModel.state.value.results)
        assertTrue(repository.searchCalls.isEmpty())
    }

    @Test
    fun `search is debounced and only the last query is sent`() = runTest(mainRule.dispatcher) {
        repository.searchHandler = { AppResult.Success(SearchPage(listOf(searchResult(1)), 1, 1)) }
        val viewModel = createViewModel()

        viewModel.onQueryChange("inc")
        advanceTimeBy(100)
        viewModel.onQueryChange("incepcja ")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MS + 1)
        runCurrent()

        assertEquals(listOf(FakeTmdbRepository.SearchCall("incepcja", MediaFilter.ALL, 1)), repository.searchCalls)
        val results = viewModel.state.value.results as SearchResultsState.Success
        assertEquals(1, results.items.size)
    }

    @Test
    fun `empty response gives empty state`() = runTest(mainRule.dispatcher) {
        val viewModel = createViewModel()
        typeAndWait(viewModel, "xyz")
        assertEquals(SearchResultsState.Empty, viewModel.state.value.results)
    }

    @Test
    fun `error is exposed and retry repeats the request`() = runTest(mainRule.dispatcher) {
        repository.searchHandler = { AppResult.Failure(AppError.SERVER) }
        val viewModel = createViewModel()
        typeAndWait(viewModel, "matrix")
        assertEquals(SearchResultsState.Error(AppError.SERVER), viewModel.state.value.results)

        repository.searchHandler = { AppResult.Success(SearchPage(listOf(searchResult(1)), 1, 1)) }
        viewModel.retry()
        runCurrent()
        assertTrue(viewModel.state.value.results is SearchResultsState.Success)
        assertEquals(2, repository.searchCalls.size)
    }

    @Test
    fun `network error is retried automatically when connection returns`() = runTest(mainRule.dispatcher) {
        connectivity.isOnline.value = false
        repository.searchHandler = { AppResult.Failure(AppError.NETWORK) }
        val viewModel = createViewModel()
        typeAndWait(viewModel, "matrix")
        assertEquals(SearchResultsState.Error(AppError.NETWORK), viewModel.state.value.results)

        repository.searchHandler = { AppResult.Success(SearchPage(listOf(searchResult(1)), 1, 1)) }
        connectivity.isOnline.value = true
        runCurrent()
        assertTrue(viewModel.state.value.results is SearchResultsState.Success)
    }

    @Test
    fun `changing filter re-runs search with that filter`() = runTest(mainRule.dispatcher) {
        val viewModel = createViewModel()
        typeAndWait(viewModel, "dark")
        viewModel.onFilterChange(MediaFilter.SHOWS)
        runCurrent()
        assertEquals(MediaFilter.SHOWS, repository.searchCalls.last().filter)
    }

    @Test
    fun `load more appends next page without duplicates`() = runTest(mainRule.dispatcher) {
        repository.searchHandler = { call ->
            when (call.page) {
                1 -> AppResult.Success(SearchPage(listOf(searchResult(1), searchResult(2)), 1, 2))
                else -> AppResult.Success(SearchPage(listOf(searchResult(2), searchResult(3)), 2, 2))
            }
        }
        val viewModel = createViewModel()
        typeAndWait(viewModel, "star")
        viewModel.loadMore()
        runCurrent()

        val results = viewModel.state.value.results as SearchResultsState.Success
        assertEquals(listOf(1, 2, 3), results.items.map { it.id })
        assertEquals(false, results.hasMore)
    }

    @Test
    fun `clearing query returns to idle`() = runTest(mainRule.dispatcher) {
        repository.searchHandler = { AppResult.Success(SearchPage(listOf(searchResult(1)), 1, 1)) }
        val viewModel = createViewModel()
        typeAndWait(viewModel, "star")
        typeAndWait(viewModel, "")
        assertEquals(SearchResultsState.Idle, viewModel.state.value.results)
    }
}
