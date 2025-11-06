package com.shoppit.analysis.analyzers

import com.shoppit.analysis.core.CodeAnalyzer
import com.shoppit.analysis.models.AnalysisCategory
import com.shoppit.analysis.models.Effort
import com.shoppit.analysis.models.FileInfo
import com.shoppit.analysis.models.Finding
import com.shoppit.analysis.models.Priority
import java.util.UUID

/**
 * Analyzer that validates security patterns throughout the codebase.
 * 
 * Detects:
 * - Hardcoded secrets (API keys, passwords, tokens)
 * - Logging of sensitive information
 * - SQL injection risks
 * - Insecure data storage
 * 
 * Requirements: 13.1, 13.2, 13.3, 13.4, 13.5
 */
class SecurityAnalyzer : CodeAnalyzer {
    
    override val id: String = "security"
    
    override val name: String = "Security Analyzer"
    
    override val category: AnalysisCategory = AnalysisCategory.SECURITY
    
    /**
     * Patterns that indicate potential hardcoded secrets.
     * These patterns match common secret formats.
     */
    private val secretPatterns = listOf(
        // API keys
        Regex("""(?i)(api[_-]?key|apikey)\s*[=:]\s*["']([a-zA-Z0-9_\-]{20,})["']"""),
        Regex("""(?i)(api[_-]?secret|apisecret)\s*[=:]\s*["']([a-zA-Z0-9_\-]{20,})["']"""),
        
        // Access tokens
        Regex("""(?i)(access[_-]?token|accesstoken)\s*[=:]\s*["']([a-zA-Z0-9_\-]{20,})["']"""),
        Regex("""(?i)(auth[_-]?token|authtoken)\s*[=:]\s*["']([a-zA-Z0-9_\-]{20,})["']"""),
        Regex("""(?i)(bearer[_-]?token|bearertoken)\s*[=:]\s*["']([a-zA-Z0-9_\-]{20,})["']"""),
        
        // Passwords
        Regex("""(?i)(password|passwd|pwd)\s*[=:]\s*["']([^"']{8,})["']"""),
        
        // Private keys
        Regex("""(?i)(private[_-]?key|privatekey)\s*[=:]\s*["']([a-zA-Z0-9_\-]{20,})["']"""),
        Regex("""-----BEGIN (RSA |EC |DSA )?PRIVATE KEY-----"""),
        
        // AWS credentials
        Regex("""(?i)(aws[_-]?access[_-]?key[_-]?id|awsaccesskeyid)\s*[=:]\s*["']([A-Z0-9]{20})["']"""),
        Regex("""(?i)(aws[_-]?secret[_-]?access[_-]?key|awssecretaccesskey)\s*[=:]\s*["']([a-zA-Z0-9/+=]{40})["']"""),
        
        // Generic secrets
        Regex("""(?i)(secret|token|key)\s*[=:]\s*["']([a-zA-Z0-9_\-]{32,})["']"""),
        
        // Database connection strings with credentials
        Regex("""(?i)jdbc:[^:]+://[^:]+:([^@]+)@"""),
        
        // OAuth client secrets
        Regex("""(?i)(client[_-]?secret|clientsecret)\s*[=:]\s*["']([a-zA-Z0-9_\-]{20,})["']""")
    )
    
    /**
     * Patterns that indicate sensitive data that should not be logged.
     */
    private val sensitiveDataPatterns = listOf(
        // Passwords
        Regex("""(?i)(password|passwd|pwd)"""),
        
        // Tokens and keys
        Regex("""(?i)(token|key|secret)"""),
        
        // Credit card numbers
        Regex("""(?i)(credit[_-]?card|creditcard|card[_-]?number|cardnumber)"""),
        Regex("""\b\d{4}[- ]?\d{4}[- ]?\d{4}[- ]?\d{4}\b"""),
        
        // Social security numbers
        Regex("""(?i)(ssn|social[_-]?security)"""),
        Regex("""\b\d{3}-\d{2}-\d{4}\b"""),
        
        // Email addresses (in some contexts)
        Regex("""(?i)(email|e-mail)"""),
        
        // Phone numbers
        Regex("""(?i)(phone|mobile|cell)"""),
        
        // Authentication data
        Regex("""(?i)(auth|authentication|authorization)"""),
        
        // Personal information
        Regex("""(?i)(address|birthday|birth[_-]?date)""")
    )
    
