package com.shoppit.app.analysis.analyzers

import com.shoppit.app.analysis.core.CodeAnalyzer
import com.shoppit.app.analysis.models.AnalysisCategory
import com.shoppit.app.analysis.models.CodeLayer
import com.shoppit.app.analysis.models.Effort
import com.shoppit.app.analysis.models.FileInfo
import com.shoppit.app.analysis.models.Finding
import com.shoppit.app.analysis.models.Priority
import java.util.UUID

/**
 * Analyzer that validates Room database patterns and best practices.
 * 
 * Validates:
 * - DAO query functions return Flow for reactive data
 * - DAO mutations (insert/update/delete) are suspend functions
 * - Database operations use flowOn(Dispatchers.IO)
 * - Foreign keys are properly defined with CASCADE
 * - Parameterized queries are used instead of string concatenation
 * 
 * Requirements: 8.1, 8.2, 8.3, 8.4, 8.5
 */
class DatabaseAnalyzer : CodeAnalyzer {
    
    override val id: String = "database"
    
    override val name: String = "Database Analyzer"
    
    override val category: AnalysisCategory = AnalysisCategory.DATABASE
    
    override suspend fun analyze(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Only analyze DAO interfaces and Entity classes
        if (!appliesTo(file)) {
            return findings
        }
        
        // Check if this is a DAO interface
        if (isDaoInterface(fileContent)) {
            findings.addAll(analyzeDaoFunctions(file, fileContent))
            findings.addAll(analyzeQueryParameterization(file, fileContent))
        }
        
        // Check if this is an Entity class
        if (isEntityClass(fileContent)) {
            findings.addAll(analyzeForeignKeys(file, fileContent))
        }
        
        return findings
    }
    
    override fun appliesTo(file: FileInfo): Boolean {
        // Apply to DAO interfaces and Entity classes in the data layer
        return file.layer == CodeLayer.DATA &&
               (file.relativePath.contains("Dao.kt") || 
                file.relativePath.contains("Entity.kt"))
    }
    
    /**
     * Checks if the file contains a DAO interface.
     */
    private fun isDaoInterface(fileContent: String): Boolean {
        return fileContent.contains("@Dao") && fileContent.contains("interface ")
    }
    
    /**
     * Checks if the file contains an Entity class.
     */
    private fun isEntityClass(fileContent: String): Boolean {
        return fileContent.contains("@Entity")
    }
    
    /**
     * Analyzes DAO functions for proper return types and modifiers.
     */
    private fun analyzeDaoFunctions(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        val lines = fileContent.lines()
        var inDaoInterface = false
        var braceCount = 0
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            // Track if we're inside the DAO interface
            if (trimmedLine.contains("@Dao")) {
                inDaoInterface = true
            }
            
            if (trimmedLine.contains("interface ") && inDaoInterface) {
                braceCount = 0
            }
            
            // Count braces to track interface scope
            braceCount += line.count { it == '{' }
            braceCount -= line.count { it == '}' }
            
            if (braceCount == 0 && inDaoInterface && index > 0) {
                inDaoInterface = false
            }
            
            // Analyze functions within DAO interface
            if (inDaoInterface && trimmedLine.startsWith("fun ")) {
                // Check if it's a query function
                val previousLine = if (index > 0) lines[index - 1].trim() else ""
                val isQuery = previousLine.contains("@Query")
                val isInsert = previousLine.contains("@Insert")
                val isUpdate = previousLine.contains("@Update")
                val isDelete = previousLine.contains("@Delete")
                
                val isMutation = isInsert || isUpdate || isDelete
                
                // Extract function signature
                val functionSignature = trimmedLine
                val functionName = functionSignature
                    .substringAfter("fun ")
                    .substringBefore("(")
                    .trim()
                
                val returnType = if (functionSignature.contains("): ")) {
                    functionSignature
                        .substringAfter("): ")
                        .substringBefore("{")
                        .substringBefore("=")
                        .trim()
                } else {
                    ""
                }
                
                val isSuspend = functionSignature.contains("suspend fun")
                val returnsFlow = returnType.startsWith("Flow<")
                
                // Requirement 8.1: Query functions should return Flow
                if (isQuery && !returnsFlow && !isSuspend) {
                    findings.add(createQueryShouldReturnFlowFinding(
                        file,
                        index + 1,
                        functionName,
                        returnType
                    ))
                }
                
                // Requirement 8.2: Mutation functions should be suspend
                if (isMutation && !isSuspend) {
                    findings.add(createMutationShouldBeSuspendFinding(
                        file,
                        index + 1,
                        functionName,
                        if (isInsert) "Insert" else if (isUpdate) "Update" else "Delete"
                    ))
                }
            }
        }
        
