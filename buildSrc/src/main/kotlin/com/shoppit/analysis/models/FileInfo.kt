package com.shoppit.analysis.models

/**
 * Information about a file to be analyzed.
 *
 * @property path Absolute path to the file
 * @property relativePath Path relative to project root
 * @property size File size in bytes
 * @property lastModified Last modification timestamp
 * @property layer Code layer (Data, Domain, UI, DI, Test, or null)
 */
data class FileInfo(
    val path: String,
    val relativePath: String,
    val size: Long,
    val lastModified: Long,
    val layer: CodeLayer?
)

/**
 * Code architecture layers in the project.
 */
enum class CodeLayer {
    /**
     * Data layer - repositories, DAOs, entities, DTOs.
     */
    DATA,
    
    /**
     * Domain layer - models, use cases, repository interfaces.
     */
    DOMAIN,
    
    /**
     * UI layer - Compose screens, ViewModels, navigation.
     */
    UI,
    
    /**
     * Dependency injection layer - Hilt modules.
     */
    DI,
    
    /**
     * Test layer - unit tests, instrumented tests.
     */
    TEST
}
