package pl.filmowo.data.backup

import kotlinx.serialization.Serializable
import pl.filmowo.data.local.EpisodeEntity
import pl.filmowo.data.local.LibraryItemEntity
import pl.filmowo.data.local.SeasonEntity
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStatus

/**
 * Format pliku kopii zapasowej. Osobne klasy zamiast encji Room – zmiana schematu bazy
 * nie psuje starszych kopii. Nowe pola dodajemy z wartościami domyślnymi.
 */
@Serializable
data class BackupFile(
    val version: Int,
    val exportedAt: Long,
    val items: List<BackupItem> = emptyList(),
    val seasons: List<BackupSeason> = emptyList(),
    val episodes: List<BackupEpisode> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

@Serializable
data class BackupItem(
    val tmdbId: Int,
    val mediaType: MediaType,
    val title: String,
    val originalTitle: String? = null,
    val overview: String = "",
    val year: Int? = null,
    val posterPath: String? = null,
    val runtimeMinutes: Int? = null,
    val genres: List<String> = emptyList(),
    val seasonCount: Int? = null,
    val episodeCount: Int? = null,
    val status: WatchStatus,
    val rating: Int? = null,
    val addedAt: Long,
    val updatedAt: Long,
    val watchedAt: Long? = null,
    val note: String? = null,
    val rewatchCount: Int = 0,
)

@Serializable
data class BackupSeason(
    val showId: Int,
    val seasonNumber: Int,
    val name: String? = null,
    val airDate: String? = null,
    val episodeCount: Int = 0,
)

@Serializable
data class BackupEpisode(
    val showId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String? = null,
    val overview: String = "",
    val airDate: String? = null,
    val runtimeMinutes: Int? = null,
    val watchedAt: Long? = null,
)

internal fun LibraryItemEntity.toBackup() = BackupItem(
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
    status = status,
    rating = rating,
    addedAt = addedAt,
    updatedAt = updatedAt,
    watchedAt = watchedAt,
    note = note,
    rewatchCount = rewatchCount,
)

internal fun BackupItem.toEntity(localPosterFile: String?) = LibraryItemEntity(
    tmdbId = tmdbId,
    mediaType = mediaType,
    title = title,
    originalTitle = originalTitle,
    overview = overview,
    year = year,
    posterPath = posterPath,
    localPosterFile = localPosterFile,
    runtimeMinutes = runtimeMinutes,
    genres = genres,
    seasonCount = seasonCount,
    episodeCount = episodeCount,
    status = status,
    rating = rating?.takeIf { it in 1..10 },
    addedAt = addedAt,
    updatedAt = updatedAt,
    watchedAt = watchedAt,
    note = note,
    rewatchCount = rewatchCount.coerceAtLeast(0),
)

internal fun SeasonEntity.toBackup() = BackupSeason(showId, seasonNumber, name, airDate, episodeCount)

internal fun BackupSeason.toEntity() = SeasonEntity(showId, seasonNumber, name, airDate, episodeCount)

internal fun EpisodeEntity.toBackup() =
    BackupEpisode(showId, seasonNumber, episodeNumber, name, overview, airDate, runtimeMinutes, watchedAt)

internal fun BackupEpisode.toEntity() =
    EpisodeEntity(showId, seasonNumber, episodeNumber, name, overview, airDate, runtimeMinutes, watchedAt)
