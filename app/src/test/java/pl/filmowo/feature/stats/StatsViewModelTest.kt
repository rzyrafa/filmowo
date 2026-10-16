package pl.filmowo.feature.stats

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import pl.filmowo.data.repository.StatsRepository
import pl.filmowo.domain.model.DurationParts
import pl.filmowo.domain.model.WatchStats
import pl.filmowo.domain.model.splitDuration
import pl.filmowo.testutil.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val stats = MutableSharedFlow<WatchStats>(replay = 1)
    private val repository = object : StatsRepository {
        override fun observeStats() = stats
    }

    @Test
    fun `state is loading until stats arrive and then follows updates`() = runTest(mainRule.dispatcher) {
        val viewModel = StatsViewModel(repository)
        runCurrent()
        assertTrue(viewModel.state.value.isLoading)

        stats.emit(WatchStats(2, 240, 10, 450))
        runCurrent()
        assertEquals(690L, viewModel.state.value.stats?.totalMinutes)

        stats.emit(WatchStats(3, 330, 10, 450))
        runCurrent()
        assertEquals(3, viewModel.state.value.stats?.moviesWatched)
    }

    @Test
    fun `duration is split into days hours and minutes`() {
        assertEquals(DurationParts(0, 0, 0), splitDuration(0))
        assertEquals(DurationParts(0, 2, 28), splitDuration(148))
        assertEquals(DurationParts(2, 5, 30), splitDuration(2 * 24 * 60 + 5 * 60 + 30))
        assertEquals(DurationParts(0, 0, 0), splitDuration(-5))
    }
}
