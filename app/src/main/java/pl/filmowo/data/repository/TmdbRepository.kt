package pl.filmowo.data.repository

import pl.filmowo.core.network.AppResult
import pl.filmowo.domain.model.MediaFilter
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.MovieDetails
import pl.filmowo.domain.model.SearchPage
import pl.filmowo.domain.model.SearchResult
import pl.filmowo.domain.model.SeasonDetails
import pl.filmowo.domain.model.ShowDetails

/** Dane pobierane z TMDB (wymaga internetu). */
interface TmdbRepository {
    suspend fun search(query: String, filter: MediaFilter, page: Int): AppResult<SearchPage>
    suspend fun movieDetails(id: Int): AppResult<MovieDetails>
    suspend fun showDetails(id: Int): AppResult<ShowDetails>
    suspend fun seasonDetails(showId: Int, seasonNumber: Int): AppResult<SeasonDetails>

    /** Popularne w tym tygodniu filmy i seriale. */
    suspend fun trending(): AppResult<List<SearchResult>>

    /** Tytuły polecane na podstawie danego filmu lub serialu. */
    suspend fun recommendations(id: Int, type: MediaType): AppResult<List<SearchResult>>
}
