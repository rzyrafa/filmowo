package pl.filmowo.feature.details

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import pl.filmowo.core.network.AppError
import pl.filmowo.core.network.AppResult
import pl.filmowo.core.network.ConnectivityObserver
import pl.filmowo.core.ui.UiState
import pl.filmowo.core.ui.toUiState

/**
 * Wspólna logika ekranów szczegółów: ładuje dane, pozwala ponowić
 * i sam ponawia po odzyskaniu internetu.
 */
internal class DetailsLoader<T>(
    private val scope: CoroutineScope,
    connectivity: ConnectivityObserver,
    private val fetch: suspend () -> AppResult<T>,
) {
    private val _state = MutableStateFlow<UiState<T>>(UiState.Loading)
    val state: StateFlow<UiState<T>> = _state.asStateFlow()
    private var job: Job? = null

    init {
        load()
        connectivity.isOnline
            .filter { it }
            .onEach {
                val current = _state.value
                if (current is UiState.Error && current.error == AppError.NETWORK) load()
            }
            .launchIn(scope)
    }

    fun load() {
        job?.cancel()
        _state.value = UiState.Loading
        job = scope.launch { _state.value = fetch().toUiState() }
    }
}
