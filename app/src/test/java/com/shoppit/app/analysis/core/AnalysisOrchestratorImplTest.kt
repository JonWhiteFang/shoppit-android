package com.shoppit.app.analysis.core

import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.CodeLayer
import com.shoppit.app.analysis.models.Effort
import com.shoppit.app.analysis.models.FileInfo
import com.shoppit.app.analysis.models.Finding
import com.shoppit.app.analysis.models.Priority
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests for AnalysisOrchestratorImpl.
 *
 * Tests cover:
 * - Full analysis workflow
 * - Incremental analysis
 * - Filtered analysis with specific analyzers
 * - Error handling
 * - Report generation
 * - Baseline management
 */
class AnalysisOrchestratorImplTest {
    
    private lateinit var fileScanner: FileScanner
    private lateinit var resultAggregator: ResultAggregator
    private lateinit var reportGenerator: ReportGenerator
    private lateinit var baselineManager: BaselineManager
    private lateinit var orchestrator: AnalysisOrchestratorImpl
    
    private val testProjectRoot = "test/project/root"
    
    @Before
    fun setup() {
        fileScanner = mockk()
        resultAggregator = mockk()
        reportGenerator = mockk()
        baselineManager = mockk()
        
        orchestrator = AnalysisOrchestratorImpl(
            fileScanner = fileScanner,
            resultAggregator = resultAggregator,
            reportGenerator = reportGenerator,
            baselineManager = baselineManager,
            projectRoot = testProjectRoot
        )
        
        // Mock file system operations
        mockkStatic(File::class)
    }
    
    // ========== Full Analysis Tests ==========
    
    @Test
    fun `analyzeAll scans all files in project root`() = runTest {
        // Given: File scanner returns files
        val files = listOf(
            createFileInfo("File1.kt"),
            createFileInfo("File2.kt")
        )
        every { fileScanner.scanDirectory(testProjectRoot) } returns files
        every { fileScanner.filterFiles(files) } returns files
        
        // Mock file reading
        every { File(any<String>()).readText() } returns "file content"
        
        // Mock aggregation and reporting
        val aggregatedResult = createAggregatedResult(emptyList())
        every { resultAggregator.aggregate(any()) } returns aggregatedResult
        every { reportGenerator.generate(any(), any()) } returns "report"
        every { baselineManager.loadBaseline() } returns null
        every { baselineManager.saveBaseline(any(), any()) } just Runs
        every { baselineManager.saveToHistory(any()) } just Runs
        
        // Mock report saving
        mockkConstructor(File::class)
        every { anyConstructed<File>().exists() } returns true
        every { anyConstructed<File>().mkdirs() } returns true
        every { anyConstructed<File>().writeText(any()) } just Runs
        
        // When: Analyze all
        val result = orchestrator.analyzeAll()
        
        // Then: File scanner should be called with project root
        verify { fileScanner.scanDirectory(testProjectRoot) }
        verify { fileScanner.filterFiles(files) }
    }
    
    @Test
    fun `analyzeAll aggregates findings from all analyzers`() = runTest {
        // Given: File scanner returns files
        val files = listOf(createFileInfo("File1.kt"))
        every { fileScanner.scanDirectory(any()) } returns files
        every { fileScanner.filterFiles(files) } returns files
        
        // Mock file reading
        every { File(any<String>()).readText() } returns "file content"
        
        // Mock aggregation
        val findings = listOf(createFinding())
        val aggregatedResult = createAggregatedResult(findings)
        every { resultAggregator.aggregate(any()) } returns aggregatedResult
        
        // Mock reporting and baseline
        every { reportGenerator.generate(any(), any()) } returns "report"
        every { baselineManager.loadBaseline() } returns null
        every { baselineManager.saveBaseline(any(), any()) } just Runs
        every { baselineManager.saveToHistory(any()) } just Runs
        
        // Mock report saving
        mockkConstructor(File::class)
        every { anyConstructed<File>().exists() } returns true
        every { anyConstructed<File>().mkdirs() } returns true
        every { anyConstructed<File>().writeText(any()) } just Runs
        
        // When: Analyze all
        val result = orchestrator.analyzeAll()
        
        // Then: Result aggregator should be called
        verify { resultAggregator.aggregate(any()) }
    }
    
