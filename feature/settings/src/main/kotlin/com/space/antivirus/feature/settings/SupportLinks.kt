package com.space.antivirus.feature.settings

/**
 * Every external destination the Settings section can send a user to —
 * Sprint 043A.
 *
 * One object so a release checklist has one file to check, rather than
 * URLs scattered across four screens.
 *
 * **Sprint 047: these are now the production values** from the 2.0
 * release configuration, replacing the `.invalid` placeholders Sprint
 * 043A shipped. `arePolicyLinksConfigured` consequently returns true and
 * the "Not published yet" line disappears from the Settings hub on its
 * own — it was written to do exactly that.
 *
 * The check is deliberately kept rather than deleted. It costs nothing,
 * and it is the thing that would catch a future edit that reintroduced a
 * placeholder. Play reviewers check the privacy policy link on every
 * submission.
 *
 * **Both URLs must actually resolve before you submit.** This code can
 * only point at them; it cannot verify they exist.
 */
object SupportLinks {

    const val PRIVACY_POLICY_URL = "https://zxforcesoft.com/privacy"

    const val TERMS_OF_SERVICE_URL = "https://zxforcesoft.com/terms"

    const val FEEDBACK_EMAIL = "zxforceinfo@gmail.com"

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
