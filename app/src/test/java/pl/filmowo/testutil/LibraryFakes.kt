package pl.filmowo.testutil

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import pl.filmowo.data.local.LibraryDao
import pl.filmowo.data.local.LibraryItemEntity
import pl.filmowo.data.local.PosterStorage
import pl.filmowo.data.repository.LibraryRepository
import pl.filmowo.domain.model.LibraryItem
import pl.filmowo.domain.model.LibraryMetadata
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStatus

/** DAO w pamięci – zachowuje się jak Room dla zapytań używanych w aplikacji. */
class FakeLibraryDao : LibraryDao {
    val items = MutableStateFlow<Map<Pair<Int, MediaType>, LibraryItemEntity>>(emptyMap())

    override fun observeAll(): Flow<List<LibraryItemEntity>> = items.map { it.values.toList() }

    override fun observe(id: Int, type: MediaType): Flow<LibraryItemEntity?> = items.map { it[id to type] }

    override suspend fun get(id: Int, type: MediaType): LibraryItemEntity? = items.value[id to type]

    override suspend fun upsert(item: LibraryItemEntity) {
        items.update { it + ((item.tmdbId to item.mediaType) to item) }
    }

    override suspend fun delete(id: Int, type: MediaType) {
        items.update { it - (id to type) }
    }

    override suspend fun setRating(id: Int, type: MediaType, rating: Int?, now: Long) {
        items.update { map -> map[id to type]?.let { map + ((id to type) to it.copy(rating = rating, updatedAt = now)) } ?: map }
    }

    override suspend fun setPosterFile(id: Int, type: MediaType, file: String?) {
        items.update { map -> map[id to type]?.let { map + ((id to type) to it.copy(localPosterFile = file)) } ?: map }
    }
}

class FakePosterStorage : PosterStorage {
    var result: String? = "/files/posters/poster.jpg"
    val downloads = mutableListOf<String>()
    val deleted = mutableListOf<String>()

    override suspend fun download(posterPath: String, fileName: String): String? {
        downloads += posterPath
        return result
    }

    override suspend fun delete(file: String) {
        deleted += file
    }
}

/** Uproszczone repozytorium dla testów ViewModeli. */
class FakeLibraryRepository : LibraryRepository {
    val items = MutableStateFlow<Map<Pair<Int, MediaType>, LibraryItem>>(emptyMap())
    val refreshed = mutableListOf<LibraryMetadata>()
    private var clock = 0L

    override fun observeLibrary(): Flow<List<LibraryItem>> = items.map { it.values.toList() }

    override fun observeItem(id: Int, type: MediaType): Flow<LibraryItem?> = items.map { it[id to type] }

    override suspend fun setStatus(metadata: LibraryMetadata, status: WatchStatus) {
        val key = metadata.tmdbId to metadata.mediaType
        val now = ++clock
        items.update {
            val existing = it[key]
            it + (key to (existing?.copy(metadata = metadata, status = status, updatedAt = now) ?: libraryItem(metadata, status, now)))
        }
    }

    override suspend fun refreshMetadata(metadata: LibraryMetadata) {
        refreshed += metadata
    }

    override suspend fun setRating(id: Int, type: MediaType, rating: Int?) {
        items.update { map -> map[id to type]?.let { map + ((id to type) to it.copy(rating = rating)) } ?: map }
    }

    override suspend fun remove(id: Int, type: MediaType) {
        items.update { it - (id to type) }
    }

    fun put(item: LibraryItem) {
        items.update { it + ((item.tmdbId to item.mediaType) to item) }
    }
}

fun metadata(
    id: Int = 1,
    type: MediaType = MediaType.MOVIE,
    title: String = "Tytuł $id",
    year: Int? = 2020,
    posterPath: String? = "/p$id.jpg",
) = LibraryMetadata(id, type, title, null, "Opis", year, posterPath, 120, listOf("Dramat"), null, null)

fun libraryItem(
    metadata: LibraryMetadata,
    status: WatchStatus,
    updatedAt: Long = 0,
    rating: Int? = null,
) = LibraryItem(metadata, null, status, rating, addedAt = updatedAt, updatedAt = updatedAt, watchedAt = null)
