package com.shoppit.analysis.analyzers

import com.shoppit.analysis.core.CodeAnalyzer
import com.shoppit.analysis.models.AnalysisCategory
import com.shoppit.analysis.models.Effort
import com.shoppit.analysis.models.FileInfo
import com.shoppit.analysis.models.Finding
import com.shoppit.analysis.models.Priority
import java.io.File
import java.util.UUID

/**
 * Analyzer that validates test coverage for critical components.
 * 
 * Validates:
 * - ViewModels have corresponding test files
 * - Use cases have corresponding test files
 * - Repositories have corresponding test files
 * - Test files follow naming convention [ClassName]Test.kt
 * 
 * Requirements: 11.1, 11.2, 11.3, 11.4
 */
class TestCoverageAnalyzer : CodeAnalyzer {
    
    override val id: String = "test-coverage"
    
    override val name: String = "Test Coverage Analyzer"
    
    override val category: AnalysisCategory = AnalysisCategory.TEST_COVERAGE
    
    override suspend fun analyze(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Only analyze production code files (not test files)
        if (isTestFile(file)) {
            // Validate test file naming convention
            findings.addAll(validateTestFileNaming(file))
            return findings
        }
        
        // Check if this is a component that requires tests
        val componentType = detectComponentType(file, fileContent)
        
        if (componentType != null) {
            // Check for corresponding test file
            val testFilePath = getExpectedTestFilePath(file)
            val testFileExists = File(testFilePath).exists()
            
            if (!testFileExists) {
                findings.add(createMissingTestFileFinding(file, componentType, testFilePath))
            }
        }
        
        return findings
    }
    
    override fun appliesTo(file: FileInfo): Boolean {
        // Apply to all Kotlin files in main and test directories
        return file.relativePath.endsWith(".kt") &&
                (file.relativePath.contains("/main/") || file.relativePath.contains("/test/"))
    }
    
    /**
     * Checks if the file is a test file.
     */
    private fun isTestFile(file: FileInfo): Boolean {
        return file.relativePath.contains("/test/") || 
               file.relativePath.contains("/androidTest/")
    }
    
    /**
     * Detects the type of component (ViewModel, UseCase, Repository).
     * Requirement 11.1, 11.2, 11.3: Detect ViewModels, use cases, repositories.
     */
    private fun detectComponentType(file: FileInfo, fileContent: String): ComponentType? {
        val fileName = file.relativePath.substringAfterLast("/")
        
        // Check ViewModel
        if (fileName.endsWith("ViewModel.kt")) {
            // Verify it actually extends ViewModel
            if (fileContent.contains("ViewModel()") || 
                fileContent.contains("@HiltViewModel")) {
                return ComponentType.VIEW_MODEL
            }
        }
        
        // Check UseCase
        if (fileName.endsWith("UseCase.kt")) {
            // Verify it's in the domain layer
            if (file.relativePath.contains("/domain/usecase/")) {
                return ComponentType.USE_CASE
            }
        }
        
        // Check Repository
        if (fileName.endsWith("RepositoryImpl.kt")) {
            // Verify it's in the data layer
            if (file.relativePath.contains("/data/repository/")) {
                return ComponentType.REPOSITORY
            }
        }
        
        return null
    }
    
    /**
     * Gets the expected test file path for a production file.
     */
    private fun getExpectedTestFilePath(file: FileInfo): String {
        val fileName = file.relativePath.substringAfterLast("/").substringBeforeLast(".kt")
        val testFileName = "${fileName}Test.kt"
        
        // Convert main path to test path
        val testPath = file.relativePath
            .replace("/main/java/", "/test/java/")
            .replace(fileName, testFileName.substringBeforeLast(".kt"))
        
        // Get the absolute path by combining with project root
        // Assuming file.path contains the full path
        val projectRoot = file.path.substringBefore("/app/src/")
        return "$projectRoot/$testPath"
    }
    
    /**
     * Validates test file naming convention.
     * Requirement 11.4: Verify tests follow naming convention [ClassName]Test.kt.
     */
    private fun validateTestFileNaming(file: FileInfo): List<Finding> {
        val findings = mutableListOf<Finding>()
        val fileName = file.relativePath.substringAfterLast("/")
        
        // Test files should end with Test.kt
        if (!fileName.endsWith("Test.kt")) {
            findings.add(createTestNamingViolationFinding(file, fileName))
        }
        
        return findings
    }
    
