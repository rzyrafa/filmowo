package pl.filmowo.core.network

/** Rodzaje błędów pokazywane użytkownikowi. */
enum class AppError {
    /** Brak połączenia z internetem lub przekroczony czas oczekiwania. */
    NETWORK,

    /** Serwer odrzucił klucz API (401). */
    UNAUTHORIZED,

    /** Klucz API nie został ustawiony w local.properties. */
    MISSING_API_KEY,

    /** Nie znaleziono zasobu (404). */
    NOT_FOUND,

    /** Błąd po stronie serwera (5xx) lub limit zapytań (429). */
    SERVER,

    UNKNOWN,
}

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}
