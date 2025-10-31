package com.shoppit.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.util.RepositoryTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GetMealsUseCase.
 * Tests retrieval of meals from repository.
 */
@ExperimentalCoroutinesApi
class GetMealsUseCaseTest : RepositoryTest() {

    private lateinit var repository: FakeMealRepository
    private lateinit var useCase: GetMealsUseCase

    @Before
    fun setUp() {
        repository = FakeMealRepository()
        useCase = GetMealsUseCase(repository)
    }

    @Test
    fun `returns meals from repository`() = runTest {
        // Given
        val meals = listOf(
            Meal(
                id = 1,
                name = "Pasta Carbonara",
                ingredients = listOf(Ingredient(name = "Pasta"))
            ),
            Meal(
                id = 2,
                name = "Caesar Salad",
                ingredients = listOf(Ingredient(name = "Lettuce"))
            )
        )
        repository.setMeals(meals)

        // When
        val result = useCase().first()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.size)
        assertEquals("Pasta Carbonara", result.getOrNull()?.get(0)?.name)
        assertEquals("Caesar Salad", result.getOrNull()?.get(1)?.name)
    }

    @Test
    fun `returns empty list when no meals exist`() = runTest {
        // Given
        repository.setMeals(emptyList())

        // When
        val result = useCase().first()

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        // Given
        repository.setShouldFail(true, Exception("Database error"))

        // When
        val result = useCase().first()

        // Then
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }
}
