package pl.filmowo.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pl.filmowo.core.TimeProvider
import pl.filmowo.core.di.ApplicationScope
import pl.filmowo.data.local.EpisodeDao
import pl.filmowo.data.local.LibraryDao
import pl.filmowo.data.local.LibraryItemEntity
import pl.filmowo.data.local.PosterStorage
import pl.filmowo.data.local.toDomain
import pl.filmowo.data.local.withMetadata
import pl.filmowo.domain.model.LibraryItem
import pl.filmowo.domain.model.LibraryMetadata
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStatus
import javax.inject.Inject

class LibraryRepositoryImpl @Inject constructor(
    private val dao: LibraryDao,
    private val episodeDao: EpisodeDao,
    private val posters: PosterStorage,
    private val time: TimeProvider,
    @ApplicationScope private val appScope: CoroutineScope,
) : LibraryRepository {

    override fun observeLibrary(): Flow<List<LibraryItem>> =
        dao.observeAll().map { items -> items.map { it.toDomain() } }

    override fun observeItem(id: Int, type: MediaType): Flow<LibraryItem?> =
        dao.observe(id, type).map { it?.toDomain() }

    override suspend fun setStatus(metadata: LibraryMetadata, status: WatchStatus) {
        val now = time.now()
        val existing = dao.get(metadata.tmdbId, metadata.mediaType)
        val watchedAt = when {
            status != WatchStatus.WATCHED -> null
            existing?.status == WatchStatus.WATCHED -> existing.watchedAt
            else -> now
        }
        val entity = existing?.withMetadata(metadata)?.copy(status = status, updatedAt = now, watchedAt = watchedAt)
            ?: LibraryItemEntity(
                tmdbId = metadata.tmdbId,
                mediaType = metadata.mediaType,
                title = metadata.title,
                originalTitle = metadata.originalTitle,
                overview = metadata.overview,
                year = metadata.year,
                posterPath = metadata.posterPath,
                localPosterFile = null,
                runtimeMinutes = metadata.runtimeMinutes,
                genres = metadata.genres,
                seasonCount = metadata.seasonCount,
                episodeCount = metadata.episodeCount,
                status = status,
                rating = null,
                addedAt = now,
                updatedAt = now,
                watchedAt = watchedAt,
            )
        dao.upsert(entity)
        if (needsPosterDownload(existing, metadata)) schedulePosterDownload(metadata)
    }

    override suspend fun refreshMetadata(metadata: LibraryMetadata) {
        val existing = dao.get(metadata.tmdbId, metadata.mediaType) ?: return
        val updated = existing.withMetadata(metadata)
        if (updated != existing) dao.upsert(updated)
        if (needsPosterDownload(existing, metadata)) schedulePosterDownload(metadata)
    }

    override suspend fun setRating(id: Int, type: MediaType, rating: Int?) {
        require(rating == null || rating in 1..10) { "Ocena musi być w zakresie 1–10" }
        dao.setRating(id, type, rating, time.now())
    }

    override suspend fun remove(id: Int, type: MediaType) {
        val existing = dao.get(id, type) ?: return
        dao.delete(id, type)
        if (type == MediaType.TV) episodeDao.deleteShow(id)
        existing.localPosterFile?.let { posters.delete(it) }
    }

    private fun needsPosterDownload(existing: LibraryItemEntity?, metadata: LibraryMetadata): Boolean =
        metadata.posterPath != null &&
            (existing?.localPosterFile == null || existing.posterPath != metadata.posterPath)

    /** Pobieranie w zakresie aplikacji, żeby nie przerwało go zamknięcie ekranu. */
    private fun schedulePosterDownload(metadata: LibraryMetadata) {
        val posterPath = metadata.posterPath ?: return
        val id = metadata.tmdbId
        val type = metadata.mediaType
        appScope.launch {
            val file = posters.download(posterPath, "${type.name.lowercase()}_$id.jpg") ?: return@launch
            // Pozycję mogły w międzyczasie usunąć z biblioteki.
            if (dao.get(id, type) != null) dao.setPosterFile(id, type, file) else posters.delete(file)
        }
    }
}
