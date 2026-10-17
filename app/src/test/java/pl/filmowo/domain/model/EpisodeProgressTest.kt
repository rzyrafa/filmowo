package pl.filmowo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeProgressTest {

    private val today = "2026-10-16"

    private fun ep(season: Int, episode: Int, watched: Boolean = false, airDate: String? = "2020-01-01") =
        EpisodeProgressInput(season, episode, airDate, watched)

    @Test
    fun `next episode follows the last watched one`() {
        val progress = computeShowProgress(
            listOf(ep(1, 1, watched = true), ep(1, 2), ep(1, 3, watched = true), ep(2, 1), ep(2, 2, airDate = "2027-01-01")),
            today,
        )
        // S1E2 pominięto, ale „następny” to odcinek po ostatnim obejrzanym (S1E3).
        assertEquals(EpisodeRef(2, 1), progress.nextEpisode)
    }

    @Test
    fun `next episode falls back to first skipped one and is null when all aired are watched`() {
        val skipped = computeShowProgress(listOf(ep(1, 1), ep(1, 2, watched = true)), today)
        assertEquals(EpisodeRef(1, 1), skipped.nextEpisode)

        val done = computeShowProgress(listOf(ep(1, 1, watched = true), ep(1, 2, airDate = "2027-01-01")), today)
        assertNull(done.nextEpisode)
    }

    @Test
    fun `progress counts aired regular episodes and finds last watched`() {
        val progress = computeShowProgress(
            listOf(
                ep(1, 1, watched = true), ep(1, 2, watched = true),
                ep(2, 1, watched = true), ep(2, 2), ep(2, 3),
            ),
            today,
        )
        assertEquals(3, progress.watchedCount)
        assertEquals(5, progress.airedCount)
        assertEquals(60, progress.percent)
        assertEquals(EpisodeRef(2, 1), progress.lastWatched)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `specials and unaired episodes are ignored`() {
        val progress = computeShowProgress(
            listOf(
                ep(0, 1, watched = true),
                ep(1, 1, watched = true),
                ep(1, 2, airDate = "2027-01-01"),
                ep(1, 3, airDate = null),
            ),
            today,
        )
        assertEquals(1, progress.watchedCount)
        assertEquals(1, progress.airedCount)
        assertEquals(EpisodeRef(1, 1), progress.lastWatched)
        assertTrue(progress.isComplete)
    }

    @Test
    fun `episode airing today counts as aired`() {
        assertTrue(isAired(today, today))
        assertFalse(isAired("2026-10-17", today))
        assertFalse(isAired(null, today))
    }

    @Test
    fun `empty show has zero progress`() {
        val progress = computeShowProgress(emptyList(), today)
        assertEquals(0, progress.percent)
        assertNull(progress.lastWatched)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `season progress includes all episodes in total`() {
        val progress = computeSeasonProgress(
            listOf(ep(1, 1, watched = true), ep(1, 2), ep(1, 3, airDate = "2030-01-01")),
            today,
        )
        assertEquals(SeasonProgress(watchedCount = 1, airedCount = 2, totalCount = 3), progress)
    }
}
