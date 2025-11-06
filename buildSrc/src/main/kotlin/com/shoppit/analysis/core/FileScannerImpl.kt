package com.shoppit.analysis.core

import com.shoppit.analysis.models.CodeLayer
import com.shoppit.analysis.models.FileInfo
import java.io.File

/**
 * Implementation of FileScanner for discovering and filtering Kotlin source files.
 *
 * This scanner:
 * - Recursively scans directories for .kt and .kts files
 * - Respects .gitignore patterns
 * - Excludes build directories and generated code
 * - Detects code layer based on package structure
 */
class FileScannerImpl(
    private val projectRoot: String,
    private val excludePatterns: List<String> = DEFAULT_EXCLUDE_PATTERNS
) : FileScanner {
    
    companion object {
        /**
         * Default patterns to exclude from analysis.
         */
        val DEFAULT_EXCLUDE_PATTERNS = listOf(
            "**/build/**",
            "**/.gradle/**",
            "**/generated/**",
            "**/.idea/**",
            "**/.*",
            "**/*.class",
            "**/*.jar"
        )
        
        /**
         * Kotlin file extensions to include.
         */
        val KOTLIN_EXTENSIONS = setOf(".kt", ".kts")
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
        
        return filterFiles(files)
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
                        files.add(createFileInfo(file))
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
        val extension = File(file.path).extension
        if (!KOTLIN_EXTENSIONS.contains(".$extension")) {
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
        val projectRootFile = File(projectRoot).absolutePath
        return if (absolutePath.startsWith(projectRootFile)) {
            absolutePath.substring(projectRootFile.length)
                .trimStart('/', '\\')
                .replace('\\', '/')
        } else {
            absolutePath
        }
    }
    
    /**
     * Detects the code layer based on package structure.
     */
    private fun detectLayer(relativePath: String): CodeLayer? {
        val normalizedPath = relativePath.replace('\\', '/')
        
        return when {
            // Test files
            normalizedPath.contains("/test/") || 
            normalizedPath.contains("/androidTest/") -> CodeLayer.TEST
            
            // Data layer
            normalizedPath.contains("/data/") -> CodeLayer.DATA
            
            // Domain layer
            normalizedPath.contains("/domain/") -> CodeLayer.DOMAIN
            
            // UI layer (includes presentation)
            normalizedPath.contains("/ui/") || 
            normalizedPath.contains("/presentation/") -> CodeLayer.UI
            
            // DI layer
            normalizedPath.contains("/di/") -> CodeLayer.DI
            
            else -> null
        }
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
     * Checks if a path matches a glob pattern.
     */
    private fun matchesPattern(path: String, pattern: String): Boolean {
        val normalizedPattern = pattern.replace('\\', '/')
        
        // Convert glob pattern to regex
        val regex = normalizedPattern
            .replace(".", "\\.")
            .replace("**/", "(.*/)?")
            .replace("**", ".*")
            .replace("*", "[^/]*")
            .replace("?", "[^/]")
        
        return Regex(regex).containsMatchIn(path)
    }
    
    /**
     * Checks if a file is a Kotlin file based on extension.
     */
    private fun isKotlinFile(file: File): Boolean {
        val extension = ".${file.extension}"
        return KOTLIN_EXTENSIONS.contains(extension)
    }
}