    @Test
    fun `analyzeAll generates report with baseline comparison`() = runTest {
        // Given: File scanner returns files
        val files = listOf(createFileInfo("File1.kt"))
        every { fileScanner.scanDirectory(any()) } returns files
        every { fileScanner.filterFiles(files) } returns files
        
        // Mock file reading
        every { File(any<String>()).readText() } returns "file content"
        
        // Mock aggregation
        val aggregatedResult = createAggregatedResult(emptyList())
        every { resultAggregator.aggregate(any()) } returns aggregatedResult
        
        // Mock baseline
        val baseline = createBaseline()
        every { baselineManager.loadBaseline() } returns baseline
        
        // Mock reporting
        every { reportGenerator.generate(aggregatedResult, baseline) } returns "report with baseline"
        every { baselineManager.saveBaseline(any(), any()) } just Runs
        every { baselineManager.saveToHistory(any()) } just Runs
        
        // Mock report saving
        mockkConstructor(File::class)
        every { anyConstructed<File>().exists() } returns true
        every { anyConstructed<File>().mkdirs() } returns true
        every { anyConstructed<File>().writeText(any()) } just Runs
        
        // When: Analyze all
        orchestrator.analyzeAll()
        
        // Then: Report generator should be called with baseline
        verify { reportGenerator.generate(aggregatedResult, baseline) }
    }
    
    @Test
    fun `analyzeAll updates baseline after analysis`() = runTest {
        // Given: File scanner returns files
        val files = listOf(createFileInfo("File1.kt"))
        every { fileScanner.scanDirectory(any()) } returns files
        every { fileScanner.filterFiles(files) } returns files
        
        // Mock file reading
        every { File(any<String>()).readText() } returns "file content"
        
        // Mock aggregation
        val findings = listOf(createFinding())
        val aggregatedResult = createAggregatedResult(findings)
        every { resultAggregator.aggregate(any()) } returns aggregatedResult
        
        // Mock reporting and baseline
        every { reportGenerator.generate(any(), any()) } returns "report"
        every { baselineManager.loadBaseline() } returns null
        every { baselineManager.saveBaseline(any(), any()) } just Runs
        every { baselineManager.saveToHistory(any()) } just Runs
        
        // Mock report saving
        mockkConstructor(File::class)
        every { anyConstructed<File>().exists() } returns true
        every { anyConstructed<File>().mkdirs() } returns true
        every { anyConstructed<File>().writeText(any()) } just Runs
        
        // When: Analyze all
        orchestrator.analyzeAll()
        
        // Then: Baseline should be saved
        verify { baselineManager.saveBaseline(aggregatedResult.metrics, findings) }
        verify { baselineManager.saveToHistory(aggregatedResult) }
    }
    
    @Test
    fun `analyzeAll handles file reading errors gracefully`() = runTest {
        // Given: File scanner returns files
        val files = listOf(createFileInfo("File1.kt"))
        every { fileScanner.scanDirectory(any()) } returns files
        every { fileScanner.filterFiles(files) } returns files
        
        // Mock file reading to throw exception
        every { File(any<String>()).readText() } throws RuntimeException("File read error")
        
        // Mock aggregation (should still be called with empty findings)
        val aggregatedResult = createAggregatedResult(emptyList())
        every { resultAggregator.aggregate(any()) } returns aggregatedResult
        
        // Mock reporting and baseline
        every { reportGenerator.generate(any(), any()) } returns "report"
        every { baselineManager.loadBaseline() } returns null
        every { baselineManager.saveBaseline(any(), any()) } just Runs
        every { baselineManager.saveToHistory(any()) } just Runs
        
        // Mock report saving
        mockkConstructor(File::class)
        every { anyConstructed<File>().exists() } returns true
        every { anyConstructed<File>().mkdirs() } returns true
        every { anyConstructed<File>().writeText(any()) } just Runs
        
        // When: Analyze all
        val result = orchestrator.analyzeAll()
        
        // Then: Analysis should complete without throwing
        // (errors are logged but don't stop analysis)
        verify { resultAggregator.aggregate(any()) }
    }
    
