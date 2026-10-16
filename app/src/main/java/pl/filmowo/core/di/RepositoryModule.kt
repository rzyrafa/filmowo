package pl.filmowo.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.filmowo.core.network.AndroidConnectivityObserver
import pl.filmowo.core.network.ConnectivityObserver
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
    abstract fun bindConnectivityObserver(impl: AndroidConnectivityObserver): ConnectivityObserver
}
