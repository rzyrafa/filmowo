package pl.filmowo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class WatchStatsTest {

    private val zone = ZoneOffset.UTC
    private fun millis(year: Int, month: Int, day: Int = 10) =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()

    private val now = millis(2026, 10, 16)

    private fun item(
        id: Int,
        type: MediaType,
        status: WatchStatus,
        runtime: Int? = 100,
        genres: List<String> = emptyList(),
        rating: Int? = null,
        watchedAt: Long? = null,
        rewatches: Int = 0,
    ) = LibraryItem(
        metadata = LibraryMetadata(id, type, "T$id", null, "", 2020, null, runtime, genres, null, null),
        localPosterFile = null,
        status = status,
        rating = rating,
        addedAt = 0,
        updatedAt = 0,
        watchedAt = watchedAt,
        rewatchCount = rewatches,
    )

    @Test
    fun `rewatches multiply movie time but not the movie count`() {
        val stats =
            computeWatchStats(listOf(item(1, MediaType.MOVIE, WatchStatus.WATCHED, runtime = 90, rewatches = 2)), emptyList(), now, zone)
        assertEquals(1, stats.moviesWatched)
        assertEquals(270L, stats.movieMinutes)
        assertEquals(2, stats.movieRewatches)
    }

    @Test
    fun `monthly activity covers the last six months including the current one`() {
        val stats = computeWatchStats(
            library = listOf(item(1, MediaType.MOVIE, WatchStatus.WATCHED, runtime = 120, watchedAt = millis(2026, 10))),
            episodes = listOf(
                WatchedEpisode(millis(2026, 9), 45),
                WatchedEpisode(millis(2026, 9), 45),
                WatchedEpisode(millis(2025, 12), 30),
                // Poza zakresem wykresu (ponad 6 miesięcy temu).
                WatchedEpisode(millis(2026, 1), 60),
            ),
            now = now,
            zone = zone,
        )
        assertEquals((5 downTo 0).map { YearMonth.of(2026, 10).minusMonths(it.toLong()) }, stats.monthly.map { it.month })
        assertEquals(listOf(0L, 0L, 0L, 0L, 90L, 120L), stats.monthly.map { it.minutes })
        assertEquals(listOf(0, 0, 0, 0, 2, 1), stats.monthly.map { it.count })
        // Bieżący rok: 120 + 90 + 60, bez grudnia 2025.
        assertEquals(270L, stats.thisYearMinutes)
    }

    @Test
    fun `genres, ratings and completed shows`() {
        val stats = computeWatchStats(
            library = listOf(
                item(1, MediaType.MOVIE, WatchStatus.WATCHED, genres = listOf("Dramat", "Komedia"), rating = 8),
                item(2, MediaType.TV, WatchStatus.WATCHED, genres = listOf("Dramat"), rating = 6),
                item(3, MediaType.TV, WatchStatus.WATCHING, genres = listOf("Kryminał")),
                // Planowane nie wpływają na ulubione gatunki.
                item(4, MediaType.MOVIE, WatchStatus.PLANNED, genres = listOf("Horror", "Horror")),
            ),
            episodes = emptyList(),
            now = now,
            zone = zone,
        )
        assertEquals(listOf(GenreShare("Dramat", 2), GenreShare("Komedia", 1), GenreShare("Kryminał", 1)), stats.topGenres)
        assertEquals(7.0, stats.averageRating!!, 0.001)
        assertEquals(2, stats.ratedCount)
        assertEquals(1, stats.showsCompleted)
    }

    @Test
    fun `no ratings gives no average`() {
        assertNull(computeWatchStats(emptyList(), emptyList(), now, zone).averageRating)
    }
}
