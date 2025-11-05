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
 * Unit tests for ArchitectureAnalyzer.
 * Tests validation of Clean Architecture principles including:
 * - Domain layer import validation
 * - ViewModel StateFlow validation
 * - Use case structure validation
 */
class ArchitectureAnalyzerTest {
    
    private lateinit var analyzer: ArchitectureAnalyzer
    
    @Before
    fun setup() {
        analyzer = ArchitectureAnalyzer()
    }
    
    @Test
    fun `analyzer has correct properties`() {
        assertEquals("architecture", analyzer.id)
        assertEquals("Architecture Analyzer", analyzer.name)
        assertEquals(AnalysisCategory.ARCHITECTURE, analyzer.category)
    }
    
    @Test
    fun `appliesTo returns true for files with known layers`() {
        val domainFile = FileInfo(
            path = "/domain/Model.kt",
            relativePath = "domain/Model.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        assertTrue(analyzer.appliesTo(domainFile))
    }
    
    @Test
    fun `appliesTo returns false for files with null layer`() {
        val unknownFile = FileInfo(
            path = "/unknown/File.kt",
            relativePath = "unknown/File.kt",
            size = 100L,
            lastModified = 0L,
            layer = null
        )
        
        assertFalse(analyzer.appliesTo(unknownFile))
    }
    
    // Domain Layer Import Validation Tests
    
    @Test
    fun `detects Android import in domain layer`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/GetMealsUseCase.kt",
            relativePath = "domain/usecase/GetMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            import android.content.Context
            import com.shoppit.app.domain.model.Meal
            
            class GetMealsUseCase(private val context: Context) {
                operator fun invoke() = emptyList<Meal>()
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(1, findings.size)
        val finding = findings[0]
        assertEquals("architecture", finding.analyzer)
        assertEquals(AnalysisCategory.ARCHITECTURE, finding.category)
        assertEquals(Priority.HIGH, finding.priority)
        assertTrue(finding.title.contains("Android Framework Import"))
        assertTrue(finding.description.contains("android.content.Context"))
        assertTrue(finding.description.contains("Domain layer"))
    }
    
    @Test
    fun `detects multiple Android imports in domain layer`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.model
            
            import android.os.Parcelable
            import android.content.Context
            import kotlinx.parcelize.Parcelize
            
            data class Meal(val name: String)
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(2, findings.size)
        assertTrue(findings.all { it.title.contains("Android Framework Import") })
        assertTrue(findings.any { it.description.contains("android.os.Parcelable") })
        assertTrue(findings.any { it.description.contains("android.content.Context") })
    }
    
    @Test
    fun `does not flag non-Android imports in domain layer`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/GetMealsUseCase.kt",
            relativePath = "domain/usecase/GetMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            import com.shoppit.app.domain.model.Meal
            import com.shoppit.app.domain.repository.MealRepository
            import kotlinx.coroutines.flow.Flow
            import javax.inject.Inject
            
