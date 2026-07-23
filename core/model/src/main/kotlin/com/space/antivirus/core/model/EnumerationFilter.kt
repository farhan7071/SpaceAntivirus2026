package com.space.antivirus.core.model

/**
 * Rules applied while resolving a ScanScope into concrete ScanTargets.
 * Filtering here is about WHICH ITEMS ARE CONSIDERED AT ALL (size limits,
 * excluded paths, hidden files) — never about whether an item is
 * dangerous. That distinction matters: this type must never grow a
 * "suspicious" or "risk" field, or enumeration would quietly start doing
 * detection's job.
 */
data class EnumerationFilter(
    val minSizeBytes: Long = 0L,
    val maxSizeBytes: Long? = null,
    val excludedPathPrefixes: List<String> = emptyList(),
    val includeHiddenFiles: Boolean = false,
) {
    init {
        require(minSizeBytes >= 0) { "minSizeBytes cannot be negative" }
        maxSizeBytes?.let {
            require(it >= minSizeBytes) { "maxSizeBytes ($it) cannot be less than minSizeBytes ($minSizeBytes)" }
        }
    }

    companion object {
        val DEFAULT = EnumerationFilter()
    }
}
