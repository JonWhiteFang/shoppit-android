package com.shoppit.app.presentation.ui.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

/**
 * Performance monitoring data for list rendering.
 * 
 * Requirements: 2.1, 2.2
 * 
 * @property scrollEvents Number of scroll events
 * @property recompositionCount Number of recompositions detected
 * @property avgScrollSpeed Average scroll speed in items per second
 * @property slowScrollEvents Number of slow scroll events (< 30 FPS)
 */
data class ListPerformanceMetrics(
    val scrollEvents: Int = 0,
    val recompositionCount: Int = 0,
    val avgScrollSpeed: Double = 0.0,
    val slowScrollEvents: Int = 0
)

/**
 * Composable that monitors list performance metrics.
 * Tracks scroll performance, recomposition frequency, and slow rendering.
 * 
 * Requirements: 2.1, 2.2
 * 
 * @param listState The LazyListState to monitor
 * @param listName Name of the list for logging
 * @param enabled Whether monitoring is enabled
 * @param onMetricsUpdate Callback when metrics are updated
 */
@Composable
fun ListPerformanceMonitor(
    listState: LazyListState,
    listName: String,
    enabled: Boolean = true,
    onMetricsUpdate: ((ListPerformanceMetrics) -> Unit)? = null
) {
    if (!enabled) return
    
    var metrics by remember { mutableStateOf(ListPerformanceMetrics()) }
    var lastScrollTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var lastScrollIndex by remember { mutableStateOf(0) }
    
    // Monitor scroll performance
    LaunchedEffect(listState) {
        snapshotFlow { 
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val scrollOffset = listState.firstVisibleItemScrollOffset
            Triple(firstVisibleIndex, scrollOffset, System.currentTimeMillis())
        }
            .distinctUntilChanged()
            .collect { (currentIndex, _, currentTime) ->
                val timeDelta = currentTime - lastScrollTime
                
                if (timeDelta > 0) {
                    val indexDelta = kotlin.math.abs(currentIndex - lastScrollIndex)
                    val scrollSpeed = (indexDelta.toDouble() / timeDelta) * 1000 // items per second
                    
                    // Detect slow scroll (< 30 FPS equivalent)
                    val isSlowScroll = timeDelta > 33 && indexDelta > 0 // More than 33ms per item
                    
                    metrics = metrics.copy(
                        scrollEvents = metrics.scrollEvents + 1,
                        avgScrollSpeed = (metrics.avgScrollSpeed * metrics.scrollEvents + scrollSpeed) / (metrics.scrollEvents + 1),
                        slowScrollEvents = if (isSlowScroll) metrics.slowScrollEvents + 1 else metrics.slowScrollEvents
                    )
                    
                    // Log slow scroll events
                    if (isSlowScroll) {
                        Timber.tag("ListPerformance").w(
                            "$listName: Slow scroll detected - ${timeDelta}ms for $indexDelta items"
                        )
                    }
                    
                    onMetricsUpdate?.invoke(metrics)
                }
                
                lastScrollTime = currentTime
                lastScrollIndex = currentIndex
            }
    }
    
    // Monitor recomposition frequency
    LaunchedEffect(Unit) {
        metrics = metrics.copy(recompositionCount = metrics.recompositionCount + 1)
        
        // Log excessive recompositions
        if (metrics.recompositionCount > 100) {
            Timber.tag("ListPerformance").w(
                "$listName: High recomposition count - ${metrics.recompositionCount}"
            )
        }
    }
    
    // Log performance summary periodically
    LaunchedEffect(metrics.scrollEvents) {
        if (metrics.scrollEvents > 0 && metrics.scrollEvents % 50 == 0) {
            Timber.tag("ListPerformance").d(
                "$listName Performance Summary:\n" +
                "  Scroll Events: ${metrics.scrollEvents}\n" +
                "  Avg Scroll Speed: ${"%.2f".format(metrics.avgScrollSpeed)} items/sec\n" +
                "  Slow Scroll Events: ${metrics.slowScrollEvents}\n" +
                "  Recompositions: ${metrics.recompositionCount}"
            )
        }
    }
}

/**
 * Extension function to get performance metrics from LazyListState.
 * 
 * Requirements: 2.1, 2.2
 */
fun LazyListState.getPerformanceInfo(): String {
    val layoutInfo = this.layoutInfo
    return """
        List Performance Info:
        - Total Items: ${layoutInfo.totalItemsCount}
        - Visible Items: ${layoutInfo.visibleItemsInfo.size}
        - First Visible Index: $firstVisibleItemIndex
        - Viewport Size: ${layoutInfo.viewportSize.height}px
    """.trimIndent()
}
