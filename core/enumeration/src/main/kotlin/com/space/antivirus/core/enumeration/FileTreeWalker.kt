package com.space.antivirus.core.enumeration

import com.space.antivirus.core.model.EnumerationFilter
import com.space.antivirus.core.model.FileMetadata
import java.io.File

/**
 * The actual file-traversal algorithm, deliberately isolated from any
 * Android API (Context, Environment, ContentResolver) so it's testable
 * with plain JUnit against real temp directories — no Robolectric, no
 * instrumentation, no emulator needed. Everything Android-specific
 * (resolving WHICH root path a ScanScope means) lives in
 * ScanScopePathResolver instead; this class only knows how to walk a
 * root File it's given.
 */
class FileTreeWalker {

    fun walk(root: File, filter: EnumerationFilter): List<FileMetadata> {
        if (!root.exists()) return emptyList()

        return root.walkTopDown()
            .onEnter { directory -> filter.includeHiddenFiles || !directory.isHidden }
            .filterNot { it == root } // the scope's own root isn't a scan target, only its contents are
            .filter { file -> filter.includeHiddenFiles || !file.isHidden }
            .filterNot { file -> isExcluded(file, filter.excludedPathPrefixes) }
            .filter { file -> matchesSize(file, filter) }
            .map { it.toFileMetadata() }
            .toList()
    }

    private fun isExcluded(file: File, excludedPrefixes: List<String>): Boolean {
        val path = file.path
        return excludedPrefixes.any { prefix -> path.startsWith(prefix) }
    }

    private fun matchesSize(file: File, filter: EnumerationFilter): Boolean {
        if (file.isDirectory) return true // directories themselves aren't size-filtered, only files within them
        val size = file.length()
        if (size < filter.minSizeBytes) return false
        filter.maxSizeBytes?.let { if (size > it) return false }
        return true
    }

    private fun File.toFileMetadata(): FileMetadata = FileMetadata(
        path = absolutePath,
        name = name,
        sizeBytes = if (isDirectory) 0L else length(),
        mimeType = if (isDirectory) null else guessMimeType(name),
        lastModifiedEpochMillis = lastModified(),
        isDirectory = isDirectory,
    )

    private fun guessMimeType(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        if (extension.isEmpty()) return null
        // Deliberately a small, static extension map rather than
        // android.webkit.MimeTypeMap — that's an Android framework class,
        // and this class is intentionally Android-free (see class KDoc).
        // A more complete mapping can replace this in a later sprint
        // without touching the traversal logic itself.
        return EXTENSION_MIME_TYPES[extension.lowercase()]
    }

    companion object {
        private val EXTENSION_MIME_TYPES = mapOf(
            "apk" to "application/vnd.android.package-archive",
            "zip" to "application/zip",
            "pdf" to "application/pdf",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "png" to "image/png",
            "mp4" to "video/mp4",
            "mp3" to "audio/mpeg",
            "txt" to "text/plain",
        )
    }
}
