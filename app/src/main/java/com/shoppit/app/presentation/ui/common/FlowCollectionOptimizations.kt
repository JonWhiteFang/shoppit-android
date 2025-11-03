package com.shoppit.app.presentation.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Flow collection optimizations for performance (Task 7.3).
 * Ensures proper lifecycle-aware collection and prevents duplicate collections.
 * 
 * Requirements: 6.4
 */

/**
 * Collects a StateFlow with lifecycle awareness.
 * Only collects when the lifecycle is at least STARTED.
 * This is the recommended way to collect StateFlow in composables.
 * 
 * Note: This is already the default behavior of collectAsState(),
 * but this function makes it explicit and adds lifecycle control.
 * 
 * @param minActiveState Minimum lifecycle state for collection
 * @param context Coroutine context for collection
 * @return State object with the current value
 */
@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycle(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext
): State<T> {
    return remember(this, lifecycleOwner, minActiveState, context) {
        this.flowWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            minActiveState = minActiveState
        )
    }.collectAsState(initial = this.value, context = context)
}

/**
 * Collects a Flow with lifecycle awareness.
 * Only collects when the lifecycle is at least STARTED.
 * 
 * @param initialValue Initial value before first emission
 * @param minActiveState Minimum lifecycle state for collection
 * @param context Coroutine context for collection
 * @return State object with the current value
 */
@Composable
fun <T> Flow<T>.collectAsStateWithLifecycle(
    initialValue: T,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    context: CoroutineContext = EmptyCoroutineContext
): State<T> {
    return remember(this, lifecycleOwner, minActiveState, context) {
        this.flowWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            minActiveState = minActiveState
        )
    }.collectAsState(initial = initialValue, context = context)
}

/**
 * Collects a SharedFlow for one-time events with lifecycle awareness.
 * Use this for events like navigation, snackbar messages, etc.
 * 
 * Example usage:
 * ```
 * CollectSharedFlowWithLifecycle(viewModel.errorEvent) { event ->
 *     when (event) {
 *         is ErrorEvent.Error -> snackbarHostState.showSnackbar(event.message)
 *         is ErrorEvent.Success -> snackbarHostState.showSnackbar(event.message)
 *     }
 * }
 * ```
 * 
 * @param flow The SharedFlow to collect
 * @param minActiveState Minimum lifecycle state for collection
 * @param onEvent Callback when an event is emitted
 */
@Composable
fun <T> CollectSharedFlowWithLifecycle(
    flow: SharedFlow<T>,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    onEvent: suspend (T) -> Unit
) {
    LaunchedEffect(flow, lifecycleOwner, minActiveState) {
        flow.flowWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            minActiveState = minActiveState
        ).collect { event ->
            onEvent(event)
        }
    }
}

/**
 * Best practices for Flow collection in composables:
 * 
 * 1. Use collectAsState() for StateFlow:
 *    ```
 *    val uiState by viewModel.uiState.collectAsState()
 *    ```
 * 
 * 2. Use LaunchedEffect + collect for SharedFlow (one-time events):
 *    ```
 *    LaunchedEffect(Unit) {
 *        viewModel.errorEvent.collect { event ->
 *            // Handle event
 *        }
 *    }
 *    ```
 * 
 * 3. Avoid collecting the same Flow multiple times:
 *    - Collect once at the top level
 *    - Pass the collected value down to child composables
 * 
 * 4. Use lifecycle-aware collection:
 *    - collectAsState() is lifecycle-aware by default
 *    - For manual collection, use flowWithLifecycle()
 * 
 * 5. Use SharedFlow for one-time events:
 *    - Navigation events
 *    - Snackbar messages
 *    - Dialog triggers
 *    - Any event that should only be handled once
 * 
 * 6. Use StateFlow for state:
 *    - UI state (Loading, Success, Error)
 *    - Form data
 *    - Any state that should be observed continuously
 * 
 * 7. Avoid collecting in composable body:
 *    ```
 *    // BAD - Don't do this
 *    @Composable
 *    fun MyScreen(viewModel: MyViewModel) {
 *        viewModel.uiState.collect { state ->
 *            // This will cause issues
 *        }
 *    }
 *    
 *    // GOOD - Use collectAsState or LaunchedEffect
 *    @Composable
 *    fun MyScreen(viewModel: MyViewModel) {
 *        val uiState by viewModel.uiState.collectAsState()
 *        // Use uiState
 *    }
 *    ```
 * 
 * 8. Remember Flow transformations:
 *    ```
 *    val filteredFlow = remember(sourceFlow, filter) {
 *        sourceFlow.map { it.filter(filter) }
 *    }
 *    val filteredState by filteredFlow.collectAsState(initial = emptyList())
 *    ```
 */

