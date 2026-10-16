package pl.filmowo.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pl.filmowo.data.local.EpisodeDao
import pl.filmowo.data.local.LibraryDao
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStats
import pl.filmowo.domain.model.WatchStatus
import javax.inject.Inject

interface StatsRepository {
    fun observeStats(): Flow<WatchStats>
}

/**
 * Statystyki liczone z lokalnej bazy: obejrzane filmy (status „Obejrzane”)
 * i wszystkie odhaczone odcinki, łącznie z odcinkami specjalnymi.
 */
class StatsRepositoryImpl @Inject constructor(
    private val libraryDao: LibraryDao,
    private val episodeDao: EpisodeDao,
) : StatsRepository {

    override fun observeStats(): Flow<WatchStats> = combine(
        libraryDao.observeTotals(MediaType.MOVIE, WatchStatus.WATCHED),
        episodeDao.observeWatchedTotals(),
    ) { movies, episodes ->
        WatchStats(
            moviesWatched = movies.count,
            movieMinutes = movies.minutes,
            episodesWatched = episodes.count,
            episodeMinutes = episodes.minutes,
        )
    }
}
