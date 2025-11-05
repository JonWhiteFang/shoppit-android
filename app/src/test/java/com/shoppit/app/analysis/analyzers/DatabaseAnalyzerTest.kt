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
 * Unit tests for DatabaseAnalyzer.
 * Tests validation of Room database patterns including:
 * - DAO query functions return Flow
 * - DAO mutations are suspend functions
 * - Query parameterization (SQL injection prevention)
 * - Foreign keys with CASCADE
 */
class DatabaseAnalyzerTest {
    
    private lateinit var analyzer: DatabaseAnalyzer
    
    @Before
    fun setup() {
        analyzer = DatabaseAnalyzer()
    }
    
    @Test
    fun `analyzer has correct properties`() {
        assertEquals("database", analyzer.id)
        assertEquals("Database Analyzer", analyzer.name)
        assertEquals(AnalysisCategory.DATABASE, analyzer.category)
    }
    
    @Test
    fun `appliesTo returns true for DAO files in data layer`() {
        val file = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        assertTrue(analyzer.appliesTo(file))
    }
    
    @Test
    fun `appliesTo returns true for Entity files in data layer`() {
        val file = FileInfo(
            path = "/data/local/entity/MealEntity.kt",
            relativePath = "data/local/entity/MealEntity.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        assertTrue(analyzer.appliesTo(file))
    }
    
    @Test
    fun `appliesTo returns false for non-DAO non-Entity files`() {
        val file = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        assertFalse(analyzer.appliesTo(file))
    }
    
    @Test
    fun `appliesTo returns false for files not in data layer`() {
        val file = FileInfo(
            path = "/ui/meal/MealViewModel.kt",
            relativePath = "ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        assertFalse(analyzer.appliesTo(file))
    }
    
    // DAO Query Function Tests (Requirement 8.1)
    
    @Test
    fun `detects query function not returning Flow`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            import androidx.room.Dao
            import androidx.room.Query
            
            @Dao
            interface MealDao {
                @Query("SELECT * FROM meals")
                fun getAllMeals(): List<MealEntity>
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Should Return Flow") })
        val finding = findings.first { it.title.contains("Should Return Flow") }
        assertEquals(Priority.HIGH, finding.priority)
        assertTrue(finding.description.contains("getAllMeals"))
        assertTrue(finding.recommendation.contains("Flow"))
    }
    
    @Test
    fun `does not flag query function returning Flow`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            import androidx.room.Dao
            import androidx.room.Query
            import kotlinx.coroutines.flow.Flow
            
            @Dao
            interface MealDao {
                @Query("SELECT * FROM meals")
                fun getAllMeals(): Flow<List<MealEntity>>
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Should Return Flow") })
    }
    
    @Test
    fun `does not flag suspend query function`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            import androidx.room.Dao
            import androidx.room.Query
            
            @Dao
            interface MealDao {
                @Query("SELECT * FROM meals WHERE id = :id")
                suspend fun getMealById(id: Long): MealEntity?
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Should Return Flow") })
    }
    
    // DAO Mutation Function Tests (Requirement 8.2)
    
    @Test
    fun `detects insert function not suspend`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            import androidx.room.Dao
            import androidx.room.Insert
            
            @Dao
            interface MealDao {
                @Insert
                fun insertMeal(meal: MealEntity): Long
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Should Be Suspend") })
        val finding = findings.first { it.title.contains("Should Be Suspend") }
        assertEquals(Priority.HIGH, finding.priority)
        assertTrue(finding.description.contains("insertMeal"))
        assertTrue(finding.description.contains("Insert"))
        assertTrue(finding.recommendation.contains("suspend"))
    }
    
    @Test
    fun `detects update function not suspend`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            import androidx.room.Dao
            import androidx.room.Update
            
            @Dao
            interface MealDao {
                @Update
                fun updateMeal(meal: MealEntity): Int
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Should Be Suspend") })
        val finding = findings.first { it.title.contains("Should Be Suspend") }
        assertTrue(finding.description.contains("Update"))
    }
    
    @Test
    fun `detects delete function not suspend`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            import androidx.room.Dao
            import androidx.room.Delete
            
            @Dao
            interface MealDao {
                @Delete
                fun deleteMeal(meal: MealEntity): Int
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Should Be Suspend") })
        val finding = findings.first { it.title.contains("Should Be Suspend") }
        assertTrue(finding.description.contains("Delete"))
    }
    
    @Test
    fun `does not flag suspend mutation functions`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            import androidx.room.Dao
            import androidx.room.Insert
            import androidx.room.Update
            import androidx.room.Delete
            
            @Dao
            interface MealDao {
                @Insert
                suspend fun insertMeal(meal: MealEntity): Long
                
                @Update
                suspend fun updateMeal(meal: MealEntity): Int
                
                @Delete
                suspend fun deleteMeal(meal: MealEntity): Int
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Should Be Suspend") })
    }
    
    // Query Parameterization Tests (Requirement 8.5)
    
    @Test
    fun `detects SQL injection risk with string concatenation`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            import androidx.room.Dao
            import androidx.room.Query
            import kotlinx.coroutines.flow.Flow
            
            @Dao
            interface MealDao {
                @Query("SELECT * FROM meals WHERE name = '" + name + "'")
                fun searchMeals(name: String): Flow<List<MealEntity>>
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("SQL Injection Risk") })
        val finding = findings.first { it.title.contains("SQL Injection Risk") }
        assertEquals(Priority.CRITICAL, finding.priority)
        assertTrue(finding.description.contains("string concatenation"))
        assertTrue(finding.recommendation.contains("parameterized"))
    }
    
