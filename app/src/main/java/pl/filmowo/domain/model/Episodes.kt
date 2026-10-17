package pl.filmowo.domain.model

/** Sezon z listą odcinków pobrany z TMDB. */
data class SeasonDetails(
    val seasonNumber: Int,
    val name: String?,
    val airDate: String?,
    val episodes: List<EpisodeInfo>,
)

data class EpisodeInfo(
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String?,
    val overview: String,
    /** Data premiery w formacie ISO (yyyy-MM-dd); null, gdy nieznana. */
    val airDate: String?,
    val runtimeMinutes: Int?,
)

/** Odcinek zapisany lokalnie, z informacją, czy (i kiedy) został obejrzany. */
data class Episode(
    val info: EpisodeInfo,
    val watchedAt: Long?,
) {
    val isWatched: Boolean get() = watchedAt != null
}

/** Minimalne dane odcinka potrzebne do liczenia postępu. */
data class EpisodeProgressInput(
    val seasonNumber: Int,
    val episodeNumber: Int,
    val airDate: String?,
    val isWatched: Boolean,
)

data class EpisodeRef(val seasonNumber: Int, val episodeNumber: Int)

/** Odcinek serialu z biblioteki z datą premiery – do listy „Premiery” i powiadomień. */
data class UpcomingEpisode(
    val showId: Int,
    val showTitle: String,
    val posterPath: String?,
    val localPosterFile: String?,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String?,
    /** Data premiery w formacie ISO (yyyy-MM-dd). */
    val airDate: String,
    val isWatched: Boolean,
)

/**
 * Postęp oglądania serialu. Liczą się tylko wyemitowane odcinki
 * zwykłych sezonów (sezon 0 – odcinki specjalne – jest pomijany).
 */
data class ShowProgress(
    val watchedCount: Int,
    val airedCount: Int,
    /** Ostatni (najdalszy) obejrzany odcinek zwykłego sezonu. */
    val lastWatched: EpisodeRef?,
    /** Następny wyemitowany, nieobejrzany odcinek – po ostatnim obejrzanym, a gdy go nie ma, pierwszy pominięty. */
    val nextEpisode: EpisodeRef? = null,
) {
    val percent: Int get() = if (airedCount == 0) 0 else (watchedCount * 100 / airedCount).coerceAtMost(100)
    val isComplete: Boolean get() = airedCount > 0 && watchedCount >= airedCount
}

data class SeasonProgress(
    val watchedCount: Int,
    val airedCount: Int,
    val totalCount: Int,
) {
    val isComplete: Boolean get() = airedCount > 0 && watchedCount >= airedCount
}

/** Lokalnie zapisany sezon razem z postępem. */
data class TrackedSeason(
    val seasonNumber: Int,
    val name: String?,
    val year: Int?,
    val progress: SeasonProgress,
) {
    val isSpecials: Boolean get() = seasonNumber == 0
}

/** Czy odcinek został już wyemitowany (daty ISO można porównywać jak tekst). */
fun isAired(airDate: String?, today: String): Boolean = airDate != null && airDate <= today

fun computeShowProgress(episodes: List<EpisodeProgressInput>, today: String): ShowProgress {
    val regularAired = episodes.filter { it.seasonNumber > 0 && isAired(it.airDate, today) }
    val order = compareBy<EpisodeProgressInput>({ it.seasonNumber }, { it.episodeNumber })
    val lastWatched = episodes
        .filter { it.seasonNumber > 0 && it.isWatched }
        .maxWithOrNull(order)
    val unwatched = regularAired.filterNot { it.isWatched }.sortedWith(order)
    val next = lastWatched?.let { last -> unwatched.firstOrNull { order.compare(it, last) > 0 } } ?: unwatched.firstOrNull()
    return ShowProgress(
        watchedCount = regularAired.count { it.isWatched },
        airedCount = regularAired.size,
        lastWatched = lastWatched?.let { EpisodeRef(it.seasonNumber, it.episodeNumber) },
        nextEpisode = next?.let { EpisodeRef(it.seasonNumber, it.episodeNumber) },
    )
}

fun computeSeasonProgress(episodes: List<EpisodeProgressInput>, today: String): SeasonProgress {
    val aired = episodes.filter { isAired(it.airDate, today) }
    return SeasonProgress(
        watchedCount = aired.count { it.isWatched },
        airedCount = aired.size,
        totalCount = episodes.size,
    )
}
