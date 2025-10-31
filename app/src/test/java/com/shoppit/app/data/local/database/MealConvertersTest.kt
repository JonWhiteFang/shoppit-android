package com.shoppit.app.data.local.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.shoppit.app.domain.model.MealTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MealConverters, specifically the MealTag conversion methods.
 * Tests serialization and deserialization of MealTag sets to/from strings.
 *
 * Requirements:
 * - 5.1: Perform all search operations using locally stored meal data
 * - 5.2: Perform all filter operations using locally stored meal data
 */
class MealConvertersTest {

    private lateinit var converter: MealConverters

    @Before
    fun setUp() {
        converter = MealConverters()
    }

    @Test
    fun `toTagString converts single tag to string`() {
        // Given
        val tags = setOf(MealTag.DINNER)

        // When
        val result = converter.toTagString(tags)

        // Then
        assertEquals("DINNER", result)
    }

    @Test
    fun `toTagString converts multiple tags to comma-separated string`() {
        // Given
        val tags = setOf(MealTag.LUNCH, MealTag.VEGETARIAN, MealTag.HEALTHY)

        // When
        val result = converter.toTagString(tags)

        // Then
        // Result should contain all tags separated by commas
        assertTrue(result.contains("LUNCH"))
        assertTrue(result.contains("VEGETARIAN"))
        assertTrue(result.contains("HEALTHY"))
        assertEquals(2, result.count { it == ',' }) // Two commas for three tags
    }

    @Test
    fun `toTagString converts empty set to empty string`() {
        // Given
        val tags = emptySet<MealTag>()

        // When
        val result = converter.toTagString(tags)

        // Then
        assertEquals("", result)
    }

    @Test
    fun `toTagString preserves all tag types`() {
        // Given - all possible tags
        val tags = setOf(
            MealTag.BREAKFAST,
            MealTag.LUNCH,
            MealTag.DINNER,
            MealTag.SNACK,
            MealTag.VEGETARIAN,
            MealTag.VEGAN,
            MealTag.GLUTEN_FREE,
            MealTag.DAIRY_FREE,
            MealTag.QUICK,
            MealTag.HEALTHY,
            MealTag.COMFORT_FOOD,
            MealTag.DESSERT
        )

        // When
        val result = converter.toTagString(tags)

        // Then - all tags should be present
        assertTrue(result.contains("BREAKFAST"))
        assertTrue(result.contains("LUNCH"))
        assertTrue(result.contains("DINNER"))
        assertTrue(result.contains("SNACK"))
        assertTrue(result.contains("VEGETARIAN"))
        assertTrue(result.contains("VEGAN"))
        assertTrue(result.contains("GLUTEN_FREE"))
        assertTrue(result.contains("DAIRY_FREE"))
        assertTrue(result.contains("QUICK"))
        assertTrue(result.contains("HEALTHY"))
        assertTrue(result.contains("COMFORT_FOOD"))
        assertTrue(result.contains("DESSERT"))
    }

    @Test
    fun `fromTagString converts single tag string to set`() {
        // Given
        val tagString = "DINNER"

        // When
        val result = converter.fromTagString(tagString)

        // Then
        assertEquals(1, result.size)
        assertTrue(result.contains(MealTag.DINNER))
    }

    @Test
    fun `fromTagString converts comma-separated string to set of tags`() {
        // Given
        val tagString = "LUNCH,VEGETARIAN,HEALTHY"

        // When
        val result = converter.fromTagString(tagString)

        // Then
        assertEquals(3, result.size)
        assertTrue(result.contains(MealTag.LUNCH))
        assertTrue(result.contains(MealTag.VEGETARIAN))
        assertTrue(result.contains(MealTag.HEALTHY))
    }

    @Test
    fun `fromTagString handles tags with spaces around commas`() {
        // Given
        val tagString = "LUNCH, VEGETARIAN, HEALTHY"

        // When
        val result = converter.fromTagString(tagString)

        // Then
        assertEquals(3, result.size)
        assertTrue(result.contains(MealTag.LUNCH))
        assertTrue(result.contains(MealTag.VEGETARIAN))
        assertTrue(result.contains(MealTag.HEALTHY))
    }

