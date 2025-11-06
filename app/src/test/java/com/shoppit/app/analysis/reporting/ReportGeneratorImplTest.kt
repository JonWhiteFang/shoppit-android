package com.shoppit.app.analysis.reporting

import com.shoppit.app.analysis.core.AggregatedResult
import com.shoppit.app.analysis.core.Baseline
import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.AnalysisMetrics
import com.shoppit.app.analysis.models.Effort
import com.shoppit.app.analysis.models.Finding
import com.shoppit.app.analysis.models.Priority
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ReportGeneratorImpl.
 *
 * Tests cover:
 * - Markdown formatting
 * - Executive summary generation
 * - Findings section generation
 * - Improvement recommendations
 * - Baseline comparison
 */
class ReportGeneratorImplTest {
    
    private lateinit var generator: ReportGeneratorImpl
    
    @Before
    fun setup() {
        generator = ReportGeneratorImpl()
    }
    
    // ========== Executive Summary Tests ==========
    
    @Test
    fun `generateSummary includes key metrics section`() {
        // Given: Metrics with findings
        val metrics = createMetrics(
            totalFiles = 10,
            totalFindings = 5,
            findingsByPriority = mapOf(
                Priority.CRITICAL to 1,
                Priority.HIGH to 2,
                Priority.MEDIUM to 1,
                Priority.LOW to 1
            )
        )
        
        // When: Generate summary
        val summary = generator.generateSummary(metrics, null)
        
        // Then: Summary should include key metrics
        assertTrue(summary.contains("## Executive Summary"))
        assertTrue(summary.contains("### Key Metrics"))
        assertTrue(summary.contains("Critical Issues: 1"))
        assertTrue(summary.contains("High Issues: 2"))
        assertTrue(summary.contains("Medium Issues: 1"))
        assertTrue(summary.contains("Low Issues: 1"))
    }
    
    @Test
    fun `generateSummary includes code metrics`() {
        // Given: Metrics with code quality data
        val metrics = createMetrics(
            averageComplexity = 12.5,
            averageFunctionLength = 45.3,
            averageClassLength = 250.7,
            testCoveragePercentage = 75.5,
            documentationCoveragePercentage = 60.2
        )
        
        // When: Generate summary
        val summary = generator.generateSummary(metrics, null)
        
        // Then: Summary should include code metrics
        assertTrue(summary.contains("### Code Metrics"))
        assertTrue(summary.contains("Average Cyclomatic Complexity: 12.50"))
        assertTrue(summary.contains("Average Function Length: 45.3 lines"))
        assertTrue(summary.contains("Average Class Length: 250.7 lines"))
        assertTrue(summary.contains("Test Coverage: 75.5%"))
        assertTrue(summary.contains("Documentation Coverage: 60.2%"))
    }
    
    @Test
    fun `generateSummary includes top issues by category`() {
        // Given: Metrics with findings by category
        val metrics = createMetrics(
            findingsByCategory = mapOf(
                AnalysisCategory.SECURITY to 5,
                AnalysisCategory.CODE_SMELL to 10,
                AnalysisCategory.PERFORMANCE to 3
            )
        )
        
        // When: Generate summary
        val summary = generator.generateSummary(metrics, null)
        
        // Then: Summary should include top issues
        assertTrue(summary.contains("### Top Issues by Category"))
        assertTrue(summary.contains("Code smell: 10 issues"))
        assertTrue(summary.contains("Security: 5 issues"))
        assertTrue(summary.contains("Performance: 3 issues"))
    }
    
    @Test
    fun `generateSummary shows baseline comparison when provided`() {
        // Given: Current metrics and baseline
        val currentMetrics = createMetrics(
            findingsByPriority = mapOf(
                Priority.CRITICAL to 2,
                Priority.HIGH to 3
            )
        )
        val baseline = Baseline(
            timestamp = System.currentTimeMillis() - 86400000, // 1 day ago
            metrics = createMetrics(
                findingsByPriority = mapOf(
                    Priority.CRITICAL to 3,
                    Priority.HIGH to 5
                )
            ),
            findingIds = emptySet()
        )
        
        // When: Generate summary with baseline
        val summary = generator.generateSummary(currentMetrics, baseline)
        
        // Then: Summary should show trends
        assertTrue(summary.contains("↓ -1 from baseline")) // Critical improved
        assertTrue(summary.contains("↓ -2 from baseline")) // High improved
    }
    
