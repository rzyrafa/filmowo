package pl.filmowo.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.filmowo.data.local.FilmowoDatabase
import pl.filmowo.data.local.LibraryItemEntity
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStatus
import pl.filmowo.testutil.FakeLibraryRepository
import pl.filmowo.testutil.episodeEntity

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class JsonBackupRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val source = newDatabase()
    private val target = newDatabase()

    private fun newDatabase() = Room.inMemoryDatabaseBuilder(context, FilmowoDatabase::class.java).allowMainThreadQueries().build()

    private fun repository(database: FilmowoDatabase) =
        JsonBackupRepository(context, database, FakeLibraryRepository(), { 1_000L }, Dispatchers.Unconfined)

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    private fun item(id: Int, type: MediaType, title: String, localPoster: String? = null) = LibraryItemEntity(
        tmdbId = id, mediaType = type, title = title, originalTitle = null, overview = "Opis", year = 2020,
        posterPath = "/p$id.jpg", localPosterFile = localPoster, runtimeMinutes = 50, genres = listOf("Dramat", "Komedia"),
        seasonCount = 1, episodeCount = 2, status = WatchStatus.WATCHED, rating = 8, addedAt = 1, updatedAt = 2, watchedAt = 3,
        note = "Notatka", rewatchCount = 1,
    )

    @Test
    fun `backup round trip restores items, seasons and watched episodes`() = runTest {
        source.libraryDao().upsert(item(1, MediaType.MOVIE, "Film", localPoster = "/files/posters/movie_1.jpg"))
        source.libraryDao().upsert(item(2, MediaType.TV, "Serial"))
        source.episodeDao().replaceShow(
            showId = 2,
            seasons = listOf(pl.filmowo.data.local.SeasonEntity(2, 1, "Sezon 1", "2020-01-01", 2)),
            episodes = listOf(episodeEntity(1, 1, watchedAt = 10L, showId = 2), episodeEntity(1, 2, showId = 2)),
        )
        val text = repository(source).let { it.encode(it.createBackup()) }

        // Pozycja już obecna na urządzeniu zostaje zastąpiona; inne pozycje zostają.
        target.libraryDao().upsert(item(2, MediaType.TV, "Stary tytuł", localPoster = "/files/posters/tv_2.jpg"))
        target.libraryDao().upsert(item(3, MediaType.MOVIE, "Tylko lokalnie"))
        val result = repository(target).restore(text)

        assertEquals(ImportResult.Success(2), result)
        val restored = target.libraryDao().getAll().associateBy { it.tmdbId }
        assertEquals(setOf(1, 2, 3), restored.keys)
        assertEquals("Serial", restored.getValue(2).title)
        assertEquals("Notatka", restored.getValue(1).note)
        assertEquals(1, restored.getValue(1).rewatchCount)
        // Ścieżka plakatu z innego urządzenia nie jest przenoszona; lokalny plakat zostaje.
        assertEquals(null, restored.getValue(1).localPosterFile)
        assertEquals("/files/posters/tv_2.jpg", restored.getValue(2).localPosterFile)
        assertEquals(listOf(10L, null), target.episodeDao().getEpisodes(2).sortedBy { it.episodeNumber }.map { it.watchedAt })
        assertEquals(1, target.episodeDao().getAllSeasons().size)
    }

    @Test
    fun `invalid files are rejected without changes`() = runTest {
        target.libraryDao().upsert(item(3, MediaType.MOVIE, "Tylko lokalnie"))
        val repository = repository(target)
        assertEquals(ImportResult.InvalidFile, repository.restore("to nie jest json"))
        assertEquals(ImportResult.InvalidFile, repository.restore("""{"version": 99, "exportedAt": 0}"""))
        assertEquals(1, target.libraryDao().getAll().size)
    }
}
