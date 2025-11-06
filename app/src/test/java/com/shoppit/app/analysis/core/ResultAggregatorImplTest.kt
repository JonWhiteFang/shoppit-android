package com.shoppit.app.analysis.core

import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.Effort
import com.shoppit.app.analysis.models.Finding
import com.shoppit.app.analysis.models.Priority
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ResultAggregatorImpl.
 *
 * Tests cover:
 * - Finding aggregation
 * - Deduplication logic
 * - Priority assignment
 * - Metrics calculation
 */
class ResultAggregatorImplTest {
    
    private lateinit var aggregator: ResultAggregatorImpl
    
    @Before
    fun setup() {
        aggregator = ResultAggregatorImpl()
    }
    
    // ========== Deduplication Tests ==========
    
    @Test
    fun `deduplicate removes exact duplicates`() {
        // Given: Two identical findings
        val finding1 = createFinding(
            id = "1",
            file = "Test.kt",
            lineNumber = 10,
            category = AnalysisCategory.CODE_SMELL,
            title = "Long function",
            priority = Priority.MEDIUM
        )
        val finding2 = createFinding(
            id = "2",
            file = "Test.kt",
            lineNumber = 10,
            category = AnalysisCategory.CODE_SMELL,
            title = "Long function",
            priority = Priority.MEDIUM
        )
        
        // When: Deduplicate
        val result = aggregator.deduplicate(listOf(finding1, finding2))
        
        // Then: Only one finding should remain
        assertEquals(1, result.size)
    }
    
    @Test
    fun `deduplicate keeps highest priority finding`() {
        // Given: Two similar findings with different priorities
        val lowPriority = createFinding(
            id = "1",
            file = "Test.kt",
            lineNumber = 10,
            category = AnalysisCategory.CODE_SMELL,
            title = "Long function",
            priority = Priority.LOW
        )
        val highPriority = createFinding(
            id = "2",
            file = "Test.kt",
            lineNumber = 10,
            category = AnalysisCategory.CODE_SMELL,
            title = "Long function",
            priority = Priority.HIGH
        )
        
        // When: Deduplicate
        val result = aggregator.deduplicate(listOf(lowPriority, highPriority))
        
        // Then: High priority finding should be kept
        assertEquals(1, result.size)
        assertEquals(Priority.HIGH, result[0].priority)
        assertEquals("2", result[0].id)
    }
    
    @Test
    fun `deduplicate keeps findings with different line numbers`() {
        // Given: Two findings in same file but different lines
        val finding1 = createFinding(
            id = "1",
            file = "Test.kt",
            lineNumber = 10,
            category = AnalysisCategory.CODE_SMELL,
            title = "Long function"
        )
        val finding2 = createFinding(
            id = "2",
            file = "Test.kt",
            lineNumber = 20,
            category = AnalysisCategory.CODE_SMELL,
            title = "Long function"
        )
        
        // When: Deduplicate
        val result = aggregator.deduplicate(listOf(finding1, finding2))
        
        // Then: Both findings should remain
        assertEquals(2, result.size)
    }
    
    @Test
    fun `deduplicate keeps findings with different categories`() {
        // Given: Two findings at same location but different categories
        val finding1 = createFinding(
            id = "1",
            file = "Test.kt",
            lineNumber = 10,
            category = AnalysisCategory.CODE_SMELL,
            title = "Issue"
        )
        val finding2 = createFinding(
            id = "2",
            file = "Test.kt",
            lineNumber = 10,
            category = AnalysisCategory.PERFORMANCE,
            title = "Issue"
        )
        
        // When: Deduplicate
        val result = aggregator.deduplicate(listOf(finding1, finding2))
        
        // Then: Both findings should remain
        assertEquals(2, result.size)
    }
    
    @Test
    fun `deduplicate keeps findings with different titles`() {
        // Given: Two findings at same location but different titles
        val finding1 = createFinding(
            id = "1",
            file = "Test.kt",
            lineNumber = 10,
            category = AnalysisCategory.CODE_SMELL,
            title = "Long function"
        )
        val finding2 = createFinding(
            id = "2",
            file = "Test.kt",
            lineNumber = 10,
            category = AnalysisCategory.CODE_SMELL,
            title = "High complexity"
        )
        
        // When: Deduplicate
        val result = aggregator.deduplicate(listOf(finding1, finding2))
        
        // Then: Both findings should remain
        assertEquals(2, result.size)
    }
    
