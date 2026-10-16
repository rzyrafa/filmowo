package pl.filmowo.feature.details

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.filmowo.core.network.AppError
import pl.filmowo.core.network.AppResult
import pl.filmowo.core.ui.UiState
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStatus
import pl.filmowo.domain.model.toLibraryMetadata
import pl.filmowo.testutil.FakeConnectivityObserver
import pl.filmowo.testutil.FakeLibraryRepository
import pl.filmowo.testutil.FakeTmdbRepository
import pl.filmowo.testutil.MainDispatcherRule
import pl.filmowo.testutil.libraryItem
import pl.filmowo.testutil.movie
import pl.filmowo.testutil.show

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val tmdb = FakeTmdbRepository()
    private val library = FakeLibraryRepository()
    private val connectivity = FakeConnectivityObserver()
    private val savedState = SavedStateHandle(mapOf("id" to 1))

    private fun movieViewModel() = MovieDetailsViewModel(savedState, tmdb, library, connectivity)
    private fun showViewModel() = ShowDetailsViewModel(savedState, tmdb, library, connectivity)

    @Test
    fun `movie details start loading and then show data`() = runTest(mainRule.dispatcher) {
        val viewModel = movieViewModel()
        assertEquals(UiState.Loading, viewModel.state.value.content)
        runCurrent()
        assertEquals(UiState.Success(movie()), viewModel.state.value.content)
        assertNull(viewModel.state.value.library)
    }

    @Test
    fun `movie details error can be retried`() = runTest(mainRule.dispatcher) {
        tmdb.movieResult = AppResult.Failure(AppError.SERVER)
        val viewModel = movieViewModel()
        runCurrent()
        assertEquals(UiState.Error(AppError.SERVER), viewModel.state.value.content)

        tmdb.movieResult = AppResult.Success(movie())
        viewModel.retry()
        runCurrent()
        assertTrue(viewModel.state.value.content is UiState.Success)
        assertEquals(2, tmdb.movieCalls)
    }

    @Test
    fun `library item is shown offline from local data`() = runTest(mainRule.dispatcher) {
        library.put(libraryItem(movie().toLibraryMetadata(), WatchStatus.WATCHED, rating = 9))
        tmdb.movieResult = AppResult.Failure(AppError.NETWORK)
        val viewModel = movieViewModel()
        runCurrent()

        assertEquals(UiState.Success(movie()), viewModel.state.value.content)
        assertEquals(WatchStatus.WATCHED, viewModel.state.value.library?.status)
        assertEquals(9, viewModel.state.value.library?.rating)
    }

    @Test
    fun `successful fetch refreshes local metadata`() = runTest(mainRule.dispatcher) {
        movieViewModel()
        runCurrent()
        assertEquals(listOf(movie().toLibraryMetadata()), library.refreshed)
    }

    @Test
    fun `selecting status adds to library and second status replaces it`() = runTest(mainRule.dispatcher) {
        val viewModel = movieViewModel()
        runCurrent()
        viewModel.onStatusSelected(WatchStatus.PLANNED)
        runCurrent()
        assertEquals(WatchStatus.PLANNED, viewModel.state.value.library?.status)

        viewModel.onStatusSelected(WatchStatus.WATCHED)
        runCurrent()
        assertEquals(WatchStatus.WATCHED, viewModel.state.value.library?.status)
        assertEquals(1, library.items.value.size)
    }

    @Test
    fun `status cannot be set before details are loaded`() = runTest(mainRule.dispatcher) {
        tmdb.movieResult = AppResult.Failure(AppError.NETWORK)
        val viewModel = movieViewModel()
        runCurrent()
        viewModel.onStatusSelected(WatchStatus.PLANNED)
        runCurrent()
        assertTrue(library.items.value.isEmpty())
    }

    @Test
    fun `rating is ignored outside library and saved inside`() = runTest(mainRule.dispatcher) {
        val viewModel = movieViewModel()
        runCurrent()
        viewModel.onRatingChange(7)
        runCurrent()
        assertTrue(library.items.value.isEmpty())

        viewModel.onStatusSelected(WatchStatus.WATCHED)
        runCurrent()
        viewModel.onRatingChange(7)
        runCurrent()
        assertEquals(7, viewModel.state.value.library?.rating)
    }

    @Test
    fun `remove takes item out of library`() = runTest(mainRule.dispatcher) {
        library.put(libraryItem(show().toLibraryMetadata(), WatchStatus.PLANNED))
        val viewModel = showViewModel()
        runCurrent()
        viewModel.onRemove()
        runCurrent()
        assertNull(viewModel.state.value.library)
        assertTrue(library.items.value.isEmpty())
    }

    @Test
    fun `show details reload automatically after reconnect`() = runTest(mainRule.dispatcher) {
        connectivity.isOnline.value = false
        tmdb.showResult = AppResult.Failure(AppError.NETWORK)
        val viewModel = showViewModel()
        runCurrent()
        assertEquals(UiState.Error(AppError.NETWORK), viewModel.state.value.content)

        tmdb.showResult = AppResult.Success(show())
        connectivity.isOnline.value = true
        runCurrent()
        assertEquals(UiState.Success(show()), viewModel.state.value.content)
    }

    @Test
    fun `non network error is not retried on reconnect`() = runTest(mainRule.dispatcher) {
        connectivity.isOnline.value = false
        tmdb.showResult = AppResult.Failure(AppError.NOT_FOUND)
        val viewModel = showViewModel()
        runCurrent()
        connectivity.isOnline.value = true
        runCurrent()
        assertEquals(UiState.Error(AppError.NOT_FOUND), viewModel.state.value.content)
        assertEquals(1, tmdb.showCalls)
    }

    @Test
    fun `movie and show with same id are tracked separately`() = runTest(mainRule.dispatcher) {
        library.put(libraryItem(show().toLibraryMetadata(), WatchStatus.WATCHED))
        val viewModel = movieViewModel()
        runCurrent()
        assertNull(viewModel.state.value.library)
        assertEquals(MediaType.TV, library.items.value.values.single().mediaType)
    }
}
