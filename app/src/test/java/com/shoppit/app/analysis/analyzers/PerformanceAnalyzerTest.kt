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
 * Unit tests for PerformanceAnalyzer.
 * Tests detection of performance optimization opportunities including:
 * - Inefficient list operations in loops
 * - String concatenation in loops
 * - Unstable Compose parameters
 */
class PerformanceAnalyzerTest {
    
    private lateinit var analyzer: PerformanceAnalyzer
    
    @Before
    fun setup() {
        analyzer = PerformanceAnalyzer()
    }
    
    @Test
    fun `analyzer has correct properties`() {
        assertEquals("performance", analyzer.id)
        assertEquals("Performance Analyzer", analyzer.name)
        assertEquals(AnalysisCategory.PERFORMANCE, analyzer.category)
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
    
    // List Operation Tests (Requirement 9.1)
    
    @Test
    fun `detects inefficient list operations in for loop`() = runTest {
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
                fun process(items: List<Item>) {
                    for (item in items) {
                        val result = data
                            .filter { it.value > 0 }
                            .map { it.transform() }
                            .toList()
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Inefficient List Operations") })
        val finding = findings.first { it.title.contains("Inefficient List Operations") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("intermediate collections"))
        assertTrue(finding.recommendation.contains("asSequence()"))
    }
    
    @Test
    fun `detects inefficient list operations in forEach`() = runTest {
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
                fun process(items: List<Item>) {
                    items.forEach { item ->
                        val result = data
                            .filter { it.value > 0 }
                            .map { it.transform() }
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Inefficient List Operations") })
    }
    
    @Test
    fun `detects chained operations with toList in loop`() = runTest {
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
                fun process(items: List<Item>) {
                    for (item in items) {
                        val result = data.filter { it.value > 0 }.toList()
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Inefficient List Operations") })
    }
    
    @Test
    fun `does not flag single list operation in loop`() = runTest {
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
                fun process(items: List<Item>) {
                    for (item in items) {
                        val result = data.filter { it.value > 0 }
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Inefficient List Operations") })
    }
    
    @Test
    fun `does not flag list operations outside loops`() = runTest {
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
                fun process(items: List<Item>) {
                    val result = data
                        .filter { it.value > 0 }
                        .map { it.transform() }
                        .toList()
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Inefficient List Operations") })
    }
    
    // String Concatenation Tests (Requirement 9.2)
    
    @Test
    fun `detects string concatenation with += in for loop`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/FormatMealsUseCase.kt",
            relativePath = "domain/usecase/FormatMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            class FormatMealsUseCase {
                fun format(items: List<Item>): String {
                    var result = ""
                    for (item in items) {
                        result += item.name + ", "
                    }
                    return result
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("String Concatenation in Loop") })
        val finding = findings.first { it.title.contains("String Concatenation in Loop") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("immutable"))
        assertTrue(finding.recommendation.contains("StringBuilder"))
    }
    
    @Test
    fun `detects string concatenation with = and + in loop`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/FormatMealsUseCase.kt",
            relativePath = "domain/usecase/FormatMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            class FormatMealsUseCase {
                fun format(items: List<Item>): String {
                    var result = ""
                    for (item in items) {
                        result = result + item.name
                    }
                    return result
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("String Concatenation in Loop") })
    }
    
    @Test
    fun `detects string concatenation in while loop`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/FormatMealsUseCase.kt",
            relativePath = "domain/usecase/FormatMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            class FormatMealsUseCase {
                fun format(items: List<Item>): String {
                    var result = ""
                    var i = 0
                    while (i < items.size) {
                        result += items[i].name
                        i++
                    }
                    return result
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("String Concatenation in Loop") })
    }
    
    @Test
    fun `detects string concatenation in forEach`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/FormatMealsUseCase.kt",
            relativePath = "domain/usecase/FormatMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            class FormatMealsUseCase {
                fun format(items: List<Item>): String {
                    var result = ""
                    items.forEach { item ->
                        result += item.name
                    }
                    return result
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("String Concatenation in Loop") })
    }
    
    @Test
    fun `does not flag string concatenation outside loops`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/FormatMealsUseCase.kt",
            relativePath = "domain/usecase/FormatMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            class FormatMealsUseCase {
                fun format(item1: String, item2: String): String {
                    var result = ""
                    result += item1
                    result += item2
                    return result
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("String Concatenation in Loop") })
    }
    
