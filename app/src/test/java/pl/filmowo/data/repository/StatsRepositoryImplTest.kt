package pl.filmowo.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.filmowo.data.local.LibraryItemEntity
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStatus
import pl.filmowo.testutil.FakeEpisodeDao
import pl.filmowo.testutil.FakeLibraryDao
import pl.filmowo.testutil.episodeEntity

class StatsRepositoryImplTest {

    private val libraryDao = FakeLibraryDao()
    private val episodeDao = FakeEpisodeDao()
    private val repository = StatsRepositoryImpl(libraryDao, episodeDao) { 0L }

    private fun entity(id: Int, type: MediaType, status: WatchStatus, runtime: Int?) = LibraryItemEntity(
        tmdbId = id, mediaType = type, title = "T$id", originalTitle = null, overview = "", year = null,
        posterPath = null, localPosterFile = null, runtimeMinutes = runtime, genres = emptyList(),
        seasonCount = null, episodeCount = null, status = status, rating = null, addedAt = 0, updatedAt = 0, watchedAt = null,
    )

    @Test
    fun `empty library gives empty stats`() = runTest {
        val stats = repository.observeStats().first()
        assertTrue(stats.isEmpty)
        assertEquals(0L, stats.totalMinutes)
    }

    @Test
    fun `only watched movies and watched episodes are counted`() = runTest {
        libraryDao.upsert(entity(1, MediaType.MOVIE, WatchStatus.WATCHED, 120))
        libraryDao.upsert(entity(2, MediaType.MOVIE, WatchStatus.WATCHED, null))
        libraryDao.upsert(entity(3, MediaType.MOVIE, WatchStatus.PLANNED, 90))
        // Serial ze statusem „Obejrzane” liczy się przez odcinki, nie jako film.
        libraryDao.upsert(entity(4, MediaType.TV, WatchStatus.WATCHED, 45))
        episodeDao.upsertEpisodes(
            listOf(
                episodeEntity(1, 1, watchedAt = 1L, showId = 4),
                episodeEntity(1, 2, watchedAt = 2L, showId = 4),
                episodeEntity(0, 1, watchedAt = 3L, showId = 4),
                episodeEntity(1, 3, watchedAt = null, showId = 4),
            ),
        )

        val stats = repository.observeStats().first()
        assertEquals(2, stats.moviesWatched)
        assertEquals(120L, stats.movieMinutes)
        assertEquals(3, stats.episodesWatched)
        assertEquals(135L, stats.episodeMinutes)
        assertEquals(1, stats.showsCompleted)
    }
}