    @Test
    fun `deduplicate handles empty list`() {
        // When: Deduplicate empty list
        val result = aggregator.deduplicate(emptyList())
        
        // Then: Result should be empty
        assertEquals(0, result.size)
    }
    
    @Test
    fun `deduplicate handles single finding`() {
        // Given: Single finding
        val finding = createFinding(id = "1")
        
        // When: Deduplicate
        val result = aggregator.deduplicate(listOf(finding))
        
        // Then: Finding should remain
        assertEquals(1, result.size)
        assertEquals(finding, result[0])
    }
    
    // ========== Priority Assignment Tests ==========
    
    @Test
    fun `aggregate assigns CRITICAL priority to security issues`() {
        // Given: Security finding with low priority
        val finding = createFinding(
            id = "1",
            category = AnalysisCategory.SECURITY,
            priority = Priority.LOW
        )
        
        // When: Aggregate
        val result = aggregator.aggregate(listOf(finding))
        
        // Then: Priority should be upgraded to CRITICAL
        assertEquals(1, result.findings.size)
        assertEquals(Priority.CRITICAL, result.findings[0].priority)
    }
    
    @Test
    fun `aggregate assigns HIGH priority to architecture violations`() {
        // Given: Architecture finding with low priority
        val finding = createFinding(
            id = "1",
            category = AnalysisCategory.ARCHITECTURE,
            priority = Priority.LOW
        )
        
        // When: Aggregate
        val result = aggregator.aggregate(listOf(finding))
        
        // Then: Priority should be upgraded to HIGH
        assertEquals(1, result.findings.size)
        assertEquals(Priority.HIGH, result.findings[0].priority)
    }
    
    @Test
    fun `aggregate assigns HIGH priority to error handling issues`() {
        // Given: Error handling finding with low priority
        val finding = createFinding(
            id = "1",
            category = AnalysisCategory.ERROR_HANDLING,
            priority = Priority.LOW
        )
        
        // When: Aggregate
        val result = aggregator.aggregate(listOf(finding))
        
        // Then: Priority should be upgraded to HIGH
        assertEquals(1, result.findings.size)
        assertEquals(Priority.HIGH, result.findings[0].priority)
    }
    
    @Test
    fun `aggregate assigns MEDIUM priority to performance issues`() {
        // Given: Performance finding with low priority
        val finding = createFinding(
            id = "1",
            category = AnalysisCategory.PERFORMANCE,
            priority = Priority.LOW
        )
        
        // When: Aggregate
        val result = aggregator.aggregate(listOf(finding))
        
        // Then: Priority should be upgraded to MEDIUM
        assertEquals(1, result.findings.size)
        assertEquals(Priority.MEDIUM, result.findings[0].priority)
    }
    
    @Test
    fun `aggregate assigns MEDIUM priority to code smells`() {
        // Given: Code smell finding with low priority
        val finding = createFinding(
            id = "1",
            category = AnalysisCategory.CODE_SMELL,
            priority = Priority.LOW
        )
        
        // When: Aggregate
        val result = aggregator.aggregate(listOf(finding))
        
        // Then: Priority should be upgraded to MEDIUM
        assertEquals(1, result.findings.size)
        assertEquals(Priority.MEDIUM, result.findings[0].priority)
    }
    
    @Test
    fun `aggregate assigns LOW priority to naming issues`() {
        // Given: Naming finding with medium priority
        val finding = createFinding(
            id = "1",
            category = AnalysisCategory.NAMING,
            priority = Priority.MEDIUM
        )
        
        // When: Aggregate
        val result = aggregator.aggregate(listOf(finding))
        
        // Then: Priority should remain LOW (downgraded)
        assertEquals(1, result.findings.size)
        assertEquals(Priority.LOW, result.findings[0].priority)
    }
    
    @Test
    fun `aggregate keeps higher priority if already set`() {
        // Given: Security finding already marked as CRITICAL
        val finding = createFinding(
            id = "1",
            category = AnalysisCategory.SECURITY,
            priority = Priority.CRITICAL
        )
        
        // When: Aggregate
        val result = aggregator.aggregate(listOf(finding))
        
        // Then: Priority should remain CRITICAL
        assertEquals(1, result.findings.size)
        assertEquals(Priority.CRITICAL, result.findings[0].priority)
    }
    
    // ========== Metrics Calculation Tests ==========
    
