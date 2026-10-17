package pl.filmowo.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import pl.filmowo.core.TimeProvider
import pl.filmowo.core.di.IoDispatcher
import pl.filmowo.data.local.FilmowoDatabase
import pl.filmowo.data.repository.LibraryRepository
import java.io.IOException
import javax.inject.Inject

/** Wynik importu – liczba przywróconych pozycji albo rodzaj błędu. */
sealed interface ImportResult {
    data class Success(val items: Int) : ImportResult
    data object InvalidFile : ImportResult
    data object ReadError : ImportResult
}

/** Eksport i import całej biblioteki (z odcinkami) do pliku JSON wybranego przez użytkownika. */
interface BackupRepository {
    /** Zwraca liczbę zapisanych pozycji albo null, gdy zapis się nie udał. */
    suspend fun export(uri: Uri): Int?
    suspend fun import(uri: Uri): ImportResult
}

class JsonBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: FilmowoDatabase,
    private val libraryRepository: LibraryRepository,
    private val time: TimeProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BackupRepository {

    override suspend fun export(uri: Uri): Int? = withContext(ioDispatcher) {
        val backup = createBackup()
        try {
            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: return@withContext null
            stream.use { it.write(encode(backup).toByteArray()) }
            backup.items.size
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    override suspend fun import(uri: Uri): ImportResult = withContext(ioDispatcher) {
        val text = try {
            val stream = context.contentResolver.openInputStream(uri) ?: return@withContext ImportResult.ReadError
            stream.use { it.readBytes().decodeToString() }
        } catch (e: IOException) {
            return@withContext ImportResult.ReadError
        } catch (e: SecurityException) {
            return@withContext ImportResult.ReadError
        }
        val result = restore(text)
        if (result is ImportResult.Success) libraryRepository.downloadMissingPosters()
        result
    }

    internal suspend fun createBackup(): BackupFile = database.withTransaction {
        BackupFile(
            version = BackupFile.CURRENT_VERSION,
            exportedAt = time.now(),
            items = database.libraryDao().getAll().map { it.toBackup() },
            seasons = database.episodeDao().getAllSeasons().map { it.toBackup() },
            episodes = database.episodeDao().getAllEpisodes().map { it.toBackup() },
        )
    }

    /**
     * Pozycje z pliku zastępują te same pozycje w bibliotece (pozostałe zostają).
     * Seriale z pliku dostają dokładnie te sezony i odcinki, które zapisano w kopii.
     */
    internal suspend fun restore(text: String): ImportResult {
        val backup = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: SerializationException) {
            return ImportResult.InvalidFile
        } catch (e: IllegalArgumentException) {
            return ImportResult.InvalidFile
        }
        if (backup.version > BackupFile.CURRENT_VERSION) return ImportResult.InvalidFile

        val libraryDao = database.libraryDao()
        val episodeDao = database.episodeDao()
        database.withTransaction {
            val existing = libraryDao.getAll().associateBy { it.tmdbId to it.mediaType }
            libraryDao.upsertAll(
                backup.items.map { item ->
                    // Zachowaj lokalny plakat, jeśli pozycja już jest na urządzeniu.
                    item.toEntity(localPosterFile = existing[item.tmdbId to item.mediaType]?.localPosterFile)
                },
            )
            val seasonsByShow = backup.seasons.groupBy { it.showId }
            val episodesByShow = backup.episodes.groupBy { it.showId }
            (seasonsByShow.keys + episodesByShow.keys).forEach { showId ->
                episodeDao.replaceShow(
                    showId = showId,
                    seasons = seasonsByShow[showId].orEmpty().map { it.toEntity() },
                    episodes = episodesByShow[showId].orEmpty().map { it.toEntity() },
                )
            }
        }
        return ImportResult.Success(backup.items.size)
    }

    internal fun encode(backup: BackupFile): String = json.encodeToString(BackupFile.serializer(), backup)

    private companion object {
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
