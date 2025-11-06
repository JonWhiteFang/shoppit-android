package com.shoppit.analysis.reporting

import com.shoppit.analysis.core.AnalysisResult
import java.io.File

/**
 * Interface for generating analysis reports.
 */
interface ReportGenerator {
    /**
     * Generates a Markdown report from analysis results.
     * 
     * @param result The analysis result
     * @param outputFile The file where the report will be written
     */
    fun generateReport(result: AnalysisResult, outputFile: File)
}