    // ========== Incremental Analysis Tests ==========
    
    @Test
    fun `analyzeIncremental analyzes only specified files`() = runTest {
        // Given: Specific file path
        val filePath = "app/src/main/java/Test.kt"
        val file = createFileInfo(filePath)
        
        // Mock file system
        mockkConstructor(File::class)
        every { anyConstructed<File>().isDirectory } returns false
        every { anyConstructed<File>().isFile } returns true
        every { anyConstructed<File>().extension } returns "kt"
        every { anyConstructed<File>().parentFile } returns mockk {
            every { absolutePath } returns "app/src/main/java"
        }
        every { anyConstructed<File>().absolutePath } returns filePath
        
        // Mock file scanner
        every { fileScanner.scanDirectory(any()) } returns listOf(file)
        every { fileScanner.filterFiles(any()) } returns listOf(file)
        
        // Mock file reading
        every { anyConstructed<File>().readText() } returns "file content"
        
        // Mock aggregation and reporting
        val aggregatedResult = createAggregatedResult(emptyList())
        every { resultAggregator.aggregate(any()) } returns aggregatedResult
        every { reportGenerator.generate(aggregatedResult, null) } returns "report"
        
        // Mock report saving
        every { anyConstructed<File>().exists() } returns true
        every { anyConstructed<File>().mkdirs() } returns true
        every { anyConstructed<File>().writeText(any()) } just Runs
        
        // When: Analyze incrementally
        val result = orchestrator.analyzeIncremental(listOf(filePath))
        
        // Then: Only specified file should be analyzed
        assertEquals(1, result.filesAnalyzed)
    }
    
    @Test
    fun `analyzeIncremental scans directories recursively`() = runTest {
        // Given: Directory path
        val dirPath = "app/src/main/java/feature"
        val files = listOf(
            createFileInfo("$dirPath/File1.kt"),
            createFileInfo("$dirPath/File2.kt")
        )
        
        // Mock file system
        mockkConstructor(File::class)
        every { anyConstructed<File>().isDirectory } returns true
        every { anyConstructed<File>().isFile } returns false
        
        // Mock file scanner
        every { fileScanner.scanDirectory(dirPath) } returns files
        every { fileScanner.filterFiles(files) } returns files
        
        // Mock file reading
        every { anyConstructed<File>().readText() } returns "file content"
        
        // Mock aggregation and reporting
        val aggregatedResult = createAggregatedResult(emptyList())
        every { resultAggregator.aggregate(any()) } returns aggregatedResult
        every { reportGenerator.generate(any(), null) } returns "report"
        
        // Mock report saving
        every { anyConstructed<File>().exists() } returns true
        every { anyConstructed<File>().mkdirs() } returns true
        every { anyConstructed<File>().writeText(any()) } just Runs
        
        // When: Analyze incrementally
        val result = orchestrator.analyzeIncremental(listOf(dirPath))
        
        // Then: All files in directory should be analyzed
        assertEquals(2, result.filesAnalyzed)
        verify { fileScanner.scanDirectory(dirPath) }
    }
    
