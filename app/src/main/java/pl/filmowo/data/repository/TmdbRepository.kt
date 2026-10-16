package pl.filmowo.data.repository

import pl.filmowo.core.network.AppResult
import pl.filmowo.domain.model.MovieDetails
import pl.filmowo.domain.model.MediaFilter
import pl.filmowo.domain.model.SearchPage
import pl.filmowo.domain.model.ShowDetails

/** Dane pobierane z TMDB (wymaga internetu). */
interface TmdbRepository {
    suspend fun search(query: String, filter: MediaFilter, page: Int): AppResult<SearchPage>
    suspend fun movieDetails(id: Int): AppResult<MovieDetails>
    suspend fun showDetails(id: Int): AppResult<ShowDetails>
}