            class GetMealsUseCase @Inject constructor(
                private val repository: MealRepository
            ) {
                operator fun invoke(): Flow<List<Meal>> = repository.getMeals()
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    // ViewModel StateFlow Validation Tests
    
    @Test
    fun `detects exposed MutableStateFlow in ViewModel`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealViewModel.kt",
            relativePath = "ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.lifecycle.ViewModel
            import kotlinx.coroutines.flow.MutableStateFlow
            
            class MealViewModel : ViewModel() {
                val uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(1, findings.size)
        val finding = findings[0]
        assertEquals("architecture", finding.analyzer)
        assertEquals(Priority.HIGH, finding.priority)
        assertTrue(finding.title.contains("Exposed MutableStateFlow"))
        assertTrue(finding.description.contains("uiState"))
        assertTrue(finding.recommendation.contains("asStateFlow()"))
    }
    
    @Test
    fun `does not flag private MutableStateFlow in ViewModel`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealViewModel.kt",
            relativePath = "ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.lifecycle.ViewModel
            import kotlinx.coroutines.flow.MutableStateFlow
            import kotlinx.coroutines.flow.StateFlow
            import kotlinx.coroutines.flow.asStateFlow
            
            class MealViewModel : ViewModel() {
                private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
                val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `detects multiple exposed MutableStateFlows in ViewModel`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealViewModel.kt",
            relativePath = "ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.lifecycle.ViewModel
            import kotlinx.coroutines.flow.MutableStateFlow
            
            class MealViewModel : ViewModel() {
                val uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
                val errorState = MutableStateFlow<String?>(null)
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(2, findings.size)
        assertTrue(findings.all { it.title.contains("Exposed MutableStateFlow") })
    }
    
    @Test
    fun `does not analyze non-ViewModel files in UI layer`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealScreen.kt",
            relativePath = "ui/meal/MealScreen.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            import kotlinx.coroutines.flow.MutableStateFlow
            
            @Composable
            fun MealScreen() {
                val state = MutableStateFlow<String>("test")
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    // Use Case Structure Validation Tests
    
    @Test
    fun `detects use case with multiple public functions`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/MealUseCase.kt",
            relativePath = "domain/usecase/MealUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            import com.shoppit.app.domain.model.Meal
            import javax.inject.Inject
            
            class MealUseCase @Inject constructor() {
                fun getMeals(): List<Meal> = emptyList()
                fun getMealById(id: Long): Meal? = null
                fun addMeal(meal: Meal): Long = 0L
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(1, findings.size)
        val finding = findings[0]
        assertEquals("architecture", finding.analyzer)
        assertEquals(Priority.HIGH, finding.priority)
        assertTrue(finding.title.contains("Multiple Public Functions"))
        assertTrue(finding.description.contains("3 public functions"))
        assertTrue(finding.recommendation.contains("single public operator function"))
    }
    
    @Test
    fun `detects use case with non-operator public function`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/GetMealsUseCase.kt",
            relativePath = "domain/usecase/GetMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            import com.shoppit.app.domain.model.Meal
            import javax.inject.Inject
            
            class GetMealsUseCase @Inject constructor() {
                fun execute(): List<Meal> = emptyList()
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(1, findings.size)
        val finding = findings[0]
        assertEquals("architecture", finding.analyzer)
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.title.contains("Missing Operator Function"))
        assertTrue(finding.recommendation.contains("operator fun invoke()"))
    }
    
    @Test
    fun `does not flag use case with operator invoke function`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/GetMealsUseCase.kt",
            relativePath = "domain/usecase/GetMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            import com.shoppit.app.domain.model.Meal
            import javax.inject.Inject
            
            class GetMealsUseCase @Inject constructor() {
                operator fun invoke(): List<Meal> = emptyList()
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `does not flag use case with suspend operator invoke function`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/AddMealUseCase.kt",
            relativePath = "domain/usecase/AddMealUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            import com.shoppit.app.domain.model.Meal
            import javax.inject.Inject
            
            class AddMealUseCase @Inject constructor() {
                suspend operator fun invoke(meal: Meal): Long = 0L
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `ignores private functions in use case`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/GetMealsUseCase.kt",
            relativePath = "domain/usecase/GetMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            import com.shoppit.app.domain.model.Meal
            import javax.inject.Inject
            
            class GetMealsUseCase @Inject constructor() {
                operator fun invoke(): List<Meal> = filterMeals(emptyList())
                
                private fun filterMeals(meals: List<Meal>): List<Meal> = meals
                private fun sortMeals(meals: List<Meal>): List<Meal> = meals
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    // General Tests
    
    @Test
    fun `handles empty file gracefully`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/Model.kt",
            relativePath = "domain/Model.kt",
            size = 0L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = ""
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `handles file with only package declaration`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/Model.kt",
            relativePath = "domain/Model.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = "package com.shoppit.app.domain.model"
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `does not analyze data layer files`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import android.content.Context
            
            class MealRepositoryImpl(private val context: Context)
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Data layer is allowed to have Android imports
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `finding IDs are unique`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/MealUseCase.kt",
            relativePath = "domain/usecase/MealUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            import android.content.Context
            import android.os.Bundle
            
            class MealUseCase(private val context: Context) {
                fun execute1() {}
                fun execute2() {}
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should have 2 Android imports + 1 multiple functions = 3 findings
        assertEquals(3, findings.size)
        
        // All IDs should be unique
        val ids = findings.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
    
    @Test
    fun `findings include helpful recommendations and examples`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/GetMealsUseCase.kt",
            relativePath = "domain/usecase/GetMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            import android.content.Context
            
            class GetMealsUseCase(private val context: Context) {
                fun execute() {}
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
}
