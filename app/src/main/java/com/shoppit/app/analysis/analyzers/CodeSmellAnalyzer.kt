package com.shoppit.app.analysis.analyzers

import com.shoppit.app.analysis.core.CodeAnalyzer
import com.shoppit.app.analysis.models.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.util.*

/**
 * Analyzer that detects code smells and anti-patterns in Kotlin code.
 * 
 * Detects:
 * - Long functions (> 50 lines)
 * - Large classes (> 300 lines)
 * - High cyclomatic complexity (> 15)
 * - Deep nesting (> 4 levels)
 * - Too many parameters (> 5)
 */
class CodeSmellAnalyzer : CodeAnalyzer {
    
    override val id: String = "code-smell"
    override val name: String = "Code Smell Analyzer"
    override val category: AnalysisCategory = AnalysisCategory.CODE_SMELL
    
    companion object {
        private const val MAX_FUNCTION_LINES = 50
        private const val MAX_CLASS_LINES = 300
        private const val MAX_COMPLEXITY = 15
        private const val MAX_NESTING_DEPTH = 4
        private const val MAX_PARAMETERS = 5
    }
    
    override suspend fun analyze(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Parse the file to AST
        val ktFile = parseKotlinFile(fileContent, file.path) ?: return findings
        
        // Create visitor to traverse AST
        ktFile.accept(object : KtTreeVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                super.visitNamedFunction(function)
                
                // Check function length
                checkFunctionLength(function, file, findings)
                
                // Check parameter count
                checkParameterCount(function, file, findings)
                
                // Check cyclomatic complexity
                checkComplexity(function, file, findings)
                
                // Check nesting depth
                checkNestingDepth(function, file, findings)
            }
            
            override fun visitClass(klass: KtClass) {
                super.visitClass(klass)
                
                // Check class size
                checkClassSize(klass, file, findings)
            }
        })
        
        return findings
    }
    
    override fun appliesTo(file: FileInfo): Boolean {
        // Apply to all Kotlin files except test files
        return file.path.endsWith(".kt") && file.layer != CodeLayer.TEST
    }
    
    /**
     * Checks if a function exceeds the maximum line count.
     */
    private fun checkFunctionLength(
        function: KtNamedFunction,
        file: FileInfo,
        findings: MutableList<Finding>
    ) {
        val lineCount = function.text.lines().size
        
        if (lineCount > MAX_FUNCTION_LINES) {
            val functionName = function.name ?: "anonymous"
            val lineNumber = getLineNumber(function, file)
            
            findings.add(
                Finding(
                    id = "code-smell-long-function-${file.relativePath}-$lineNumber",
                    analyzer = id,
                    category = category,
                    priority = Priority.MEDIUM,
                    title = "Long Function: $functionName",
                    description = "Function '$functionName' has $lineCount lines, exceeding the recommended maximum of $MAX_FUNCTION_LINES lines. " +
                            "Long functions are harder to understand, test, and maintain.",
                    file = file.relativePath,
                    lineNumber = lineNumber,
                    codeSnippet = getCodeSnippet(function),
                    recommendation = "Consider breaking this function into smaller, more focused functions. " +
                            "Each function should have a single responsibility. " +
                            "Extract logical blocks into separate private functions with descriptive names.",
                    beforeExample = """
                        fun processOrder(order: Order) {
                            // 60+ lines of code
                            validateOrder(order)
                            calculateTotals(order)
                            applyDiscounts(order)
                            processPayment(order)
                            updateInventory(order)
                            sendConfirmation(order)
                        }
                    """.trimIndent(),
                    afterExample = """
                        fun processOrder(order: Order) {
                            validateOrder(order)
                            val totals = calculateOrderTotals(order)
                            val finalAmount = applyDiscounts(totals)
                            processPayment(order, finalAmount)
                            updateInventory(order)
                            sendOrderConfirmation(order)
                        }
                        
                        private fun calculateOrderTotals(order: Order): OrderTotals { ... }
                        private fun applyDiscounts(totals: OrderTotals): Money { ... }
                    """.trimIndent(),
                    effort = Effort.MEDIUM,
                    references = listOf(
                        "https://refactoring.guru/smells/long-method",
                        "https://kotlinlang.org/docs/coding-conventions.html#functions"
                    )
                )
            )
        }
    }
    
    /**
     * Checks if a class exceeds the maximum line count.
     */
    private fun checkClassSize(
        klass: KtClass,
        file: FileInfo,
        findings: MutableList<Finding>
    ) {
        val lineCount = klass.text.lines().size
        
        if (lineCount > MAX_CLASS_LINES) {
            val className = klass.name ?: "anonymous"
            val lineNumber = getLineNumber(klass, file)
            
            findings.add(
                Finding(
                    id = "code-smell-large-class-${file.relativePath}-$lineNumber",
                    analyzer = id,
                    category = category,
                    priority = Priority.MEDIUM,
                    title = "Large Class: $className",
                    description = "Class '$className' has $lineCount lines, exceeding the recommended maximum of $MAX_CLASS_LINES lines. " +
                            "Large classes often violate the Single Responsibility Principle and are difficult to maintain.",
                    file = file.relativePath,
                    lineNumber = lineNumber,
                    codeSnippet = getCodeSnippet(klass, maxLines = 20),
                    recommendation = "Consider breaking this class into smaller, more focused classes. " +
                            "Look for groups of related methods and properties that can be extracted into separate classes. " +
                            "Apply the Single Responsibility Principle - each class should have one reason to change.",
                    beforeExample = """
                        class OrderManager {
                            // 350+ lines
                            // Order validation
                            // Payment processing
                            // Inventory management
                            // Email notifications
                            // Reporting
                        }
                    """.trimIndent(),
                    afterExample = """
                        class OrderManager(
                            private val validator: OrderValidator,
                            private val paymentProcessor: PaymentProcessor,
                            private val inventoryManager: InventoryManager,
                            private val notificationService: NotificationService
                        ) {
                            fun processOrder(order: Order) {
                                validator.validate(order)
                                paymentProcessor.process(order)
                                inventoryManager.update(order)
                                notificationService.sendConfirmation(order)
                            }
                        }
                    """.trimIndent(),
                    effort = Effort.LARGE,
                    references = listOf(
                        "https://refactoring.guru/smells/large-class",
                        "https://en.wikipedia.org/wiki/Single-responsibility_principle"
                    )
                )
            )
        }
    }
    
    /**
     * Checks if a function has too many parameters.
     */
    private fun checkParameterCount(
        function: KtNamedFunction,
        file: FileInfo,
        findings: MutableList<Finding>
    ) {
        val paramCount = function.valueParameters.size
        
        if (paramCount > MAX_PARAMETERS) {
            val functionName = function.name ?: "anonymous"
            val lineNumber = getLineNumber(function, file)
            
            findings.add(
                Finding(
                    id = "code-smell-too-many-params-${file.relativePath}-$lineNumber",
                    analyzer = id,
                    category = category,
                    priority = Priority.LOW,
                    title = "Too Many Parameters: $functionName",
                    description = "Function '$functionName' has $paramCount parameters, exceeding the recommended maximum of $MAX_PARAMETERS. " +
                            "Functions with many parameters are harder to understand and use.",
                    file = file.relativePath,
                    lineNumber = lineNumber,
                    codeSnippet = getCodeSnippet(function, maxLines = 5),
                    recommendation = "Consider grouping related parameters into a data class or parameter object. " +
                            "This makes the function signature cleaner and the parameters easier to manage.",
                    beforeExample = """
                        fun createUser(
                            firstName: String,
                            lastName: String,
                            email: String,
                            phone: String,
                            address: String,
                            city: String,
                            zipCode: String
                        ): User
                    """.trimIndent(),
                    afterExample = """
                        data class UserInfo(
                            val firstName: String,
                            val lastName: String,
                            val email: String,
                            val phone: String,
                            val address: Address
                        )
                        
                        data class Address(
                            val street: String,
                            val city: String,
                            val zipCode: String
                        )
                        
                        fun createUser(userInfo: UserInfo): User
                    """.trimIndent(),
                    effort = Effort.SMALL,
                    references = listOf(
                        "https://refactoring.guru/smells/long-parameter-list",
                        "https://kotlinlang.org/docs/coding-conventions.html#functions"
                    )
                )
            )
        }
    }
    
    /**
     * Calculates and checks the cyclomatic complexity of a function.
     */
    private fun checkComplexity(
        function: KtNamedFunction,
        file: FileInfo,
        findings: MutableList<Finding>
    ) {
        val complexity = calculateComplexity(function)
        
        if (complexity > MAX_COMPLEXITY) {
            val functionName = function.name ?: "anonymous"
            val lineNumber = getLineNumber(function, file)
            
            findings.add(
                Finding(
                    id = "code-smell-high-complexity-${file.relativePath}-$lineNumber",
                    analyzer = id,
                    category = category,
                    priority = Priority.MEDIUM,
                    title = "High Cyclomatic Complexity: $functionName",
                    description = "Function '$functionName' has a cyclomatic complexity of $complexity, exceeding the recommended maximum of $MAX_COMPLEXITY. " +
                            "High complexity indicates the function has too many decision points, making it difficult to test and understand.",
                    file = file.relativePath,
                    lineNumber = lineNumber,
                    codeSnippet = getCodeSnippet(function),
                    recommendation = "Simplify the function by extracting complex conditions into separate functions with descriptive names. " +
                            "Consider using early returns to reduce nesting. " +
                            "Replace complex conditional logic with polymorphism or strategy pattern where appropriate.",
                    beforeExample = """
                        fun calculateDiscount(order: Order): Double {
                            if (order.total > 100) {
                                if (order.customer.isPremium) {
                                    if (order.items.size > 5) {
                                        return 0.25
                                    } else {
                                        return 0.15
                                    }
                                } else {
                                    return 0.10
                                }
                            } else {
                                return 0.0
                            }
                        }
                    """.trimIndent(),
                    afterExample = """
                        fun calculateDiscount(order: Order): Double {
                            if (order.total <= 100) return 0.0
                            
                            return when {
                                order.customer.isPremium && order.items.size > 5 -> 0.25
                                order.customer.isPremium -> 0.15
                                else -> 0.10
                            }
                        }
                    """.trimIndent(),
                    effort = Effort.MEDIUM,
                    references = listOf(
                        "https://en.wikipedia.org/wiki/Cyclomatic_complexity",
                        "https://refactoring.guru/refactoring/techniques/simplifying-conditional-expressions"
                    )
                )
            )
        }
    }
    
    /**
     * Checks the nesting depth of code blocks in a function.
     */
    private fun checkNestingDepth(
        function: KtNamedFunction,
        file: FileInfo,
        findings: MutableList<Finding>
    ) {
        val maxDepth = calculateMaxNestingDepth(function)
        
        if (maxDepth > MAX_NESTING_DEPTH) {
            val functionName = function.name ?: "anonymous"
            val lineNumber = getLineNumber(function, file)
            
            findings.add(
                Finding(
                    id = "code-smell-deep-nesting-${file.relativePath}-$lineNumber",
                    analyzer = id,
                    category = category,
                    priority = Priority.MEDIUM,
                    title = "Deep Nesting: $functionName",
                    description = "Function '$functionName' has a maximum nesting depth of $maxDepth, exceeding the recommended maximum of $MAX_NESTING_DEPTH. " +
                            "Deeply nested code is harder to read and understand.",
                    file = file.relativePath,
                    lineNumber = lineNumber,
                    codeSnippet = getCodeSnippet(function),
                    recommendation = "Reduce nesting by using early returns (guard clauses) to handle edge cases first. " +
                            "Extract nested blocks into separate functions. " +
                            "Consider inverting conditions to reduce nesting levels.",
                    beforeExample = """
                        fun processOrder(order: Order?) {
                            if (order != null) {
                                if (order.isValid()) {
                                    if (order.customer != null) {
                                        if (order.customer.hasCredit()) {
                                            if (order.items.isNotEmpty()) {
                                                // Process order
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    """.trimIndent(),
                    afterExample = """
                        fun processOrder(order: Order?) {
                            if (order == null) return
                            if (!order.isValid()) return
                            if (order.customer == null) return
                            if (!order.customer.hasCredit()) return
                            if (order.items.isEmpty()) return
                            
                            // Process order
                        }
                    """.trimIndent(),
                    effort = Effort.SMALL,
                    references = listOf(
                        "https://refactoring.guru/refactoring/techniques/simplifying-conditional-expressions/replace-nested-conditional-with-guard-clauses",
                        "https://kotlinlang.org/docs/coding-conventions.html#control-flow-statements"
                    )
                )
            )
        }
    }
    
    /**
     * Calculates the cyclomatic complexity of a function.
     * Complexity = 1 + number of decision points (if, when, for, while, &&, ||, ?:, catch)
     */
    private fun calculateComplexity(function: KtNamedFunction): Int {
        var complexity = 1 // Base complexity
        
        function.accept(object : KtTreeVisitorVoid() {
            override fun visitIfExpression(expression: KtIfExpression) {
                super.visitIfExpression(expression)
                complexity++
            }
            
            override fun visitWhenExpression(expression: KtWhenExpression) {
                super.visitWhenExpression(expression)
                // Each when entry adds to complexity
                complexity += expression.entries.size
            }
            
            override fun visitForExpression(expression: KtForExpression) {
                super.visitForExpression(expression)
                complexity++
            }
            
            override fun visitWhileExpression(expression: KtWhileExpression) {
                super.visitWhileExpression(expression)
                complexity++
            }
            
            override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
                super.visitDoWhileExpression(expression)
                complexity++
            }
            
            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                super.visitBinaryExpression(expression)
                // Count logical operators (&&, ||)
                val operationToken = expression.operationToken.toString()
                if (operationToken == "ANDAND" || operationToken == "OROR") {
                    complexity++
                }
            }
            
            override fun visitCatchSection(catchClause: KtCatchClause) {
                super.visitCatchSection(catchClause)
                complexity++
            }
        })
        
        return complexity
    }
    
    /**
     * Calculates the maximum nesting depth in a function.
     */
    private fun calculateMaxNestingDepth(function: KtNamedFunction): Int {
        var maxDepth = 0
        var currentDepth = 0
        
        function.accept(object : KtTreeVisitorVoid() {
            override fun visitBlockExpression(expression: KtBlockExpression) {
                currentDepth++
                maxDepth = maxOf(maxDepth, currentDepth)
                super.visitBlockExpression(expression)
                currentDepth--
            }
            
            override fun visitIfExpression(expression: KtIfExpression) {
                currentDepth++
                maxDepth = maxOf(maxDepth, currentDepth)
                super.visitIfExpression(expression)
                currentDepth--
            }
            
            override fun visitWhenExpression(expression: KtWhenExpression) {
                currentDepth++
                maxDepth = maxOf(maxDepth, currentDepth)
                super.visitWhenExpression(expression)
                currentDepth--
            }
            
            override fun visitForExpression(expression: KtForExpression) {
                currentDepth++
                maxDepth = maxOf(maxDepth, currentDepth)
                super.visitForExpression(expression)
                currentDepth--
            }
            
            override fun visitWhileExpression(expression: KtWhileExpression) {
                currentDepth++
                maxDepth = maxOf(maxDepth, currentDepth)
                super.visitWhileExpression(expression)
                currentDepth--
            }
        })
        
        return maxDepth
    }
    
    /**
     * Parses Kotlin source code into a KtFile AST.
     */
    private fun parseKotlinFile(content: String, fileName: String): KtFile? {
        return try {
            val disposable = Disposer.newDisposable()
            try {
                val configuration = CompilerConfiguration()
                val environment = KotlinCoreEnvironment.createForProduction(
                    disposable,
                    configuration,
                    EnvironmentConfigFiles.JVM_CONFIG_FILES
                )
                
                val psiFactory = KtPsiFactory(environment.project)
                psiFactory.createFile(fileName, content)
            } finally {
                Disposer.dispose(disposable)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Gets the line number of a PSI element in the file.
     */
    private fun getLineNumber(element: PsiElement, file: FileInfo): Int {
        val text = element.containingFile.text
        val offset = element.startOffset
        return text.substring(0, offset).count { it == '\n' } + 1
    }
    
    /**
     * Extracts a code snippet from a PSI element.
     */
    private fun getCodeSnippet(element: PsiElement, maxLines: Int = 10): String {
        val lines = element.text.lines()
        return if (lines.size <= maxLines) {
            element.text
        } else {
            lines.take(maxLines).joinToString("\n") + "\n// ... (${lines.size - maxLines} more lines)"
        }
    }
}
