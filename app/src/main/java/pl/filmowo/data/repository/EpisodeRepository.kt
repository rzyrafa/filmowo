package pl.filmowo.data.repository

import kotlinx.coroutines.flow.Flow
import pl.filmowo.core.network.AppResult
import pl.filmowo.domain.model.Episode
import pl.filmowo.domain.model.ShowProgress
import pl.filmowo.domain.model.TrackedSeason

/**
 * Śledzenie odcinków seriali z biblioteki. Sezony i odcinki są zapisywane lokalnie,
 * a status serialu (Oglądam / Obejrzane) wynika z obejrzanych odcinków.
 */
interface EpisodeRepository {
    fun observeSeasons(showId: Int): Flow<List<TrackedSeason>>
    fun observeEpisodes(showId: Int, seasonNumber: Int): Flow<List<Episode>>
    fun observeProgress(showId: Int): Flow<ShowProgress>

    /** Postęp wszystkich seriali, które mają zapisane odcinki (klucz: ID serialu). */
    fun observeAllProgress(): Flow<Map<Int, ShowProgress>>

    suspend fun hasEpisodes(showId: Int): Boolean

    /** Pobiera z TMDB wszystkie sezony i odcinki serialu z biblioteki, zachowując obejrzane. */
    suspend fun sync(showId: Int): AppResult<Unit>

    suspend fun setEpisodeWatched(showId: Int, seasonNumber: Int, episodeNumber: Int, watched: Boolean)
    suspend fun setSeasonWatched(showId: Int, seasonNumber: Int, watched: Boolean)

    /** „Obejrzane” na serialu – odhacza wszystkie wyemitowane odcinki (w razie potrzeby najpierw je pobiera). */
    suspend fun markShowWatched(showId: Int): AppResult<Unit>
}
