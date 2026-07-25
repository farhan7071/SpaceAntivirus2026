package com.space.antivirus.viruscleaner.mobilesecurity

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Composition root. Deliberately thin — per Sprint 002 §7's module
 * structure, :app is "a thin composition root," not where logic lives.
 *
 * Sprint 024: implements Configuration.Provider so WorkManager's default
 * initializer (androidx.startup, already wired via the manifest merger —
 * no manifest changes needed) picks up HiltWorkerFactory automatically,
 * letting @HiltWorker-annotated workers (ScanWorker, core:workmanager)
 * be constructed by Hilt rather than WorkManager's default no-arg
 * reflection-based instantiation.
 *
 * NOTE on the one real API-surface uncertainty in this sprint: this
 * project's pinned WorkManager version (2.9.0) is recent enough that
 * Configuration.Provider should expose workManagerConfiguration as a
 * Kotlin property (this override), not the older getWorkManagerConfiguration()
 * function form some earlier WorkManager versions required — but this
 * could not be verified against a real compiler in this sandbox. If
 * Gradle Sync reports otherwise, this is a single-line, isolated
 * compatibility fix (property vs. function), not an architectural
 * change — nothing else in this sprint depends on which form is correct.
 */
@HiltAndroidApp
class SpaceAntivirusApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
