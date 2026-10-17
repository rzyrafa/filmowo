package pl.filmowo.feature.library

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pl.filmowo.domain.model.EpisodeRef
import pl.filmowo.domain.model.MediaFilter
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.ShowProgress
import pl.filmowo.domain.model.WatchStatus
import pl.filmowo.testutil.FakeEpisodeRepository
import pl.filmowo.testutil.FakeLibraryRepository
import pl.filmowo.testutil.FakeUserPreferences
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

    private val episodes = FakeEpisodeRepository()

    private val preferences = FakeUserPreferences()

    // Stan jest współdzielony tylko przy aktywnym obserwatorze (WhileSubscribed) – test go zapewnia.
    private fun TestScope.createViewModel() = LibraryViewModel(SavedStateHandle(), repository, episodes, preferences).also { vm ->
        backgroundScope.launch { vm.state.collect {} }
    }

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
        assertEquals(listOf(2, 3, 1), viewModel.state.value.entries.map { it.item.tmdbId })
    }

    @Test
    fun `filter limits items and counts`() = runTest(mainRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.onTabSelected(LibraryTab.WATCHED)
        viewModel.onFilterSelected(MediaFilter.SHOWS)
        runCurrent()
        assertEquals(listOf(3), viewModel.state.value.entries.map { it.item.tmdbId })
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
        assertEquals(listOf("Amadeusz", "Ślepnąc od świateł", "Żywot Briana"), viewModel.state.value.entries.map { it.item.metadata.title })
    }

    @Test
    fun `rating and year sorts put highest first and missing last`() = runTest(mainRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.onTabSelected(LibraryTab.WATCHED)
        viewModel.onSortSelected(LibrarySort.RATING)
        runCurrent()
        assertEquals(listOf(1, 3, 2), viewModel.state.value.entries.map { it.item.tmdbId })

        viewModel.onSortSelected(LibrarySort.YEAR)
        runCurrent()
        assertEquals(listOf(3, 2, 1), viewModel.state.value.entries.map { it.item.tmdbId })
    }

    @Test
    fun `library changes are reflected immediately`() = runTest(mainRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.onTabSelected(LibraryTab.PLANNED)
        runCurrent()
        repository.setStatus(metadata(4, MediaType.TV, "Dark", 2017), WatchStatus.WATCHED)
        runCurrent()
        assertEquals(emptyList<Int>(), viewModel.state.value.entries.map { it.item.tmdbId })
        assertEquals(4, viewModel.state.value.counts[LibraryTab.WATCHED])
    }

    @Test
    fun `search matches titles ignoring case and polish diacritics`() = runTest(mainRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.onTabSelected(LibraryTab.WATCHED)
        viewModel.onQueryChange("slepnac")
        runCurrent()
        assertEquals(listOf(3), viewModel.state.value.entries.map { it.item.tmdbId })
        assertEquals(1, viewModel.state.value.counts[LibraryTab.WATCHED])
        assertEquals(0, viewModel.state.value.counts[LibraryTab.PLANNED])

        viewModel.onQueryChange("  ŻYWOT ")
        runCurrent()
        assertEquals(listOf(1), viewModel.state.value.entries.map { it.item.tmdbId })
    }

    @Test
    fun `sort and filter are stored in preferences`() = runTest(mainRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.onSortSelected(LibrarySort.TITLE)
        viewModel.onFilterSelected(MediaFilter.MOVIES)
        runCurrent()
        assertEquals("TITLE", preferences.library.value.sort)
        assertEquals(MediaFilter.MOVIES, preferences.library.value.filter)

        // Nowy ekran (np. po ponownym uruchomieniu) startuje z zapisanymi ustawieniami.
        val restored = createViewModel()
        runCurrent()
        assertEquals(LibrarySort.TITLE, restored.state.value.sort)
        assertEquals(MediaFilter.MOVIES, restored.state.value.filter)
    }

    @Test
    fun `marking next episode from the tile updates the episode`() = runTest(mainRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.onMarkEpisodeWatched(5, EpisodeRef(2, 3))
        runCurrent()
        assertEquals(listOf(Triple(2, 3, true)), episodes.episodeChanges)
    }

    @Test
    fun `show progress is attached only to shows`() = runTest(mainRule.dispatcher) {
        episodes.progress.value = mapOf(
            5 to ShowProgress(6, 10, EpisodeRef(1, 6)),
            1 to ShowProgress(1, 1, EpisodeRef(1, 1)),
        )
        val viewModel = createViewModel()
        runCurrent()
        assertEquals(60, viewModel.state.value.entries.single().progress?.percent)

        viewModel.onTabSelected(LibraryTab.WATCHED)
        runCurrent()
        // Film o ID 1 nie dostaje postępu serialu o tym samym ID.
        assertEquals(null, viewModel.state.value.entries.first { it.item.tmdbId == 1 }.progress)
    }
}
