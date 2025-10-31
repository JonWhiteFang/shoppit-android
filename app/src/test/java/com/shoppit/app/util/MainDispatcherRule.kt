package com.shoppit.app.util

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule for setting up the Main dispatcher for coroutine testing.
 * 
 * This rule replaces the Main dispatcher with a TestDispatcher,
 * allowing coroutines to be tested synchronously.
 * 
 * Usage:
 * ```
 * @ExperimentalCoroutinesApi
 * class MyViewModelTest {
 *     @get:Rule
 *     val mainDispatcherRule = MainDispatcherRule()
 *     
 *     @Test
 *     fun testCoroutine() = runTest {
 *         // Test code with coroutines
 *     }
 * }
 * ```
 */
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
