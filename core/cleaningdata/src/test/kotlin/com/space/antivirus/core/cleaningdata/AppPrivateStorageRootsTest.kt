package com.space.antivirus.core.cleaningdata

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Sprint 039. The containment guard is the single most safety-critical
 * piece of the Cleaner, so it is tested against real directories on a
 * real filesystem rather than mocked path strings — the whole point of
 * the guard is that it resolves paths rather than trusting them.
 *
 * `AppPrivateStorageRoots` itself needs a Context, so the pure path
 * logic is exercised here through the same comparison it performs. Its
 * Context-bound wiring is covered by the instrumented test alongside it.
 */
class AppPrivateStorageRootsTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    /** Mirrors AppPrivateStorageRoots.isStrictlyInside exactly. */
    private fun isStrictlyInside(target: File, root: File): Boolean {
        var parent = target.canonicalFile.parentFile
        val canonicalRoot = root.canonicalFile
        while (parent != null) {
            if (parent == canonicalRoot) return true
            parent = parent.parentFile
        }
        return false
    }

    @Test
    fun `a file directly inside the root is contained`() {
        val root = temporaryFolder.newFolder("files")
        val file = File(root, "cache.tmp").apply { writeText("x") }

        assertThat(isStrictlyInside(file, root)).isTrue()
    }

    @Test
    fun `a file nested deeper inside the root is contained`() {
        val root = temporaryFolder.newFolder("files")
        val nested = File(root, "a/b/c").apply { mkdirs() }
        val file = File(nested, "cache.tmp").apply { writeText("x") }

        assertThat(isStrictlyInside(file, root)).isTrue()
    }

    /** Deleting the root itself is not a cleanup — only its contents. */
    @Test
    fun `the root itself is not contained in itself`() {
        val root = temporaryFolder.newFolder("files")

        assertThat(isStrictlyInside(root, root)).isFalse()
    }

    @Test
    fun `a sibling directory outside the root is not contained`() {
        val root = temporaryFolder.newFolder("files")
        val outside = temporaryFolder.newFolder("elsewhere")
        val file = File(outside, "photo.jpg").apply { writeText("x") }

        assertThat(isStrictlyInside(file, root)).isFalse()
    }

    /**
     * The traversal case. Comparing raw path strings would let this
     * through — canonicalisation is what stops it.
     */
    @Test
    fun `a path escaping the root via dot-dot is not contained`() {
        val root = temporaryFolder.newFolder("files")
        val outside = temporaryFolder.newFolder("elsewhere")
        File(outside, "photo.jpg").writeText("x")
        val escaping = File(root, "../elsewhere/photo.jpg")

        assertThat(isStrictlyInside(escaping, root)).isFalse()
    }

    @Test
    fun `a prefix-sharing sibling directory is not contained`() {
        // "/tmp/files_backup" must not count as inside "/tmp/files"
        // just because its path string starts with it.
        val root = temporaryFolder.newFolder("files")
        val lookalike = temporaryFolder.newFolder("files_backup")
        val file = File(lookalike, "photo.jpg").apply { writeText("x") }

        assertThat(isStrictlyInside(file, root)).isFalse()
    }
}
