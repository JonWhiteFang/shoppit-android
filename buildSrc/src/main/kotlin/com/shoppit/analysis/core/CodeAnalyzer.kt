package com.shoppit.analysis.core

import com.shoppit.analysis.models.AnalysisCategory
import com.shoppit.analysis.models.FileInfo
import com.shoppit.analysis.models.Finding
import org.jetbrains.kotlin.psi.KtFile

/**
 * Interface for code analyzers that examine Kotlin source files.
 * Each analyzer focuses on a specific aspect of code quality.
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
     * @param ast Parsed Kotlin file AST
     * @return List of findings discovered in the file
     */
    suspend fun analyze(file: FileInfo, ast: KtFile): List<Finding>
    
    /**
     * Checks if this analyzer should be applied to the given file.
     *
     * @param file Metadata about the file
     * @return true if the analyzer should analyze this file, false otherwise
     */
    fun appliesTo(file: FileInfo): Boolean
}
