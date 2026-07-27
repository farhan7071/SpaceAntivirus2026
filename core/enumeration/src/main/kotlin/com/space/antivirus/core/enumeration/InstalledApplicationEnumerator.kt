package com.space.antivirus.core.enumeration

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.AppCategory
import com.space.antivirus.core.model.InstalledApplicationInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Wraps PackageManager — identity/install metadata only. Deliberately
 * does NOT read certificates or manifest components beyond permissions;
 * that's deeper analysis and belongs to a later sprint's analyzers
 * operating on the InstalledApplicationInfo this produces.
 *
 * Requests PackageManager.GET_PERMISSIONS as of Sprint 014 — added
 * alongside InstalledApplicationInfo.requestedPermissions when this
 * project's first ThreatAnalyzer needed real permission data that
 * neither this class nor that model actually carried before. See ADR
 * 0027.
 *
 * Sprint 027: also populates isDebuggable (ApplicationInfo.FLAG_DEBUGGABLE,
 * read from the SAME appInfo already fetched per iteration — no new
 * PackageManager call) and installerPackageName. The latter uses the
 * deprecated getInstallerPackageName(String) rather than API 30+'s
 * getInstallSourceInfo() — this project's minSdk is 26 and targets API
 * 28 through 35 (ADR 0003, Sprint 026.1's Android-9 verification), and
 * getInstallerPackageName has been stable and functional since API 1;
 * it's deprecated, not broken, on every API level this app supports.
 * Branching for a newer API just to avoid a @Suppress("DEPRECATION")
 * would add real complexity for zero behavioral benefit within this
 * app's actual supported range — same reasoning already applied to
 * versionCode below.
 *
 * Sprint 028: also populates category, from ApplicationInfo.category —
 * a real, stable Android API since API 26 (this project's exact
 * minSdk), read from the same appInfo already fetched per iteration, no
 * new PackageManager call. Mapped via ApplicationInfo's own named
 * CATEGORY_* constants, not guessed integer values, to eliminate any
 * risk of an incorrect raw-int mapping.
 *
 * Requires android.permission.QUERY_ALL_PACKAGES (declared in
 * AndroidManifest.xml as of this sprint). Without it, Android 11+'s
 * package-visibility filtering does NOT throw — it silently returns a
 * reduced subset of installed packages. That's the accurate failure mode
 * to document here: a caller seeing a suspiciously short list is more
 * likely than a caller seeing a SecurityException. The SecurityException
 * catch below remains as a defensive fallback for older API levels /
 * restricted environments where PackageManager can still throw, not as
 * the primary way this permission gap shows up.
 */
class InstalledApplicationEnumerator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun enumerate(): AppResult<List<InstalledApplicationInfo>> {
        val packageManager = context.packageManager
        return try {
            val packages = packageManager.getInstalledPackages(
                PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS,
            )
            val apps = packages.mapNotNull { packageInfo ->
                val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
                InstalledApplicationInfo(
                    packageName = packageInfo.packageName,
                    appLabel = packageManager.getApplicationLabel(appInfo).toString(),
                    versionName = packageInfo.versionName,
                    // packageInfo.longVersionCode requires API 28+; this
                    // project's minSdk is 26 (ADR 0003) — branching here
                    // avoids exactly the kind of API-level violation
                    // Sprint 003.5 already found once (an API-31-only
                    // color resource with no fallback for minSdk 26-30).
                    versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toLong()
                    },
                    installedAtEpochMillis = packageInfo.firstInstallTime,
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    apkPath = appInfo.sourceDir,
                    // requestedPermissions is null (not empty) when a
                    // package declares none at all — normalized to an
                    // empty list here so nothing downstream needs to
                    // handle a nullable permissions list.
                    requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList(),
                    isDebuggable = (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
                    installerPackageName = installerPackageNameFor(packageManager, packageInfo.packageName),
                    category = categoryFor(appInfo.category),
                )
            }
            AppResult.Success(apps)
        } catch (e: SecurityException) {
            // Defensive fallback — see class KDoc for why the more common
            // real-world failure mode is silent filtering, not this catch.
            AppResult.Failure(AppError.PermissionMissing)
        }
    }

    /**
     * Best-effort — a small, defensive try/catch of its own, separate
     * from the outer one. getInstallerPackageName can throw
     * IllegalArgumentException for a package PackageManager otherwise
     * happily returned in getInstalledPackages() (a known, documented
     * edge case for certain system/pseudo packages) — that's a reason to
     * treat installer provenance as unknown for THAT one package, not a
     * reason to fail the entire enumeration the outer catch guards.
     */
    private fun installerPackageNameFor(packageManager: PackageManager, packageName: String): String? = try {
        @Suppress("DEPRECATION")
        packageManager.getInstallerPackageName(packageName)
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun categoryFor(rawCategory: Int): AppCategory = when (rawCategory) {
        ApplicationInfo.CATEGORY_GAME -> AppCategory.GAME
        ApplicationInfo.CATEGORY_AUDIO -> AppCategory.AUDIO
        ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
        ApplicationInfo.CATEGORY_IMAGE -> AppCategory.IMAGE
        ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
        ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
        ApplicationInfo.CATEGORY_MAPS -> AppCategory.MAPS
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
        // CATEGORY_UNDEFINED and anything this project doesn't map both
        // land here deliberately — an unrecognized value must never be
        // silently treated as a positively-identified category.
        else -> AppCategory.UNDEFINED
    }
}
