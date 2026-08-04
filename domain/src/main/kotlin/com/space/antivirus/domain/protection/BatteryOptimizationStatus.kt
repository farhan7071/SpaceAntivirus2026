package com.space.antivirus.domain.protection

/**
 * Whether the OS's battery optimisation is likely to delay this app's
 * scheduled work — Sprint 042.
 *
 * Informational only, by design. This app asks nothing and changes
 * nothing: `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`' direct-request Intent
 * is Play-restricted to a narrow set of app categories, and a security
 * app nagging its way onto an unrestricted battery allowlist is exactly
 * the behaviour that gives cleaner apps their reputation. The user is
 * told what the trade-off is and left to decide.
 */
interface BatteryOptimizationStatus {

    /**
     * True when this app is exempt from battery optimisation.
     *
     * Note the honest limits of this signal: it reflects the standard
     * Android allowlist only. Several manufacturers layer their own
     * aggressive process management on top of it, which this cannot
     * see — so `true` means "not restricted by the standard mechanism",
     * not "guaranteed to run on time".
     */
    fun isIgnoringBatteryOptimizations(): Boolean
}
