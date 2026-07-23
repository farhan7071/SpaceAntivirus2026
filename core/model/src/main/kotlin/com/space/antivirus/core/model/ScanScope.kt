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
    data object ExternalStorage : ScanScope
    data object DownloadsFolder : ScanScope
    data object MediaCollection : ScanScope
    data class UserSelectedFolder(val path: String) : ScanScope
}
