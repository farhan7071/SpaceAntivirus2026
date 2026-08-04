package com.space.antivirus.core.protection

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.space.antivirus.domain.protection.ProtectionNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every notification this app posts — Sprint 042.
 *
 * **On the copy, which is the part that needed the most care.**
 *
 * The sprint brief's example text for the ongoing notification was
 * "Real-time protection active". This app has no real-time protection.
 * It runs scheduled scans; live file scanning, APK interception,
 * accessibility monitoring and install interception are all explicitly
 * out of this sprint's scope and none of them exist anywhere in the
 * project. A permanent notification claiming real-time protection would
 * be a false security claim sitting in the user's shade indefinitely —
 * the most consequential place this project could put one, and squarely
 * against ADR 0015's "never exaggerate" rule. The ongoing notification
 * therefore says what is true: scheduled scanning is on, and roughly
 * when the next one is due.
 *
 * For the same reason the next-scan line is worded as approximate.
 * WorkManager decides when periodic work actually fires and defers it
 * for the battery and storage constraints this project sets, so an exact
 * "next scan at 14:30" would state a guarantee the platform does not
 * make.
 *
 * **On POST_NOTIFICATIONS.** The manifest has declared it since Sprint
 * 003, but on API 33+ it is a runtime permission, and this project has
 * never requested it — so it can be, and by default is, denied. Every
 * method here checks first and no-ops rather than throwing, and
 * `areNotificationsPermitted()` exposes the real answer so the UI can be
 * honest about it instead of showing a protection toggle whose
 * notification silently never appears.
 *
 * **Channels.** Three, matching the three genuinely different kinds of
 * interruption, at three genuinely different importances. Protection
 * status is LOW: it is a persistent status line, not an event, and
 * should never make a sound. Scheduled-scan results are DEFAULT.
 * Security alerts is HIGH and is created here but deliberately not yet
 * posted to by anything — see its own constant.
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) : ProtectionNotifier {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createChannels()
    }

    override fun areNotificationsPermitted(): Boolean {
        if (!notificationManager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun showProtectionActive(earliestNextScanEpochMillis: Long?) {
        if (!areNotificationsPermitted()) return

        val body = earliestNextScanEpochMillis
            ?.let { "Scheduled scans are on. Next scan around ${formatTime(it)}." }
            // Says less rather than guessing, per the contract.
            ?: "Scheduled scans are on."

        val notification = NotificationCompat.Builder(context, CHANNEL_PROTECTION_STATUS)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Protection enabled")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setOngoing(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()

        postSafely(NOTIFICATION_ID_PROTECTION_STATUS, notification)
    }

    override fun hideProtectionActive() {
        notificationManager.cancel(NOTIFICATION_ID_PROTECTION_STATUS)
    }

    override fun showScanCompleted(threatsFound: Int, highRiskFound: Int) {
        if (!areNotificationsPermitted()) return

        // Three genuinely different outcomes, worded to match what was
        // actually found. A clean scan is reported as a clean scan, not
        // dressed up as a narrow escape.
        val (title, body) = when {
            threatsFound == 0 ->
                "Scheduled scan complete" to "No threats found."
            highRiskFound > 0 ->
                "Attention needed" to
                    "$highRiskFound of $threatsFound finding(s) look higher risk. Tap to review."
            else ->
                "Scheduled scan complete" to
                    "$threatsFound finding(s) worth reviewing. Nothing high risk."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_SCHEDULED_SCAN)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        postSafely(NOTIFICATION_ID_SCAN_RESULT, notification)
    }

    /**
     * Even with the permission check above, `notify` can throw on some
     * OEM builds and in edge cases around permission revocation while
     * the process is alive. A failure to post a status notification must
     * never take down a background worker or a settings toggle.
     */
    private fun postSafely(id: Int, notification: android.app.Notification) {
        try {
            notificationManager.notify(id, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the post.
        }
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channels = listOf(
            NotificationChannel(
                CHANNEL_PROTECTION_STATUS,
                "Protection status",
                // LOW: a persistent status line should never make a
                // sound. It is there to be glanced at, not to interrupt.
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "An ongoing reminder that scheduled scanning is switched on."
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_SCHEDULED_SCAN,
                "Scheduled scan results",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "The result of an automatic scan, if you've asked to be told."
            },
            NotificationChannel(
                CHANNEL_SECURITY_ALERTS,
                "Security alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Urgent security findings that need your attention."
            },
        )

        val systemManager = context.getSystemService(NotificationManager::class.java) ?: return
        channels.forEach(systemManager::createNotificationChannel)
    }

    private fun formatTime(epochMillis: Long): String =
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(epochMillis))

    companion object {
        const val CHANNEL_PROTECTION_STATUS = "protection_status"
        const val CHANNEL_SCHEDULED_SCAN = "scheduled_scan"

        /**
         * Created so the channel exists and the user can configure it
         * before anything ever uses it, but deliberately not posted to
         * by anything in Sprint 042.
         *
         * A HIGH-importance alert channel is for something urgent that
         * the user has not already been shown. Every finding this app
         * can currently produce comes from a scan the user either
         * started themselves or has already been told about through the
         * scheduled-scan channel. Posting the same information a second
         * time at higher urgency would be manufacturing alarm, not
         * conveying it. This becomes real when something genuinely
         * urgent and previously-unseen exists to report.
         */
        const val CHANNEL_SECURITY_ALERTS = "security_alerts"

        private const val NOTIFICATION_ID_PROTECTION_STATUS = 1001
        private const val NOTIFICATION_ID_SCAN_RESULT = 1002
    }
}
