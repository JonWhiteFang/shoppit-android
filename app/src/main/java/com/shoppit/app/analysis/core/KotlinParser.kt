package com.shoppit.app.analysis.core

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

/**
 * Utility class for parsing Kotlin source files into PSI (Program Structure Interface) trees.
 * 
 * This parser uses the Kotlin compiler's PSI API to create abstract syntax trees (AST)
 * that can be analyzed by code quality analyzers.
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
     * Parses a Kotlin file from a file path.
     *
     * @param filePath Path to the Kotlin file
     * @return Parsed KtFile AST, or null if parsing fails
     */
    fun parseFile(filePath: String): KtFile? {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return null
            }
            
            val content = file.readText()
            parseContent(content, file.name)
        } catch (e: Exception) {
            // Log error but don't throw - allow analysis to continue
            null
        }
    }
    
    /**
     * Parses Kotlin source code from a string.
     *
     * @param content Kotlin source code
     * @param fileName Name to use for the virtual file (for error reporting)
     * @return Parsed KtFile AST, or null if parsing fails
     */
    fun parseContent(content: String, fileName: String = "temp.kt"): KtFile? {
        return try {
            val virtualFile = LightVirtualFile(fileName, KotlinFileType.INSTANCE, content)
            val psiManager = PsiManager.getInstance(environment.project)
            psiManager.findFile(virtualFile) as? KtFile
        } catch (e: Exception) {
            // Log error but don't throw - allow analysis to continue
            null
        }
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
        fun create(): KotlinParser = KotlinParser()
        
        /**
         * Executes a block with a KotlinParser and automatically disposes it.
         *
         * @param block Code block to execute with the parser
         * @return Result of the block
         */
        inline fun <T> use(block: (KotlinParser) -> T): T {
            val parser = create()
            return try {
                block(parser)
            } finally {
                parser.dispose()
            }
        }
    }
}
