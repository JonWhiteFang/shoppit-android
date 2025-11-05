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
 * Unit tests for ComposeAnalyzer.
 * Tests validation of Jetpack Compose best practices including:
 * - Modifier parameter validation
 * - remember usage detection
 * - LazyColumn validation
 */
class ComposeAnalyzerTest {
    
    private lateinit var analyzer: ComposeAnalyzer
    
    @Before
    fun setup() {
        analyzer = ComposeAnalyzer()
    }
    
    @Test
    fun `analyzer has correct properties`() {
        assertEquals("compose", analyzer.id)
        assertEquals("Compose Analyzer", analyzer.name)
        assertEquals(AnalysisCategory.COMPOSE, analyzer.category)
    }
    
    @Test
    fun `appliesTo returns true for UI layer Screen files`() {
        val screenFile = FileInfo(
            path = "/ui/meal/MealListScreen.kt",
            relativePath = "ui/meal/MealListScreen.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        assertTrue(analyzer.appliesTo(screenFile))
    }
    
    @Test
    fun `appliesTo returns true for UI layer files in ui directory`() {
        val uiFile = FileInfo(
            path = "/ui/components/MealCard.kt",
            relativePath = "ui/components/MealCard.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        assertTrue(analyzer.appliesTo(uiFile))
    }
    
    @Test
    fun `appliesTo returns false for non-UI layer files`() {
        val domainFile = FileInfo(
            path = "/domain/model/Meal.kt",
            relativePath = "domain/model/Meal.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        assertFalse(analyzer.appliesTo(domainFile))
    }
    
    // Modifier Parameter Validation Tests
    
    @Test
    fun `detects missing Modifier parameter in public Composable`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealCard.kt",
            relativePath = "ui/meal/MealCard.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            import androidx.compose.material3.Card
            import androidx.compose.material3.Text
            
            @Composable
            fun MealCard(
                meal: Meal,
                onClick: () -> Unit
            ) {
                Card(onClick = onClick) {
                    Text(meal.name)
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Missing Modifier Parameter") })
        val finding = findings.first { it.title.contains("Missing Modifier Parameter") }
        assertEquals("compose", finding.analyzer)
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("MealCard"))
        assertTrue(finding.recommendation.contains("modifier: Modifier = Modifier"))
    }
    
    @Test
    fun `detects Modifier parameter without default value`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealCard.kt",
            relativePath = "ui/meal/MealCard.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            
            @Composable
            fun MealCard(
                meal: Meal,
                modifier: Modifier
            ) {
                Card(modifier = modifier) {
                    Text(meal.name)
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Modifier Parameter Missing Default Value") })
        val finding = findings.first { it.title.contains("Modifier Parameter Missing Default Value") }
        assertEquals(Priority.LOW, finding.priority)
        assertTrue(finding.autoFixable)
    }
    
    @Test
    fun `does not flag Composable with correct Modifier parameter`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealCard.kt",
            relativePath = "ui/meal/MealCard.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            
            @Composable
            fun MealCard(
                meal: Meal,
                onClick: () -> Unit,
                modifier: Modifier = Modifier
            ) {
                Card(modifier = modifier, onClick = onClick) {
                    Text(meal.name)
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Modifier") })
    }
    
    @Test
    fun `does not flag private Composables without Modifier`() = runTest {
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
            private fun MealHeader(title: String) {
                Text(title)
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Missing Modifier Parameter") })
    }
    
    // Remember Usage Detection Tests
    
    @Test
    fun `detects expensive list operation not wrapped in remember`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealListScreen.kt",
            relativePath = "ui/meal/MealListScreen.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            
            @Composable
            fun MealListScreen(meals: List<Meal>) {
                val sortedMeals = meals.sortedBy { it.name }
                
                LazyColumn {
                    items(sortedMeals) { meal ->
                        MealCard(meal)
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Expensive Computation Not Wrapped in remember") })
        val finding = findings.first { it.title.contains("Expensive Computation Not Wrapped in remember") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("sortedBy"))
        assertTrue(finding.recommendation.contains("remember"))
    }
    
    @Test
    fun `detects filter operation not wrapped in remember`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealListScreen.kt",
            relativePath = "ui/meal/MealListScreen.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            
            @Composable
            fun MealListScreen(meals: List<Meal>, showFavorites: Boolean) {
                val filteredMeals = meals.filter { it.isFavorite }
                
                LazyColumn {
                    items(filteredMeals) { meal ->
                        MealCard(meal)
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Expensive Computation Not Wrapped in remember") })
    }
    
    @Test
    fun `does not flag list operation wrapped in remember`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealListScreen.kt",
            relativePath = "ui/meal/MealListScreen.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            
            @Composable
            fun MealListScreen(meals: List<Meal>) {
                val sortedMeals = remember(meals) {
                    meals.sortedBy { it.name }
                }
                
                LazyColumn {
                    items(sortedMeals) { meal ->
                        MealCard(meal)
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Expensive Computation Not Wrapped in remember") })
    }
    
    // LazyColumn Validation Tests
    
    @Test
    fun `detects LazyColumn items without key parameter`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealListScreen.kt",
            relativePath = "ui/meal/MealListScreen.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            import androidx.compose.foundation.lazy.LazyColumn
            import androidx.compose.foundation.lazy.items
            
            @Composable
            fun MealListScreen(meals: List<Meal>) {
                LazyColumn {
                    items(meals) { meal ->
                        MealCard(meal = meal)
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("LazyColumn items() Missing key Parameter") })
        val finding = findings.first { it.title.contains("LazyColumn items() Missing key Parameter") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.recommendation.contains("key parameter"))
        assertTrue(finding.afterExample?.contains("key = { it.id }") == true)
    }
    
    @Test
    fun `does not flag LazyColumn items with key parameter`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealListScreen.kt",
            relativePath = "ui/meal/MealListScreen.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            import androidx.compose.foundation.lazy.LazyColumn
            import androidx.compose.foundation.lazy.items
            
            @Composable
            fun MealListScreen(meals: List<Meal>) {
                LazyColumn {
                    items(meals, key = { it.id }) { meal ->
                        MealCard(meal = meal)
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("LazyColumn items() Missing key Parameter") })
    }
    
    @Test
    fun `detects nested LazyColumn`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealListScreen.kt",
            relativePath = "ui/meal/MealListScreen.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            import androidx.compose.foundation.lazy.LazyColumn
            import androidx.compose.foundation.lazy.items
            
            @Composable
            fun MealListScreen(sections: List<MealSection>) {
                LazyColumn {
                    items(sections) { section ->
                        Text(section.title)
                        LazyColumn {
                            items(section.meals) { meal ->
                                MealCard(meal)
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Nested LazyColumn Detected") })
        val finding = findings.first { it.title.contains("Nested LazyColumn Detected") }
        assertEquals(Priority.HIGH, finding.priority)
        assertTrue(finding.description.contains("anti-pattern"))
        assertTrue(finding.recommendation.contains("single LazyColumn"))
    }
    
    @Test
    fun `does not flag single LazyColumn`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealListScreen.kt",
            relativePath = "ui/meal/MealListScreen.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            import androidx.compose.foundation.lazy.LazyColumn
            import androidx.compose.foundation.lazy.items
            
            @Composable
            fun MealListScreen(meals: List<Meal>) {
                LazyColumn {
                    items(meals, key = { it.id }) { meal ->
                        MealCard(meal = meal)
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Nested LazyColumn") })
    }
    
    // Multiple Issues Tests
    
    @Test
    fun `detects multiple issues in same Composable`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealListScreen.kt",
            relativePath = "ui/meal/MealListScreen.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            import androidx.compose.foundation.lazy.LazyColumn
            import androidx.compose.foundation.lazy.items
            
            @Composable
            fun MealListScreen(meals: List<Meal>) {
                val sortedMeals = meals.sortedBy { it.name }
                
                LazyColumn {
                    items(sortedMeals) { meal ->
                        MealCard(meal = meal)
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should detect: missing Modifier, missing remember, missing key
        assertTrue(findings.size >= 3)
        assertTrue(findings.any { it.title.contains("Missing Modifier Parameter") })
        assertTrue(findings.any { it.title.contains("Expensive Computation Not Wrapped in remember") })
        assertTrue(findings.any { it.title.contains("LazyColumn items() Missing key Parameter") })
    }
    
    @Test
    fun `handles multiple Composables in same file`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealComponents.kt",
            relativePath = "ui/meal/MealComponents.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            
            @Composable
            fun MealCard(meal: Meal) {
                Card {
                    Text(meal.name)
                }
            }
            
            @Composable
            fun MealList(meals: List<Meal>, modifier: Modifier = Modifier) {
                LazyColumn(modifier = modifier) {
                    items(meals) { meal ->
                        MealCard(meal)
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // MealCard is missing Modifier, MealList items missing key
        assertTrue(findings.size >= 2)
        assertTrue(findings.any { it.description.contains("MealCard") })
        assertTrue(findings.any { it.title.contains("LazyColumn items() Missing key Parameter") })
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
    fun `handles file with no Composables`() = runTest {
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
    fun `handles Composable with complex parameter list`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealCard.kt",
            relativePath = "ui/meal/MealCard.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            
            @Composable
            fun MealCard(
                meal: Meal,
                onClick: () -> Unit,
                onLongClick: () -> Unit = {},
                enabled: Boolean = true,
                modifier: Modifier = Modifier
            ) {
                Card(modifier = modifier, onClick = onClick) {
                    Text(meal.name)
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should not flag this - it has correct Modifier parameter
        assertFalse(findings.any { it.title.contains("Modifier") })
    }
    
    @Test
    fun `finding IDs are unique`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealListScreen.kt",
            relativePath = "ui/meal/MealListScreen.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            import androidx.compose.foundation.lazy.LazyColumn
            import androidx.compose.foundation.lazy.items
            
            @Composable
            fun MealListScreen(meals: List<Meal>) {
                val sortedMeals = meals.sortedBy { it.name }
                val filteredMeals = meals.filter { it.isFavorite }
                
                LazyColumn {
                    items(sortedMeals) { meal ->
                        MealCard(meal = meal)
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
            path = "/ui/meal/MealCard.kt",
            relativePath = "ui/meal/MealCard.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            import androidx.compose.runtime.Composable
            
            @Composable
            fun MealCard(meal: Meal) {
                Card {
                    Text(meal.name)
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
}
