package pl.filmowo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDto(
    val page: Int = 1,
    val results: List<SearchItemDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 1,
)

/** Wynik wyszukiwania – wspólny dla filmów (`title`) i seriali (`name`). */
@Serializable
data class SearchItemDto(
    val id: Int,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    val overview: String? = null,
)

@Serializable
data class GenreDto(
    val id: Int,
    val name: String,
)

@Serializable
data class MovieDetailsDto(
    val id: Int,
    val title: String,
    @SerialName("original_title") val originalTitle: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    val runtime: Int? = null,
    val genres: List<GenreDto> = emptyList(),
)

@Serializable
data class TvDetailsDto(
    val id: Int,
    val name: String,
    @SerialName("original_name") val originalName: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
    val seasons: List<SeasonSummaryDto> = emptyList(),
)

@Serializable
data class SeasonSummaryDto(
    val id: Int,
    @SerialName("season_number") val seasonNumber: Int,
    val name: String? = null,
    @SerialName("episode_count") val episodeCount: Int = 0,
    @SerialName("air_date") val airDate: String? = null,
)

@Serializable
data class SeasonDetailsDto(
    @SerialName("season_number") val seasonNumber: Int,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    val episodes: List<EpisodeDto> = emptyList(),
)

@Serializable
data class EpisodeDto(
    @SerialName("season_number") val seasonNumber: Int,
    @SerialName("episode_number") val episodeNumber: Int,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    val runtime: Int? = null,
)