    @Test
    fun `fromTagString converts empty string to empty set`() {
        // Given
        val tagString = ""

        // When
        val result = converter.fromTagString(tagString)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `fromTagString converts null to empty set`() {
        // Given
        val tagString: String? = null

        // When
        val result = converter.fromTagString(tagString)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `fromTagString converts blank string to empty set`() {
        // Given
        val tagString = "   "

        // When
        val result = converter.fromTagString(tagString)

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `fromTagString ignores invalid tag names`() {
        // Given - string with valid and invalid tags
        val tagString = "DINNER,INVALID_TAG,LUNCH,ANOTHER_INVALID"

        // When
        val result = converter.fromTagString(tagString)

        // Then - should only include valid tags
        assertEquals(2, result.size)
        assertTrue(result.contains(MealTag.DINNER))
        assertTrue(result.contains(MealTag.LUNCH))
    }

    @Test
    fun `fromTagString handles all valid tags`() {
        // Given - string with all valid tags
        val tagString = "BREAKFAST,LUNCH,DINNER,SNACK,VEGETARIAN,VEGAN,GLUTEN_FREE,DAIRY_FREE,QUICK,HEALTHY,COMFORT_FOOD,DESSERT"

        // When
        val result = converter.fromTagString(tagString)

        // Then - should include all tags
        assertEquals(12, result.size)
        assertTrue(result.contains(MealTag.BREAKFAST))
        assertTrue(result.contains(MealTag.LUNCH))
        assertTrue(result.contains(MealTag.DINNER))
        assertTrue(result.contains(MealTag.SNACK))
        assertTrue(result.contains(MealTag.VEGETARIAN))
        assertTrue(result.contains(MealTag.VEGAN))
        assertTrue(result.contains(MealTag.GLUTEN_FREE))
        assertTrue(result.contains(MealTag.DAIRY_FREE))
        assertTrue(result.contains(MealTag.QUICK))
        assertTrue(result.contains(MealTag.HEALTHY))
        assertTrue(result.contains(MealTag.COMFORT_FOOD))
        assertTrue(result.contains(MealTag.DESSERT))
    }

    @Test
    fun `fromTagString handles only invalid tags`() {
        // Given - string with only invalid tags
        val tagString = "INVALID1,INVALID2,INVALID3"

        // When
        val result = converter.fromTagString(tagString)

        // Then - should return empty set
        assertTrue(result.isEmpty())
    }

    @Test
    fun `roundtrip conversion preserves tags`() {
        // Given
        val originalTags = setOf(MealTag.DINNER, MealTag.VEGETARIAN, MealTag.QUICK)

        // When - convert to string and back
        val tagString = converter.toTagString(originalTags)
        val resultTags = converter.fromTagString(tagString)

        // Then - should get back the same tags
        assertEquals(originalTags, resultTags)
    }

    @Test
    fun `roundtrip conversion with empty set`() {
        // Given
        val originalTags = emptySet<MealTag>()

        // When - convert to string and back
        val tagString = converter.toTagString(originalTags)
        val resultTags = converter.fromTagString(tagString)

        // Then - should get back empty set
        assertTrue(resultTags.isEmpty())
    }

    @Test
    fun `roundtrip conversion with single tag`() {
        // Given
        val originalTags = setOf(MealTag.BREAKFAST)

        // When - convert to string and back
        val tagString = converter.toTagString(originalTags)
        val resultTags = converter.fromTagString(tagString)

        // Then - should get back the same tag
        assertEquals(originalTags, resultTags)
    }

    @Test
    fun `roundtrip conversion with all tags`() {
        // Given - all possible tags
        val originalTags = setOf(
            MealTag.BREAKFAST,
            MealTag.LUNCH,
            MealTag.DINNER,
            MealTag.SNACK,
            MealTag.VEGETARIAN,
            MealTag.VEGAN,
            MealTag.GLUTEN_FREE,
            MealTag.DAIRY_FREE,
            MealTag.QUICK,
            MealTag.HEALTHY,
            MealTag.COMFORT_FOOD,
            MealTag.DESSERT
        )

        // When - convert to string and back
        val tagString = converter.toTagString(originalTags)
        val resultTags = converter.fromTagString(tagString)

        // Then - should get back all tags
        assertEquals(originalTags, resultTags)
    }

    @Test
    fun `fromTagString is case sensitive`() {
        // Given - lowercase tag names (invalid)
        val tagString = "dinner,lunch"

        // When
        val result = converter.fromTagString(tagString)

        // Then - should return empty set (case-sensitive enum matching)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `fromTagString handles mixed valid and invalid with spaces`() {
        // Given
        val tagString = " DINNER , INVALID , LUNCH "

        // When
        val result = converter.fromTagString(tagString)

        // Then - should only include valid tags
        assertEquals(2, result.size)
        assertTrue(result.contains(MealTag.DINNER))
        assertTrue(result.contains(MealTag.LUNCH))
    }
}
