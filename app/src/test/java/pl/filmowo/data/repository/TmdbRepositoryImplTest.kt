package pl.filmowo.data.repository

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import pl.filmowo.core.di.NetworkModule
import pl.filmowo.core.network.AppError
import pl.filmowo.core.network.AppResult
import pl.filmowo.data.remote.TmdbApi
import pl.filmowo.data.remote.TmdbApiKeyInterceptor
import pl.filmowo.domain.model.MediaFilter
import pl.filmowo.domain.model.MediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class TmdbRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var api: TmdbApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json: Json = NetworkModule.provideJson()
        api = Retrofit.Builder()
            .baseUrl(server.url("/3/"))
            .client(OkHttpClient.Builder().addInterceptor(TmdbApiKeyInterceptor(API_KEY)).build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TmdbApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun repository(apiKey: String = API_KEY) = TmdbRepositoryImpl(api, apiKey)

    private fun enqueueJson(body: String, code: Int = 200) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))
    }

    @Test
    fun `multi search maps movies and shows and skips people`() = runTest {
        enqueueJson(
            """
            {"page":1,"total_pages":3,"results":[
              {"id":1,"media_type":"movie","title":"Incepcja","release_date":"2010-07-15","poster_path":"/a.jpg","overview":"Sen"},
              {"id":2,"media_type":"tv","name":"Dark","first_air_date":"2017-12-01"},
              {"id":3,"media_type":"person","name":"Leonardo DiCaprio"}
            ]}
            """.trimIndent(),
        )

        val result = repository().search("inc", MediaFilter.ALL, 1) as AppResult.Success
        assertEquals(listOf(1, 2), result.data.items.map { it.id })
        assertEquals(MediaType.TV, result.data.items[1].mediaType)
        assertEquals(2010, result.data.items[0].year)
        assertEquals(true, result.data.hasMore)

        val request = server.takeRequest()
        assertEquals("/3/search/multi", request.requestUrl!!.encodedPath)
        assertEquals("inc", request.requestUrl!!.queryParameter("query"))
        assertEquals("pl-PL", request.requestUrl!!.queryParameter("language"))
        assertEquals(API_KEY, request.requestUrl!!.queryParameter("api_key"))
    }

    @Test
    fun `movie filter uses movie endpoint and sets media type`() = runTest {
        enqueueJson("""{"page":1,"total_pages":1,"results":[{"id":5,"title":"Matrix"}]}""")

        val result = repository().search("matrix", MediaFilter.MOVIES, 1) as AppResult.Success
        assertEquals(MediaType.MOVIE, result.data.items.single().mediaType)
        assertNull(result.data.items.single().year)
        assertEquals("/3/search/movie", server.takeRequest().requestUrl!!.encodedPath)
    }

    @Test
    fun `missing polish overview falls back to english`() = runTest {
        enqueueJson("""{"id":7,"title":"Film","overview":"","runtime":100}""")
        enqueueJson("""{"id":7,"title":"Film","overview":"English overview","runtime":100}""")

        val result = repository().movieDetails(7) as AppResult.Success
        assertEquals("English overview", result.data.overview)
        assertNull(result.data.originalTitle)
        assertEquals("pl-PL", server.takeRequest().requestUrl!!.queryParameter("language"))
        assertEquals("en-US", server.takeRequest().requestUrl!!.queryParameter("language"))
    }

    @Test
    fun `show details put specials last and use average runtime`() = runTest {
        enqueueJson(
            """
            {"id":9,"name":"Serial","overview":"Opis","episode_run_time":[40,50],
             "number_of_seasons":2,"number_of_episodes":20,
             "seasons":[
               {"id":100,"season_number":0,"name":"Specials","episode_count":3},
               {"id":101,"season_number":1,"name":"Sezon 1","episode_count":10,"air_date":"2015-01-01"},
               {"id":102,"season_number":2,"name":"","episode_count":10}
             ]}
            """.trimIndent(),
        )

        val show = (repository().showDetails(9) as AppResult.Success).data
        assertEquals(listOf(1, 2, 0), show.seasons.map { it.seasonNumber })
        assertEquals(45, show.episodeRuntimeMinutes)
        assertNull(show.seasons[1].name)
        assertEquals(2015, show.seasons[0].year)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `http errors are mapped to app errors`() = runTest {
        enqueueJson("""{"status_code":7}""", code = 401)
        enqueueJson("{}", code = 404)
        enqueueJson("{}", code = 503)

        assertEquals(AppResult.Failure(AppError.UNAUTHORIZED), repository().movieDetails(1))
        assertEquals(AppResult.Failure(AppError.NOT_FOUND), repository().movieDetails(1))
        assertEquals(AppResult.Failure(AppError.SERVER), repository().movieDetails(1))
    }

    @Test
    fun `connection failure is a network error`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertEquals(AppResult.Failure(AppError.NETWORK), repository().showDetails(1))
    }

    @Test
    fun `blank api key fails without calling the server`() = runTest {
        assertEquals(AppResult.Failure(AppError.MISSING_API_KEY), repository(apiKey = "").movieDetails(1))
        assertEquals(0, server.requestCount)
    }

    private companion object {
        const val API_KEY = "test-key"
    }
}
