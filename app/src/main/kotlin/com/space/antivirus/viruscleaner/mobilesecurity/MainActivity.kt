package com.space.antivirus.viruscleaner.mobilesecurity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.space.antivirus.core.designsystem.theme.SpaceAntivirusTheme
import com.space.antivirus.viruscleaner.mobilesecurity.navigation.SpaceAntivirusNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-Activity host per Sprint 002 §7's Compose migration decision.
 * Replaces the old app's MainActivity + PermissionHelpActivity two-Activity
 * pattern (Sprint 001) — permission rationale is now an in-nav-graph
 * screen/dialog, not a separate transparent Activity.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpaceAntivirusRoot()
        }
    }
}

@Composable
private fun SpaceAntivirusRoot() {
    SpaceAntivirusTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            SpaceAntivirusNavHost(navController = navController)
        }
    }
}
