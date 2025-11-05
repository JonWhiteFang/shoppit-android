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
 * Unit tests for NamingAnalyzer.
 * Tests validation of naming conventions including:
 * - File naming conventions
 * - Class names (PascalCase)
 * - Function names (camelCase)
 * - Constants (UPPER_SNAKE_CASE)
 * - Private mutable state (underscore prefix)
 */
class NamingAnalyzerTest {
    
    private lateinit var analyzer: NamingAnalyzer
    
    @Before
    fun setup() {
        analyzer = NamingAnalyzer()
    }
    
    @Test
    fun `analyzer has correct properties`() {
        assertEquals("naming", analyzer.id)
        assertEquals("Naming Analyzer", analyzer.name)
        assertEquals(AnalysisCategory.NAMING, analyzer.category)
    }
    
    @Test
    fun `appliesTo returns true for all Kotlin files`() {
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
    
    // File Naming Tests (Requirement 10.1)
    
    @Test
    fun `detects file name not in PascalCase`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/meal_view_model.kt",
            relativePath = "ui/meal/meal_view_model.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            class MealViewModel {
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("File Name Does Not Follow PascalCase") })
        val finding = findings.first { it.title.contains("File Name Does Not Follow PascalCase") }
        assertEquals(Priority.LOW, finding.priority)
        assertTrue(finding.description.contains("meal_view_model"))
    }
    
    @Test
    fun `accepts valid PascalCase file name`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealViewModel.kt",
            relativePath = "ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            class MealViewModel {
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("File Name Does Not Follow PascalCase") })
    }
    
    // Class Naming Tests (Requirement 10.2)
    
    @Test
    fun `detects class name not in PascalCase`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            class meal_item {
                val name: String = ""
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Class Name Does Not Follow PascalCase") })
        val finding = findings.first { it.title.contains("Class Name Does Not Follow PascalCase") }
        assertEquals(Priority.LOW, finding.priority)
        assertTrue(finding.description.contains("meal_item"))
    }
    
    @Test
    fun `detects data class name not in PascalCase`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            data class mealItem(
                val name: String
            )
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Class Name Does Not Follow PascalCase") })
    }
    
    @Test
    fun `detects sealed class name not in PascalCase`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/model/Result.kt",
            relativePath = "domain/model/Result.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            sealed class api_result {
                data class Success(val data: String) : api_result()
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Class Name Does Not Follow PascalCase") })
    }
    
    @Test
    fun `detects interface name not in PascalCase`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/repository/MealRepository.kt",
            relativePath = "domain/repository/MealRepository.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.repository
            
            interface meal_repository {
                fun getMeals(): List<Meal>
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Class Name Does Not Follow PascalCase") })
    }
    
    @Test
    fun `detects object name not in PascalCase`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/Constants.kt",
            relativePath = "domain/Constants.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain
            
            object app_constants {
                const val MAX_COUNT = 100
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Class Name Does Not Follow PascalCase") })
    }
    
    @Test
    fun `accepts valid PascalCase class names`() = runTest {
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
                val name: String
            )
            
            class MealProcessor {
            }
            
            sealed class MealResult {
            }
            
            interface MealRepository {
            }
            
            object MealConstants {
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Class Name Does Not Follow PascalCase") })
    }
    
    // Function Naming Tests (Requirement 10.3)
    
    @Test
    fun `detects function name not in camelCase`() = runTest {
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
                fun Get_Meals(): List<Meal> {
                    return emptyList()
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Function Name Does Not Follow camelCase") })
        val finding = findings.first { it.title.contains("Function Name Does Not Follow camelCase") }
        assertEquals(Priority.LOW, finding.priority)
        assertTrue(finding.description.contains("Get_Meals"))
    }
    
    @Test
    fun `detects function name starting with uppercase`() = runTest {
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
                fun GetMeals(): List<Meal> {
                    return emptyList()
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Function Name Does Not Follow camelCase") })
    }
    
    @Test
    fun `accepts valid camelCase function names`() = runTest {
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
                
                fun loadMealData(): Unit {
                }
                
                private fun processItems(): Unit {
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Function Name Does Not Follow camelCase") })
    }
    
    @Test
    fun `skips operator functions`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            data class Meal(val name: String) {
                operator fun plus(other: Meal): Meal {
                    return this
                }
                
                operator fun get(index: Int): String {
                    return name
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Function Name Does Not Follow camelCase") })
    }
    
    @Test
    fun `skips test functions`() = runTest {
        val fileInfo = FileInfo(
            path = "/test/MealViewModelTest.kt",
            relativePath = "test/MealViewModelTest.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.TEST
        )
        
        val code = """
            package com.shoppit.app.test
            
            class MealViewModelTest {
                fun testLoadMeals() {
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Function Name Does Not Follow camelCase") })
    }
    
    // Constant Naming Tests (Requirement 10.4)
    
    @Test
    fun `detects const val not in UPPER_SNAKE_CASE`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/Constants.kt",
            relativePath = "domain/Constants.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain
            
            const val maxCount = 100
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Constant Does Not Follow UPPER_SNAKE_CASE") })
        val finding = findings.first { it.title.contains("Constant Does Not Follow UPPER_SNAKE_CASE") }
        assertEquals(Priority.LOW, finding.priority)
        assertTrue(finding.description.contains("maxCount"))
    }
    
    @Test
    fun `detects companion object val not in UPPER_SNAKE_CASE`() = runTest {
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
                companion object {
                    val defaultName = "Untitled"
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Constant Does Not Follow UPPER_SNAKE_CASE") })
    }
    
    @Test
    fun `accepts valid UPPER_SNAKE_CASE constants`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/Constants.kt",
            relativePath = "domain/Constants.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain
            
            const val MAX_COUNT = 100
            const val DEFAULT_NAME = "Untitled"
            const val API_VERSION = 1
            
            class Config {
                companion object {
                    val MAX_RETRIES = 3
                    val TIMEOUT_MS = 5000
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Constant Does Not Follow UPPER_SNAKE_CASE") })
    }
    
    @Test
    fun `skips properties with custom getters`() = runTest {
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
                companion object {
                    val defaultName: String
                        get() = "Untitled"
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Constant Does Not Follow UPPER_SNAKE_CASE") })
    }
    
    // Private Mutable State Tests (Requirement 10.5)
    
    @Test
    fun `detects private MutableStateFlow without underscore prefix`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealViewModel.kt",
            relativePath = "ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import kotlinx.coroutines.flow.MutableStateFlow
            
            class MealViewModel {
                private val uiState = MutableStateFlow<String>("")
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Private Mutable State Missing Underscore Prefix") })
        val finding = findings.first { it.title.contains("Private Mutable State Missing Underscore Prefix") }
        assertEquals(Priority.LOW, finding.priority)
        assertTrue(finding.description.contains("uiState"))
    }
    
    @Test
    fun `detects private MutableSharedFlow without underscore prefix`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealViewModel.kt",
            relativePath = "ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import kotlinx.coroutines.flow.MutableSharedFlow
            
            class MealViewModel {
                private val events = MutableSharedFlow<String>()
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Private Mutable State Missing Underscore Prefix") })
    }
    
    @Test
    fun `detects private mutableStateOf without underscore prefix`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealScreen.kt",
            relativePath = "ui/meal/MealScreen.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.mutableStateOf
            
            class MealScreen {
                private val isLoading = mutableStateOf(false)
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Private Mutable State Missing Underscore Prefix") })
    }
    
    @Test
    fun `accepts private mutable state with underscore prefix`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealViewModel.kt",
            relativePath = "ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import kotlinx.coroutines.flow.MutableStateFlow
            import kotlinx.coroutines.flow.StateFlow
            import kotlinx.coroutines.flow.asStateFlow
            
            class MealViewModel {
                private val _uiState = MutableStateFlow<String>("")
                val uiState: StateFlow<String> = _uiState.asStateFlow()
                
                private val _events = MutableSharedFlow<String>()
                val events = _events.asSharedFlow()
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Private Mutable State Missing Underscore Prefix") })
    }
    
    @Test
    fun `does not flag non-mutable state properties`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealViewModel.kt",
            relativePath = "ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import kotlinx.coroutines.flow.StateFlow
            
            class MealViewModel {
                private val uiState: StateFlow<String> = TODO()
                private val count: Int = 0
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Private Mutable State Missing Underscore Prefix") })
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
        
        // Should only have file naming violation
        assertTrue(findings.size <= 1)
    }
    
    @Test
    fun `detects multiple naming violations in same file`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/meal_screen.kt",
            relativePath = "ui/meal/meal_screen.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import kotlinx.coroutines.flow.MutableStateFlow
            
            class meal_screen {
                private val uiState = MutableStateFlow<String>("")
                
                fun Load_Data() {
                }
                
                companion object {
                    const val maxCount = 100
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should have multiple violations
        assertTrue(findings.size >= 4)
        assertTrue(findings.any { it.title.contains("File Name") })
        assertTrue(findings.any { it.title.contains("Class Name") })
        assertTrue(findings.any { it.title.contains("Function Name") })
        assertTrue(findings.any { it.title.contains("Constant") })
        assertTrue(findings.any { it.title.contains("Private Mutable State") })
    }
    
    @Test
    fun `finding IDs are unique`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/meal_screen.kt",
            relativePath = "ui/meal/meal_screen.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import kotlinx.coroutines.flow.MutableStateFlow
            
            class meal_screen {
                private val uiState = MutableStateFlow<String>("")
                
                fun Load_Data() {
                }
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
            
            class meal_item {
                fun Get_Name(): String {
                    return ""
                }
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
    fun `handles complex class declarations`() = runTest {
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
                val name: String
            ) {
                companion object {
                    const val MAX_NAME_LENGTH = 100
                }
            }
            
            sealed class MealResult {
                data class Success(val meal: Meal) : MealResult()
                data class Error(val message: String) : MealResult()
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should not have any violations
        assertFalse(findings.any { it.title.contains("Class Name") })
        assertFalse(findings.any { it.title.contains("Constant") })
    }
    
    @Test
    fun `handles multiline function declarations`() = runTest {
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
                suspend fun getMeals(
                    query: String,
                    limit: Int
                ): List<Meal> {
                    return emptyList()
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should not have function naming violations
        assertFalse(findings.any { it.title.contains("Function Name") })
    }
    
    @Test
    fun `all findings have LOW priority`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/meal_screen.kt",
            relativePath = "ui/meal/meal_screen.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            class meal_screen {
                fun Load_Data() {
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.isNotEmpty())
        findings.forEach { finding ->
            assertEquals(Priority.LOW, finding.priority)
        }
    }
}
