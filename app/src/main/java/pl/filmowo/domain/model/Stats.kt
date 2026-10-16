package pl.filmowo.domain.model

data class WatchStats(
    val moviesWatched: Int,
    val movieMinutes: Long,
    val episodesWatched: Int,
    val episodeMinutes: Long,
) {
    val totalMinutes: Long get() = movieMinutes + episodeMinutes
    val isEmpty: Boolean get() = moviesWatched == 0 && episodesWatched == 0
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
