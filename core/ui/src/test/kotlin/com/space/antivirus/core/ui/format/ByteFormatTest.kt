package com.space.antivirus.core.ui.format

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Sprint 040. Plain JUnit — this is pure formatting with no Compose or
 * Android dependency, so it needs neither an emulator nor Robolectric.
 */
class ByteFormatTest {

    @Test
    fun `formats bytes below one kilobyte as plain bytes`() {
        assertThat(formatBytes(0L)).isEqualTo("0 B")
        assertThat(formatBytes(999L)).isEqualTo("999 B")
    }

    @Test
    fun `formats kilobytes megabytes and gigabytes`() {
        assertThat(formatBytes(2_000L)).isEqualTo("2.0 KB")
        assertThat(formatBytes(482_000_000L)).isEqualTo("482.0 MB")
        assertThat(formatBytes(1_200_000_000L)).isEqualTo("1.2 GB")
    }

    /**
     * Decimal units, matching what Android's own Settings > Storage
     * reports. Binary units would render this same value as ~1.1 GB and
     * quietly disagree with the system on the user's own device.
     */
    @Test
    fun `uses decimal units so figures match the platform's own reporting`() {
        assertThat(formatBytes(1_000_000_000L)).isEqualTo("1.0 GB")
        assertThat(formatBytes(1_000_000L)).isEqualTo("1.0 MB")
    }

    @Test
    fun `switches unit exactly at each boundary`() {
        assertThat(formatBytes(1_000L)).isEqualTo("1.0 KB")
        assertThat(formatBytes(999_999L)).isEqualTo("1000.0 KB")
        assertThat(formatBytes(1_000_000L)).isEqualTo("1.0 MB")
    }
}
