package com.shoppit.analysis.models

import java.io.File

/**
 * Information about a source file.
 */
data class FileInfo(
    val file: File,
    val relativePath: String,
    val packageName: String,
    val isTest: Boolean = false
) {
    val name: String get() = file.name
    val extension: String get() = file.extension
    val absolutePath: String get() = file.absolutePath
}
