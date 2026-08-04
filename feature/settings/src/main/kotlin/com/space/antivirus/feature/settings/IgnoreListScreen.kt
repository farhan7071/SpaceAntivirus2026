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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.space.antivirus.core.designsystem.theme.IconTokens
import com.space.antivirus.core.designsystem.theme.LayoutTokens
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.model.TrustedItem
import com.space.antivirus.core.ui.component.AppCircularProgress
import com.space.antivirus.core.ui.component.AppEmptyState
import com.space.antivirus.core.ui.component.AppSectionHeader
import com.space.antivirus.core.ui.component.EmptyStateTone
import com.space.antivirus.core.ui.component.SettingsRow
import com.space.antivirus.core.ui.component.SettingsRowControl
import java.text.DateFormat
import java.util.Date

@Composable
fun IgnoreListRoute(
    modifier: Modifier = Modifier,
    viewModel: IgnoreListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    IgnoreListScreen(
        uiState = uiState,
        onRemoveClick = viewModel::onRemoveClick,
        modifier = modifier,
    )
}

/**
 * Apps the user has chosen to skip — Sprint 043A.
 *
 * One view over the existing trusted-items table, not a second store:
 * removing an entry here is the same operation Security Center's Ignore
 * action inverts, so the app flags that package again on the next scan.
 *
 * `reason` is shown when the stored item has one, because a user
 * revisiting this list weeks later needs to remember why they ignored
 * something before deciding whether to un-ignore it. It's an optional
 * field on `TrustedItem` and is simply absent when unset — never
 * back-filled with a plausible-sounding default.
 */
@Composable
fun IgnoreListScreen(
    uiState: IgnoreListUiState,
    onRemoveClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is IgnoreListUiState.Loading -> Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppCircularProgress(progress = null)
        }

        is IgnoreListUiState.Error -> AppEmptyState(
            icon = IconTokens.warning,
            title = "Couldn't load your ignore list",
            message = uiState.message,
            modifier = modifier.fillMaxSize(),
        )

        is IgnoreListUiState.Loaded ->
            if (uiState.items.isEmpty()) {
                // Positive, not neutral: an empty ignore list means
                // nothing has been waved through, which is the better
                // state to be in, not a gap to be filled.
                AppEmptyState(
                    icon = IconTokens.trusted,
                    title = "Nothing ignored",
                    message = "Apps you choose to ignore from a scan finding will appear here, " +
                        "so you can always undo that later.",
                    tone = EmptyStateTone.POSITIVE,
                    modifier = modifier.fillMaxSize(),
                )
            } else {
                IgnoreListLoaded(uiState.items, onRemoveClick, modifier)
            }
    }
}

@Composable
private fun IgnoreListLoaded(
    items: List<TrustedItem>,
    onRemoveClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LayoutTokens.screenHorizontalPadding),
        contentPadding = PaddingValues(vertical = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        item { AppSectionHeader(title = "${items.size} app(s) ignored") }

        items(items.size) { index ->
            val item = items[index]
            SettingsRow(
                title = item.identifier,
                supportingText = item.supportingText(),
                icon = IconTokens.trusted,
                control = SettingsRowControl.Action(
                    label = "Remove",
                    onClick = { onRemoveClick(item.identifier) },
                ),
            )
        }

        item {
            Text(
                text = "Removing an app from this list means Space Antivirus will check it " +
                    "again during the next scan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.small),
            )
        }
    }
}

/** Ignored date plus the stored reason when there is one. */
private fun TrustedItem.supportingText(): String {
    val ignoredOn = "Ignored " +
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(addedAtEpochMillis))
    return reason?.takeIf { it.isNotBlank() }?.let { "$ignoredOn \u00B7 $it" } ?: ignoredOn
}
