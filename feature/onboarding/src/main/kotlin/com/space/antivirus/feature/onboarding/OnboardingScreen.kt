package com.space.antivirus.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.space.antivirus.core.designsystem.theme.LocalSpacing
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Text(
                text = "${uiState.currentPageIndex + 1} of ${uiState.totalPages}",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(text = page.headline, style = MaterialTheme.typography.headlineSmall)
            Text(text = page.body, style = MaterialTheme.typography.bodyLarge)
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
