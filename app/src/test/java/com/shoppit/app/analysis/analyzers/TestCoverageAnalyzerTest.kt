package com.shoppit.app.analysis.analyzers

import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.CodeLayer
import com.shoppit.app.analysis.models.FileInfo
import com.shoppit.app.analysis.models.Priority
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests for TestCoverageAnalyzer.
 * Tests validation of test coverage including:
 * - ViewModel test detection
 * - Use case test detection
 * - Repository test detection
 * - Test file naming convention validation
 */
class TestCoverageAnalyzerTest {
    
    private lateinit var analyzer: TestCoverageAnalyzer
    
    @Before
    fun setup() {
        analyzer = TestCoverageAnalyzer()
    }
    
    @Test
    fun `analyzer has correct properties`() {
        assertEquals("test-coverage", analyzer.id)
        assertEquals("Test Coverage Analyzer", analyzer.name)
        assertEquals(AnalysisCategory.TEST_COVERAGE, analyzer.category)
    }
    
    @Test
    fun `appliesTo returns true for Kotlin files in main directory`() {
        val file = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt",
            relativePath = "app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        assertTrue(analyzer.appliesTo(file))
    }
    
    @Test
    fun `appliesTo returns true for Kotlin files in test directory`() {
        val file = FileInfo(
            path = "/app/src/test/java/com/shoppit/app/ui/meal/MealViewModelTest.kt",
            relativePath = "app/src/test/java/com/shoppit/app/ui/meal/MealViewModelTest.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.TEST
        )
        
        assertTrue(analyzer.appliesTo(file))
    }
    
    @Test
    fun `appliesTo returns false for non-Kotlin files`() {
        val file = FileInfo(
            path = "/resources/layout.xml",
            relativePath = "resources/layout.xml",
            size = 100L,
            lastModified = 0L,
            layer = null
        )
        
        assertFalse(analyzer.appliesTo(file))
    }
    
    @Test
    fun `appliesTo returns false for Kotlin files not in main or test directories`() {
        val file = FileInfo(
            path = "/build/generated/source/Meal.kt",
            relativePath = "build/generated/source/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = null
        )
        
        assertFalse(analyzer.appliesTo(file))
    }
    
    // ViewModel Test Detection (Requirement 11.1)
    
    @Test
    fun `detects missing test file for ViewModel`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt",
            relativePath = "app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.lifecycle.ViewModel
            import dagger.hilt.android.lifecycle.HiltViewModel
            import javax.inject.Inject
            
            @HiltViewModel
            class MealViewModel @Inject constructor() : ViewModel() {
                // ViewModel implementation
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing Test File for ViewModel") })
        val finding = findings.first { it.title.contains("Missing Test File for ViewModel") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("MealViewModel"))
        assertTrue(finding.recommendation.contains("MealViewModelTest.kt"))
        assertTrue(finding.recommendation.contains("Initial state verification"))
    }
    
    @Test
    fun `detects ViewModel by HiltViewModel annotation`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/ui/planner/PlannerViewModel.kt",
            relativePath = "app/src/main/java/com/shoppit/app/ui/planner/PlannerViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.planner
            
            import dagger.hilt.android.lifecycle.HiltViewModel
            
            @HiltViewModel
            class PlannerViewModel {
                // ViewModel implementation
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing Test File for ViewModel") })
    }
    
    @Test
    fun `detects ViewModel by ViewModel parent class`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/ui/shopping/ShoppingViewModel.kt",
            relativePath = "app/src/main/java/com/shoppit/app/ui/shopping/ShoppingViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.shopping
            
            import androidx.lifecycle.ViewModel
            
            class ShoppingViewModel : ViewModel() {
                // ViewModel implementation
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing Test File for ViewModel") })
    }
    
    @Test
    fun `does not flag ViewModel file without ViewModel indicators`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt",
            relativePath = "app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            // This is not actually a ViewModel
            class MealViewModel {
                // Regular class
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing Test File for ViewModel") })
    }
    
    // Use Case Test Detection (Requirement 11.2)
    
    @Test
    fun `detects missing test file for UseCase`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/domain/usecase/AddMealUseCase.kt",
            relativePath = "app/src/main/java/com/shoppit/app/domain/usecase/AddMealUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            import javax.inject.Inject
            
            class AddMealUseCase @Inject constructor() {
                suspend operator fun invoke(meal: Meal): Result<Long> {
                    // Use case implementation
                    return Result.success(1L)
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing Test File for Use Case") })
        val finding = findings.first { it.title.contains("Missing Test File for Use Case") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("AddMealUseCase"))
        assertTrue(finding.recommendation.contains("AddMealUseCaseTest.kt"))
        assertTrue(finding.recommendation.contains("Business logic validation"))
    }
    
