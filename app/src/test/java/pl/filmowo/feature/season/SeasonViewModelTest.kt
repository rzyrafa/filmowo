package pl.filmowo.feature.season

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.filmowo.core.network.AppError
import pl.filmowo.core.network.AppResult
import pl.filmowo.core.ui.UiState
import pl.filmowo.domain.model.Episode
import pl.filmowo.domain.model.WatchStatus
import pl.filmowo.domain.model.toLibraryMetadata
import pl.filmowo.testutil.FakeConnectivityObserver
import pl.filmowo.testutil.FakeEpisodeRepository
import pl.filmowo.testutil.FakeLibraryRepository
import pl.filmowo.testutil.FakeTmdbRepository
import pl.filmowo.testutil.MainDispatcherRule
import pl.filmowo.testutil.episodeInfo
import pl.filmowo.testutil.libraryItem
import pl.filmowo.testutil.seasonDetails
import pl.filmowo.testutil.show
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class SeasonViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val tmdb = FakeTmdbRepository()
    private val library = FakeLibraryRepository()
    private val episodes = FakeEpisodeRepository()
    private val connectivity = FakeConnectivityObserver()
    private val now = LocalDate.of(2026, 10, 16).atStartOfDay(ZoneId.systemDefault()).plusHours(12).toInstant().toEpochMilli()

    private fun viewModel() = SeasonViewModel(
        SavedStateHandle(mapOf("showId" to 1, "seasonNumber" to 1, "title" to "Sezon 1")),
        tmdb, library, episodes, connectivity, { now },
    )

    private fun storeEpisodes(vararg list: Episode) {
        episodes.episodes.value = mapOf((1 to 1) to list.toList())
    }

    @Test
    fun `tracked show shows local episodes with progress`() = runTest(mainRule.dispatcher) {
        library.put(libraryItem(show().toLibraryMetadata(), WatchStatus.WATCHING))
        storeEpisodes(
            Episode(episodeInfo(1, 1), watchedAt = 5L),
            Episode(episodeInfo(1, 2), watchedAt = null),
            Episode(episodeInfo(1, 3, airDate = "2027-01-01"), watchedAt = null),
        )
        val viewModel = viewModel()
        runCurrent()

        val state = viewModel.state.value
        assertTrue(state.isTracked)
        val rows = (state.episodes as UiState.Success).data
        assertEquals(listOf(true, false, false), rows.map { it.isWatched })
        assertEquals(listOf(true, true, false), rows.map { it.isAired })
        assertEquals(1, state.progress?.watchedCount)
        assertEquals(0, tmdb.seasonCalls.size)
    }

    @Test
    fun `toggling episode and season goes to repository`() = runTest(mainRule.dispatcher) {
        library.put(libraryItem(show().toLibraryMetadata(), WatchStatus.WATCHING))
        storeEpisodes(Episode(episodeInfo(1, 1), watchedAt = null))
        val viewModel = viewModel()
        runCurrent()

        viewModel.onEpisodeWatchedChange(1, true)
        viewModel.onSeasonWatchedChange(true)
        runCurrent()
        assertEquals(listOf(Triple(1, 1, true)), episodes.episodeChanges)
        assertEquals(listOf(Triple(1, 1, true)), episodes.seasonChanges)
        assertTrue((viewModel.state.value.episodes as UiState.Success).data.single().isWatched)
    }

    @Test
    fun `show outside library is read only from network`() = runTest(mainRule.dispatcher) {
        tmdb.seasonHandler = { _, _ -> AppResult.Success(seasonDetails(1, 4)) }
        val viewModel = viewModel()
        runCurrent()

        val state = viewModel.state.value
        assertFalse(state.isTracked)
        assertFalse(state.inLibrary)
        assertEquals(4, (state.episodes as UiState.Success).data.size)

        viewModel.onEpisodeWatchedChange(1, true)
        runCurrent()
        assertTrue(episodes.episodeChanges.isEmpty())
        assertTrue(episodes.syncCalls.isEmpty())
    }

    @Test
    fun `library show without episodes triggers sync`() = runTest(mainRule.dispatcher) {
        library.put(libraryItem(show().toLibraryMetadata(), WatchStatus.PLANNED))
        tmdb.seasonHandler = { _, _ -> AppResult.Success(seasonDetails(1, 2)) }
        val viewModel = viewModel()
        runCurrent()

        assertEquals(listOf(1), episodes.syncCalls)
        assertTrue(viewModel.state.value.inLibrary)
        assertFalse(viewModel.state.value.isTracked)
    }

    @Test
    fun `network error is retried when connection returns`() = runTest(mainRule.dispatcher) {
        connectivity.isOnline.value = false
        tmdb.seasonHandler = { _, _ -> AppResult.Failure(AppError.NETWORK) }
        val viewModel = viewModel()
        runCurrent()
        assertEquals(UiState.Error(AppError.NETWORK), viewModel.state.value.episodes)

        tmdb.seasonHandler = { _, _ -> AppResult.Success(seasonDetails(1, 2)) }
        connectivity.isOnline.value = true
        runCurrent()
        assertEquals(2, (viewModel.state.value.episodes as UiState.Success).data.size)
    }
}
