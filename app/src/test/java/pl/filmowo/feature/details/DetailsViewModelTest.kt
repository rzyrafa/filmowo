package pl.filmowo.feature.details

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.filmowo.core.network.AppError
import pl.filmowo.core.network.AppResult
import pl.filmowo.core.ui.UiState
import pl.filmowo.testutil.FakeConnectivityObserver
import pl.filmowo.testutil.FakeTmdbRepository
import pl.filmowo.testutil.MainDispatcherRule
import pl.filmowo.testutil.movie
import pl.filmowo.testutil.show

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeTmdbRepository()
    private val connectivity = FakeConnectivityObserver()
    private val savedState = SavedStateHandle(mapOf("id" to 27205))

    @Test
    fun `movie details start loading and then show data`() = runTest(mainRule.dispatcher) {
        val viewModel = MovieDetailsViewModel(savedState, repository, connectivity)
        assertEquals(UiState.Loading, viewModel.state.value)
        runCurrent()
        assertEquals(UiState.Success(movie()), viewModel.state.value)
    }

    @Test
    fun `movie details error can be retried`() = runTest(mainRule.dispatcher) {
        repository.movieResult = AppResult.Failure(AppError.SERVER)
        val viewModel = MovieDetailsViewModel(savedState, repository, connectivity)
        runCurrent()
        assertEquals(UiState.Error(AppError.SERVER), viewModel.state.value)

        repository.movieResult = AppResult.Success(movie())
        viewModel.retry()
        runCurrent()
        assertTrue(viewModel.state.value is UiState.Success)
        assertEquals(2, repository.movieCalls)
    }

    @Test
    fun `show details reload automatically after reconnect`() = runTest(mainRule.dispatcher) {
        connectivity.isOnline.value = false
        repository.showResult = AppResult.Failure(AppError.NETWORK)
        val viewModel = ShowDetailsViewModel(savedState, repository, connectivity)
        runCurrent()
        assertEquals(UiState.Error(AppError.NETWORK), viewModel.state.value)

        repository.showResult = AppResult.Success(show())
        connectivity.isOnline.value = true
        runCurrent()
        assertEquals(UiState.Success(show()), viewModel.state.value)
    }

    @Test
    fun `non network error is not retried on reconnect`() = runTest(mainRule.dispatcher) {
        connectivity.isOnline.value = false
        repository.showResult = AppResult.Failure(AppError.NOT_FOUND)
        val viewModel = ShowDetailsViewModel(savedState, repository, connectivity)
        runCurrent()
        connectivity.isOnline.value = true
        runCurrent()
        assertEquals(UiState.Error(AppError.NOT_FOUND), viewModel.state.value)
        assertEquals(1, repository.showCalls)
    }
}
