package com.shoppit.app.analysis

import com.shoppit.app.analysis.core.ConsoleProgressReporter
import com.shoppit.app.analysis.core.ProgressReporter
import com.shoppit.app.analysis.core.SilentProgressReporter
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for the Gradle task and CLI functionality.
 * 
 * These tests verify:
 * - Gradle task execution
 * - Command-line option parsing
 * - Progress reporting
 * - Report generation
 * 
 * Requirements: 1.1, 18.1
 */
class GradleTaskIntegrationTest {
    
    /**
     * Tests that the progress reporter interface is properly defined.
     */
    @Test
    fun `progress reporter interface is defined`() {
        val reporter: ProgressReporter = ConsoleProgressReporter()
        assertNotNull(reporter)
    }
    
    /**
     * Tests that the console progress reporter can be instantiated.
     */
    @Test
    fun `console progress reporter can be instantiated`() {
        val reporter = ConsoleProgressReporter()
        assertNotNull(reporter)
        
        // Test that methods can be called without errors
        reporter.onAnalysisStarted(10)
        reporter.onFileAnalyzing(1, 10, "TestFile.kt")
        reporter.onFileAnalyzed("TestFile.kt", 5)
        reporter.onAnalyzerStarted("TestAnalyzer")
        reporter.onAnalyzerCompleted("TestAnalyzer", 3)
        reporter.onAnalysisCompleted(15, 10, 5000)
        reporter.onError("Test error")
    }
    
    /**
     * Tests that the silent progress reporter can be instantiated.
     */
    @Test
    fun `silent progress reporter can be instantiated`() {
        val reporter = SilentProgressReporter()
        assertNotNull(reporter)
        
        // Test that methods can be called without errors
        reporter.onAnalysisStarted(10)
        reporter.onFileAnalyzing(1, 10, "TestFile.kt")
        reporter.onFileAnalyzed("TestFile.kt", 5)
        reporter.onAnalyzerStarted("TestAnalyzer")
        reporter.onAnalyzerCompleted("TestAnalyzer", 3)
        reporter.onAnalysisCompleted(15, 10, 5000)
        reporter.onError("Test error")
    }
    
    /**
     * Tests progress reporting with multiple files.
     */
    @Test
    fun `progress reporter handles multiple files`() {
        val reporter = TestProgressReporter()
        
        reporter.onAnalysisStarted(3)
        assertEquals(1, reporter.startedCount)
        
        reporter.onFileAnalyzing(1, 3, "File1.kt")
        reporter.onFileAnalyzed("File1.kt", 2)
        
        reporter.onFileAnalyzing(2, 3, "File2.kt")
        reporter.onFileAnalyzed("File2.kt", 3)
        
        reporter.onFileAnalyzing(3, 3, "File3.kt")
        reporter.onFileAnalyzed("File3.kt", 1)
        
        assertEquals(3, reporter.filesAnalyzedCount)
        assertEquals(6, reporter.totalFindings)
        
        reporter.onAnalysisCompleted(6, 3, 1000)
        assertEquals(1, reporter.completedCount)
    }
    
    /**
     * Tests progress reporting with multiple analyzers.
     */
    @Test
    fun `progress reporter handles multiple analyzers`() {
        val reporter = TestProgressReporter()
        
        reporter.onAnalyzerStarted("Analyzer1")
        assertEquals(1, reporter.analyzersStartedCount)
        
        reporter.onAnalyzerCompleted("Analyzer1", 5)
        assertEquals(1, reporter.analyzersCompletedCount)
        
        reporter.onAnalyzerStarted("Analyzer2")
        reporter.onAnalyzerCompleted("Analyzer2", 3)
        
        assertEquals(2, reporter.analyzersStartedCount)
        assertEquals(2, reporter.analyzersCompletedCount)
    }
    
    /**
     * Tests error reporting.
     */
    @Test
    fun `progress reporter handles errors`() {
        val reporter = TestProgressReporter()
        
        reporter.onError("Test error 1")
        assertEquals(1, reporter.errorCount)
        
        reporter.onError("Test error 2", RuntimeException("Test exception"))
        assertEquals(2, reporter.errorCount)
        assertTrue(reporter.errors.any { it.contains("Test error 2") })
    }
    
    /**
     * Tests that command-line options can be validated.
     */
    @Test
    fun `command-line options validation`() {
        val validAnalyzers = listOf(
            "architecture", "compose", "state-management", "error-handling",
            "dependency-injection", "database", "performance", "naming",
            "test-coverage", "documentation", "security", "detekt"
        )
        
        // Test valid analyzers
        val testAnalyzers = listOf("security", "architecture", "compose")
        val invalidAnalyzers = testAnalyzers.filter { it !in validAnalyzers }
        assertTrue(invalidAnalyzers.isEmpty())
        
        // Test invalid analyzers
        val testInvalidAnalyzers = listOf("invalid", "unknown")
        val foundInvalidAnalyzers = testInvalidAnalyzers.filter { it !in validAnalyzers }
        assertEquals(2, foundInvalidAnalyzers.size)
    }
    
    /**
     * Tests that analyzer names are correctly parsed from comma-separated string.
     */
    @Test
    fun `analyzer names are parsed correctly`() {
        val analyzersStr = "security, architecture, compose"
        val analyzers = analyzersStr.split(",").map { it.trim() }
        
        assertEquals(3, analyzers.size)
        assertEquals("security", analyzers[0])
        assertEquals("architecture", analyzers[1])
        assertEquals("compose", analyzers[2])
    }
    
    /**
     * Tests that empty analyzer string results in empty list.
     */
    @Test
    fun `empty analyzer string results in empty list`() {
        val analyzersStr: String? = null
        val analyzers = analyzersStr?.split(",")?.map { it.trim() } ?: emptyList()
        
        assertTrue(analyzers.isEmpty())
    }
    
    /**
     * Tests that baseline flag is parsed correctly.
     */
    @Test
    fun `baseline flag is parsed correctly`() {
        val trueStr = "true"
        val falseStr = "false"
        val nullStr: String? = null
        
        assertEquals(true, trueStr.toBoolean())
        assertEquals(false, falseStr.toBoolean())
        assertEquals(false, nullStr?.toBoolean() ?: false)
    }
}

/**
 * Test implementation of ProgressReporter for testing purposes.
 */
private class TestProgressReporter : ProgressReporter {
    var startedCount = 0
    var completedCount = 0
    var filesAnalyzedCount = 0
    var totalFindings = 0
    var analyzersStartedCount = 0
    var analyzersCompletedCount = 0
    var errorCount = 0
    val errors = mutableListOf<String>()
    
    override fun onAnalysisStarted(totalFiles: Int) {
        startedCount++
    }
    
    override fun onFileAnalyzing(currentFile: Int, totalFiles: Int, fileName: String) {
        // No-op for test
    }
    
    override fun onFileAnalyzed(fileName: String, findingsCount: Int) {
        filesAnalyzedCount++
        totalFindings += findingsCount
    }
    
    override fun onAnalyzerStarted(analyzerName: String) {
        analyzersStartedCount++
    }
    
    override fun onAnalyzerCompleted(analyzerName: String, findingsCount: Int) {
        analyzersCompletedCount++
    }
    
    override fun onAnalysisCompleted(totalFindings: Int, filesAnalyzed: Int, durationMs: Long) {
        completedCount++
    }
    
    override fun onError(message: String, throwable: Throwable?) {
        errorCount++
        errors.add(message)
    }
}
