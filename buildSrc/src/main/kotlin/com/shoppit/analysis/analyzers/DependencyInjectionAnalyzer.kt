package com.shoppit.analysis.analyzers

import com.shoppit.analysis.core.CodeAnalyzer
import com.shoppit.analysis.models.AnalysisCategory
import com.shoppit.analysis.models.CodeLayer
import com.shoppit.analysis.models.Effort
import com.shoppit.analysis.models.FileInfo
import com.shoppit.analysis.models.Finding
import com.shoppit.analysis.models.Priority
import java.util.UUID

/**
 * Analyzer that validates dependency injection patterns using Hilt.
 * 
 * Validates:
 * - ViewModels are annotated with @HiltViewModel
 * - Constructors use @Inject constructor() for dependency injection
 * - Hilt modules use @Module and @InstallIn annotations
 * - @Binds is used for interface binding in abstract modules
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.5
 */
class DependencyInjectionAnalyzer : CodeAnalyzer {
    
    override val id: String = "dependency-injection"
    
    override val name: String = "Dependency Injection Analyzer"
    
    override val category: AnalysisCategory = AnalysisCategory.DEPENDENCY_INJECTION
    
    override suspend fun analyze(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        // Check ViewModels for @HiltViewModel annotation
        if (file.relativePath.contains("ViewModel.kt") && file.layer == CodeLayer.UI) {
            findings.addAll(analyzeViewModel(file, fileContent))
        }
        
        // Check for constructor injection
        findings.addAll(analyzeConstructorInjection(file, fileContent))
        
        // Check Hilt modules
        if (file.relativePath.contains("Module.kt") || fileContent.contains("@Module")) {
            findings.addAll(analyzeHiltModule(file, fileContent))
        }
        
        return findings
    }
    
    override fun appliesTo(file: FileInfo): Boolean {
        // Apply to all Kotlin files that might use dependency injection
        return file.relativePath.endsWith(".kt")
    }
    
    /**
     * Analyzes ViewModel for @HiltViewModel annotation.
     */
    private fun analyzeViewModel(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        val lines = fileContent.lines()
        var viewModelLineNumber = -1
        var hasHiltViewModelAnnotation = false
        var viewModelClassName = ""
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            // Check for @HiltViewModel annotation
            if (trimmedLine.startsWith("@HiltViewModel")) {
                hasHiltViewModelAnnotation = true
            }
            
            // Find ViewModel class declaration
            if (trimmedLine.contains("class ") && 
                (trimmedLine.contains(": ViewModel()") || 
                 trimmedLine.contains(": ViewModel(") ||
                 trimmedLine.contains("ViewModel {") ||
                 trimmedLine.contains("ViewModel("))) {
                
                viewModelLineNumber = index + 1
                viewModelClassName = trimmedLine
                    .substringAfter("class ")
                    .substringBefore("(")
                    .substringBefore(":")
                    .substringBefore("{")
                    .trim()
            }
        }
        
        // If we found a ViewModel class but no @HiltViewModel annotation
        if (viewModelLineNumber > 0 && !hasHiltViewModelAnnotation) {
            findings.add(createMissingHiltViewModelAnnotationFinding(
                file, 
                viewModelLineNumber, 
                viewModelClassName
            ))
        }
        