    @Test
    fun `generateSummary handles no findings`() {
        // Given: Metrics with no findings
        val metrics = createMetrics(totalFindings = 0)
        
        // When: Generate summary
        val summary = generator.generateSummary(metrics, null)
        
        // Then: Summary should handle empty state
        assertTrue(summary.contains("No issues found"))
    }
    
    // ========== Findings Generation Tests ==========
    
    @Test
    fun `generateFindings returns message for empty findings`() {
        // When: Generate findings for empty list
        val result = generator.generateFindings(emptyList())
        
        // Then: Should return no findings message
        assertTrue(result.contains("No findings to report"))
    }
    
    @Test
    fun `generateFindings groups by priority`() {
        // Given: Findings with different priorities
        val findings = listOf(
            createFinding(id = "1", priority = Priority.CRITICAL, title = "Critical Issue"),
            createFinding(id = "2", priority = Priority.HIGH, title = "High Issue"),
            createFinding(id = "3", priority = Priority.MEDIUM, title = "Medium Issue")
        )
        
        // When: Generate findings
        val result = generator.generateFindings(findings)
        
        // Then: Should group by priority
        assertTrue(result.contains("### Critical Priority"))
        assertTrue(result.contains("### High Priority"))
        assertTrue(result.contains("### Medium Priority"))
    }
    
    @Test
    fun `generateFindings groups by category within priority`() {
        // Given: Findings with same priority but different categories
        val findings = listOf(
            createFinding(
                id = "1",
                priority = Priority.HIGH,
                category = AnalysisCategory.SECURITY,
                title = "Security Issue"
            ),
            createFinding(
                id = "2",
                priority = Priority.HIGH,
                category = AnalysisCategory.ARCHITECTURE,
                title = "Architecture Issue"
            )
        )
        
        // When: Generate findings
        val result = generator.generateFindings(findings)
        
        // Then: Should group by category
        assertTrue(result.contains("#### Security"))
        assertTrue(result.contains("#### Architecture"))
    }
    
    @Test
    fun `generateFindings includes all finding details`() {
        // Given: Finding with all details
        val finding = createFinding(
            id = "1",
            title = "Long Function",
            description = "Function exceeds 50 lines",
            file = "Test.kt",
            lineNumber = 42,
            codeSnippet = "fun longFunction() {\n  // code\n}",
            recommendation = "Break into smaller functions",
            beforeExample = "fun longFunction() { }",
            afterExample = "fun shortFunction1() { }\nfun shortFunction2() { }",
            effort = Effort.MEDIUM,
            autoFixable = true,
            references = listOf("https://example.com/best-practices")
        )
        
        // When: Generate findings
        val result = generator.generateFindings(listOf(finding))
        
        // Then: Should include all details
        assertTrue(result.contains("##### Long Function"))
        assertTrue(result.contains("**File:** `Test.kt:42`"))
        assertTrue(result.contains("**Description:** Function exceeds 50 lines"))
        assertTrue(result.contains("**Current Code:**"))
        assertTrue(result.contains("fun longFunction()"))
        assertTrue(result.contains("**Recommendation:** Break into smaller functions"))
        assertTrue(result.contains("**Before:**"))
        assertTrue(result.contains("**After:**"))
        assertTrue(result.contains("**Effort:** Medium"))
        assertTrue(result.contains("**Auto-fixable:** Yes"))
        assertTrue(result.contains("**References:**"))
        assertTrue(result.contains("https://example.com/best-practices"))
    }
    
    @Test
    fun `generateFindings formats code snippets correctly`() {
        // Given: Finding with code snippet
        val finding = createFinding(
            id = "1",
            codeSnippet = "fun test() {\n    println(\"test\")\n}"
        )
        
        // When: Generate findings
        val result = generator.generateFindings(listOf(finding))
        
        // Then: Should format as code block
        assertTrue(result.contains("```kotlin"))
        assertTrue(result.contains("fun test()"))
        assertTrue(result.contains("```"))
    }
    
    // ========== Complete Report Tests ==========
    
