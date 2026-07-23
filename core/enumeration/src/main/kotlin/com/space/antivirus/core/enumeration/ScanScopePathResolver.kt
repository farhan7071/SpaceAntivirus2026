package com.space.antivirus.core.enumeration

import android.content.Context
import android.os.Environment
import com.space.antivirus.core.common.AppError
import com.space.antivirus.core.common.AppResult
import com.space.antivirus.core.model.ScanScope
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

/**
 * The ONLY place a ScanScope's abstract "where" is turned into an actual
 * filesystem root — isolated here specifically so FileTreeWalker itself
 * stays Android-free and unit-testable. If a later sprint needs to change
 * how "Downloads folder" is resolved (e.g. scoped storage / MediaStore
 * changes), this is the one class that changes.
 */
class ScanScopePathResolver @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Returns the root directory for a file-oriented scope. Fails with
     *  AppError.InvalidScanConfiguration if called with
     *  ScanScope.InstalledApplications (not a file scope — that's
     *  InstalledApplicationEnumerator's job), or AppError.StorageUnavailable
     *  if the resolved path doesn't exist / isn't accessible.
     *
     *  Two decisions worth being explicit about:
     *  - ExternalStorage resolves to the app-scoped external directory
     *    (getExternalFilesDir), not broad shared storage. Sprint 001
     *    flagged MANAGE_EXTERNAL_STORAGE as the project's highest-scrutiny
     *    permission; scoping enumeration to what the app can already
     *    reach without it is the more defensible default. Broad
     *    "all files" scanning, if the product actually needs it, is a
     *    deliberate future decision to revisit — not something to fall
     *    into by default here.
     *  - MediaCollection currently resolves to Pictures only, not a
     *    combined pictures+video+audio view — a real simplification, not
     *    an oversight. Extending it to a proper multi-directory
     *    MediaStore-based query is reasonable follow-up work once a
     *    concrete feature actually needs it. */
    fun resolve(scope: ScanScope): AppResult<File> = when (scope) {
        is ScanScope.InstalledApplications ->
            AppResult.Failure(
                AppError.InvalidScanConfiguration(
                    "ScanScope.InstalledApplications is not a file scope — use " +
                        "enumerateInstalledApplications() instead of enumerateFiles().",
                ),
            )
        is ScanScope.InternalStorage -> context.filesDir.toAppResult()
        is ScanScope.ExternalStorage -> context.getExternalFilesDir(null).toAppResult()
        is ScanScope.DownloadsFolder ->
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toAppResult()
        is ScanScope.MediaCollection ->
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toAppResult()
        is ScanScope.UserSelectedFolder -> File(scope.path).toAppResult()
    }

    private fun File?.toAppResult(): AppResult<File> {
        if (this == null) return AppResult.Failure(AppError.StorageUnavailable)
        if (!exists() || !canRead()) return AppResult.Failure(AppError.StorageUnavailable)
        return AppResult.Success(this)
    }
}
