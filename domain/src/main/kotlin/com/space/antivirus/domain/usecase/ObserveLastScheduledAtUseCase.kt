package com.space.antivirus.domain.usecase

import com.space.antivirus.domain.repository.BackgroundProtectionPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveLastScheduledAtUseCase @Inject constructor(
    private val preferences: BackgroundProtectionPreferences,
) {
    operator fun invoke(): Flow<Long?> = preferences.lastScheduledAtEpochMillis
}
