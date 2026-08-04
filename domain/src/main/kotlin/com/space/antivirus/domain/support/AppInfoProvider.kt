package com.space.antivirus.domain.support

import com.space.antivirus.core.model.AppInfo

/**
 * Reads the running build's own identity — Sprint 043A.
 *
 * An interface rather than reading BuildConfig directly from the About
 * screen, for two reasons. A feature module's own BuildConfig describes
 * that module, not the installed application, so it would report the
 * wrong thing. And a Composable reading platform APIs is business logic
 * in the UI layer, which this project does not do.
 */
interface AppInfoProvider {
    fun getAppInfo(): AppInfo
}
