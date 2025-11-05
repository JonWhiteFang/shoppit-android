package com.shoppit.app.analysis.core

import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.FileInfo
import com.shoppit.app.analysis.models.Finding

/**
 * Interface for code analyzers that examine Kotlin source files.
 * Each analyzer focuses on a specific aspect of code quality.
 * 
 * Note: The AST parameter type will be added when Kotlin PSI dependency is integrated (Task 2.2).
 */
interface CodeAnalyzer {
    /**
     * Unique identifier for this analyzer.
     */
    val id: String
    
    /**
     * Human-readable name of the analyzer.
     */
    val name: String
    
    /**
     * Category of analysis performed by this analyzer.
     */
    val category: AnalysisCategory
    
    /**
     * Analyzes a single Kotlin file and returns findings.
     *
     * @param file Metadata about the file being analyzed
     * @param fileContent Content of the file as a string
     * @return List of findings discovered in the file
     * 
     * TODO: Replace fileContent parameter with KtFile AST in Task 2.2
     */
    suspend fun analyze(file: FileInfo, fileContent: String): List<Finding>
    
    /**
     * Checks if this analyzer should be applied to the given file.
     *
     * @param file Metadata about the file
     * @return true if the analyzer should analyze this file, false otherwise
     */
    fun appliesTo(file: FileInfo): Boolean
}
