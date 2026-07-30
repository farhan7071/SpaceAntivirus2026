package com.space.antivirus.feature.security

import android.util.Log
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AnalyzerId
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
import com.space.antivirus.domain.reporting.ThreatDescriptionProvider
import com.space.antivirus.domain.repository.SecurityRepository
import com.space.antivirus.domain.repository.TrustedItemRepository
import com.space.antivirus.domain.usecase.AddTrustedItemUseCase
import com.space.antivirus.domain.usecase.ObserveScanHistoryUseCase
import com.space.antivirus.domain.usecase.ObserveTrustedItemsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Same proportionate testing choice as HomeViewModelTest (ADR 0030):
 * mockk on SecurityRepository, stubbed only for the one method actually
 * called, fed into a real ObserveScanHistoryUseCase — not a hand-written
 * local Fake covering all 14 SecurityRepository methods this ViewModel
 * never touches.
 *
 * Sprint 030: descriptionProvider is mocked for shortSummaryFor and the
 * new three-argument recommendationFor. addTrustedItem is also mocked —
 * built from a real AddTrustedItemUseCase fed a mocked TrustedItemRepository,
 * the same "mock the repository, use the real UseCase" pattern as
 * securityRepository above, not a hand-rolled fake UseCase.
 */
class SecurityCenterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val securityRepository = mockk<SecurityRepository>()
    private val descriptionProvider = mockk<ThreatDescriptionProvider>()
    private val trustedItemRepository = mockk<TrustedItemRepository>()

    // DIAGNOSTIC (Sprint 32.1) — temporary, remove together with the
    // Log.d() calls this mocks for. android.util.Log throws
    // "not mocked" when actually invoked on the plain JVM (this test
    // runs here, not on a real Android runtime, and this project has no
    // testOptions.unitTests.isReturnDefaultValues configured anywhere) —
    // SecurityCenterViewModel.onIgnoreClick and its uiState mapping both
    // now call Log.d() directly, so every test below that exercises
    // either would otherwise fail on an unrelated, unmocked platform
    // call rather than the behavior actually being tested.
    @Before
    fun mockAndroidLog() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @After
    fun unmockAndroidLog() {
        unmockkStatic(Log::class)
    }

    private fun buildViewModel(): SecurityCenterViewModel {
        every { descriptionProvider.shortSummaryFor(any()) } returns "test short summary"
        every { descriptionProvider.recommendationFor(any(), any(), any()) } returns "test recommendation"
        every { descriptionProvider.categoryFor(any()) } returns "test category"
        every { descriptionProvider.confidenceLevelFor(any(), any()) } returns "test confidence level"
        // Default: no trusted items, matching every existing test's
        // prior behavior exactly. Sprint 32.1's own tests override this
        // per-test where a trusted item needs to actually be present.
        every { trustedItemRepository.observeTrustedItems() } returns flowOf(emptyList())
        val addTrustedItem = AddTrustedItemUseCase(trustedItemRepository, StandardTestDispatcher())
        return SecurityCenterViewModel(
            ObserveScanHistoryUseCase(securityRepository),
            ObserveTrustedItemsUseCase(trustedItemRepository),
            descriptionProvider,
            addTrustedItem,
        )
    }

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

    private fun threat(
        id: String,
        appLabel: String = "Example App $id",
        packageName: String = "com.example.app.$id",
        evidenceDescription: String = "test evidence $id",
        riskLevel: RiskLevel = RiskLevel.ATTENTION,
    ) = Threat(
        id = id,
        targetIdentifier = packageName,
        threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
        riskLevel = riskLevel,
        title = "Unusual permission combination",
        description = "legacy description $id",
        detections = listOf(
            Detection(
                id = "$id-d1",
                analyzerId = AnalyzerId("test"),
                threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                evidenceDescription = evidenceDescription,
                riskLevel = riskLevel,
            ),
        ),
        discoveredAtEpochMillis = 2_000L,
        appLabel = appLabel,
    )

    private fun flaggedScanResult(
        threats: List<Threat>,
        itemsScanned: Int = 10,
        itemsTrusted: Int = 0,
        durationMillis: Long = 500,
    ) = ScanResult(
        session = completedSession(),
        statistics = ScanStatistics(
            itemsScanned = itemsScanned,
            threatsFound = threats.size,
            itemsInconclusive = 0,
            itemsTrusted = itemsTrusted,
            durationMillis = durationMillis,
        ),
        threats = threats,
    )

    @Test
    fun `no scan history yields UNKNOWN status and an empty threat list`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(emptyList())

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val state = awaitItem() as SecurityCenterUiState.Loaded
            assertThat(state.protectionStatus).isEqualTo(ProtectionStatus.UNKNOWN)
            assertThat(state.threats).isEmpty()
        }
    }

    @Test
    fun `a clean completed scan yields PROTECTED status and an empty threat list`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(listOf(cleanScanResult()))

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val state = awaitItem() as SecurityCenterUiState.Loaded
            assertThat(state.protectionStatus).isEqualTo(ProtectionStatus.PROTECTED)
            assertThat(state.threats).isEmpty()
        }
    }

    @Test
    fun `a scan with threats yields NEEDS_ATTENTION and one ThreatSummary per app, identified by appLabel`() =
        runTest {
            val threats = listOf(
                threat("t1", appLabel = "Chrome"),
                threat("t2", appLabel = "WhatsApp"),
            )
            every { securityRepository.observeScanHistory() } returns flowOf(listOf(flaggedScanResult(threats)))

            buildViewModel().uiState.test {
                assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
                val state = awaitItem() as SecurityCenterUiState.Loaded
                assertThat(state.protectionStatus).isEqualTo(ProtectionStatus.NEEDS_ATTENTION)
                assertThat(state.threats).hasSize(2)
                // Sprint 029 root-cause fix, verified at the ViewModel
                // layer: two DIFFERENT apps are distinguishable by name,
                // not indistinguishable behind the same generic title.
                assertThat(state.threats.map { it.appLabel }).containsExactly("Chrome", "WhatsApp")
            }
        }

    @Test
    fun `each ThreatSummary carries all its fields correctly, including threatCategory and confidenceLabel`() =
        runTest {
            every { descriptionProvider.shortSummaryFor(any()) } returns "Can access SMS and internet."
            every {
                descriptionProvider.recommendationFor(
                    ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                    any(),
                    RiskLevel.ATTENTION,
                )
            } returns "Review if unexpected."
            every {
                descriptionProvider.categoryFor(ThreatType.SUSPICIOUS_PERMISSION_USAGE)
            } returns "Permission Usage"
            every {
                descriptionProvider.confidenceLevelFor(RiskLevel.ATTENTION, any())
            } returns "Medium"
            val realThreat = threat(
                "t1",
                appLabel = "Suspicious App",
                packageName = "com.example.suspicious",
                evidenceDescription = "SMS access with INTERNET access",
                riskLevel = RiskLevel.ATTENTION,
            )
            every { securityRepository.observeScanHistory() } returns
                flowOf(listOf(flaggedScanResult(listOf(realThreat))))

            buildViewModel().uiState.test {
                assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
                val state = awaitItem() as SecurityCenterUiState.Loaded
                val summary = state.threats.single()
                assertThat(summary.appLabel).isEqualTo("Suspicious App")
                assertThat(summary.packageName).isEqualTo("com.example.suspicious")
                assertThat(summary.riskLevel).isEqualTo(RiskLevel.ATTENTION)
                assertThat(summary.threatCategory).isEqualTo("Permission Usage")
                assertThat(summary.shortSummary).isEqualTo("Can access SMS and internet.")
                assertThat(summary.technicalDetail).isEqualTo("legacy description t1")
                assertThat(summary.evidenceBullets).containsExactly("SMS access with INTERNET access")
                assertThat(summary.recommendation).isEqualTo("Review if unexpected.")
                assertThat(summary.confidenceLabel).isEqualTo("Medium")
            }
        }

    @Test
    fun `a Threat with multiple Detections maps to multiple evidenceBullets, one per Detection`() = runTest {
        val multiDetectionThreat = Threat(
            id = "t1",
            targetIdentifier = "com.example.app",
            threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
            riskLevel = RiskLevel.ACTION_NEEDED,
            title = "Unusual permission combination",
            description = "legacy",
            detections = listOf(
                Detection(
                    id = "d1",
                    analyzerId = AnalyzerId("overlay-permission-pattern"),
                    threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                    evidenceDescription = "Overlay reason",
                    riskLevel = RiskLevel.ATTENTION,
                ),
                Detection(
                    id = "d2",
                    analyzerId = AnalyzerId("surveillance-permission-combination"),
                    threatType = ThreatType.SUSPICIOUS_PERMISSION_USAGE,
                    evidenceDescription = "Surveillance reason",
                    riskLevel = RiskLevel.ATTENTION,
                ),
            ),
            discoveredAtEpochMillis = 2_000L,
            appLabel = "Example App",
        )
        every { securityRepository.observeScanHistory() } returns
            flowOf(listOf(flaggedScanResult(listOf(multiDetectionThreat))))

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val state = awaitItem() as SecurityCenterUiState.Loaded
            val summary = state.threats.single()
            assertThat(summary.evidenceBullets).containsExactly("Overlay reason", "Surveillance reason")
        }
    }

    @Test
    fun `an empty appLabel falls back to the package name, defensively`() = runTest {
        val threatWithBlankLabel = threat("t1", appLabel = "", packageName = "com.example.blank")
        every { securityRepository.observeScanHistory() } returns
            flowOf(listOf(flaggedScanResult(listOf(threatWithBlankLabel))))

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val state = awaitItem() as SecurityCenterUiState.Loaded
            assertThat(state.threats.single().appLabel).isEqualTo("com.example.blank")
        }
    }

    @Test
    fun `the most recently completed scan is used, matching observeScanHistory's most-recent-first ordering`() =
        runTest {
            val mostRecent = flaggedScanResult(listOf(threat("t1")))
            val older = cleanScanResult()
            every { securityRepository.observeScanHistory() } returns flowOf(listOf(mostRecent, older))

            buildViewModel().uiState.test {
                assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
                val state = awaitItem() as SecurityCenterUiState.Loaded
                assertThat(state.protectionStatus).isEqualTo(ProtectionStatus.NEEDS_ATTENTION)
                assertThat(state.threats).hasSize(1)
            }
        }

    @Test
    fun `an upstream failure surfaces as Error state, not a crash`() = runTest {
        every { securityRepository.observeScanHistory() } returns flow { throw IllegalStateException("db error") }

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val state = awaitItem()
            assertThat(state).isInstanceOf(SecurityCenterUiState.Error::class.java)
        }
    }

    // --- Sprint 32.1 hotfix: a trusted item's threat disappears from the visible list ---

    @Test
    fun `a threat whose package is already trusted is filtered out of the visible list`() = runTest {
        val threats = listOf(threat("t1", packageName = "com.example.trusted"))
        every { securityRepository.observeScanHistory() } returns flowOf(listOf(flaggedScanResult(threats)))
        every { trustedItemRepository.observeTrustedItems() } returns flowOf(
            listOf(
                TrustedItem(
                    id = "trust-1",
                    identifier = "com.example.trusted",
                    type = TrustedItemType.APPLICATION,
                    addedAtEpochMillis = 0L,
                ),
            ),
        )

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val state = awaitItem() as SecurityCenterUiState.Loaded
            // The real root cause this hotfix addresses: before this
            // fix, uiState had no way to know a trusted item existed at
            // all, and this threat would still be visible here.
            assertThat(state.threats).isEmpty()
        }
    }

    @Test
    fun `when the only threat is trusted, protection status reads PROTECTED, not NEEDS_ATTENTION`() = runTest {
        val threats = listOf(threat("t1", packageName = "com.example.trusted"))
        every { securityRepository.observeScanHistory() } returns flowOf(listOf(flaggedScanResult(threats)))
        every { trustedItemRepository.observeTrustedItems() } returns flowOf(
            listOf(
                TrustedItem(
                    id = "trust-1",
                    identifier = "com.example.trusted",
                    type = TrustedItemType.APPLICATION,
                    addedAtEpochMillis = 0L,
                ),
            ),
        )

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val state = awaitItem() as SecurityCenterUiState.Loaded
            assertThat(state.protectionStatus).isEqualTo(ProtectionStatus.PROTECTED)
        }
    }

    @Test
    fun `trusting one of two threats leaves the other one visible, protection status still NEEDS_ATTENTION`() =
        runTest {
            val threats = listOf(
                threat("t1", packageName = "com.example.trusted"),
                threat("t2", packageName = "com.example.stillsuspicious"),
            )
            every { securityRepository.observeScanHistory() } returns flowOf(listOf(flaggedScanResult(threats)))
            every { trustedItemRepository.observeTrustedItems() } returns flowOf(
                listOf(
                    TrustedItem(
                        id = "trust-1",
                        identifier = "com.example.trusted",
                        type = TrustedItemType.APPLICATION,
                        addedAtEpochMillis = 0L,
                    ),
                ),
            )

            buildViewModel().uiState.test {
                assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
                val state = awaitItem() as SecurityCenterUiState.Loaded
                assertThat(state.threats).hasSize(1)
                assertThat(state.threats.single().packageName).isEqualTo("com.example.stillsuspicious")
                assertThat(state.protectionStatus).isEqualTo(ProtectionStatus.NEEDS_ATTENTION)
            }
        }

    // --- onIgnoreClick: Sprint 030, the real "Ignore" action ---

    @Test
    fun `onIgnoreClick adds the package as a trusted APPLICATION item`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(emptyList())
        coEvery { trustedItemRepository.addTrustedItem(any(), any(), any()) } returns
            AppResult.Success(
                TrustedItem(
                    id = "generated",
                    identifier = "com.example.ignored",
                    type = TrustedItemType.APPLICATION,
                    addedAtEpochMillis = 0L,
                ),
            )
        val viewModel = buildViewModel()

        viewModel.onIgnoreClick("com.example.ignored")
        runCurrent()

        coVerify(exactly = 1) {
            trustedItemRepository.addTrustedItem("com.example.ignored", TrustedItemType.APPLICATION, any())
        }
    }

    @Test
    fun `onIgnoreClick does not crash the ViewModel if the underlying repository call fails`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(emptyList())
        coEvery { trustedItemRepository.addTrustedItem(any(), any(), any()) } returns
            AppResult.Failure(AppError.Unexpected(null))
        val viewModel = buildViewModel()

        viewModel.onIgnoreClick("com.example.ignored")
        runCurrent()

        // Fire-and-forget by design (see the ViewModel's own KDoc) — a
        // failure here should not throw or crash the calling coroutine.
        // Reaching this line at all is the assertion.
    }

    // --- Sprint 033, Part 4: scanSummary ---

    @Test
    fun `scanSummary is null when there is no scan yet`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(emptyList())

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val state = awaitItem() as SecurityCenterUiState.Loaded
            assertThat(state.scanSummary).isNull()
        }
    }

    @Test
    fun `scanSummary's appsScanned, trustedApps, and scanDurationMillis come directly from ScanStatistics`() =
        runTest {
            every { securityRepository.observeScanHistory() } returns flowOf(
                listOf(flaggedScanResult(emptyList(), itemsScanned = 42, itemsTrusted = 3, durationMillis = 1_234L)),
            )

            buildViewModel().uiState.test {
                assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
                val summary = (awaitItem() as SecurityCenterUiState.Loaded).scanSummary!!
                assertThat(summary.appsScanned).isEqualTo(42)
                assertThat(summary.trustedApps).isEqualTo(3)
                assertThat(summary.scanDurationMillis).isEqualTo(1_234L)
            }
        }

    @Test
    fun `threatsDetected counts only visible threats, not ones already trusted`() = runTest {
        val threats = listOf(
            threat("t1", packageName = "com.example.visible"),
            threat("t2", packageName = "com.example.trusted"),
        )
        every { securityRepository.observeScanHistory() } returns flowOf(listOf(flaggedScanResult(threats)))
        every { trustedItemRepository.observeTrustedItems() } returns flowOf(
            listOf(
                TrustedItem(
                    id = "trust-1",
                    identifier = "com.example.trusted",
                    type = TrustedItemType.APPLICATION,
                    addedAtEpochMillis = 0L,
                ),
            ),
        )

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val summary = (awaitItem() as SecurityCenterUiState.Loaded).scanSummary!!
            assertThat(summary.threatsDetected).isEqualTo(1)
        }
    }

    @Test
    fun `ignoredThreats counts threats trusted after the fact, distinct from trustedApps`() = runTest {
        // Directly the distinction ADR 0045/scanSummaryFor's own KDoc
        // makes: itemsTrusted (apps skipped from analysis, from
        // ScanStatistics) and ignoredThreats (threats this scan actually
        // found that have since been marked trusted) are different
        // numbers, even in the same scan.
        val threats = listOf(threat("t1", packageName = "com.example.trusted"))
        every { securityRepository.observeScanHistory() } returns flowOf(
            listOf(flaggedScanResult(threats, itemsTrusted = 0)),
        )
        every { trustedItemRepository.observeTrustedItems() } returns flowOf(
            listOf(
                TrustedItem(
                    id = "trust-1",
                    identifier = "com.example.trusted",
                    type = TrustedItemType.APPLICATION,
                    addedAtEpochMillis = 0L,
                ),
            ),
        )

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val summary = (awaitItem() as SecurityCenterUiState.Loaded).scanSummary!!
            assertThat(summary.ignoredThreats).isEqualTo(1)
            assertThat(summary.trustedApps).isEqualTo(0)
        }
    }

    @Test
    fun `highestThreatLabel is None when there are no visible threats`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(listOf(flaggedScanResult(emptyList())))

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val summary = (awaitItem() as SecurityCenterUiState.Loaded).scanSummary!!
            assertThat(summary.highestThreatLabel).isEqualTo("None")
        }
    }

    @Test
    fun `highestThreatLabel reflects the most severe visible threat, not the first or last one`() = runTest {
        val threats = listOf(
            threat("t1", packageName = "com.example.low", riskLevel = RiskLevel.INFO),
            threat("t2", packageName = "com.example.high", riskLevel = RiskLevel.ACTION_NEEDED),
            threat("t3", packageName = "com.example.mid", riskLevel = RiskLevel.ATTENTION),
        )
        every { securityRepository.observeScanHistory() } returns flowOf(listOf(flaggedScanResult(threats)))

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val summary = (awaitItem() as SecurityCenterUiState.Loaded).scanSummary!!
            assertThat(summary.highestThreatLabel).isEqualTo("Action Needed")
        }
    }

    @Test
    fun `averageConfidenceLabel is None when there are no visible threats`() = runTest {
        every { securityRepository.observeScanHistory() } returns flowOf(listOf(flaggedScanResult(emptyList())))

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val summary = (awaitItem() as SecurityCenterUiState.Loaded).scanSummary!!
            assertThat(summary.averageConfidenceLabel).isEqualTo("None")
        }
    }

    @Test
    fun `averageConfidenceLabel rounds to the nearest tier across multiple visible threats`() = runTest {
        // Low (ordinal 0) and High (ordinal 2) average to 1.0, which
        // rounds to Medium (ordinal 1) - a genuinely computed average,
        // not just one of the two input tiers.
        every {
            descriptionProvider.confidenceLevelFor(RiskLevel.INFO, any())
        } returns "Low"
        every {
            descriptionProvider.confidenceLevelFor(RiskLevel.ACTION_NEEDED, any())
        } returns "High"
        val threats = listOf(
            threat("t1", packageName = "com.example.low", riskLevel = RiskLevel.INFO),
            threat("t2", packageName = "com.example.high", riskLevel = RiskLevel.ACTION_NEEDED),
        )
        every { securityRepository.observeScanHistory() } returns flowOf(listOf(flaggedScanResult(threats)))

        buildViewModel().uiState.test {
            assertThat(awaitItem()).isEqualTo(SecurityCenterUiState.Loading)
            val summary = (awaitItem() as SecurityCenterUiState.Loaded).scanSummary!!
            assertThat(summary.averageConfidenceLabel).isEqualTo("Medium")
        }
    }
}
