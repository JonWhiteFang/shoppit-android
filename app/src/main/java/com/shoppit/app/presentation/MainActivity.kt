package com.shoppit.app.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shoppit.app.data.startup.StartupOptimizer
import com.shoppit.app.data.startup.StartupPhase
import com.shoppit.app.presentation.ui.navigation.MainScreen
import com.shoppit.app.presentation.ui.navigation.util.BackPressHandler
import com.shoppit.app.presentation.ui.navigation.util.DeepLinkHandler
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Main activity for the Shoppit app.
 * Sets up the navigation and theme for the entire application.
 * Handles deep links from external sources and notifications.
 *
 * Requirements:
 * - 1.1: Display first screen within 2000ms on cold start
 * - 1.2: Display first screen within 1000ms on warm start
 * - 1.3: Display first screen within 500ms on hot start
 * - 5.1: Back button pops navigation stack correctly on detail screens
 * - 5.2: Exit app when back is pressed on main screens
 * - 8.1: Navigate directly to specified screen from deep links
 * - 8.4: Handle deep links while app is running
 * - 8.5: Support deep links from external sources and notifications
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var startupOptimizer: StartupOptimizer
    
    private var activityStartTime: Long = 0L
    
    override fun onCreate(savedInstanceState: Bundle?) {
        activityStartTime = System.currentTimeMillis()
        super.onCreate(savedInstanceState)
        
        // Measure time to setContent
        val setContentStartTime = System.currentTimeMillis()
        
        setContent {
            ShoppitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Use rememberSaveable for state restoration
                    var deepLinkIntent by rememberSaveable(stateSaver = IntentSaver) {
                        mutableStateOf(intent)
                    }
                    
                    // Get current route for back press handling
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    
                    // Defer non-critical UI setup to LaunchedEffect
                    LaunchedEffect(Unit) {
                        // Track first frame time
                        val firstFrameTime = System.currentTimeMillis() - activityStartTime
                        startupOptimizer.trackStartupPhase(StartupPhase.FIRST_FRAME, firstFrameTime)
                        Timber.d("First frame rendered in ${firstFrameTime}ms")
                        
                        // Preload first screen data if needed
                        // This runs after first frame is rendered
                        preloadFirstScreenData()
                    }
                    
                    // Handle deep link on initial launch
                    LaunchedEffect(deepLinkIntent) {
                        if (deepLinkIntent?.data != null) {
                            DeepLinkHandler.handleDeepLink(deepLinkIntent, navController)
                        }
                    }
                    
                    // Requirement 5.1, 5.2: Consistent back button handling
                    BackHandler {
                        BackPressHandler.handleBackPress(
                            navController = navController,
                            currentRoute = currentRoute,
                            onExitApp = { finish() }
                        )
                    }
                    
                    MainScreen(navController = navController)
                }
            }
        }
        
        val setContentDuration = System.currentTimeMillis() - setContentStartTime
        Timber.d("setContent completed in ${setContentDuration}ms")
    }
    
    /**
     * Handle new intents when activity is already running (singleTask launch mode).
     * This allows deep links to work when the app is already open.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        // Trigger recomposition with new intent
        setContent {
            ShoppitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Get current route for back press handling
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    
                    // Handle deep link from new intent
                    LaunchedEffect(intent) {
                        if (intent.data != null) {
                            DeepLinkHandler.handleDeepLink(intent, navController)
                        }
                    }
                    
                    // Requirement 5.1, 5.2: Consistent back button handling
                    BackHandler {
                        BackPressHandler.handleBackPress(
                            navController = navController,
                            currentRoute = currentRoute,
                            onExitApp = { finish() }
                        )
                    }
                    
                    MainScreen(navController = navController)
                }
            }
        }
    }
    
    /**
     * Preloads data for the first screen to improve perceived performance.
     * This runs after the first frame is rendered to avoid blocking startup.
     */
    private suspend fun preloadFirstScreenData() {
        try {
            Timber.d("Preloading first screen data")
            // Preload meal list data, cache warming, etc.
            // This is deferred to not block first frame rendering
        } catch (e: Exception) {
            Timber.e(e, "Error preloading first screen data")
        }
    }
}

/**
 * Custom saver for Intent to support rememberSaveable.
 * Intents are not Parcelable in a way that works with rememberSaveable,
 * so we use a simple approach of not saving/restoring the intent.
 */
private val IntentSaver = androidx.compose.runtime.saveable.Saver<Intent?, String>(
    save = { null }, // Don't save intent
    restore = { null } // Don't restore intent
)