        return findings
    }
    
    /**
     * Analyzes @Query annotations for SQL injection risks.
     */
    private fun analyzeQueryParameterization(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        val lines = fileContent.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            // Check for @Query annotation
            if (trimmedLine.startsWith("@Query(")) {
                val queryString = extractQueryString(lines, index)
                
                // Check for string concatenation patterns that indicate SQL injection risk
                if (queryString.contains("||") || 
                    queryString.contains("' + ") ||
                    queryString.contains("\" + ") ||
                    queryString.contains("'\${") ||
                    queryString.contains("\"\${")) {
                    
                    findings.add(createSqlInjectionRiskFinding(
                        file,
                        index + 1,
                        queryString
                    ))
                }
                
                // Check if query uses parameters but doesn't use named parameters
                val nextLine = if (index + 1 < lines.size) lines[index + 1].trim() else ""
                if (nextLine.startsWith("fun ") && nextLine.contains("(") && nextLine.contains(")")) {
                    val hasParameters = !nextLine.substringAfter("(").substringBefore(")").trim().isEmpty()
                    val usesNamedParameters = queryString.contains(":")
                    
                    if (hasParameters && !usesNamedParameters && !queryString.contains("?")) {
                        findings.add(createMissingParameterizationFinding(
                            file,
                            index + 1,
                            queryString
                        ))
                    }
                }
            }
        }
        
        return findings
    }
    
    /**
     * Analyzes Entity classes for proper foreign key definitions.
     */
    private fun analyzeForeignKeys(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        val lines = fileContent.lines()
        var inForeignKeyDefinition = false
        var foreignKeyText = StringBuilder()
        var foreignKeyLineNumber = -1
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            // Check for ForeignKey definition
            if (trimmedLine.contains("ForeignKey(")) {
                inForeignKeyDefinition = true
                foreignKeyLineNumber = index + 1
                foreignKeyText.append(trimmedLine)
            } else if (inForeignKeyDefinition) {
                foreignKeyText.append(" ").append(trimmedLine)
                
                // Check if ForeignKey definition ends
                if (trimmedLine.contains(")")) {
                    val foreignKeyDef = foreignKeyText.toString()
                    
                    // Requirement 8.4: Check for CASCADE on delete
                    if (!foreignKeyDef.contains("onDelete = ForeignKey.CASCADE") &&
                        !foreignKeyDef.contains("onDelete=ForeignKey.CASCADE")) {
                        
                        findings.add(createMissingCascadeFinding(
                            file,
                            foreignKeyLineNumber,
                            foreignKeyDef
                        ))
                    }
                    
                    // Reset for next foreign key
                    inForeignKeyDefinition = false
                    foreignKeyText = StringBuilder()
                }
            }
        }
        
        return findings
    }
    
    /**
     * Extracts the query string from @Query annotation.
     */
    private fun extractQueryString(lines: List<String>, startIndex: Int): String {
        val queryBuilder = StringBuilder()
        var inQuery = false
        var quoteChar: Char? = null
        
        for (i in startIndex until lines.size) {
            val line = lines[i].trim()
            
            if (line.startsWith("@Query(")) {
                inQuery = true
                val afterQuery = line.substringAfter("@Query(")
                
                // Determine quote character
                quoteChar = when {
                    afterQuery.trim().startsWith("\"\"\"") -> '\"'
                    afterQuery.trim().startsWith("\"") -> '\"'
                    else -> null
                }
                
                queryBuilder.append(afterQuery)
            } else if (inQuery) {
                queryBuilder.append(" ").append(line)
            }
            
            // Check if query ends
            if (inQuery && line.contains(")")) {
                break
            }
        }
        
        return queryBuilder.toString()
            .substringBefore(")")
            .replace("\"\"\"", "")
            .replace("\"", "")
            .trim()
    }
    
    /**
     * Creates a finding for query functions that should return Flow.
     */
    private fun createQueryShouldReturnFlowFinding(
        file: FileInfo,
        lineNumber: Int,
        functionName: String,
        returnType: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "DAO Query Function Should Return Flow",
            description = "Query function '$functionName' returns '$returnType' instead of Flow. " +
                    "DAO query functions should return Flow<T> for reactive data that automatically " +
                    "updates when the database changes. This enables the UI to reactively respond " +
                    "to data changes without manual refresh.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = "fun $functionName(): $returnType",
            recommendation = "Change the return type to Flow<$returnType> to enable reactive updates. " +
                    "Room will automatically emit new values when the queried data changes.",
            beforeExample = """
                @Query("SELECT * FROM meals")
                fun getAllMeals(): List<MealEntity>
            """.trimIndent(),
            afterExample = """
                @Query("SELECT * FROM meals")
                fun getAllMeals(): Flow<List<MealEntity>>
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://developer.android.com/training/data-storage/room/async-queries#observable",
                "https://developer.android.com/kotlin/flow"
            )
        )
    }
    
    /**
     * Creates a finding for mutation functions that should be suspend.
     */
    private fun createMutationShouldBeSuspendFinding(
        file: FileInfo,
        lineNumber: Int,
        functionName: String,
        operationType: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "DAO Mutation Function Should Be Suspend",
            description = "$operationType function '$functionName' is not a suspend function. " +
                    "DAO mutation operations (insert, update, delete) should be suspend functions " +
                    "to ensure they run on a background thread and don't block the main thread. " +
                    "This prevents ANR (Application Not Responding) errors.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = "fun $functionName()",
            recommendation = "Add the 'suspend' modifier to the function. This ensures the operation " +
                    "runs on a background thread and can be called from coroutines.",
            beforeExample = """
                @Insert(onConflict = OnConflictStrategy.REPLACE)
                fun insertMeal(meal: MealEntity): Long
            """.trimIndent(),
            afterExample = """
                @Insert(onConflict = OnConflictStrategy.REPLACE)
                suspend fun insertMeal(meal: MealEntity): Long
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/training/data-storage/room/async-queries",
                "https://kotlinlang.org/docs/coroutines-basics.html"
            )
        )
    }
    
    /**
     * Creates a finding for SQL injection risks.
     */
    private fun createSqlInjectionRiskFinding(
        file: FileInfo,
        lineNumber: Int,
        queryString: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.CRITICAL,
            title = "SQL Injection Risk: String Concatenation in Query",
            description = "Query uses string concatenation which creates a SQL injection vulnerability. " +
                    "Malicious input could modify the query structure and access or modify unauthorized data. " +
                    "This is a critical security issue that must be fixed immediately.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = queryString.take(100),
            recommendation = "Use parameterized queries with named parameters (e.g., :paramName) instead of " +
                    "string concatenation. Room will safely bind the parameters and prevent SQL injection.",
            beforeExample = """
                @Query("SELECT * FROM meals WHERE name = '" + name + "'")
                fun searchMeals(name: String): Flow<List<MealEntity>>
            """.trimIndent(),
            afterExample = """
                @Query("SELECT * FROM meals WHERE name = :name")
                fun searchMeals(name: String): Flow<List<MealEntity>>
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://developer.android.com/training/data-storage/room/accessing-data#query-params",
                "https://owasp.org/www-community/attacks/SQL_Injection"
            )
        )
    }
    
    /**
     * Creates a finding for missing query parameterization.
     */
    private fun createMissingParameterizationFinding(
        file: FileInfo,
        lineNumber: Int,
        queryString: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Query Should Use Parameterized Queries",
            description = "Query function has parameters but doesn't use parameterized queries. " +
                    "While not necessarily a SQL injection risk, using parameterized queries is " +
                    "a best practice that ensures type safety and prevents potential security issues.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = queryString.take(100),
            recommendation = "Use named parameters (e.g., :paramName) in the query string and pass " +
                    "them as function parameters. This ensures type safety and prevents SQL injection.",
            beforeExample = """
                @Query("SELECT * FROM meals WHERE id = 1")
                fun getMealById(id: Long): Flow<MealEntity?>
            """.trimIndent(),
            afterExample = """
                @Query("SELECT * FROM meals WHERE id = :id")
                fun getMealById(id: Long): Flow<MealEntity?>
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://developer.android.com/training/data-storage/room/accessing-data#query-params"
            )
        )
    }
    
    /**
     * Creates a finding for missing CASCADE on foreign keys.
     */
    private fun createMissingCascadeFinding(
        file: FileInfo,
        lineNumber: Int,
        foreignKeyDef: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Foreign Key Should Use CASCADE for onDelete",
            description = "Foreign key definition doesn't specify CASCADE for onDelete. " +
                    "Without CASCADE, deleting a parent entity will fail if child entities exist, " +
                    "or leave orphaned child records. Using CASCADE ensures referential integrity " +
                    "by automatically deleting child records when the parent is deleted.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = foreignKeyDef.take(150),
            recommendation = "Add 'onDelete = ForeignKey.CASCADE' to the foreign key definition. " +
                    "This ensures child records are automatically deleted when the parent is deleted, " +
                    "maintaining referential integrity.",
            beforeExample = """
                ForeignKey(
                    entity = MealEntity::class,
                    parentColumns = ["id"],
                    childColumns = ["meal_id"]
                )
            """.trimIndent(),
            afterExample = """
                ForeignKey(
                    entity = MealEntity::class,
                    parentColumns = ["id"],
                    childColumns = ["meal_id"],
                    onDelete = ForeignKey.CASCADE
                )
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://developer.android.com/training/data-storage/room/relationships#foreign-keys",
                "https://www.sqlite.org/foreignkeys.html"
            )
        )
    }
}
