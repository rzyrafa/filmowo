package pl.filmowo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    /** Sezony w kolejności numerów, odcinki specjalne (sezon 0) na końcu. */
    @Query(
        "SELECT * FROM seasons WHERE showId = :showId " +
            "ORDER BY CASE WHEN seasonNumber = 0 THEN 1 ELSE 0 END, seasonNumber",
    )
    fun observeSeasons(showId: Int): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM episodes WHERE showId = :showId AND seasonNumber = :seasonNumber ORDER BY episodeNumber")
    fun observeEpisodes(showId: Int, seasonNumber: Int): Flow<List<EpisodeEntity>>

    @Query("SELECT showId, seasonNumber, episodeNumber, airDate, watchedAt FROM episodes WHERE showId = :showId")
    fun observeProgressRows(showId: Int): Flow<List<EpisodeProgressRow>>

    @Query("SELECT showId, seasonNumber, episodeNumber, airDate, watchedAt FROM episodes")
    fun observeAllProgressRows(): Flow<List<EpisodeProgressRow>>

    @Query("SELECT showId, seasonNumber, episodeNumber, airDate, watchedAt FROM episodes WHERE showId = :showId")
    suspend fun getProgressRows(showId: Int): List<EpisodeProgressRow>

    @Query("SELECT * FROM episodes WHERE showId = :showId")
    suspend fun getEpisodes(showId: Int): List<EpisodeEntity>

    @Query("SELECT COUNT(*) FROM episodes WHERE showId = :showId")
    suspend fun countEpisodes(showId: Int): Int

    @Query(
        "UPDATE episodes SET watchedAt = :watchedAt " +
            "WHERE showId = :showId AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber",
    )
    suspend fun setWatched(showId: Int, seasonNumber: Int, episodeNumber: Int, watchedAt: Long?)

    /** Oznacza wyemitowane odcinki sezonu; już obejrzane zachowują swoją datę. */
    @Query(
        "UPDATE episodes SET watchedAt = :now WHERE showId = :showId AND seasonNumber = :seasonNumber " +
            "AND watchedAt IS NULL AND airDate IS NOT NULL AND airDate <= :today",
    )
    suspend fun markSeasonWatched(showId: Int, seasonNumber: Int, today: String, now: Long)

    @Query("UPDATE episodes SET watchedAt = NULL WHERE showId = :showId AND seasonNumber = :seasonNumber")
    suspend fun clearSeason(showId: Int, seasonNumber: Int)

    /** Oznacza wszystkie wyemitowane odcinki zwykłych sezonów (bez odcinków specjalnych). */
    @Query(
        "UPDATE episodes SET watchedAt = :now WHERE showId = :showId AND seasonNumber > 0 " +
            "AND watchedAt IS NULL AND airDate IS NOT NULL AND airDate <= :today",
    )
    suspend fun markAllAiredWatched(showId: Int, today: String, now: Long)

    @Upsert
    suspend fun upsertSeasons(seasons: List<SeasonEntity>)

    @Upsert
    suspend fun upsertEpisodes(episodes: List<EpisodeEntity>)

    @Query("DELETE FROM seasons WHERE showId = :showId")
    suspend fun deleteSeasons(showId: Int)

    @Query("DELETE FROM episodes WHERE showId = :showId")
    suspend fun deleteEpisodes(showId: Int)

    /** Zastępuje sezony i odcinki serialu nowymi danymi z TMDB. */
    @Transaction
    suspend fun replaceShow(showId: Int, seasons: List<SeasonEntity>, episodes: List<EpisodeEntity>) {
        deleteEpisodes(showId)
        deleteSeasons(showId)
        upsertSeasons(seasons)
        upsertEpisodes(episodes)
    }

    @Transaction
    suspend fun deleteShow(showId: Int) {
        deleteEpisodes(showId)
        deleteSeasons(showId)
    }
}
