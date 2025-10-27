package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.Ingredient
import com.shoppit.app.domain.model.Meal
import com.shoppit.app.util.RepositoryTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for DeleteMealUseCase.
 * Tests deletion of meals from repository.
 */
@ExperimentalCoroutinesApi
class DeleteMealUseCaseTest : RepositoryTest() {

    private lateinit var repository: FakeMealRepository
    private lateinit var useCase: DeleteMealUseCase

    @Before
    fun setUp() {
        repository = FakeMealRepository()
        useCase = DeleteMealUseCase(repository)
    }

    @Test
    fun `calls repository to delete meal`() = runTest {
        // Given - existing meals in repository
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
        val result = useCase(1)

        // Then
        assertTrue(result.isSuccess)
        // Verify meal was deleted from repository
        assertEquals(1, repository.getMealsList().size)
        assertEquals(2L, repository.getMealsList()[0].id)
        assertEquals("Caesar Salad", repository.getMealsList()[0].name)
    }

    @Test
    fun `successfully deletes when meal exists`() = runTest {
        // Given
        val meal = Meal(
            id = 1,
            name = "Pasta Carbonara",
            ingredients = listOf(Ingredient(name = "Pasta"))
        )
        repository.setMeals(listOf(meal))

        // When
        val result = useCase(1)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(repository.getMealsList().isEmpty())
    }

    @Test
    fun `successfully completes when meal does not exist`() = runTest {
        // Given - empty repository
        repository.setMeals(emptyList())

        // When
        val result = useCase(999)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(repository.getMealsList().isEmpty())
    }

    @Test
    fun `returns error when repository fails`() = runTest {
        // Given
        val meal = Meal(
            id = 1,
            name = "Pasta Carbonara",
            ingredients = listOf(Ingredient(name = "Pasta"))
        )
        repository.setMeals(listOf(meal))
        repository.setShouldFail(true, Exception("Database error"))

        // When
        val result = useCase(1)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
        // Verify meal was not deleted
        assertEquals(1, repository.getMealsList().size)
    }
}
