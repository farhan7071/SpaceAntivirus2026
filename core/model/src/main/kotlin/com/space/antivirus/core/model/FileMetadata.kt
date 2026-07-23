package com.space.antivirus.core.model

/**
 * What we know about one file or directory after enumeration — metadata
 * only. No file contents, no hashes, no scan verdict; that's later
 * sprints' job entirely (this sprint explicitly excludes hash databases
 * and signature matching).
 */
data class FileMetadata(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String?,
    val lastModifiedEpochMillis: Long,
    val isDirectory: Boolean,
) {
    init {
        require(path.isNotBlank()) { "path cannot be blank" }
        require(sizeBytes >= 0) { "sizeBytes cannot be negative" }
    }
}
