package com.shoppit.app.presentation.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.SharedFlow

/**
 * Composable that observes an ErrorEvent flow and displays snackbars accordingly.
 * 
 * Error messages are displayed with long duration and a dismiss action.
 * Success messages are displayed with short duration and auto-dismiss.
 * 
 * Requirements: 1.1, 9.1, 9.2, 9.3, 9.4
 * 
 * @param errorEventFlow The SharedFlow of ErrorEvent to observe
 * @param snackbarHostState The SnackbarHostState to use for displaying snackbars
 */
@Composable
fun ErrorSnackbarHandler(
    errorEventFlow: SharedFlow<ErrorEvent>,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(Unit) {
        errorEventFlow.collect { event ->
            when (event) {
                is ErrorEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long,
                        withDismissAction = true
                    )
                }
                is ErrorEvent.Success -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
}
