package com.space.antivirus.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TrustedItemTest {

    @Test
    fun `rejects a blank identifier`() {
        val exception = runCatching {
            TrustedItem(
                id = "t1",
                identifier = "",
                type = TrustedItemType.FILE,
                addedAtEpochMillis = 0L,
            )
        }.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `constructs successfully with a valid identifier and no reason`() {
        val item = TrustedItem(
            id = "t1",
            identifier = "/downloads/known-tool.apk",
            type = TrustedItemType.FILE,
            addedAtEpochMillis = 100L,
        )
        assertThat(item.reason).isNull()
    }
}
