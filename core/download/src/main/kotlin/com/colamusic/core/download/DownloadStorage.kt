package com.colamusic.core.download

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Single source of truth for the music download directory layout. */
@Singleton
class DownloadStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val root: File = File(context.filesDir, "music").apply { mkdirs() }

    /** Relative path (under root) we persist in the DB. */
    fun relativePathFor(songId: String, suffix: String?): String {
        val ext = suffix?.ifBlank { null }?.lowercase() ?: "bin"
        return "$songId.$ext"
    }

    fun fileFor(relativePath: String): File = File(root, relativePath)

    fun resolveOrNull(relativePath: String?): File? =
        relativePath?.takeIf { it.isNotBlank() }?.let { fileFor(it) }?.takeIf { it.exists() && it.length() > 0 }

    fun usedBytes(): Long = root.walkTopDown().filter { it.isFile }.map { it.length() }.sum()

    fun deleteFile(relativePath: String?) {
        if (relativePath.isNullOrBlank()) return
        val f = fileFor(relativePath)
        if (f.exists()) runCatching { f.delete() }
    }
}
