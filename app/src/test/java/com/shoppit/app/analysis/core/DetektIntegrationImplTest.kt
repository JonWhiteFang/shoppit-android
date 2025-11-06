package com.shoppit.app.analysis.core

import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.Effort
import com.shoppit.app.analysis.models.Priority
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Severity
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests for DetektIntegrationImpl.
 * 
 * Tests the integration with Detekt static analysis tool, including:
 * - Running Detekt analysis
 * - Converting Detekt issues to Finding model
 * - Mapping rule sets to categories
 * - Mapping severity to priority
 * - Mapping debt to effort
 * 
 * Requirements: 17.1, 17.2
 */
class DetektIntegrationImplTest {
    
    private lateinit var detektIntegration: DetektIntegrationImpl
    private lateinit var projectRoot: String
    
    @Before
    fun setup() {
        // Use a temporary directory for testing
        projectRoot = System.getProperty("user.dir")
        detektIntegration = DetektIntegrationImpl(projectRoot)
    }
    
    /**
     * Test that convertIssue creates a Finding with correct properties.
     */
    @Test
    fun `convertIssue creates Finding with correct properties`() {
        // Given
        val issue = Issue(
            id = "LongMethod",
            severity = Severity.CodeSmell,
            description = "Method is too long",
            debt = Debt(mins = 10)
        )
        val file = "app/src/main/java/com/shoppit/app/Example.kt"
        val line = 42
        
        // When
        val finding = detektIntegration.convertIssue(issue, file, line)
        
        // Then
        assertEquals("detekt-LongMethod-$file-$line", finding.id)
        assertEquals("detekt", finding.analyzer)
        assertEquals(file, finding.file)
        assertEquals(line, finding.lineNumber)
        assertEquals("LongMethod", finding.title)
        assertEquals("Method is too long", finding.description)
        assertEquals(Priority.LOW, finding.priority)
        assertEquals(Effort.SMALL, finding.effort)
        assertTrue(finding.references.isNotEmpty())
        assertTrue(finding.references[0].contains("detekt.dev"))
    }
    
    /**
     * Test that convertIssue maps CodeSmell severity to LOW priority.
     */
    @Test
    fun `convertIssue maps CodeSmell severity to LOW priority`() {
        // Given
        val issue = Issue(
            id = "MagicNumber",
            severity = Severity.CodeSmell,
            description = "Magic number detected",
            debt = Debt(mins = 5)
        )
        
        // When
        val finding = detektIntegration.convertIssue(issue, "test.kt", 1)
        
        // Then
        assertEquals(Priority.LOW, finding.priority)
    }
    
    /**
     * Test that convertIssue maps Warning severity to MEDIUM priority.
     */
    @Test
    fun `convertIssue maps Warning severity to MEDIUM priority`() {
        // Given
        val issue = Issue(
            id = "ComplexMethod",
            severity = Severity.Warning,
            description = "Method is too complex",
            debt = Debt(mins = 30)
        )
        
        // When
        val finding = detektIntegration.convertIssue(issue, "test.kt", 1)
        
        // Then
        assertEquals(Priority.MEDIUM, finding.priority)
    }
    
    /**
     * Test that convertIssue maps Defect severity to HIGH priority.
     */
    @Test
    fun `convertIssue maps Defect severity to HIGH priority`() {
        // Given
        val issue = Issue(
            id = "UnusedPrivateMember",
            severity = Severity.Defect,
            description = "Unused private member",
            debt = Debt(mins = 15)
        )
        
        // When
        val finding = detektIntegration.convertIssue(issue, "test.kt", 1)
        
        // Then
        assertEquals(Priority.HIGH, finding.priority)
    }
    
