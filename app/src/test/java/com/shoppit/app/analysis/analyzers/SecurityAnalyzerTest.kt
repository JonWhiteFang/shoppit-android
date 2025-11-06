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
 * Unit tests for SecurityAnalyzer.
 * Tests validation of security patterns including:
 * - Hardcoded secrets detection
 * - Sensitive information logging
 * - SQL injection risks
 * - Insecure data storage
 */
class SecurityAnalyzerTest {
    
    private lateinit var analyzer: SecurityAnalyzer
    
    @Before
    fun setup() {
        analyzer = SecurityAnalyzer()
    }
    
    @Test
    fun `analyzer has correct properties`() {
        assertEquals("security", analyzer.id)
        assertEquals("Security Analyzer", analyzer.name)
        assertEquals(AnalysisCategory.SECURITY, analyzer.category)
    }
    
    @Test
    fun `appliesTo returns true for Kotlin files in main source`() {
        val file = FileInfo(
            path = "/data/repository/MealRepositoryImpl.kt",
            relativePath = "data/repository/MealRepositoryImpl.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        assertTrue(analyzer.appliesTo(file))
    }
    
    @Test
    fun `appliesTo returns false for test files`() {
        val file = FileInfo(
            path = "/test/MealRepositoryTest.kt",
            relativePath = "test/MealRepositoryTest.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.TEST
        )
        
        assertFalse(analyzer.appliesTo(file))
    }
    
    @Test
    fun `appliesTo returns false for non-Kotlin files`() {
        val file = FileInfo(
            path = "/resources/config.xml",
            relativePath = "resources/config.xml",
            size = 100L,
            lastModified = 0L,
            layer = null
        )
        
        assertFalse(analyzer.appliesTo(file))
    }
    
    // Hardcoded Secret Detection Tests (Requirement 13.1)
    
    @Test
    fun `detects hardcoded API key`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/remote/ApiService.kt",
            relativePath = "data/remote/ApiService.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.remote
            
            object ApiConfig {
                const val API_KEY = "sk_test_FAKE_KEY_FOR_TESTING_ONLY_NOT_REAL"
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Hardcoded Secret Detected") })
        val finding = findings.first { it.title.contains("Hardcoded Secret Detected") }
        assertEquals(Priority.CRITICAL, finding.priority)
        assertTrue(finding.description.contains("hardcoded secret"))
    }
    
    @Test
    fun `detects hardcoded password`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/DatabaseConfig.kt",
            relativePath = "data/local/DatabaseConfig.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local
            
            object DatabaseConfig {
                const val PASSWORD = "mySecretPassword123"
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Hardcoded Secret Detected") })
        val finding = findings.first { it.title.contains("Hardcoded Secret Detected") }
        assertEquals(Priority.CRITICAL, finding.priority)
    }
    
    @Test
    fun `detects hardcoded access token`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/remote/AuthService.kt",
            relativePath = "data/remote/AuthService.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.remote
            
            class AuthService {
                private val accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Hardcoded Secret Detected") })
    }
    
    @Test
    fun `detects AWS credentials`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/remote/S3Config.kt",
            relativePath = "data/remote/S3Config.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.remote
            
            object S3Config {
                const val AWS_ACCESS_KEY_ID = "AKIAIOSFODNN7EXAMPLE"
                const val AWS_SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.size >= 2)
        assertTrue(findings.all { it.title.contains("Hardcoded Secret Detected") })
    }
    
    @Test
    fun `detects private key`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/security/KeyManager.kt",
            relativePath = "data/security/KeyManager.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.security
            
            object KeyManager {
                const val PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----"
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Hardcoded Secret Detected") })
    }
    
    @Test
    fun `ignores placeholder values`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/remote/ApiService.kt",
            relativePath = "data/remote/ApiService.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.remote
            
            object ApiConfig {
                const val API_KEY = "your_api_key_here"
                const val SECRET = "xxxxxxxxxxxxxxxx"
                const val TOKEN = "example_token_123"
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should not flag placeholder values
        assertFalse(findings.any { it.title.contains("Hardcoded Secret Detected") })
    }
    
    @Test
    fun `ignores comments with secrets`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/remote/ApiService.kt",
            relativePath = "data/remote/ApiService.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.remote
            
            object ApiConfig {
                // Example: API_KEY = "sk_test_FAKE_KEY_FOR_TESTING_ONLY_NOT_REAL"
                const val API_KEY = BuildConfig.API_KEY
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should not flag secrets in comments
        assertFalse(findings.any { it.title.contains("Hardcoded Secret Detected") })
    }
    
    @Test
    fun `accepts BuildConfig usage`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/remote/ApiService.kt",
            relativePath = "data/remote/ApiService.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.remote
            
            object ApiConfig {
                const val API_KEY = BuildConfig.API_KEY
                const val SECRET = BuildConfig.API_SECRET
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Hardcoded Secret Detected") })
    }
    
