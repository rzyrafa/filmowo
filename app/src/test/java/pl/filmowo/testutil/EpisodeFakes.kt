package pl.filmowo.testutil

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import pl.filmowo.core.network.AppResult
import pl.filmowo.data.local.EpisodeDao
import pl.filmowo.data.local.EpisodeEntity
import pl.filmowo.data.local.EpisodeProgressRow
import pl.filmowo.data.local.SeasonEntity
import pl.filmowo.data.repository.EpisodeRepository
import pl.filmowo.domain.model.Episode
import pl.filmowo.domain.model.EpisodeInfo
import pl.filmowo.domain.model.SeasonDetails
import pl.filmowo.domain.model.ShowProgress
import pl.filmowo.domain.model.TrackedSeason

/** DAO odcinków w pamięci, odwzorowujące zapytania SQL z [EpisodeDao]. */
class FakeEpisodeDao : EpisodeDao {
    val seasons = MutableStateFlow<List<SeasonEntity>>(emptyList())
    val episodes = MutableStateFlow<List<EpisodeEntity>>(emptyList())

    private fun EpisodeEntity.toRow() = EpisodeProgressRow(showId, seasonNumber, episodeNumber, airDate, watchedAt)
    private fun EpisodeEntity.isAired(today: String) = airDate != null && airDate <= today

    override fun observeSeasons(showId: Int): Flow<List<SeasonEntity>> = seasons.map { list ->
        list.filter { it.showId == showId }.sortedWith(compareBy({ it.seasonNumber == 0 }, { it.seasonNumber }))
    }

    override fun observeEpisodes(showId: Int, seasonNumber: Int): Flow<List<EpisodeEntity>> = episodes.map { list ->
        list.filter { it.showId == showId && it.seasonNumber == seasonNumber }.sortedBy { it.episodeNumber }
    }

    override fun observeProgressRows(showId: Int): Flow<List<EpisodeProgressRow>> =
        episodes.map { list -> list.filter { it.showId == showId }.map { it.toRow() } }

    override fun observeAllProgressRows(): Flow<List<EpisodeProgressRow>> = episodes.map { list -> list.map { it.toRow() } }

    override suspend fun getProgressRows(showId: Int) = episodes.value.filter { it.showId == showId }.map { it.toRow() }

    override suspend fun getEpisodes(showId: Int) = episodes.value.filter { it.showId == showId }

    override suspend fun countEpisodes(showId: Int) = episodes.value.count { it.showId == showId }

    override suspend fun setWatched(showId: Int, seasonNumber: Int, episodeNumber: Int, watchedAt: Long?) = update {
        if (it.showId == showId && it.seasonNumber == seasonNumber && it.episodeNumber == episodeNumber) it.copy(watchedAt = watchedAt) else it
    }

    override suspend fun markSeasonWatched(showId: Int, seasonNumber: Int, today: String, now: Long) = update {
        if (it.showId == showId && it.seasonNumber == seasonNumber && it.watchedAt == null && it.isAired(today)) it.copy(watchedAt = now) else it
    }

    override suspend fun clearSeason(showId: Int, seasonNumber: Int) = update {
        if (it.showId == showId && it.seasonNumber == seasonNumber) it.copy(watchedAt = null) else it
    }

    override suspend fun markAllAiredWatched(showId: Int, today: String, now: Long) = update {
        if (it.showId == showId && it.seasonNumber > 0 && it.watchedAt == null && it.isAired(today)) it.copy(watchedAt = now) else it
    }

    override suspend fun upsertSeasons(seasons: List<SeasonEntity>) {
        this.seasons.update { current ->
            current.filterNot { old -> seasons.any { it.showId == old.showId && it.seasonNumber == old.seasonNumber } } + seasons
        }
    }

    override suspend fun upsertEpisodes(episodes: List<EpisodeEntity>) {
        this.episodes.update { current ->
            current.filterNot { old ->
                episodes.any { it.showId == old.showId && it.seasonNumber == old.seasonNumber && it.episodeNumber == old.episodeNumber }
            } + episodes
        }
    }

    override suspend fun deleteSeasons(showId: Int) = seasons.update { list -> list.filterNot { it.showId == showId } }

    override suspend fun deleteEpisodes(showId: Int) = episodes.update { list -> list.filterNot { it.showId == showId } }

    private fun update(transform: (EpisodeEntity) -> EpisodeEntity) = episodes.update { list -> list.map(transform) }
}

/** Repozytorium odcinków dla testów ViewModeli. */
class FakeEpisodeRepository : EpisodeRepository {
    val seasons = MutableStateFlow<Map<Int, List<TrackedSeason>>>(emptyMap())
    val episodes = MutableStateFlow<Map<Pair<Int, Int>, List<Episode>>>(emptyMap())
    val progress = MutableStateFlow<Map<Int, ShowProgress>>(emptyMap())
    val syncCalls = mutableListOf<Int>()
    val markShowWatchedCalls = mutableListOf<Int>()
    val episodeChanges = mutableListOf<Triple<Int, Int, Boolean>>()
    val seasonChanges = mutableListOf<Triple<Int, Int, Boolean>>()
    var syncResult: AppResult<Unit> = AppResult.Success(Unit)

    override fun observeSeasons(showId: Int) = seasons.map { it[showId].orEmpty() }
    override fun observeEpisodes(showId: Int, seasonNumber: Int) = episodes.map { it[showId to seasonNumber].orEmpty() }
    override fun observeProgress(showId: Int) = progress.map { it[showId] ?: ShowProgress(0, 0, null) }
    override fun observeAllProgress() = progress

    override suspend fun hasEpisodes(showId: Int) = episodes.value.keys.any { it.first == showId }

    override suspend fun sync(showId: Int): AppResult<Unit> {
        syncCalls += showId
        return syncResult
    }

    override suspend fun setEpisodeWatched(showId: Int, seasonNumber: Int, episodeNumber: Int, watched: Boolean) {
        episodeChanges += Triple(seasonNumber, episodeNumber, watched)
        episodes.update { map ->
            val key = showId to seasonNumber
            map + (key to map[key].orEmpty().map {
                if (it.info.episodeNumber == episodeNumber) it.copy(watchedAt = if (watched) 1L else null) else it
            })
        }
    }

    override suspend fun setSeasonWatched(showId: Int, seasonNumber: Int, watched: Boolean) {
        seasonChanges += Triple(showId, seasonNumber, watched)
    }

    override suspend fun markShowWatched(showId: Int): AppResult<Unit> {
        markShowWatchedCalls += showId
        return AppResult.Success(Unit)
    }
}

fun episodeEntity(
    season: Int,
    episode: Int,
    airDate: String? = "2020-01-01",
    watchedAt: Long? = null,
    showId: Int = 1,
) = EpisodeEntity(showId, season, episode, "Odcinek $episode", "", airDate, 45, watchedAt)

fun episodeInfo(season: Int, episode: Int, airDate: String? = "2020-01-01") =
    EpisodeInfo(season, episode, "Odcinek $episode", "", airDate, 45)

fun seasonDetails(season: Int, episodes: Int, airDate: String? = "2020-01-01") =
    SeasonDetails(season, "Sezon $season", airDate, (1..episodes).map { episodeInfo(season, it, airDate) })
