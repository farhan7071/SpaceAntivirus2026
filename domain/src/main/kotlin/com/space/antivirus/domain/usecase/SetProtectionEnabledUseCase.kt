package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.domain.protection.ProtectionManager
import javax.inject.Inject

/**
 * Sprint 042. The single entry point for turning background protection
 * on or off, from anywhere.
 *
 * Home's quick toggle and Settings' switch both call this, so they
 * cannot drift: the schedule-then-persist-then-notify ordering lives in
 * ProtectionManager and neither caller can reorder it.
 */
class SetProtectionEnabledUseCase @Inject constructor(
    private val protectionManager: ProtectionManager,
) {
    suspend operator fun invoke(enabled: Boolean, intervalHours: Long? = null): AppResult<Unit> =
        if (enabled) protectionManager.enable(intervalHours) else protectionManager.disable()
}
