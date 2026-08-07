package com.space.antivirus.core.ads

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Sprint 049.
 *
 * `UmpConsentManager` itself needs the UMP SDK and an Activity, so the
 * one piece of judgement inside it — how a UMP answer becomes an
 * ad-serving decision — is extracted and tested here on the JVM.
 *
 * The mapping is short, and short is the point: it deliberately reads
 * `canRequestAds()` rather than switching on `ConsentStatus`. Mapping
 * `OBTAINED` to yes and everything else to no is wrong in both
 * directions — a user outside the EEA gets `NOT_REQUIRED` and may
 * lawfully be shown ads, while `OBTAINED` alone does not distinguish a
 * user who accepted personalised ads from one who declined them but
 * remains eligible for non-personalised. `canRequestAds()` folds all of
 * that in and is the value the Mobile Ads SDK itself respects.
 */
class ConsentStateMappingTest {

    @Test
    fun `ads permitted maps to granted`() {
        assertThat(consentStateFor(canRequestAds = true)).isEqualTo(ConsentState.GRANTED)
    }

    @Test
    fun `ads not permitted maps to denied`() {
        assertThat(consentStateFor(canRequestAds = false)).isEqualTo(ConsentState.DENIED)
    }

    /** Both mapped outcomes must agree with the gate's own reading, or
     *  the state would say one thing and the gate do another. */
    @Test
    fun `the mapped states drive the gate as expected`() {
        assertThat(consentStateFor(canRequestAds = true).allowsAds).isTrue()
        assertThat(consentStateFor(canRequestAds = false).allowsAds).isFalse()
    }

    /**
     * UNKNOWN is never produced by the mapping — once UMP has answered,
     * the question is decided. It survives only as the state before any
     * answer exists, which is the initial value and what every failure
     * path leaves in place. This is what makes the architecture
     * fail-closed: a network failure, a form that will not load, or a
     * dismissed form all end with the app serving nothing.
     */
    @Test
    fun `unknown is never a mapped outcome and blocks ads`() {
        assertThat(consentStateFor(canRequestAds = true)).isNotEqualTo(ConsentState.UNKNOWN)
        assertThat(consentStateFor(canRequestAds = false)).isNotEqualTo(ConsentState.UNKNOWN)
        assertThat(ConsentState.UNKNOWN.allowsAds).isFalse()
    }
}