    @Test
    fun `analyzeIncremental does not update baseline`() = runTest {
        // Given: File path
        val filePath = "app/src/main/java/Test.kt"
        val file = createFileInfo(filePath)
        
        // Mock file system
        mockkConstructor(File::class)
        every { anyConstructed<File>().isDirectory } returns false
        every { anyConstructed<File>().isFile } returns true
        every { anyConstructed<File>().extension } returns "kt"
        every { anyConstructed<File>().parentFile } returns mockk {
            every { absolutePath } returns "app/src/main/java"
        }
        every { anyConstructed<File>().absolutePath } returns filePath
        
        // Mock file scanner
        every { fileScanner.scanDirectory(any()) } returns listOf(file)
        every { fileScanner.filterFiles(any()) } returns listOf(file)
        
        // Mock file reading
        every { anyConstructed<File>().readText() } returns "file content"
        
        // Mock aggregation and reporting
        val aggregatedResult = createAggregatedResult(emptyList())
        every { resultAggregator.aggregate(any()) } returns aggregatedResult
        every { reportGenerator.generate(any(), null) } returns "report"
        
        // Mock report saving
        every { anyConstructed<File>().exists() } returns true
        every { anyConstructed<File>().mkdirs() } returns true
        every { anyConstructed<File>().writeText(any()) } just Runs
        
        // When: Analyze incrementally
        orchestrator.analyzeIncremental(listOf(filePath))
        
        // Then: Baseline should not be updated
        verify(exactly = 0) { baselineManager.saveBaseline(any(), any()) }
        verify(exactly = 0) { baselineManager.saveToHistory(any()) }
    }
    
    // ========== Filtered Analysis Tests ==========
    
    @Test
    fun `analyzeWithFilters runs only specified analyzers`() = runTest {
        // Given: File scanner returns files
        val files = listOf(createFileInfo("File1.kt"))
        every { fileScanner.scanDirectory(any()) } returns files
        every { fileScanner.filterFiles(files) } returns files
        
        // Mock file reading
        every { File(any<String>()).readText() } returns "file content"
        
        // Mock aggregation and reporting
        val aggregatedResult = createAggregatedResult(emptyList())
        every { resultAggregator.aggregate(any()) } returns aggregatedResult
        every { reportGenerator.generate(any(), null) } returns "report"
        
        // Mock report saving
        mockkConstructor(File::class)
        every { anyConstructed<File>().exists() } returns true
        every { anyConstructed<File>().mkdirs() } returns true
        every { anyConstructed<File>().writeText(any()) } just Runs
        
        // When: Analyze with specific analyzers
        val result = orchestrator.analyzeWithFilters(
            paths = null,
            analyzers = listOf("security", "architecture")
        )
        
        // Then: Analysis should complete
        verify { resultAggregator.aggregate(any()) }
    }
    
    @Test
    fun `analyzeWithFilters handles invalid analyzer IDs`() = runTest {
        // Given: File scanner returns files
        val files = listOf(createFileInfo("File1.kt"))
        every { fileScanner.scanDirectory(any()) } returns files
        every { fileScanner.filterFiles(files) } returns files
        
        // Mock file reading
        every { File(any<String>()).readText() } returns "file content"
        
        // Mock aggregation and reporting
        val aggregatedResult = createAggregatedResult(emptyList())
        every { resultAggregator.aggregate(any()) } returns aggregatedResult
        every { reportGenerator.generate(any(), null) } returns "report"
        
        // Mock report saving
        mockkConstructor(File::class)
        every { anyConstructed<File>().exists() } returns true
        every { anyConstructed<File>().mkdirs() } returns true
        every { anyConstructed<File>().writeText(any()) } just Runs
        
        // When: Analyze with invalid analyzer ID
        val result = orchestrator.analyzeWithFilters(
            paths = null,
            analyzers = listOf("invalid-analyzer")
        )
        
        // Then: Should return empty result
        assertEquals(0, result.filesAnalyzed)
    }
    
