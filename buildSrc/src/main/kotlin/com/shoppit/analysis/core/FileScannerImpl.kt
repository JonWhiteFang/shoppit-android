package com.shoppit.analysis.core

import com.shoppit.analysis.models.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Implementation of FileScanner that discovers Kotlin source files.
 */
class FileScannerImpl : FileScanner {
    
    override suspend fun scanFiles(rootDir: File): List<FileInfo> = withContext(Dispatchers.IO) {
        if (!rootDir.exists() || !rootDir.isDirectory) {
            println("Warning: Source directory does not exist or is not a directory: ${rootDir.absolutePath}")
            return@withContext emptyList()
        }
        
        val files = mutableListOf<FileInfo>()
        
        rootDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                try {
                    val fileInfo = createFileInfo(file, rootDir)
                    files.add(fileInfo)
                } catch (e: Exception) {
                    println("Error processing file ${file.absolutePath}: ${e.message}")
                }
            }
        
        println("Scanned ${files.size} Kotlin files from ${rootDir.absolutePath}")
        files
    }
    
    private fun createFileInfo(file: File, rootDir: File): FileInfo {
        val relativePath = file.relativeTo(rootDir).path
        val packageName = extractPackageName(file)
        val isTest = isTestFile(file, relativePath)
        
        return FileInfo(
            file = file,
            relativePath = relativePath,
            packageName = packageName,
            isTest = isTest
        )
    }
    
    /**
     * Extracts package name from Kotlin file.
     * Reads the first few lines to find the package declaration.
     */
    private fun extractPackageName(file: File): String {
        try {
            file.bufferedReader().use { reader ->
                // Read first 20 lines to find package declaration
                repeat(20) {
                    val line = reader.readLine() ?: return ""
                    val trimmed = line.trim()
                    
                    // Skip comments and blank lines
                    if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("/*")) {
                        return@repeat
                    }
                    
                    // Check for package declaration
                    if (trimmed.startsWith("package ")) {
                        return trimmed.removePrefix("package ").trim()
                    }
                    
                    // If we hit an import or class declaration, no package was found
                    if (trimmed.startsWith("import ") || 
                        trimmed.startsWith("class ") ||
                        trimmed.startsWith("interface ") ||
                        trimmed.startsWith("object ") ||
                        trimmed.startsWith("fun ") ||
                        trimmed.startsWith("val ") ||
                        trimmed.startsWith("var ")) {
                        return ""
                    }
                }
            }
        } catch (e: Exception) {
            println("Error extracting package name from ${file.name}: ${e.message}")
        }
        
        return ""
    }
    
    /**
     * Determines if a file is a test file based on path and naming conventions.
     */
    private fun isTestFile(file: File, relativePath: String): Boolean {
        // Check if file is in test directory
        val pathLower = relativePath.lowercase()
        if (pathLower.contains("/test/") || 
            pathLower.contains("\\test\\") ||
            pathLower.contains("/androidtest/") ||
            pathLower.contains("\\androidtest\\")) {
            return true
        }
        
        // Check if filename ends with Test
        val nameLower = file.nameWithoutExtension.lowercase()
        if (nameLower.endsWith("test") || 
            nameLower.endsWith("tests") ||
            nameLower.endsWith("spec")) {
            return true
        }
        
        return false
    }
}
