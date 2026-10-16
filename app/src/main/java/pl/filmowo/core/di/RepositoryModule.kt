package pl.filmowo.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.filmowo.core.network.AndroidConnectivityObserver
import pl.filmowo.core.network.ConnectivityObserver
import pl.filmowo.data.local.FilePosterStorage
import pl.filmowo.data.local.PosterStorage
import pl.filmowo.data.repository.EpisodeRepository
import pl.filmowo.data.repository.EpisodeRepositoryImpl
import pl.filmowo.data.repository.LibraryRepository
import pl.filmowo.data.repository.LibraryRepositoryImpl
import pl.filmowo.data.repository.TmdbRepository
import pl.filmowo.data.repository.TmdbRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTmdbRepository(impl: TmdbRepositoryImpl): TmdbRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindEpisodeRepository(impl: EpisodeRepositoryImpl): EpisodeRepository

    @Binds
    @Singleton
    abstract fun bindPosterStorage(impl: FilePosterStorage): PosterStorage

    @Binds
    @Singleton
    abstract fun bindConnectivityObserver(impl: AndroidConnectivityObserver): ConnectivityObserver
}