    @Test
    fun `analyzeWithFilters combines path and analyzer filtering`() = runTest {
        // Given: Specific file path
        val filePath = "app/src/main/java/Test.kt"
        val file = createFileInfo(filePath)
        
        // Mock file system
        mockkConstructor(File::class)
        every { anyConstructed<File>().isDirectory } returns false
        every { anyConstructed<File>().isFile } returns true
        every { anyConstructed<File>().extension } returns "kt"
        every { anyConstructed<File>().parentFile } returns mockk {
            every { absolutePath } returns "app/src/main/java"
        }
        every { anyConstructed<File>().absolutePath } returns filePath
        
        // Mock file scanner
        every { fileScanner.scanDirectory(any()) } returns listOf(file)
        every { fileScanner.filterFiles(any()) } returns listOf(file)
        
        // Mock file reading
        every { anyConstructed<File>().readText() } returns "file content"
        
        // Mock aggregation and reporting
        val aggregatedResult = createAggregatedResult(emptyList())
        every { resultAggregator.aggregate(any()) } returns aggregatedResult
        every { reportGenerator.generate(any(), null) } returns "report"
        
        // Mock report saving
        every { anyConstructed<File>().exists() } returns true
        every { anyConstructed<File>().mkdirs() } returns true
        every { anyConstructed<File>().writeText(any()) } just Runs
        
        // When: Analyze with path and analyzer filters
        val result = orchestrator.analyzeWithFilters(
            paths = listOf(filePath),
            analyzers = listOf("security")
        )
        
        // Then: Should analyze specified file with specified analyzer
        assertEquals(1, result.filesAnalyzed)
    }
    
    // ========== Error Handling Tests ==========
    
    @Test
    fun `analyzeAll continues analysis when analyzer throws exception`() = runTest {
        // Given: File scanner returns files
        val files = listOf(createFileInfo("File1.kt"))
        every { fileScanner.scanDirectory(any()) } returns files
        every { fileScanner.filterFiles(files) } returns files
        
        // Mock file reading
        every { File(any<String>()).readText() } returns "file content"
        
        // Mock aggregation (should still be called)
        val aggregatedResult = createAggregatedResult(emptyList())
        every { resultAggregator.aggregate(any()) } returns aggregatedResult
        
        // Mock reporting and baseline
        every { reportGenerator.generate(any(), any()) } returns "report"
        every { baselineManager.loadBaseline() } returns null
        every { baselineManager.saveBaseline(any(), any()) } just Runs
        every { baselineManager.saveToHistory(any()) } just Runs
        
        // Mock report saving
        mockkConstructor(File::class)
        every { anyConstructed<File>().exists() } returns true
        every { anyConstructed<File>().mkdirs() } returns true
        every { anyConstructed<File>().writeText(any()) } just Runs
        
        // When: Analyze all (analyzers may throw exceptions internally)
        val result = orchestrator.analyzeAll()
        
        // Then: Analysis should complete
        verify { resultAggregator.aggregate(any()) }
    }
    
    // ========== Helper Methods ==========
    
    private fun createFileInfo(
        path: String,
        relativePath: String = path,
        layer: CodeLayer? = CodeLayer.UI
    ): FileInfo {
        return FileInfo(
            path = path,
            relativePath = relativePath,
            size = 1000,
            lastModified = System.currentTimeMillis(),
            layer = layer
        )
    }
    
    private fun createFinding(
        id: String = "test-id",
        file: String = "Test.kt"
    ): Finding {
        return Finding(
            id = id,
            analyzer = "test-analyzer",
            category = AnalysisCategory.CODE_SMELL,
            priority = Priority.MEDIUM,
            title = "Test Finding",
            description = "Test description",
            file = file,
            lineNumber = 1,
            columnNumber = null,
            codeSnippet = "code snippet",
            recommendation = "Fix this issue",
            beforeExample = null,
            afterExample = null,
            autoFixable = false,
            autoFix = null,
            effort = Effort.SMALL,
            references = emptyList(),
            relatedFindings = emptyList()
        )
    }
    
    private fun createAggregatedResult(findings: List<Finding>): AggregatedResult {
        val metrics = resultAggregator.calculateMetrics(findings)
        return AggregatedResult(
            findings = findings,
            metrics = metrics,
            byCategory = findings.groupBy { it.category },
            byPriority = findings.groupBy { it.priority },
            byFile = findings.groupBy { it.file }
        )
    }
    
    private fun createBaseline(): Baseline {
        return Baseline(
            timestamp = System.currentTimeMillis(),
            metrics = resultAggregator.calculateMetrics(emptyList()),
            findingIds = emptySet()
        )
    }
}
