package com.space.antivirus.core.cleaningdata

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The complete, closed set of directories this app is permitted to
 * delete from — and the guard that enforces it. Sprint 039.
 *
 * This is the single most safety-critical class in the Cleaner. Every
 * deletion in the project passes through [contains] first, at the
 * repository level, below every use case, so that no calling code can
 * forget the check or be talked out of it by a bad candidate list.
 *
 * **The roots, and why only these four.** All four are app-private
 * storage — directories Android grants this app exclusive ownership of,
 * which need no permission to read or write, and which no other app's
 * data lives in:
 *
 * - `filesDir` — internal app files
 * - `cacheDir` — internal app cache
 * - `getExternalFilesDir(null)` — app-private external files
 * - `externalCacheDir` — app-private external cache
 *
 * The last two can be null on a device with no external volume mounted;
 * they are simply absent from the set when that happens rather than
 * being treated as an error.
 *
 * **What is deliberately NOT here, and why it can't be.** Other apps'
 * cache directories are unreachable on any modern Android — clearing
 * them requires a system/privileged permission this app will never
 * hold, and no amount of application-level code changes that. Shared
 * storage (Downloads, Pictures, the rest of `/sdcard`) is reachable only
 * with `MANAGE_EXTERNAL_STORAGE`, a Play-restricted permission this
 * project has declined since Sprint 001 and does not declare, or with
 * per-file `MediaStore` delete requests that prompt the user for each
 * batch. Both are product decisions with real policy weight, not
 * technical oversights. See ADR 0054.
 *
 * **Symlink and traversal safety.** Paths are compared after
 * `canonicalFile` resolution, so `filesDir/../../../data/other.app` and
 * a symlink pointing outside the sandbox both resolve to their real
 * location before the check, and both fail it. Comparing raw path
 * strings would let either one through.
 */
@Singleton
class AppPrivateStorageRoots @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Resolved lazily and cached: these paths do not change for the
     *  lifetime of the process, and canonicalisation touches the
     *  filesystem. */
    private val canonicalRoots: List<File> by lazy {
        listOfNotNull(
            context.filesDir,
            context.cacheDir,
            context.getExternalFilesDir(null),
            context.externalCacheDir,
        ).mapNotNull { it.canonicalFileOrNull() }
    }

    /**
     * True only if [path] resolves to a location strictly inside one of
     * the app-private roots.
     *
     * A root itself is deliberately NOT contained in itself: deleting
     * `filesDir` wholesale is not a cleanup, it is removing a directory
     * Android expects to exist. Only its contents are eligible.
     */
    fun contains(path: String): Boolean {
        if (path.isBlank()) return false
        val target = File(path).canonicalFileOrNull() ?: return false
        return canonicalRoots.any { root -> target.isStrictlyInside(root) }
    }

    /** Exposed for diagnostics and tests; never used to build a
     *  deletable list, since enumeration already does that. */
    fun roots(): List<File> = canonicalRoots

    private fun File.isStrictlyInside(root: File): Boolean {
        var parent = parentFile
        while (parent != null) {
            if (parent == root) return true
            parent = parent.parentFile
        }
        return false
    }

    /**
     * `canonicalFile` performs real I/O and throws on a path the
     * filesystem cannot resolve. A path we cannot canonicalise is a path
     * we cannot prove is inside the sandbox, so it fails the guard —
     * the safe direction for an unresolvable input.
     */
    private fun File.canonicalFileOrNull(): File? = try {
        canonicalFile
    } catch (e: java.io.IOException) {
        null
    } catch (e: SecurityException) {
        null
    }
}
