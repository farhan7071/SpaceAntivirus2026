package com.space.antivirus.viruscleaner.mobilesecurity

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Composition root. Deliberately thin — per Sprint 002 §7's module
 * structure, :app is "a thin composition root," not where logic lives.
 */
@HiltAndroidApp
class SpaceAntivirusApp : Application()
