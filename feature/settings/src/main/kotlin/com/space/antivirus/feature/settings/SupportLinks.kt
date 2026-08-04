package com.space.antivirus.feature.settings

/**
 * Every external destination the Settings section can send a user to —
 * Sprint 043A.
 *
 * One object so a release checklist has one file to check, rather than
 * URLs scattered across four screens.
 *
 * **The policy URLs are placeholders and are marked as such in the UI.**
 * They have to be: this project has no published privacy policy or terms
 * page that I can point at, and inventing a plausible-looking URL would
 * be worse than an honest placeholder — it would look finished, ship,
 * and 404 in front of a user (or a Play reviewer, who checks the privacy
 * policy link on every submission). `arePolicyLinksConfigured` exists so
 * the UI can say plainly that these aren't set up yet instead of
 * pretending, and so a release build fails a human's eye rather than
 * silently shipping.
 *
 * Replace both constants with the real ZX Force Soft URLs before
 * release, and the marker disappears on its own.
 */
object SupportLinks {

    /** REPLACE BEFORE RELEASE. */
    const val PRIVACY_POLICY_URL = "https://example.invalid/privacy"

    /** REPLACE BEFORE RELEASE. */
    const val TERMS_OF_SERVICE_URL = "https://example.invalid/terms"

    /** REPLACE BEFORE RELEASE — the address feedback should reach. */
    const val FEEDBACK_EMAIL = "support@example.invalid"

    const val FEEDBACK_SUBJECT = "Space Antivirus feedback"

    /**
     * `market://` resolves to the installed Play Store app; the callers
     * fall back to the https form when it isn't present (a device with
     * no Play Store, or a sideloaded build).
     */
    fun playStoreMarketUri(packageName: String): String = "market://details?id=$packageName"

    fun playStoreWebUrl(packageName: String): String =
        "https://play.google.com/store/apps/details?id=$packageName"

    fun shareMessage(packageName: String): String =
        "I'm using Space Antivirus to keep my phone clean and secure: ${playStoreWebUrl(packageName)}"

    /**
     * False while the constants above are still placeholders. Deliberately
     * a substring check on the reserved `.invalid` TLD (RFC 2606), which
     * can never be a real destination — so this cannot accidentally
     * report true for an unconfigured build, and cannot report false for
     * a real one.
     */
    val arePolicyLinksConfigured: Boolean
        get() = !PRIVACY_POLICY_URL.contains(".invalid") && !TERMS_OF_SERVICE_URL.contains(".invalid")
}
