package pl.filmowo.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStatus
import pl.filmowo.testutil.episodeEntity
import java.io.File

/** Zapytania SQL i migracje na prawdziwej bazie Room (SQLite w Robolectric). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FilmowoDatabaseTest {

    private val database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), FilmowoDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val libraryDao = database.libraryDao()
    private val episodeDao = database.episodeDao()

    @After
    fun tearDown() = database.close()

    private fun item(id: Int, type: MediaType, title: String = "Serial $id", status: WatchStatus = WatchStatus.WATCHING) =
        LibraryItemEntity(
            tmdbId = id, mediaType = type, title = title, originalTitle = null, overview = "", year = 2020,
            posterPath = "/p.jpg", localPosterFile = null, runtimeMinutes = 45, genres = listOf("Dramat"),
            seasonCount = 1, episodeCount = 3, status = status, rating = null, addedAt = 0, updatedAt = 0, watchedAt = null,
        )

    @Test
    fun `upcoming joins only library shows and orders by air date`() = runTest {
        libraryDao.upsert(item(1, MediaType.TV, "Beta"))
        libraryDao.upsert(item(2, MediaType.TV, "Alfa"))
        // Film z tym samym ID co serial 3 nie może podpiąć odcinków serialu spoza biblioteki.
        libraryDao.upsert(item(3, MediaType.MOVIE))
        episodeDao.upsertEpisodes(
            listOf(
                episodeEntity(1, 1, airDate = "2026-10-10", showId = 1),
                episodeEntity(1, 2, airDate = "2026-10-20", showId = 1),
                episodeEntity(1, 5, airDate = "2026-10-17", showId = 2),
                episodeEntity(1, 6, airDate = null, showId = 2),
                episodeEntity(1, 1, airDate = "2026-10-18", showId = 3),
            ),
        )

        val upcoming = episodeDao.observeUpcoming(from = "2026-10-17", limit = 10).first()
        assertEquals(listOf("Alfa" to 5, "Beta" to 2), upcoming.map { it.showTitle to it.episodeNumber })

        assertEquals(listOf(5), episodeDao.getAiringOn("2026-10-17").map { it.episodeNumber })
    }

    @Test
    fun `season and show updates keep other episodes untouched`() = runTest {
        episodeDao.upsertEpisodes(
            listOf(
                episodeEntity(1, 1, airDate = "2020-01-01", showId = 1),
                episodeEntity(1, 2, airDate = "2099-01-01", showId = 1),
                episodeEntity(1, 1, airDate = "2020-01-01", watchedAt = 5L, showId = 2),
            ),
        )
        episodeDao.markSeasonWatched(showId = 1, seasonNumber = 1, today = "2026-10-17", now = 9L)
        // Odcinek bez premiery nie zostaje odhaczony.
        assertEquals(listOf(9L, null), episodeDao.getEpisodes(1).sortedBy { it.episodeNumber }.map { it.watchedAt })

        episodeDao.clearShow(1)
        assertTrue(episodeDao.getEpisodes(1).all { it.watchedAt == null })
        assertEquals(5L, episodeDao.getEpisodes(2).single().watchedAt)

        assertEquals(listOf(2), episodeDao.observeWatchedEpisodes().first().map { it.showId })
    }

    @Test
    fun `note and rewatch columns are stored`() = runTest {
        libraryDao.upsert(item(1, MediaType.MOVIE, status = WatchStatus.WATCHED))
        libraryDao.setNote(1, MediaType.MOVIE, "Świetny", now = 1)
        libraryDao.setRewatchCount(1, MediaType.MOVIE, 2, now = 2)
        val stored = libraryDao.get(1, MediaType.MOVIE)!!
        assertEquals("Świetny", stored.note)
        assertEquals(2, stored.rewatchCount)
        assertEquals(2L, stored.updatedAt)
    }

    /**
     * Baza w wersji 2 tworzona wprost z wyeksportowanego schematu (schemas/…/2.json),
     * potem otwierana przez Room w bieżącej wersji – Room uruchamia migrację i sprawdza schemat.
     */
    @Test
    fun `migration 2 to 3 keeps library and adds defaults`() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = context.getDatabasePath(MIGRATION_DB).apply {
            parentFile?.mkdirs()
            delete()
        }
        val schema = Json.parseToJsonElement(File(SCHEMA_DIR, "2.json").readText()).jsonObject.getValue("database").jsonObject
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            schema.getValue("entities").jsonArray.forEach { entity ->
                val obj = entity.jsonObject
                val table = obj.getValue("tableName").jsonPrimitive.content
                db.execSQL(obj.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                obj["indices"]?.jsonArray?.forEach { index ->
                    db.execSQL(index.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                }
            }
            schema.getValue("setupQueries").jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
            db.execSQL(
                "INSERT INTO library_items (tmdbId, mediaType, title, originalTitle, overview, year, posterPath, localPosterFile, " +
                    "runtimeMinutes, genres, seasonCount, episodeCount, status, rating, addedAt, updatedAt, watchedAt) " +
                    "VALUES (7, 'MOVIE', 'Incepcja', NULL, '', 2010, NULL, NULL, 148, '[]', NULL, NULL, 'WATCHED', 9, 1, 1, 1)",
            )
            db.version = 2
        }

        val migrated = Room.databaseBuilder(context, FilmowoDatabase::class.java, MIGRATION_DB).allowMainThreadQueries().build()
        try {
            val item = migrated.libraryDao().get(7, MediaType.MOVIE)!!
            assertEquals("Incepcja", item.title)
            assertEquals(9, item.rating)
            assertNull(item.note)
            assertEquals(0, item.rewatchCount)
        } finally {
            migrated.close()
        }
    }

    @Test
    fun `missing item returns null`() = runTest {
        assertNull(libraryDao.get(42, MediaType.TV))
    }

    private companion object {
        const val MIGRATION_DB = "migration-test.db"

        // Testy jednostkowe uruchamiane są z katalogu modułu app.
        val SCHEMA_DIR = File("schemas/pl.filmowo.data.local.FilmowoDatabase")
    }
}