    /**
     * Creates a finding for missing test file.
     * Requirement 11.1, 11.2, 11.3: Report untested components.
     */
    private fun createMissingTestFileFinding(
        file: FileInfo,
        componentType: ComponentType,
        expectedTestPath: String
    ): Finding {
        val className = file.relativePath.substringAfterLast("/").substringBeforeLast(".kt")
        val testClassName = "${className}Test"
        
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Missing Test File for ${componentType.displayName}",
            description = "${componentType.displayName} '$className' does not have a corresponding test file. " +
                    "Critical components like ${componentType.displayName}s should have comprehensive unit tests " +
                    "to ensure business logic correctness and prevent regressions. Tests help document expected " +
                    "behavior and make refactoring safer.",
            file = file.relativePath,
            lineNumber = 1,
            codeSnippet = "class $className",
            recommendation = "Create a test file named '${testClassName}.kt' in the corresponding test directory. " +
                    "The test should cover:\n" +
                    componentType.getTestRecommendations(),
            beforeExample = "// No test file exists",
            afterExample = """
                // ${testClassName}.kt
                class ${testClassName} {
                    ${componentType.getTestExample(className)}
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.MEDIUM,
            references = listOf(
                "Project testing-strategy.md: ${componentType.displayName} Testing",
                "https://developer.android.com/training/testing/unit-testing/local-unit-tests"
            )
        )
    }
    
    /**
     * Creates a finding for test file naming violations.
     */
    private fun createTestNamingViolationFinding(
        file: FileInfo,
        fileName: String
    ): Finding {
        val suggestedName = if (fileName.contains("Test")) {
            fileName.replace(Regex("Test(?!\\.)"), "") + "Test.kt"
        } else {
            fileName.substringBeforeLast(".kt") + "Test.kt"
        }
        
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.LOW,
            title = "Test File Does Not Follow Naming Convention",
            description = "Test file '$fileName' does not follow the project's naming convention. Test files " +
                    "should be named '[ClassName]Test.kt' to clearly indicate they are tests and to match the " +
                    "class they are testing. This convention makes it easy to find tests and ensures consistency " +
                    "across the codebase.",
            file = file.relativePath,
            lineNumber = 1,
            codeSnippet = "File: $fileName",
            recommendation = "Rename the test file to follow the '[ClassName]Test.kt' convention. The test file " +
                    "name should match the production class name with 'Test' appended.",
            beforeExample = "File: $fileName",
            afterExample = "File: $suggestedName",
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "Project testing-strategy.md: Test Naming Convention",
                "https://kotlinlang.org/docs/coding-conventions.html#naming-rules"
            )
        )
    }
    
    /**
     * Enum representing component types that require tests.
     */
    private enum class ComponentType(
        val displayName: String
    ) {
        VIEW_MODEL("ViewModel") {
            override fun getTestRecommendations(): String {
                return """
                    - Initial state verification
                    - User action handling
                    - State transitions
                    - Error handling
                    - Loading states
                """.trimIndent()
            }
            
            override fun getTestExample(className: String): String {
                return """
                    @Test
                    fun `initial state is loading`() {
                        assertEquals(UiState.Loading, viewModel.uiState.value)
                    }
                    
                    @Test
                    fun `loads data successfully`() = runTest {
                        // Given
                        coEvery { useCase() } returns flowOf(Result.success(data))
                        
                        // When
                        viewModel.loadData()
                        advanceUntilIdle()
                        
                        // Then
                        assertTrue(viewModel.uiState.value is UiState.Success)
                    }
                """.trimIndent()
            }
        },
        
        USE_CASE("Use Case") {
            override fun getTestRecommendations(): String {
                return """
                    - Business logic validation
                    - Input validation
                    - Error handling
                    - Edge cases
                    - Repository interaction
                """.trimIndent()
            }
            
            override fun getTestExample(className: String): String {
                return """
                    @Test
                    fun `executes successfully with valid input`() = runTest {
                        // Given
                        val input = validInput()
                        coEvery { repository.getData() } returns Result.success(data)
                        
                        // When
                        val result = useCase(input)
                        
                        // Then
                        assertTrue(result.isSuccess)
                        coVerify { repository.getData() }
                    }
                """.trimIndent()
            }
        },
        
        REPOSITORY("Repository") {
            override fun getTestRecommendations(): String {
                return """
                    - Data flow verification
                    - Error mapping
                    - Caching logic
                    - DAO/API interaction
                    - Exception handling
                """.trimIndent()
            }
            
            override fun getTestExample(className: String): String {
                return """
                    @Test
                    fun `returns data from DAO`() = runTest {
                        // Given
                        val entities = listOf(entity1, entity2)
                        every { dao.getData() } returns flowOf(entities)
                        
                        // When
                        val result = repository.getData().first()
                        
                        // Then
                        assertTrue(result.isSuccess)
                        assertEquals(2, result.getOrNull()?.size)
                    }
                """.trimIndent()
            }
        };
        
        abstract fun getTestRecommendations(): String
        abstract fun getTestExample(className: String): String
    }
}
