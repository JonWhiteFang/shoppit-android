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
 * Unit tests for CodeSmellAnalyzer.
 * Tests detection of code smells including long functions, large classes,
 * high complexity, deep nesting, and too many parameters.
 */
class CodeSmellAnalyzerTest {
    
    private lateinit var analyzer: CodeSmellAnalyzer
    private lateinit var testFileInfo: FileInfo
    
    @Before
    fun setup() {
        analyzer = CodeSmellAnalyzer()
        testFileInfo = FileInfo(
            path = "/test/TestFile.kt",
            relativePath = "test/TestFile.kt",
            size = 1000L,
            lastModified = System.currentTimeMillis(),
            layer = CodeLayer.DOMAIN
        )
    }
    
    @Test
    fun `analyzer has correct properties`() {
        assertEquals("code-smell", analyzer.id)
        assertEquals("Code Smell Analyzer", analyzer.name)
        assertEquals(AnalysisCategory.CODE_SMELL, analyzer.category)
    }
    
    @Test
    fun `appliesTo returns true for Kotlin files in non-test layers`() {
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
    fun `appliesTo returns false for test files`() {
        val testFile = FileInfo(
            path = "/test/ModelTest.kt",
            relativePath = "test/ModelTest.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.TEST
        )
        
        assertFalse(analyzer.appliesTo(testFile))
    }
    
    @Test
    fun `detects long function exceeding 50 lines`() = runTest {
        val code = """
            package com.example
            
            fun longFunction() {
                ${(1..55).joinToString("\n") { "    println(\"Line $it\")" }}
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(testFileInfo, code)
        
        assertEquals(1, findings.size)
        val finding = findings[0]
        assertEquals("code-smell", finding.analyzer)
        assertEquals(AnalysisCategory.CODE_SMELL, finding.category)
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.title.contains("Long Function"))
        assertTrue(finding.description.contains("55 lines"))
    }
    
    @Test
    fun `does not flag function with 50 lines or less`() = runTest {
        val code = """
            package com.example
            
            fun normalFunction() {
                ${(1..45).joinToString("\n") { "    println(\"Line $it\")" }}
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(testFileInfo, code)
        
        assertTrue(findings.none { it.title.contains("Long Function") })
    }
    
    @Test
    fun `detects large class exceeding 300 lines`() = runTest {
        val code = """
            package com.example
            
            class LargeClass {
                ${(1..305).joinToString("\n") { "    fun function$it() { println(\"Function $it\") }" }}
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(testFileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Large Class") })
        val finding = findings.first { it.title.contains("Large Class") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("300 lines"))
    }
    
    @Test
    fun `does not flag class with 300 lines or less`() = runTest {
        val code = """
            package com.example
            
            class NormalClass {
                ${(1..250).joinToString("\n") { "    fun function$it() { println(\"Function $it\") }" }}
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(testFileInfo, code)
        
        assertTrue(findings.none { it.title.contains("Large Class") })
    }
    
    @Test
    fun `detects function with too many parameters`() = runTest {
        val code = """
            package com.example
            
            fun manyParams(
                param1: String,
                param2: Int,
                param3: Boolean,
                param4: Double,
                param5: Long,
                param6: Float
            ) {
                println("Too many parameters")
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(testFileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Too Many Parameters") })
        val finding = findings.first { it.title.contains("Too Many Parameters") }
        assertEquals(Priority.LOW, finding.priority)
        assertTrue(finding.description.contains("6 parameters"))
    }
    
    @Test
    fun `does not flag function with 5 or fewer parameters`() = runTest {
        val code = """
            package com.example
            
            fun normalParams(
                param1: String,
                param2: Int,
                param3: Boolean,
                param4: Double,
                param5: Long
            ) {
                println("Normal parameters")
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(testFileInfo, code)
        
        assertTrue(findings.none { it.title.contains("Too Many Parameters") })
    }
    
    @Test
    fun `detects high cyclomatic complexity`() = runTest {
        val code = """
            package com.example
            
            fun complexFunction(x: Int): String {
                if (x > 0) {
                    if (x > 10) {
                        if (x > 20) {
                            if (x > 30) {
                                if (x > 40) {
                                    if (x > 50) {
                                        if (x > 60) {
                                            if (x > 70) {
                                                if (x > 80) {
                                                    if (x > 90) {
                                                        return "Very high"
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
                return "Low"
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(testFileInfo, code)
        
        assertTrue(findings.any { it.title.contains("High Cyclomatic Complexity") })
        val finding = findings.first { it.title.contains("High Cyclomatic Complexity") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("complexity"))
    }
    
    @Test
    fun `calculates complexity correctly with when expressions`() = runTest {
        val code = """
            package com.example
            
            fun whenFunction(x: Int): String {
                return when (x) {
                    1 -> "one"
                    2 -> "two"
                    3 -> "three"
                    4 -> "four"
                    5 -> "five"
                    6 -> "six"
                    7 -> "seven"
                    8 -> "eight"
                    9 -> "nine"
                    10 -> "ten"
                    11 -> "eleven"
                    12 -> "twelve"
                    13 -> "thirteen"
                    14 -> "fourteen"
                    15 -> "fifteen"
                    16 -> "sixteen"
                    else -> "other"
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(testFileInfo, code)
        
        // When with 17 branches should exceed complexity threshold of 15
        assertTrue(findings.any { it.title.contains("High Cyclomatic Complexity") })
    }
    
    @Test
    fun `detects deep nesting exceeding 4 levels`() = runTest {
        val code = """
            package com.example
            
            fun deeplyNested(x: Int?) {
                if (x != null) {
                    if (x > 0) {
                        if (x < 100) {
                            if (x % 2 == 0) {
                                if (x % 3 == 0) {
                                    println("Deeply nested")
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(testFileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Deep Nesting") })
        val finding = findings.first { it.title.contains("Deep Nesting") }
        assertEquals(Priority.MEDIUM, finding.priority)
        assertTrue(finding.description.contains("nesting depth"))
    }
    
    @Test
    fun `does not flag nesting at or below 4 levels`() = runTest {
        val code = """
            package com.example
            
            fun normalNesting(x: Int?) {
                if (x != null) {
                    if (x > 0) {
                        if (x < 100) {
                            if (x % 2 == 0) {
                                println("Normal nesting")
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(testFileInfo, code)
        
        assertTrue(findings.none { it.title.contains("Deep Nesting") })
    }
    
    @Test
    fun `detects multiple code smells in same function`() = runTest {
        val code = """
            package com.example
            
            fun problematicFunction(
                param1: String,
                param2: Int,
                param3: Boolean,
                param4: Double,
                param5: Long,
                param6: Float
            ) {
                ${(1..55).joinToString("\n") { "    println(\"Line $it\")" }}
                
                if (param1.isNotEmpty()) {
                    if (param2 > 0) {
                        if (param3) {
                            if (param4 > 0.0) {
                                if (param5 > 0L) {
                                    println("Deeply nested")
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(testFileInfo, code)
        
        // Should detect: long function, too many parameters, deep nesting
        assertTrue(findings.size >= 3)
        assertTrue(findings.any { it.title.contains("Long Function") })
        assertTrue(findings.any { it.title.contains("Too Many Parameters") })
        assertTrue(findings.any { it.title.contains("Deep Nesting") })
    }
    
    @Test
    fun `handles empty file gracefully`() = runTest {
        val code = ""
        
        val findings = analyzer.analyze(testFileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `handles file with only package declaration`() = runTest {
        val code = "package com.example"
        
        val findings = analyzer.analyze(testFileInfo, code)
        
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `handles file with syntax errors gracefully`() = runTest {
        val code = """
            package com.example
            
            fun brokenFunction( {
                this is not valid kotlin
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(testFileInfo, code)
        
        // Should return empty list instead of crashing
        assertEquals(0, findings.size)
    }
    
    @Test
    fun `finding IDs are unique per issue location`() = runTest {
        val code = """
            package com.example
            
            fun longFunction1() {
                ${(1..55).joinToString("\n") { "    println(\"Line $it\")" }}
            }
            
            fun longFunction2() {
                ${(1..55).joinToString("\n") { "    println(\"Line $it\")" }}
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(testFileInfo, code)
        
        val longFunctionFindings = findings.filter { it.title.contains("Long Function") }
        assertEquals(2, longFunctionFindings.size)
        
        // IDs should be different
        assertNotEquals(longFunctionFindings[0].id, longFunctionFindings[1].id)
    }
    
    @Test
    fun `findings include helpful recommendations`() = runTest {
        val code = """
            package com.example
            
            fun longFunction() {
                ${(1..55).joinToString("\n") { "    println(\"Line $it\")" }}
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(testFileInfo, code)
        
        val finding = findings[0]
        assertNotNull(finding.recommendation)
        assertTrue(finding.recommendation.isNotEmpty())
        assertTrue(finding.beforeExample != null)
        assertTrue(finding.afterExample != null)
        assertTrue(finding.references.isNotEmpty())
    }
}
