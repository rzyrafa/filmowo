package pl.filmowo.data.remote

import pl.filmowo.data.remote.dto.MovieDetailsDto
import pl.filmowo.data.remote.dto.SearchResponseDto
import pl.filmowo.data.remote.dto.SeasonDetailsDto
import pl.filmowo.data.remote.dto.TvDetailsDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Endpointy TMDB API v3. Klucz API dokleja [TmdbApiKeyInterceptor]. */
interface TmdbApi {

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("language") language: String,
        @Query("include_adult") includeAdult: Boolean = false,
    ): SearchResponseDto

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("language") language: String,
        @Query("include_adult") includeAdult: Boolean = false,
    ): SearchResponseDto

    @GET("search/tv")
    suspend fun searchTv(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("language") language: String,
        @Query("include_adult") includeAdult: Boolean = false,
    ): SearchResponseDto

    @GET("movie/{id}")
    suspend fun movieDetails(
        @Path("id") id: Int,
        @Query("language") language: String,
    ): MovieDetailsDto

    @GET("tv/{id}")
    suspend fun tvDetails(
        @Path("id") id: Int,
        @Query("language") language: String,
    ): TvDetailsDto

    @GET("tv/{id}/season/{season}")
    suspend fun seasonDetails(
        @Path("id") showId: Int,
        @Path("season") seasonNumber: Int,
        @Query("language") language: String,
    ): SeasonDetailsDto

    @GET("trending/all/week")
    suspend fun trending(
        @Query("language") language: String,
        @Query("page") page: Int = 1,
    ): SearchResponseDto

    @GET("movie/{id}/recommendations")
    suspend fun movieRecommendations(
        @Path("id") id: Int,
        @Query("language") language: String,
    ): SearchResponseDto

    @GET("tv/{id}/recommendations")
    suspend fun tvRecommendations(
        @Path("id") id: Int,
        @Query("language") language: String,
    ): SearchResponseDto

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val LANGUAGE_PRIMARY = "pl-PL"
        const val LANGUAGE_FALLBACK = "en-US"
    }
}
