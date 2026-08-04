package com.space.antivirus.domain.usecase

import com.space.antivirus.core.model.ProtectionState
import com.space.antivirus.domain.protection.ProtectionManager
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Sprint 042. Same shape as every other Observe* use case: a live Flow
 * exposed directly rather than forced through the one-shot UseCase base.
 * Exists so screens depend on `domain`'s use-case surface like every
 * other screen does, rather than reaching for the manager directly.
 */
class ObserveProtectionStateUseCase @Inject constructor(
    private val protectionManager: ProtectionManager,
) {
    operator fun invoke(): Flow<ProtectionState> = protectionManager.state
}
