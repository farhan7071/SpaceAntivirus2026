package com.space.antivirus.viruscleaner.mobilesecurity

import android.os.Bundle
import android.util.Log
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

    // DIAGNOSTIC (Sprint 32.4) — temporary, remove before release.
    // Overriding this Activity's own lifecycle methods directly, rather
    // than registering an Application.ActivityLifecycleCallbacks on
    // SpaceAntivirusApp, deliberately — this app is single-Activity by
    // design (this class's own KDoc above), so both approaches observe
    // identical information here, and this avoids touching
    // SpaceAntivirusApp.onCreate() at all, whose WorkManager/Hilt
    // initialization ordering is already deliberately fragile and
    // carefully reasoned (ADR 0040) — no diagnostic addition belongs
    // anywhere near it if it doesn't have to be. Purpose: during an
    // uninstall attempt, this answers whether the system Package
    // Installer actually took foreground (onPause, likely onStop, then
    // onResume only once the user returns) or immediately handed control
    // back (onPause immediately followed by onResume, with little or no
    // time and no onStop in between).
    override fun onPause() {
        Log.d("OverflowMenuDiag", "MainActivity.onPause()")
        super.onPause()
    }

    override fun onStop() {
        Log.d("OverflowMenuDiag", "MainActivity.onStop()")
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        Log.d("OverflowMenuDiag", "MainActivity.onResume()")
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
