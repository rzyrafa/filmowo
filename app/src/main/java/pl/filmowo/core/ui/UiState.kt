package pl.filmowo.core.ui

import pl.filmowo.core.network.AppError
import pl.filmowo.core.network.AppResult

/** Stan ekranu ładującego jeden zestaw danych. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: AppError) : UiState<Nothing>
}

fun <T> AppResult<T>.toUiState(): UiState<T> = when (this) {
    is AppResult.Success -> UiState.Success(data)
    is AppResult.Failure -> UiState.Error(error)
}
