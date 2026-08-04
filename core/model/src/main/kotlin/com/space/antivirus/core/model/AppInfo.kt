package com.space.antivirus.core.model

/**
 * Identity of the running build, for the About screen — Sprint 043A.
 *
 * Deliberately holds only facts the platform can answer for. There is no
 * signature-database version and no last-signature-update field: this
 * project has never shipped a signature database (standing rule since
 * Sprint 002 — no signature databases, no cloud scanning), so those
 * fields would have nothing behind them. Sprint 043's own brief lists
 * them as out of scope for exactly that reason.
 */
data class AppInfo(
    val versionName: String,
    val versionCode: Long,
    val packageName: String,
    val isDebugBuild: Boolean,
) {
    init {
        require(packageName.isNotBlank()) { "packageName cannot be blank" }
    }
}
