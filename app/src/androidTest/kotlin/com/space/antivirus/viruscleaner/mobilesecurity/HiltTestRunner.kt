package com.space.antivirus.viruscleaner.mobilesecurity

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Standard Hilt instrumented-test runner — launches HiltTestApplication
 * instead of the real SpaceAntivirusApp, so @HiltAndroidTest-annotated
 * tests get a real, isolated Hilt component graph to inject from. Added
 * in Sprint 013 specifically to make AnalysisEngineBindingModule's
 * bindings verifiable by something other than static reasoning.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
