package pl.filmowo.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.filmowo.core.TimeProvider
import pl.filmowo.core.di.ApplicationScope
import pl.filmowo.core.network.AppResult
import pl.filmowo.core.today
import pl.filmowo.data.local.EpisodeDao
import pl.filmowo.data.local.EpisodeEntity
import pl.filmowo.data.local.LibraryDao
import pl.filmowo.data.local.SeasonEntity
import pl.filmowo.data.local.toDomain
import pl.filmowo.data.local.toProgressInput
import pl.filmowo.data.local.withMetadata
import pl.filmowo.domain.model.Episode
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.SeasonDetails
import pl.filmowo.domain.model.ShowDetails
import pl.filmowo.domain.model.ShowProgress
import pl.filmowo.domain.model.TrackedSeason
import pl.filmowo.domain.model.WatchStatus
import pl.filmowo.domain.model.computeSeasonProgress
import pl.filmowo.domain.model.computeShowProgress
import pl.filmowo.domain.model.toLibraryMetadata
import javax.inject.Inject

class EpisodeRepositoryImpl @Inject constructor(
    private val tmdb: TmdbRepository,
    private val libraryDao: LibraryDao,
    private val episodeDao: EpisodeDao,
    private val time: TimeProvider,
    @ApplicationScope private val appScope: CoroutineScope,
) : EpisodeRepository {

    /**
     * Chroni zapisy odcinków: synchronizacja zastępuje odcinki serialu, więc odhaczenie
     * w trakcie jej zapisu mogłoby zostać nadpisane. Pobieranie z sieci odbywa się poza blokadą.
     */
    private val writeMutex = Mutex()

    override fun observeSeasons(showId: Int): Flow<List<TrackedSeason>> =
        combine(episodeDao.observeSeasons(showId), episodeDao.observeProgressRows(showId)) { seasons, rows ->
            val today = time.today()
            val bySeason = rows.groupBy { it.seasonNumber }
            seasons.map { season ->
                TrackedSeason(
                    seasonNumber = season.seasonNumber,
                    name = season.name,
                    year = season.airDate?.take(4)?.toIntOrNull(),
                    progress = computeSeasonProgress(
                        bySeason[season.seasonNumber].orEmpty().map { it.toProgressInput() },
                        today,
                    ),
                )
            }
        }

    override fun observeEpisodes(showId: Int, seasonNumber: Int): Flow<List<Episode>> =
        episodeDao.observeEpisodes(showId, seasonNumber).map { list -> list.map { it.toDomain() } }

    override fun observeProgress(showId: Int): Flow<ShowProgress> =
        episodeDao.observeProgressRows(showId).map { rows ->
            computeShowProgress(rows.map { it.toProgressInput() }, time.today())
        }

    override fun observeAllProgress(): Flow<Map<Int, ShowProgress>> =
        episodeDao.observeAllProgressRows().map { rows ->
            val today = time.today()
            rows.groupBy { it.showId }.mapValues { (_, episodes) ->
                computeShowProgress(episodes.map { it.toProgressInput() }, today)
            }
        }

    override suspend fun hasEpisodes(showId: Int): Boolean = episodeDao.countEpisodes(showId) > 0

    // Praca w zakresie aplikacji: opuszczenie ekranu nie przerywa synchronizacji w połowie.
    override suspend fun sync(showId: Int): AppResult<Unit> =
        appScope.async { doSync(showId) }.await()

    override suspend fun setEpisodeWatched(showId: Int, seasonNumber: Int, episodeNumber: Int, watched: Boolean) =
        writeMutex.withLock {
            episodeDao.setWatched(showId, seasonNumber, episodeNumber, if (watched) time.now() else null)
            reconcileStatus(showId, afterSync = false)
        }

    override suspend fun setSeasonWatched(showId: Int, seasonNumber: Int, watched: Boolean) =
        writeMutex.withLock {
            if (watched) {
                episodeDao.markSeasonWatched(showId, seasonNumber, time.today(), time.now())
            } else {
                episodeDao.clearSeason(showId, seasonNumber)
            }
            reconcileStatus(showId, afterSync = false)
        }

    override suspend fun markShowWatched(showId: Int): AppResult<Unit> =
        appScope.async {
            if (episodeDao.countEpisodes(showId) == 0) {
                // Synchronizacja sama odhaczy odcinki serialu oznaczonego jako obejrzany.
                doSync(showId)
            } else {
                writeMutex.withLock {
                    episodeDao.markAllAiredWatched(showId, time.today(), time.now())
                    reconcileStatus(showId, afterSync = false)
                }
                AppResult.Success(Unit)
            }
        }.await()

    private suspend fun doSync(showId: Int): AppResult<Unit> {
        val show = when (val result = tmdb.showDetails(showId)) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
        val seasons = coroutineScope {
            show.seasons.map { async { tmdb.seasonDetails(showId, it.seasonNumber) } }.awaitAll()
        }
        seasons.filterIsInstance<AppResult.Failure>().firstOrNull()?.let { return it }

        writeMutex.withLock { saveSync(show, seasons.map { (it as AppResult.Success<SeasonDetails>).data }) }
        return AppResult.Success(Unit)
    }

    private suspend fun saveSync(show: ShowDetails, details: List<SeasonDetails>) {
        val showId = show.id
        // Serial mógł zostać usunięty z biblioteki w trakcie pobierania.
        val item = libraryDao.get(showId, MediaType.TV) ?: return
        libraryDao.upsert(item.withMetadata(show.toLibraryMetadata()))

        val watched = episodeDao.getEpisodes(showId)
            .filter { it.watchedAt != null }
            .associate { (it.seasonNumber to it.episodeNumber) to it.watchedAt }
        episodeDao.replaceShow(
            showId = showId,
            seasons = details.map { it.toEntity(showId) },
            episodes = details.flatMap { season ->
                season.episodes.map { episode ->
                    EpisodeEntity(
                        showId = showId,
                        seasonNumber = episode.seasonNumber,
                        episodeNumber = episode.episodeNumber,
                        name = episode.name,
                        overview = episode.overview,
                        airDate = episode.airDate,
                        runtimeMinutes = episode.runtimeMinutes ?: show.episodeRuntimeMinutes,
                        watchedAt = watched[episode.seasonNumber to episode.episodeNumber],
                    )
                }
            },
        )
        reconcileStatus(showId, afterSync = true)
    }

    /**
     * Ustala status serialu na podstawie odcinków:
     * - wszystkie wyemitowane obejrzane → Obejrzane,
     * - część obejrzana → Oglądam,
     * - nic nie obejrzane po odznaczeniu → Do obejrzenia.
     *
     * Po synchronizacji serial oznaczony wcześniej jako obejrzany (np. offline, bez odcinków)
     * dostaje odhaczone wszystkie wyemitowane odcinki, a gdy nic nie obejrzano – status zostaje.
     */
    private suspend fun reconcileStatus(showId: Int, afterSync: Boolean) {
        val item = libraryDao.get(showId, MediaType.TV) ?: return
        val now = time.now()
        val today = time.today()
        var progress = computeShowProgress(episodeDao.getProgressRows(showId).map { it.toProgressInput() }, today)

        if (afterSync && item.status == WatchStatus.WATCHED && progress.watchedCount == 0 && progress.airedCount > 0) {
            episodeDao.markAllAiredWatched(showId, today, now)
            progress = computeShowProgress(episodeDao.getProgressRows(showId).map { it.toProgressInput() }, today)
        }

        val status = when {
            progress.isComplete -> WatchStatus.WATCHED
            progress.watchedCount > 0 -> WatchStatus.WATCHING
            afterSync -> item.status
            else -> WatchStatus.PLANNED
        }
        when {
            status != item.status -> {
                val watchedAt = if (status == WatchStatus.WATCHED) now else null
                libraryDao.updateStatus(showId, MediaType.TV, status, watchedAt, now)
            }
            // Zmiana odcinków to aktywność – serial trafia na górę sortowania „ostatnio zmienione”.
            !afterSync -> libraryDao.touch(showId, MediaType.TV, now)
        }
    }

    private fun SeasonDetails.toEntity(showId: Int) = SeasonEntity(
        showId = showId,
        seasonNumber = seasonNumber,
        name = name,
        airDate = airDate,
        episodeCount = episodes.size,
    )
}
