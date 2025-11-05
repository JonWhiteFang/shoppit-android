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
 * Unit tests for ErrorHandlingAnalyzer.
 * Tests validation of error handling patterns including:
 * - Exception mapping in repositories
 * - Result type usage
 * - Empty catch block detection
 */
class ErrorHandlingAnalyzerTest {
    
    private lateinit var analyzer: ErrorHandlingAnalyzer
    
    @Before
    fun setup() {
        analyzer = ErrorHandlingAnalyzer()
    }
    
    @Test
    fun `analyzer has correct properties`() {
        assertEquals("error-handling", analyzer.id)
        assertEquals("Error Handling Analyzer", analyzer.name)
        assertEquals(AnalysisCategory.ERROR_HANDLING, analyzer.category)
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
    fun `appliesTo returns true for UseCase files`() {
        val useCaseFile = FileInfo(
            path = "/domain/usecase/AddMealUseCase.kt",
            relativePath = "domain/usecase/AddMealUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        assertTrue(analyzer.appliesTo(useCaseFile))
    }
    
    @Test
    fun `appliesTo returns true for UI layer files`() {
        val uiFile = FileInfo(
            path = "/ui/meal/MealViewModel.kt",
            relativePath = "ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        assertTrue(analyzer.appliesTo(uiFile))
    }
    
    @Test
    fun `appliesTo returns false for non-applicable files`() {
        val modelFile = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        assertFalse(analyzer.appliesTo(modelFile))
    }
    
    // Exception Mapping Validation Tests (Requirement 6.1)
    
    @Test
    fun `detects missing exception mapping in repository`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.withContext
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor(
                private val mealDao: MealDao
            ) : MealRepository {
                
                override suspend fun addMeal(meal: Meal): Result<Long> = withContext(Dispatchers.IO) {
                    val id = mealDao.insertMeal(meal.toEntity())
                    Result.success(id)
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing Exception Mapping in Repository") })
        val finding = findings.first { it.title.contains("Missing Exception Mapping in Repository") }
        assertEquals("error-handling", finding.analyzer)
        assertEquals(Priority.HIGH, finding.priority)
        assertTrue(finding.description.contains("addMeal"))
        assertTrue(finding.recommendation.contains("try-catch"))
        assertTrue(finding.recommendation.contains("mapException"))
    }
    
    @Test
    fun `does not flag repository with proper exception mapping`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.withContext
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor(
                private val mealDao: MealDao
            ) : MealRepository {
                
                override suspend fun addMeal(meal: Meal): Result<Long> = withContext(Dispatchers.IO) {
                    try {
                        val id = mealDao.insertMeal(meal.toEntity())
                        Result.success(id)
                    } catch (e: Exception) {
                        Result.failure(mapException(e))
                    }
                }
                
                private fun mapException(e: Exception): AppError {
                    return AppError.DatabaseError("Database error", e)
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing Exception Mapping in Repository") })
    }
    
    @Test
    fun `detects missing exception mapping with network operations`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.withContext
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor(
                private val mealApi: MealApi
            ) : MealRepository {
                
                override suspend fun syncMeals(): Result<Unit> = withContext(Dispatchers.IO) {
                    val remoteMeals = mealApi.getMeals()
                    Result.success(Unit)
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing Exception Mapping in Repository") })
    }
    
    // Result Type Validation Tests (Requirement 6.2)
    
    @Test
    fun `detects missing Result type for failable operation`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/AddMealUseCase.kt",
            relativePath = "domain/usecase/AddMealUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            import javax.inject.Inject
            
            class AddMealUseCase @Inject constructor(
                private val repository: MealRepository
            ) {
                suspend operator fun invoke(meal: Meal): Long {
                    require(meal.name.isNotBlank())
                    return repository.addMeal(meal)
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing Result Type for Failable Operation") })
        val finding = findings.first { it.title.contains("Missing Result Type for Failable Operation") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("invoke"))
        assertTrue(finding.recommendation.contains("Result<T>"))
    }
    
    @Test
    fun `does not flag function with Result return type`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/AddMealUseCase.kt",
            relativePath = "domain/usecase/AddMealUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            import javax.inject.Inject
            
            class AddMealUseCase @Inject constructor(
                private val repository: MealRepository
            ) {
                suspend operator fun invoke(meal: Meal): Result<Long> {
                    require(meal.name.isNotBlank())
                    return repository.addMeal(meal)
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing Result Type for Failable Operation") })
    }
    
    @Test
    fun `detects missing Result type in repository with throw statement`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor(
                private val mealDao: MealDao
            ) : MealRepository {
                
                override suspend fun getMealById(id: Long): Meal {
                    val entity = mealDao.getMealById(id)
                    if (entity == null) {
                        throw IllegalArgumentException("Meal not found")
                    }
                    return entity.toMeal()
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing Result Type for Failable Operation") })
    }
    
    // Exception in UI Layer Tests (Requirement 6.3)
    
    @Test
    fun `detects exception thrown in UI layer`() = runTest {
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
                        if (meals.isEmpty()) {
                            throw IllegalStateException("No meals found")
                        }
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Exception Thrown in UI Layer") })
        val finding = findings.first { it.title.contains("Exception Thrown in UI Layer") }
        assertEquals(Priority.HIGH, finding.priority)
        assertTrue(finding.description.contains("UI layer"))
        assertTrue(finding.recommendation.contains("Result types"))
    }
    
    @Test
    fun `does not flag UI layer without throw statements`() = runTest {
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
            import kotlinx.coroutines.flow.MutableStateFlow
            import kotlinx.coroutines.flow.update
            
            class MealViewModel : ViewModel() {
                private val _uiState = MutableStateFlow<MealUiState>(MealUiState.Loading)
                
                fun loadMeals() {
                    viewModelScope.launch {
                        getMealsUseCase().collect { result ->
                            result.fold(
                                onSuccess = { meals ->
                                    _uiState.update { MealUiState.Success(meals) }
                                },
                                onFailure = { error ->
                                    _uiState.update { MealUiState.Error(error.message ?: "Unknown error") }
                                }
                            )
                        }
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Exception Thrown in UI Layer") })
    }
    
    // Empty Catch Block Tests (Requirement 6.4)
    
    @Test
    fun `detects empty catch block`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor(
                private val mealDao: MealDao
            ) : MealRepository {
                
                override suspend fun addMeal(meal: Meal): Result<Long> {
                    try {
                        val id = mealDao.insertMeal(meal.toEntity())
                        return Result.success(id)
                    } catch (e: SQLiteException) {
                    }
                    return Result.failure(AppError.UnknownError("Failed"))
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Empty or Logging-Only Catch Block") })
        val finding = findings.first { it.title.contains("Empty or Logging-Only Catch Block") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("SQLiteException"))
        assertTrue(finding.recommendation.contains("Handle the exception"))
    }
    
    @Test
    fun `detects catch block with only logging`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import timber.log.Timber
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor(
                private val mealDao: MealDao
            ) : MealRepository {
                
                override suspend fun addMeal(meal: Meal): Result<Long> {
                    try {
                        val id = mealDao.insertMeal(meal.toEntity())
                        return Result.success(id)
                    } catch (e: SQLiteException) {
                        Timber.e(e, "Error inserting meal")
                    }
                    return Result.failure(AppError.UnknownError("Failed"))
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Empty or Logging-Only Catch Block") })
    }
    
    @Test
    fun `does not flag catch block with proper error handling`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import timber.log.Timber
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor(
                private val mealDao: MealDao
            ) : MealRepository {
                
                override suspend fun addMeal(meal: Meal): Result<Long> {
                    try {
                        val id = mealDao.insertMeal(meal.toEntity())
                        return Result.success(id)
                    } catch (e: SQLiteException) {
                        Timber.e(e, "Error inserting meal")
                        return Result.failure(AppError.DatabaseError("Failed to insert meal", e))
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Empty or Logging-Only Catch Block") })
    }
    
    @Test
    fun `detects generic Exception catch`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor(
                private val mealDao: MealDao
            ) : MealRepository {
                
                override suspend fun addMeal(meal: Meal): Result<Long> {
                    try {
                        val id = mealDao.insertMeal(meal.toEntity())
                        return Result.success(id)
                    } catch (e: Exception) {
                        return Result.failure(mapException(e))
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Generic Exception Catch") })
        val finding = findings.first { it.title.contains("Generic Exception Catch") }
        assertEquals(Priority.LOW, finding.priority)
        assertTrue(finding.description.contains("generic"))
        assertTrue(finding.recommendation.contains("specific exception types"))
    }
    
    @Test
    fun `does not flag specific exception catch`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor(
                private val mealDao: MealDao
            ) : MealRepository {
                
                override suspend fun addMeal(meal: Meal): Result<Long> {
                    try {
                        val id = mealDao.insertMeal(meal.toEntity())
                        return Result.success(id)
                    } catch (e: SQLiteException) {
                        return Result.failure(AppError.DatabaseError("Database error", e))
                    } catch (e: IOException) {
                        return Result.failure(AppError.NetworkError("Network error", e))
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Generic Exception Catch") })
    }
    
    // Multiple Issues Tests
    
    @Test
    fun `detects multiple error handling issues in same file`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor(
                private val mealDao: MealDao
            ) : MealRepository {
                
                override suspend fun addMeal(meal: Meal): Result<Long> {
                    val id = mealDao.insertMeal(meal.toEntity())
                    return Result.success(id)
                }
                
                override suspend fun deleteMeal(id: Long): Long {
                    try {
                        mealDao.deleteMealById(id)
                        return id
                    } catch (e: Exception) {
                    }
                    return -1
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should detect: missing exception mapping, missing Result type, empty catch, generic catch
        assertTrue(findings.size >= 3)
        assertTrue(findings.any { it.title.contains("Missing Exception Mapping in Repository") })
        assertTrue(findings.any { it.title.contains("Missing Result Type for Failable Operation") })
        assertTrue(findings.any { it.title.contains("Empty or Logging-Only Catch Block") })
    }
    
    @Test
    fun `handles well-structured repository without issues`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import kotlinx.coroutines.Dispatchers
            import kotlinx.coroutines.withContext
            import timber.log.Timber
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor(
                private val mealDao: MealDao
            ) : MealRepository {
                
                override suspend fun addMeal(meal: Meal): Result<Long> = withContext(Dispatchers.IO) {
                    try {
                        val id = mealDao.insertMeal(meal.toEntity())
                        Result.success(id)
                    } catch (e: SQLiteException) {
                        Timber.e(e, "Failed to insert meal")
                        Result.failure(AppError.DatabaseError("Failed to insert meal", e))
                    } catch (e: IOException) {
                        Timber.e(e, "IO error")
                        Result.failure(AppError.NetworkError("IO error", e))
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should have no findings - this is properly structured
        assertEquals(0, findings.size)
    }
    
    // Edge Cases
    
    @Test
    fun `handles empty file gracefully`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/Empty.kt",
            relativePath = "data/repository/Empty.kt",
            size = 0L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = ""
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `handles file without functions`() = runTest {
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
        
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `finding IDs are unique`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor(
                private val mealDao: MealDao
            ) : MealRepository {
                
                override suspend fun addMeal(meal: Meal): Result<Long> {
                    val id = mealDao.insertMeal(meal.toEntity())
                    return Result.success(id)
                }
                
                override suspend fun deleteMeal(id: Long): Long {
                    try {
                        mealDao.deleteMealById(id)
                        return id
                    } catch (e: Exception) {
                    }
                    return -1
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
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor(
                private val mealDao: MealDao
            ) : MealRepository {
                
                override suspend fun addMeal(meal: Meal): Result<Long> {
                    val id = mealDao.insertMeal(meal.toEntity())
                    return Result.success(id)
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
    fun `handles use case with proper Result type`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/AddMealUseCase.kt",
            relativePath = "domain/usecase/AddMealUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            import javax.inject.Inject
            
            class AddMealUseCase @Inject constructor(
                private val repository: MealRepository,
                private val validator: MealValidator
            ) {
                suspend operator fun invoke(meal: Meal): Result<Long> {
                    val validationResult = validator.validate(meal)
                    if (validationResult.isFailure) {
                        return validationResult.map { 0L }
                    }
                    
                    return repository.addMeal(meal)
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should have no findings - this is properly structured
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `detects multiple empty catch blocks`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            import javax.inject.Inject
            
            class MealRepositoryImpl @Inject constructor(
                private val mealDao: MealDao
            ) : MealRepository {
                
                override suspend fun addMeal(meal: Meal): Result<Long> {
                    try {
                        val id = mealDao.insertMeal(meal.toEntity())
                        return Result.success(id)
                    } catch (e: SQLiteException) {
                    }
                    return Result.failure(AppError.UnknownError("Failed"))
                }
                
                override suspend fun deleteMeal(id: Long): Result<Unit> {
                    try {
                        mealDao.deleteMealById(id)
                        return Result.success(Unit)
                    } catch (e: Exception) {
                    }
                    return Result.failure(AppError.UnknownError("Failed"))
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        val emptyBlockFindings = findings.filter { it.title.contains("Empty or Logging-Only Catch Block") }
        assertEquals(2, emptyBlockFindings.size)
    }
}
