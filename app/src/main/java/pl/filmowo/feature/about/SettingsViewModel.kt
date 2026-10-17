package pl.filmowo.feature.about

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.filmowo.data.backup.BackupRepository
import pl.filmowo.data.backup.ImportResult
import pl.filmowo.data.preferences.UserPreferencesRepository
import javax.inject.Inject

/** Jednorazowy komunikat po eksporcie lub imporcie. */
sealed interface BackupMessage {
    data class Exported(val items: Int) : BackupMessage
    data object ExportFailed : BackupMessage
    data class Imported(val items: Int) : BackupMessage
    data object InvalidFile : BackupMessage
    data object ImportFailed : BackupMessage
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val backup: BackupRepository,
) : ViewModel() {

    val notificationsEnabled: StateFlow<Boolean> =
        preferences.notificationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _isWorking = MutableStateFlow(false)

    /** Trwa eksport lub import – przyciski są wtedy nieaktywne. */
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    private val _message = MutableStateFlow<BackupMessage?>(null)
    val message: StateFlow<BackupMessage?> = _message.asStateFlow()

    fun onNotificationsChange(enabled: Boolean) {
        viewModelScope.launch { preferences.setNotificationsEnabled(enabled) }
    }

    fun onExport(uri: Uri) = runBackup {
        backup.export(uri)?.let { BackupMessage.Exported(it) } ?: BackupMessage.ExportFailed
    }

    fun onImport(uri: Uri) = runBackup {
        when (val result = backup.import(uri)) {
            is ImportResult.Success -> BackupMessage.Imported(result.items)
            ImportResult.InvalidFile -> BackupMessage.InvalidFile
            ImportResult.ReadError -> BackupMessage.ImportFailed
        }
    }

    fun onMessageShown() {
        _message.value = null
    }

    private fun runBackup(block: suspend () -> BackupMessage) {
        if (_isWorking.value) return
        _isWorking.value = true
        viewModelScope.launch {
            try {
                _message.value = block()
            } finally {
                _isWorking.value = false
            }
        }
    }
}
