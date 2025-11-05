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
 * Unit tests for DependencyInjectionAnalyzer.
 * Tests validation of Hilt dependency injection patterns including:
 * - ViewModel @HiltViewModel annotation
 * - Constructor @Inject annotation
 * - Module @Module and @InstallIn annotations
 * - @Binds usage for interface binding
 */
class DependencyInjectionAnalyzerTest {
    
    private lateinit var analyzer: DependencyInjectionAnalyzer
    
    @Before
    fun setup() {
        analyzer = DependencyInjectionAnalyzer()
    }
    
    @Test
    fun `analyzer has correct properties`() {
        assertEquals("dependency-injection", analyzer.id)
        assertEquals("Dependency Injection Analyzer", analyzer.name)
        assertEquals(AnalysisCategory.DEPENDENCY_INJECTION, analyzer.category)
    }
    
    @Test
    fun `appliesTo returns true for all Kotlin files`() {
        val file = FileInfo(
            path = "/ui/MealViewModel.kt",
            relativePath = "ui/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        assertTrue(analyzer.appliesTo(file))
    }
    
    // ViewModel @HiltViewModel Annotation Tests
    
    @Test
    fun `detects ViewModel missing HiltViewModel annotation`() = runTest {
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
            import javax.inject.Inject
            
            class MealViewModel @Inject constructor(
                private val getMealsUseCase: GetMealsUseCase
            ) : ViewModel() {
                // ...
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(1, findings.size)
        val finding = findings[0]
        assertEquals("dependency-injection", finding.analyzer)
        assertEquals(AnalysisCategory.DEPENDENCY_INJECTION, finding.category)
        assertEquals(Priority.HIGH, finding.priority)
        assertTrue(finding.title.contains("Missing @HiltViewModel"))
        assertTrue(finding.description.contains("MealViewModel"))
        assertTrue(finding.recommendation.contains("@HiltViewModel"))
    }
    
    @Test
    fun `does not flag ViewModel with HiltViewModel annotation`() = runTest {
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
            import dagger.hilt.android.lifecycle.HiltViewModel
            import javax.inject.Inject
            
            @HiltViewModel
            class MealViewModel @Inject constructor(
                private val getMealsUseCase: GetMealsUseCase
            ) : ViewModel() {
                // ...
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should not have HiltViewModel finding (might have other findings)
        assertFalse(findings.any { it.title.contains("Missing @HiltViewModel") })
    }
    
    @Test
    fun `detects ViewModel with parentheses in extends`() = runTest {
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
            
            class MealViewModel : ViewModel() {
                // ...
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing @HiltViewModel") })
    }
    
    @Test
    fun `does not analyze non-ViewModel files`() = runTest {
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
            
            @Composable
            fun MealScreen() {
                // ...
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("@HiltViewModel") })
    }
    
    // Constructor @Inject Annotation Tests
    
    @Test
    fun `detects constructor missing Inject annotation`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import com.shoppit.app.domain.repository.MealRepository
            
            class MealRepositoryImpl constructor(
                private val mealDao: MealDao,
                private val mealApi: MealApi
            ) : MealRepository {
                // ...
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing @Inject") })
        val finding = findings.first { it.title.contains("Missing @Inject") }
        assertEquals(Priority.HIGH, finding.priority)
        assertTrue(finding.description.contains("MealRepositoryImpl"))
        assertTrue(finding.recommendation.contains("@Inject"))
    }
    
    @Test
    fun `does not flag constructor with Inject annotation`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import com.shoppit.app.domain.repository.MealRepository
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor(
                private val mealDao: MealDao,
                private val mealApi: MealApi
            ) : MealRepository {
                // ...
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing @Inject") })
    }
    
    @Test
    fun `does not flag data class constructors`() = runTest {
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
                val ingredients: List<Ingredient>
            )
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing @Inject") })
    }
    
    @Test
    fun `does not flag test file constructors`() = runTest {
        val fileInfo = FileInfo(
            path = "/test/MealViewModelTest.kt",
            relativePath = "test/MealViewModelTest.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.TEST
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            class MealViewModelTest constructor(
                private val testDispatcher: TestDispatcher
            ) {
                // ...
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing @Inject") })
    }
    
    @Test
    fun `does not flag constructor without parameters`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/GetMealsUseCase.kt",
            relativePath = "domain/usecase/GetMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            class GetMealsUseCase constructor() {
                operator fun invoke() = emptyList<Meal>()
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing @Inject") })
    }
    
    // Hilt Module Annotation Tests
    
    @Test
    fun `detects module missing Module annotation`() = runTest {
        val fileInfo = FileInfo(
            path = "/di/DatabaseModule.kt",
            relativePath = "di/DatabaseModule.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DI
        )
        
        val code = """
            package com.shoppit.app.di
            
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            
            @InstallIn(SingletonComponent::class)
            object DatabaseModule {
                // ...
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing @Module") })
        val finding = findings.first { it.title.contains("Missing @Module") }
        assertEquals(Priority.HIGH, finding.priority)
        assertTrue(finding.description.contains("DatabaseModule"))
    }
    
    @Test
    fun `detects module missing InstallIn annotation`() = runTest {
        val fileInfo = FileInfo(
            path = "/di/DatabaseModule.kt",
            relativePath = "di/DatabaseModule.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DI
        )
        
        val code = """
            package com.shoppit.app.di
            
            import dagger.Module
            
            @Module
            object DatabaseModule {
                // ...
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing @InstallIn") })
        val finding = findings.first { it.title.contains("Missing @InstallIn") }
        assertEquals(Priority.HIGH, finding.priority)
        assertTrue(finding.description.contains("DatabaseModule"))
        assertTrue(finding.recommendation.contains("SingletonComponent"))
    }
    
    @Test
    fun `does not flag module with both annotations`() = runTest {
        val fileInfo = FileInfo(
            path = "/di/DatabaseModule.kt",
            relativePath = "di/DatabaseModule.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DI
        )
        
        val code = """
            package com.shoppit.app.di
            
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            
            @Module
            @InstallIn(SingletonComponent::class)
            object DatabaseModule {
                // ...
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing @Module") })
        assertFalse(findings.any { it.title.contains("Missing @InstallIn") })
    }
    
    @Test
    fun `detects module class with missing annotations`() = runTest {
        val fileInfo = FileInfo(
            path = "/di/RepositoryModule.kt",
            relativePath = "di/RepositoryModule.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DI
        )
        
        val code = """
            package com.shoppit.app.di
            
            abstract class RepositoryModule {
                // ...
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing @Module") })
    }
    
    // @Binds Usage Tests
    
    @Test
    fun `suggests Binds for simple Provides function`() = runTest {
        val fileInfo = FileInfo(
            path = "/di/RepositoryModule.kt",
            relativePath = "di/RepositoryModule.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DI
        )
        
        val code = """
            package com.shoppit.app.di
            
            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            
            @Module
            @InstallIn(SingletonComponent::class)
            abstract class RepositoryModule {
                @Provides
                fun provideMealRepository(impl: MealRepositoryImpl): MealRepository {
                    return impl
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Consider Using @Binds") })
        val finding = findings.first { it.title.contains("Consider Using @Binds") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.recommendation.contains("@Binds"))
    }
    
    @Test
    fun `does not suggest Binds for module with Binds functions`() = runTest {
        val fileInfo = FileInfo(
            path = "/di/RepositoryModule.kt",
            relativePath = "di/RepositoryModule.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DI
        )
        
        val code = """
            package com.shoppit.app.di
            
            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            
            @Module
            @InstallIn(SingletonComponent::class)
            abstract class RepositoryModule {
                @Binds
                abstract fun bindMealRepository(impl: MealRepositoryImpl): MealRepository
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Consider Using @Binds") })
    }
    
    // General Tests
    
    @Test
    fun `handles empty file gracefully`() = runTest {
        val fileInfo = FileInfo(
            path = "/di/Module.kt",
            relativePath = "di/Module.kt",
            size = 0L,
            lastModified = 0L,
            layer = CodeLayer.DI
        )
        
        val code = ""
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `handles file with only package declaration`() = runTest {
        val fileInfo = FileInfo(
            path = "/di/Module.kt",
            relativePath = "di/Module.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DI
        )
        
        val code = "package com.shoppit.app.di"
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `finding IDs are unique`() = runTest {
        val fileInfo = FileInfo(
            path = "/di/AppModule.kt",
            relativePath = "di/AppModule.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DI
        )
        
        val code = """
            package com.shoppit.app.di
            
            object AppModule {
                fun provideDatabase(): Database = TODO()
                fun provideApi(): Api = TODO()
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should have findings for missing @Module and @InstallIn
        assertTrue(findings.size >= 2)
        
        // All IDs should be unique
        val ids = findings.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
    
    @Test
    fun `findings include helpful recommendations and examples`() = runTest {
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
            
            class MealViewModel : ViewModel() {
                // ...
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
    fun `detects multiple issues in same file`() = runTest {
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
            
            class MealViewModel constructor(
                private val useCase: GetMealsUseCase
            ) : ViewModel() {
                // ...
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should have both missing @HiltViewModel and missing @Inject
        assertEquals(2, findings.size)
        assertTrue(findings.any { it.title.contains("Missing @HiltViewModel") })
        assertTrue(findings.any { it.title.contains("Missing @Inject") })
    }
    
    @Test
    fun `handles multiline constructor`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            class MealRepositoryImpl constructor(
                private val mealDao: MealDao,
                private val mealApi: MealApi,
                private val dispatcher: CoroutineDispatcher
            ) : MealRepository {
                // ...
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing @Inject") })
    }
    
    @Test
    fun `handles abstract module class`() = runTest {
        val fileInfo = FileInfo(
            path = "/di/RepositoryModule.kt",
            relativePath = "di/RepositoryModule.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DI
        )
        
        val code = """
            package com.shoppit.app.di
            
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            
            @Module
            @InstallIn(SingletonComponent::class)
            abstract class RepositoryModule {
                // ...
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should not have missing annotation findings
        assertFalse(findings.any { it.title.contains("Missing @Module") })
        assertFalse(findings.any { it.title.contains("Missing @InstallIn") })
    }
}
