package com.space.antivirus.domain.cleaning

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.CleanableCategory
import com.space.antivirus.core.model.FileMetadata
import org.junit.Test

class JunkFileClassifierTest {

    private val classifier = JunkFileClassifier()
    private val now = 1_000_000_000_000L // arbitrary fixed "now" — determinism, no System.currentTimeMillis()

    private fun file(
        path: String,
        name: String = path.substringAfterLast('/'),
        sizeBytes: Long = 1_000L,
        lastModifiedEpochMillis: Long = now,
        isDirectory: Boolean = false,
    ) = FileMetadata(
        path = path,
        name = name,
        sizeBytes = sizeBytes,
        mimeType = null,
        lastModifiedEpochMillis = lastModifiedEpochMillis,
        isDirectory = isDirectory,
    )

    // --- CACHE_FILE ---

    @Test
    fun `a file inside a cache directory is CACHE_FILE`() {
        val result = classifier.classify(file("/data/data/com.example.app/cache/image_thumb.jpg"), now)

        assertThat(result?.category).isEqualTo(CleanableCategory.CACHE_FILE)
    }

    @Test
    fun `cache directory matching is case-insensitive`() {
        val result = classifier.classify(file("/data/data/com.example.app/CACHE/thumb.jpg"), now)

        assertThat(result?.category).isEqualTo(CleanableCategory.CACHE_FILE)
    }

    @Test
    fun `a file merely named 'cache' without the directory segment is not CACHE_FILE`() {
        // "cache.txt" as a filename, not inside a /cache/ directory —
        // must not match on substring alone.
        val result = classifier.classify(file("/storage/emulated/0/Documents/cache.txt"), now)

        assertThat(result).isNull()
    }

    // --- TEMPORARY_FILE ---

    @Test
    fun `tmp extension is TEMPORARY_FILE`() {
        val result = classifier.classify(file("/storage/emulated/0/Download/export.tmp"), now)

        assertThat(result?.category).isEqualTo(CleanableCategory.TEMPORARY_FILE)
    }

    @Test
    fun `bak extension is TEMPORARY_FILE`() {
        val result = classifier.classify(file("/storage/emulated/0/Documents/notes.bak"), now)

        assertThat(result?.category).isEqualTo(CleanableCategory.TEMPORARY_FILE)
    }

    @Test
    fun `temp extension matching is case-insensitive`() {
        val result = classifier.classify(file("/storage/emulated/0/Download/export.TMP"), now)

        assertThat(result?.category).isEqualTo(CleanableCategory.TEMPORARY_FILE)
    }

    // --- LOG_FILE ---

    @Test
    fun `log extension is LOG_FILE`() {
        val result = classifier.classify(file("/storage/emulated/0/Documents/crash.log"), now)

        assertThat(result?.category).isEqualTo(CleanableCategory.LOG_FILE)
    }

    // --- LEFTOVER_INSTALLER ---

    @Test
    fun `an apk in Downloads unmodified for over 24 hours is LEFTOVER_INSTALLER`() {
        val twoDaysAgo = now - (48L * 60 * 60 * 1000)
        val result = classifier.classify(
            file("/storage/emulated/0/Download/some-app.apk", lastModifiedEpochMillis = twoDaysAgo),
            now,
        )

        assertThat(result?.category).isEqualTo(CleanableCategory.LEFTOVER_INSTALLER)
    }

    @Test
    fun `an apk in Downloads modified less than 24 hours ago is NOT flagged - avoids catching an in-progress install`() {
        val oneHourAgo = now - (60 * 60 * 1000)
        val result = classifier.classify(
            file("/storage/emulated/0/Download/some-app.apk", lastModifiedEpochMillis = oneHourAgo),
            now,
        )

        assertThat(result).isNull()
    }

    @Test
    fun `exactly at the 24-hour threshold is flagged - boundary is inclusive`() {
        val exactlyThreshold = now - (24L * 60 * 60 * 1000)
        val result = classifier.classify(
            file("/storage/emulated/0/Download/some-app.apk", lastModifiedEpochMillis = exactlyThreshold),
            now,
        )

        assertThat(result?.category).isEqualTo(CleanableCategory.LEFTOVER_INSTALLER)
    }

    @Test
    fun `an old apk NOT in a Downloads path is not flagged as LEFTOVER_INSTALLER`() {
        val twoDaysAgo = now - (48L * 60 * 60 * 1000)
        // An APK elsewhere (e.g. app's own external files dir) isn't the
        // "leftover installer" pattern this rule targets.
        val result = classifier.classify(
            file("/storage/emulated/0/Android/data/com.example/files/backup.apk", lastModifiedEpochMillis = twoDaysAgo),
            now,
        )

        assertThat(result).isNull()
    }

    // --- negative cases ---

    @Test
    fun `an ordinary photo is not classified as anything`() {
        val result = classifier.classify(file("/storage/emulated/0/DCIM/Camera/photo.jpg"), now)

        assertThat(result).isNull()
    }

    @Test
    fun `a document in a normal folder is not classified as anything`() {
        val result = classifier.classify(file("/storage/emulated/0/Documents/report.pdf"), now)

        assertThat(result).isNull()
    }

    @Test
    fun `directories are never classified, even if their name matches a junk pattern`() {
        val result = classifier.classify(
            file("/data/data/com.example.app/cache", name = "cache", isDirectory = true),
            now,
        )

        assertThat(result).isNull()
    }

    // --- evidence and attribution ---

    @Test
    fun `every classified item carries a non-blank reason`() {
        val result = classifier.classify(file("/data/data/com.example.app/cache/thumb.jpg"), now)

        assertThat(result?.reason).isNotEmpty()
    }

    @Test
    fun `path, name, and size are carried through unchanged from the source FileMetadata`() {
        val result = classifier.classify(
            file("/storage/emulated/0/Documents/notes.bak", sizeBytes = 4_096L),
            now,
        )

        assertThat(result?.path).isEqualTo("/storage/emulated/0/Documents/notes.bak")
        assertThat(result?.name).isEqualTo("notes.bak")
        assertThat(result?.sizeBytes).isEqualTo(4_096L)
    }

    // --- determinism ---

    @Test
    fun `classifying the same file with the same now always produces an equal result`() {
        val target = file("/storage/emulated/0/Documents/notes.bak")

        val first = classifier.classify(target, now)
        val second = classifier.classify(target, now)

        assertThat(first).isEqualTo(second)
    }
}