    @Test
    fun `does not flag non-string variables in loops`() = runTest {
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
                fun process(items: List<Item>): Int {
                    var count = 0
                    for (item in items) {
                        count += 1
                    }
                    return count
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("String Concatenation in Loop") })
    }
    
    // Unstable Compose Parameters Tests (Requirement 9.4)
    
    @Test
    fun `detects MutableList parameter in Composable`() = runTest {
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
            fun MealList(
                items: MutableList<Meal>,
                onItemClick: (Meal) -> Unit
            ) {
                // Composable content
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Unstable Compose Parameters") })
        val finding = findings.first { it.title.contains("Unstable Compose Parameters") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("items"))
        assertTrue(finding.description.contains("MutableList"))
        assertTrue(finding.recommendation.contains("immutable"))
    }
    
    @Test
    fun `detects ArrayList parameter in Composable`() = runTest {
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
            fun MealList(
                items: ArrayList<Meal>,
                onItemClick: (Meal) -> Unit
            ) {
                // Composable content
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Unstable Compose Parameters") })
    }
    
    @Test
    fun `detects MutableSet parameter in Composable`() = runTest {
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
            fun MealTags(
                tags: MutableSet<String>
            ) {
                // Composable content
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Unstable Compose Parameters") })
    }
    
    @Test
    fun `detects HashMap parameter in Composable`() = runTest {
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
            fun MealDetails(
                properties: HashMap<String, String>
            ) {
                // Composable content
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Unstable Compose Parameters") })
    }
    
    @Test
    fun `detects Array parameter in Composable`() = runTest {
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
            fun MealList(
                items: Array<Meal>
            ) {
                // Composable content
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Unstable Compose Parameters") })
    }
    
    @Test
    fun `does not flag stable List parameter in Composable`() = runTest {
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
            fun MealList(
                items: List<Meal>,
                onItemClick: (Meal) -> Unit
            ) {
                // Composable content
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Unstable Compose Parameters") })
    }
    
    @Test
    fun `does not flag stable Set parameter in Composable`() = runTest {
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
            fun MealTags(
                tags: Set<String>
            ) {
                // Composable content
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Unstable Compose Parameters") })
    }
    
    @Test
    fun `does not flag stable Map parameter in Composable`() = runTest {
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
            fun MealDetails(
                properties: Map<String, String>
            ) {
                // Composable content
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Unstable Compose Parameters") })
    }
    
    @Test
    fun `does not analyze non-Compose files for unstable parameters`() = runTest {
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
                fun process(items: MutableList<Meal>) {
                    // Not a Composable
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Unstable Compose Parameters") })
    }
    
    @Test
    fun `detects multiple unstable parameters in same Composable`() = runTest {
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
            fun MealList(
                items: MutableList<Meal>,
                tags: MutableSet<String>,
                properties: HashMap<String, String>
            ) {
                // Composable content
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Unstable Compose Parameters") })
        val finding = findings.first { it.title.contains("Unstable Compose Parameters") }
        assertTrue(finding.description.contains("items"))
        assertTrue(finding.description.contains("tags"))
        assertTrue(finding.description.contains("properties"))
    }
    
    // General Tests
    
    @Test
    fun `handles empty file`() = runTest {
        val fileInfo = FileInfo(
            path = "/domain/usecase/EmptyUseCase.kt",
            relativePath = "domain/usecase/EmptyUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = ""
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `detects multiple performance issues in same file`() = runTest {
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
            
            class MealProcessor {
                fun process(items: List<Item>): String {
                    var result = ""
                    for (item in items) {
                        result += item.name
                        val filtered = data.filter { it.value > 0 }.map { it.transform() }
                    }
                    return result
                }
            }
            
            @Composable
            fun MealList(
                items: MutableList<Meal>
            ) {
                // Composable content
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should have all three types of findings
        assertTrue(findings.size >= 3)
        assertTrue(findings.any { it.title.contains("String Concatenation") })
        assertTrue(findings.any { it.title.contains("Inefficient List Operations") })
        assertTrue(findings.any { it.title.contains("Unstable Compose Parameters") })
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
            
            class MealProcessor {
                fun process(items: List<Item>): String {
                    var result = ""
                    for (item in items) {
                        result += item.name
                    }
                    return result
                }
            }
            
            @Composable
            fun MealList(
                items: MutableList<Meal>
            ) {
                // Composable content
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
            path = "/domain/usecase/FormatMealsUseCase.kt",
            relativePath = "domain/usecase/FormatMealsUseCase.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DOMAIN
        )
        
        val code = """
            package com.shoppit.app.domain.usecase
            
            class FormatMealsUseCase {
                fun format(items: List<Item>): String {
                    var result = ""
                    for (item in items) {
                        result += item.name
                    }
                    return result
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
    fun `handles nested loops correctly`() = runTest {
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
                fun process(items: List<Item>) {
                    for (item in items) {
                        for (subItem in item.subItems) {
                            val result = data.filter { it.value > 0 }.map { it.transform() }
                        }
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should detect the issue in the inner loop
        assertTrue(findings.any { it.title.contains("Inefficient List Operations") })
    }
    
    @Test
    fun `handles multiline function parameters`() = runTest {
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
            fun MealList(
                items: MutableList<Meal>,
                onItemClick: (Meal) -> Unit,
                modifier: Modifier = Modifier
            ) {
                // Composable content
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Unstable Compose Parameters") })
        val finding = findings.first { it.title.contains("Unstable Compose Parameters") }
        assertTrue(finding.description.contains("items"))
    }
}
