package pl.filmowo.core.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import pl.filmowo.BuildConfig
import pl.filmowo.data.remote.TmdbApi
import pl.filmowo.data.remote.TmdbApiKeyInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    const val TMDB_API_KEY = "tmdbApiKey"
    const val IMAGE_CLIENT = "imageClient"

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Named(TMDB_API_KEY)
    fun provideApiKey(): String = BuildConfig.TMDB_API_KEY

    @Provides
    @Singleton
    fun provideOkHttpClient(@Named(TMDB_API_KEY) apiKey: String): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(TmdbApiKeyInterceptor(apiKey))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    /** Klient do pobierania obrazków – bez klucza API (image.tmdb.org go nie wymaga). */
    @Provides
    @Singleton
    @Named(IMAGE_CLIENT)
    fun provideImageClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideTmdbApi(client: OkHttpClient, json: Json): TmdbApi =
        Retrofit.Builder()
            .baseUrl(TmdbApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TmdbApi::class.java)
}
