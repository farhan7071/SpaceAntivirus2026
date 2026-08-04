package com.space.antivirus.core.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import com.space.antivirus.core.model.AppInfo
import com.space.antivirus.domain.support.AppInfoProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sprint 043A. Reads the installed package rather than any module's
 * generated BuildConfig — a feature module's BuildConfig describes that
 * module, not the application the user actually installed, so it would
 * report the wrong version.
 *
 * `PackageInfoCompat.getLongVersionCode` handles the API 28 split
 * between versionCode and longVersionCode without a manual SDK check.
 *
 * The debug flag is read from the manifest's own FLAG_DEBUGGABLE, which
 * is what actually distinguishes a debug install — the About screen uses
 * it to hide build details from release users, so reading the real flag
 * rather than a compile-time constant matters.
 */
@Singleton
class AndroidAppInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppInfoProvider {

    override fun getAppInfo(): AppInfo {
        val packageName = context.packageName
        val versionName: String
        val versionCode: Long
        try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            versionName = packageInfo.versionName ?: UNKNOWN_VERSION
            versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            // An app failing to find itself is close to impossible, but
            // a crash on the About screen would be a poor trade for a
            // version string. Reported as unknown rather than invented.
            return AppInfo(
                versionName = UNKNOWN_VERSION,
                versionCode = 0L,
                packageName = packageName,
                isDebugBuild = isDebuggable(),
            )
        }

        return AppInfo(
            versionName = versionName,
            versionCode = versionCode,
            packageName = packageName,
            isDebugBuild = isDebuggable(),
        )
    }

    private fun isDebuggable(): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private companion object {
        const val UNKNOWN_VERSION = "Unknown"
    }
}
