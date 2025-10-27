package com.shoppit.app.data.performance

import kotlin.system.measureTimeMillis

/**
 * Utility functions for measuring and tracking performance.
 */

/**
 * Executes a block of code and tracks its execution time as a query.
 *
 * @param monitor The performance monitor to track with
 * @param queryName The name of the query/operation
 * @param block The code block to execute
 * @return The result of the block execution
 */
inline fun <T> trackQuery(
    monitor: PerformanceMonitor,
    queryName: String,
    block: () -> T
): T {
    var result: T
    val duration = measureTimeMillis {
        result = block()
    }
    monitor.trackQuery(queryName, duration)
    return result
}

/**
 * Executes a suspend block of code and tracks its execution time as a query.
 *
 * @param monitor The performance monitor to track with
 * @param queryName The name of the query/operation
 * @param block The suspend code block to execute
 * @return The result of the block execution
 */
suspend inline fun <T> trackQuerySuspend(
    monitor: PerformanceMonitor,
    queryName: String,
    crossinline block: suspend () -> T
): T {
    val startTime = System.currentTimeMillis()
    val result = block()
    val duration = System.currentTimeMillis() - startTime
    monitor.trackQuery(queryName, duration)
    return result
}

/**
 * Executes a block of code and tracks its execution time as a transaction.
 *
 * @param monitor The performance monitor to track with
 * @param operationName The name of the transaction operation
 * @param block The code block to execute
 * @return The result of the block execution
 */
inline fun <T> trackTransaction(
    monitor: PerformanceMonitor,
    operationName: String,
    block: () -> T
): T {
    var result: T
    val duration = measureTimeMillis {
        result = block()
    }
    monitor.trackTransaction(operationName, duration)
    return result
}

/**
 * Executes a suspend block of code and tracks its execution time as a transaction.
 *
 * @param monitor The performance monitor to track with
 * @param operationName The name of the transaction operation
 * @param block The suspend code block to execute
 * @return The result of the block execution
 */
suspend inline fun <T> trackTransactionSuspend(
    monitor: PerformanceMonitor,
    operationName: String,
    crossinline block: suspend () -> T
): T {
    val startTime = System.currentTimeMillis()
    val result = block()
    val duration = System.currentTimeMillis() - startTime
    monitor.trackTransaction(operationName, duration)
    return result
}
