package pl.filmowo.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import pl.filmowo.core.di.NetworkModule
import pl.filmowo.core.network.AppError
import pl.filmowo.core.network.AppResult
import pl.filmowo.data.remote.TmdbApi
import pl.filmowo.data.remote.TmdbApi.Companion.LANGUAGE_FALLBACK
import pl.filmowo.data.remote.TmdbApi.Companion.LANGUAGE_PRIMARY
import pl.filmowo.data.remote.dto.MovieDetailsDto
import pl.filmowo.data.remote.dto.SearchItemDto
import pl.filmowo.data.remote.dto.TvDetailsDto
import pl.filmowo.domain.model.EpisodeInfo
import pl.filmowo.domain.model.MediaFilter
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.MovieDetails
import pl.filmowo.domain.model.SearchPage
import pl.filmowo.domain.model.SearchResult
import pl.filmowo.domain.model.SeasonDetails
import pl.filmowo.domain.model.SeasonSummary
import pl.filmowo.domain.model.ShowDetails
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

class TmdbRepositoryImpl @Inject constructor(
    private val api: TmdbApi,
    @Named(NetworkModule.TMDB_API_KEY) private val apiKey: String,
) : TmdbRepository {

    override suspend fun search(query: String, filter: MediaFilter, page: Int): AppResult<SearchPage> =
        call {
            val response = when (filter) {
                MediaFilter.ALL -> api.searchMulti(query, page, LANGUAGE_PRIMARY)
                MediaFilter.MOVIES -> api.searchMovies(query, page, LANGUAGE_PRIMARY)
                MediaFilter.SHOWS -> api.searchTv(query, page, LANGUAGE_PRIMARY)
            }
            val defaultType = when (filter) {
                MediaFilter.MOVIES -> MediaType.MOVIE
                MediaFilter.SHOWS -> MediaType.TV
                MediaFilter.ALL -> null
            }
            SearchPage(
                items = response.results.mapNotNull { it.toDomain(defaultType) },
                page = response.page,
                totalPages = response.totalPages,
            )
        }

    override suspend fun movieDetails(id: Int): AppResult<MovieDetails> = call {
        val dto = api.movieDetails(id, LANGUAGE_PRIMARY)
        val overview = dto.overview.orEmpty().ifBlank {
            api.movieDetails(id, LANGUAGE_FALLBACK).overview.orEmpty()
        }
        dto.toDomain(overview)
    }

    override suspend fun showDetails(id: Int): AppResult<ShowDetails> = call {
        val dto = api.tvDetails(id, LANGUAGE_PRIMARY)
        val overview = dto.overview.orEmpty().ifBlank {
            api.tvDetails(id, LANGUAGE_FALLBACK).overview.orEmpty()
        }
        dto.toDomain(overview)
    }

    override suspend fun seasonDetails(showId: Int, seasonNumber: Int): AppResult<SeasonDetails> = call {
        val dto = api.seasonDetails(showId, seasonNumber, LANGUAGE_PRIMARY)
        SeasonDetails(
            seasonNumber = dto.seasonNumber,
            name = dto.name?.takeIf { it.isNotBlank() },
            airDate = dto.airDate?.takeIf { it.isNotBlank() },
            episodes = dto.episodes.map {
                EpisodeInfo(
                    seasonNumber = it.seasonNumber,
                    episodeNumber = it.episodeNumber,
                    name = it.name?.takeIf { n -> n.isNotBlank() },
                    overview = it.overview.orEmpty(),
                    airDate = it.airDate?.takeIf { d -> d.isNotBlank() },
                    runtimeMinutes = it.runtime?.takeIf { r -> r > 0 },
                )
            },
        )
    }

    override suspend fun trending(): AppResult<List<SearchResult>> = call {
        api.trending(LANGUAGE_PRIMARY).results.mapNotNull { it.toDomain(defaultType = null) }.distinctBy { it.mediaType to it.id }
    }

    override suspend fun recommendations(id: Int, type: MediaType): AppResult<List<SearchResult>> = call {
        val response = when (type) {
            MediaType.MOVIE -> api.movieRecommendations(id, LANGUAGE_PRIMARY)
            MediaType.TV -> api.tvRecommendations(id, LANGUAGE_PRIMARY)
        }
        response.results.mapNotNull { it.toDomain(defaultType = type) }.distinctBy { it.mediaType to it.id }
    }

    private suspend fun <T> call(block: suspend () -> T): AppResult<T> {
        if (apiKey.isBlank()) return AppResult.Failure(AppError.MISSING_API_KEY)
        return try {
            AppResult.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            AppResult.Failure(
                when (e.code()) {
                    401 -> AppError.UNAUTHORIZED
                    404 -> AppError.NOT_FOUND
                    429, in 500..599 -> AppError.SERVER
                    else -> AppError.UNKNOWN
                },
            )
        } catch (e: IOException) {
            AppResult.Failure(AppError.NETWORK)
        } catch (e: SerializationException) {
            AppResult.Failure(AppError.UNKNOWN)
        }
    }
}

internal fun String?.toYear(): Int? = this?.takeIf { it.length >= 4 }?.take(4)?.toIntOrNull()

/** Zwraca null dla osób (media_type = person) – wyszukiwarka pokazuje tylko filmy i seriale. */
private fun SearchItemDto.toDomain(defaultType: MediaType?): SearchResult? {
    val type = when (mediaType) {
        "movie" -> MediaType.MOVIE
        "tv" -> MediaType.TV
        null -> defaultType
        else -> null
    } ?: return null
    val title = (if (type == MediaType.MOVIE) title else name)?.takeIf { it.isNotBlank() } ?: return null
    return SearchResult(
        id = id,
        mediaType = type,
        title = title,
        year = (if (type == MediaType.MOVIE) releaseDate else firstAirDate).toYear(),
        posterPath = posterPath,
        overview = overview.orEmpty(),
    )
}

private fun MovieDetailsDto.toDomain(overview: String) = MovieDetails(
    id = id,
    title = title,
    originalTitle = originalTitle?.takeIf { it != title },
    overview = overview,
    posterPath = posterPath,
    year = releaseDate.toYear(),
    runtimeMinutes = runtime?.takeIf { it > 0 },
    genres = genres.map { it.name },
)

private fun TvDetailsDto.toDomain(overview: String) = ShowDetails(
    id = id,
    title = name,
    originalTitle = originalName?.takeIf { it != name },
    overview = overview,
    posterPath = posterPath,
    year = firstAirDate.toYear(),
    seasonCount = numberOfSeasons ?: seasons.count { it.seasonNumber > 0 },
    episodeCount = numberOfEpisodes ?: 0,
    episodeRuntimeMinutes = episodeRunTime.takeIf { it.isNotEmpty() }?.average()?.toInt(),
    genres = genres.map { it.name },
    seasons = seasons
        .sortedWith(compareBy { if (it.seasonNumber == 0) Int.MAX_VALUE else it.seasonNumber })
        .map {
            SeasonSummary(
                seasonNumber = it.seasonNumber,
                name = it.name?.takeIf { n -> n.isNotBlank() },
                episodeCount = it.episodeCount,
                year = it.airDate.toYear(),
            )
        },
)
