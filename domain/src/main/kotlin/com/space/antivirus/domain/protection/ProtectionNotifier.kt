package com.space.antivirus.domain.protection

/**
 * Everything this app is allowed to tell the user through the
 * notification shade — Sprint 042.
 *
 * An interface in `domain` with its Android implementation elsewhere,
 * the same split every repository has used since Sprint 004B. That
 * matters more than usual here: it is what lets `ProtectionManagerImpl`
 * — the class deciding *when* the user gets interrupted — be tested
 * without an Android framework, which is the part worth testing.
 *
 * Deliberately narrow. Three things to say, three methods. There is no
 * general `notify(title, body)` entry point, because the surest route to
 * a spammy security app is a notification API that makes it easy to add
 * one more.
 */
interface ProtectionNotifier {

    /**
     * Posts (or refreshes) the ongoing status notification.
     *
     * `earliestNextScanEpochMillis` is null when unknown; the
     * implementation must then say less rather than guess.
     */
    fun showProtectionActive(earliestNextScanEpochMillis: Long?)

    /** Removes the ongoing status notification. */
    fun hideProtectionActive()

    /**
     * The result of a scheduled scan, if the user asked to be told.
     *
     * Takes real counts rather than a pre-written message, so the
     * implementation cannot be handed a claim the scan didn't support.
     */
    fun showScanCompleted(threatsFound: Int, highRiskFound: Int)

    /** True if the OS will actually display anything this posts.
     *  POST_NOTIFICATIONS is a runtime permission on API 33+, so this
     *  can be false even though the manifest declares it. */
    fun areNotificationsPermitted(): Boolean
}
