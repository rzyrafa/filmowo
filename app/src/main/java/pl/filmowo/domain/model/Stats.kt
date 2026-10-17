package pl.filmowo.domain.model

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class WatchStats(
    val moviesWatched: Int,
    /** Łącznie z powtórnymi obejrzeniami. */
    val movieMinutes: Long,
    val episodesWatched: Int,
    val episodeMinutes: Long,
    val movieRewatches: Int = 0,
    val showsCompleted: Int = 0,
    val averageRating: Double? = null,
    val ratedCount: Int = 0,
    /** Czas oglądania w bieżącym roku kalendarzowym (filmy wg daty pierwszego obejrzenia). */
    val thisYearMinutes: Long = 0,
    val topGenres: List<GenreShare> = emptyList(),
    /** Ostatnie [MONTHS_IN_CHART] miesięcy, od najstarszego, łącznie z bieżącym. */
    val monthly: List<MonthActivity> = emptyList(),
) {
    val totalMinutes: Long get() = movieMinutes + episodeMinutes
    val isEmpty: Boolean get() = moviesWatched == 0 && episodesWatched == 0
}

data class GenreShare(val name: String, val count: Int)

data class MonthActivity(val month: YearMonth, val minutes: Long, val count: Int)

/** Obejrzany odcinek – tylko dane potrzebne do statystyk. */
data class WatchedEpisode(val watchedAt: Long, val runtimeMinutes: Int?)

const val MONTHS_IN_CHART = 6
private const val TOP_GENRES = 5

fun computeWatchStats(
    library: List<LibraryItem>,
    episodes: List<WatchedEpisode>,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): WatchStats {
    val movies = library.filter { it.mediaType == MediaType.MOVIE && it.status == WatchStatus.WATCHED }
    val movieMinutes = movies.sumOf { (it.metadata.runtimeMinutes ?: 0).toLong() * (1 + it.rewatchCount) }
    val rated = library.mapNotNull { it.rating }

    // Aktywność w czasie: odcinki wg daty odhaczenia, filmy wg daty (pierwszego) obejrzenia.
    val events = episodes.map { it.watchedAt to (it.runtimeMinutes ?: 0).toLong() } +
        movies.mapNotNull { movie -> movie.watchedAt?.let { it to (movie.metadata.runtimeMinutes ?: 0).toLong() } }
    fun Long.month(): YearMonth = YearMonth.from(Instant.ofEpochMilli(this).atZone(zone))
    val current = now.month()
    val byMonth = events.groupBy { it.first.month() }
    val monthly = (MONTHS_IN_CHART - 1 downTo 0).map { back ->
        val month = current.minusMonths(back.toLong())
        val inMonth = byMonth[month].orEmpty()
        MonthActivity(month, inMonth.sumOf { it.second }, inMonth.size)
    }
    val thisYearMinutes = byMonth.filterKeys { it.year == current.year }.values.sumOf { list -> list.sumOf { it.second } }

    val topGenres = library
        .filter { it.status != WatchStatus.PLANNED }
        .flatMap { it.metadata.genres }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(TOP_GENRES)
        .map { GenreShare(it.key, it.value) }

    return WatchStats(
        moviesWatched = movies.size,
        movieMinutes = movieMinutes,
        episodesWatched = episodes.size,
        episodeMinutes = episodes.sumOf { (it.runtimeMinutes ?: 0).toLong() },
        movieRewatches = movies.sumOf { it.rewatchCount },
        showsCompleted = library.count { it.mediaType == MediaType.TV && it.status == WatchStatus.WATCHED },
        averageRating = rated.takeIf { it.isNotEmpty() }?.average(),
        ratedCount = rated.size,
        thisYearMinutes = thisYearMinutes,
        topGenres = topGenres,
        monthly = monthly,
    )
}

/** Czas rozbity na dni, godziny i minuty – do wyświetlenia np. „2 dni 5 godz. 30 min”. */
data class DurationParts(
    val days: Long,
    val hours: Long,
    val minutes: Long,
)

fun splitDuration(totalMinutes: Long): DurationParts {
    val safe = totalMinutes.coerceAtLeast(0)
    return DurationParts(
        days = safe / (24 * 60),
        hours = safe % (24 * 60) / 60,
        minutes = safe % 60,
    )
}
