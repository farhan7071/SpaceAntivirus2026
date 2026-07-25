package com.space.antivirus.domain.usecase

import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.common.IoDispatcher
import com.space.antivirus.core.model.CleanableItem
import com.space.antivirus.core.model.ScanScope
import com.space.antivirus.domain.UseCase
import com.space.antivirus.domain.cleaning.JunkFileClassifier
import com.space.antivirus.domain.repository.EnumerationRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

/**
 * The junk-file domain capability's entry point — deliberately reuses
 * EnumerationRepository.enumerateFiles (Sprint 004B) rather than any new
 * file-walking mechanism; this is the same "what can be scanned" contract
 * RunScanRequestUseCase's own target resolution already depends on.
 * JunkFileClassifier stays a pure, standalone policy (domain/cleaning) —
 * this UseCase's only job is enumerate-then-classify, no detection logic
 * of its own.
 *
 * Deliberately separate from RunScanRequestUseCase/ThreatAnalyzer
 * entirely — a cache file is not a security concern (CleanableCategory's
 * own KDoc), and folding storage-reclamation into the threat-detection
 * pipeline would conflate two genuinely different domain concepts. No new
 * AppError cases needed: EnumerationRepository's existing failure mapping
 * (PermissionMissing/StorageUnavailable/InvalidScanConfiguration) already
 * covers every failure this UseCase can encounter.
 *
 * Identifies candidates only — nothing in this project yet deletes a
 * file. That's explicitly a future Clean UI sprint's job, once this
 * domain layer exists for it to act on.
 */
class FindCleanableItemsUseCase @Inject constructor(
    private val enumerationRepository: EnumerationRepository,
    private val classifier: JunkFileClassifier,
    @IoDispatcher dispatcher: CoroutineDispatcher,
) : UseCase<ScanScope, List<CleanableItem>>(dispatcher) {

    override suspend fun execute(params: ScanScope): AppResult<List<CleanableItem>> {
        val nowEpochMillis = System.currentTimeMillis()
        return when (val result = enumerationRepository.enumerateFiles(params)) {
            is AppResult.Success -> AppResult.Success(
                result.data.mapNotNull { file -> classifier.classify(file, nowEpochMillis) },
            )
            is AppResult.Failure -> result
            AppResult.Loading -> AppResult.Loading
        }
    }
}