        return findings
    }
    
    /**
     * Analyzes constructor injection patterns.
     */
    private fun analyzeConstructorInjection(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        val lines = fileContent.lines()
        var inConstructor = false
        var constructorLineNumber = -1
        var hasInjectAnnotation = false
        var constructorText = StringBuilder()
        var className = ""
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            // Track class name
            if (trimmedLine.startsWith("class ") || trimmedLine.contains(" class ")) {
                className = trimmedLine
                    .substringAfter("class ")
                    .substringBefore("(")
                    .substringBefore(":")
                    .substringBefore("{")
                    .trim()
            }
            
            // Check for @Inject annotation before constructor
            if (trimmedLine.startsWith("@Inject constructor")) {
                hasInjectAnnotation = true
                inConstructor = true
                constructorLineNumber = index + 1
                constructorText.append(line)
            } else if (trimmedLine.startsWith("constructor(") && !hasInjectAnnotation) {
                // Found constructor without @Inject
                inConstructor = true
                constructorLineNumber = index + 1
                constructorText.append(line)
            }
            
            // Continue collecting constructor text
            if (inConstructor) {
                if (index > constructorLineNumber - 1) {
                    constructorText.append("\n").append(line)
                }
                
                // Check if constructor ends
                if (trimmedLine.endsWith(")") || trimmedLine.endsWith(") {")) {
                    // Check if this constructor has parameters (dependencies)
                    val constructorStr = constructorText.toString()
                    val hasParameters = constructorStr.contains("(") && 
                                       !constructorStr.substringAfter("(").substringBefore(")").trim().isEmpty()
                    
                    // Only flag if constructor has parameters but no @Inject
                    if (hasParameters && !hasInjectAnnotation && 
                        !isDataClass(fileContent) && 
                        !isTestFile(file)) {
                        
                        findings.add(createMissingInjectAnnotationFinding(
                            file,
                            constructorLineNumber,
                            className,
                            constructorStr
                        ))
                    }
                    
                    // Reset for next constructor
                    inConstructor = false
                    hasInjectAnnotation = false
                    constructorText = StringBuilder()
                }
            }
        }
        
        return findings
    }
    
    /**
     * Analyzes Hilt module for proper annotations.
     */
    private fun analyzeHiltModule(file: FileInfo, fileContent: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        val lines = fileContent.lines()
        var hasModuleAnnotation = false
        var hasInstallInAnnotation = false
        var moduleLineNumber = -1
        var moduleName = ""
        var isAbstractModule = false
        var hasBindsFunction = false
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            // Check for @Module annotation
            if (trimmedLine.startsWith("@Module")) {
                hasModuleAnnotation = true
            }
            
            // Check for @InstallIn annotation
            if (trimmedLine.startsWith("@InstallIn")) {
                hasInstallInAnnotation = true
            }
            
            // Find module class/object declaration
            if ((trimmedLine.startsWith("object ") || trimmedLine.startsWith("abstract class ")) &&
                (trimmedLine.contains("Module") || hasModuleAnnotation)) {
                
                moduleLineNumber = index + 1
                moduleName = trimmedLine
                    .substringAfter("object ")
                    .substringAfter("class ")
                    .substringBefore("{")
                    .substringBefore(":")
                    .trim()
                
                isAbstractModule = trimmedLine.startsWith("abstract class")
            }
            
            // Check for @Binds annotation
            if (trimmedLine.startsWith("@Binds")) {
                hasBindsFunction = true
            }
        }
        
        // Check if module is missing @Module annotation
        if (moduleLineNumber > 0 && !hasModuleAnnotation) {
            findings.add(createMissingModuleAnnotationFinding(
                file,
                moduleLineNumber,
                moduleName
            ))
        }
        
        // Check if module is missing @InstallIn annotation
        if (moduleLineNumber > 0 && hasModuleAnnotation && !hasInstallInAnnotation) {
            findings.add(createMissingInstallInAnnotationFinding(
                file,
                moduleLineNumber,
                moduleName
            ))
        }
        
        // Check if abstract module should use @Binds
        if (isAbstractModule && !hasBindsFunction && hasModuleAnnotation) {
            findings.addAll(analyzeBindsUsage(file, fileContent, moduleName))
        }
        
        return findings
    }
    
    /**
     * Analyzes if @Binds should be used for interface binding.
     */
    private fun analyzeBindsUsage(file: FileInfo, fileContent: String, moduleName: String): List<Finding> {
        val findings = mutableListOf<Finding>()
        
        val lines = fileContent.lines()
        var inModule = false
        var braceCount = 0
        
        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            // Track if we're inside the module
            if (trimmedLine.contains("abstract class $moduleName") || 
                trimmedLine.contains("object $moduleName")) {
                inModule = true
            }
            
            // Count braces to track module scope
            braceCount += line.count { it == '{' }
            braceCount -= line.count { it == '}' }
            
            if (braceCount == 0 && inModule) {
                inModule = false
            }
            
            // Check for @Provides functions that could use @Binds
            if (inModule && trimmedLine.startsWith("@Provides")) {
                val nextLineIndex = index + 1
                if (nextLineIndex < lines.size) {
                    val functionLine = lines[nextLineIndex].trim()
                    
                    // Check if function just returns an implementation of an interface
                    // This is a simple heuristic - could be improved
                    if (functionLine.contains("fun provide") && 
                        functionLine.contains("): ") &&
                        !functionLine.contains("@ApplicationContext") &&
                        !functionLine.contains("@Singleton")) {
                        
                        // This might be a candidate for @Binds
                        // We'll suggest it as a potential improvement
                        findings.add(createConsiderBindsAnnotationFinding(
                            file,
                            nextLineIndex + 1,
                            functionLine
                        ))
                    }
                }
            }
        }
        
        return findings
    }
    
    /**
     * Checks if the file contains a data class.
     */
    private fun isDataClass(fileContent: String): Boolean {
        return fileContent.contains("data class ")
    }
    
    /**
     * Checks if the file is a test file.
     */
    private fun isTestFile(file: FileInfo): Boolean {
        return file.relativePath.contains("/test/") || 
               file.relativePath.contains("/androidTest/") ||
               file.relativePath.endsWith("Test.kt")
    }
    
    /**
     * Creates a finding for missing @HiltViewModel annotation.
     */
    private fun createMissingHiltViewModelAnnotationFinding(
        file: FileInfo,
        lineNumber: Int,
        className: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "ViewModel Missing @HiltViewModel Annotation",
            description = "ViewModel '$className' is not annotated with @HiltViewModel. " +
                    "All ViewModels that use Hilt for dependency injection must be annotated " +
                    "with @HiltViewModel to enable proper injection and lifecycle management.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = "class $className",
            recommendation = "Add @HiltViewModel annotation to the ViewModel class. " +
                    "This enables Hilt to provide the ViewModel to Activities and Fragments " +
                    "using the by viewModels() delegate.",
            beforeExample = """
                class MealViewModel @Inject constructor(
                    private val getMealsUseCase: GetMealsUseCase
                ) : ViewModel() {
                    // ...
                }
            """.trimIndent(),
            afterExample = """
                @HiltViewModel
                class MealViewModel @Inject constructor(
                    private val getMealsUseCase: GetMealsUseCase
                ) : ViewModel() {
                    // ...
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/training/dependency-injection/hilt-jetpack#viewmodels",
                "https://dagger.dev/hilt/view-model.html"
            )
        )
    }
    
    /**
     * Creates a finding for missing @Inject annotation on constructor.
     */
    private fun createMissingInjectAnnotationFinding(
        file: FileInfo,
        lineNumber: Int,
        className: String,
        constructorText: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Constructor Missing @Inject Annotation",
            description = "Constructor of '$className' has dependencies but is not annotated " +
                    "with @Inject. Hilt requires the @Inject annotation on constructors to " +
                    "enable dependency injection. Without it, Hilt cannot provide the dependencies.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = constructorText.lines().take(3).joinToString("\n"),
            recommendation = "Add @Inject annotation before the constructor keyword. " +
                    "This tells Hilt to inject the dependencies when creating instances of this class.",
            beforeExample = """
                class MealRepositoryImpl constructor(
                    private val mealDao: MealDao,
                    private val mealApi: MealApi
                ) : MealRepository {
                    // ...
                }
            """.trimIndent(),
            afterExample = """
                class MealRepositoryImpl @Inject constructor(
                    private val mealDao: MealDao,
                    private val mealApi: MealApi
                ) : MealRepository {
                    // ...
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/training/dependency-injection/hilt-android#inject-dependencies",
                "https://dagger.dev/hilt/quick-start.html"
            )
        )
    }
    
    /**
     * Creates a finding for missing @Module annotation.
     */
    private fun createMissingModuleAnnotationFinding(
        file: FileInfo,
        lineNumber: Int,
        moduleName: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Hilt Module Missing @Module Annotation",
            description = "Class '$moduleName' appears to be a Hilt module but is missing " +
                    "the @Module annotation. Hilt modules must be annotated with @Module " +
                    "to be recognized by Hilt's code generation.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = "object $moduleName",
            recommendation = "Add @Module annotation to the module class or object. " +
                    "This tells Hilt that this class provides dependencies.",
            beforeExample = """
                @InstallIn(SingletonComponent::class)
                object DatabaseModule {
                    @Provides
                    fun provideDatabase(): Database {
                        // ...
                    }
                }
            """.trimIndent(),
            afterExample = """
                @Module
                @InstallIn(SingletonComponent::class)
                object DatabaseModule {
                    @Provides
                    fun provideDatabase(): Database {
                        // ...
                    }
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/training/dependency-injection/hilt-android#hilt-modules",
                "https://dagger.dev/hilt/modules.html"
            )
        )
    }
    
    /**
     * Creates a finding for missing @InstallIn annotation.
     */
    private fun createMissingInstallInAnnotationFinding(
        file: FileInfo,
        lineNumber: Int,
        moduleName: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.HIGH,
            title = "Hilt Module Missing @InstallIn Annotation",
            description = "Module '$moduleName' is annotated with @Module but missing " +
                    "@InstallIn annotation. Hilt modules must specify which component they " +
                    "should be installed in using @InstallIn. This determines the lifecycle " +
                    "and scope of the provided dependencies.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = "@Module\nobject $moduleName",
            recommendation = "Add @InstallIn annotation with the appropriate component. " +
                    "Common components: SingletonComponent (app-wide), ActivityComponent, " +
                    "ViewModelComponent, etc.",
            beforeExample = """
                @Module
                object DatabaseModule {
                    @Provides
                    @Singleton
                    fun provideDatabase(): Database {
                        // ...
                    }
                }
            """.trimIndent(),
            afterExample = """
                @Module
                @InstallIn(SingletonComponent::class)
                object DatabaseModule {
                    @Provides
                    @Singleton
                    fun provideDatabase(): Database {
                        // ...
                    }
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.TRIVIAL,
            references = listOf(
                "https://developer.android.com/training/dependency-injection/hilt-android#hilt-modules",
                "https://dagger.dev/hilt/components.html"
            )
        )
    }
    
    /**
     * Creates a finding suggesting @Binds instead of @Provides.
     */
    private fun createConsiderBindsAnnotationFinding(
        file: FileInfo,
        lineNumber: Int,
        functionLine: String
    ): Finding {
        return Finding(
            id = UUID.randomUUID().toString(),
            analyzer = id,
            category = category,
            priority = Priority.MEDIUM,
            title = "Consider Using @Binds Instead of @Provides",
            description = "This @Provides function might be a candidate for @Binds. " +
                    "When binding an interface to its implementation, @Binds is more efficient " +
                    "than @Provides because it generates less code and is processed at compile time.",
            file = file.relativePath,
            lineNumber = lineNumber,
            codeSnippet = functionLine,
            recommendation = "If this function simply returns an implementation of an interface, " +
                    "consider using @Binds in an abstract module instead. @Binds is more efficient " +
                    "and is the recommended approach for interface binding.",
            beforeExample = """
                @Module
                @InstallIn(SingletonComponent::class)
                object RepositoryModule {
                    @Provides
                    @Singleton
                    fun provideMealRepository(
                        impl: MealRepositoryImpl
                    ): MealRepository {
                        return impl
                    }
                }
            """.trimIndent(),
            afterExample = """
                @Module
                @InstallIn(SingletonComponent::class)
                abstract class RepositoryModule {
                    @Binds
                    @Singleton
                    abstract fun bindMealRepository(
                        impl: MealRepositoryImpl
                    ): MealRepository
                }
            """.trimIndent(),
            autoFixable = false,
            effort = Effort.SMALL,
            references = listOf(
                "https://developer.android.com/training/dependency-injection/hilt-android#inject-interfaces",
                "https://dagger.dev/api/latest/dagger/Binds.html"
            )
        )
    }
}
