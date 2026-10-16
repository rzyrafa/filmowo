package pl.filmowo.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pl.filmowo.data.local.FilmowoDatabase
import pl.filmowo.data.local.LibraryDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FilmowoDatabase =
        Room.databaseBuilder(context, FilmowoDatabase::class.java, FilmowoDatabase.NAME).build()

    @Provides
    fun provideLibraryDao(database: FilmowoDatabase): LibraryDao = database.libraryDao()
}
