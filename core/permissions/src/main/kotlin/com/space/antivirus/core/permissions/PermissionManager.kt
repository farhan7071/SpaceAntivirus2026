package com.space.antivirus.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Named permission set for every permission the Feature Strategy (Sprint
 * 002 §3) confirmed as justified. Deliberately closed/enumerated rather
 * than raw strings scattered across features — a permission not in this
 * enum cannot be requested, which is itself an architectural guard against
 * permission-sprawl (Sprint 001 Risk #4).
 */
enum class AppPermission(val manifestPermission: String, val rationaleKey: String) {
    STORAGE(Manifest.permission.READ_EXTERNAL_STORAGE, "permission_storage_rationale"),
    NOTIFICATIONS(Manifest.permission.POST_NOTIFICATIONS, "permission_notifications_rationale"),
    // LOCATION intentionally omitted pending Sprint 002 §3's Feature
    // Strategy decision on the Wi-Fi info feature — see
    // Sprint 002.75 §6 / §21 checklist. Add here only once that decision
    // is confirmed, never speculatively.
}

sealed interface PermissionStatus {
    data object Granted : PermissionStatus
    data object Denied : PermissionStatus
    data object NotRequested : PermissionStatus
}

/**
 * Central permission-status source of truth. Feature ViewModels ask this
 * rather than calling ContextCompat.checkSelfPermission directly, so the
 * "recovery" UI pattern (Sprint 002.5 permission-missing cards) has one
 * consistent status API to observe. Just-in-time *requesting* is done via
 * Compose's rememberLauncherForActivityResult in each feature screen —
 * this class only reports status and rationale text keys.
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun statusOf(permission: AppPermission): PermissionStatus {
        val granted = ContextCompat.checkSelfPermission(
            context,
            permission.manifestPermission,
        ) == PackageManager.PERMISSION_GRANTED
        return if (granted) PermissionStatus.Granted else PermissionStatus.Denied
    }
}
