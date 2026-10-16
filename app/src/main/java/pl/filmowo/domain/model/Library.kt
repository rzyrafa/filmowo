package pl.filmowo.domain.model

enum class WatchStatus { WATCHING, PLANNED, WATCHED }

/** Dane z TMDB zapisywane lokalnie, żeby biblioteka działała offline. */
data class LibraryMetadata(
    val tmdbId: Int,
    val mediaType: MediaType,
    val title: String,
    val originalTitle: String?,
    val overview: String,
    val year: Int?,
    val posterPath: String?,
    /** Film: czas trwania; serial: średni czas odcinka. */
    val runtimeMinutes: Int?,
    val genres: List<String>,
    val seasonCount: Int?,
    val episodeCount: Int?,
)

data class LibraryItem(
    val metadata: LibraryMetadata,
    val localPosterFile: String?,
    val status: WatchStatus,
    val rating: Int?,
    val addedAt: Long,
    val updatedAt: Long,
    val watchedAt: Long?,
) {
    val tmdbId: Int get() = metadata.tmdbId
    val mediaType: MediaType get() = metadata.mediaType
}

fun MovieDetails.toLibraryMetadata() = LibraryMetadata(
    tmdbId = id,
    mediaType = MediaType.MOVIE,
    title = title,
    originalTitle = originalTitle,
    overview = overview,
    year = year,
    posterPath = posterPath,
    runtimeMinutes = runtimeMinutes,
    genres = genres,
    seasonCount = null,
    episodeCount = null,
)

fun ShowDetails.toLibraryMetadata() = LibraryMetadata(
    tmdbId = id,
    mediaType = MediaType.TV,
    title = title,
    originalTitle = originalTitle,
    overview = overview,
    year = year,
    posterPath = posterPath,
    runtimeMinutes = episodeRuntimeMinutes,
    genres = genres,
    seasonCount = seasonCount,
    episodeCount = episodeCount,
)

fun LibraryMetadata.toMovieDetails() = MovieDetails(
    id = tmdbId,
    title = title,
    originalTitle = originalTitle,
    overview = overview,
    posterPath = posterPath,
    year = year,
    runtimeMinutes = runtimeMinutes,
    genres = genres,
)

/** Sezony nie są jeszcze zapisywane lokalnie – lista sezonów offline jest pusta. */
fun LibraryMetadata.toShowDetails() = ShowDetails(
    id = tmdbId,
    title = title,
    originalTitle = originalTitle,
    overview = overview,
    posterPath = posterPath,
    year = year,
    seasonCount = seasonCount ?: 0,
    episodeCount = episodeCount ?: 0,
    episodeRuntimeMinutes = runtimeMinutes,
    genres = genres,
    seasons = emptyList(),
)