    // Sensitive Logging Detection Tests (Requirement 13.2)
    
    @Test
    fun `detects password logging`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/auth/LoginViewModel.kt",
            relativePath = "ui/auth/LoginViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.auth
            
            class LoginViewModel {
                fun login(username: String, password: String) {
                    Log.d(TAG, "Login attempt with password: ${'$'}password")
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Sensitive Information Logged") })
        val finding = findings.first { it.title.contains("Sensitive Information Logged") }
        assertEquals(Priority.HIGH, finding.priority)
    }
    
    @Test
    fun `detects token logging`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/remote/AuthInterceptor.kt",
            relativePath = "data/remote/AuthInterceptor.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.remote
            
            class AuthInterceptor {
                fun intercept(chain: Chain): Response {
                    val token = getToken()
                    Timber.d("Using auth token: ${'$'}token")
                    return chain.proceed(request)
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Sensitive Information Logged") })
    }
    
    @Test
    fun `detects credit card logging`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/payment/PaymentViewModel.kt",
            relativePath = "ui/payment/PaymentViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.payment
            
            class PaymentViewModel {
                fun processPayment(cardNumber: String) {
                    Log.d(TAG, "Processing payment for card: ${'$'}cardNumber")
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Sensitive Information Logged") })
    }
    
    @Test
    fun `detects email logging`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/profile/ProfileViewModel.kt",
            relativePath = "ui/profile/ProfileViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.profile
            
            class ProfileViewModel {
                fun updateProfile(email: String) {
                    println("Updating email: ${'$'}email")
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Sensitive Information Logged") })
    }
    
