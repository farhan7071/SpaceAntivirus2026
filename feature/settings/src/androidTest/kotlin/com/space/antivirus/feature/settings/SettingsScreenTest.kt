package com.space.antivirus.feature.settings

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import org.junit.Rule
import org.junit.Test

/**
 * Tests the stateless SettingsScreen directly with hand-built
 * SettingsUiState, same pattern established since Sprint 017 (ADR 0030).
 * No Hilt test infrastructure needed.
 */
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setScreen(
        uiState: SettingsUiState,
        onBackgroundProtectionToggled: (Boolean) -> Unit = {},
        onIntervalSelected: (ScanInterval) -> Unit = {},
        onDismissError: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                SettingsScreen(
                    uiState = uiState,
                    onBackgroundProtectionToggled = onBackgroundProtectionToggled,
                    onIntervalSelected = onIntervalSelected,
                    onDismissError = onDismissError,
                )
            }
        }
    }

    @Test
    fun disabledState_showsTheSwitchOff_andNoIntervalCard() {
        setScreen(
            SettingsUiState.Loaded(
                backgroundProtectionEnabled = false,
                selectedInterval = ScanInterval.DAILY,
                lastScheduledAtEpochMillis = null,
            ),
        )

        composeTestRule.onNodeWithText("Background Protection").assertExists()
        composeTestRule.onNodeWithText("Off").assertExists()
        composeTestRule.onNodeWithText("Scan Interval").assertDoesNotExist()
    }

    @Test
    fun enabledState_showsTheSwitchOn_andTheIntervalCard() {
        setScreen(
            SettingsUiState.Loaded(
                backgroundProtectionEnabled = true,
                selectedInterval = ScanInterval.DAILY,
                lastScheduledAtEpochMillis = null,
            ),
        )

        composeTestRule.onNodeWithText("On").assertExists()
        composeTestRule.onNodeWithText("Scan Interval").assertExists()
        composeTestRule.onNodeWithText("Daily (selected)").assertExists()
    }

    @Test
    fun enabledState_withALastScheduledTimestamp_showsIt() {
        setScreen(
            SettingsUiState.Loaded(
                backgroundProtectionEnabled = true,
                selectedInterval = ScanInterval.DAILY,
                lastScheduledAtEpochMillis = 0L,
            ),
        )

        // The exact formatted date string is locale-dependent, so this
        // checks for the stable prefix rather than the full string.
        composeTestRule.onNodeWithText("On \u2014 last scheduled", substring = true).assertExists()
    }

    @Test
    fun theSwitch_reflectsTheEnabledState() {
        setScreen(
            SettingsUiState.Loaded(
                backgroundProtectionEnabled = true,
                selectedInterval = ScanInterval.DAILY,
                lastScheduledAtEpochMillis = null,
            ),
        )

        composeTestRule.onNodeWithTag(BACKGROUND_PROTECTION_SWITCH_TEST_TAG).assertIsOn()
    }

    @Test
    fun togglingTheSwitch_invokesTheCallbackWithTheOppositeValue() {
        var toggledTo: Boolean? = null
        setScreen(
            SettingsUiState.Loaded(
                backgroundProtectionEnabled = false,
                selectedInterval = ScanInterval.DAILY,
                lastScheduledAtEpochMillis = null,
            ),
            onBackgroundProtectionToggled = { toggledTo = it },
        )

        composeTestRule.onNodeWithTag(BACKGROUND_PROTECTION_SWITCH_TEST_TAG).performClick()

        assertThat(toggledTo).isTrue()
    }

    @Test
    fun tappingAnUnselectedInterval_invokesOnIntervalSelected() {
        var selected: ScanInterval? = null
        setScreen(
            SettingsUiState.Loaded(
                backgroundProtectionEnabled = true,
                selectedInterval = ScanInterval.DAILY,
                lastScheduledAtEpochMillis = null,
            ),
            onIntervalSelected = { selected = it },
        )

        composeTestRule.onNodeWithText("Weekly").performClick()

        assertThat(selected).isEqualTo(ScanInterval.WEEKLY)
    }

    @Test
    fun anErrorMessage_isShownWithADismissButton() {
        setScreen(
            SettingsUiState.Loaded(
                backgroundProtectionEnabled = false,
                selectedInterval = ScanInterval.DAILY,
                lastScheduledAtEpochMillis = null,
                errorMessage = "Couldn't turn on background protection. Please try again.",
            ),
        )

        composeTestRule.onNodeWithText("Couldn't turn on background protection. Please try again.").assertExists()
        composeTestRule.onNodeWithText("Dismiss").assertExists()
    }

    @Test
    fun tappingDismiss_invokesOnDismissError() {
        var dismissed = false
        setScreen(
            SettingsUiState.Loaded(
                backgroundProtectionEnabled = false,
                selectedInterval = ScanInterval.DAILY,
                lastScheduledAtEpochMillis = null,
                errorMessage = "Something went wrong.",
            ),
            onDismissError = { dismissed = true },
        )

        composeTestRule.onNodeWithText("Dismiss").performClick()

        assertThat(dismissed).isTrue()
    }

    @Test
    fun loadingState_doesNotShowAnySettingsContent() {
        setScreen(SettingsUiState.Loading)

        composeTestRule.onNodeWithText("Background Protection").assertDoesNotExist()
    }

    @Test
    fun errorState_showsTheErrorMessage() {
        setScreen(SettingsUiState.Error("Something went wrong loading your settings."))

        composeTestRule.onNodeWithText("Something went wrong loading your settings.").assertExists()
    }
}