    @Test
    fun `calculateMetrics counts total files correctly`() {
        // Given: Findings from multiple files
        val findings = listOf(
            createFinding(id = "1", file = "File1.kt"),
            createFinding(id = "2", file = "File2.kt"),
            createFinding(id = "3", file = "File1.kt")
        )
        
        // When: Calculate metrics
        val metrics = aggregator.calculateMetrics(findings)
        
        // Then: Total files should be 2 (unique files)
        assertEquals(2, metrics.totalFiles)
    }
    
    @Test
    fun `calculateMetrics counts total findings correctly`() {
        // Given: Multiple findings
        val findings = listOf(
            createFinding(id = "1"),
            createFinding(id = "2"),
            createFinding(id = "3")
        )
        
        // When: Calculate metrics
        val metrics = aggregator.calculateMetrics(findings)
        
        // Then: Total findings should be 3
        assertEquals(3, metrics.totalFindings)
    }
    
    @Test
    fun `calculateMetrics groups findings by priority`() {
        // Given: Findings with different priorities
        val findings = listOf(
            createFinding(id = "1", priority = Priority.CRITICAL),
            createFinding(id = "2", priority = Priority.HIGH),
            createFinding(id = "3", priority = Priority.HIGH),
            createFinding(id = "4", priority = Priority.MEDIUM)
        )
        
        // When: Calculate metrics
        val metrics = aggregator.calculateMetrics(findings)
        
        // Then: Findings should be grouped correctly
        assertEquals(1, metrics.findingsByPriority[Priority.CRITICAL])
        assertEquals(2, metrics.findingsByPriority[Priority.HIGH])
        assertEquals(1, metrics.findingsByPriority[Priority.MEDIUM])
        assertEquals(0, metrics.findingsByPriority[Priority.LOW])
    }
    
    @Test
    fun `calculateMetrics groups findings by category`() {
        // Given: Findings with different categories
        val findings = listOf(
            createFinding(id = "1", category = AnalysisCategory.SECURITY),
            createFinding(id = "2", category = AnalysisCategory.CODE_SMELL),
            createFinding(id = "3", category = AnalysisCategory.CODE_SMELL),
            createFinding(id = "4", category = AnalysisCategory.PERFORMANCE)
        )
        
        // When: Calculate metrics
        val metrics = aggregator.calculateMetrics(findings)
        
        // Then: Findings should be grouped correctly
        assertEquals(1, metrics.findingsByCategory[AnalysisCategory.SECURITY])
        assertEquals(2, metrics.findingsByCategory[AnalysisCategory.CODE_SMELL])
        assertEquals(1, metrics.findingsByCategory[AnalysisCategory.PERFORMANCE])
    }
    
    @Test
    fun `calculateMetrics extracts complexity values`() {
        // Given: Findings with complexity information
        val findings = listOf(
            createFinding(
                id = "1",
                category = AnalysisCategory.CODE_SMELL,
                title = "High complexity",
                description = "Function has complexity of 20"
            ),
            createFinding(
                id = "2",
                category = AnalysisCategory.CODE_SMELL,
                title = "High complexity",
                description = "Function has complexity: 30"
            )
        )
        
        // When: Calculate metrics
        val metrics = aggregator.calculateMetrics(findings)
        
        // Then: Average complexity should be calculated
        assertEquals(25.0, metrics.averageComplexity, 0.01)
    }
    
    @Test
    fun `calculateMetrics extracts function length values`() {
        // Given: Findings with function length information
        val findings = listOf(
            createFinding(
                id = "1",
                category = AnalysisCategory.CODE_SMELL,
                title = "Long function",
                description = "Function has 60 lines"
            ),
            createFinding(
                id = "2",
                category = AnalysisCategory.CODE_SMELL,
                title = "Long function",
                description = "Function has 80 lines"
            )
        )
        
        // When: Calculate metrics
        val metrics = aggregator.calculateMetrics(findings)
        
        // Then: Average function length should be calculated
        assertEquals(70.0, metrics.averageFunctionLength, 0.01)
    }
    
    @Test
    fun `calculateMetrics extracts class length values`() {
        // Given: Findings with class length information
        val findings = listOf(
            createFinding(
                id = "1",
                category = AnalysisCategory.CODE_SMELL,
                title = "Large class",
                description = "Class has 400 lines"
            ),
            createFinding(
                id = "2",
                category = AnalysisCategory.CODE_SMELL,
                title = "Large class",
                description = "Class has 500 lines"
            )
        )
        
        // When: Calculate metrics
        val metrics = aggregator.calculateMetrics(findings)
        
        // Then: Average class length should be calculated
        assertEquals(450.0, metrics.averageClassLength, 0.01)
    }
    
