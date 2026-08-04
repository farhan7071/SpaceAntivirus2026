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
import androidx.compose.ui.text.style.TextAlign
import com.space.antivirus.core.designsystem.theme.IconTokens
import com.space.antivirus.core.designsystem.theme.LayoutTokens
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.model.AppInfo
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.AppSectionHeader
import com.space.antivirus.core.ui.component.SettingsRow
import com.space.antivirus.core.ui.component.SettingsRowControl

/**
 * About — Sprint 043A.
 *
 * **Three things this screen deliberately does not show**, all of which
 * the original Sprint 043 brief asked for and Sprint 043A's own brief
 * then correctly excluded:
 *
 * - *Signature database version* and *last signature update*. This
 *   project has never shipped a signature database — no signature
 *   matching, no cloud lookups, standing rule since Sprint 002. Both
 *   fields would have nothing behind them, and on an antivirus app's
 *   About screen they would imply a detection model this app does not
 *   use.
 * - *Open source licenses*. Doing this honestly needs the OSS-licenses
 *   Gradle plugin and its generated artifact, neither of which is in
 *   this project. A hand-written list would go stale the first time a
 *   dependency changed, and a wrong attribution list is worse than none.
 *
 * Everything shown comes from the installed package via
 * `AppInfoProvider`, not from a module's generated BuildConfig — a
 * feature module's BuildConfig describes that module, not the app the
 * user installed.
 */
@Composable
fun AboutScreen(uiState: SettingsUiState, modifier: Modifier = Modifier) {
    when (uiState) {
        is SettingsUiState.Loading -> Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppCircularProgress(progress = null)
        }
        is SettingsUiState.Error -> AppEmptyState(
            icon = IconTokens.warning,
            title = "Couldn't load your settings",
            message = uiState.message,
            modifier = modifier.fillMaxSize(),
        )
        is SettingsUiState.Loaded -> AboutLoaded(uiState.appInfo, modifier)
    }
}

@Composable
private fun AboutLoaded(appInfo: AppInfo?, modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutTokens.screenHorizontalPadding),
        contentPadding = PaddingValues(vertical = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        item { AppSectionHeader(title = "Version") }
        item {
            SettingsRow(
                title = "App version",
                icon = IconTokens.recommendation,
                control = SettingsRowControl.Value(appInfo?.versionName ?: "Unknown"),
            )
        }
        item {
            SettingsRow(
                title = "Version code",
                icon = IconTokens.document,
                control = SettingsRowControl.Value(appInfo?.versionCode?.toString() ?: "Unknown"),
            )
        }

        // Debug builds only. A release user has no use for the package
        // name and build type, and putting internal detail on a
        // production About screen is clutter at best.
        if (appInfo?.isDebugBuild == true) {
            item { AppSectionHeader(title = "Build (debug only)") }
            item {
                SettingsRow(
                    title = "Package",
                    supportingText = appInfo.packageName,
                    icon = IconTokens.settings,
                    control = SettingsRowControl.None,
                )
            }
            item {
                SettingsRow(
                    title = "Build type",
                    icon = IconTokens.settings,
                    control = SettingsRowControl.Value("Debug"),
                )
            }
        }

        item { AppSectionHeader(title = "How Space Antivirus works") }
        item {
            SettingsRow(
                title = "On-device only",
                supportingText = "Scans run entirely on your phone. Nothing about your apps or " +
                    "files is uploaded, and there is no cloud lookup or signature database.",
                icon = IconTokens.security,
                control = SettingsRowControl.None,
            )
        }

        item {
            Text(
                text = "\u00A9 ZX Force Soft",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.medium),
            )
        }
    }
}
