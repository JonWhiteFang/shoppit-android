package com.shoppit.analysis.core

import com.shoppit.analysis.models.FileInfo
import java.io.File

/**
 * Interface for scanning source files.
 */
interface FileScanner {
    /**
     * Scans a directory and returns information about all Kotlin source files.
     * 
     * @param rootDir The root directory to scan
     * @return List of file information for all discovered Kotlin files
     */
    suspend fun scanFiles(rootDir: File): List<FileInfo>
}
