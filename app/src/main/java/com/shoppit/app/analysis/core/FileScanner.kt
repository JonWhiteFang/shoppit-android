package com.shoppit.app.analysis.core

import com.shoppit.app.analysis.models.FileInfo

/**
 * Interface for scanning and discovering Kotlin source files.
 */
interface FileScanner {
    /**
     * Scans a directory recursively for Kotlin files.
     *
     * @param path Path to the directory to scan
     * @return List of file information for all discovered Kotlin files
     */
    fun scanDirectory(path: String): List<FileInfo>
    
    /**
     * Filters files based on exclusion patterns.
     *
     * @param files List of files to filter
     * @return Filtered list of files
     */
    fun filterFiles(files: List<FileInfo>): List<FileInfo>
    
    /**
     * Checks if a file should be analyzed.
     *
     * @param file File information to check
     * @return true if the file should be analyzed, false otherwise
     */
    fun shouldAnalyze(file: FileInfo): Boolean
}
