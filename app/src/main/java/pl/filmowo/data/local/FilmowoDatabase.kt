package pl.filmowo.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [LibraryItemEntity::class, SeasonEntity::class, EpisodeEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        // v2: tabele sezonów i odcinków.
        AutoMigration(from = 1, to = 2),
    ],
)
@TypeConverters(Converters::class)
abstract class FilmowoDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun episodeDao(): EpisodeDao

    companion object {
        const val NAME = "filmowo.db"
    }
}