    @Test
    fun `generate creates complete report structure`() {
        // Given: Aggregated result
        val result = createAggregatedResult(
            findings = listOf(
                createFinding(id = "1", priority = Priority.HIGH)
            ),
            metrics = createMetrics(totalFiles = 5, totalFindings = 1)
        )
        
        // When: Generate complete report
        val report = generator.generate(result, null)
        
        // Then: Should include all sections
        assertTrue(report.contains("# Code Quality Analysis Report"))
        assertTrue(report.contains("**Generated:**"))
        assertTrue(report.contains("**Files Analyzed:** 5"))
        assertTrue(report.contains("**Total Findings:** 1"))
        assertTrue(report.contains("## Executive Summary"))
        assertTrue(report.contains("## Findings by Category"))
        assertTrue(report.contains("## Detailed Findings"))
        assertTrue(report.contains("## Improvement Recommendations"))
        assertTrue(report.contains("## Next Steps"))
    }
    
    @Test
    fun `generate includes baseline comparison when provided`() {
        // Given: Result with baseline
        val result = createAggregatedResult()
        val baseline = Baseline(
            timestamp = System.currentTimeMillis(),
            metrics = createMetrics(),
            findingIds = emptySet()
        )
        
        // When: Generate report with baseline
        val report = generator.generate(result, baseline)
        
        // Then: Should include baseline section
        assertTrue(report.contains("## Baseline Comparison"))
    }
    
    @Test
    fun `generate omits baseline comparison when not provided`() {
        // Given: Result without baseline
        val result = createAggregatedResult()
        
        // When: Generate report without baseline
        val report = generator.generate(result, null)
        
        // Then: Should not include baseline section
        assertFalse(report.contains("## Baseline Comparison"))
    }
    
    // ========== Improvement Recommendations Tests ==========
    
    @Test
    fun `generate groups recommendations by effort`() {
        // Given: Findings with different effort levels
        val findings = listOf(
            createFinding(id = "1", effort = Effort.TRIVIAL, title = "Trivial Fix"),
            createFinding(id = "2", effort = Effort.SMALL, title = "Small Fix"),
            createFinding(id = "3", effort = Effort.MEDIUM, title = "Medium Fix"),
            createFinding(id = "4", effort = Effort.LARGE, title = "Large Fix")
        )
        val result = createAggregatedResult(findings = findings)
        
        // When: Generate report
        val report = generator.generate(result, null)
        
        // Then: Should group by effort
        assertTrue(report.contains("### Quick Wins (< 5 minutes each)"))
        assertTrue(report.contains("### Short Term (5-30 minutes each)"))
        assertTrue(report.contains("### Medium Term (30 minutes - 2 hours each)"))
        assertTrue(report.contains("### Long Term (> 2 hours each)"))
    }
    
    @Test
    fun `generate limits recommendations to 10 per effort level`() {
        // Given: Many trivial findings
        val findings = (1..15).map { 
            createFinding(id = "$it", effort = Effort.TRIVIAL, title = "Fix $it")
        }
        val result = createAggregatedResult(findings = findings)
        
        // When: Generate report
        val report = generator.generate(result, null)
        
        // Then: Should show only 10 and indicate more
        assertTrue(report.contains("... and 5 more"))
    }
    
    // ========== Next Steps Tests ==========
    
    @Test
    fun `generate provides next steps based on priorities`() {
        // Given: Findings with different priorities
        val findings = listOf(
            createFinding(id = "1", priority = Priority.CRITICAL),
            createFinding(id = "2", priority = Priority.HIGH),
            createFinding(id = "3", priority = Priority.MEDIUM),
            createFinding(id = "4", priority = Priority.LOW)
        )
        val result = createAggregatedResult(findings = findings)
        
        // When: Generate report
        val report = generator.generate(result, null)
        
        // Then: Should provide prioritized next steps
        assertTrue(report.contains("Address all Critical priority issues immediately"))
        assertTrue(report.contains("Create tasks for High priority issues"))
        assertTrue(report.contains("Schedule refactoring for Medium priority issues"))
        assertTrue(report.contains("Consider Low priority issues for future improvements"))
    }
    
