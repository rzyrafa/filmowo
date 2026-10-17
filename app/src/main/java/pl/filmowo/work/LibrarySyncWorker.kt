package pl.filmowo.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import pl.filmowo.core.TimeProvider
import pl.filmowo.core.network.AppError
import pl.filmowo.core.network.AppResult
import pl.filmowo.core.today
import pl.filmowo.data.preferences.UserPreferencesRepository
import pl.filmowo.data.repository.EpisodeRepository
import java.util.concurrent.TimeUnit

/**
 * Okresowo odświeża odcinki seriali z biblioteki (nowe sezony, zmienione daty premier)
 * i raz dziennie powiadamia o odcinkach, które mają dziś premierę.
 */
@HiltWorker
class LibrarySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val episodes: EpisodeRepository,
    private val preferences: UserPreferencesRepository,
    private val notifier: PremiereNotifier,
    private val time: TimeProvider,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val sync = episodes.syncAll()
        // Powiadomienie także po nieudanej synchronizacji – daty z poprzedniej wciąż są aktualne.
        notifyTodayPremieres()
        return when {
            sync is AppResult.Success -> Result.success()
            sync is AppResult.Failure && sync.error in PERMANENT_ERRORS -> Result.success()
            runAttemptCount < MAX_RETRIES -> Result.retry()
            else -> Result.success()
        }
    }

    private suspend fun notifyTodayPremieres() {
        if (!preferences.notificationsEnabled.first()) return
        val today = time.today()
        if (preferences.lastNotifiedDate() == today) return
        val airing = episodes.getAiringOn(today).filterNot { it.isWatched }
        if (airing.isNotEmpty() && !notifier.notifyPremieres(airing)) return
        preferences.setLastNotifiedDate(today)
    }

    companion object {
        private const val WORK_NAME = "library_sync"
        private const val MAX_RETRIES = 3
        private val PERMANENT_ERRORS = setOf(AppError.MISSING_API_KEY, AppError.UNAUTHORIZED)

        /** Planuje pracę co 12 godzin (gdy jest internet); ponowne wywołanie nie zmienia harmonogramu. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<LibrarySyncWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
