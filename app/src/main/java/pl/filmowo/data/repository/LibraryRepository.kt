package pl.filmowo.data.repository

import kotlinx.coroutines.flow.Flow
import pl.filmowo.domain.model.LibraryItem
import pl.filmowo.domain.model.LibraryMetadata
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStatus

/** Biblioteka użytkownika – wyłącznie lokalnie w Room. */
interface LibraryRepository {
    fun observeLibrary(): Flow<List<LibraryItem>>
    fun observeItem(id: Int, type: MediaType): Flow<LibraryItem?>

    /** Dodaje pozycję do biblioteki albo zmienia jej status (z aktualizacją danych z TMDB). */
    suspend fun setStatus(metadata: LibraryMetadata, status: WatchStatus)

    /** Aktualizuje dane z TMDB, jeśli pozycja jest w bibliotece; inaczej nic nie robi. */
    suspend fun refreshMetadata(metadata: LibraryMetadata)

    /** Ocena 1–10 albo null, żeby ją usunąć. */
    suspend fun setRating(id: Int, type: MediaType, rating: Int?)

    suspend fun remove(id: Int, type: MediaType)
}
