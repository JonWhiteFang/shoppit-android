package com.shoppit.app.analysis.core

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import timber.log.Timber
import java.io.File

/**
 * Utility class for parsing Kotlin source files into PSI (Program Structure Interface) trees.
 *
 * Uses the Kotlin compiler's PSI parser to create abstract syntax trees (AST) from Kotlin files.
 * This enables static analysis of code structure, patterns, and potential issues.
 */
class KotlinParser {
    
    private val disposable = Disposer.newDisposable()
    private val environment: KotlinCoreEnvironment
    
    init {
        // Initialize Kotlin compiler environment
        val configuration = CompilerConfiguration()
        environment = KotlinCoreEnvironment.createForProduction(
            disposable,
            configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }
    
    /**
     * Parses a Kotlin file into a KtFile AST.
     *
     * @param file The file to parse
     * @return KtFile AST representation, or null if parsing fails
     */
    fun parseFile(file: File): KtFile? {
        return try {
            if (!file.exists()) {
                Timber.w("File does not exist: ${file.absolutePath}")
                return null
            }
            
            if (!file.canRead()) {
                Timber.w("Cannot read file: ${file.absolutePath}")
                return null
            }
            
            val content = file.readText()
            parseContent(content, file.name)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse file: ${file.absolutePath}")
            null
        }
    }
    
    /**
     * Parses Kotlin source code content into a KtFile AST.
     *
     * @param content The Kotlin source code content
     * @param fileName The name of the file (for error reporting)
     * @return KtFile AST representation, or null if parsing fails
     */
    fun parseContent(content: String, fileName: String = "temp.kt"): KtFile? {
        return try {
            // Create a virtual file with the content
            val virtualFile = LightVirtualFile(fileName, KotlinFileType.INSTANCE, content)
            
            // Get PSI manager and create PSI file
            val psiManager = PsiManager.getInstance(environment.project)
            val psiFile = psiManager.findFile(virtualFile)
            
            // Cast to KtFile
            psiFile as? KtFile
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse content for file: $fileName")
            null
        }
    }
    
    /**
     * Parses a file by path.
     *
     * @param filePath Path to the file to parse
     * @return KtFile AST representation, or null if parsing fails
     */
    fun parseFilePath(filePath: String): KtFile? {
        return parseFile(File(filePath))
    }
    
    /**
     * Validates if a file can be parsed without errors.
     *
     * @param file The file to validate
     * @return true if the file can be parsed successfully, false otherwise
     */
    fun canParse(file: File): Boolean {
        return parseFile(file) != null
    }
    
    /**
     * Cleans up resources used by the parser.
     * Should be called when the parser is no longer needed.
     */
    fun dispose() {
        Disposer.dispose(disposable)
    }
    
    companion object {
        /**
         * Creates a new KotlinParser instance.
         * Remember to call dispose() when done.
         */
        fun create(): KotlinParser {
            return KotlinParser()
        }
        
        /**
         * Parses a file using a temporary parser instance.
         * Automatically disposes the parser after use.
         *
         * @param file The file to parse
         * @return KtFile AST representation, or null if parsing fails
         */
        fun parseFileOnce(file: File): KtFile? {
            val parser = create()
            return try {
                parser.parseFile(file)
            } finally {
                parser.dispose()
            }
        }
        
        /**
         * Parses content using a temporary parser instance.
         * Automatically disposes the parser after use.
         *
         * @param content The Kotlin source code content
         * @param fileName The name of the file (for error reporting)
         * @return KtFile AST representation, or null if parsing fails
         */
        fun parseContentOnce(content: String, fileName: String = "temp.kt"): KtFile? {
            val parser = create()
            return try {
                parser.parseContent(content, fileName)
            } finally {
                parser.dispose()
            }
        }
    }
}
