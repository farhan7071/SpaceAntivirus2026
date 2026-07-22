package com.space.antivirus.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures cold-start time against the Sprint 002 §10 performance target
 * (<1.5s to first interactive frame). This is Sprint 003's foundation only
 * — meaningful numbers require a real device/emulator run, which this
 * sandbox cannot perform (no Android runtime available here). Structure is
 * real and ready to run in Android Studio / CI.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupCompilationModeNone() = startup(CompilationMode.None())

    @Test
    fun startupBaselineProfile() = startup(CompilationMode.Partial())

    private fun startup(compilationMode: CompilationMode) {
        benchmarkRule.measureRepeated(
            packageName = "com.space.antivirus.viruscleaner.mobilesecurity",
            metrics = listOf(StartupTimingMetric()),
            iterations = 5,
            startupMode = StartupMode.COLD,
            compilationMode = compilationMode,
        ) {
            pressHome()
            startActivityAndWait()
        }
    }
}
