package pl.filmowo.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import pl.filmowo.core.network.AppResult
import pl.filmowo.core.network.ConnectivityObserver
import pl.filmowo.data.repository.TmdbRepository
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.MovieDetails
import pl.filmowo.domain.model.SearchFilter
import pl.filmowo.domain.model.SearchPage
import pl.filmowo.domain.model.SearchResult
import pl.filmowo.domain.model.ShowDetails

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class FakeConnectivityObserver(online: Boolean = true) : ConnectivityObserver {
    override val isOnline = MutableStateFlow(online)
}

class FakeTmdbRepository : TmdbRepository {
    data class SearchCall(val query: String, val filter: SearchFilter, val page: Int)

    val searchCalls = mutableListOf<SearchCall>()
    var searchHandler: (SearchCall) -> AppResult<SearchPage> = { AppResult.Success(SearchPage(emptyList(), 1, 1)) }
    var movieResult: AppResult<MovieDetails> = AppResult.Success(movie())
    var showResult: AppResult<ShowDetails> = AppResult.Success(show())
    var movieCalls = 0
    var showCalls = 0

    override suspend fun search(query: String, filter: SearchFilter, page: Int): AppResult<SearchPage> {
        val call = SearchCall(query, filter, page)
        searchCalls += call
        return searchHandler(call)
    }

    override suspend fun movieDetails(id: Int): AppResult<MovieDetails> {
        movieCalls++
        return movieResult
    }

    override suspend fun showDetails(id: Int): AppResult<ShowDetails> {
        showCalls++
        return showResult
    }
}

fun searchResult(id: Int, type: MediaType = MediaType.MOVIE) =
    SearchResult(id, type, "Tytuł $id", 2020, null, "")

fun movie(id: Int = 1) = MovieDetails(id, "Incepcja", "Inception", "Opis", null, 2010, 148, listOf("Akcja"))

fun show(id: Int = 1) = ShowDetails(id, "Breaking Bad", null, "Opis", null, 2008, 5, 62, 47, emptyList(), emptyList())
