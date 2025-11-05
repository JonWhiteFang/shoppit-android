package com.shoppit.app.presentation.ui.navigation

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shoppit.app.presentation.ui.navigation.util.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Navigation Analytics Dashboard
 * 
 * Displays comprehensive navigation analytics including:
 * - Overall performance metrics
 * - Most viewed screens
 * - Common navigation paths
 * - Error rates and issues
 * - Performance scores
 * 
 * Requirements:
 * - 7.5: Monitor navigation performance metrics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationAnalyticsDashboard(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val analyticsState by NavigationAnalytics.analyticsState.collectAsState()
    val performanceScore by NavigationPerformanceAnalytics.performanceScore.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Screens", "Paths", "Performance", "Issues")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Navigation Analytics") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        NavigationAnalytics.reset()
                        NavigationPerformanceAnalytics.clearAllData()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Analytics")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Tab Content
            when (selectedTab) {
                0 -> OverviewTab(analyticsState, performanceScore)
                1 -> ScreensTab(analyticsState)
                2 -> PathsTab(analyticsState)
                3 -> PerformanceTab(performanceScore, context)
                4 -> IssuesTab()
            }
        }
    }
}

@Composable
private fun OverviewTab(
    analyticsState: AnalyticsState,
    performanceScore: PerformanceScore
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Session Summary
        item {
            SectionCard(title = "Session Summary") {
                MetricRow(
                    label = "Duration",
                    value = formatDuration(analyticsState.sessionDurationMs),
                    icon = Icons.Default.Timer
                )
                MetricRow(
                    label = "Total Navigations",
                    value = analyticsState.totalNavigations.toString(),
                    icon = Icons.Default.Navigation
                )
                MetricRow(
                    label = "Total Errors",
                    value = analyticsState.totalErrors.toString(),
                    icon = Icons.Default.Error,
                    valueColor = if (analyticsState.totalErrors > 0) 
                        MaterialTheme.colorScheme.error else Color.Unspecified
                )
                MetricRow(
                    label = "Error Rate",
                    value = "${"%.2f".format(analyticsState.errorRate)}%",
                    icon = Icons.Default.Warning,
                    valueColor = getErrorRateColor(analyticsState.errorRate)
                )
            }
        }
        
        // Performance Score
        item {
            SectionCard(title = "Performance Score") {
                PerformanceScoreDisplay(performanceScore)
            }
        }
        
        // Top Screens
        if (analyticsState.mostViewedScreens.isNotEmpty()) {
            item {
                SectionCard(title = "Most Viewed Screens") {
                    analyticsState.mostViewedScreens.take(3).forEach { (route, count) ->
                        MetricRow(
                            label = route,
                            value = "$count views",
                            icon = Icons.Default.Visibility
                        )
                    }
                }
            }
        }
        
        // Top Paths
        if (analyticsState.mostCommonPaths.isNotEmpty()) {
            item {
                SectionCard(title = "Common Navigation Paths") {
                    analyticsState.mostCommonPaths.take(3).forEach { (path, count) ->
                        MetricRow(
                            label = path,
                            value = "$count times",
                            icon = Icons.Default.Route
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreensTab(analyticsState: AnalyticsState) {
    val mostViewedScreens = remember(analyticsState) {
        NavigationAnalytics.getMostViewedScreens(20)
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Screen Views",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        if (mostViewedScreens.isEmpty()) {
            item {
                EmptyStateMessage("No screen views recorded yet")
            }
        } else {
            items(mostViewedScreens) { (route, count) ->
                ScreenViewCard(route, count)
            }
        }
    }
}

@Composable
private fun PathsTab(analyticsState: AnalyticsState) {
    val mostCommonPaths = remember(analyticsState) {
        NavigationAnalytics.getMostCommonPaths(20)
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Navigation Paths",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        if (mostCommonPaths.isEmpty()) {
            item {
                EmptyStateMessage("No navigation paths recorded yet")
            }
        } else {
            items(mostCommonPaths) { (path, count) ->
                NavigationPathCard(path, count)
            }
        }
    }
}

@Composable
private fun PerformanceTab(performanceScore: PerformanceScore, context: Context) {
    val timingMetrics = remember { NavigationMetrics() }
    val frameMetrics = remember { FrameMetrics() }
    val memoryMetrics = remember { MemoryMetrics() }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overall Performance Score
        item {
            SectionCard(title = "Overall Performance") {
                PerformanceScoreDisplay(performanceScore)
            }
        }
        
        // Timing Metrics
        item {
            SectionCard(title = "Timing Metrics") {
                MetricRow(
                    label = "Average Transition",
                    value = "${"%.0f".format(timingMetrics.averageTransitionTimeMs)}ms",
                    icon = Icons.Default.Timer,
                    valueColor = getTimingColor(timingMetrics.averageTransitionTimeMs)
                )
                MetricRow(
                    label = "Max Transition",
                    value = "${timingMetrics.maxTransitionTimeMs}ms",
                    icon = Icons.Default.SlowMotionVideo
                )
                MetricRow(
                    label = "Slow Transitions",
                    value = "${timingMetrics.slowTransitionCount} (${"%.1f".format(timingMetrics.slowTransitionPercentage)}%)",
                    icon = Icons.Default.Warning,
                    valueColor = if (timingMetrics.slowTransitionPercentage > 10f) Color(0xFFF44336) else Color.Unspecified
                )
                MetricRow(
                    label = "Total Transitions",
                    value = timingMetrics.totalTransitions.toString(),
                    icon = Icons.Default.SwapHoriz
                )
            }
        }
        
        // Frame Rate Metrics
        item {
            SectionCard(title = "Frame Rate Metrics") {
                MetricRow(
                    label = "Average FPS",
                    value = "${"%.1f".format(frameMetrics.averageFps)} fps",
                    icon = Icons.Default.Speed,
                    valueColor = getFrameRateColor(frameMetrics.averageFps)
                )
                MetricRow(
                    label = "Frame Drops",
                    value = "${"%.1f".format(frameMetrics.frameDropPercentage)}%",
                    icon = Icons.Default.Warning,
                    valueColor = getFrameDropColor(frameMetrics.frameDropPercentage)
                )
                MetricRow(
                    label = "Total Frames",
                    value = frameMetrics.totalFrames.toString(),
                    icon = Icons.Default.ViewModule
                )
            }
        }
        
        // Memory Metrics
        item {
            SectionCard(title = "Memory Metrics") {
                MetricRow(
                    label = "Memory Usage",
                    value = "${"%.1f".format(memoryMetrics.memoryUsagePercentage)}%",
                    icon = Icons.Default.Memory,
                    valueColor = getMemoryColor(memoryMetrics.memoryUsagePercentage)
                )
                MetricRow(
                    label = "Memory Delta",
                    value = formatBytes(memoryMetrics.memoryDeltaBytes),
                    icon = Icons.Default.TrendingUp
                )
                MetricRow(
                    label = "Max Memory",
                    value = formatBytes(memoryMetrics.maxMemoryBytes),
                    icon = Icons.Default.Storage
                )
                MetricRow(
                    label = "Used Memory",
                    value = formatBytes(memoryMetrics.usedMemoryBytes),
                    icon = Icons.Default.Memory
                )
            }
        }
    }
}

@Composable
private fun IssuesTab() {
    val recentIssues = remember {
        NavigationPerformanceAnalytics.getRecentIssues(20)
    }
    
    val screensWithErrors = remember {
        NavigationAnalytics.getScreensWithMostErrors(10)
    }
    
    val commonFailures = remember {
        NavigationAnalytics.getMostCommonFailures(10)
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Performance Issues
        item {
            Text(
                text = "Performance Issues",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        if (recentIssues.isEmpty()) {
            item {
                EmptyStateMessage("No performance issues detected")
            }
        } else {
            items(recentIssues) { issue ->
                PerformanceIssueCard(issue)
            }
        }
        
        // Screens with Errors
        if (screensWithErrors.isNotEmpty()) {
            item {
                Text(
                    text = "Screens with Most Errors",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            items(screensWithErrors) { (route, count) ->
                ErrorScreenCard(route, count)
            }
        }
        
        // Common Failures
        if (commonFailures.isNotEmpty()) {
            item {
                Text(
                    text = "Common Failure Types",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            items(commonFailures) { (type, count) ->
                FailureTypeCard(type, count)
            }
        }
    }
}

// Reusable Components

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.Unspecified
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun PerformanceScoreDisplay(score: PerformanceScore) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Overall Score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Overall Score",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${score.overallScore}/100",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = getScoreColor(score.overallScore)
                )
                PerformanceRatingBadge(score.rating)
            }
        }
        
        Divider()
        
        // Score Breakdown
        ScoreBar(label = "Timing", score = score.timingScore)
        ScoreBar(label = "Frame Rate", score = score.frameRateScore)
        ScoreBar(label = "Memory", score = score.memoryScore)
    }
}

@Composable
private fun ScoreBar(label: String, score: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$score/100",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = getScoreColor(score)
            )
        }
        LinearProgressIndicator(
            progress = score / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = getScoreColor(score),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun PerformanceRatingBadge(rating: PerformanceRating) {
    val (text, color) = when (rating) {
        PerformanceRating.EXCELLENT -> "Excellent" to Color(0xFF4CAF50)
        PerformanceRating.GOOD -> "Good" to Color(0xFF8BC34A)
        PerformanceRating.FAIR -> "Fair" to Color(0xFFFFC107)
        PerformanceRating.POOR -> "Poor" to Color(0xFFFF9800)
        PerformanceRating.CRITICAL -> "Critical" to Color(0xFFF44336)
        PerformanceRating.UNKNOWN -> "Unknown" to Color.Gray
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ScreenViewCard(route: String, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = route,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "$count views",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun NavigationPathCard(path: String, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "$count times",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun PerformanceIssueCard(issue: PerformanceIssue) {
    val severityColor = when (issue.severity) {
        IssueSeverity.HIGH -> Color(0xFFF44336)
        IssueSeverity.MEDIUM -> Color(0xFFFF9800)
        IssueSeverity.LOW -> Color(0xFFFFC107)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = severityColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = issue.type.name.replace("_", " "),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = severityColor
                    )
                }
                Text(
                    text = issue.severity.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = severityColor
                )
            }
            Text(
                text = issue.route,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = issue.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatTimestamp(issue.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorScreenCard(route: String, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = route,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "$count errors",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun FailureTypeCard(type: String, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = type,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "$count occurrences",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper Functions

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1 -> "${"%.2f".format(gb)} GB"
        mb >= 1 -> "${"%.2f".format(mb)} MB"
        kb >= 1 -> "${"%.2f".format(kb)} KB"
        else -> "$bytes B"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
private fun getScoreColor(score: Int): Color {
    return when {
        score >= 90 -> Color(0xFF4CAF50)
        score >= 75 -> Color(0xFF8BC34A)
        score >= 60 -> Color(0xFFFFC107)
        score >= 40 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

@Composable
private fun getErrorRateColor(rate: Float): Color {
    return when {
        rate < 1.0f -> Color(0xFF4CAF50)
        rate < 5.0f -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
}

@Composable
private fun getTimingColor(ms: Double): Color {
    return when {
        ms <= 300 -> Color(0xFF4CAF50)
        ms <= 500 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
}

@Composable
private fun getFrameRateColor(fps: Double): Color {
    return when {
        fps >= 55 -> Color(0xFF4CAF50)
        fps >= 45 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
}

@Composable
private fun getFrameDropColor(dropRate: Float): Color {
    return when {
        dropRate < 5.0f -> Color(0xFF4CAF50)
        dropRate < 10.0f -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
}

@Composable
private fun getMemoryColor(usage: Float): Color {
    return when {
        usage < 70f -> Color(0xFF4CAF50)
        usage < 85f -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
}

// Placeholder data classes for metrics
data class FrameMetrics(
    val averageFps: Double = 0.0,
    val frameDropPercentage: Float = 0f,
    val totalFrames: Int = 0
)

data class MemoryMetrics(
    val memoryUsagePercentage: Float = 0f,
    val memoryDeltaBytes: Long = 0L,
    val maxMemoryBytes: Long = 0L,
    val usedMemoryBytes: Long = 0L
)
