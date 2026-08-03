package com.space.antivirus.core.model

/**
 * What a scan should look at — the answer to "where do we look", not
 * "what do we look for" (that's a later sprint's job entirely). Each case
 * is a location/category, not a concrete list of items yet; resolving a
 * ScanScope into actual ScanTargets is what EnumerationRepository does.
 */
sealed interface ScanScope {
    data object InstalledApplications : ScanScope
    data object InternalStorage : ScanScope

    /**
     * The app's own cache directories (internal + external), added in
     * Sprint 039 when the Cleaner gained real deletion.
     *
     * Deliberately a distinct scope rather than folding cache into
     * InternalStorage: `filesDir` holds data the app is expected to
     * keep, `cacheDir` holds data Android itself documents as
     * discardable at any time. A cleaner should treat those two very
     * differently, and a scope that conflated them would remove the
     * ability to.
     */
    data object ApplicationCache : ScanScope
    data object ExternalStorage : ScanScope
    data object DownloadsFolder : ScanScope
    data object MediaCollection : ScanScope
    data class UserSelectedFolder(val path: String) : ScanScope
}
