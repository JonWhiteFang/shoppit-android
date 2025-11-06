package com.shoppit.analysis.analyzers

import com.shoppit.analysis.core.CodeAnalyzer
import com.shoppit.analysis.models.AnalysisCategory
import com.shoppit.analysis.models.Effort
import com.shoppit.analysis.models.FileInfo
import com.shoppit.analysis.models.Finding
import com.shoppit.analysis.models.Priority
import java.util.UUID

/**
 * Analyzer that validates documentation quality throughout the codebase.
 * 
 * Validates:
 * - Public API functions have KDoc comments
 * - Complex algorithms have inline comments
 * - Data class properties are documented
 * - Sealed class subclasses are documented
 * 
 * Requirements: 12.1, 12.2, 12.3, 12.4, 12.5
 */
class DocumentationAnalyzer : CodeAnalyzer {
    
    override val id: String = "documentation"
    
    override val name: String = "Documentation Analyzer"
    
    override val category: AnalysisCategory = AnalysisCategory.DOCUMENTATION
    
    /**
     * Complexity threshold for requiring inline comments.
     * Functions with cyclomatic complexity above this should have comments.
     */
    private val complexityThreshold = 10
    
    override suspend fun analyze(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Validate public API has KDoc comments
        findings.addAll(validatePublicApiDocumentation(file, fileContent))
        
        // Validate complex algorithms have inline comments
        findings.addAll(validateComplexFunctionComments(file, fileContent))
        
        // Validate data class properties are documented
        findings.addAll(validateDataClassDocumentation(file, fileContent))
        
        // Validate sealed class subclasses are documented
        findings.addAll(validateSealedClassDocumentation(file, fileContent))
        
        return findings
    }
    
    override fun appliesTo(file: FileInfo): Boolean {
        // Apply to all Kotlin files except test files
        return file.relativePath.endsWith(".kt") && 
               !file.relativePath.contains("/test/") &&
               !file.relativePath.contains("/androidTest/")
    }

    
    /**
     * Validates that public API functions and classes have KDoc comments.
     * Requirement 12.1: Verify KDoc comments are present for public API.
     */
    private fun validatePublicApiDocumentation(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // Check for public class declarations
            if (isPublicClassDeclaration(line)) {
                val hasKDoc = hasKDocAbove(lines, i)
                if (!hasKDoc) {
                    val className = extractClassName(line)
                    findings.add(createMissingKDocFinding(
                        file,
                        i + 1,
                        "class",
                        className,
                        line
                    ))
                }
            }
            
            // Check for public function declarations
            if (isPublicFunctionDeclaration(line)) {
                val hasKDoc = hasKDocAbove(lines, i)
                if (!hasKDoc) {
                    val functionName = extractFunctionName(line)
                    // Skip test functions and overrides
                    if (!functionName.startsWith("test") && !line.contains("override")) {
                        findings.add(createMissingKDocFinding(
                            file,
                            i + 1,
                            "function",
                            functionName,
                            line
                        ))
                    }
                }
            }
            
            // Check for public interface declarations
            if (isPublicInterfaceDeclaration(line)) {
                val hasKDoc = hasKDocAbove(lines, i)
                if (!hasKDoc) {
                    val interfaceName = extractInterfaceName(line)
                    findings.add(createMissingKDocFinding(
                        file,
                        i + 1,
                        "interface",
                        interfaceName,
                        line
                    ))
                }
            }
            
            i++
        }
        
