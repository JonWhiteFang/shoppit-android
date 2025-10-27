package com.shoppit.app.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule

/**
 * Base class for ViewModel unit tests.
 * 
 * This class provides:
 * - Main dispatcher rule for coroutine testing
 * - Consistent test setup for all ViewModel tests
 * 
 * Usage:
 * ```
 * @ExperimentalCoroutinesApi
 * class MealViewModelTest : ViewModelTest() {
 *     
 *     private lateinit var viewModel: MealViewModel
 *     private lateinit var getMealsUseCase: GetMealsUseCase
 *     
 *     @Before
 *     fun setUp() {
 *         getMealsUseCase = mockk()
 *         viewModel = MealViewModel(getMealsUseCase)
 *     }
 *     
 *     @Test
 *     fun testLoadMeals() = runTest {
 *         // Test code
 *     }
 * }
 * ```
 */
@ExperimentalCoroutinesApi
abstract class ViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
}
