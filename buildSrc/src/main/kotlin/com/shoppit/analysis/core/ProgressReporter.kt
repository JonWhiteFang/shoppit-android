package com.shoppit.analysis.core

/**
 * Interface for reporting analysis progress.
 * 
 * This allows the orchestrator to report progress to different outputs
 * (console, file, UI, etc.) without being tightly coupled to the output mechanism.
 * 
 * Requirements: 1.3
 */
interface ProgressReporter {
    /**
     * Reports the start of the analysis.
     * 
     * @param totalFiles Total number of files to analyze
     */
    fun onAnalysisStarted(totalFiles: Int)
    
    /**
     * Reports progress on file analysis.
     * 
     * @param currentFile Index of current file being analyzed (1-based)
     * @param totalFiles Total number of files
     * @param fileName Name of the file being analyzed
     */
    fun onFileAnalyzing(currentFile: Int, totalFiles: Int, fileName: String)
    
    /**
     * Reports completion of a file analysis.
     * 
     * @param fileName Name of the file that was analyzed
     * @param findingsCount Number of findings in this file
     */
    fun onFileAnalyzed(fileName: String, findingsCount: Int)
    
    /**
     * Reports the start of an analyzer execution.
     * 
     * @param analyzerName Name of the analyzer
     */
    fun onAnalyzerStarted(analyzerName: String)
    
    /**
     * Reports completion of an analyzer execution.
     * 
     * @param analyzerName Name of the analyzer
     * @param findingsCount Total findings from this analyzer
     */
    fun onAnalyzerCompleted(analyzerName: String, findingsCount: Int)
    
    /**
     * Reports the completion of the analysis.
     * 
     * @param totalFindings Total number of findings
     * @param filesAnalyzed Number of files analyzed
     * @param durationMs Duration in milliseconds
     */
    fun onAnalysisCompleted(totalFindings: Int, filesAnalyzed: Int, durationMs: Long)
    
    /**
     * Reports an error during analysis.
     * 
     * @param message Error message
     * @param throwable Optional throwable
     */
    fun onError(message: String, throwable: Throwable? = null)
}

/**
 * Console-based progress reporter that prints to stdout.
 * 
 * This implementation provides formatted console output with progress indicators.
 */
class ConsoleProgressReporter : ProgressReporter {
    
    override fun onAnalysisStarted(totalFiles: Int) {
        println("\n${"=".repeat(80)}")
        println("Starting Code Quality Analysis")
        println("${"=".repeat(80)}")
        println("Files to analyze: $totalFiles")
        println()
    }
    
    override fun onFileAnalyzing(currentFile: Int, totalFiles: Int, fileName: String) {
        val percentage = (currentFile * 100) / totalFiles
        val progressBar = buildProgressBar(percentage)
        print("\r[$progressBar] $percentage% - Analyzing: ${fileName.takeLast(50).padEnd(50)}")
    }
    
    override fun onFileAnalyzed(fileName: String, findingsCount: Int) {
        // Don't print individual file completion to avoid cluttering output
        // Progress is shown by onFileAnalyzing
    }
    
    override fun onAnalyzerStarted(analyzerName: String) {
        println("\n\n[ANALYZER] Running: $analyzerName")
    }
    
    override fun onAnalyzerCompleted(analyzerName: String, findingsCount: Int) {
        println("[ANALYZER] Completed: $analyzerName - Found $findingsCount issues")
    }
    
    override fun onAnalysisCompleted(totalFindings: Int, filesAnalyzed: Int, durationMs: Long) {
        println("\n\n${"=".repeat(80)}")
        println("Analysis Complete")
        println("${"=".repeat(80)}")
        println("Files analyzed:  $filesAnalyzed")
        println("Total findings:  $totalFindings")
        println("Duration:        ${formatDuration(durationMs)}")
        println("${"=".repeat(80)}\n")
    }
    
    override fun onError(message: String, throwable: Throwable?) {
        println("\n[ERROR] $message")
        throwable?.let {
            println("        ${it.message}")
        }
    }
    
    /**
     * Builds a progress bar string.
     * 
     * @param percentage Progress percentage (0-100)
     * @return Progress bar string
     */
    private fun buildProgressBar(percentage: Int): String {
        val barLength = 40
        val filledLength = (barLength * percentage) / 100
        val filled = "█".repeat(filledLength)
        val empty = "░".repeat(barLength - filledLength)
        return filled + empty
    }
    
    /**
     * Formats duration in milliseconds to human-readable string.
     * 
     * @param durationMs Duration in milliseconds
     * @return Formatted duration string
     */
    private fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        
        return when {
            minutes > 0 -> "${minutes}m ${remainingSeconds}s"
            seconds > 0 -> "${seconds}s"
            else -> "${durationMs}ms"
        }
    }
}

/**
 * Silent progress reporter that doesn't output anything.
 * 
 * Useful for testing or when progress reporting is not desired.
 */
class SilentProgressReporter : ProgressReporter {
    override fun onAnalysisStarted(totalFiles: Int) {}
    override fun onFileAnalyzing(currentFile: Int, totalFiles: Int, fileName: String) {}
    override fun onFileAnalyzed(fileName: String, findingsCount: Int) {}
    override fun onAnalyzerStarted(analyzerName: String) {}
    override fun onAnalyzerCompleted(analyzerName: String, findingsCount: Int) {}
    override fun onAnalysisCompleted(totalFindings: Int, filesAnalyzed: Int, durationMs: Long) {}
    override fun onError(message: String, throwable: Throwable?) {}
}
