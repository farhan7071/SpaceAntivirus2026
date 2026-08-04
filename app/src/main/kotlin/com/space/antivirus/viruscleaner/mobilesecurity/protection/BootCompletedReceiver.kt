package com.space.antivirus.viruscleaner.mobilesecurity.protection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.space.antivirus.domain.protection.ProtectionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Restores protection's user-visible state after a device restart —
 * Sprint 042.
 *
 * **What this deliberately does not do: reschedule work.** WorkManager
 * already reschedules its own persisted periodic work after boot, via a
 * receiver merged automatically from the androidx.work AAR — that is
 * what the manifest's RECEIVE_BOOT_COMPLETED permission has been for
 * since Sprint 025, and it is documented there. Enqueueing again here
 * would replace a live schedule and reset its interval window, so a user
 * who reboots frequently would see scans repeatedly pushed back. The
 * work survives reboot without our help.
 *
 * What genuinely does not survive a reboot is the ongoing status
 * notification, which the system clears. Without this receiver a user
 * who restarts their phone would find protection still working but the
 * notification saying so gone — which reads as protection having
 * silently switched itself off. That is the whole job here, and
 * `ProtectionManager.restoreAfterBoot` no-ops when protection is
 * disabled.
 *
 * `goAsync()` is used because `onReceive` runs on the main thread and
 * must return quickly, while reading DataStore suspends. The pending
 * result is always finished, on every path, or the process would be
 * held alive until the system kills it.
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var protectionManager: ProtectionManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                protectionManager.restoreAfterBoot()
            } catch (e: Exception) {
                // A failure to restore a notification must never crash
                // the boot broadcast. Protection itself is unaffected —
                // WorkManager has already rescheduled the real work.
            } finally {
                pendingResult.finish()
            }
        }
    }
}
