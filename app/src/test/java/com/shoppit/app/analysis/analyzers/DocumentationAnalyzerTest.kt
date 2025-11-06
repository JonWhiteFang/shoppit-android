package com.shoppit.app.analysis.analyzers

import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.CodeLayer
import com.shoppit.app.analysis.models.FileInfo
import com.shoppit.app.analysis.models.Priority
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DocumentationAnalyzer.
 * Tests validation of documentation quality including:
 * - KDoc comments for public API
 * - Inline comments for complex functions
 * - Data class property documentation
 * - Sealed class subclass documentation
 */
class DocumentationAnalyzerTest {
    
    private lateinit var analyzer: DocumentationAnalyzer
    
    @Before
    fun setup() {
        analyzer = DocumentationAnalyzer()
    }
    
    @Test
    fun `analyzer has correct properties`() {
        assertEquals("documentation", analyzer.id)
        assertEquals("Documentation Analyzer", analyzer.name)
        assertEquals(AnalysisCategory.DOCUMENTATION, analyzer.category)
    }
    
    @Test
    fun `appliesTo returns true for Kotlin files in main source`() {
        val file = FileInfo(
            path = "/ui/meal/MealViewModel.kt",
            relativePath = "ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        assertTrue(analyzer.appliesTo(file))
    }
    
    @Test
    fun `appliesTo returns false for test files`() {
        val file = FileInfo(
            path = "/test/MealViewModelTest.kt",
            relativePath = "test/MealViewModelTest.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.TEST
        )
        
        assertFalse(analyzer.appliesTo(file))
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
    
    // KDoc Validation Tests (Requirement 12.1)
    
    @Test
    fun `detects missing KDoc for public class`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            class Meal {
                val name: String = ""
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing KDoc Documentation for Public class") })
        val finding = findings.first { it.title.contains("Missing KDoc Documentation for Public class") }
        assertEquals(Priority.LOW, finding.priority)
        assertTrue(finding.description.contains("Meal"))
    }
    
    @Test
    fun `detects missing KDoc for public function`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/GetMealsUseCase.kt",
            relativePath = "domain/usecase/GetMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            class GetMealsUseCase {
                fun getMeals(): List<Meal> {
                    return emptyList()
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing KDoc Documentation for Public function") })
    }
    
    @Test
    fun `detects missing KDoc for public interface`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/repository/MealRepository.kt",
            relativePath = "domain/repository/MealRepository.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.repository
            
            interface MealRepository {
                fun getMeals(): List<Meal>
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing KDoc Documentation for Public interface") })
    }
    
    @Test
    fun `accepts class with KDoc`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            /**
             * Represents a meal with ingredients.
             */
            class Meal {
                val name: String = ""
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing KDoc Documentation") })
    }
    
    @Test
    fun `accepts function with KDoc`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/GetMealsUseCase.kt",
            relativePath = "domain/usecase/GetMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            class GetMealsUseCase {
                /**
                 * Retrieves all meals from the repository.
                 * 
                 * @return List of meals
                 */
                fun getMeals(): List<Meal> {
                    return emptyList()
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing KDoc Documentation") })
    }
    
    @Test
    fun `skips private classes and functions`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            private class InternalHelper {
            }
            
            class Meal {
                private fun processData() {
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should only flag the public Meal class
        assertTrue(findings.size == 1)
        assertTrue(findings.first().description.contains("Meal"))
    }
    
    @Test
    fun `skips override functions`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealViewModel.kt",
            relativePath = "ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            class MealViewModel : ViewModel() {
                override fun onCleared() {
                    super.onCleared()
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should only flag the class, not the override function
        assertFalse(findings.any { it.description.contains("onCleared") })
    }
    
    @Test
    fun `skips test functions`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            class Meal {
                fun testHelper() {
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should only flag the class
        assertFalse(findings.any { it.description.contains("testHelper") })
    }
    
    // Inline Comment Validation Tests (Requirement 12.2)
    
    @Test
    fun `detects complex function without inline comments`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/ProcessMealsUseCase.kt",
            relativePath = "domain/usecase/ProcessMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            class ProcessMealsUseCase {
                fun processMeals(meals: List<Meal>): List<Meal> {
                    if (meals.isEmpty()) {
                        return emptyList()
                    }
                    
                    val filtered = meals.filter { it.name.isNotEmpty() }
                    
                    if (filtered.size > 10) {
                        return filtered.take(10)
                    }
                    
                    for (meal in filtered) {
                        if (meal.ingredients.isEmpty()) {
                            continue
                        }
                        
                        when (meal.category) {
                            "breakfast" -> processBreakfast(meal)
                            "lunch" -> processLunch(meal)
                            "dinner" -> processDinner(meal)
                        }
                    }
                    
                    return filtered
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Complex Function Missing Inline Comments") })
        val finding = findings.first { it.title.contains("Complex Function Missing Inline Comments") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("processMeals"))
    }
    
    @Test
    fun `accepts complex function with inline comments`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/ProcessMealsUseCase.kt",
            relativePath = "domain/usecase/ProcessMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            class ProcessMealsUseCase {
                fun processMeals(meals: List<Meal>): List<Meal> {
                    // Early return for empty list
                    if (meals.isEmpty()) {
                        return emptyList()
                    }
                    
                    // Filter out meals without names
                    val filtered = meals.filter { it.name.isNotEmpty() }
                    
                    // Limit to 10 meals for performance
                    if (filtered.size > 10) {
                        return filtered.take(10)
                    }
                    
                    // Process each meal based on category
                    for (meal in filtered) {
                        if (meal.ingredients.isEmpty()) {
                            continue
                        }
                        
                        when (meal.category) {
                            "breakfast" -> processBreakfast(meal)
                            "lunch" -> processLunch(meal)
                            "dinner" -> processDinner(meal)
                        }
                    }
                    
                    return filtered
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Complex Function Missing Inline Comments") })
    }
    
    @Test
    fun `does not flag simple functions`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/GetMealsUseCase.kt",
            relativePath = "domain/usecase/GetMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            class GetMealsUseCase {
                fun getMeals(): List<Meal> {
                    return repository.getMeals()
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Complex Function Missing Inline Comments") })
    }
    
    // Data Class Documentation Tests (Requirement 12.3)
    
    @Test
    fun `detects data class with multiple properties missing documentation`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            data class Meal(
                val id: Long,
                val name: String,
                val ingredients: List<Ingredient>,
                val notes: String?
            )
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Data Class Properties Not Documented") })
        val finding = findings.first { it.title.contains("Data Class Properties Not Documented") }
        assertEquals(Priority.LOW, finding.priority)
        assertTrue(finding.description.contains("Meal"))
    }
    
    @Test
    fun `accepts data class with property documentation`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            /**
             * Represents a meal with ingredients.
             * 
             * @property id Unique identifier
             * @property name Name of the meal
             * @property ingredients List of ingredients
             * @property notes Optional cooking notes
             */
            data class Meal(
                val id: Long,
                val name: String,
                val ingredients: List<Ingredient>,
                val notes: String?
            )
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Data Class Properties Not Documented") })
    }
    
    @Test
    fun `does not flag simple data classes with few properties`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/model/Ingredient.kt",
            relativePath = "domain/model/Ingredient.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            data class Ingredient(
                val name: String,
                val quantity: String
            )
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Simple data classes with 2 properties don't require documentation
        assertFalse(findings.any { it.title.contains("Data Class Properties Not Documented") })
    }
    
