package pl.filmowo.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import pl.filmowo.domain.model.MediaFilter
import javax.inject.Inject

/** Ustawienia widoku biblioteki zapamiętywane między uruchomieniami. */
data class LibraryPreferences(
    /** Nazwa wartości enum sortowania; null – domyślne. */
    val sort: String? = null,
    val filter: MediaFilter = MediaFilter.ALL,
)

/** Ustawienia użytkownika zapisywane lokalnie (DataStore). */
interface UserPreferencesRepository {
    val library: Flow<LibraryPreferences>
    val notificationsEnabled: Flow<Boolean>

    suspend fun setLibrarySort(sort: String)
    suspend fun setLibraryFilter(filter: MediaFilter)
    suspend fun setNotificationsEnabled(enabled: Boolean)

    /** Dzień (ISO), dla którego wysłano już powiadomienie o premierach – żeby go nie powtarzać. */
    suspend fun lastNotifiedDate(): String?
    suspend fun setLastNotifiedDate(date: String)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreUserPreferences @Inject constructor(
    @ApplicationContext context: Context,
) : UserPreferencesRepository {

    private val store = context.dataStore

    override val library: Flow<LibraryPreferences> = store.data.map { prefs ->
        LibraryPreferences(
            sort = prefs[LIBRARY_SORT],
            filter = prefs[LIBRARY_FILTER]?.let { name -> MediaFilter.entries.firstOrNull { it.name == name } } ?: MediaFilter.ALL,
        )
    }

    override val notificationsEnabled: Flow<Boolean> = store.data.map { it[NOTIFICATIONS] ?: true }

    override suspend fun setLibrarySort(sort: String) {
        store.edit { it[LIBRARY_SORT] = sort }
    }

    override suspend fun setLibraryFilter(filter: MediaFilter) {
        store.edit { it[LIBRARY_FILTER] = filter.name }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        store.edit { it[NOTIFICATIONS] = enabled }
    }

    override suspend fun lastNotifiedDate(): String? = store.data.first()[LAST_NOTIFIED]

    override suspend fun setLastNotifiedDate(date: String) {
        store.edit { it[LAST_NOTIFIED] = date }
    }

    private companion object {
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
        val LIBRARY_FILTER = stringPreferencesKey("library_filter")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val LAST_NOTIFIED = stringPreferencesKey("last_notified_date")
    }
}
