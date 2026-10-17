package pl.filmowo

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import pl.filmowo.work.LibrarySyncWorker
import javax.inject.Inject

@HiltAndroidApp
class FilmowoApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    // WorkManager inicjalizowany ręcznie (domyślny inicjalizator wyłączony w manifeście), żeby workery dostały zależności z Hilt.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        LibrarySyncWorker.schedule(this)
    }
}
