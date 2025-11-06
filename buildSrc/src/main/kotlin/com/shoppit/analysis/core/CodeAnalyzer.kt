package com.shoppit.analysis.core

import com.shoppit.analysis.models.FileInfo
import com.shoppit.analysis.models.Finding

/**
 * Interface for code analyzers.
 * 
 * Each analyzer focuses on a specific aspect of code quality
 * (e.g., architecture, code smells, Compose patterns, etc.).
 */
interface CodeAnalyzer {
    /**
     * Unique identifier for this analyzer.
     */
    val id: String
    
    /**
     * Human-readable name for this analyzer.
     */
    val name: String
    
    /**
     * Analyzes a single file and returns findings.
     * 
     * @param fileInfo Information about the file to analyze
     * @param content The file content as a string
     * @return List of findings discovered in this file
     */
    suspend fun analyze(fileInfo: FileInfo, content: String): List<Finding>
}