    /**
     * Logging method patterns to detect logging statements.
     */
    private val loggingPatterns = listOf(
        Regex("""Log\.[diwev]\("""),
        Regex("""Timber\.[diwev]\("""),
        Regex("""println\("""),
        Regex("""print\("""),
        Regex("""System\.out\.print"""),
        Regex("""System\.err\.print""")
    )
    
    override suspend fun analyze(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Detect hardcoded secrets
        findings.addAll(detectHardcodedSecrets(file, fileContent))
        
        // Detect sensitive information logging
        findings.addAll(detectSensitiveLogging(file, fileContent))
        
        // Detect SQL injection risks
        findings.addAll(detectSqlInjectionRisks(file, fileContent))
        
        // Detect insecure data storage
        findings.addAll(detectInsecureDataStorage(file, fileContent))
        
        return findings
    }
    
    override fun appliesTo(file: FileInfo): Boolean {
        // Apply to all Kotlin files except test files
        return file.relativePath.endsWith(".kt") && 
               !file.relativePath.contains("/test/") &&
               !file.relativePath.contains("/androidTest/")
    }
    
    /**
     * Detects hardcoded secrets in string literals.
     * Requirement 13.1: Detect hardcoded API keys or secrets.
     */
    private fun detectHardcodedSecrets(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            // Skip comments and test files
            if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                return@forEachIndexed
            }
            
