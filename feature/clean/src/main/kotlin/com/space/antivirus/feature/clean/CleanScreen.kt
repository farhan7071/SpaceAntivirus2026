package com.space.antivirus.feature.clean

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Placeholder destination per Sprint 003 Task 4 ("only empty destinations,
 * no feature logic"). Real UI lands in Sprint 004+ per Sprint 002.5's
 * screen inventory for Clean.
 */
@Composable
fun CleanRoute(
    viewModel: CleanViewModel = hiltViewModel(),
) {
    CleanScreen()
}

@Composable
private fun CleanScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Clean — placeholder (Sprint 004+)")
    }
}
