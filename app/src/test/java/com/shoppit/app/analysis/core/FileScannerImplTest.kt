package com.shoppit.app.analysis.core

import com.shoppit.app.analysis.models.CodeLayer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for FileScannerImpl.
 *
 * Tests cover:
 * - Directory scanning
 * - File filtering
 * - Layer detection
 * - Exclusion patterns
 */
class FileScannerImplTest {
    
    @get:Rule
    val tempFolder = TemporaryFolder()
    
    private lateinit var projectRoot: String
    private lateinit var scanner: FileScannerImpl
    
    @Before
    fun setup() {
        projectRoot = tempFolder.root.absolutePath
        scanner = FileScannerImpl(projectRoot)
    }
    
    @After
    fun teardown() {
        // Cleanup is handled by TemporaryFolder rule
    }
    
    @Test
    fun `scanDirectory finds Kotlin files recursively`() {
        // Given: Create test directory structure with Kotlin files
        val srcDir = tempFolder.newFolder("src", "main", "java", "com", "test")
        File(srcDir, "TestFile1.kt").writeText("class TestFile1")
        File(srcDir, "TestFile2.kt").writeText("class TestFile2")
        
        val subDir = File(srcDir, "subpackage")
        subDir.mkdirs()
        File(subDir, "TestFile3.kt").writeText("class TestFile3")
        
        // When: Scan the directory
        val files = scanner.scanDirectory(tempFolder.root.absolutePath)
        
        // Then: All Kotlin files should be found
        assertEquals(3, files.size)
        assertTrue(files.any { it.relativePath.contains("TestFile1.kt") })
        assertTrue(files.any { it.relativePath.contains("TestFile2.kt") })
        assertTrue(files.any { it.relativePath.contains("TestFile3.kt") })
    }
    
    @Test
    fun `scanDirectory filters out non-Kotlin files`() {
        // Given: Create directory with mixed file types
        val srcDir = tempFolder.newFolder("src")
        File(srcDir, "Test.kt").writeText("class Test")
        File(srcDir, "Test.java").writeText("class Test {}")
        File(srcDir, "Test.txt").writeText("text file")
        File(srcDir, "README.md").writeText("# README")
        
        // When: Scan the directory
        val files = scanner.scanDirectory(tempFolder.root.absolutePath)
        
        // Then: Only Kotlin files should be found
        assertEquals(1, files.size)
        assertTrue(files[0].relativePath.endsWith(".kt"))
    }
    
    @Test
    fun `scanDirectory includes kts files`() {
        // Given: Create directory with .kts files
        val srcDir = tempFolder.newFolder("src")
        File(srcDir, "build.gradle.kts").writeText("plugins { }")
        File(srcDir, "Test.kt").writeText("class Test")
        
        // When: Scan the directory
        val files = scanner.scanDirectory(tempFolder.root.absolutePath)
        
        // Then: Both .kt and .kts files should be found
        assertEquals(2, files.size)
        assertTrue(files.any { it.relativePath.endsWith(".kt") })
        assertTrue(files.any { it.relativePath.endsWith(".kts") })
    }
    
    @Test
    fun `scanDirectory excludes build directories`() {
        // Given: Create directory structure with build directory
        val srcDir = tempFolder.newFolder("src")
        File(srcDir, "Source.kt").writeText("class Source")
        
        val buildDir = tempFolder.newFolder("build", "generated")
        File(buildDir, "Generated.kt").writeText("class Generated")
        
        // When: Scan the directory
        val files = scanner.scanDirectory(tempFolder.root.absolutePath)
        
        // Then: Only source files should be found, not build files
        assertEquals(1, files.size)
        assertTrue(files[0].relativePath.contains("Source.kt"))
        assertFalse(files.any { it.relativePath.contains("build") })
    }
    