    @Test
    fun `generate shows success message when no issues found`() {
        // Given: No findings
        val result = createAggregatedResult(findings = emptyList())
        
        // When: Generate report
        val report = generator.generate(result, null)
        
        // Then: Should show success message
        assertTrue(report.contains("No issues found. Great job maintaining code quality!"))
    }
    
    // ========== Baseline Comparison Tests ==========
    
    @Test
    fun `generate shows improvements in baseline comparison`() {
        // Given: Current metrics better than baseline
        val currentMetrics = createMetrics(
            findingsByPriority = mapOf(Priority.HIGH to 2),
            averageComplexity = 10.0,
            testCoveragePercentage = 80.0
        )
        val baselineMetrics = createMetrics(
            findingsByPriority = mapOf(Priority.HIGH to 5),
            averageComplexity = 15.0,
            testCoveragePercentage = 70.0
        )
        val result = createAggregatedResult(metrics = currentMetrics)
        val baseline = Baseline(
            timestamp = System.currentTimeMillis(),
            metrics = baselineMetrics,
            findingIds = emptySet()
        )
        
        // When: Generate report
        val report = generator.generate(result, baseline)
        
        // Then: Should show improvements
        assertTrue(report.contains("### Improvements"))
        assertTrue(report.contains("High: 3 issues resolved"))
        assertTrue(report.contains("Complexity: Improved by"))
        assertTrue(report.contains("Test Coverage: Increased by"))
    }
    
    @Test
    fun `generate shows regressions in baseline comparison`() {
        // Given: Current metrics worse than baseline
        val currentMetrics = createMetrics(
            findingsByPriority = mapOf(Priority.HIGH to 5),
            averageComplexity = 15.0,
            testCoveragePercentage = 70.0
        )
        val baselineMetrics = createMetrics(
            findingsByPriority = mapOf(Priority.HIGH to 2),
            averageComplexity = 10.0,
            testCoveragePercentage = 80.0
        )
        val result = createAggregatedResult(metrics = currentMetrics)
        val baseline = Baseline(
            timestamp = System.currentTimeMillis(),
            metrics = baselineMetrics,
            findingIds = emptySet()
        )
        
        // When: Generate report
        val report = generator.generate(result, baseline)
        
        // Then: Should show regressions
        assertTrue(report.contains("### Regressions"))
        assertTrue(report.contains("High: 3 new issues"))
        assertTrue(report.contains("Complexity: Degraded by"))
        assertTrue(report.contains("Test Coverage: Decreased by"))
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
        beforeExample: String? = null,
        afterExample: String? = null,
        effort: Effort = Effort.SMALL,
        autoFixable: Boolean = false,
        references: List<String> = emptyList()
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
            beforeExample = beforeExample,
            afterExample = afterExample,
            autoFixable = autoFixable,
            autoFix = null,
            effort = effort,
            references = references,
            relatedFindings = emptyList()
        )
    }
    
    private fun createMetrics(
        totalFiles: Int = 0,
        totalFindings: Int = 0,
        findingsByPriority: Map<Priority, Int> = emptyMap(),
        findingsByCategory: Map<AnalysisCategory, Int> = emptyMap(),
        averageComplexity: Double = 0.0,
        averageFunctionLength: Double = 0.0,
        averageClassLength: Double = 0.0,
        testCoveragePercentage: Double = 0.0,
        documentationCoveragePercentage: Double = 0.0
    ): AnalysisMetrics {
        return AnalysisMetrics(
            totalFiles = totalFiles,
            totalFindings = totalFindings,
            findingsByPriority = findingsByPriority,
            findingsByCategory = findingsByCategory,
            averageComplexity = averageComplexity,
            averageFunctionLength = averageFunctionLength,
            averageClassLength = averageClassLength,
            testCoveragePercentage = testCoveragePercentage,
            documentationCoveragePercentage = documentationCoveragePercentage
        )
    }
    
    private fun createAggregatedResult(
        findings: List<Finding> = emptyList(),
        metrics: AnalysisMetrics = createMetrics()
    ): AggregatedResult {
        return AggregatedResult(
            findings = findings,
            metrics = metrics,
            byCategory = findings.groupBy { it.category },
            byPriority = findings.groupBy { it.priority },
            byFile = findings.groupBy { it.file }
        )
    }
}