/**
 * Example of optimized Flow collection pattern:
 * 
 * ```
 * @Composable
 * fun MyScreen(viewModel: MyViewModel) {
 *     // Collect StateFlow once
 *     val uiState by viewModel.uiState.collectAsState()
 *     
 *     // Collect SharedFlow for one-time events
 *     val snackbarHostState = remember { SnackbarHostState() }
 *     LaunchedEffect(Unit) {
 *         viewModel.errorEvent.collect { event ->
 *             when (event) {
 *                 is ErrorEvent.Error -> snackbarHostState.showSnackbar(event.message)
 *                 is ErrorEvent.Success -> snackbarHostState.showSnackbar(event.message)
 *             }
 *         }
 *     }
 *     
 *     // Pass collected state to child composables
 *     MyScreenContent(
 *         uiState = uiState,
 *         snackbarHostState = snackbarHostState
 *     )
 * }
 * 
 * @Composable
 * fun MyScreenContent(
 *     uiState: MyUiState,
 *     snackbarHostState: SnackbarHostState
 * ) {
 *     // Use uiState directly - no need to collect again
 *     when (uiState) {
 *         is MyUiState.Loading -> LoadingScreen()
 *         is MyUiState.Success -> SuccessScreen(uiState.data)
 *         is MyUiState.Error -> ErrorScreen(uiState.message)
 *     }
 * }
 * ```
 */

/**
 * Anti-patterns to avoid:
 * 
 * 1. Collecting the same Flow multiple times:
 *    ```
 *    // BAD
 *    @Composable
 *    fun MyScreen(viewModel: MyViewModel) {
 *        val state1 by viewModel.uiState.collectAsState()
 *        val state2 by viewModel.uiState.collectAsState() // Duplicate collection!
 *    }
 *    ```
 * 
 * 2. Collecting in nested composables:
 *    ```
 *    // BAD
 *    @Composable
 *    fun ParentScreen(viewModel: MyViewModel) {
 *        ChildScreen(viewModel) // Passing ViewModel down
 *    }
 *    
 *    @Composable
 *    fun ChildScreen(viewModel: MyViewModel) {
 *        val state by viewModel.uiState.collectAsState() // Collecting again!
 *    }
 *    
 *    // GOOD
 *    @Composable
 *    fun ParentScreen(viewModel: MyViewModel) {
 *        val state by viewModel.uiState.collectAsState()
 *        ChildScreen(state) // Pass state, not ViewModel
 *    }
 *    
 *    @Composable
 *    fun ChildScreen(state: MyUiState) {
 *        // Use state directly
 *    }
 *    ```
 * 
 * 3. Using StateFlow for one-time events:
 *    ```
 *    // BAD
 *    private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
 *    val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent
 *    
 *    // GOOD
 *    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
 *    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent
 *    ```
 * 
 * 4. Not using lifecycle-aware collection:
 *    ```
 *    // BAD - Collects even when app is in background
 *    LaunchedEffect(Unit) {
 *        viewModel.uiState.collect { state ->
 *            // This runs even when app is backgrounded
 *        }
 *    }
 *    
 *    // GOOD - Stops collection when app is backgrounded
 *    LaunchedEffect(Unit) {
 *        viewModel.uiState
 *            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
 *            .collect { state ->
 *                // This only runs when app is in foreground
 *            }
 *    }
 *    
 *    // BETTER - Use collectAsState (lifecycle-aware by default)
 *    val state by viewModel.uiState.collectAsState()
 *    ```
 */
