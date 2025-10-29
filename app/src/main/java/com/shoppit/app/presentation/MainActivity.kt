package com.shoppit.app.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.shoppit.app.presentation.ui.navigation.MainScreen
import com.shoppit.app.presentation.ui.navigation.util.DeepLinkHandler
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the Shoppit app.
 * Sets up the navigation and theme for the entire application.
 * Handles deep links from external sources and notifications.
 *
 * Requirements:
 * - 8.1: Navigate directly to specified screen from deep links
 * - 8.4: Handle deep links while app is running
 * - 8.5: Support deep links from external sources and notifications
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShoppitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var deepLinkIntent by remember { mutableStateOf(intent) }
                    
                    // Handle deep link on initial launch
                    LaunchedEffect(deepLinkIntent) {
                        if (deepLinkIntent?.data != null) {
                            DeepLinkHandler.handleDeepLink(deepLinkIntent, navController)
                        }
                    }
                    
                    MainScreen(navController = navController)
                }
            }
        }
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
                    
                    // Handle deep link from new intent
                    LaunchedEffect(intent) {
                        if (intent.data != null) {
                            DeepLinkHandler.handleDeepLink(intent, navController)
                        }
                    }
                    
                    MainScreen(navController = navController)
                }
            }
        }
    }
}