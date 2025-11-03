package com.shoppit.app.presentation.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.StateFlow

/**
 * State batching optimizations for performance (Task 7.4).
 * Implements debouncing, throttling, and batching for frequent state updates.
 * 
 * Requirements: 6.5
 */

/**
 * Debounces a StateFlow to reduce update frequency.
 * Waits for a pause in updates before emitting the latest value.
 * 
 * Use for: Search input, text fields, user typing
 * 
 * Example:
 * ```
 * val searchQuery = MutableStateFlow("")
 * val debouncedQuery = searchQuery.debounce(300) // Wait 300ms after last change
 * ```
 * 
 * @param timeoutMillis Time to wait after last emission
 * @return Debounced flow
 */
@OptIn(FlowPreview::class)
fun <T> Flow<T>.debounce(timeoutMillis: Long): Flow<T> {
    return this.debounce(timeoutMillis)
}

/**
 * Throttles a StateFlow to limit update frequency.
 * Emits at most one value per time period.
 * 
 * Use for: Scroll position, continuous updates, animations
 * 
 * Example:
 * ```
 * val scrollPosition = MutableStateFlow(0f)
 * val throttledPosition = scrollPosition.throttle(100) // Max 10 updates per second
 * ```
 * 
 * @param periodMillis Minimum time between emissions
 * @return Throttled flow
 */
fun <T> Flow<T>.throttle(periodMillis: Long): Flow<T> {
    return this.sample(periodMillis)
}

/**
 * Composable that debounces search input.
 * Only triggers search after user stops typing for the specified delay.
 * 
 * @param searchQuery Current search query
 * @param delayMillis Debounce delay in milliseconds
 * @param onDebouncedQuery Callback with debounced query
 */
@Composable
fun DebouncedSearchEffect(
    searchQuery: String,
    delayMillis: Long = 300,
    onDebouncedQuery: (String) -> Unit
) {
    LaunchedEffect(searchQuery) {
        snapshotFlow { searchQuery }
            .debounce(delayMillis)
            .distinctUntilChanged()
            .collect { debouncedQuery ->
                onDebouncedQuery(debouncedQuery)
            }
    }
}

/**
 * Composable that throttles scroll position updates.
 * Limits how often scroll position is processed.
 * 
 * @param scrollPosition Current scroll position
 * @param periodMillis Throttle period in milliseconds
 * @param onThrottledPosition Callback with throttled position
 */
@Composable
fun ThrottledScrollEffect(
    scrollPosition: Float,
    periodMillis: Long = 100,
    onThrottledPosition: (Float) -> Unit
) {
    LaunchedEffect(scrollPosition) {
        snapshotFlow { scrollPosition }
            .throttle(periodMillis)
            .distinctUntilChanged()
            .collect { throttledPosition ->
                onThrottledPosition(throttledPosition)
            }
    }
}

/**
 * State batcher for collecting multiple rapid updates into a single batch.
 * Useful for bulk operations like updating multiple items at once.
 * 
 * Example:
 * ```
 * class MyViewModel : ViewModel() {
 *     private val stateBatcher = StateBatcher<ItemUpdate>(
 *         batchSize = 10,
 *         timeoutMillis = 500
 *     ) { batch ->
 *         // Process batch of updates
 *         updateItems(batch)
 *     }
 *     
 *     fun updateItem(update: ItemUpdate) {
 *         stateBatcher.add(update)
 *     }
 * }
 * ```
 */
class StateBatcher<T>(
    private val batchSize: Int = 10,
    private val timeoutMillis: Long = 500,
    private val onBatch: suspend (List<T>) -> Unit
) {
    private val buffer = mutableListOf<T>()
    private var lastFlushTime = System.currentTimeMillis()
    
    suspend fun add(item: T) {
        buffer.add(item)
        
        val shouldFlush = buffer.size >= batchSize ||
                (System.currentTimeMillis() - lastFlushTime) >= timeoutMillis
        
        if (shouldFlush) {
            flush()
        }
    }
    
    suspend fun flush() {
        if (buffer.isNotEmpty()) {
            val batch = buffer.toList()
            buffer.clear()
            lastFlushTime = System.currentTimeMillis()
            onBatch(batch)
        }
    }
}

/**
 * Debounced state holder for ViewModel.
 * Automatically debounces state updates.
 * 
 * Example:
 * ```
 * class MyViewModel : ViewModel() {
 *     private val _searchQuery = DebouncedStateFlow(
 *         initialValue = "",
 *         delayMillis = 300,
 *         scope = viewModelScope
 *     ) { debouncedQuery ->
 *         // Perform search with debounced query
 *         performSearch(debouncedQuery)
 *     }
 *     
 *     fun updateSearchQuery(query: String) {
 *         _searchQuery.value = query
 *     }
 * }
 * ```
 */
