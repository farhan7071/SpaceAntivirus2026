package com.space.antivirus.feature.realtime

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
 * screen inventory for RealTime.
 */
@Composable
fun RealTimeRoute(
    viewModel: RealTimeViewModel = hiltViewModel(),
) {
    RealTimeScreen()
}

@Composable
private fun RealTimeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "RealTime — placeholder (Sprint 004+)")
    }
}
