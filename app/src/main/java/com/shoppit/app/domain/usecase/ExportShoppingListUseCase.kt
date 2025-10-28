package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.model.ShoppingListItem
import com.shoppit.app.domain.repository.ShoppingListRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for exporting shopping list in various formats.
 * 
 * Supports text, CSV, and JSON export formats.
 */
class ExportShoppingListUseCase @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository
) {

    suspend operator fun invoke(params: Params): Result<String> {
        val result = shoppingListRepository.getShoppingList().first()
        
        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull() ?: Exception("Failed to get shopping list"))
        }
        
        val data = result.getOrNull() ?: return Result.failure(Exception("Shopping list data is null"))
        val items = data.itemsByCategory.flatMap { it.value }
            .filter { !it.isChecked } // Only export unchecked items
        
        return try {
            val exportResult = when (params.format) {
                ExportFormat.TEXT -> exportAsText(items)
                ExportFormat.CSV -> exportAsCsv(items)
                ExportFormat.JSON -> exportAsJson(items)
            }
            Result.success(exportResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports shopping list as plain text.
     */
    private fun exportAsText(items: List<ShoppingListItem>): String {
        return buildString {
            appendLine("Shopping List")
            appendLine("=" .repeat(50))
            appendLine()
            
            val itemsByCategory = items.groupBy { it.category }
            
            itemsByCategory.forEach { (category, categoryItems) ->
                appendLine(category.displayName())
                appendLine("-".repeat(category.displayName().length))
                
                categoryItems.forEach { item ->
                    append("  • ${item.name}")
                    if (item.quantity.isNotBlank() && item.unit.isNotBlank()) {
                        append(" (${item.quantity} ${item.unit})")
                    } else if (item.quantity.isNotBlank()) {
                        append(" (${item.quantity})")
                    }
                    
                    if (item.notes.isNotBlank()) {
                        append(" - ${item.notes}")
                    }
                    
                    if (item.estimatedPrice != null) {
                        append(" [$${String.format("%.2f", item.estimatedPrice)}]")
                    }
                    
                    appendLine()
                }
                
                appendLine()
            }
            
            appendLine("Total Items: ${items.size}")
            
            val totalPrice = items.mapNotNull { it.estimatedPrice }.sum()
            if (totalPrice > 0) {
                appendLine("Estimated Total: $${String.format("%.2f", totalPrice)}")
            }
        }
    }

    /**
     * Exports shopping list as CSV.
     */
    private fun exportAsCsv(items: List<ShoppingListItem>): String {
        return buildString {
            // CSV header
            appendLine("Category,Item Name,Quantity,Unit,Notes,Estimated Price,Priority")
            
            // CSV rows
            items.forEach { item ->
                val category = item.category.displayName()
                val name = escapeCsv(item.name)
                val quantity = escapeCsv(item.quantity)
                val unit = escapeCsv(item.unit)
                val notes = escapeCsv(item.notes)
                val price = item.estimatedPrice?.let { String.format("%.2f", it) } ?: ""
                val priority = if (item.isPriority) "Yes" else "No"
                
                appendLine("$category,$name,$quantity,$unit,$notes,$price,$priority")
            }
        }
    }

    /**
     * Exports shopping list as JSON.
     */
    private fun exportAsJson(items: List<ShoppingListItem>): String {
        return buildString {
            appendLine("{")
            appendLine("  \"shoppingList\": {")
            appendLine("    \"exportDate\": \"${System.currentTimeMillis()}\",")
            appendLine("    \"totalItems\": ${items.size},")
            
            val totalPrice = items.mapNotNull { it.estimatedPrice }.sum()
            appendLine("    \"estimatedTotal\": ${String.format("%.2f", totalPrice)},")
            
            appendLine("    \"items\": [")
            
            items.forEachIndexed { index, item ->
                appendLine("      {")
                appendLine("        \"name\": \"${escapeJson(item.name)}\",")
                appendLine("        \"quantity\": \"${escapeJson(item.quantity)}\",")
                appendLine("        \"unit\": \"${escapeJson(item.unit)}\",")
                appendLine("        \"category\": \"${item.category.displayName()}\",")
                appendLine("        \"notes\": \"${escapeJson(item.notes)}\",")
                appendLine("        \"estimatedPrice\": ${item.estimatedPrice ?: "null"},")
                appendLine("        \"isPriority\": ${item.isPriority},")
                appendLine("        \"storeSection\": \"${escapeJson(item.storeSection)}\"")
                append("      }")
                
                if (index < items.size - 1) {
                    appendLine(",")
                } else {
                    appendLine()
                }
            }
            
            appendLine("    ]")
            appendLine("  }")
            append("}")
        }
    }

    /**
     * Escapes special characters for CSV format.
     */
    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    /**
     * Escapes special characters for JSON format.
     */
    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    data class Params(val format: ExportFormat)

    enum class ExportFormat {
        TEXT,
        CSV,
        JSON
    }
}
