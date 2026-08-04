package com.space.antivirus.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.space.antivirus.core.designsystem.theme.IconTokens
import com.space.antivirus.core.designsystem.theme.LayoutTokens
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.model.AppInfo
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.AppSectionHeader
import com.space.antivirus.core.ui.component.AppTextButton
import com.space.antivirus.core.ui.component.SettingsRow
import com.space.antivirus.core.ui.component.SettingsRowControl
import java.text.DateFormat
import java.util.Date

/** A Switch has no text of its own to match on. */
const val BACKGROUND_PROTECTION_SWITCH_TEST_TAG = "background_protection_switch"

/**
 * The Settings hub — Sprint 043A.
 *
 * Sprint 026 built this as three cards with inline controls, which was
 * right for three settings and would not have survived seven sections.
 * It is now a grouped hub of `SettingsRow`s, with everything that needs
 * more than a row of its own screen.
 *
 * **Every row here is backed by real behavior.** Sprint 043's brief
 * originally proposed a Scanning section with "Scan APK files", "Scan
 * installed apps" and "Ignore trusted apps" toggles; all three were cut
 * before this sprint started, because no analyzer in this project
 * accepts anything but `ScanTarget.ApplicationTarget`, the security scan
 * requests exactly one scope, and trusted filtering is unconditional
 * inside `RunScanRequestUseCase`. Three switches persisting preferences
 * nothing reads is the definition of a settings screen that lies. What
 * survived of that section is the Ignore List, which is genuinely real.
 */
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBackgroundProtectionToggled: (Boolean) -> Unit,
    onDismissError: () -> Unit,
    onNavigateToScheduledScan: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToIgnoreList: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is SettingsUiState.Loading -> SettingsLoading(modifier)
        is SettingsUiState.Error -> AppEmptyState(
            icon = IconTokens.warning,
            title = "Couldn't load your settings",
            message = uiState.message,
            modifier = modifier.fillMaxSize(),
        )
        is SettingsUiState.Loaded -> SettingsHub(
            state = uiState,
            onBackgroundProtectionToggled = onBackgroundProtectionToggled,
            onDismissError = onDismissError,
            onNavigateToScheduledScan = onNavigateToScheduledScan,
            onNavigateToNotifications = onNavigateToNotifications,
            onNavigateToIgnoreList = onNavigateToIgnoreList,
            onNavigateToAbout = onNavigateToAbout,
            modifier = modifier,
        )
    }
}

@Composable
private fun SettingsLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppCircularProgress(progress = null)
    }
}

