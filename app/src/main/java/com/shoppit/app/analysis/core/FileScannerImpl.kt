package com.shoppit.app.analysis.core

import com.shoppit.app.analysis.models.CodeLayer
import com.shoppit.app.analysis.models.FileInfo
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.pathString

/**
 * Implementation of FileScanner that discovers Kotlin source files.
 *
 * Features:
 * - Recursive directory scanning
 * - File filtering based on extensions (.kt, .kts)
 * - Exclusion pattern matching (.gitignore, build directories)
 * - Layer detection based on package structure
 */
class FileScannerImpl(
    private val projectRoot: String,
    private val excludePatterns: List<String> = DEFAULT_EXCLUDE_PATTERNS
) : FileScanner {
    
    companion object {
        /**
         * Default patterns to exclude from scanning.
         */
        val DEFAULT_EXCLUDE_PATTERNS = listOf(
            "**/build/**",
            "**/.gradle/**",
            "**/generated/**",
            "**/.idea/**",
            "**/.git/**",
            "**/bin/**",
            "**/out/**"
        )
        
        /**
         * Kotlin file extensions to scan.
         */
        private val KOTLIN_EXTENSIONS = setOf("kt", "kts")
    }
    
    /**
     * Scans a directory recursively for Kotlin files.
     */
    override fun scanDirectory(path: String): List<FileInfo> {
        val directory = File(path)
        
        if (!directory.exists()) {
            throw IllegalArgumentException("Directory does not exist: $path")
        }
        
        if (!directory.isDirectory) {
            throw IllegalArgumentException("Path is not a directory: $path")
        }
        
        val files = mutableListOf<FileInfo>()
        scanDirectoryRecursive(directory, files)
        
        return files
    }
    
    /**
     * Recursively scans a directory and collects Kotlin files.
     */
    private fun scanDirectoryRecursive(directory: File, files: MutableList<FileInfo>) {
        directory.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> {
                    // Skip excluded directories
                    if (!isExcluded(file.absolutePath)) {
                        scanDirectoryRecursive(file, files)
                    }
                }
                file.isFile && isKotlinFile(file) -> {
                    if (!isExcluded(file.absolutePath)) {
                        val fileInfo = createFileInfo(file)
                        files.add(fileInfo)
                    }
                }
            }
        }
    }
    
    /**
     * Filters files based on exclusion patterns.
     */
    override fun filterFiles(files: List<FileInfo>): List<FileInfo> {
        return files.filter { shouldAnalyze(it) }
    }
    
    /**
     * Checks if a file should be analyzed.
     */
    override fun shouldAnalyze(file: FileInfo): Boolean {
        // Check if file is excluded by patterns
        if (isExcluded(file.path)) {
            return false
        }
        
        // Check if file is a Kotlin file
        val extension = file.path.substringAfterLast('.', "")
        if (extension !in KOTLIN_EXTENSIONS) {
            return false
        }
        
        return true
    }
    
    /**
     * Creates FileInfo from a File object.
     */
    private fun createFileInfo(file: File): FileInfo {
        val absolutePath = file.absolutePath
        val relativePath = getRelativePath(absolutePath)
        val layer = detectLayer(relativePath)
        
        return FileInfo(
            path = absolutePath,
            relativePath = relativePath,
            size = file.length(),
            lastModified = file.lastModified(),
            layer = layer
        )
    }
    
    /**
     * Gets the relative path from project root.
     */
    private fun getRelativePath(absolutePath: String): String {
        val projectRootPath = Paths.get(projectRoot).normalize()
        val filePath = Paths.get(absolutePath).normalize()
        
        return try {
            projectRootPath.relativize(filePath).pathString
        } catch (e: IllegalArgumentException) {
            // If paths are on different drives or can't be relativized, return absolute path
            absolutePath
        }
    }
    
    /**
     * Detects the code layer based on package structure.
     *
     * Layer detection rules:
     * - /data/ -> DATA layer
     * - /domain/ -> DOMAIN layer
     * - /ui/ or /presentation/ -> UI layer
     * - /di/ -> DI layer
     * - /test/ or /androidTest/ -> TEST layer
     */
    private fun detectLayer(relativePath: String): CodeLayer? {
        val normalizedPath = relativePath.replace('\\', '/')
        
        return when {
            normalizedPath.contains("/data/") -> CodeLayer.DATA
            normalizedPath.contains("/domain/") -> CodeLayer.DOMAIN
            normalizedPath.contains("/ui/") || normalizedPath.contains("/presentation/") -> CodeLayer.UI
            normalizedPath.contains("/di/") -> CodeLayer.DI
            normalizedPath.contains("/test/") || normalizedPath.contains("/androidTest/") -> CodeLayer.TEST
            else -> null
        }
    }
    
    /**
     * Checks if a file is a Kotlin file based on extension.
     */
    private fun isKotlinFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in KOTLIN_EXTENSIONS
    }
    
    /**
     * Checks if a path matches any exclusion pattern.
     */
    private fun isExcluded(path: String): Boolean {
        val normalizedPath = path.replace('\\', '/')
        
        return excludePatterns.any { pattern ->
            matchesPattern(normalizedPath, pattern)
        }
    }
    
    /**
     * Matches a path against a glob pattern.
     *
     * Supports:
     * - ** for matching any number of directories
     * - * for matching any characters within a directory
     */
    private fun matchesPattern(path: String, pattern: String): Boolean {
        val normalizedPattern = pattern.replace('\\', '/')
        
        // Convert glob pattern to regex
        val regexPattern = normalizedPattern
            .replace(".", "\\.")
            .replace("**", "DOUBLE_STAR")
            .replace("*", "[^/]*")
            .replace("DOUBLE_STAR", ".*")
        
        return path.matches(Regex(regexPattern))
    }
}
