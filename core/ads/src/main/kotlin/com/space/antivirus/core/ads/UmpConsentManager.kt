package com.space.antivirus.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The real consent implementation — Sprint 049.
 *
 * Replaces `UnresolvedConsentProvider`, which Sprint 044 shipped
 * deliberately returning UNKNOWN so that no build could serve an ad
 * before a certified consent platform existed. That placeholder has done
 * its job and is deleted in this sprint.
 *
 * **What UMP actually decides, and what this class does not.** The SDK
 * determines whether the user is in a region where consent is required,
 * presents Google's own certified form when it is, and persists the
 * outcome itself — in its own SharedPreferences, surviving restarts and
 * shared with the Mobile Ads SDK. This project stores nothing about
 * consent and must not: a second copy would be a second source of truth
 * that goes stale the moment the user changes their mind through the
 * privacy options form.
 *
 * **`canRequestAds()` is the authoritative signal, not the status
 * enum.** It is tempting to map `ConsentStatus.OBTAINED` to "yes" and
 * everything else to "no", and that mapping is wrong in both directions.
 * A user outside the EEA gets `NOT_REQUIRED`, where ads are perfectly
 * permitted. A user who declined personalised ads may still be eligible
 * for non-personalised ones, and `OBTAINED` alone does not tell you
 * which. Google exposes `canRequestAds()` precisely because it folds all
 * of that in, and it is the value the Mobile Ads SDK itself respects.
 *
 * **Failure means no ads.** Every error path here leaves the state at
 * UNKNOWN, which `AdsGate` refuses. A network failure fetching the
 * consent form, a form that fails to load, a user who dismisses it —
 * all of them end with the app serving nothing rather than serving
 * something it cannot prove it is allowed to.
 */
@Singleton
class UmpConsentManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : ConsentProvider {

    private val consentInformation: ConsentInformation by lazy {
        UserMessagingPlatform.getConsentInformation(context)
    }

    /**
     * Written on the main thread by UMP's callbacks and read from
     * arbitrary threads by `AdsGate`, so it is marked volatile rather
     * than left as a plain field.
     */
    @Volatile
    private var resolvedState: ConsentState = ConsentState.UNKNOWN

    /** Guards against a second gather while one is in flight — a
     *  configuration change recreating the Activity is the ordinary way
     *  that happens. */
    private val gatherInFlight = AtomicBoolean(false)

    override fun currentConsent(): ConsentState = resolvedState

    /**
     * Runs the consent flow, then reports whether ads may now be
     * requested.
     *
     * [onConsentResolved] fires exactly once per call, on the main
     * thread, on every path including failure — the caller uses it to
     * decide whether to initialise the Mobile Ads SDK at all, so a path
     * that never called back would leave ads permanently uninitialised.
     *
     * Requires an Activity because that is what UMP needs to present a
     * form. It stops here rather than travelling further: no other class
     * in this project sees a consent type.
     */
    fun gatherConsent(activity: Activity, onConsentResolved: (canRequestAds: Boolean) -> Unit) {
        if (!gatherInFlight.compareAndSet(false, true)) {
            // Already running. Report the state as it currently stands
            // rather than starting a second flow, which would risk two
            // forms racing each other onto the screen.
            onConsentResolved(resolvedState.allowsAds)
            return
        }

        // Deliberately no ConsentDebugSettings and no test-device hash.
        // Those force a geography for testing and would be a compliance
        // hazard if one ever reached a release build. Testing the EEA
        // path is done by registering a test device in the AdMob console
        // instead — see docs/ads.md.
        val parameters = ConsentRequestParameters.Builder()
            // The app has no child-directed treatment declaration and is
            // not listed for families; stating false here matches that.
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            parameters,
            {
                // Info updated. If a form is required this shows it; if
                // not, it returns immediately. Either way the callback
                // below is where the outcome is read.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    // The FormError argument is deliberately ignored:
                    // whether the form failed or the user dismissed it,
                    // the only question that matters is the one asked on
                    // the next line, and canRequestAds() already
                    // accounts for both.
                    finish(onConsentResolved)
                }
            },
            {
                // requestConsentInfoUpdate failed — typically no network
                // on first launch. canRequestAds() may still be true
                // from a previously persisted decision, so it is asked
                // rather than assumed; if it is not, the state stays
                // UNKNOWN and nothing is served.
                finish(onConsentResolved)
            },
        )
    }

    /**
     * True when Google requires this app to offer an ongoing way to
     * change consent — exposed so the UI layer can decide whether to
     * show that entry point, without any screen learning what UMP is.
     */
    fun arePrivacyOptionsRequired(): Boolean =
        consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Re-presents the consent form so a user can change a decision they
     * have already made.
     *
     * Required by Google wherever [arePrivacyOptionsRequired] is true.
     * The capability exists here and is ready to be wired to a Settings
     * row; Sprint 049's brief scoped UI changes out, so no screen calls
     * it yet.
     */
    fun showPrivacyOptionsForm(activity: Activity, onDismissed: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            // The user may have withdrawn consent, so the cached state is
            // re-read rather than left as it was.
            resolvedState = consentStateFor(consentInformation.canRequestAds())
            onDismissed()
        }
    }

    private fun finish(onConsentResolved: (Boolean) -> Unit) {
        val canRequestAds = consentInformation.canRequestAds()
        resolvedState = consentStateFor(canRequestAds)
        gatherInFlight.set(false)
        onConsentResolved(canRequestAds)
    }
}

/**
 * The whole mapping, extracted so it can be tested without the SDK.
 *
 * Two values, not three: once UMP has answered, "unknown" is no longer a
 * possible outcome — the question has been decided one way or the other.
 * UNKNOWN survives only as the state before any answer exists, which is
 * the initial value and the one every failure path leaves in place.
 */
internal fun consentStateFor(canRequestAds: Boolean): ConsentState =
    if (canRequestAds) ConsentState.GRANTED else ConsentState.DENIED