            // Check each secret pattern
            for (pattern in secretPatterns) {
                val matches = pattern.findAll(line)
                for (match in matches) {
                    // Extract the secret value
                    val secretValue = if (match.groupValues.size > 2) {
                        match.groupValues[2]
                    } else {
                        match.value
                    }
                    
                    // Skip if it looks like a placeholder or example
                    if (isPlaceholder(secretValue)) {
                        continue
                    }
                    
                    findings.add(createHardcodedSecretFinding(
                        file,
                        index + 1,
                        match.value,
                        line.trim()
                    ))
                }
            }
        }
        
        return findings
    }
    
    /**
     * Detects logging of sensitive information.
     * Requirement 13.2: Detect logging of sensitive information.
     */
    private fun detectSensitiveLogging(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            // Check if line contains logging
            val hasLogging = loggingPatterns.any { it.containsMatchIn(line) }
            
            if (hasLogging) {
                // Check if the logged content contains sensitive data patterns
                for (pattern in sensitiveDataPatterns) {
                    if (pattern.containsMatchIn(line)) {
                        findings.add(createSensitiveLoggingFinding(
                            file,
                            index + 1,
                            pattern.pattern,
                            line.trim()
                        ))
                        break // Only report once per line
                    }
                }
            }
        }
        
        return findings
    }
    
    /**
     * Detects SQL injection risks from string concatenation in queries.
     * Requirement 13.3: Verify parameterized queries prevent SQL injection.
     */
    private fun detectSqlInjectionRisks(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            // Check for @Query annotation with string concatenation
            if (trimmed.contains("@Query") || trimmed.contains("@RawQuery")) {
                // Look for string concatenation in the query
                val queryLines = extractQueryString(lines, index)
                val queryString = queryLines.joinToString(" ")
                
                // Check for string concatenation patterns
                if (hasSqlInjectionRisk(queryString)) {
                    findings.add(createSqlInjectionFinding(
                        file,
                        index + 1,
                        queryString.take(100),
                        line.trim()
                    ))
                }
            }
            
            // Check for raw SQL execution with string concatenation
            if (trimmed.contains("execSQL") || trimmed.contains("rawQuery")) {
                if (line.contains("+") || line.contains("$")) {
                    findings.add(createSqlInjectionFinding(
                        file,
                        index + 1,
                        "Raw SQL with string concatenation",
                        line.trim()
                    ))
                }
            }
        }
        
        return findings
    }
    
    /**
     * Detects insecure data storage patterns.
     * Requirement 13.4: Verify sensitive data uses encrypted storage.
     */
    private fun detectInsecureDataStorage(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        val lines = fileContent.lines()
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            // Check for SharedPreferences usage with sensitive data
            if (trimmed.contains("SharedPreferences") && !trimmed.contains("EncryptedSharedPreferences")) {
                // Check if the context suggests sensitive data
                val contextLines = getContextLines(lines, index, 5)
                val context = contextLines.joinToString(" ")
                
                for (pattern in sensitiveDataPatterns) {
                    if (pattern.containsMatchIn(context)) {
                        findings.add(createInsecureStorageFinding(
                            file,
                            index + 1,
                            "SharedPreferences",
                            line.trim()
                        ))
                        break
                    }
                }
            }
            
            // Check for file storage of sensitive data without encryption
            if ((trimmed.contains("FileOutputStream") || trimmed.contains("writeText")) &&
                !trimmed.contains("encrypt")) {
                val contextLines = getContextLines(lines, index, 5)
                val context = contextLines.joinToString(" ")
                
                for (pattern in sensitiveDataPatterns) {
                    if (pattern.containsMatchIn(context)) {
                        findings.add(createInsecureStorageFinding(
                            file,
                            index + 1,
                            "File storage",
                            line.trim()
                        ))
                        break
                    }
                }
            }
        }
        
        return findings
    }
    
    // Helper methods
    
    private fun isPlaceholder(value: String): Boolean {
        val placeholders = listOf(
            "your_api_key",
            "your_secret",
            "your_token",
            "example",
            "test",
            "dummy",
            "placeholder",
            "xxx",
            "yyy",
            "zzz",
            "abc",
            "123",
            "000"
        )
        
        val lowerValue = value.lowercase()
        return placeholders.any { lowerValue.contains(it) } || 
               value.all { it == 'x' || it == 'X' || it == '*' }
    }
    
    private fun extractQueryString(lines: List<String>, startIndex: Int): List<String> {
        val queryLines = mutableListOf<String>()
        var inQuery = false
        
        for (i in startIndex until minOf(startIndex + 10, lines.size)) {
            val line = lines[i].trim()
            
            if (line.contains("@Query") || line.contains("@RawQuery")) {
                inQuery = true
            }
            
            if (inQuery) {
                queryLines.add(line)
                
                // Stop at the end of the annotation
                if (line.contains(")") && !line.contains("(")) {
                    break
                }
            }
        }
        
        return queryLines
    }
    
    private fun hasSqlInjectionRisk(queryString: String): Boolean {
        // Check for string concatenation in SQL
        return (queryString.contains("+") && queryString.contains("\"")) ||
               queryString.contains("$") ||
               queryString.contains("'$") ||
               (queryString.contains("WHERE") && queryString.contains("+"))
    }
    
    private fun getContextLines(lines: List<String>, centerIndex: Int, radius: Int): List<String> {
        val start = maxOf(0, centerIndex - radius)
        val end = minOf(lines.size, centerIndex + radius + 1)
        return lines.subList(start, end)
    }
    
    // Finding creation methods
    
    private fun createHardcodedSecretFinding(
        file: FileInfo,
        lineNumber: Int,
        secretPattern: String,
        code: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.CRITICAL,
            title = "Hardcoded Secret Detected",
            description = "A potential hardcoded secret (API key, password, or token) was detected in the code. " +
                    "Hardcoded secrets pose a critical security risk as they can be exposed in version control, " +
                    "decompiled APKs, or logs. Secrets should never be committed to source code.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = code,
            recommendation = "Remove the hardcoded secret immediately and use one of these secure alternatives:\n" +
                    "1. Store secrets in BuildConfig (defined in build.gradle.kts, excluded from VCS)\n" +
                    "2. Use Android Keystore for sensitive cryptographic keys\n" +
                    "3. Use EncryptedSharedPreferences for runtime secrets\n" +
                    "4. Fetch secrets from a secure backend service\n" +
                    "5. Use environment variables for CI/CD pipelines\n\n" +
                    "IMPORTANT: After removing, rotate the exposed secret immediately as it may have been compromised.",
            beforeExample = code,
            afterExample = """
                // Use BuildConfig for API keys
                val apiKey = BuildConfig.API_KEY
                
                // Or use secure storage
                val encryptedPrefs = EncryptedSharedPreferences.create(...)
                val apiKey = encryptedPrefs.getString("api_key", "")
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://developer.android.com/topic/security/data",
                "https://developer.android.com/training/articles/keystore",
                "https://owasp.org/www-community/vulnerabilities/Use_of_hard-coded_password"
            )
        )
    }
    
    private fun createSensitiveLoggingFinding(
        file: FileInfo,
        lineNumber: Int,
        sensitivePattern: String,
        code: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Sensitive Information Logged",
            description = "Sensitive information (passwords, tokens, keys, or personal data) is being logged. " +
                    "Logs can be accessed by other apps with READ_LOGS permission, exposed in crash reports, " +
                    "or inadvertently shared during debugging. Logging sensitive data violates privacy " +
                    "regulations (GDPR, CCPA) and security best practices.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = code,
            recommendation = "Remove or redact sensitive information from logs:\n" +
                    "1. Never log passwords, tokens, keys, or authentication data\n" +
                    "2. Redact personal information (email, phone, address)\n" +
                    "3. Log only non-sensitive metadata (e.g., 'User logged in' instead of credentials)\n" +
                    "4. Use Timber with a custom tree that filters sensitive data in production\n" +
                    "5. Consider logging only in debug builds using BuildConfig.DEBUG checks",
            beforeExample = code,
            afterExample = """
                // Bad - logs sensitive data
                Log.d(TAG, "User password: ${'$'}password")
                
                // Good - logs only metadata
                Log.d(TAG, "User authentication successful")
                
                // Good - redacts sensitive data
                Log.d(TAG, "Token received (length: ${'$'}{token.length})")
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/studio/debug/am-logcat",
                "https://owasp.org/www-project-mobile-top-10/2016-risks/m2-insecure-data-storage",
                "https://github.com/JakeWharton/timber"
            )
        )
    }
    
    private fun createSqlInjectionFinding(
        file: FileInfo,
        lineNumber: Int,
        queryPattern: String,
        code: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.CRITICAL,
            title = "SQL Injection Risk Detected",
            description = "SQL query uses string concatenation or interpolation, which creates a SQL injection " +
                    "vulnerability. Attackers can manipulate input to execute arbitrary SQL commands, potentially " +
                    "accessing, modifying, or deleting data. This is a critical security flaw.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = code,
            recommendation = "Use parameterized queries to prevent SQL injection:\n" +
                    "1. Room: Use query parameters with ':paramName' syntax\n" +
                    "2. Raw SQL: Use '?' placeholders with selectionArgs\n" +
                    "3. Never concatenate user input into SQL strings\n" +
                    "4. Validate and sanitize all input (defense in depth)\n" +
                    "5. Use Room's compile-time query verification",
            beforeExample = """
                // VULNERABLE - string concatenation
                @Query("SELECT * FROM users WHERE name = '" + userName + "'")
                fun getUser(userName: String): User
            """.trimIndent(),
            afterExample = """
                // SAFE - parameterized query
                @Query("SELECT * FROM users WHERE name = :userName")
                fun getUser(userName: String): User
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://developer.android.com/training/data-storage/room/accessing-data",
                "https://owasp.org/www-community/attacks/SQL_Injection",
                "https://cheatsheetseries.owasp.org/cheatsheets/SQL_Injection_Prevention_Cheat_Sheet.html"
            )
        )
    }
    
    private fun createInsecureStorageFinding(
        file: FileInfo,
        lineNumber: Int,
        storageType: String,
        code: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Insecure Data Storage Detected",
            description = "Sensitive data is being stored using $storageType without encryption. " +
                    "Unencrypted storage can be accessed by attackers with physical access to the device, " +
                    "through backup files, or via rooted devices. This violates security best practices and " +
                    "privacy regulations.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = code,
            recommendation = "Use encrypted storage for sensitive data:\n" +
                    "1. Use EncryptedSharedPreferences for key-value data\n" +
                    "2. Use Android Keystore for cryptographic keys\n" +
                    "3. Encrypt files before writing to storage\n" +
                    "4. Use Room with SQLCipher for encrypted databases\n" +
                    "5. Never store highly sensitive data (passwords, credit cards) on device",
            beforeExample = """
                // INSECURE - plain SharedPreferences
                val prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
                prefs.edit().putString("auth_token", token).apply()
            """.trimIndent(),
            afterExample = """
                // SECURE - EncryptedSharedPreferences
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                
                val encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    "secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                
                encryptedPrefs.edit().putString("auth_token", token).apply()
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.MEDIUM,
            references = listOf(
                "https://developer.android.com/topic/security/data",
                "https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences",
                "https://developer.android.com/training/articles/keystore"
            )
        )
    }
}
