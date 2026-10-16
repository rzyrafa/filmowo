package pl.filmowo.domain.model

enum class MediaType { MOVIE, TV }

/** Filtr typu (wyszukiwarka i biblioteka): wszystko, tylko filmy albo tylko seriale. */
enum class MediaFilter { ALL, MOVIES, SHOWS }

data class SearchResult(
    val id: Int,
    val mediaType: MediaType,
    val title: String,
    val year: Int?,
    val posterPath: String?,
    val overview: String,
)

data class SearchPage(
    val items: List<SearchResult>,
    val page: Int,
    val totalPages: Int,
) {
    val hasMore: Boolean get() = page < totalPages
}

data class MovieDetails(
    val id: Int,
    val title: String,
    val originalTitle: String?,
    val overview: String,
    val posterPath: String?,
    val year: Int?,
    val runtimeMinutes: Int?,
    val genres: List<String>,
)

data class ShowDetails(
    val id: Int,
    val title: String,
    val originalTitle: String?,
    val overview: String,
    val posterPath: String?,
    val year: Int?,
    val seasonCount: Int,
    val episodeCount: Int,
    val episodeRuntimeMinutes: Int?,
    val genres: List<String>,
    val seasons: List<SeasonSummary>,
)

data class SeasonSummary(
    val seasonNumber: Int,
    /** Nazwa z TMDB; null, gdy brak – UI pokazuje wtedy „Sezon N”. */
    val name: String?,
    val episodeCount: Int,
    val year: Int?,
) {
    /** Sezon 0 w TMDB to odcinki specjalne – nie liczą się do postępu. */
    val isSpecials: Boolean get() = seasonNumber == 0
}