    /**
     * Test that convertIssue maps Security severity to CRITICAL priority.
     */
    @Test
    fun `convertIssue maps Security severity to CRITICAL priority`() {
        // Given
        val issue = Issue(
            id = "HardcodedPassword",
            severity = Severity.Security,
            description = "Hardcoded password detected",
            debt = Debt(mins = 60)
        )
        
        // When
        val finding = detektIntegration.convertIssue(issue, "test.kt", 1)
        
        // Then
        assertEquals(Priority.CRITICAL, finding.priority)
    }
    
    /**
     * Test that convertIssue maps Performance severity to MEDIUM priority.
     */
    @Test
    fun `convertIssue maps Performance severity to MEDIUM priority`() {
        // Given
        val issue = Issue(
            id = "SpreadOperator",
            severity = Severity.Performance,
            description = "Spread operator usage",
            debt = Debt(mins = 20)
        )
        
        // When
        val finding = detektIntegration.convertIssue(issue, "test.kt", 1)
        
        // Then
        assertEquals(Priority.MEDIUM, finding.priority)
    }
    
    /**
     * Test that convertIssue maps debt to TRIVIAL effort for <= 5 minutes.
     */
    @Test
    fun `convertIssue maps debt to TRIVIAL effort for 5 minutes or less`() {
        // Given
        val issue = Issue(
            id = "UnusedImport",
            severity = Severity.Style,
            description = "Unused import",
            debt = Debt(mins = 5)
        )
        
        // When
        val finding = detektIntegration.convertIssue(issue, "test.kt", 1)
        
        // Then
        assertEquals(Effort.TRIVIAL, finding.effort)
    }
    
    /**
     * Test that convertIssue maps debt to SMALL effort for 6-30 minutes.
     */
    @Test
    fun `convertIssue maps debt to SMALL effort for 6 to 30 minutes`() {
        // Given
        val issue = Issue(
            id = "LongParameterList",
            severity = Severity.CodeSmell,
            description = "Too many parameters",
            debt = Debt(mins = 15)
        )
        
        // When
        val finding = detektIntegration.convertIssue(issue, "test.kt", 1)
        
        // Then
        assertEquals(Effort.SMALL, finding.effort)
    }
    
    /**
     * Test that convertIssue maps debt to MEDIUM effort for 31-120 minutes.
     */
    @Test
    fun `convertIssue maps debt to MEDIUM effort for 31 to 120 minutes`() {
        // Given
        val issue = Issue(
            id = "ComplexMethod",
            severity = Severity.Warning,
            description = "Method is too complex",
            debt = Debt(mins = 60)
        )
        
        // When
        val finding = detektIntegration.convertIssue(issue, "test.kt", 1)
        
        // Then
        assertEquals(Effort.MEDIUM, finding.effort)
    }
    
    /**
     * Test that convertIssue maps debt to LARGE effort for > 120 minutes.
     */
    @Test
    fun `convertIssue maps debt to LARGE effort for more than 120 minutes`() {
        // Given
        val issue = Issue(
            id = "LargeClass",
            severity = Severity.Maintainability,
            description = "Class is too large",
            debt = Debt(mins = 180)
        )
        
        // When
        val finding = detektIntegration.convertIssue(issue, "test.kt", 1)
        
        // Then
        assertEquals(Effort.LARGE, finding.effort)
    }
    
    /**
     * Test that runDetekt returns empty list when config file doesn't exist.
     */
    @Test
    fun `runDetekt returns failure when config file does not exist`() = runTest {
        // Given
        val paths = listOf("app/src/main/java")
        val nonExistentConfig = "non-existent-config.yml"
        
        // When
        val result = detektIntegration.runDetekt(paths, nonExistentConfig)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
    
    /**
     * Test that runDetekt returns empty list when no valid paths are provided.
     */
    @Test
    fun `runDetekt returns empty list when no valid paths exist`() = runTest {
        // Given
        val paths = listOf("non-existent-path")
        val configPath = "app/detekt-config.yml"
        
        // When
        val result = detektIntegration.runDetekt(paths, configPath)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }
}