    @Test
    fun `scanDirectory excludes gradle directories`() {
        // Given: Create directory structure with .gradle directory
        val srcDir = tempFolder.newFolder("src")
        File(srcDir, "Source.kt").writeText("class Source")
        
        val gradleDir = tempFolder.newFolder(".gradle", "cache")
        File(gradleDir, "Cache.kt").writeText("class Cache")
        
        // When: Scan the directory
        val files = scanner.scanDirectory(tempFolder.root.absolutePath)
        
        // Then: Only source files should be found
        assertEquals(1, files.size)
        assertFalse(files.any { it.relativePath.contains(".gradle") })
    }
    
    @Test
    fun `detectLayer identifies DATA layer`() {
        // Given: Create file in data layer
        val dataDir = tempFolder.newFolder("src", "main", "java", "com", "test", "data")
        File(dataDir, "Repository.kt").writeText("class Repository")
        
        // When: Scan the directory
        val files = scanner.scanDirectory(tempFolder.root.absolutePath)
        
        // Then: File should be identified as DATA layer
        assertEquals(1, files.size)
        assertEquals(CodeLayer.DATA, files[0].layer)
    }
    
    @Test
    fun `detectLayer identifies DOMAIN layer`() {
        // Given: Create file in domain layer
        val domainDir = tempFolder.newFolder("src", "main", "java", "com", "test", "domain")
        File(domainDir, "UseCase.kt").writeText("class UseCase")
        
        // When: Scan the directory
        val files = scanner.scanDirectory(tempFolder.root.absolutePath)
        
        // Then: File should be identified as DOMAIN layer
        assertEquals(1, files.size)
        assertEquals(CodeLayer.DOMAIN, files[0].layer)
    }
    
    @Test
    fun `detectLayer identifies UI layer`() {
        // Given: Create file in ui layer
        val uiDir = tempFolder.newFolder("src", "main", "java", "com", "test", "ui")
        File(uiDir, "Screen.kt").writeText("@Composable fun Screen()")
        
        // When: Scan the directory
        val files = scanner.scanDirectory(tempFolder.root.absolutePath)
        
        // Then: File should be identified as UI layer
        assertEquals(1, files.size)
        assertEquals(CodeLayer.UI, files[0].layer)
    }
    
    @Test
    fun `detectLayer identifies DI layer`() {
        // Given: Create file in di layer
        val diDir = tempFolder.newFolder("src", "main", "java", "com", "test", "di")
        File(diDir, "Module.kt").writeText("@Module class Module")
        
        // When: Scan the directory
        val files = scanner.scanDirectory(tempFolder.root.absolutePath)
        
        // Then: File should be identified as DI layer
        assertEquals(1, files.size)
        assertEquals(CodeLayer.DI, files[0].layer)
    }
    
    @Test
    fun `detectLayer identifies TEST layer`() {
        // Given: Create file in test directory
        val testDir = tempFolder.newFolder("src", "test", "java", "com", "test")
        File(testDir, "TestClass.kt").writeText("class TestClass")
        
        // When: Scan the directory
        val files = scanner.scanDirectory(tempFolder.root.absolutePath)
        
        // Then: File should be identified as TEST layer
        assertEquals(1, files.size)
        assertEquals(CodeLayer.TEST, files[0].layer)
    }
    
    @Test
    fun `detectLayer returns null for unknown layer`() {
        // Given: Create file in unknown location
        val unknownDir = tempFolder.newFolder("src", "main", "java", "com", "test", "unknown")
        File(unknownDir, "Unknown.kt").writeText("class Unknown")
        
        // When: Scan the directory
        val files = scanner.scanDirectory(tempFolder.root.absolutePath)
        
        // Then: Layer should be null
        assertEquals(1, files.size)
        assertNull(files[0].layer)
    }
    
