package com.space.antivirus.core.enumeration

import com.google.common.truth.Truth.assertThat
import com.space.antivirus.core.model.EnumerationFilter
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Runs against real temporary directories on the local JVM — no
 * Robolectric, no emulator needed, since this class touches only
 * java.io.File (see FileTreeWalker's own KDoc on why it's kept
 * Android-free).
 */
class FileTreeWalkerTest {

    private lateinit var root: File
    private val walker = FileTreeWalker()

    @Before
    fun setUp() {
        root = File.createTempFile("enum-test", "").apply {
            delete()
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun `walks nested files and directories`() {
        File(root, "a.txt").writeText("hello")
        File(root, "sub").mkdirs()
        File(root, "sub/b.jpg").writeText("fake image bytes")

        val results = walker.walk(root, EnumerationFilter.DEFAULT)

        val paths = results.map { it.name }
        assertThat(paths).containsAtLeast("a.txt", "sub", "b.jpg")
    }

    @Test
    fun `returns empty list for a non-existent root`() {
        val missing = File(root, "does-not-exist")
        assertThat(walker.walk(missing, EnumerationFilter.DEFAULT)).isEmpty()
    }

    @Test
    fun `excludes hidden files by default`() {
        File(root, ".hidden").writeText("secret")
        File(root, "visible.txt").writeText("data")

        val results = walker.walk(root, EnumerationFilter.DEFAULT)

        assertThat(results.map { it.name }).containsExactly("visible.txt")
    }

    @Test
    fun `includes hidden files when the filter asks for them`() {
        File(root, ".hidden").writeText("secret")

        val results = walker.walk(root, EnumerationFilter(includeHiddenFiles = true))

        assertThat(results.map { it.name }).contains(".hidden")
    }

    @Test
    fun `respects minSizeBytes`() {
        File(root, "small.txt").writeText("x") // 1 byte
        File(root, "big.txt").writeText("x".repeat(1000))

        val results = walker.walk(root, EnumerationFilter(minSizeBytes = 500))

        assertThat(results.map { it.name }).containsExactly("big.txt")
    }

    @Test
    fun `respects excludedPathPrefixes`() {
        File(root, "keep.txt").writeText("data")
        File(root, "cache").mkdirs()
        File(root, "cache/temp.txt").writeText("data")

        val results = walker.walk(
            root,
            EnumerationFilter(excludedPathPrefixes = listOf(File(root, "cache").absolutePath)),
        )

        assertThat(results.map { it.name }).containsExactly("keep.txt")
    }

    @Test
    fun `guesses mime type from extension`() {
        File(root, "photo.jpg").writeText("data")

        val results = walker.walk(root, EnumerationFilter.DEFAULT)

        assertThat(results.first { it.name == "photo.jpg" }.mimeType).isEqualTo("image/jpeg")
    }

    @Test
    fun `directories have zero size and null mime type`() {
        File(root, "sub").mkdirs()

        val results = walker.walk(root, EnumerationFilter.DEFAULT)
        val dir = results.first { it.name == "sub" }

        assertThat(dir.isDirectory).isTrue()
        assertThat(dir.sizeBytes).isEqualTo(0L)
        assertThat(dir.mimeType).isNull()
    }
}
