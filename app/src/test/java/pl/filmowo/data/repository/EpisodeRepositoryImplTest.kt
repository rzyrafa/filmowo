package pl.filmowo.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.filmowo.core.network.AppError
import pl.filmowo.core.network.AppResult
import pl.filmowo.data.local.LibraryItemEntity
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.SeasonSummary
import pl.filmowo.domain.model.WatchStatus
import pl.filmowo.testutil.FakeEpisodeDao
import pl.filmowo.testutil.FakeLibraryDao
import pl.filmowo.testutil.FakeTmdbRepository
import pl.filmowo.testutil.episodeEntity
import pl.filmowo.testutil.seasonDetails
import pl.filmowo.testutil.show
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeRepositoryImplTest {

    private val tmdb = FakeTmdbRepository()
    private val libraryDao = FakeLibraryDao()
    private val episodeDao = FakeEpisodeDao()

    // 16.10.2026 w południe czasu lokalnego.
    private val now = LocalDate.of(2026, 10, 16).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun TestScope.repository() = EpisodeRepositoryImpl(tmdb, libraryDao, episodeDao, { now }, backgroundScope)

    private suspend fun addShow(status: WatchStatus, id: Int = SHOW_ID) {
        libraryDao.upsert(
            LibraryItemEntity(
                tmdbId = id, mediaType = MediaType.TV, title = "Serial", originalTitle = null, overview = "",
                year = 2020, posterPath = null, localPosterFile = null, runtimeMinutes = 45, genres = emptyList(),
                seasonCount = 2, episodeCount = 5, status = status, rating = null, addedAt = 0, updatedAt = 0, watchedAt = null,
            ),
        )
    }

    private suspend fun status() = libraryDao.get(SHOW_ID, MediaType.TV)?.status

    @Test
    fun `reset progress unchecks every episode and plans the show`() = runTest {
        addShow(WatchStatus.WATCHING)
        episodeDao.upsertEpisodes(listOf(episodeEntity(1, 1, watchedAt = 1L, showId = SHOW_ID), episodeEntity(1, 2, showId = SHOW_ID)))
        repository().resetProgress(SHOW_ID)
        assertTrue(episodeDao.episodes.value.all { it.watchedAt == null })
        assertEquals(WatchStatus.PLANNED, status())
    }

    @Test
    fun `sync all refreshes every show and finds new episodes of a watched show`() = runTest {
        addShow(WatchStatus.WATCHED)
        givenTmdbShow()
        // Wcześniej zapisany był tylko sezon 1, w całości obejrzany.
        episodeDao.upsertEpisodes((1..3).map { episodeEntity(1, it, watchedAt = 1L, showId = SHOW_ID) })

        assertEquals(AppResult.Success(Unit), repository().syncAll())
        // Sezon 2 ma wyemitowany, nieobejrzany odcinek – serial wraca do „Oglądam”.
        assertEquals(WatchStatus.WATCHING, status())
    }

    @Test
    fun `sync all stops on configuration errors`() = runTest {
        addShow(WatchStatus.WATCHING, id = 1)
        addShow(WatchStatus.WATCHING, id = 2)
        tmdb.showResult = AppResult.Failure(AppError.UNAUTHORIZED)
        assertEquals(AppResult.Failure(AppError.UNAUTHORIZED), repository().syncAll())
        assertEquals(1, tmdb.showCalls)
    }

    private fun givenTmdbShow() {
        tmdb.showResult = AppResult.Success(
            show(SHOW_ID).copy(
                seasons = listOf(SeasonSummary(1, null, 3, 2020), SeasonSummary(2, null, 2, 2021), SeasonSummary(0, null, 1, null)),
            ),
        )
        tmdb.seasonHandler = { _, season ->
            AppResult.Success(
                when (season) {
                    1 -> seasonDetails(1, 3)
                    2 -> seasonDetails(2, 2).let { s ->
                        // Ostatni odcinek sezonu 2 jeszcze się nie ukazał.
                        s.copy(episodes = s.episodes.mapIndexed { i, e -> if (i == 1) e.copy(airDate = "2027-01-01") else e })
                    }
                    else -> seasonDetails(0, 1)
                },
            )
        }
    }

    @Test
    fun `sync stores seasons and episodes and keeps watched state`() = runTest {
        addShow(WatchStatus.WATCHING)
        episodeDao.upsertEpisodes(listOf(episodeEntity(1, 2, watchedAt = 123L)))
        givenTmdbShow()

        assertEquals(AppResult.Success(Unit), repository().sync(SHOW_ID))

        assertEquals(listOf(1, 2, 0), episodeDao.observeSeasons(SHOW_ID).first().map { it.seasonNumber })
        assertEquals(6, episodeDao.episodes.value.size)
        assertEquals(123L, episodeDao.episodes.value.single { it.seasonNumber == 1 && it.episodeNumber == 2 }.watchedAt)
        assertEquals(WatchStatus.WATCHING, status())
    }

    @Test
    fun `sync failure leaves data untouched`() = runTest {
        addShow(WatchStatus.PLANNED)
        givenTmdbShow()
        tmdb.seasonHandler = { _, _ -> AppResult.Failure(AppError.NETWORK) }

        assertEquals(AppResult.Failure(AppError.NETWORK), repository().sync(SHOW_ID))
        assertTrue(episodeDao.episodes.value.isEmpty())
    }

    @Test
    fun `sync skips shows removed from library`() = runTest {
        givenTmdbShow()
        repository().sync(SHOW_ID)
        assertTrue(episodeDao.episodes.value.isEmpty())
    }

    @Test
    fun `first watched episode moves show to watching and unchecking moves it back`() = runTest {
        addShow(WatchStatus.PLANNED)
        givenTmdbShow()
        val repository = repository()
        repository.sync(SHOW_ID)

        repository.setEpisodeWatched(SHOW_ID, 1, 1, watched = true)
        assertEquals(WatchStatus.WATCHING, status())
        assertNotNull(episodeDao.episodes.value.single { it.seasonNumber == 1 && it.episodeNumber == 1 }.watchedAt)

        repository.setEpisodeWatched(SHOW_ID, 1, 1, watched = false)
        assertEquals(WatchStatus.PLANNED, status())
    }

    @Test
    fun `watching all aired episodes completes the show`() = runTest {
        addShow(WatchStatus.PLANNED)
        givenTmdbShow()
        val repository = repository()
        repository.sync(SHOW_ID)

        repository.setSeasonWatched(SHOW_ID, 1, watched = true)
        assertEquals(WatchStatus.WATCHING, status())
        repository.setSeasonWatched(SHOW_ID, 2, watched = true)
        // Nieemitowany odcinek nie jest odhaczany ani wymagany.
        assertNull(episodeDao.episodes.value.single { it.seasonNumber == 2 && it.episodeNumber == 2 }.watchedAt)
        assertEquals(WatchStatus.WATCHED, status())
        assertNotNull(libraryDao.get(SHOW_ID, MediaType.TV)?.watchedAt)

        repository.setSeasonWatched(SHOW_ID, 2, watched = false)
        assertEquals(WatchStatus.WATCHING, status())
    }

    @Test
    fun `mark show watched checks every aired regular episode but not specials`() = runTest {
        addShow(WatchStatus.WATCHED)
        givenTmdbShow()
        val repository = repository()

        // Brak zapisanych odcinków – repozytorium najpierw je pobiera.
        repository.markShowWatched(SHOW_ID)

        val watched = episodeDao.episodes.value.filter { it.watchedAt != null }
        assertEquals(4, watched.size)
        assertTrue(watched.none { it.seasonNumber == 0 })
        assertEquals(WatchStatus.WATCHED, status())
    }

    @Test
    fun `show marked watched offline gets its episodes checked after sync`() = runTest {
        addShow(WatchStatus.WATCHED)
        givenTmdbShow()
        repository().sync(SHOW_ID)

        assertEquals(4, episodeDao.episodes.value.count { it.watchedAt != null })
        assertEquals(WatchStatus.WATCHED, status())
    }

    @Test
    fun `new aired episode moves finished show back to watching`() = runTest {
        addShow(WatchStatus.WATCHED)
        episodeDao.upsertEpisodes(listOf(episodeEntity(1, 1, watchedAt = 1L, showId = SHOW_ID)))
        tmdb.showResult = AppResult.Success(show(SHOW_ID).copy(seasons = listOf(SeasonSummary(1, null, 2, 2020))))
        tmdb.seasonHandler = { _, _ -> AppResult.Success(seasonDetails(1, 2)) }

        repository().sync(SHOW_ID)
        assertEquals(WatchStatus.WATCHING, status())
    }

    @Test
    fun `progress is computed per show`() = runTest {
        addShow(WatchStatus.WATCHING)
        givenTmdbShow()
        val repository = repository()
        repository.sync(SHOW_ID)
        repository.setEpisodeWatched(SHOW_ID, 1, 1, watched = true)
        repository.setEpisodeWatched(SHOW_ID, 1, 2, watched = true)

        val progress = repository.observeProgress(SHOW_ID).first()
        assertEquals(2, progress.watchedCount)
        assertEquals(4, progress.airedCount)
        assertEquals(50, progress.percent)
        assertEquals(progress, repository.observeAllProgress().first()[SHOW_ID])

        val season1 = repository.observeSeasons(SHOW_ID).first().first()
        assertEquals(2, season1.progress.watchedCount)
        assertEquals(3, season1.progress.totalCount)
    }

    private companion object {
        const val SHOW_ID = 1
    }
}
