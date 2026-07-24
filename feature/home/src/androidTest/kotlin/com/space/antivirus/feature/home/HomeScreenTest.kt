package com.space.antivirus.feature.home

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import org.junit.Rule
import org.junit.Test

/**
 * This project's first Compose UI test — establishes the pattern every
 * remaining feature screen follows: test the STATELESS composable
 * (HomeScreen) directly with a hand-built HomeUiState, never HomeRoute
 * (which needs a real or fake Hilt graph to construct HomeViewModel).
 * No Hilt/androidTest DI infrastructure needed for this reason — plain
 * createComposeRule() is sufficient. Wrapped in SpaceAntivirusTheme to
 * match how HomeRoute is actually rendered in the real app (the NavHost
 * is always theme-wrapped at the Activity level), not because any
 * component here would crash without it — LocalSpacing and MaterialTheme
 * both have built-in defaults.
 */
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingState_showsTheLoadingIndicator() {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                HomeScreen(uiState = HomeUiState.Loading)
            }
        }

        composeTestRule.onNodeWithTag(HOME_LOADING_TEST_TAG).assertExists()
    }

    @Test
    fun loadedState_withNoScanHistory_showsUnknownStatusAndNoScansYet() {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                HomeScreen(
                    uiState = HomeUiState.Loaded(
                        protectionStatus = ProtectionStatus.UNKNOWN,
                        lastScanSummary = null,
                        trustedItemsCount = 0,
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Protection status unknown").assertExists()
        composeTestRule.onNodeWithText("No scans yet").assertExists()
        composeTestRule.onNodeWithText("Trusted Items").assertExists()
    }

    @Test
    fun loadedState_protected_showsThePositiveStatusMessage() {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                HomeScreen(
                    uiState = HomeUiState.Loaded(
                        protectionStatus = ProtectionStatus.PROTECTED,
                        lastScanSummary = LastScanSummary(
                            isClean = true,
                            threatsFound = 0,
                            scannedAtEpochMillis = 0L,
                        ),
                        trustedItemsCount = 0,
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("You're protected").assertExists()
    }

    @Test
    fun loadedState_needsAttention_showsTheAttentionMessage() {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                HomeScreen(
                    uiState = HomeUiState.Loaded(
                        protectionStatus = ProtectionStatus.NEEDS_ATTENTION,
                        lastScanSummary = LastScanSummary(
                            isClean = false,
                            threatsFound = 2,
                            scannedAtEpochMillis = 0L,
                        ),
                        trustedItemsCount = 0,
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Attention needed").assertExists()
    }

    @Test
    fun loadedState_trustedItemsCount_isReflectedInTheCardText() {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                HomeScreen(
                    uiState = HomeUiState.Loaded(
                        protectionStatus = ProtectionStatus.UNKNOWN,
                        lastScanSummary = null,
                        trustedItemsCount = 5,
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("5 items trusted").assertExists()
    }

    @Test
    fun loadedState_scanNowButton_isDisabled() {
        // Scan execution is explicitly out of scope for this sprint — the
        // button must exist as the "entry point for a future scan action"
        // this sprint's brief asked for, but disabled, not functional yet.
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                HomeScreen(
                    uiState = HomeUiState.Loaded(
                        protectionStatus = ProtectionStatus.UNKNOWN,
                        lastScanSummary = null,
                        trustedItemsCount = 0,
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("Scan Now").assertIsNotEnabled()
    }

    @Test
    fun errorState_showsTheErrorMessage() {
        composeTestRule.setContent {
            SpaceAntivirusTheme {
                HomeScreen(uiState = HomeUiState.Error("Something went wrong"))
            }
        }

        composeTestRule.onNodeWithText("Something went wrong").assertExists()
    }
}
