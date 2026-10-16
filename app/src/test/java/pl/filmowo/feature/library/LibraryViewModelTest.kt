package pl.filmowo.feature.library

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pl.filmowo.domain.model.MediaFilter
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStatus
import pl.filmowo.testutil.FakeLibraryRepository
import pl.filmowo.testutil.MainDispatcherRule
import pl.filmowo.testutil.libraryItem
import pl.filmowo.testutil.metadata

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeLibraryRepository().apply {
        put(libraryItem(metadata(1, MediaType.MOVIE, "Żywot Briana", 1979), WatchStatus.WATCHED, updatedAt = 10, rating = 9))
        put(libraryItem(metadata(2, MediaType.MOVIE, "Amadeusz", 1984), WatchStatus.WATCHED, updatedAt = 30))
        put(libraryItem(metadata(3, MediaType.TV, "Ślepnąc od świateł", 2018), WatchStatus.WATCHED, updatedAt = 20, rating = 7))
        put(libraryItem(metadata(4, MediaType.TV, "Dark", 2017), WatchStatus.PLANNED, updatedAt = 40))
        put(libraryItem(metadata(5, MediaType.TV, "Sukcesja", 2018), WatchStatus.WATCHING, updatedAt = 50))
    }

    private fun createViewModel() = LibraryViewModel(repository)

    @Test
    fun `counts reflect every tab`() = runTest(mainRule.dispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        val counts = viewModel.state.value.counts
        assertEquals(1, counts[LibraryTab.WATCHING])
        assertEquals(1, counts[LibraryTab.PLANNED])
        assertEquals(3, counts[LibraryTab.WATCHED])
    }

    @Test
    fun `tab shows only items with that status sorted by recent change`() = runTest(mainRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.onTabSelected(LibraryTab.WATCHED)
        runCurrent()
        assertEquals(listOf(2, 3, 1), viewModel.state.value.items.map { it.tmdbId })
    }

    @Test
    fun `filter limits items and counts`() = runTest(mainRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.onTabSelected(LibraryTab.WATCHED)
        viewModel.onFilterSelected(MediaFilter.SHOWS)
        runCurrent()
        assertEquals(listOf(3), viewModel.state.value.items.map { it.tmdbId })
        assertEquals(1, viewModel.state.value.counts[LibraryTab.WATCHING])
        assertEquals(1, viewModel.state.value.counts[LibraryTab.PLANNED])
        assertEquals(1, viewModel.state.value.counts[LibraryTab.WATCHED])

        viewModel.onFilterSelected(MediaFilter.MOVIES)
        runCurrent()
        assertEquals(0, viewModel.state.value.counts[LibraryTab.WATCHING])
        assertEquals(2, viewModel.state.value.counts[LibraryTab.WATCHED])
    }

    @Test
    fun `title sort uses polish alphabet`() = runTest(mainRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.onTabSelected(LibraryTab.WATCHED)
        viewModel.onSortSelected(LibrarySort.TITLE)
        runCurrent()
        // „Ś” po „S”, „Ż” na końcu – nie według kodów Unicode.
        assertEquals(listOf("Amadeusz", "Ślepnąc od świateł", "Żywot Briana"), viewModel.state.value.items.map { it.metadata.title })
    }

    @Test
    fun `rating and year sorts put highest first and missing last`() = runTest(mainRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.onTabSelected(LibraryTab.WATCHED)
        viewModel.onSortSelected(LibrarySort.RATING)
        runCurrent()
        assertEquals(listOf(1, 3, 2), viewModel.state.value.items.map { it.tmdbId })

        viewModel.onSortSelected(LibrarySort.YEAR)
        runCurrent()
        assertEquals(listOf(3, 2, 1), viewModel.state.value.items.map { it.tmdbId })
    }

    @Test
    fun `library changes are reflected immediately`() = runTest(mainRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.onTabSelected(LibraryTab.PLANNED)
        runCurrent()
        repository.setStatus(metadata(4, MediaType.TV, "Dark", 2017), WatchStatus.WATCHED)
        runCurrent()
        assertEquals(emptyList<Int>(), viewModel.state.value.items.map { it.tmdbId })
        assertEquals(4, viewModel.state.value.counts[LibraryTab.WATCHED])
    }
}
