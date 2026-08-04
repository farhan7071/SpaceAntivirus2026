package com.space.antivirus.feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.AnalyzerId
import com.space.antivirus.core.model.CleanupRecord
import com.space.antivirus.core.model.Detection
import com.space.antivirus.core.model.RiskLevel
import com.space.antivirus.core.model.ScanResult
import com.space.antivirus.core.model.ScanSession
import com.space.antivirus.core.model.ScanSessionState
import com.space.antivirus.core.model.ScanStatistics
import com.space.antivirus.core.model.ScanType
import com.space.antivirus.core.model.Threat
import com.space.antivirus.core.model.ThreatType
import com.space.antivirus.core.model.TrustedItem
import com.space.antivirus.core.model.TrustedItemType
import com.space.antivirus.core.testing.MainDispatcherRule
import com.space.antivirus.domain.repository.CleanupHistoryRepository
import com.space.antivirus.domain.repository.SecurityRepository
import com.space.antivirus.domain.repository.TrustedItemRepository
import com.space.antivirus.domain.usecase.ObserveCleanupHistoryUseCase
import com.space.antivirus.domain.usecase.ObserveScanHistoryUseCase
import com.space.antivirus.domain.usecase.ObserveTrustedItemsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * This project's first production ViewModel test — establishes the
 * pattern every remaining feature ViewModel test follows (Sprint 017).
 * Uses mockk directly on SecurityRepository/TrustedItemRepository fed
 * into REAL ObserveScanHistoryUseCase/ObserveTrustedItemsUseCase
 * instances, rather than hand-writing full local Fake* classes (domain's
 * own Fake* classes exist only in :domain's own test source set,
 * invisible here) — proportionate for the 2 methods actually exercised,
 * versus stubbing out 14 SecurityRepository methods this ViewModel never
 * calls.
 */
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val securityRepository = mockk<SecurityRepository>()
    private val trustedItemRepository = mockk<TrustedItemRepository>()
    private val cleanupHistoryRepository = mockk<CleanupHistoryRepository>()

    private fun buildViewModel(): HomeViewModel = HomeViewModel(
        observeScanHistory = ObserveScanHistoryUseCase(securityRepository),
        observeTrustedItems = ObserveTrustedItemsUseCase(trustedItemRepository),
        observeCleanupHistory = ObserveCleanupHistoryUseCase(cleanupHistoryRepository),
    )

    /** Sprint 040 added a third source. Existing tests call this so they
     *  keep asserting only what they were written to assert. */
    private fun noCleanupHistory() {
        every { cleanupHistoryRepository.observeHistory() } returns flowOf(emptyList())
    }

    private fun cleanupRecord(
        bytesFreed: Long = 482_000_000L,
        itemsDeleted: Int = 152,
        wasCancelled: Boolean = false,
    ) = CleanupRecord(
        id = "c1",
        completedAtEpochMillis = 9_000L,
        itemsDeleted = itemsDeleted,
        itemsFailed = 0,
        bytesFreed = bytesFreed,
        durationMillis = 18_000L,
        wasCancelled = wasCancelled,
    )

    private fun completedSession(id: String = "s1") = ScanSession(
        id = id,
        scanType = ScanType.QUICK,
        state = ScanSessionState.COMPLETED,
        startedAtEpochMillis = 1_000L,
        completedAtEpochMillis = 2_000L,
    )

    private fun cleanScanResult() = ScanResult(
        session = completedSession(),
        statistics = ScanStatistics(
            itemsScanned = 10,
            threatsFound = 0,
            itemsInconclusive = 0,
            itemsTrusted = 0,
            durationMillis = 500,
        ),
        threats = emptyList(),
    )

    private fun flaggedScanResult(threatCount: Int = 1) = ScanResult(
        session = completedSession(),
        statistics = ScanStatistics(
            itemsScanned = 10,
            threatsFound = threatCount,
            itemsInconclusive = 0,
            itemsTrusted = 0,
            durationMillis = 500,
        ),
        threats = List(threatCount) { index ->
            Threat(
                id = "t$index",
                targetIdentifier = "com.example.app$index",
                threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                riskLevel = RiskLevel.ATTENTION,
                title = "Unusual permission combination",
                description = "test description",
                detections = listOf(
                    Detection(
                        id = "d$index",
                        analyzerId = AnalyzerId("test"),
                        threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                        evidenceDescription = "test evidence",
                        riskLevel = RiskLevel.ATTENTION,
                    ),
                ),
                discoveredAtEpochMillis = 2_000L,
            )
        },
    )

    private fun trustedItem(id: String) = TrustedItem(
        id = id,
        identifier = "com.example.trusted$id",
        type = TrustedItemType.APPLICATION,
        addedAtEpochMillis = 0L,
    )

    @Test
    fun `no scan history and no trusted items yields UNKNOWN status and zero trusted count`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(emptyList())
        noCleanupHistory()
        every { trustedItemRepository.observeTrustedItems() } returns flowOf(emptyList())

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(HomeUiState.Loading)
            val state = awaitItem() as HomeUiState.Loaded
            assertThat(state.protectionStatus).isEqualTo(ProtectionStatus.UNKNOWN)
            assertThat(state.lastScanSummary).isNull()
            assertThat(state.trustedItemsCount).isEqualTo(0)
        }
    }

    @Test
    fun `a clean completed scan yields PROTECTED status`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(listOf(cleanScanResult()))
        noCleanupHistory()
        every { trustedItemRepository.observeTrustedItems() } returns flowOf(emptyList())

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(HomeUiState.Loading)
            val state = awaitItem() as HomeUiState.Loaded
            assertThat(state.protectionStatus).isEqualTo(ProtectionStatus.PROTECTED)
            assertThat(state.lastScanSummary?.isClean).isTrue()
        }
    }

    @Test
    fun `a completed scan with threats yields NEEDS_ATTENTION status and the correct threat count`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(listOf(flaggedScanResult(threatCount = 2)))
        noCleanupHistory()
        every { trustedItemRepository.observeTrustedItems() } returns flowOf(emptyList())

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(HomeUiState.Loading)
            val state = awaitItem() as HomeUiState.Loaded
            assertThat(state.protectionStatus).isEqualTo(ProtectionStatus.NEEDS_ATTENTION)
            assertThat(state.lastScanSummary?.threatsFound).isEqualTo(2)
            assertThat(state.lastScanSummary?.isClean).isFalse()
        }
    }

    @Test
    fun `trusted items count reflects the observed list size`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(emptyList())
        noCleanupHistory()
        every { trustedItemRepository.observeTrustedItems() } returns
            flowOf(listOf(trustedItem("1"), trustedItem("2"), trustedItem("3")))

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(HomeUiState.Loading)
            val state = awaitItem() as HomeUiState.Loaded
            assertThat(state.trustedItemsCount).isEqualTo(3)
        }
    }

    @Test
    fun `the most recently completed scan is used, matching observeScanHistory's most-recent-first ordering`() =
        runTest {
            val mostRecent = flaggedScanResult(threatCount = 1)
            val older = cleanScanResult()
            every { securityRepository.observeScanHistory() } returns flowOf(listOf(mostRecent, older))
            noCleanupHistory()
            every { trustedItemRepository.observeTrustedItems() } returns flowOf(emptyList())

            buildViewModel().uiState.test {
                assertThat(awaitItem()).isEqualTo(HomeUiState.Loading)
                val state = awaitItem() as HomeUiState.Loaded
                // ObserveScanHistoryUseCase's underlying query orders most-
                // recent-first (Sprint 010) — HomeViewModel must take the
                // FIRST item, not assume/re-sort.
                assertThat(state.protectionStatus).isEqualTo(ProtectionStatus.NEEDS_ATTENTION)
            }
        }

    @Test
    fun `an upstream failure surfaces as Error state, not a crash`() = runTest {
        every { securityRepository.observeScanHistory() } returns flow { throw IllegalStateException("db error") }
        noCleanupHistory()
        every { trustedItemRepository.observeTrustedItems() } returns flowOf(emptyList())

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(HomeUiState.Loading)
            val state = awaitItem()
            assertThat(state).isInstanceOf(HomeUiState.Error::class.java)
        }
    }

    // -- Cleanup history on Home (Sprint 040) -------------------------

    @Test
    fun `last cleanup is exposed when one has been recorded`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(emptyList())
        every { trustedItemRepository.observeTrustedItems() } returns flowOf(emptyList())
        every { cleanupHistoryRepository.observeHistory() } returns flowOf(listOf(cleanupRecord()))

        buildViewModel().uiState.test {
            skipItems(1)
            val loaded = awaitItem() as HomeUiState.Loaded
            assertThat(loaded.lastCleanupSummary?.bytesFreed).isEqualTo(482_000_000L)
            assertThat(loaded.lastCleanupSummary?.itemsDeleted).isEqualTo(152)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Absent, never a placeholder zero. */
    @Test
    fun `last cleanup is null when the user has never run one`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(emptyList())
        every { trustedItemRepository.observeTrustedItems() } returns flowOf(emptyList())
        noCleanupHistory()

        buildViewModel().uiState.test {
            skipItems(1)
            assertThat((awaitItem() as HomeUiState.Loaded).lastCleanupSummary).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the most recent cleanup is the one exposed`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(emptyList())
        every { trustedItemRepository.observeTrustedItems() } returns flowOf(emptyList())
        // observeHistory's contract is most-recent-first.
        every { cleanupHistoryRepository.observeHistory() } returns flowOf(
            listOf(cleanupRecord(bytesFreed = 900L), cleanupRecord(bytesFreed = 100L)),
        )

        buildViewModel().uiState.test {
            skipItems(1)
            assertThat((awaitItem() as HomeUiState.Loaded).lastCleanupSummary?.bytesFreed).isEqualTo(900L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** A cancelled cleanup really did free the bytes it reports, so it
     *  belongs in Recent Activity — carrying the flag so the screen can
     *  say it was stopped rather than implying it finished. */
    @Test
    fun `a cancelled cleanup is still surfaced and carries its flag`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(emptyList())
        every { trustedItemRepository.observeTrustedItems() } returns flowOf(emptyList())
        every { cleanupHistoryRepository.observeHistory() } returns flowOf(
            listOf(cleanupRecord(wasCancelled = true)),
        )

        buildViewModel().uiState.test {
            skipItems(1)
            val loaded = awaitItem() as HomeUiState.Loaded
            assertThat(loaded.lastCleanupSummary?.wasCancelled).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
