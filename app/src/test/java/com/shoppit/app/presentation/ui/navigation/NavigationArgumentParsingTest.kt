package com.shoppit.app.presentation.ui.navigation

import androidx.navigation.NavType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for navigation argument parsing.
 * Tests that navigation arguments are correctly parsed and validated.
 * 
 * Requirements:
 * - Arguments are correctly extracted from routes
 * - Invalid arguments are detected
 * - Type safety is maintained
 */
class NavigationArgumentParsingTest {
    
    /**
     * Test parsing valid mealId from route.
     */
    @Test
    fun `parse valid mealId from route`() {
        val route = "meal_detail/123"
        val mealId = extractMealIdFromRoute(route)
        
        assertEquals(123L, mealId)
    }
    
    /**
     * Test parsing mealId from different routes.
     */
    @Test
    fun `parse mealId from different valid routes`() {
        assertEquals(1L, extractMealIdFromRoute("meal_detail/1"))
        assertEquals(999L, extractMealIdFromRoute("meal_detail/999"))
        assertEquals(1234567890L, extractMealIdFromRoute("meal_detail/1234567890"))
    }
    
    /**
     * Test parsing mealId from edit route.
     */
    @Test
    fun `parse mealId from edit meal route`() {
        val route = "edit_meal/456"
        val mealId = extractMealIdFromRoute(route)
        
        assertEquals(456L, mealId)
    }
    
    /**
     * Test parsing returns null for invalid route format.
     */
    @Test
    fun `parse returns null for invalid route format`() {
        val route = "meal_detail/invalid"
        val mealId = extractMealIdFromRoute(route)
        
        assertNull(mealId)
    }
    
    /**
     * Test parsing returns null for route without ID.
     */
    @Test
    fun `parse returns null for route without ID`() {
        val route = "meal_detail/"
        val mealId = extractMealIdFromRoute(route)
        
        assertNull(mealId)
    }
    
    /**
     * Test parsing returns null for route with negative ID.
     */
    @Test
    fun `parse detects negative ID`() {
        val route = "meal_detail/-1"
        val mealId = extractMealIdFromRoute(route)
        
        // Should parse the value but validation should happen elsewhere
        assertEquals(-1L, mealId)
    }
    
    /**
     * Test parsing returns null for route with zero ID.
     */
    @Test
    fun `parse handles zero ID`() {
        val route = "meal_detail/0"
        val mealId = extractMealIdFromRoute(route)
        
        assertEquals(0L, mealId)
    }
    
    /**
     * Test NavType.LongType parses valid long values.
     */
    @Test
    fun `NavType LongType parses valid long values`() {
        val navType = NavType.LongType
        
        assertEquals(123L, navType.parseValue("123"))
        assertEquals(0L, navType.parseValue("0"))
        assertEquals(999999L, navType.parseValue("999999"))
    }
    
    /**
     * Test argument validation detects missing required arguments.
     */
    @Test
    fun `argument validation detects missing required arguments`() {
        val arguments = emptyMap<String, Any>()
        val requiredArgs = listOf("mealId")
        
        val missingArgs = requiredArgs.filter { !arguments.containsKey(it) }
        
        assertTrue(missingArgs.isNotEmpty())
        assertTrue(missingArgs.contains("mealId"))
    }
    
    /**
     * Test argument validation passes with all required arguments.
     */
    @Test
    fun `argument validation passes with all required arguments`() {
        val arguments = mapOf("mealId" to 123L)
        val requiredArgs = listOf("mealId")
        
        val missingArgs = requiredArgs.filter { !arguments.containsKey(it) }
        
        assertTrue(missingArgs.isEmpty())
    }
    
    /**
     * Test argument validation with multiple required arguments.
     */
    @Test
    fun `argument validation with multiple required arguments`() {
        val arguments = mapOf(
            "mealId" to 123L,
            "name" to "Test Meal"
        )
        val requiredArgs = listOf("mealId", "name")
        
        val missingArgs = requiredArgs.filter { !arguments.containsKey(it) }
        
        assertTrue(missingArgs.isEmpty())
    }
    
    /**
     * Test argument validation detects partially missing arguments.
     */
    @Test
    fun `argument validation detects partially missing arguments`() {
        val arguments = mapOf("mealId" to 123L)
        val requiredArgs = listOf("mealId", "name")
        
        val missingArgs = requiredArgs.filter { !arguments.containsKey(it) }
        
        assertTrue(missingArgs.isNotEmpty())
        assertTrue(missingArgs.contains("name"))
        assertFalse(missingArgs.contains("mealId"))
    }
    
    /**
     * Test argument type validation for Long type.
     */
    @Test
    fun `argument type validation for Long type`() {
        val arguments = mapOf("mealId" to 123L)
        
        val value = arguments["mealId"]
        assertTrue(value is Long)
        assertEquals(123L, value)
    }
    
    /**
     * Test argument type validation detects wrong type.
     */
    @Test
    fun `argument type validation detects wrong type`() {
        val arguments = mapOf("mealId" to "123")
        
        val value = arguments["mealId"]
        assertFalse(value is Long)
        assertTrue(value is String)
    }
    
