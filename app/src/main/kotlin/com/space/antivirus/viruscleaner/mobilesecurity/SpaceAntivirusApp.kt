package com.space.antivirus.viruscleaner.mobilesecurity

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Composition root. Deliberately thin — per Sprint 002 section 7's module
 * structure, :app is "a thin composition root," not where logic lives.
 *
 * Sprint 026.1 hotfix — real-device testing (Samsung Galaxy S9+,
 * Android 9 / API 28) surfaced NoSuchMethodException on ScanWorker's
 * <init>(Context, WorkerParameters). Root cause, traced end to end
 * before any fix was written: WorkManager's default ContentProvider-
 * based auto-initializer (androidx.startup) runs during
 * ContentProvider.onCreate(), which the Android platform always executes
 * BEFORE Application.onCreate() — on every API level, not just 28; this
 * was never an Android-9-specific behavior, just the first device it was
 * exercised on. Reading this class's Configuration.Provider implementation
 * that early is not guaranteed to see workerFactory already
 * field-injected by Hilt, so WorkManager silently fell back to its own
 * default, non-Hilt-aware WorkerFactory, which only knows the plain
 * 2-argument Worker constructor — not ScanWorker's real
 * @AssistedInject one, which also needs RunScanRequestUseCase.
 *
 * Fix: WorkManager's default auto-initializer is now disabled (manifest
 * <meta-data> override) and WorkManager.initialize(...) is called
 * manually here in onCreate() instead — which Hilt's generated base
 * class always invokes AFTER field injection completes, guaranteeing
 * workerFactory is real before WorkManager ever touches it. This is the
 * documented, standard fix for this exact Hilt+WorkManager integration
 * pattern (see e.g. Google's own Hilt+WorkManager guidance), not a
 * workaround, and it doesn't depend on the property-vs-function
 * Configuration.Provider API surface at all — that override remains
 * correct and unrelated to this fix.
 *
 * DELIBERATELY SYNCHRONOUS, not backgrounded — an earlier draft of this
 * fix dispatched WorkManager.initialize(...) to a background coroutine to
 * also address a separately-reported ~2-second startup frame-skip
 * (Xiaomi 23053RN02A, Android 15). That was reverted: RECEIVE_BOOT_COMPLETED
 * (Sprint 025) means this Application can be launched as part of a
 * boot-triggered start, and WorkManager's OWN bundled boot-rescheduling
 * receiver needs a genuinely initialized WorkManager instance soon after
 * boot — deferring initialize() asynchronously would risk that receiver
 * firing before the background dispatch completes, undoing the whole
 * point of the RECEIVE_BOOT_COMPLETED fix. A real, if narrow,
 * correctness regression is not an acceptable trade for an unverified
 * performance gain, especially since WorkManager.initialize() itself is
 * not the kind of call known to cost anywhere near 2 seconds in normal
 * operation. A full audit of MainActivity, this class, and
 * OnboardingViewModel (the app's actual start destination) found no
 * other synchronous Room/DataStore/repository work reachable from cold
 * launch — Room's databaseBuilder().build() and the DataStore delegate
 * are both lazy by construction and don't touch disk until first real
 * query, confirmed by inspection, not assumed. Root-causing the full
 * ~2 second duration precisely would need live on-device profiling this
 * sandbox cannot perform; nothing was found here that could be changed
 * with real confidence without either guessing or introducing a new bug.
 */
@HiltAndroidApp
class SpaceAntivirusApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // workerFactory is guaranteed already injected by this point —
        // Hilt's generated Hilt_SpaceAntivirusApp base class performs
        // field injection before delegating to this onCreate() override,
        // for every API level this app supports.
        WorkManager.initialize(this, workManagerConfiguration)

// Sprint 049 moved ads initialisation out of this class.
        //
        // It used to run here, right after WorkManager. That is no longer
        // permissible: the EU User Consent Policy requires consent to be
        // resolved before the ads SDK is initialised, and gathering
        // consent needs an Activity to present a form on. Initialisation
        // now happens in MainActivity, after UMP answers. See ADR 0056.
    }
}
