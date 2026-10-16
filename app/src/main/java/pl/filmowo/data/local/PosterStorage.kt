package pl.filmowo.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import pl.filmowo.core.di.IoDispatcher
import pl.filmowo.core.di.NetworkModule
import pl.filmowo.data.remote.TmdbImages
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

/**
 * Trwały zapis plakatów pozycji z biblioteki. Cache Coila może zostać wyczyszczony,
 * pliki w `filesDir` zostają – dzięki temu plakaty są dostępne offline.
 */
interface PosterStorage {
    /** Zwraca ścieżkę zapisanego pliku albo null, gdy pobranie się nie udało. */
    suspend fun download(posterPath: String, fileName: String): String?
    suspend fun delete(file: String)
}

class FilePosterStorage @Inject constructor(
    @ApplicationContext context: Context,
    @Named(NetworkModule.IMAGE_CLIENT) private val client: OkHttpClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PosterStorage {

    private val directory = File(context.filesDir, "posters")

    override suspend fun download(posterPath: String, fileName: String): String? = withContext(ioDispatcher) {
        val url = TmdbImages.posterLarge(posterPath) ?: return@withContext null
        try {
            directory.mkdirs()
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val temp = File(directory, "$fileName.tmp")
                response.body.byteStream().use { input -> temp.outputStream().use { input.copyTo(it) } }
                val target = File(directory, fileName)
                target.delete()
                if (!temp.renameTo(target)) return@withContext null
                target.absolutePath
            }
        } catch (e: IOException) {
            null
        }
    }

    override suspend fun delete(file: String) {
        withContext(ioDispatcher) { File(file).delete() }
    }
}
