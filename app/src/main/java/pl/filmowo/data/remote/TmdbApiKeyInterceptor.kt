package pl.filmowo.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/** Dodaje klucz API v3 jako parametr `api_key` do każdego zapytania. */
class TmdbApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.newBuilder()
            .addQueryParameter("api_key", apiKey)
            .build()
        return chain.proceed(request.newBuilder().url(url).build())
    }
}
