package com.space.antivirus.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.space.antivirus.core.designsystem.brand.SpaceBrandMark
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.designsystem.theme.ShapeTokens
import com.space.antivirus.core.ui.component.AppFilledButton
import com.space.antivirus.core.ui.component.AppTextButton

/**
 * This project's second production screen, following ADR 0030's
 * stateful/stateless split exactly: OnboardingRoute is the only
 * composable touching hiltViewModel()/collectAsStateWithLifecycle();
 * OnboardingScreen is a pure function of OnboardingUiState plus explicit
 * callbacks, with no ViewModel/DI awareness at all.
 *
 * Deliberately uses NO Material icons anywhere in this file — Sprint
 * 017's verification found Icons.Default.ErrorOutline doesn't exist in
 * this project's baseline (non-Extended) Material icon set. Rather than
 * guess at which icons ARE safely in the baseline set without a real
 * compiler available to check, this screen is entirely text-based.
 */
@Composable
fun OnboardingRoute(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(
        uiState = uiState,
        onNext = viewModel::onNext,
        onBack = viewModel::onBack,
        onGetStarted = onOnboardingComplete,
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val page = OnboardingPages[uiState.currentPageIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.large),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Sprint 045. Onboarding was three stacked paragraphs, which is
        // a weak first impression for the screen that decides whether a
        // user trusts a security app. The mark gives each page an anchor
        // and carries the brand from the very first screen; the page
        // indicator becomes dots because "1 of 3" is a status readout
        // where a progress cue belongs.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SpaceBrandMark(
                size = BRAND_MARK_SIZE,
                contentDescription = null,
                modifier = Modifier.padding(top = spacing.extraLarge, bottom = spacing.medium),
            )
            PageIndicator(
                currentPageIndex = uiState.currentPageIndex,
                totalPages = uiState.totalPages,
            )
            Text(
                text = page.headline,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = page.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (uiState.isFirstPage) {
                // Reserve the row position without a visible Back action —
                // keeps the Next/Get Started button's horizontal position
                // stable across pages rather than jumping when Back appears.
                Text(text = "")
            } else {
                AppTextButton(text = "Back", onClick = onBack)
            }

            if (uiState.isLastPage) {
                AppFilledButton(text = "Get Started", onClick = onGetStarted)
            } else {
                AppFilledButton(text = "Next", onClick = onNext)
            }
        }
    }
}

/**
 * Sprint 045. Replaces the "1 of 3" label.
 *
 * The count is still announced — it moves into the row's
 * contentDescription rather than disappearing, so a screen-reader user
 * gets the same information a sighted user reads from the dots. The dots
 * themselves are cleared from the semantics tree so the position is read
 * once, not three times.
 */
@Composable
private fun PageIndicator(currentPageIndex: Int, totalPages: Int) {
    val spacing = LocalSpacing.current
    val label = "Page ${currentPageIndex + 1} of $totalPages"
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { contentDescription = label },
    ) {
        repeat(totalPages) { index ->
            val isCurrent = index == currentPageIndex
            Box(
                modifier = Modifier
                    .size(
                        width = if (isCurrent) INDICATOR_ACTIVE_WIDTH else INDICATOR_DOT_SIZE,
                        height = INDICATOR_DOT_SIZE,
                    )
                    .clip(ShapeTokens.chip)
                    .background(
                        if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}

private val BRAND_MARK_SIZE = 112.dp
private val INDICATOR_DOT_SIZE = 8.dp
private val INDICATOR_ACTIVE_WIDTH = 24.dp