    @Test
    fun `detects SQL injection risk with string interpolation`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            import androidx.room.Dao
            import androidx.room.Query
            import kotlinx.coroutines.flow.Flow
            
            @Dao
            interface MealDao {
                @Query("SELECT * FROM meals WHERE name = '\${name}'")
                fun searchMeals(name: String): Flow<List<MealEntity>>
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("SQL Injection Risk") })
    }
    
    @Test
    fun `does not flag parameterized queries`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            import androidx.room.Dao
            import androidx.room.Query
            import kotlinx.coroutines.flow.Flow
            
            @Dao
            interface MealDao {
                @Query("SELECT * FROM meals WHERE name = :name")
                fun searchMeals(name: String): Flow<List<MealEntity>>
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("SQL Injection Risk") })
    }
    
    @Test
    fun `detects missing parameterization when function has parameters`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            import androidx.room.Dao
            import androidx.room.Query
            import kotlinx.coroutines.flow.Flow
            
            @Dao
            interface MealDao {
                @Query("SELECT * FROM meals WHERE id = 1")
                fun getMealById(id: Long): Flow<MealEntity?>
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Should Use Parameterized") })
        val finding = findings.first { it.title.contains("Should Use Parameterized") }
        assertEquals(Priority.HIGH, finding.priority)
        assertTrue(finding.recommendation.contains("named parameters"))
    }
    
    // Foreign Key Tests (Requirement 8.4)
    
    @Test
    fun `detects foreign key without CASCADE`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/entity/IngredientEntity.kt",
            relativePath = "data/local/entity/IngredientEntity.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.entity
            
            import androidx.room.Entity
            import androidx.room.ForeignKey
            
            @Entity(
                tableName = "ingredients",
                foreignKeys = [
                    ForeignKey(
                        entity = MealEntity::class,
                        parentColumns = ["id"],
                        childColumns = ["meal_id"]
                    )
                ]
            )
            data class IngredientEntity(
                val id: Long,
                val mealId: Long,
                val name: String
            )
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Should Use CASCADE") })
        val finding = findings.first { it.title.contains("Should Use CASCADE") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("CASCADE"))
        assertTrue(finding.recommendation.contains("onDelete = ForeignKey.CASCADE"))
    }
    
    @Test
    fun `does not flag foreign key with CASCADE`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/entity/IngredientEntity.kt",
            relativePath = "data/local/entity/IngredientEntity.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.entity
            
            import androidx.room.Entity
            import androidx.room.ForeignKey
            
            @Entity(
                tableName = "ingredients",
                foreignKeys = [
                    ForeignKey(
                        entity = MealEntity::class,
                        parentColumns = ["id"],
                        childColumns = ["meal_id"],
                        onDelete = ForeignKey.CASCADE
                    )
                ]
            )
            data class IngredientEntity(
                val id: Long,
                val mealId: Long,
                val name: String
            )
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Should Use CASCADE") })
    }
    
    @Test
    fun `handles multiline foreign key definition`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/entity/IngredientEntity.kt",
            relativePath = "data/local/entity/IngredientEntity.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.entity
            
            import androidx.room.Entity
            import androidx.room.ForeignKey
            
            @Entity(
                tableName = "ingredients",
                foreignKeys = [
                    ForeignKey(
                        entity = MealEntity::class,
                        parentColumns = ["id"],
                        childColumns = ["meal_id"],
                        onUpdate = ForeignKey.CASCADE
                    )
                ]
            )
            data class IngredientEntity(
                val id: Long,
                val mealId: Long,
                val name: String
            )
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should still flag missing onDelete CASCADE
        assertTrue(findings.any { it.title.contains("Should Use CASCADE") })
    }
    
    // General Tests
    
    @Test
    fun `handles empty DAO interface`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            import androidx.room.Dao
            
            @Dao
            interface MealDao {
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `handles entity without foreign keys`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/entity/MealEntity.kt",
            relativePath = "data/local/entity/MealEntity.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.entity
            
            import androidx.room.Entity
            import androidx.room.PrimaryKey
            
            @Entity(tableName = "meals")
            data class MealEntity(
                @PrimaryKey(autoGenerate = true)
                val id: Long = 0,
                val name: String
            )
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `detects multiple issues in same DAO`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            import androidx.room.Dao
            import androidx.room.Insert
            import androidx.room.Query
            
            @Dao
            interface MealDao {
                @Query("SELECT * FROM meals")
                fun getAllMeals(): List<MealEntity>
                
                @Insert
                fun insertMeal(meal: MealEntity): Long
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should have both Flow and suspend findings
        assertEquals(2, findings.size)
        assertTrue(findings.any { it.title.contains("Should Return Flow") })
        assertTrue(findings.any { it.title.contains("Should Be Suspend") })
    }
    
    @Test
    fun `finding IDs are unique`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            import androidx.room.Dao
            import androidx.room.Insert
            import androidx.room.Query
            
            @Dao
            interface MealDao {
                @Query("SELECT * FROM meals")
                fun getAllMeals(): List<MealEntity>
                
                @Insert
                fun insertMeal(meal: MealEntity): Long
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
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            import androidx.room.Dao
            import androidx.room.Query
            
            @Dao
            interface MealDao {
                @Query("SELECT * FROM meals")
                fun getAllMeals(): List<MealEntity>
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
    fun `handles DAO with complex query`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            import androidx.room.Dao
            import androidx.room.Query
            import kotlinx.coroutines.flow.Flow
            
            @Dao
            interface MealDao {
                @Query(""${'"'}
                    SELECT * FROM meals 
                    WHERE name LIKE '%' || :query || '%'
                    ORDER BY created_at DESC
                ""${'"'})
                fun searchMeals(query: String): Flow<List<MealEntity>>
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should not flag this as it uses proper parameterization
        assertFalse(findings.any { it.title.contains("SQL Injection Risk") })
    }
    
    @Test
    fun `does not analyze non-DAO non-Entity files`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.repository
            
            class MealRepositoryImpl {
                fun getMeals() = emptyList<Meal>()
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
}