    @Test
    fun `detects UseCase in domain layer`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/domain/usecase/GetMealsUseCase.kt",
            relativePath = "app/src/main/java/com/shoppit/app/domain/usecase/GetMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            class GetMealsUseCase {
                // Use case implementation
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing Test File for Use Case") })
    }
    
    @Test
    fun `does not flag UseCase file not in domain layer`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/ui/meal/AddMealUseCase.kt",
            relativePath = "app/src/main/java/com/shoppit/app/ui/meal/AddMealUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            // This is not in the domain layer
            class AddMealUseCase {
                // Not a real use case
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing Test File for Use Case") })
    }
    
    // Repository Test Detection (Requirement 11.3)
    
    @Test
    fun `detects missing test file for Repository`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/data/repository/MealRepositoryImpl.kt",
            relativePath = "app/src/main/java/com/shoppit/app/data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor() : MealRepository {
                // Repository implementation
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing Test File for Repository") })
        val finding = findings.first { it.title.contains("Missing Test File for Repository") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("MealRepositoryImpl"))
        assertTrue(finding.recommendation.contains("MealRepositoryImplTest.kt"))
        assertTrue(finding.recommendation.contains("Data flow verification"))
    }
    
    @Test
    fun `detects Repository in data layer`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/data/repository/ShoppingListRepositoryImpl.kt",
            relativePath = "app/src/main/java/com/shoppit/app/data/repository/ShoppingListRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            class ShoppingListRepositoryImpl {
                // Repository implementation
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing Test File for Repository") })
    }
    
    @Test
    fun `does not flag Repository file not in data layer`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/domain/MealRepositoryImpl.kt",
            relativePath = "app/src/main/java/com/shoppit/app/domain/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain
            
            // This is not in the data layer
            class MealRepositoryImpl {
                // Not a real repository
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing Test File for Repository") })
    }
    
    // Test File Naming Convention (Requirement 11.4)
    
    @Test
    fun `detects test file not following naming convention`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/test/java/com/shoppit/app/ui/meal/MealViewModelTests.kt",
            relativePath = "app/src/test/java/com/shoppit/app/ui/meal/MealViewModelTests.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.TEST
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            class MealViewModelTests {
                // Test implementation
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Test File Does Not Follow Naming Convention") })
        val finding = findings.first { it.title.contains("Test File Does Not Follow Naming Convention") }
        assertEquals(Priority.LOW, finding.priority)
        assertTrue(finding.description.contains("MealViewModelTests.kt"))
        assertTrue(finding.recommendation.contains("[ClassName]Test.kt"))
    }
    
    @Test
    fun `detects test file with incorrect suffix`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/test/java/com/shoppit/app/domain/usecase/AddMealUseCaseSpec.kt",
            relativePath = "app/src/test/java/com/shoppit/app/domain/usecase/AddMealUseCaseSpec.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.TEST
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            class AddMealUseCaseSpec {
                // Test implementation
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Test File Does Not Follow Naming Convention") })
    }
    
    @Test
    fun `accepts valid test file naming convention`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/test/java/com/shoppit/app/ui/meal/MealViewModelTest.kt",
            relativePath = "app/src/test/java/com/shoppit/app/ui/meal/MealViewModelTest.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.TEST
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            class MealViewModelTest {
                // Test implementation
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Test File Does Not Follow Naming Convention") })
    }
    
    @Test
    fun `accepts test file in androidTest directory`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/androidTest/java/com/shoppit/app/data/local/dao/MealDaoTest.kt",
            relativePath = "app/src/androidTest/java/com/shoppit/app/data/local/dao/MealDaoTest.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.TEST
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            class MealDaoTest {
                // Instrumented test implementation
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Test File Does Not Follow Naming Convention") })
    }
    
    // General Tests
    
    @Test
    fun `does not flag non-testable components`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/domain/model/Meal.kt",
            relativePath = "app/src/main/java/com/shoppit/app/domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            data class Meal(
                val id: Long,
                val name: String
            )
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing Test File") })
    }
    
    @Test
    fun `does not flag UI screens`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/ui/meal/MealListScreen.kt",
            relativePath = "app/src/main/java/com/shoppit/app/ui/meal/MealListScreen.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            
            @Composable
            fun MealListScreen() {
                // Composable implementation
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing Test File") })
    }
    
    @Test
    fun `handles empty file`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt",
            relativePath = "app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = ""
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should not crash, may or may not have findings
        assertNotNull(findings)
    }
    
    @Test
    fun `finding IDs are unique`() = runTest {
        val fileInfo1 = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt",
            relativePath = "app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code1 = """
            package com.shoppit.app.ui.meal
            
            import dagger.hilt.android.lifecycle.HiltViewModel
            
            @HiltViewModel
            class MealViewModel {
            }
        """.trimIndent()
        
        val fileInfo2 = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/domain/usecase/AddMealUseCase.kt",
            relativePath = "app/src/main/java/com/shoppit/app/domain/usecase/AddMealUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code2 = """
            package com.shoppit.app.domain.usecase
            
            class AddMealUseCase {
            }
        """.trimIndent()
        
        val findings1 = analyzer.analyze(fileInfo1, code1)
        val findings2 = analyzer.analyze(fileInfo2, code2)
        
        val allFindings = findings1 + findings2
        val ids = allFindings.map { it.id }
        
        // All IDs should be unique
        assertEquals(ids.size, ids.toSet().size)
    }
    
    @Test
    fun `findings include helpful recommendations and examples`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt",
            relativePath = "app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import dagger.hilt.android.lifecycle.HiltViewModel
            
            @HiltViewModel
            class MealViewModel {
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.isNotEmpty())
        findings.forEach { finding ->
            assertNotNull(finding.recommendation)
            assertTrue(finding.recommendation.isNotEmpty())
            assertNotNull(finding.beforeExample)
            assertNotNull(finding.afterExample)
            assertTrue(finding.references.isNotEmpty())
            assertEquals(Effort.MEDIUM, finding.effort)
        }
    }
    
    @Test
    fun `all missing test findings have MEDIUM priority`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt",
            relativePath = "app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import dagger.hilt.android.lifecycle.HiltViewModel
            
            @HiltViewModel
            class MealViewModel {
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        val missingTestFindings = findings.filter { it.title.contains("Missing Test File") }
        assertTrue(missingTestFindings.isNotEmpty())
        missingTestFindings.forEach { finding ->
            assertEquals(Priority.MEDIUM, finding.priority)
        }
    }
    
    @Test
    fun `test naming violations have LOW priority`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/test/java/com/shoppit/app/ui/meal/MealViewModelTests.kt",
            relativePath = "app/src/test/java/com/shoppit/app/ui/meal/MealViewModelTests.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.TEST
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            class MealViewModelTests {
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        val namingFindings = findings.filter { it.title.contains("Test File Does Not Follow Naming Convention") }
        assertTrue(namingFindings.isNotEmpty())
        namingFindings.forEach { finding ->
            assertEquals(Priority.LOW, finding.priority)
        }
    }
    
    @Test
    fun `handles complex file paths`() = runTest {
        val fileInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/ui/meal/list/MealListViewModel.kt",
            relativePath = "app/src/main/java/com/shoppit/app/ui/meal/list/MealListViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal.list
            
            import dagger.hilt.android.lifecycle.HiltViewModel
            
            @HiltViewModel
            class MealListViewModel {
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing Test File for ViewModel") })
        val finding = findings.first { it.title.contains("Missing Test File for ViewModel") }
        assertTrue(finding.description.contains("MealListViewModel"))
    }
    
    @Test
    fun `provides component-specific test recommendations`() = runTest {
        val viewModelInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt",
            relativePath = "app/src/main/java/com/shoppit/app/ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val viewModelCode = """
            @HiltViewModel
            class MealViewModel {
            }
        """.trimIndent()
        
        val useCaseInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/domain/usecase/AddMealUseCase.kt",
            relativePath = "app/src/main/java/com/shoppit/app/domain/usecase/AddMealUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val useCaseCode = """
            class AddMealUseCase {
            }
        """.trimIndent()
        
        val repositoryInfo = FileInfo(
            path = "/app/src/main/java/com/shoppit/app/data/repository/MealRepositoryImpl.kt",
            relativePath = "app/src/main/java/com/shoppit/app/data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val repositoryCode = """
            class MealRepositoryImpl {
            }
        """.trimIndent()
        
        val viewModelFindings = analyzer.analyze(viewModelInfo, viewModelCode)
        val useCaseFindings = analyzer.analyze(useCaseInfo, useCaseCode)
        val repositoryFindings = analyzer.analyze(repositoryInfo, repositoryCode)
        
        // Each should have different recommendations
        val viewModelRec = viewModelFindings.first().recommendation
        val useCaseRec = useCaseFindings.first().recommendation
        val repositoryRec = repositoryFindings.first().recommendation
        
        assertTrue(viewModelRec.contains("Initial state"))
        assertTrue(useCaseRec.contains("Business logic"))
        assertTrue(repositoryRec.contains("Data flow"))
        
        // Each should have different examples
        val viewModelExample = viewModelFindings.first().afterExample
        val useCaseExample = useCaseFindings.first().afterExample
        val repositoryExample = repositoryFindings.first().afterExample
        
        assertNotNull(viewModelExample)
        assertNotNull(useCaseExample)
        assertNotNull(repositoryExample)
        
        assertTrue(viewModelExample!!.contains("viewModel"))
        assertTrue(useCaseExample!!.contains("useCase"))
        assertTrue(repositoryExample!!.contains("repository"))
    }
}
