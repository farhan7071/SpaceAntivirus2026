package com.space.antivirus.feature.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import com.space.antivirus.core.model.AppInfo
import org.junit.Rule
import org.junit.Test

/**
 * Sprint 043A rewrote this suite for the grouped hub.
 *
 * Several assertions here exist to lock in the *absence* of controls
 * that back nothing — the three scanning toggles the original Sprint 043
 * brief proposed, which were cut because no analyzer accepts a file
 * target, the security scan requests exactly one scope, and trusted
 * filtering is unconditional in the scan engine. They should be deleted
 * only alongside the real capability, not to make a screen look fuller.
 */
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun loadedState(
        backgroundProtectionEnabled: Boolean = false,
        selectedInterval: ScanInterval = ScanInterval.DAILY,
        lastScheduledAtEpochMillis: Long? = null,
        notifyAfterScan: Boolean = false,
        isIgnoringBatteryOptimizations: Boolean = true,
        errorMessage: String? = null,
    ) = SettingsUiState.Loaded(
        backgroundProtectionEnabled = backgroundProtectionEnabled,
        selectedInterval = selectedInterval,
        lastScheduledAtEpochMillis = lastScheduledAtEpochMillis,
        notifyAfterScan = notifyAfterScan,
        isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
        appInfo = AppInfo(
            versionName = "1.4.0",
            versionCode = 42L,
            packageName = "com.space.antivirus",
            isDebugBuild = false,
        ),
        errorMessage = errorMessage,
    )

    private fun setHub(
        uiState: SettingsUiState = loadedState(),
        onBackgroundProtectionToggled: (Boolean) -> Unit = {},
        onDismissError: () -> Unit = {},
        onNavigateToScheduledScan: () -> Unit = {},
        onNavigateToIgnoreList: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                SettingsScreen(
                    uiState = uiState,
                    onBackgroundProtectionToggled = onBackgroundProtectionToggled,
                    onDismissError = onDismissError,
                    onNavigateToScheduledScan = onNavigateToScheduledScan,
                    onNavigateToNotifications = {},
                    onNavigateToIgnoreList = onNavigateToIgnoreList,
                    onNavigateToAbout = {},
                )
            }
        }
    }

    // -- Structure -----------------------------------------------------

    @Test
    fun hub_showsItsSectionHeadersAndRows() {
        setHub()

        composeTestRule.onNodeWithText("Protection").assertExists()
        composeTestRule.onNodeWithText("Background Protection").assertExists()
        composeTestRule.onNodeWithText("Scheduled Scan").assertExists()
        composeTestRule.onNodeWithText("Notifications").assertExists()
        composeTestRule.onNodeWithText("Ignore List").assertExists()
    }

    /**
     * Cut before this sprint began: no analyzer in this project accepts
     * anything but ScanTarget.ApplicationTarget, the security scan
     * requests exactly one scope, and trusted filtering is unconditional
     * inside RunScanRequestUseCase. Three switches persisting
     * preferences nothing reads would be a settings screen that lies.
     */
    @Test
    fun hub_doesNotOfferControlsThatBackNothing() {
        setHub()

        composeTestRule.onNodeWithText("Scan APK Files").assertDoesNotExist()
        composeTestRule.onNodeWithText("Scan Installed Apps").assertDoesNotExist()
        composeTestRule.onNodeWithText("Ignore Trusted Apps").assertDoesNotExist()
    }

    // -- Protection ----------------------------------------------------

    @Test
    fun protectionRow_describesTheOffState() {
        setHub(loadedState(backgroundProtectionEnabled = false))

        composeTestRule
            .onNodeWithText("Off \u2014 turn on to scan automatically even when the app is closed")
            .assertExists()
    }

    @Test
    fun protectionRow_showsTheRealLastScheduledTimeWhenThereIsOne() {
        setHub(
            loadedState(backgroundProtectionEnabled = true, lastScheduledAtEpochMillis = 1_700_000_000_000L),
        )

        composeTestRule.onNodeWithText("On \u2014 scheduled", substring = true).assertExists()
    }

    /** Never "Never" against a value that was never recorded. */
    @Test
    fun protectionRow_saysNothingAboutSchedulingWhenNothingWasRecorded() {
        setHub(loadedState(backgroundProtectionEnabled = true, lastScheduledAtEpochMillis = null))

        composeTestRule
            .onNodeWithText("On \u2014 scans run automatically even when the app is closed")
            .assertExists()
    }

    @Test
    fun protectionSwitch_forwardsToggles() {
        var toggled: Boolean? = null
        setHub(onBackgroundProtectionToggled = { toggled = it })

        composeTestRule.onNodeWithTag(BACKGROUND_PROTECTION_SWITCH_TEST_TAG).performClick()

        assertThat(toggled).isTrue()
    }

    // -- Scheduled scan row --------------------------------------------

    @Test
    fun scheduledScanRow_isDisabledWhileProtectionIsOff() {
        setHub(loadedState(backgroundProtectionEnabled = false))

        composeTestRule
            .onNodeWithText("Turn on background protection to schedule scans")
            .assertExists()
    }

    @Test
    fun scheduledScanRow_navigatesWhenProtectionIsOn() {
        var navigated = false
        setHub(
            uiState = loadedState(backgroundProtectionEnabled = true, selectedInterval = ScanInterval.WEEKLY),
            onNavigateToScheduledScan = { navigated = true },
        )

        composeTestRule.onNodeWithText("Runs weekly").performClick()

        assertThat(navigated).isTrue()
    }

    // -- Battery -------------------------------------------------------

    /** A card about a problem the user doesn't have is noise. */
    @Test
    fun batteryRow_isHiddenWhenTheAppIsAlreadyUnrestricted() {
        setHub(loadedState(isIgnoringBatteryOptimizations = true))

        composeTestRule.onNodeWithText("Battery optimisation is on").assertDoesNotExist()
    }

    @Test
    fun batteryRow_appearsWhenTheAppIsRestricted() {
        setHub(loadedState(isIgnoringBatteryOptimizations = false))

        composeTestRule.onNodeWithText("Battery optimisation is on").assertExists()
    }

    // -- Privacy -------------------------------------------------------

    /**
     * The policy URLs are still placeholders, and the hub says so rather
     * than looking finished and 404-ing in front of a user or a Play
     * reviewer. Delete this test once SupportLinks holds real URLs.
     */
    @Test
    fun privacyRows_sayPlainlyThatTheLinksAreNotPublishedYet() {
        setHub()

        composeTestRule.onNodeWithText("Privacy Policy").assertExists()
        composeTestRule.onNodeWithText("Terms of Service").assertExists()
        if (!SupportLinks.arePolicyLinksConfigured) {
            // Both privacy rows carry the same supporting line, so the
            // default single-node matcher would fail on ambiguity.
            composeTestRule.onAllNodesWithText("Not published yet").assertCountEquals(2)
        }
    }

    // -- Errors --------------------------------------------------------

    @Test
    fun errorMessage_isShownAndDismissible() {
        var dismissed = false
        setHub(
            uiState = loadedState(errorMessage = "Couldn't turn on background protection."),
            onDismissError = { dismissed = true },
        )

        composeTestRule.onNodeWithText("Couldn't turn on background protection.").assertExists()
        composeTestRule.onNodeWithText("Dismiss").performClick()

        assertThat(dismissed).isTrue()
    }

    @Test
    fun loadingState_showsNoRows() {
        setHub(SettingsUiState.Loading)

        composeTestRule.onNodeWithText("Background Protection").assertDoesNotExist()
    }

    @Test
    fun errorState_showsTheMessage() {
        setHub(SettingsUiState.Error("Something went wrong loading your settings."))

        composeTestRule.onNodeWithText("Something went wrong loading your settings.").assertExists()
    }
}
