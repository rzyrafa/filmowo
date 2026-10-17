package pl.filmowo.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import pl.filmowo.domain.model.LibraryItem
import pl.filmowo.domain.model.LibraryMetadata
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStatus

/** Pozycja biblioteki. Klucz złożony, bo TMDB może nadać to samo ID filmowi i serialowi. */
@Entity(tableName = "library_items", primaryKeys = ["tmdbId", "mediaType"])
data class LibraryItemEntity(
    val tmdbId: Int,
    val mediaType: MediaType,
    val title: String,
    val originalTitle: String?,
    val overview: String,
    val year: Int?,
    val posterPath: String?,
    val localPosterFile: String?,
    val runtimeMinutes: Int?,
    val genres: List<String>,
    val seasonCount: Int?,
    val episodeCount: Int?,
    val status: WatchStatus,
    val rating: Int?,
    val addedAt: Long,
    val updatedAt: Long,
    val watchedAt: Long?,
    /** Własna notatka użytkownika (v3). */
    val note: String? = null,
    /** Ile razy film obejrzano ponownie, poza pierwszym obejrzeniem (v3). */
    @ColumnInfo(defaultValue = "0") val rewatchCount: Int = 0,
)

fun LibraryItemEntity.toDomain() = LibraryItem(
    metadata = LibraryMetadata(
        tmdbId = tmdbId,
        mediaType = mediaType,
        title = title,
        originalTitle = originalTitle,
        overview = overview,
        year = year,
        posterPath = posterPath,
        runtimeMinutes = runtimeMinutes,
        genres = genres,
        seasonCount = seasonCount,
        episodeCount = episodeCount,
    ),
    localPosterFile = localPosterFile,
    status = status,
    rating = rating,
    addedAt = addedAt,
    updatedAt = updatedAt,
    watchedAt = watchedAt,
    note = note,
    rewatchCount = rewatchCount,
)

/** Nadpisuje dane z TMDB, zachowując dane użytkownika (status, ocena, daty, plakat). */
fun LibraryItemEntity.withMetadata(metadata: LibraryMetadata) = copy(
    title = metadata.title,
    originalTitle = metadata.originalTitle,
    overview = metadata.overview,
    year = metadata.year,
    posterPath = metadata.posterPath,
    runtimeMinutes = metadata.runtimeMinutes,
    genres = metadata.genres,
    seasonCount = metadata.seasonCount,
    episodeCount = metadata.episodeCount,
)
