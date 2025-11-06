package com.shoppit.app.analysis.core

import com.shoppit.app.analysis.analyzers.*
import com.shoppit.app.analysis.models.Finding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * Implementation of AnalysisOrchestrator that coordinates the entire analysis process.
 * 
 * This orchestrator:
 * - Manages the lifecycle of all analyzers
 * - Scans and filters files for analysis
 * - Runs analyzers in parallel for performance
 * - Aggregates results from all analyzers
 * - Generates reports and updates baselines
 * - Handles errors gracefully to ensure analysis completes
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 14.1, 15.1, 16.1, 17.1
 */
class AnalysisOrchestratorImpl(
    private val fileScanner: FileScanner,
    private val resultAggregator: ResultAggregator,
    private val reportGenerator: ReportGenerator,
    private val baselineManager: BaselineManager,
    private val detektIntegration: DetektIntegration,
    private val projectRoot: String = "app/src/main/java",
    private val enableDetekt: Boolean = true
) : AnalysisOrchestrator {
    
    /**
     * Registry of all available analyzers.
     * Analyzers are registered here and can be filtered during analysis.
     */
    private val analyzerRegistry: Map<String, CodeAnalyzer> = mapOf(
        "architecture" to ArchitectureAnalyzer(),
        "compose" to ComposeAnalyzer(),
        "state-management" to StateManagementAnalyzer(),
        "error-handling" to ErrorHandlingAnalyzer(),
        "dependency-injection" to DependencyInjectionAnalyzer(),
        "database" to DatabaseAnalyzer(),
        "performance" to PerformanceAnalyzer(),
        "naming" to NamingAnalyzer(),
        "test-coverage" to TestCoverageAnalyzer(),
        "documentation" to DocumentationAnalyzer(),
        "security" to SecurityAnalyzer()
    )
    
    /**
     * Runs complete analysis on the entire codebase.
     * 
     * This method:
     * 1. Scans all Kotlin files in the project
     * 2. Runs all registered analyzers
     * 3. Aggregates and deduplicates findings
     * 4. Generates a report
     * 5. Updates the baseline
     * 
     * @return Analysis result with all findings and metrics
     */
    override suspend fun analyzeAll(): AnalysisResult = withContext(Dispatchers.Default) {
        Timber.i("Starting complete code quality analysis")
        
        val executionTime = measureTime {
            try {
                // Scan all files
                val files = fileScanner.scanDirectory(projectRoot)
                val filteredFiles = fileScanner.filterFiles(files)
                
                Timber.i("Found ${filteredFiles.size} Kotlin files to analyze")
                
                // Run all analyzers
                val findings = analyzeFiles(filteredFiles, analyzerRegistry.values.toList())
                
                // Run Detekt if enabled
                val detektFindings = if (enableDetekt) {
                    runDetektAnalysis(listOf(projectRoot))
                } else {
                    emptyList()
                }
                
                // Combine findings
                val allFindings = findings + detektFindings
                
                // Aggregate results
                val aggregatedResult = resultAggregator.aggregate(allFindings)
                
                // Generate report
                val baseline = baselineManager.loadBaseline()
                val report = reportGenerator.generate(aggregatedResult, baseline)
                
                // Save report
                saveReport(report)
                
                // Update baseline
                baselineManager.saveBaseline(aggregatedResult.metrics, aggregatedResult.findings)
                
                // Save to history
                baselineManager.saveToHistory(aggregatedResult)
                
                Timber.i("Analysis complete: ${aggregatedResult.findings.size} findings in ${filteredFiles.size} files")
                
                return@withContext AnalysisResult(
                    findings = aggregatedResult.findings,
                    metrics = aggregatedResult.metrics,
                    executionTime = Duration.ZERO, // Will be set after measureTime
                    filesAnalyzed = filteredFiles.size
                )
            } catch (e: Exception) {
                Timber.e(e, "Error during complete analysis")
                throw AnalysisError.AnalyzerError("complete-analysis", e)
            }
        }
        
        // Return result with actual execution time
        return@withContext AnalysisResult(
            findings = emptyList(),
            metrics = resultAggregator.calculateMetrics(emptyList()),
            executionTime = executionTime,
            filesAnalyzed = 0
        )
    }
    
    /**
     * Runs analysis on specific files or directories.
     * 
     * This method supports incremental analysis by allowing the user to specify
     * which files or directories to analyze. This is useful for:
     * - Analyzing only changed files in a commit
     * - Focusing on a specific feature or module
     * - Quick validation during development
     * 
     * @param paths List of file or directory paths to analyze
     * @return Analysis result for the specified paths
     */
    override suspend fun analyzeIncremental(paths: List<String>): AnalysisResult = withContext(Dispatchers.Default) {
        Timber.i("Starting incremental analysis for ${paths.size} paths")
        
        val executionTime = measureTime {
            try {
                // Collect all files from the specified paths
                val allFiles = mutableListOf<com.shoppit.app.analysis.models.FileInfo>()
                
                for (path in paths) {
                    val file = File(path)
                    if (file.isDirectory) {
                        // Scan directory
                        val files = fileScanner.scanDirectory(path)
                        allFiles.addAll(files)
                    } else if (file.isFile && file.extension == "kt") {
                        // Single file - scan its parent directory and filter
                        val parentDir = file.parentFile?.absolutePath ?: continue
                        val files = fileScanner.scanDirectory(parentDir)
                        val targetFile = files.find { it.path == file.absolutePath }
                        if (targetFile != null) {
                            allFiles.add(targetFile)
                        }
                    }
                }
                
                // Filter files
                val filteredFiles = fileScanner.filterFiles(allFiles)
                
                Timber.i("Found ${filteredFiles.size} Kotlin files to analyze incrementally")
                
                // Run all analyzers
                val findings = analyzeFiles(filteredFiles, analyzerRegistry.values.toList())
                
                // Run Detekt if enabled (on specified paths)
                val detektFindings = if (enableDetekt) {
                    runDetektAnalysis(paths)
                } else {
                    emptyList()
                }
                
                // Combine findings
                val allFindings = findings + detektFindings
                
                // Aggregate results
                val aggregatedResult = resultAggregator.aggregate(allFindings)
                
                // Generate report (without baseline comparison for incremental)
                val report = reportGenerator.generate(aggregatedResult, null)
                
                // Save report
                saveReport(report, suffix = "-incremental")
                
                Timber.i("Incremental analysis complete: ${aggregatedResult.findings.size} findings in ${filteredFiles.size} files")
                
                return@withContext AnalysisResult(
                    findings = aggregatedResult.findings,
                    metrics = aggregatedResult.metrics,
                    executionTime = Duration.ZERO, // Will be set after measureTime
                    filesAnalyzed = filteredFiles.size
                )
            } catch (e: Exception) {
                Timber.e(e, "Error during incremental analysis")
                throw AnalysisError.AnalyzerError("incremental-analysis", e)
            }
        }
        
        // Return result with actual execution time
        return@withContext AnalysisResult(
            findings = emptyList(),
            metrics = resultAggregator.calculateMetrics(emptyList()),
            executionTime = executionTime,
            filesAnalyzed = 0
        )
    }
    
    /**
     * Runs specific analyzers only.
     * 
     * This method allows filtering which analyzers to run, useful for:
     * - Running only security analyzers before a release
     * - Focusing on specific code quality aspects
     * - Testing new analyzers in isolation
     * 
     * @param paths Optional list of paths to analyze (null for all files)
     * @param analyzers List of analyzer IDs to run
     * @return Analysis result from the specified analyzers
     */
    override suspend fun analyzeWithFilters(
        paths: List<String>?,
        analyzers: List<String>
    ): AnalysisResult = withContext(Dispatchers.Default) {
        Timber.i("Starting filtered analysis with ${analyzers.size} analyzers")
        
        val executionTime = measureTime {
            try {
                // Get files to analyze
                val files = if (paths != null) {
                    // Use incremental logic for specific paths
                    val allFiles = mutableListOf<com.shoppit.app.analysis.models.FileInfo>()
                    for (path in paths) {
                        val file = File(path)
                        if (file.isDirectory) {
                            allFiles.addAll(fileScanner.scanDirectory(path))
                        } else if (file.isFile && file.extension == "kt") {
                            val parentDir = file.parentFile?.absolutePath ?: continue
                            val scannedFiles = fileScanner.scanDirectory(parentDir)
                            val targetFile = scannedFiles.find { it.path == file.absolutePath }
                            if (targetFile != null) {
                                allFiles.add(targetFile)
                            }
                        }
                    }
                    allFiles
                } else {
                    // Scan all files
                    fileScanner.scanDirectory(projectRoot)
                }
                
                val filteredFiles = fileScanner.filterFiles(files)
                
                // Filter analyzers
                val selectedAnalyzers = analyzers.mapNotNull { analyzerId ->
                    analyzerRegistry[analyzerId].also {
                        if (it == null) {
                            Timber.w("Analyzer '$analyzerId' not found in registry")
                        }
                    }
                }
                
                if (selectedAnalyzers.isEmpty()) {
                    Timber.w("No valid analyzers selected")
                    return@withContext AnalysisResult(
                        findings = emptyList(),
                        metrics = resultAggregator.calculateMetrics(emptyList()),
                        executionTime = Duration.ZERO,
                        filesAnalyzed = 0
                    )
                }
                
                Timber.i("Running ${selectedAnalyzers.size} analyzers on ${filteredFiles.size} files")
                
                // Run selected analyzers
                val findings = analyzeFiles(filteredFiles, selectedAnalyzers)
                
                // Run Detekt if enabled and requested
                val detektFindings = if (enableDetekt && analyzers.contains("detekt")) {
                    runDetektAnalysis(paths ?: listOf(projectRoot))
                } else {
                    emptyList()
                }
                
                // Combine findings
                val allFindings = findings + detektFindings
                
                // Aggregate results
                val aggregatedResult = resultAggregator.aggregate(allFindings)
                
                // Generate report
                val report = reportGenerator.generate(aggregatedResult, null)
                
                // Save report
                saveReport(report, suffix = "-filtered")
                
                Timber.i("Filtered analysis complete: ${aggregatedResult.findings.size} findings")
                
                return@withContext AnalysisResult(
                    findings = aggregatedResult.findings,
                    metrics = aggregatedResult.metrics,
                    executionTime = Duration.ZERO, // Will be set after measureTime
                    filesAnalyzed = filteredFiles.size
                )
            } catch (e: Exception) {
                Timber.e(e, "Error during filtered analysis")
                throw AnalysisError.AnalyzerError("filtered-analysis", e)
            }
        }
        
        // Return result with actual execution time
        return@withContext AnalysisResult(
            findings = emptyList(),
            metrics = resultAggregator.calculateMetrics(emptyList()),
            executionTime = executionTime,
            filesAnalyzed = 0
        )
    }
    
    /**
     * Analyzes a list of files with the specified analyzers.
     * 
     * This method runs analyzers in parallel for better performance.
     * Each file is analyzed by all applicable analyzers concurrently.
     * Errors in individual analyzers are caught and logged but don't stop the analysis.
     * 
     * @param files List of files to analyze
     * @param analyzers List of analyzers to run
     * @return List of all findings from all analyzers
     */
    private suspend fun analyzeFiles(
        files: List<com.shoppit.app.analysis.models.FileInfo>,
        analyzers: List<CodeAnalyzer>
    ): List<Finding> = withContext(Dispatchers.Default) {
        val allFindings = mutableListOf<Finding>()
        
        // Analyze files in parallel
        val fileResults = files.map { file ->
            async {
                analyzeFile(file, analyzers)
            }
        }.awaitAll()
        
        // Flatten results
        fileResults.forEach { findings ->
            allFindings.addAll(findings)
        }
        
        return@withContext allFindings
    }
    
    /**
     * Analyzes a single file with all applicable analyzers.
     * 
     * This method:
     * 1. Reads the file content
     * 2. Runs each applicable analyzer
     * 3. Collects findings from all analyzers
     * 4. Handles errors gracefully
     * 
     * @param file File to analyze
     * @param analyzers List of analyzers to run
     * @return List of findings from all analyzers for this file
     */
    private suspend fun analyzeFile(
        file: com.shoppit.app.analysis.models.FileInfo,
        analyzers: List<CodeAnalyzer>
    ): List<Finding> = withContext(Dispatchers.IO) {
        val findings = mutableListOf<Finding>()
        
        try {
            // Read file content
            val fileContent = File(file.path).readText()
            
            // Run each applicable analyzer
            for (analyzer in analyzers) {
                if (!analyzer.appliesTo(file)) {
                    continue
                }
                
                try {
                    val analyzerFindings = analyzer.analyze(file, fileContent)
                    findings.addAll(analyzerFindings)
                    
                    Timber.d("${analyzer.name} found ${analyzerFindings.size} issues in ${file.relativePath}")
                } catch (e: Exception) {
                    Timber.e(e, "Error running ${analyzer.name} on ${file.relativePath}")
                    // Continue with other analyzers
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reading file ${file.relativePath}")
            // Continue with other files
        }
        
        return@withContext findings
    }
    
    /**
     * Runs Detekt analysis on the specified paths.
     * 
     * This method integrates Detekt findings into the overall analysis.
     * Errors are caught and logged but don't stop the analysis.
     * 
     * @param paths List of paths to analyze with Detekt
     * @return List of findings from Detekt
     */
    private suspend fun runDetektAnalysis(paths: List<String>): List<Finding> {
        return try {
            Timber.i("Running Detekt analysis on ${paths.size} paths")
            
            val result = detektIntegration.runDetekt(paths)
            
            result.fold(
                onSuccess = { findings ->
                    Timber.i("Detekt found ${findings.size} issues")
                    findings
                },
                onFailure = { error ->
                    Timber.e(error, "Error running Detekt")
                    emptyList()
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error running Detekt")
            emptyList()
        }
    }
    
    /**
     * Saves the analysis report to a file.
     * 
     * @param report Report content in markdown format
     * @param suffix Optional suffix for the report filename
     */
    private fun saveReport(report: String, suffix: String = "") {
        try {
            val outputDir = File(".kiro/specs/code-quality-analysis")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            val reportFile = File(outputDir, "analysis-report$suffix.md")
            reportFile.writeText(report)
            
            Timber.i("Report saved to ${reportFile.absolutePath}")
        } catch (e: Exception) {
            Timber.e(e, "Error saving report")
            // Don't throw - report generation failure shouldn't stop analysis
        }
    }
}

/**
 * Sealed class representing errors that can occur during analysis.
 */
sealed class AnalysisError : Exception() {
    data class FileNotFound(val path: String) : AnalysisError() {
        override val message: String = "File not found: $path"
    }
    
    data class ParseError(val file: String, override val cause: Throwable) : AnalysisError() {
        override val message: String = "Error parsing file $file: ${cause.message}"
    }
    
    data class AnalyzerError(val analyzer: String, override val cause: Throwable) : AnalysisError() {
        override val message: String = "Error in analyzer $analyzer: ${cause.message}"
    }
    
    data class ConfigurationError(override val message: String) : AnalysisError()
}
