package pl.filmowo.data.local

import androidx.room.Entity
import androidx.room.Index
import pl.filmowo.domain.model.Episode
import pl.filmowo.domain.model.EpisodeInfo
import pl.filmowo.domain.model.EpisodeProgressInput

@Entity(tableName = "seasons", primaryKeys = ["showId", "seasonNumber"])
data class SeasonEntity(
    val showId: Int,
    val seasonNumber: Int,
    val name: String?,
    val airDate: String?,
    val episodeCount: Int,
)

@Entity(
    tableName = "episodes",
    primaryKeys = ["showId", "seasonNumber", "episodeNumber"],
    indices = [Index("showId")],
)
data class EpisodeEntity(
    val showId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String?,
    val overview: String,
    val airDate: String?,
    val runtimeMinutes: Int?,
    val watchedAt: Long?,
)

/** Lekka projekcja do liczenia postępu bez wczytywania opisów odcinków. */
data class EpisodeProgressRow(
    val showId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val airDate: String?,
    val watchedAt: Long?,
)

fun EpisodeEntity.toDomain() = Episode(
    info = EpisodeInfo(
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
        name = name,
        overview = overview,
        airDate = airDate,
        runtimeMinutes = runtimeMinutes,
    ),
    watchedAt = watchedAt,
)

fun EpisodeProgressRow.toProgressInput() = EpisodeProgressInput(
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    airDate = airDate,
    isWatched = watchedAt != null,
)
