package com.shoppit.app.util

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule

/**
 * Base class for Repository unit tests.
 * 
 * This class provides:
 * - Main dispatcher rule for coroutine testing
 * - Consistent test setup for all Repository tests
 * 
 * Usage:
 * ```
 * @ExperimentalCoroutinesApi
 * class MealRepositoryImplTest : RepositoryTest() {
 *     
 *     private lateinit var repository: MealRepositoryImpl
 *     private lateinit var mealDao: MealDao
 *     
 *     @Before
 *     fun setUp() {
 *         mealDao = mockk()
 *         repository = MealRepositoryImpl(mealDao)
 *     }
 *     
 *     @Test
 *     fun testGetMeals() = runTest {
 *         // Test code
 *     }
 * }
 * ```
 */
@ExperimentalCoroutinesApi
abstract class RepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
}
