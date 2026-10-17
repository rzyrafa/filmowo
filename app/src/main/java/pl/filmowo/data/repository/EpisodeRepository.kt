package pl.filmowo.data.repository

import kotlinx.coroutines.flow.Flow
import pl.filmowo.core.network.AppResult
import pl.filmowo.domain.model.Episode
import pl.filmowo.domain.model.ShowProgress
import pl.filmowo.domain.model.TrackedSeason
import pl.filmowo.domain.model.UpcomingEpisode

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

    /** Odcinki seriali z biblioteki z premierą dziś lub później. */
    fun observeUpcoming(): Flow<List<UpcomingEpisode>>

    /** Odcinki seriali z biblioteki z premierą w dniu [date] (ISO). */
    suspend fun getAiringOn(date: String): List<UpcomingEpisode>

    /**
     * Odświeża odcinki wszystkich seriali z biblioteki (praca w tle).
     * Zwraca pierwszy błąd, jeśli któryś serial się nie zsynchronizował.
     */
    suspend fun syncAll(): AppResult<Unit>

    /** Pobiera z TMDB wszystkie sezony i odcinki serialu z biblioteki, zachowując obejrzane. */
    suspend fun sync(showId: Int): AppResult<Unit>

    suspend fun setEpisodeWatched(showId: Int, seasonNumber: Int, episodeNumber: Int, watched: Boolean)
    suspend fun setSeasonWatched(showId: Int, seasonNumber: Int, watched: Boolean)

    /** „Obejrzane” na serialu – odhacza wszystkie wyemitowane odcinki (w razie potrzeby najpierw je pobiera). */
    suspend fun markShowWatched(showId: Int): AppResult<Unit>

    /** Odznacza wszystkie odcinki serialu – serial wraca do „Do obejrzenia”. */
    suspend fun resetProgress(showId: Int)
}