    // Sealed Class Documentation Tests (Requirement 12.4)
    
    @Test
    fun `detects sealed class subclass missing documentation`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealUiState.kt",
            relativePath = "ui/meal/MealUiState.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            sealed class MealUiState {
                data object Loading : MealUiState()
                data class Success(val meals: List<Meal>) : MealUiState()
                data class Error(val message: String) : MealUiState()
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Sealed Class Subclass Not Documented") })
        val finding = findings.first { it.title.contains("Sealed Class Subclass Not Documented") }
        assertEquals(Priority.LOW, finding.priority)
    }
    
    @Test
    fun `accepts sealed class subclasses with documentation`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealUiState.kt",
            relativePath = "ui/meal/MealUiState.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            sealed class MealUiState {
                /**
                 * Loading state while fetching meals.
                 */
                data object Loading : MealUiState()
                
                /**
                 * Success state with loaded meals.
                 */
                data class Success(val meals: List<Meal>) : MealUiState()
                
                /**
                 * Error state with error message.
                 */
                data class Error(val message: String) : MealUiState()
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Sealed Class Subclass Not Documented") })
    }
    
    // General Tests
    
    @Test
    fun `handles empty file`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/Empty.kt",
            relativePath = "domain/Empty.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = ""
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.isEmpty())
    }
    
    @Test
    fun `detects multiple documentation issues in same file`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            class Meal {
                fun processMeal() {
                    if (true) {
                        if (true) {
                            if (true) {
                                if (true) {
                                    if (true) {
                                        if (true) {
                                            if (true) {
                                                if (true) {
                                                    if (true) {
                                                        if (true) {
                                                            if (true) {
                                                                println("complex")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            data class MealData(
                val id: Long,
                val name: String,
                val description: String
            )
            
            sealed class MealResult {
                data class Success(val meal: Meal) : MealResult()
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should have multiple violations
        assertTrue(findings.size >= 4)
        assertTrue(findings.any { it.title.contains("Missing KDoc") })
        assertTrue(findings.any { it.title.contains("Complex Function") })
        assertTrue(findings.any { it.title.contains("Data Class Properties") })
        assertTrue(findings.any { it.title.contains("Sealed Class Subclass") })
    }
    
    @Test
    fun `finding IDs are unique`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            class Meal {
                fun getMeal() {
                }
            }
            
            interface MealRepository {
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.size >= 2)
        
        // All IDs should be unique
        val ids = findings.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
    
    @Test
    fun `findings include helpful recommendations and examples`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            class Meal {
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
        }
    }
    
    @Test
    fun `handles annotations above declarations`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealViewModel.kt",
            relativePath = "ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            /**
             * ViewModel for meal list screen.
             */
            @HiltViewModel
            class MealViewModel {
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should not flag the class as it has KDoc
        assertFalse(findings.any { it.description.contains("MealViewModel") })
    }
    
    @Test
    fun `handles multiline class declarations`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            /**
             * Represents a meal.
             */
            data class Meal(
                val id: Long,
                val name: String
            )
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should not flag as it has KDoc
        assertFalse(findings.any { it.title.contains("Missing KDoc") })
    }
    
    @Test
    fun `complexity calculation is accurate`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/ProcessMealsUseCase.kt",
            relativePath = "domain/usecase/ProcessMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        // Function with exactly 11 decision points (above threshold of 10)
        val code = """
            package com.shoppit.app.domain.usecase
            
            class ProcessMealsUseCase {
                fun processMeals(meals: List<Meal>): List<Meal> {
                    if (meals.isEmpty()) return emptyList()
                    if (meals.size > 100) return emptyList()
                    if (meals.size > 50) return emptyList()
                    if (meals.size > 25) return emptyList()
                    if (meals.size > 10) return emptyList()
                    if (meals.size > 5) return emptyList()
                    if (meals.size > 3) return emptyList()
                    if (meals.size > 2) return emptyList()
                    if (meals.size > 1) return emptyList()
                    if (meals.size > 0) return emptyList()
                    if (meals.isNotEmpty()) return emptyList()
                    return meals
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should flag as complex
        assertTrue(findings.any { it.title.contains("Complex Function Missing Inline Comments") })
    }
}
