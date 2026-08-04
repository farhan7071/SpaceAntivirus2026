package com.space.antivirus.domain.usecase

import com.space.antivirus.domain.protection.ProtectionManager
import javax.inject.Inject

/** Sprint 042. */
class SetNotifyAfterScanUseCase @Inject constructor(
    private val protectionManager: ProtectionManager,
) {
    suspend operator fun invoke(enabled: Boolean) = protectionManager.setNotifyAfterScan(enabled)
}