class DebouncedStateFlow<T>(
    initialValue: T,
    private val delayMillis: Long = 300,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val onDebounced: suspend (T) -> Unit
) {
    private val _state = MutableStateFlow(initialValue)
    val state: StateFlow<T> = _state
    
    var value: T
        get() = _state.value
        set(value) {
            _state.value = value
        }
    
    init {
        scope.kotlinx.coroutines.launch {
            _state
                .debounce(delayMillis)
                .distinctUntilChanged()
                .collect { debouncedValue ->
                    onDebounced(debouncedValue)
                }
        }
    }
}

/**
 * Best practices for state batching:
 * 
 * 1. Use debounce for search input:
 *    - Prevents excessive API calls
 *    - Waits for user to finish typing
 *    - Typical delay: 300-500ms
 * 
 * 2. Use throttle for scroll position:
 *    - Limits update frequency
 *    - Prevents performance issues
 *    - Typical period: 100-200ms
 * 
 * 3. Batch multiple updates:
 *    - Collect rapid updates into batches
 *    - Process batches together
 *    - Reduces database/network calls
 * 
 * 4. Use distinctUntilChanged:
 *    - Prevents duplicate updates
 *    - Reduces unnecessary recompositions
 *    - Always combine with debounce/throttle
 * 
 * 5. Choose appropriate delays:
 *    - Search: 300-500ms (wait for typing pause)
 *    - Scroll: 100-200ms (smooth but not excessive)
 *    - Auto-save: 1000-2000ms (balance between UX and performance)
 */

/**
 * Example usage patterns:
 * 
 * 1. Debounced search in ViewModel:
 * ```
 * class MealViewModel : ViewModel() {
 *     private val _searchQuery = MutableStateFlow("")
 *     val searchQuery: StateFlow<String> = _searchQuery
 *     
 *     init {
 *         viewModelScope.launch {
 *             _searchQuery
 *                 .debounce(300)
 *                 .distinctUntilChanged()
 *                 .collect { query ->
 *                     performSearch(query)
 *                 }
 *         }
 *     }
 *     
 *     fun updateSearchQuery(query: String) {
 *         _searchQuery.value = query
 *     }
 * }
 * ```
 * 
 * 2. Throttled scroll tracking:
 * ```
 * @Composable
 * fun MyList(items: List<Item>) {
 *     val listState = rememberLazyListState()
 *     
 *     ThrottledScrollEffect(
 *         scrollPosition = listState.firstVisibleItemScrollOffset.toFloat(),
 *         periodMillis = 100
 *     ) { position ->
 *         // Track scroll position (max 10 times per second)
 *         analytics.trackScroll(position)
 *     }
 *     
 *     LazyColumn(state = listState) {
 *         items(items) { item ->
 *             ItemCard(item)
 *         }
 *     }
 * }
 * ```
 * 
 * 3. Batched item updates:
 * ```
 * class ShoppingListViewModel : ViewModel() {
 *     private val updateBatcher = StateBatcher<ItemUpdate>(
 *         batchSize = 10,
 *         timeoutMillis = 500
 *     ) { batch ->
 *         repository.updateItems(batch)
 *     }
 *     
 *     fun updateItem(itemId: Long, isChecked: Boolean) {
 *         viewModelScope.launch {
 *             updateBatcher.add(ItemUpdate(itemId, isChecked))
 *         }
 *     }
 * }
 * ```
 * 
 * 4. Debounced text field:
 * ```
 * @Composable
 * fun SearchBar(
 *     query: String,
 *     onQueryChange: (String) -> Unit,
 *     onSearch: (String) -> Unit
 * ) {
 *     var localQuery by remember { mutableStateOf(query) }
 *     
 *     DebouncedSearchEffect(
 *         searchQuery = localQuery,
 *         delayMillis = 300
 *     ) { debouncedQuery ->
 *         onSearch(debouncedQuery)
 *     }
 *     
 *     TextField(
 *         value = localQuery,
 *         onValueChange = { newQuery ->
 *             localQuery = newQuery
 *             onQueryChange(newQuery) // Update immediately for UI
 *         }
 *     )
 * }
 * ```
 */

/**
 * Performance considerations:
 * 
 * 1. Debounce delay trade-offs:
 *    - Too short: Still too many updates
 *    - Too long: Feels unresponsive
 *    - Sweet spot: 300-500ms for search
 * 
 * 2. Throttle period trade-offs:
 *    - Too short: Doesn't reduce load enough
 *    - Too long: Updates feel choppy
 *    - Sweet spot: 100-200ms for scroll
 * 
 * 3. Batch size trade-offs:
 *    - Too small: Doesn't reduce overhead
 *    - Too large: Delays updates too much
 *    - Sweet spot: 10-50 items depending on operation
 * 
 * 4. Memory considerations:
 *    - Batching uses memory to buffer updates
 *    - Set reasonable batch size limits
 *    - Flush batches on timeout to prevent memory buildup
 * 
 * 5. When NOT to batch:
 *    - Critical user actions (delete, save)
 *    - Navigation events
 *    - Error states
 *    - Any action requiring immediate feedback
 */