    @Test
    fun `accepts safe logging`() = runTest {
        val fileInfo = FileInfo(
            path = "/ui/meal/MealViewModel.kt",
            relativePath = "ui/meal/MealViewModel.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.UI
        )
        
        val code = """
            package com.shoppit.app.ui.meal
            
            class MealViewModel {
                fun loadMeals() {
                    Log.d(TAG, "Loading meals")
                    Timber.d("Meals loaded successfully")
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Sensitive Information Logged") })
    }
    
    @Test
    fun `accepts redacted logging`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/remote/AuthInterceptor.kt",
            relativePath = "data/remote/AuthInterceptor.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.remote
            
            class AuthInterceptor {
                fun intercept(chain: Chain): Response {
                    val token = getToken()
                    Timber.d("Token received (length: ${'$'}{token.length})")
                    return chain.proceed(request)
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Sensitive Information Logged") })
    }
    
    // SQL Injection Detection Tests (Requirement 13.3)
    
    @Test
    fun `detects SQL injection in Query annotation`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            @Dao
            interface MealDao {
                @Query("SELECT * FROM meals WHERE name = '" + name + "'")
                fun getMealByName(name: String): Meal?
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("SQL Injection Risk Detected") })
        val finding = findings.first { it.title.contains("SQL Injection Risk Detected") }
        assertEquals(Priority.CRITICAL, finding.priority)
    }
    
    @Test
    fun `detects SQL injection with string interpolation`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            @Dao
            interface MealDao {
                @Query("SELECT * FROM meals WHERE id = ${'$'}mealId")
                fun getMealById(mealId: Long): Meal?
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("SQL Injection Risk Detected") })
    }
    
    @Test
    fun `detects SQL injection in execSQL`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/DatabaseHelper.kt",
            relativePath = "data/local/DatabaseHelper.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local
            
            class DatabaseHelper {
                fun deleteMeal(name: String) {
                    database.execSQL("DELETE FROM meals WHERE name = '" + name + "'")
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("SQL Injection Risk Detected") })
    }
    
    @Test
    fun `accepts parameterized queries`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            @Dao
            interface MealDao {
                @Query("SELECT * FROM meals WHERE name = :name")
                fun getMealByName(name: String): Meal?
                
                @Query("SELECT * FROM meals WHERE id = :mealId")
                fun getMealById(mealId: Long): Meal?
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("SQL Injection Risk Detected") })
    }
    
    // Insecure Data Storage Detection Tests (Requirement 13.4)
    
    @Test
    fun `detects SharedPreferences with sensitive data`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/PreferencesManager.kt",
            relativePath = "data/local/PreferencesManager.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local
            
            class PreferencesManager {
                fun saveToken(token: String) {
                    val prefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
                    prefs.edit().putString("auth_token", token).apply()
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Insecure Data Storage Detected") })
        val finding = findings.first { it.title.contains("Insecure Data Storage Detected") }
        assertEquals(Priority.HIGH, finding.priority)
    }
    
    @Test
    fun `detects file storage with sensitive data`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/FileManager.kt",
            relativePath = "data/local/FileManager.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local
            
            class FileManager {
                fun savePassword(password: String) {
                    val file = File(context.filesDir, "credentials.txt")
                    file.writeText(password)
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.any { it.title.contains("Insecure Data Storage Detected") })
    }
    
    @Test
    fun `accepts EncryptedSharedPreferences`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/SecurePreferencesManager.kt",
            relativePath = "data/local/SecurePreferencesManager.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local
            
            class SecurePreferencesManager {
                fun saveToken(token: String) {
                    val encryptedPrefs = EncryptedSharedPreferences.create(...)
                    encryptedPrefs.edit().putString("auth_token", token).apply()
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Insecure Data Storage Detected") })
    }
    
    @Test
    fun `accepts SharedPreferences for non-sensitive data`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/PreferencesManager.kt",
            relativePath = "data/local/PreferencesManager.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local
            
            class PreferencesManager {
                fun saveTheme(theme: String) {
                    val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                    prefs.edit().putString("theme", theme).apply()
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertFalse(findings.any { it.title.contains("Insecure Data Storage Detected") })
    }
    
    // General Tests
    
    @Test
    fun `handles empty file`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/Empty.kt",
            relativePath = "data/Empty.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = ""
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.isEmpty())
    }
    
    @Test
    fun `detects multiple security issues in same file`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/remote/ApiService.kt",
            relativePath = "data/remote/ApiService.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.remote
            
            class ApiService {
                private val apiKey = "sk_test_FAKE_KEY_FOR_TESTING_ONLY_NOT_REAL"
                
                fun authenticate(password: String) {
                    Log.d(TAG, "Authenticating with password: ${'$'}password")
                    
                    val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                    prefs.edit().putString("token", password).apply()
                }
                
                fun getUser(name: String) {
                    database.execSQL("SELECT * FROM users WHERE name = '" + name + "'")
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should have multiple security violations
        assertTrue(findings.size >= 4)
        assertTrue(findings.any { it.title.contains("Hardcoded Secret") })
        assertTrue(findings.any { it.title.contains("Sensitive Information Logged") })
        assertTrue(findings.any { it.title.contains("Insecure Data Storage") })
        assertTrue(findings.any { it.title.contains("SQL Injection") })
    }
    
    @Test
    fun `finding IDs are unique`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/remote/ApiService.kt",
            relativePath = "data/remote/ApiService.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.remote
            
            class ApiService {
                private val apiKey = "sk_test_FAKE_KEY_FOR_TESTING_ONLY_NOT_REAL"
                private val secret = "test_secret_FAKE_FOR_TESTING_ONLY_NOT_REAL"
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
            path = "/data/remote/ApiService.kt",
            relativePath = "data/remote/ApiService.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.remote
            
            class ApiService {
                private val apiKey = "sk_test_FAKE_KEY_FOR_TESTING_ONLY_NOT_REAL"
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
    fun `all security findings have CRITICAL or HIGH priority`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/remote/ApiService.kt",
            relativePath = "data/remote/ApiService.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.remote
            
            class ApiService {
                private val apiKey = "sk_test_FAKE_KEY_FOR_TESTING_ONLY_NOT_REAL"
                
                fun authenticate(password: String) {
                    Log.d(TAG, "Password: ${'$'}password")
                }
                
                fun getUser(name: String) {
                    database.execSQL("SELECT * FROM users WHERE name = '" + name + "'")
                }
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        assertTrue(findings.isNotEmpty())
        findings.forEach { finding ->
            assertTrue(
                "Finding should be CRITICAL or HIGH priority: ${finding.title}",
                finding.priority == Priority.CRITICAL || finding.priority == Priority.HIGH
            )
        }
    }
    
    @Test
    fun `handles multiline strings`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/local/dao/MealDao.kt",
            relativePath = "data/local/dao/MealDao.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data.local.dao
            
            @Dao
            interface MealDao {
                @Query(${"\"\"\""}
                    SELECT * FROM meals 
                    WHERE name = :name
                    ORDER BY created_at DESC
                ${"\"\"\""})
                fun getMealByName(name: String): Meal?
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should not flag parameterized query
        assertFalse(findings.any { it.title.contains("SQL Injection Risk Detected") })
    }
    
    @Test
    fun `detects secrets in different string formats`() = runTest {
        val fileInfo = FileInfo(
            path = "/data/Config.kt",
            relativePath = "data/Config.kt",
            size = 100L,
            lastModified = 0L,
            layer = CodeLayer.DATA
        )
        
        val code = """
            package com.shoppit.app.data
            
            object Config {
                const val KEY1 = "sk_test_FAKE_KEY_FOR_TESTING_ONLY_NOT_REAL"
                val key2 = "sk_test_FAKE_KEY_FOR_TESTING_ONLY_NOT_REAL"
                private val key3: String = "sk_test_FAKE_KEY_FOR_TESTING_ONLY_NOT_REAL"
            }
        """.trimIndent()
        
        val findings = analyzer.analyze(fileInfo, code)
        
        // Should detect all three
        assertTrue(findings.size >= 3)
        assertTrue(findings.all { it.title.contains("Hardcoded Secret Detected") })
    }
}