    @Test
    fun `calculateMetrics handles empty findings list`() {
        // When: Calculate metrics for empty list
        val metrics = aggregator.calculateMetrics(emptyList())
        
        // Then: All metrics should be zero or empty
        assertEquals(0, metrics.totalFiles)
        assertEquals(0, metrics.totalFindings)
        assertEquals(0.0, metrics.averageComplexity, 0.01)
        assertEquals(0.0, metrics.averageFunctionLength, 0.01)
        assertEquals(0.0, metrics.averageClassLength, 0.01)
    }
    
    // ========== Aggregation Tests ==========
    
    @Test
    fun `aggregate groups findings by category`() {
        // Given: Findings with different categories
        val findings = listOf(
            createFinding(id = "1", category = AnalysisCategory.SECURITY),
            createFinding(id = "2", category = AnalysisCategory.CODE_SMELL),
            createFinding(id = "3", category = AnalysisCategory.CODE_SMELL)
        )
        
        // When: Aggregate
        val result = aggregator.aggregate(findings)
        
        // Then: Findings should be grouped by category
        assertEquals(1, result.byCategory[AnalysisCategory.SECURITY]?.size)
        assertEquals(2, result.byCategory[AnalysisCategory.CODE_SMELL]?.size)
    }
    
    @Test
    fun `aggregate groups findings by priority`() {
        // Given: Findings with different priorities (will be reassigned)
        val findings = listOf(
            createFinding(id = "1", category = AnalysisCategory.SECURITY, priority = Priority.LOW),
            createFinding(id = "2", category = AnalysisCategory.NAMING, priority = Priority.HIGH),
            createFinding(id = "3", category = AnalysisCategory.NAMING, priority = Priority.HIGH)
        )
        
        // When: Aggregate
        val result = aggregator.aggregate(findings)
        
        // Then: Findings should be grouped by assigned priority
        assertEquals(1, result.byPriority[Priority.CRITICAL]?.size) // Security -> CRITICAL
        assertEquals(2, result.byPriority[Priority.LOW]?.size) // Naming -> LOW
    }
    
    @Test
    fun `aggregate groups findings by file`() {
        // Given: Findings from different files
        val findings = listOf(
            createFinding(id = "1", file = "File1.kt"),
            createFinding(id = "2", file = "File2.kt"),
            createFinding(id = "3", file = "File1.kt")
        )
        
        // When: Aggregate
        val result = aggregator.aggregate(findings)
        
        // Then: Findings should be grouped by file
        assertEquals(2, result.byFile["File1.kt"]?.size)
        assertEquals(1, result.byFile["File2.kt"]?.size)
    }
    
    @Test
    fun `aggregate includes metrics`() {
        // Given: Multiple findings
        val findings = listOf(
            createFinding(id = "1", file = "File1.kt"),
            createFinding(id = "2", file = "File2.kt")
        )
        
        // When: Aggregate
        val result = aggregator.aggregate(findings)
        
        // Then: Metrics should be included
        assertNotNull(result.metrics)
        assertEquals(2, result.metrics.totalFiles)
        assertEquals(2, result.metrics.totalFindings)
    }
    
    @Test
    fun `aggregate handles empty findings list`() {
        // When: Aggregate empty list
        val result = aggregator.aggregate(emptyList())
        
        // Then: Result should have empty collections
        assertEquals(0, result.findings.size)
        assertEquals(0, result.byCategory.size)
        assertEquals(0, result.byPriority.size)
        assertEquals(0, result.byFile.size)
        assertEquals(0, result.metrics.totalFindings)
    }
    
    // ========== Helper Methods ==========
    
    private fun createFinding(
        id: String = "test-id",
        analyzer: String = "test-analyzer",
        category: AnalysisCategory = AnalysisCategory.CODE_SMELL,
        priority: Priority = Priority.MEDIUM,
        title: String = "Test Finding",
        description: String = "Test description",
        file: String = "Test.kt",
        lineNumber: Int = 1,
        codeSnippet: String = "code snippet",
        recommendation: String = "Fix this issue",
        effort: Effort = Effort.SMALL
    ): Finding {
        return Finding(
            id = id,
            analyzer = analyzer,
            category = category,
            priority = priority,
            title = title,
            description = description,
            file = file,
            lineNumber = lineNumber,
            columnNumber = null,
            codeSnippet = codeSnippet,
            recommendation = recommendation,
            beforeExample = null,
            afterExample = null,
            autoFixable = false,
            autoFix = null,
            effort = effort,
            references = emptyList(),
            relatedFindings = emptyList()
        )
    }
}
