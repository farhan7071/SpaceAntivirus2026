package com.space.antivirus.core.analysisengine.analyzer

import com.space.antivirus.core.model.Confidence
import com.space.antivirus.core.model.InstalledApplicationInfo

/**
 * Sprint 031 — Confidence Engine v2's central mechanism, shared by the
 * three "permission-behavior" analyzers (SuspiciousPermissionPatternAnalyzer,
 * OverlayPermissionAnalyzer, SurveillanceCombinationAnalyzer). See ADR
 * 0045 for the full root-cause analysis this responds to; summarized
 * here for the reader of this specific file:
 *
 * Physical-device testing found well-known, feature-rich apps
 * (communication, banking, Samsung, ride-sharing) reaching ACTION_NEEDED
 * despite expected behavior. Traced to CumulativeRiskScorer's existing,
 * unchanged escalation rule (ADR 0041: two or more distinct analyzers,
 * each at least ATTENTION severity and at least MODERATE confidence,
 * escalate together) combined with a real gap: these three analyzers
 * describe WHAT an app CAN DO — permission combinations legitimate,
 * feature-rich apps routinely need several of simultaneously (SMS for
 * OTP, camera/microphone for calls, overlay for chat heads or
 * multi-window features). Every one of them always reports flat
 * Confidence.MODERATE regardless of anything else known about the app,
 * so three entirely ordinary, expected permission clusters on one
 * legitimate app could jointly qualify for escalation exactly as if they
 * were three independent attack indicators.
 *
 * This is deliberately NOT a change to CumulativeRiskScorer itself — its
 * escalation rule is unchanged, still exactly two-or-more distinct
 * MODERATE+-confidence analyzers. That rule was already correct: two
 * genuinely independent, each-already-meaningful signals ARE stronger
 * evidence together. What was missing was giving these three analyzers
 * a way to recognize when the "signal" they're contributing is actually
 * common, expected behavior for the specific app in front of them,
 * rather than reporting a flat confidence that can't reflect that
 * distinction. Fixing it here — where the actual information gap is —
 * is "prefer extending existing components over introducing parallel
 * systems," not a new scoring system alongside the existing one.
 *
 * Two independent legitimacy signals, either one downgrading confidence
 * by one tier (HIGH -> MODERATE, MODERATE -> LOW); a finding matching
 * both signals is still only downgraded once, not twice, since both are
 * evidence toward the SAME underlying question ("is this ordinary,
 * expected behavior for this specific app?"), not two separately
 * additive facts:
 *
 * 1. Installed from a known, established app store. installerPackageName
 *    (Sprint 027) already carries this — no new data collected. Three
 *    real, well-known distribution channels are recognized: Google Play
 *    Store (com.android.vending), Samsung Galaxy Store
 *    (com.sec.android.app.samsungapps), and Xiaomi's own app store
 *    (com.xiaomi.mipicks, "GetApps" — Sprint 033, added after
 *    physical-device testing on Xiaomi hardware showed Xiaomi's own
 *    first-party apps, e.g. Xiaomi Home and Mi Store, reaching full,
 *    undowngraded confidence: they have no consistent AppCategory to
 *    match against — Android's taxonomy has no "smart-home" category,
 *    the same gap ADR 0046 already documented for banking apps — and
 *    when installed or updated through Xiaomi's own store rather than
 *    Play Store, neither existing installer entry applied either,
 *    leaving both legitimacy signals unavailable at once). This is
 *    provenance, not a reputation service — no external call, no
 *    app-specific allowlist, exactly the same kind of on-device signal
 *    UnknownInstallerSourceAnalyzer (Sprint 027) already reasons about
 *    in the opposite direction. A store listing isn't a guarantee of
 *    safety (no app store's review is perfect), so this only ever
 *    lowers confidence one tier, never suppresses a finding outright
 *    the way Sprint 028's category suppression does for the one case
 *    (video-calling apps) that was unambiguous enough to warrant it.
 *
 *    com.xiaomi.mipicks is Xiaomi's current package name for this app
 *    (formerly com.xiaomi.market, renamed by Xiaomi at least once) —
 *    included with the same moderate-not-full confidence ADR 0045 was
 *    explicit about for the Samsung Galaxy Store package name: not
 *    verified against a live device or official Xiaomi documentation in
 *    this sandbox. If this exact package name has changed again, this
 *    is an isolated, one-line fix here — nothing else in this project
 *    depends on it being exactly right.
 *
 * 2. The app's own declared category is consistent with the specific
 *    finding. Each analyzer decides for itself which categories are
 *    consistent with what it's flagging (a category consistent with
 *    "needs overlay" isn't the same set as "needs SMS") — that
 *    judgment belongs with the analyzer that knows what it's actually
 *    evaluating, not centralized here as one blanket rule.
 *
 * Identity-deception analyzers (AppIdentityImpersonationAnalyzer,
 * HighRiskPackageNameAnalyzer) deliberately do NOT use this — an app
 * claiming to be a brand it isn't, or squatting a reserved system
 * namespace, has no legitimate reason to do so regardless of which
 * store it came from or what category it declares. Confidence
 * modulation only applies to analyzers reasoning about WHAT an app
 * does, never to ones reasoning about WHO an app claims to be.
 */
object ConfidenceModulation {

    private val TRUSTED_INSTALLERS = setOf(
        "com.android.vending",
        "com.sec.android.app.samsungapps",
        "com.xiaomi.mipicks",
    )

    fun modulate(base: Confidence, app: InstalledApplicationInfo, categoryIsConsistent: Boolean): Confidence {
        val fromTrustedInstaller = app.installerPackageName in TRUSTED_INSTALLERS
        val hasLegitimacySignal = fromTrustedInstaller || categoryIsConsistent
        return if (hasLegitimacySignal) base.oneStepDown() else base
    }

    private fun Confidence.oneStepDown(): Confidence = when (this) {
        Confidence.HIGH -> Confidence.MODERATE
        Confidence.MODERATE -> Confidence.LOW
        Confidence.LOW -> Confidence.LOW
    }
}
