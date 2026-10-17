package pl.filmowo.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.filmowo.MainActivity
import pl.filmowo.R
import pl.filmowo.domain.model.UpcomingEpisode
import javax.inject.Inject

/** Powiadomienia o premierach odcinków seriali z biblioteki. */
interface PremiereNotifier {
    /** Zwraca false, gdy powiadomienia są zablokowane (brak uprawnienia). */
    fun notifyPremieres(episodes: List<UpcomingEpisode>): Boolean
}

class AndroidPremiereNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : PremiereNotifier {

    override fun notifyPremieres(episodes: List<UpcomingEpisode>): Boolean {
        if (!canNotify(context)) return false
        ensureChannel()
        val lines = episodes.map { context.getString(R.string.notification_episode_line, it.showTitle, it.seasonNumber, it.episodeNumber) }
        val title = context.resources.getQuantityString(R.plurals.notification_premieres_title, episodes.size, episodes.size)
        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(lines.joinToString(", "))
            .setStyle(NotificationCompat.InboxStyle().also { style -> lines.forEach(style::addLine) })
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_premieres),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "premieres"
        private const val NOTIFICATION_ID = 1001

        /** Na Androidzie 13+ potrzebne jest uprawnienie POST_NOTIFICATIONS. */
        fun canNotify(context: Context): Boolean =
            (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                ) &&
                NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