@Composable
private fun SettingsHub(
    state: SettingsUiState.Loaded,
    onBackgroundProtectionToggled: (Boolean) -> Unit,
    onDismissError: () -> Unit,
    onDismissError: () -> Unit,
    onNavigateToScheduledScan: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToIgnoreList: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val packageName = state.appInfo?.packageName ?: context.packageName

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutTokens.screenHorizontalPadding),
        contentPadding = PaddingValues(vertical = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        // -- Protection ------------------------------------------------
        item { AppSectionHeader(title = "Protection") }
        item {
            SettingsRow(
                title = "Background Protection",
                supportingText = protectionSupportingText(state),
                icon = IconTokens.security,
                control = SettingsRowControl.Toggle(
                    checked = state.backgroundProtectionEnabled,
                    onCheckedChange = onBackgroundProtectionToggled,
                ),
                modifier = Modifier.testTag(BACKGROUND_PROTECTION_SWITCH_TEST_TAG),
            )
        }
        item {
            SettingsRow(
                title = "Scheduled Scan",
                supportingText = if (state.backgroundProtectionEnabled) {
                    "Runs ${state.selectedInterval.label.lowercase()}"
                } else {
                    "Turn on background protection to schedule scans"
                },
                icon = IconTokens.schedule,
                control = SettingsRowControl.Value(state.selectedInterval.label),
                enabled = state.backgroundProtectionEnabled,
                onClick = onNavigateToScheduledScan,
            )
        }
        // Shown only when the standard allowlist actually reports this
        // app as restricted — a card about a problem the user doesn't
        // have is noise.
        if (!state.isIgnoringBatteryOptimizations) {
            item {
                SettingsRow(
                    title = "Battery optimisation is on",
                    supportingText = "Android may delay scheduled scans to save battery. " +
                        "Allowing unrestricted usage makes them more likely to run on time. " +
                        "This is optional \u2014 protection works either way.",
                    icon = IconTokens.battery,
                    control = SettingsRowControl.Navigate,
                    onClick = { SettingsIntents.openBatteryOptimizationSettings(context) },
                )
            }
        }

        // -- Notifications ---------------------------------------------
        item { AppSectionHeader(title = "Notifications") }
        item {
            SettingsRow(
                title = "Notification Settings",
                supportingText = if (state.notifyAfterScan) {
                    "You'll be told when an automatic scan finishes"
                } else {
                    "Automatic scans run quietly"
                },
                icon = IconTokens.notifications,
                control = SettingsRowControl.Navigate,
                onClick = onNavigateToNotifications,
            )
        }

        // -- Scanning ---------------------------------------------------
        item { AppSectionHeader(title = "Scanning") }
        item {
            SettingsRow(
                title = "Ignore List",
                supportingText = "Apps you've chosen to skip during scans",
                icon = IconTokens.trusted,
                control = SettingsRowControl.Navigate,
                onClick = onNavigateToIgnoreList,
            )
        }

        // -- Privacy -----------------------------------------------------
        item { AppSectionHeader(title = "Privacy") }
        item {
            SettingsRow(
                title = "Privacy Policy",
                supportingText = policySupportingText(),
                icon = IconTokens.privacy,
                control = SettingsRowControl.Navigate,
                onClick = { SettingsIntents.openUrl(context, SupportLinks.PRIVACY_POLICY_URL) },
            )
        }
        item {
            SettingsRow(
                title = "Terms of Service",
                supportingText = policySupportingText(),
                icon = IconTokens.document,
                control = SettingsRowControl.Navigate,
                onClick = { SettingsIntents.openUrl(context, SupportLinks.TERMS_OF_SERVICE_URL) },
            )
        }

        // -- Support -----------------------------------------------------
        item { AppSectionHeader(title = "Support") }
        item {
            SettingsRow(
                title = "Rate App",
                supportingText = "Leave a review on Google Play",
                icon = IconTokens.rate,
                control = SettingsRowControl.Navigate,
                onClick = { SettingsIntents.openPlayStoreListing(context, packageName) },
            )
        }
        item {
            SettingsRow(
                title = "Share App",
                supportingText = "Tell someone about Space Antivirus",
                icon = IconTokens.share,
                control = SettingsRowControl.Navigate,
                onClick = { SettingsIntents.shareApp(context, packageName) },
            )
        }
        item {
            SettingsRow(
                title = "Send Feedback",
                supportingText = "Email us about a problem or an idea",
                icon = IconTokens.feedback,
                control = SettingsRowControl.Navigate,
                onClick = {
                    SettingsIntents.sendFeedback(context, appInfoLine = state.appInfo.supportLine())
                },
            )
        }

        // -- About --------------------------------------------------------
        item { AppSectionHeader(title = "About") }
        item {
            SettingsRow(
                title = "About Space Antivirus",
                supportingText = state.appInfo?.let { "Version ${it.versionName}" },
                icon = IconTokens.recommendation,
                control = SettingsRowControl.Navigate,
                onClick = onNavigateToAbout,
            )
        }

        // Transient action errors clear on the next attempt anyway, but
        // an explicit dismiss is kept from Sprint 026 — an error the user
        // cannot acknowledge is one they have to work around.
        state.errorMessage?.let { message ->
            item {
                Column(modifier = Modifier.padding(top = spacing.small)) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    AppTextButton(text = "Dismiss", onClick = onDismissError)
                }
            }
        }
    }
}

/**
 * The Protection row's supporting line. Reports the real last-scheduled
 * timestamp when there is one and says nothing about scheduling when
 * there isn't, rather than showing "Never" against a value that was
 * never recorded.
 */
private fun protectionSupportingText(state: SettingsUiState.Loaded): String = when {
    !state.backgroundProtectionEnabled ->
        "Off \u2014 turn on to scan automatically even when the app is closed"
    state.lastScheduledAtEpochMillis != null ->
        "On \u2014 scheduled " +
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(Date(state.lastScheduledAtEpochMillis))
    else -> "On \u2014 scans run automatically even when the app is closed"
}

/**
 * Says so plainly while the policy URLs are still placeholders. An
 * unconfigured link that looks configured is how a 404 reaches a user —
 * or a Play reviewer, who checks the privacy policy link on every
 * submission.
 */
private fun policySupportingText(): String? =
    if (SupportLinks.arePolicyLinksConfigured) null else "Not published yet"

/** Version details so a support reply doesn't open by asking which build
 *  the user is on. */
private fun AppInfo?.supportLine(): String =
    this?.let { "Space Antivirus ${it.versionName} (${it.versionCode})" } ?: "Space Antivirus"
