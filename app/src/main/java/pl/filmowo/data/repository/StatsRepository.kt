package pl.filmowo.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pl.filmowo.core.TimeProvider
import pl.filmowo.data.local.EpisodeDao
import pl.filmowo.data.local.LibraryDao
import pl.filmowo.data.local.toDomain
import pl.filmowo.domain.model.WatchStats
import pl.filmowo.domain.model.WatchedEpisode
import pl.filmowo.domain.model.computeWatchStats
import javax.inject.Inject

interface StatsRepository {
    fun observeStats(): Flow<WatchStats>
}

/**
 * Statystyki liczone z lokalnej bazy: obejrzane filmy (status „Obejrzane”, z powtórkami)
 * i wszystkie odhaczone odcinki, łącznie z odcinkami specjalnymi.
 */
class StatsRepositoryImpl @Inject constructor(
    private val libraryDao: LibraryDao,
    private val episodeDao: EpisodeDao,
    private val time: TimeProvider,
) : StatsRepository {

    override fun observeStats(): Flow<WatchStats> = combine(
        libraryDao.observeAll(),
        episodeDao.observeWatchedEpisodes(),
    ) { items, episodes ->
        computeWatchStats(
            library = items.map { it.toDomain() },
            episodes = episodes.map { WatchedEpisode(it.watchedAt, it.runtimeMinutes) },
            now = time.now(),
        )
    }
}
