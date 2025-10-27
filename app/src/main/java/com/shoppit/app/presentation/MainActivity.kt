package com.shoppit.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.shoppit.app.presentation.ui.navigation.ShoppitNavHost
import com.shoppit.app.presentation.ui.theme.ShoppitTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the Shoppit app.
 * Sets up the navigation and theme for the entire application.
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
                    ShoppitNavHost(navController = navController)
                }
            }
        }
    }
}