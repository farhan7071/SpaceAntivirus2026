package com.space.antivirus.core.enumeration

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.ScanScope
import io.mockk.mockk
import org.junit.Test

/**
 * Limited to the one branch testable without Robolectric: resolving
 * ScanScope.InstalledApplications never touches Context, so a relaxed
 * mock is enough. The Environment/Context-dependent branches
 * (InternalStorage, ExternalStorage, etc.) need a real or Robolectric-
 * simulated Android runtime — out of scope for this sandbox and this
 * sprint's fast-mode pass; flagged in the sprint report rather than
 * silently skipped.
 */
class ScanScopePathResolverTest {

    @Test
    fun `InstalledApplications is rejected as an invalid file scope`() {
        val context = mockk<Context>(relaxed = true)
        val resolver = ScanScopePathResolver(context)

        val result = resolver.resolve(ScanScope.InstalledApplications)

        assertThat(result).isInstanceOf(AppResult.Failure::class.java)
        val error = (result as AppResult.Failure).error
        assertThat(error).isInstanceOf(AppError.InvalidScanConfiguration::class.java)
    }
}
