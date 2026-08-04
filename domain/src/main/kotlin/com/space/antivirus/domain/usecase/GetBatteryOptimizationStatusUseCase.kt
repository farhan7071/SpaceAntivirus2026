package com.space.antivirus.domain.usecase

import com.space.antivirus.domain.protection.BatteryOptimizationStatus
import javax.inject.Inject

/**
 * Sprint 042. Whether the standard Android battery allowlist may delay
 * scheduled scans. Informational only — nothing in this app acts on the
 * answer beyond showing the user a card explaining the trade-off.
 */
class GetBatteryOptimizationStatusUseCase @Inject constructor(
    private val batteryOptimizationStatus: BatteryOptimizationStatus,
) {
    operator fun invoke(): Boolean = batteryOptimizationStatus.isIgnoringBatteryOptimizations()
}
