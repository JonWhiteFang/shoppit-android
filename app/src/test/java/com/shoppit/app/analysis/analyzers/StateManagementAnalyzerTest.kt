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
 * Unit tests for StateManagementAnalyzer.
 * Tests validation of state management patterns including:
 * - State exposure validation
 * - State update pattern validation
 * - Flow dispatcher validation
 * - ViewModel scope validation
 */
class StateManagementAnalyzerTest {
    
    private lateinit var analyzer: StateManagementAnalyzer
    
    @Before
    fun setup() {
        analyzer = StateManagementAnalyzer()
    }
    
    @Test
    fun `analyzer has correct properties`() {
        assertEquals("state-management", analyzer.id)
        assertEquals("State Management Analyzer", analyzer.name)
        assertEquals(AnalysisCategory.STATE_MANAGEMENT, analyzer.category)
    }
    
    @Test
    fun `appliesTo returns true for ViewModel files`() {
        val viewModelFile = FileInfo(
            path = "/ui/meal/MealViewModel.kt",
            relativePath = "ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        assertTrue(analyzer.appliesTo(viewModelFile))
    }
    
    @Test
    fun `appliesTo returns true for Repository files`() {
        val repositoryFile = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        assertTrue(analyzer.appliesTo(repositoryFile))
    }
    
    @Test
    fun `appliesTo returns false for non-ViewModel non-Repository files`() {
        val domainFile = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        assertFalse(analyzer.appliesTo(domainFile))
    }
    
    // State Exposure Validation Tests (Requirement 5.1)
    
    @Test
    fun `detects public MutableStateFlow in ViewModel`() = runTest {
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
            import dagger.hilt.android.lifecycle.HiltViewModel
            import javax.inject.Inject
            
            @HiltViewModel
            class MealViewModel @Inject constructor() : ViewModel() {
                val uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
                
                fun loadMeals() {
                    uiState.value = MealUiState.Success(emptyList())
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Public MutableStateFlow Exposed") })
        val finding = findings.first { it.title.contains("Public MutableStateFlow Exposed") }
        assertEquals("state-management", finding.analyzer)
        assertEquals(Priority.HIGH, finding.priority)
        assertTrue(finding.description.contains("uiState"))
        assertTrue(finding.recommendation.contains("private"))
        assertTrue(finding.recommendation.contains("asStateFlow()"))
    }
    
    @Test
    fun `does not flag private MutableStateFlow`() = runTest {
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
        
        assertFalse(findings.any { it.title.contains("Public MutableStateFlow Exposed") })
    }
    
    @Test
    fun `detects multiple exposed MutableStateFlow properties`() = runTest {
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
                val searchQuery = MutableStateFlow("")
                val isLoading = MutableStateFlow(false)
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        val exposedStateFindings = findings.filter { it.title.contains("Public MutableStateFlow Exposed") }
        assertEquals(3, exposedStateFindings.size)
    }
    
    // State Update Pattern Validation Tests (Requirement 5.2)
    
    @Test
    fun `detects direct state mutation instead of update`() = runTest {
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
                private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
                
                fun loadMeals() {
                    _uiState.value = MealUiState.Loading
                    // Load meals...
                    _uiState.value = MealUiState.Success(emptyList())
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Direct State Mutation Instead of update") })
        val finding = findings.first { it.title.contains("Direct State Mutation Instead of update") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("_uiState"))
        assertTrue(finding.recommendation.contains(".update { }"))
    }
    
    @Test
    fun `does not flag state updates using update pattern`() = runTest {
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
            import kotlinx.coroutines.flow.update
            
            class MealViewModel : ViewModel() {
                private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
                
                fun loadMeals() {
                    _uiState.update { MealUiState.Loading }
                    // Load meals...
                    _uiState.update { MealUiState.Success(emptyList()) }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Direct State Mutation Instead of update") })
    }
    
    @Test
    fun `detects multiple direct state mutations`() = runTest {
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
                private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
                
                fun loadMeals() {
                    _uiState.value = MealUiState.Loading
                    _uiState.value = MealUiState.Success(emptyList())
                }
                
                fun refresh() {
                    _uiState.value = MealUiState.Loading
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        val mutationFindings = findings.filter { it.title.contains("Direct State Mutation Instead of update") }
        assertEquals(3, mutationFindings.size)
    }
    
    // Flow Dispatcher Validation Tests (Requirement 5.4)
    
    @Test
    fun `detects missing flowOn for database operation`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import kotlinx.coroutines.flow.Flow
            import kotlinx.coroutines.flow.flow
            
            class MealRepositoryImpl : MealRepository {
                override fun getMeals(): Flow<Result<List<Meal>>> = flow {
                    mealDao.getAllMeals()
                        .collect { meals ->
                            emit(Result.success(meals))
                        }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing flowOn(Dispatchers.IO) for Database Operation") })
        val finding = findings.first { it.title.contains("Missing flowOn(Dispatchers.IO) for Database Operation") }
        assertEquals(Priority.HIGH, finding.priority)
        assertTrue(finding.description.contains("getMeals"))
        assertTrue(finding.recommendation.contains("flowOn(Dispatchers.IO)"))
    }
    
    @Test
    fun `does not flag Flow with flowOn`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import kotlinx.coroutines.flow.Flow
            import kotlinx.coroutines.flow.flow
            import kotlinx.coroutines.flow.flowOn
            import kotlinx.coroutines.Dispatchers
            
            class MealRepositoryImpl : MealRepository {
                override fun getMeals(): Flow<Result<List<Meal>>> = flow {
                    mealDao.getAllMeals()
                        .collect { meals ->
                            emit(Result.success(meals))
                        }
                }.flowOn(Dispatchers.IO)
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing flowOn(Dispatchers.IO) for Database Operation") })
    }
    
    @Test
    fun `does not flag Flow without database operations`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import kotlinx.coroutines.flow.Flow
            import kotlinx.coroutines.flow.flow
            
            class MealRepositoryImpl : MealRepository {
                override fun getLocalMeals(): Flow<List<Meal>> = flow {
                    emit(emptyList())
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing flowOn(Dispatchers.IO) for Database Operation") })
    }
    
    // ViewModel Scope Validation Tests (Requirement 5.5)
    
    @Test
    fun `detects coroutine launch not using viewModelScope`() = runTest {
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
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.launch
            
            class MealViewModel : ViewModel() {
                fun loadMeals() {
                    GlobalScope.launch {
                        // Load meals
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Coroutine Launch Not Using viewModelScope") })
        val finding = findings.first { it.title.contains("Coroutine Launch Not Using viewModelScope") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("viewModelScope"))
        assertTrue(finding.recommendation.contains("viewModelScope.launch"))
    }
    
    @Test
    fun `does not flag viewModelScope launch`() = runTest {
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
            import androidx.lifecycle.viewModelScope
            import kotlinx.coroutines.launch
            
            class MealViewModel : ViewModel() {
                fun loadMeals() {
                    viewModelScope.launch {
                        // Load meals
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Coroutine Launch Not Using viewModelScope") })
    }
    
    // Multiple Issues Tests
    
    @Test
    fun `detects multiple state management issues in same file`() = runTest {
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
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.launch
            
            class MealViewModel : ViewModel() {
                val uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
                
                fun loadMeals() {
                    GlobalScope.launch {
                        uiState.value = MealUiState.Loading
                        uiState.value = MealUiState.Success(emptyList())
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should detect: exposed MutableStateFlow, direct mutations, wrong scope
        assertTrue(findings.size >= 3)
        assertTrue(findings.any { it.title.contains("Public MutableStateFlow Exposed") })
        assertTrue(findings.any { it.title.contains("Direct State Mutation Instead of update") })
        assertTrue(findings.any { it.title.contains("Coroutine Launch Not Using viewModelScope") })
    }
    
    @Test
    fun `handles well-structured ViewModel without issues`() = runTest {
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
            import androidx.lifecycle.viewModelScope
            import kotlinx.coroutines.flow.MutableStateFlow
            import kotlinx.coroutines.flow.StateFlow
            import kotlinx.coroutines.flow.asStateFlow
            import kotlinx.coroutines.flow.update
            import kotlinx.coroutines.launch
            import dagger.hilt.android.lifecycle.HiltViewModel
            import javax.inject.Inject
            
            @HiltViewModel
            class MealViewModel @Inject constructor(
                private val getMealsUseCase: GetMealsUseCase
            ) : ViewModel() {
                
                private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
                val uiState: StateFlow<MealUiState> = _uiState.asStateFlow()
                
                fun loadMeals() {
                    viewModelScope.launch {
                        _uiState.update { MealUiState.Loading }
                        
                        getMealsUseCase()
                            .collect { result ->
                                _uiState.update {
                                    result.fold(
                                        onSuccess = { MealUiState.Success(it) },
                                        onFailure = { MealUiState.Error(it.message ?: "Unknown error") }
                                    )
                                }
                            }
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should have no findings - this is a well-structured ViewModel
        assertEquals(0, findings.size)
    }
    
    // Edge Cases
    
    @Test
    fun `handles empty file gracefully`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/Empty.kt",
            relativePath = "ui/Empty.kt",
            size = 0L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = ""
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `handles non-ViewModel UI file`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealUiState.kt",
            relativePath = "ui/meal/MealUiState.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            sealed interface MealUiState {
                data object Loading : MealUiState
                data class Success(val meals: List<Meal>) : MealUiState
                data class Error(val message: String) : MealUiState
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `finding IDs are unique`() = runTest {
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
            import kotlinx.coroutines.GlobalScope
            import kotlinx.coroutines.launch
            
            class MealViewModel : ViewModel() {
                val uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
                val searchQuery = MutableStateFlow("")
                
                fun loadMeals() {
                    GlobalScope.launch {
                        uiState.value = MealUiState.Loading
                        searchQuery.value = ""
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.isNotEmpty())
        
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
            import kotlinx.coroutines.flow.MutableStateFlow
            
            class MealViewModel : ViewModel() {
                val uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
                
                fun loadMeals() {
                    uiState.value = MealUiState.Success(emptyList())
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
    fun `handles Repository with proper Flow dispatcher`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import kotlinx.coroutines.flow.Flow
            import kotlinx.coroutines.flow.flow
            import kotlinx.coroutines.flow.flowOn
            import kotlinx.coroutines.flow.catch
            import kotlinx.coroutines.Dispatchers
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor(
                private val mealDao: MealDao
            ) : MealRepository {
                
                override fun getMeals(): Flow<Result<List<Meal>>> = flow {
                    mealDao.getAllMeals()
                        .map { entities -> entities.map { it.toMeal() } }
                        .collect { meals ->
                            emit(Result.success(meals))
                        }
                }.catch { e ->
                    emit(Result.failure(mapException(e)))
                }.flowOn(Dispatchers.IO)
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should have no findings - this is properly structured
        assertEquals(0, findings.size)
    }
}