        return findings
    }
    
    /**
     * Validates that complex functions have inline comments explaining the logic.
     * Requirement 12.2: Verify inline comments explain complex algorithms.
     */
    private fun validateComplexFunctionComments(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // Check for function declarations
            if (isFunctionDeclaration(line)) {
                val functionName = extractFunctionName(line)
                val functionStartLine = i
                
                // Find function body
                val functionBody = extractFunctionBody(lines, i)
                val complexity = calculateCyclomaticComplexity(functionBody)
                
                // If function is complex, check for inline comments
                if (complexity > complexityThreshold) {
                    val hasInlineComments = hasInlineComments(functionBody)
                    
                    if (!hasInlineComments) {
                        findings.add(createMissingInlineCommentsFinding(
                            file,
                            functionStartLine + 1,
                            functionName,
                            complexity,
                            line
                        ))
                    }
                }
                
                // Skip to end of function
                i += functionBody.size
            }
            
            i++
        }
        
        return findings
    }
    
    /**
     * Validates that data class properties are documented.
     * Requirement 12.3: Verify data class properties are documented.
     */
    private fun validateDataClassDocumentation(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // Check for data class declarations
            if (line.startsWith("data class")) {
                val className = extractClassName(line)
                val hasKDoc = hasKDocAbove(lines, i)
                
                // Check if data class has properties in constructor
                if (line.contains("(")) {
                    val properties = extractDataClassProperties(lines, i)
                    
                    // If data class has multiple properties or complex properties, it should be documented
                    if (properties.size > 2 || hasComplexProperties(properties)) {
                        if (!hasKDoc || !hasPropertyDocumentation(lines, i, properties)) {
                            findings.add(createMissingDataClassDocumentationFinding(
                                file,
                                i + 1,
                                className,
                                properties,
                                line
                            ))
                        }
                    }
                }
            }
            
            i++
        }
        
        return findings
    }
    
    /**
     * Validates that sealed class subclasses are documented.
     * Requirement 12.4: Verify sealed class subclasses are documented.
     */
    private fun validateSealedClassDocumentation(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        var i = 0
        var inSealedClass = false
        var sealedClassName = ""
        
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // Check for sealed class declaration
            if (line.startsWith("sealed class") || line.startsWith("sealed interface")) {
                inSealedClass = true
                sealedClassName = extractClassName(line)
            }
            
            // Check for subclass declarations within sealed class scope
            if (inSealedClass && (line.startsWith("data class") || 
                                  line.startsWith("object") || 
                                  line.startsWith("class"))) {
                val subclassName = extractClassName(line)
                val hasKDoc = hasKDocAbove(lines, i)
                
                if (!hasKDoc) {
                    findings.add(createMissingSealedSubclassDocumentationFinding(
                        file,
                        i + 1,
                        sealedClassName,
                        subclassName,
                        line
                    ))
                }
            }
            
            // Exit sealed class scope when we find the closing brace at the same indentation level
            if (inSealedClass && line == "}") {
                inSealedClass = false
            }
            
            i++
        }
        
        return findings
    }

    
    private fun isPublicClassDeclaration(line: String): Boolean {
        return (line.startsWith("class ") || 
                line.startsWith("data class ") ||
                line.startsWith("sealed class ")) &&
               !line.contains("private") &&
               !line.contains("internal")
    }
    
    private fun isPublicFunctionDeclaration(line: String): Boolean {
        return line.contains("fun ") &&
               !line.contains("private") &&
               !line.contains("internal") &&
               !line.startsWith("//")
    }
    
    private fun isFunctionDeclaration(line: String): Boolean {
        return line.contains("fun ") && !line.startsWith("//")
    }
    
    private fun isPublicInterfaceDeclaration(line: String): Boolean {
        return line.startsWith("interface ") &&
               !line.contains("private") &&
               !line.contains("internal")
    }
    
    private fun hasKDocAbove(lines: List<String>, lineIndex: Int): Boolean {
        if (lineIndex == 0) return false
        
        var i = lineIndex - 1
        
        // Skip empty lines and annotations
        while (i >= 0 && (lines[i].trim().isEmpty() || lines[i].trim().startsWith("@"))) {
            i--
        }
        
        // Check if we found a KDoc comment
        if (i >= 0) {
            val line = lines[i].trim()
            return line == "*/" && hasKDocStart(lines, i)
        }
        
        return false
    }
    
    private fun hasKDocStart(lines: List<String>, endIndex: Int): Boolean {
        var i = endIndex - 1
        while (i >= 0) {
            val line = lines[i].trim()
            if (line.startsWith("/**")) {
                return true
            }
            if (!line.startsWith("*") && line != "*/") {
                return false
            }
            i--
        }
        return false
    }
    
    private fun extractClassName(line: String): String {
        val patterns = listOf(
            Regex("""(?:data\s+)?(?:sealed\s+)?class\s+(\w+)"""),
            Regex("""object\s+(\w+)"""),
            Regex("""enum\s+class\s+(\w+)""")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(line)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        
        return "Unknown"
    }
    
    private fun extractFunctionName(line: String): String {
        val pattern = Regex("""fun\s+(\w+)""")
        val match = pattern.find(line)
        return match?.groupValues?.get(1) ?: "Unknown"
    }
    
    private fun extractInterfaceName(line: String): String {
        val pattern = Regex("""interface\s+(\w+)""")
        val match = pattern.find(line)
        return match?.groupValues?.get(1) ?: "Unknown"
    }
    
    private fun extractFunctionBody(lines: List<String>, startIndex: Int): List<String> {
        val body = mutableListOf<String>()
        var braceCount = 0
        var foundOpenBrace = false
        
        for (i in startIndex until lines.size) {
            val line = lines[i]
            body.add(line)
            
            braceCount += line.count { it == '{' }
            if (braceCount > 0) foundOpenBrace = true
            
            braceCount -= line.count { it == '}' }
            
            if (foundOpenBrace && braceCount == 0) {
                break
            }
        }
        
        return body
    }
    
    private fun calculateCyclomaticComplexity(functionBody: List<String>): Int {
        var complexity = 1 // Base complexity
        
        for (line in functionBody) {
            val trimmed = line.trim()
            
            // Count decision points
            complexity += trimmed.split(Regex("\\bif\\b")).size - 1
            complexity += trimmed.split(Regex("\\bwhen\\b")).size - 1
            complexity += trimmed.split(Regex("\\bfor\\b")).size - 1
            complexity += trimmed.split(Regex("\\bwhile\\b")).size - 1
            complexity += trimmed.split("&&").size - 1
            complexity += trimmed.split("||").size - 1
            complexity += trimmed.split(Regex("\\?:")).size - 1
            complexity += trimmed.split(Regex("\\bcatch\\b")).size - 1
        }
        
        return complexity
    }
    
    private fun hasInlineComments(functionBody: List<String>): Boolean {
        return functionBody.any { line ->
            val trimmed = line.trim()
            trimmed.startsWith("//") || trimmed.contains("// ")
        }
    }
    
    private fun extractDataClassProperties(lines: List<String>, startIndex: Int): List<String> {
        val properties = mutableListOf<String>()
        var inConstructor = false
        
        for (i in startIndex until lines.size) {
            val line = lines[i]
            
            if (line.contains("(")) inConstructor = true
            if (!inConstructor) continue
            
            // Match property declarations in constructor
            val pattern = Regex("""(?:val|var)\s+(\w+)""")
            val matches = pattern.findAll(line)
            properties.addAll(matches.map { it.groupValues[1] })
            
            if (line.contains(")")) break
        }
        
        return properties
    }
    
    private fun hasComplexProperties(properties: List<String>): Boolean {
        return properties.isNotEmpty()
    }
    
    private fun hasPropertyDocumentation(lines: List<String>, lineIndex: Int, properties: List<String>): Boolean {
        if (lineIndex == 0) return false
        
        var i = lineIndex - 1
        val kdocLines = mutableListOf<String>()
        
        // Collect KDoc lines
        while (i >= 0) {
            val line = lines[i].trim()
            if (line.startsWith("/**")) break
            kdocLines.add(line)
            i--
        }
        
        // Check if at least some properties are mentioned in KDoc
        val kdocText = kdocLines.joinToString(" ")
        return properties.any { property ->
            kdocText.contains("@property $property") || kdocText.contains(property)
        }
    }

    
    private fun createMissingKDocFinding(
        file: FileInfo,
        lineNumber: Int,
        elementType: String,
        elementName: String,
        code: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.LOW,
            title = "Missing KDoc Documentation for Public $elementType",
            description = "Public $elementType '$elementName' is missing KDoc documentation. Public APIs " +
                    "should be documented to help other developers understand their purpose, parameters, " +
                    "return values, and usage examples.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = code.take(100),
            recommendation = "Add KDoc documentation above the $elementType declaration. Include a " +
                    "description of what the $elementType does, document all parameters with @param, " +
                    "document the return value with @return (if applicable), and consider adding usage " +
                    "examples for complex APIs.",
            beforeExample = code.take(100),
            afterExample = """
                /**
                 * Brief description of what this $elementType does.
                 * 
                 * @param paramName Description of parameter
                 * @return Description of return value
                 */
                ${code.take(100)}
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://kotlinlang.org/docs/kotlin-doc.html",
                "https://developer.android.com/kotlin/style-guide#documentation"
            )
        )
    }
    
    private fun createMissingInlineCommentsFinding(
        file: FileInfo,
        lineNumber: Int,
        functionName: String,
        complexity: Int,
        code: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Complex Function Missing Inline Comments",
            description = "Function '$functionName' has high cyclomatic complexity ($complexity) but lacks " +
                    "inline comments explaining the logic. Complex functions should have comments that " +
                    "explain the algorithm, decision points, and any non-obvious logic to aid future " +
                    "maintainers.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = code.take(100),
            recommendation = "Add inline comments throughout the function to explain:\n" +
                    "- The overall algorithm or approach\n" +
                    "- Why specific decisions are made (if/when branches)\n" +
                    "- Any non-obvious logic or edge cases\n" +
                    "- Complex calculations or transformations\n" +
                    "Consider refactoring if the function is too complex (complexity > 15).",
            beforeExample = "fun $functionName() {\n    // Complex logic without comments\n}",
            afterExample = "fun $functionName() {\n    // Explain what this section does\n    // Complex logic with helpful comments\n}",
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://kotlinlang.org/docs/coding-conventions.html#documentation",
                "Project docs: Code complexity guidelines"
            )
        )
    }
    
    private fun createMissingDataClassDocumentationFinding(
        file: FileInfo,
        lineNumber: Int,
        className: String,
        properties: List<String>,
        code: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.LOW,
            title = "Data Class Properties Not Documented",
            description = "Data class '$className' has ${properties.size} properties but lacks proper " +
                    "documentation. Data classes with multiple properties should document the purpose of " +
                    "each property, especially if they represent domain concepts or have specific constraints.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = code.take(100),
            recommendation = "Add KDoc documentation with @property tags for each property:\n" +
                    properties.joinToString("\n") { " * @property $it Description of $it" } +
                    "\n\nThis helps other developers understand the purpose and constraints of each property.",
            beforeExample = "data class $className(...)",
            afterExample = """
                /**
                 * Description of what this data class represents.
                 * 
                ${properties.joinToString("\n") { " * @property $it Description of $it" }}
                 */
                data class $className(...)
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://kotlinlang.org/docs/kotlin-doc.html",
                "https://developer.android.com/kotlin/style-guide#documentation"
            )
        )
    }
    
    private fun createMissingSealedSubclassDocumentationFinding(
        file: FileInfo,
        lineNumber: Int,
        sealedClassName: String,
        subclassName: String,
        code: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.LOW,
            title = "Sealed Class Subclass Not Documented",
            description = "Subclass '$subclassName' of sealed class '$sealedClassName' is missing " +
                    "documentation. Each subclass of a sealed class should be documented to explain " +
                    "what state or case it represents and when it should be used.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = code.take(100),
            recommendation = "Add KDoc documentation above the subclass declaration explaining:\n" +
                    "- What state or case this subclass represents\n" +
                    "- When this state occurs\n" +
                    "- Any data it carries and what it means\n" +
                    "This is especially important for UI state classes and result types.",
            beforeExample = code.take(100),
            afterExample = """
                /**
                 * Represents the [state/case] when [condition].
                 * 
                 * @property data Description of any data carried
                 */
                ${code.take(100)}
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://kotlinlang.org/docs/kotlin-doc.html",
                "https://kotlinlang.org/docs/sealed-classes.html"
            )
        )
    }
}
