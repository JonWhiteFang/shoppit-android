package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.error.AppError
import com.shoppit.app.domain.model.ItemCategory
import com.shoppit.app.domain.model.ShoppingListItem
import com.shoppit.app.domain.repository.ShoppingListRepository
import javax.inject.Inject

/**
 * Use case for processing voice input and adding items to the shopping list.
 * Parses voice text to extract item name, quantity, and unit.
 */
class ProcessVoiceInputUseCase @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository
) {
    /**
     * Process voice input text and add the parsed item to the shopping list.
     * Supports patterns like "add 2 pounds of chicken" or just "milk".
     * @param voiceText The recognized voice input text
     * @return Result containing the new item ID
     */
    suspend operator fun invoke(voiceText: String): Result<Long> {
        val parsed = parseVoiceInput(voiceText)
        
        if (parsed.itemName.isBlank()) {
            return Result.failure(AppError.ValidationError("Could not understand item name"))
        }
        
        val item = ShoppingListItem(
            name = parsed.itemName,
            quantity = parsed.quantity,
            unit = parsed.unit,
            category = ItemCategory.OTHER,
            isManual = true,
            storeSection = ItemCategory.OTHER.name
        )
        
        return shoppingListRepository.addShoppingListItem(item)
    }
    
    /**
     * Parse voice input text to extract item details.
     * Simple parsing logic that can be enhanced with NLP in the future.
     */
    private fun parseVoiceInput(text: String): ParsedVoiceInput {
        val words = text.lowercase().trim().split(Regex("\\s+"))
        
        // Look for "add" keyword
        val addIndex = words.indexOf("add")
        
        if (addIndex == -1) {
            // No "add" keyword, treat entire text as item name
            return ParsedVoiceInput(itemName = text.trim())
        }
        
        // Process words after "add"
        val afterAdd = words.drop(addIndex + 1)
        
        if (afterAdd.isEmpty()) {
            return ParsedVoiceInput(itemName = "")
        }
        
        // Try to parse quantity (first word after "add")
        val firstWord = afterAdd.first()
        val quantity = firstWord.toIntOrNull()?.toString()
        
        if (quantity != null) {
            // Has quantity, check for unit
            val remainingWords = afterAdd.drop(1)
            
            if (remainingWords.isEmpty()) {
                return ParsedVoiceInput(itemName = "", quantity = quantity)
            }
            
            // Check if second word is a unit
            val potentialUnit = remainingWords.first()
            val isUnit = isCommonUnit(potentialUnit)
            
            val unit = if (isUnit) potentialUnit else ""
            val nameStart = if (isUnit) 2 else 1
            
            // Skip "of" if present
            val nameWords = remainingWords.drop(nameStart - 1)
            val finalNameWords = if (nameWords.firstOrNull() == "of") {
                nameWords.drop(1)
            } else {
                nameWords
            }
            
            val itemName = finalNameWords.joinToString(" ")
            
            return ParsedVoiceInput(
                itemName = itemName,
                quantity = quantity,
                unit = unit
            )
        } else {
            // No quantity, entire text after "add" is item name
            val itemName = afterAdd.joinToString(" ")
            return ParsedVoiceInput(itemName = itemName)
        }
    }
    
    /**
     * Check if a word is a common unit of measurement.
     */
    private fun isCommonUnit(word: String): Boolean {
        val commonUnits = setOf(
            "pound", "pounds", "lb", "lbs",
            "ounce", "ounces", "oz",
            "gram", "grams", "g",
            "kilogram", "kilograms", "kg",
            "cup", "cups",
            "tablespoon", "tablespoons", "tbsp",
            "teaspoon", "teaspoons", "tsp",
            "liter", "liters", "l",
            "milliliter", "milliliters", "ml",
            "gallon", "gallons", "gal",
            "quart", "quarts", "qt",
            "pint", "pints", "pt",
            "piece", "pieces", "pc", "pcs",
            "can", "cans",
            "bottle", "bottles",
            "jar", "jars",
            "box", "boxes",
            "bag", "bags",
            "package", "packages", "pkg"
        )
        return word in commonUnits
    }
    
    /**
     * Data class for parsed voice input components.
     */
    private data class ParsedVoiceInput(
        val itemName: String,
        val quantity: String = "1",
        val unit: String = ""
    )
}