    @Test
    fun `filterFiles removes excluded files`() {
        // Given: Create files in various locations
        val srcDir = tempFolder.newFolder("src")
        File(srcDir, "Source.kt").writeText("class Source")
        
        val buildDir = tempFolder.newFolder("build")
        File(buildDir, "Generated.kt").writeText("class Generated")
        
        // When: Scan and filter
        val allFiles = scanner.scanDirectory(tempFolder.root.absolutePath)
        val filteredFiles = scanner.filterFiles(allFiles)
        
        // Then: Build files should be filtered out
        assertTrue(filteredFiles.size < allFiles.size || allFiles.size == 1)
        assertTrue(filteredFiles.all { !it.relativePath.contains("build") })
    }
    
    @Test
    fun `shouldAnalyze returns true for valid Kotlin files`() {
        // Given: Create a valid Kotlin file
        val srcDir = tempFolder.newFolder("src")
        val file = File(srcDir, "Test.kt")
        file.writeText("class Test")
        
        // When: Scan the directory
        val files = scanner.scanDirectory(tempFolder.root.absolutePath)
        
        // Then: File should be analyzable
        assertEquals(1, files.size)
        assertTrue(scanner.shouldAnalyze(files[0]))
    }
    
    @Test
    fun `shouldAnalyze returns false for excluded files`() {
        // Given: Create file in build directory
        val buildDir = tempFolder.newFolder("build")
        val file = File(buildDir, "Generated.kt")
        file.writeText("class Generated")
        
        // When: Scan the directory
        val files = scanner.scanDirectory(tempFolder.root.absolutePath)
        
        // Then: No files should be found (excluded during scan)
        assertEquals(0, files.size)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `scanDirectory throws exception for non-existent directory`() {
        // When: Try to scan non-existent directory
        scanner.scanDirectory("/non/existent/path")
        
        // Then: Exception should be thrown
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun `scanDirectory throws exception for file instead of directory`() {
        // Given: Create a file
        val file = tempFolder.newFile("test.kt")
        
        // When: Try to scan the file as directory
        scanner.scanDirectory(file.absolutePath)
        
        // Then: Exception should be thrown
    }
    
    @Test
    fun `scanDirectory handles empty directory`() {
        // Given: Create empty directory
        tempFolder.newFolder("empty")
        
        // When: Scan the directory
        val files = scanner.scanDirectory(tempFolder.root.absolutePath)
        
        // Then: No files should be found
        assertEquals(0, files.size)
    }
    
    @Test
    fun `custom exclusion patterns are respected`() {
        // Given: Create scanner with custom exclusion patterns
        val customScanner = FileScannerImpl(
            projectRoot = projectRoot,
            excludePatterns = listOf("**/custom/**")
        )
        
        val srcDir = tempFolder.newFolder("src")
        File(srcDir, "Source.kt").writeText("class Source")
        
        val customDir = tempFolder.newFolder("custom")
        File(customDir, "Custom.kt").writeText("class Custom")
        
        // When: Scan the directory
        val files = customScanner.scanDirectory(tempFolder.root.absolutePath)
        
        // Then: Custom directory should be excluded
        assertEquals(1, files.size)
        assertFalse(files.any { it.relativePath.contains("custom") })
    }
    
    @Test
    fun `fileInfo contains correct metadata`() {
        // Given: Create a test file
        val srcDir = tempFolder.newFolder("src", "main", "java", "com", "test", "data")
        val file = File(srcDir, "Test.kt")
        file.writeText("class Test")
        
        // When: Scan the directory
        val files = scanner.scanDirectory(tempFolder.root.absolutePath)
        
        // Then: FileInfo should contain correct metadata
        assertEquals(1, files.size)
        val fileInfo = files[0]
        
        assertTrue(fileInfo.path.endsWith("Test.kt"))
        assertTrue(fileInfo.relativePath.contains("Test.kt"))
        assertTrue(fileInfo.size > 0)
        assertTrue(fileInfo.lastModified > 0)
        assertEquals(CodeLayer.DATA, fileInfo.layer)
    }
}
