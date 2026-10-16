package pl.filmowo.data.remote

/** Budowanie adresów obrazków TMDB (https://developer.themoviedb.org/docs/image-basics). */
object TmdbImages {
    private const val BASE = "https://image.tmdb.org/t/p/"

    fun posterSmall(path: String?): String? = path?.let { "${BASE}w185$it" }

    fun posterLarge(path: String?): String? = path?.let { "${BASE}w500$it" }
}
