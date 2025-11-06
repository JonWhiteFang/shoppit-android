package com.shoppit.app.analysis.baseline

import com.shoppit.app.analysis.core.AggregatedResult
import com.shoppit.app.analysis.core.Baseline
import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.AnalysisMetrics
import com.shoppit.app.analysis.models.Effort
import com.shoppit.app.analysis.models.Finding
import com.shoppit.app.analysis.models.Priority
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class BaselineManagerImplTest {
    
    private lateinit var tempDir: File
    private lateinit var baselineManager: BaselineManagerImpl
    
    @Before
    fun setup() {
        // Create temporary directory for tests
        tempDir = File.createTempFile("baseline_test", "").apply {
            delete()
            mkdirs()
        }
        
        baselineManager = BaselineManagerImpl(tempDir.absolutePath)
    }
    
    @After
    fun teardown() {
        // Clean up temporary directory
        tempDir.deleteRecursively()
    }
    
    @Test
    fun `loadBaseline returns null when no baseline exists`() {
        // When
        val baseline = baselineManager.loadBaseline()
        
        // Then
        assertNull(baseline)
    }
    
    @Test
    fun `saveBaseline creates baseline file`() {
        // Given
        val metrics = createTestMetrics()
        val findings = createTestFindings()
        
        // When
        baselineManager.saveBaseline(metrics, findings)
        
        // Then
        val baselineFile = File(tempDir, "baseline.json")
        assertTrue(baselineFile.exists())
    }
    
    @Test
    fun `saveBaseline and loadBaseline round trip`() {
        // Given
        val metrics = createTestMetrics()
        val findings = createTestFindings()
        
        // When
        baselineManager.saveBaseline(metrics, findings)
        val loadedBaseline = baselineManager.loadBaseline()
        
        // Then
        assertNotNull(loadedBaseline)
        assertEquals(metrics.totalFiles, loadedBaseline!!.metrics.totalFiles)
        assertEquals(metrics.totalFindings, loadedBaseline.metrics.totalFindings)
        assertEquals(findings.size, loadedBaseline.findingIds.size)
        assertTrue(loadedBaseline.findingIds.containsAll(findings.map { it.id }))
    }
    
    @Test
    fun `compare detects improvements in priority counts`() {
        // Given
        val baselineMetrics = createTestMetrics(
            criticalCount = 5,
            highCount = 10,
            mediumCount = 20,
            lowCount = 30
        )
        val baseline = Baseline(
            timestamp = System.currentTimeMillis(),
            metrics = baselineMetrics,
            findingIds = setOf("1", "2", "3")
        )
        
        val currentMetrics = createTestMetrics(
            criticalCount = 2, // Improved
            highCount = 8,     // Improved
            mediumCount = 20,  // No change
            lowCount = 30      // No change
        )
        
        // When
        val comparison = baselineManager.compare(currentMetrics, baseline)
        
        // Then
        assertTrue(comparison.improved.containsKey("critical_issues"))
        assertTrue(comparison.improved.containsKey("high_issues"))
        assertEquals(60.0, comparison.improved["critical_issues"]!!, 0.1) // (5-2)/5 * 100
        assertEquals(20.0, comparison.improved["high_issues"]!!, 0.1)     // (10-8)/10 * 100
    }
    
    @Test
    fun `compare detects regressions in priority counts`() {
        // Given
        val baselineMetrics = createTestMetrics(
            criticalCount = 2,
            highCount = 5,
            mediumCount = 10,
            lowCount = 15
        )
        val baseline = Baseline(
            timestamp = System.currentTimeMillis(),
            metrics = baselineMetrics,
            findingIds = setOf("1", "2", "3")
        )
        
        val currentMetrics = createTestMetrics(
            criticalCount = 5, // Regressed
            highCount = 10,    // Regressed
            mediumCount = 10,  // No change
            lowCount = 15      // No change
        )
        
        // When
        val comparison = baselineManager.compare(currentMetrics, baseline)
        
        // Then
        assertTrue(comparison.regressed.containsKey("critical_issues"))
        assertTrue(comparison.regressed.containsKey("high_issues"))
        assertEquals(150.0, comparison.regressed["critical_issues"]!!, 0.1) // (5-2)/2 * 100
        assertEquals(100.0, comparison.regressed["high_issues"]!!, 0.1)     // (10-5)/5 * 100
    }
    
    @Test
    fun `compare detects improvements in complexity`() {
        // Given
        val baselineMetrics = createTestMetrics(averageComplexity = 10.0)
        val baseline = Baseline(
            timestamp = System.currentTimeMillis(),
            metrics = baselineMetrics,
            findingIds = emptySet()
        )
        
        val currentMetrics = createTestMetrics(averageComplexity = 8.0)
        
        // When
        val comparison = baselineManager.compare(currentMetrics, baseline)
        
        // Then
        assertTrue(comparison.improved.containsKey("average_complexity"))
        assertEquals(20.0, comparison.improved["average_complexity"]!!, 0.1) // (10-8)/10 * 100
    }
    
    @Test
    fun `compare detects regressions in complexity`() {
        // Given
        val baselineMetrics = createTestMetrics(averageComplexity = 8.0)
        val baseline = Baseline(
            timestamp = System.currentTimeMillis(),
            metrics = baselineMetrics,
            findingIds = emptySet()
        )
        
        val currentMetrics = createTestMetrics(averageComplexity = 10.0)
        
        // When
        val comparison = baselineManager.compare(currentMetrics, baseline)
        
        // Then
        assertTrue(comparison.regressed.containsKey("average_complexity"))
        assertEquals(25.0, comparison.regressed["average_complexity"]!!, 0.1) // (10-8)/8 * 100
    }
    
    @Test
    fun `compare detects improvements in test coverage`() {
        // Given
        val baselineMetrics = createTestMetrics(testCoverage = 60.0)
        val baseline = Baseline(
            timestamp = System.currentTimeMillis(),
            metrics = baselineMetrics,
            findingIds = emptySet()
        )
        
        val currentMetrics = createTestMetrics(testCoverage = 75.0)
        
        // When
        val comparison = baselineManager.compare(currentMetrics, baseline)
        
        // Then
        assertTrue(comparison.improved.containsKey("test_coverage"))
        assertEquals(15.0, comparison.improved["test_coverage"]!!, 0.1) // 75 - 60
    }
    
    @Test
    fun `compareFindings identifies resolved issues`() {
        // Given
        val baseline = Baseline(
            timestamp = System.currentTimeMillis(),
            metrics = createTestMetrics(),
            findingIds = setOf("finding-1", "finding-2", "finding-3")
        )
        
        val currentFindings = listOf(
            createTestFinding(id = "finding-2"),
            createTestFinding(id = "finding-4")
        )
        
        // When
        val comparison = baselineManager.compareFindings(currentFindings, baseline)
        
        // Then
        assertEquals(2, comparison.resolved.size)
        assertTrue(comparison.resolved.contains("finding-1"))
        assertTrue(comparison.resolved.contains("finding-3"))
    }
    
    @Test
    fun `compareFindings identifies new issues`() {
        // Given
        val baseline = Baseline(
            timestamp = System.currentTimeMillis(),
            metrics = createTestMetrics(),
            findingIds = setOf("finding-1", "finding-2")
        )
        
        val currentFindings = listOf(
            createTestFinding(id = "finding-1"),
            createTestFinding(id = "finding-3"),
            createTestFinding(id = "finding-4")
        )
        
        // When
        val comparison = baselineManager.compareFindings(currentFindings, baseline)
        
        // Then
        assertEquals(2, comparison.newIssues.size)
        assertTrue(comparison.newIssues.contains("finding-3"))
        assertTrue(comparison.newIssues.contains("finding-4"))
    }
    
    @Test
    fun `saveToHistory creates history file`() {
        // Given
        val result = createTestAggregatedResult()
        
        // When
        baselineManager.saveToHistory(result)
        
        // Then
        val historyDir = File(tempDir, "history")
        assertTrue(historyDir.exists())
        assertTrue(historyDir.listFiles()?.isNotEmpty() == true)
    }
    
    @Test
    fun `saveToHistory includes comparison when baseline exists`() {
        // Given
        val baselineMetrics = createTestMetrics(criticalCount = 5)
        val baselineFindings = createTestFindings()
        baselineManager.saveBaseline(baselineMetrics, baselineFindings)
        
        val currentResult = createTestAggregatedResult(
            metrics = createTestMetrics(criticalCount = 3)
        )
        
        // When
        baselineManager.saveToHistory(currentResult)
        
        // Then
        val history = baselineManager.loadHistory()
        assertEquals(1, history.size)
        assertNotNull(history[0].comparison)
        assertTrue(history[0].comparison!!.improved.isNotEmpty())
    }
    
    @Test
    fun `loadHistory returns entries sorted by timestamp`() {
        // Given
        val result1 = createTestAggregatedResult()
        val result2 = createTestAggregatedResult()
        val result3 = createTestAggregatedResult()
        
        // When
        baselineManager.saveToHistory(result1)
        Thread.sleep(10) // Ensure different timestamps
        baselineManager.saveToHistory(result2)
        Thread.sleep(10)
        baselineManager.saveToHistory(result3)
        
        val history = baselineManager.loadHistory()
        
        // Then
        assertEquals(3, history.size)
        // Verify sorted by timestamp (newest first)
        assertTrue(history[0].timestamp >= history[1].timestamp)
        assertTrue(history[1].timestamp >= history[2].timestamp)
    }
    
    @Test
    fun `loadHistory returns empty list when no history exists`() {
        // When
        val history = baselineManager.loadHistory()
        
        // Then
        assertTrue(history.isEmpty())
    }
    
    @Test
    fun `clearBaseline removes baseline file`() {
        // Given
        baselineManager.saveBaseline(createTestMetrics(), createTestFindings())
        assertTrue(File(tempDir, "baseline.json").exists())
        
        // When
        baselineManager.clearBaseline()
        
        // Then
        assertFalse(File(tempDir, "baseline.json").exists())
    }
    
    @Test
    fun `clearHistory removes all history files`() {
        // Given
        baselineManager.saveToHistory(createTestAggregatedResult())
        baselineManager.saveToHistory(createTestAggregatedResult())
        val historyDir = File(tempDir, "history")
        assertTrue(historyDir.listFiles()?.isNotEmpty() == true)
        
        // When
        baselineManager.clearHistory()
        
        // Then
        assertTrue(historyDir.listFiles()?.isEmpty() == true)
    }
    
    // Helper functions
    
    private fun createTestMetrics(
        totalFiles: Int = 100,
        totalFindings: Int = 50,
        criticalCount: Int = 2,
        highCount: Int = 5,
        mediumCount: Int = 15,
        lowCount: Int = 28,
        averageComplexity: Double = 8.5,
        averageFunctionLength: Double = 25.0,
        averageClassLength: Double = 150.0,
        testCoverage: Double = 70.0,
        docCoverage: Double = 60.0
    ): AnalysisMetrics {
        return AnalysisMetrics(
            totalFiles = totalFiles,
            totalFindings = totalFindings,
            findingsByPriority = mapOf(
                Priority.CRITICAL to criticalCount,
                Priority.HIGH to highCount,
                Priority.MEDIUM to mediumCount,
                Priority.LOW to lowCount
            ),
            findingsByCategory = mapOf(
                AnalysisCategory.CODE_SMELL to 20,
                AnalysisCategory.ARCHITECTURE to 10,
                AnalysisCategory.COMPOSE to 8,
                AnalysisCategory.STATE_MANAGEMENT to 5,
                AnalysisCategory.ERROR_HANDLING to 7
            ),
            averageComplexity = averageComplexity,
            averageFunctionLength = averageFunctionLength,
            averageClassLength = averageClassLength,
            testCoveragePercentage = testCoverage,
            documentationCoveragePercentage = docCoverage
        )
    }
    
    private fun createTestFinding(
        id: String = "test-finding-1",
        priority: Priority = Priority.MEDIUM
    ): Finding {
        return Finding(
            id = id,
            analyzer = "test-analyzer",
            category = AnalysisCategory.CODE_SMELL,
            priority = priority,
            title = "Test Finding",
            description = "This is a test finding",
            file = "TestFile.kt",
            lineNumber = 42,
            codeSnippet = "val test = \"test\"",
            recommendation = "Fix this issue",
            effort = Effort.SMALL
        )
    }
    
    private fun createTestFindings(): List<Finding> {
        return listOf(
            createTestFinding(id = "finding-1", priority = Priority.CRITICAL),
            createTestFinding(id = "finding-2", priority = Priority.HIGH),
            createTestFinding(id = "finding-3", priority = Priority.MEDIUM),
            createTestFinding(id = "finding-4", priority = Priority.LOW)
        )
    }
    
    private fun createTestAggregatedResult(
        metrics: AnalysisMetrics = createTestMetrics()
    ): AggregatedResult {
        val findings = createTestFindings()
        return AggregatedResult(
            findings = findings,
            metrics = metrics,
            byCategory = findings.groupBy { it.category },
            byPriority = findings.groupBy { it.priority },
            byFile = findings.groupBy { it.file }
        )
    }
}