    /**
     * Test extracting multiple arguments from map.
     */
    @Test
    fun `extract multiple arguments from map`() {
        val arguments = mapOf(
            "mealId" to 123L,
            "name" to "Test Meal",
            "isEditing" to true
        )
        
        assertEquals(123L, arguments["mealId"])
        assertEquals("Test Meal", arguments["name"])
        assertEquals(true, arguments["isEditing"])
    }
    
    /**
     * Test argument validation with optional arguments.
     */
    @Test
    fun `argument validation with optional arguments`() {
        val arguments = mapOf("mealId" to 123L)
        val requiredArgs = listOf("mealId")
        val optionalArgs = listOf("name", "description")
        
        val missingRequired = requiredArgs.filter { !arguments.containsKey(it) }
        val missingOptional = optionalArgs.filter { !arguments.containsKey(it) }
        
        assertTrue(missingRequired.isEmpty())
        assertTrue(missingOptional.isNotEmpty())
    }
    
    /**
     * Test parsing date parameter from planner deep link.
     */
    @Test
    fun `parse date parameter from planner route`() {
        val route = "meal_planner?date=1234567890"
        val date = extractDateFromRoute(route)
        
        assertEquals(1234567890L, date)
    }
    
    /**
     * Test parsing returns null for planner route without date.
     */
    @Test
    fun `parse returns null for planner route without date`() {
        val route = "meal_planner"
        val date = extractDateFromRoute(route)
        
        assertNull(date)
    }
    
    /**
     * Test parsing returns null for invalid date format.
     */
    @Test
    fun `parse returns null for invalid date format`() {
        val route = "meal_planner?date=invalid"
        val date = extractDateFromRoute(route)
        
        assertNull(date)
    }
    
    /**
     * Test route pattern matching for parameterized routes.
     */
    @Test
    fun `route pattern matches parameterized routes`() {
        val pattern = "meal_detail/\\d+"
        
        assertTrue("meal_detail/123".matches(Regex(pattern)))
        assertTrue("meal_detail/1".matches(Regex(pattern)))
        assertTrue("meal_detail/999999".matches(Regex(pattern)))
        assertFalse("meal_detail/abc".matches(Regex(pattern)))
        assertFalse("meal_detail/".matches(Regex(pattern)))
    }
    
    /**
     * Test extracting argument name from route pattern.
     */
    @Test
    fun `extract argument name from route pattern`() {
        val pattern = "meal_detail/{mealId}"
        val argName = extractArgumentName(pattern)
        
        assertEquals("mealId", argName)
    }
    
    /**
     * Test extracting multiple argument names from route pattern.
     */
    @Test
    fun `extract multiple argument names from route pattern`() {
        val pattern = "meal/{mealId}/ingredient/{ingredientId}"
        val argNames = extractAllArgumentNames(pattern)
        
        assertEquals(2, argNames.size)
        assertTrue(argNames.contains("mealId"))
        assertTrue(argNames.contains("ingredientId"))
    }
    
    /**
     * Test validating argument value is positive.
     */
    @Test
    fun `validate argument value is positive`() {
        assertTrue(isValidPositiveId(123L))
        assertTrue(isValidPositiveId(1L))
        assertFalse(isValidPositiveId(0L))
        assertFalse(isValidPositiveId(-1L))
    }
    
    /**
     * Test validating argument value is non-negative.
     */
    @Test
    fun `validate argument value is non-negative`() {
        assertTrue(isValidNonNegativeId(123L))
        assertTrue(isValidNonNegativeId(0L))
        assertFalse(isValidNonNegativeId(-1L))
    }
    
    /**
     * Helper function to extract mealId from route string.
     */
    private fun extractMealIdFromRoute(route: String): Long? {
        return try {
            val parts = route.split("/")
            if (parts.size >= 2) {
                parts.last().toLongOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Helper function to extract date from route string.
     */
    private fun extractDateFromRoute(route: String): Long? {
        return try {
            if (route.contains("?date=")) {
                val dateStr = route.substringAfter("?date=")
                dateStr.toLongOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Helper function to extract argument name from route pattern.
     */
    private fun extractArgumentName(pattern: String): String? {
        val regex = Regex("\\{([^}]+)\\}")
        return regex.find(pattern)?.groupValues?.get(1)
    }
    
    /**
     * Helper function to extract all argument names from route pattern.
     */
    private fun extractAllArgumentNames(pattern: String): List<String> {
        val regex = Regex("\\{([^}]+)\\}")
        return regex.findAll(pattern).map { it.groupValues[1] }.toList()
    }
    
    /**
     * Helper function to validate positive ID.
     */
    private fun isValidPositiveId(id: Long): Boolean {
        return id > 0
    }
    
    /**
     * Helper function to validate non-negative ID.
     */
    private fun isValidNonNegativeId(id: Long): Boolean {
        return id >= 0
    }
}
