package pl.filmowo.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStatus

@Dao
interface LibraryDao {

    @Query("SELECT * FROM library_items")
    fun observeAll(): Flow<List<LibraryItemEntity>>

    @Query("SELECT * FROM library_items")
    suspend fun getAll(): List<LibraryItemEntity>

    @Query("SELECT * FROM library_items WHERE mediaType = :type")
    suspend fun getAllOfType(type: MediaType): List<LibraryItemEntity>

    @Upsert
    suspend fun upsertAll(items: List<LibraryItemEntity>)

    @Query("SELECT * FROM library_items WHERE tmdbId = :id AND mediaType = :type")
    fun observe(id: Int, type: MediaType): Flow<LibraryItemEntity?>

    @Query("SELECT * FROM library_items WHERE tmdbId = :id AND mediaType = :type")
    suspend fun get(id: Int, type: MediaType): LibraryItemEntity?

    @Upsert
    suspend fun upsert(item: LibraryItemEntity)

    @Query("DELETE FROM library_items WHERE tmdbId = :id AND mediaType = :type")
    suspend fun delete(id: Int, type: MediaType)

    @Query("UPDATE library_items SET rating = :rating, updatedAt = :now WHERE tmdbId = :id AND mediaType = :type")
    suspend fun setRating(id: Int, type: MediaType, rating: Int?, now: Long)

    @Query(
        "UPDATE library_items SET status = :status, watchedAt = :watchedAt, updatedAt = :now " +
            "WHERE tmdbId = :id AND mediaType = :type",
    )
    suspend fun updateStatus(id: Int, type: MediaType, status: WatchStatus, watchedAt: Long?, now: Long)

    @Query("UPDATE library_items SET note = :note, updatedAt = :now WHERE tmdbId = :id AND mediaType = :type")
    suspend fun setNote(id: Int, type: MediaType, note: String?, now: Long)

    @Query("UPDATE library_items SET rewatchCount = :count, updatedAt = :now WHERE tmdbId = :id AND mediaType = :type")
    suspend fun setRewatchCount(id: Int, type: MediaType, count: Int, now: Long)

    @Query("UPDATE library_items SET updatedAt = :now WHERE tmdbId = :id AND mediaType = :type")
    suspend fun touch(id: Int, type: MediaType, now: Long)

    @Query("UPDATE library_items SET localPosterFile = :file WHERE tmdbId = :id AND mediaType = :type")
    suspend fun setPosterFile(id: Int, type: MediaType, file: String?)
}
