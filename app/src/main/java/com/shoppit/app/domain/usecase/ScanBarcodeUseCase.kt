package com.shoppit.app.domain.usecase

import com.shoppit.app.domain.error.AppError
import com.shoppit.app.domain.model.ItemCategory
import com.shoppit.app.domain.model.ShoppingListItem
import com.shoppit.app.domain.repository.ShoppingListRepository
import javax.inject.Inject

/**
 * Use case for processing barcode scans and adding items to shopping list.
 * 
 * For now, this is a mock implementation that simulates product lookup.
 * In a production app, this would integrate with a product database API.
 */
class ScanBarcodeUseCase @Inject constructor(
    private val shoppingListRepository: ShoppingListRepository
) {

    suspend operator fun invoke(params: Params): Result<Long> {
        // Validate barcode
        if (params.barcode.isBlank()) {
            return Result.failure(
                AppError.BarcodeScanError("Invalid barcode. Please try scanning again.")
            )
        }
        
        // Validate barcode format (basic check for numeric barcodes)
        if (!params.barcode.matches(Regex("^[0-9]{8,13}$"))) {
            return Result.failure(
                AppError.BarcodeScanError(
                    "Barcode format not recognized. Please enter item name manually."
                )
            )
        }
        
        // Mock product lookup - in production, this would call a product database API
        val productInfo = try {
            lookupProduct(params.barcode)
        } catch (e: Exception) {
            return Result.failure(
                AppError.BarcodeScanError("Failed to lookup product: ${e.message}")
            )
        }
        
        return if (productInfo != null) {
            // Product found, add to shopping list
            val item = ShoppingListItem(
                name = productInfo.name,
                quantity = "1",
                unit = productInfo.unit,
                category = productInfo.category,
                isManual = true,
                estimatedPrice = productInfo.price,
                storeSection = productInfo.category.name
            )
            
            shoppingListRepository.addShoppingListItem(item).fold(
                onSuccess = { itemId ->
                    Result.success(itemId)
                },
                onFailure = { error ->
                    Result.failure(
                        AppError.BarcodeScanError("Failed to add item: ${error.message}")
                    )
                }
            )
        } else {
            // Product not found
            Result.failure(
                AppError.BarcodeScanError(
                    "Product not found in database. Please enter item name manually."
                )
            )
        }
    }

    /**
     * Mock product lookup function.
     * In production, this would make an API call to a product database.
     */
    private fun lookupProduct(barcode: String): ProductInfo? {
        // Mock database of common products
        return when {
            barcode.startsWith("0") -> ProductInfo(
                name = "Milk",
                unit = "gallon",
                category = ItemCategory.DAIRY,
                price = 3.99
            )
            barcode.startsWith("1") -> ProductInfo(
                name = "Bread",
                unit = "loaf",
                category = ItemCategory.BAKERY,
                price = 2.49
            )
            barcode.startsWith("2") -> ProductInfo(
                name = "Eggs",
                unit = "dozen",
                category = ItemCategory.DAIRY,
                price = 4.99
            )
            barcode.startsWith("3") -> ProductInfo(
                name = "Chicken Breast",
                unit = "lb",
                category = ItemCategory.MEAT,
                price = 5.99
            )
            barcode.startsWith("4") -> ProductInfo(
                name = "Bananas",
                unit = "bunch",
                category = ItemCategory.PRODUCE,
                price = 1.99
            )
            else -> null // Product not found
        }
    }

    data class Params(val barcode: String)

    private data class ProductInfo(
        val name: String,
        val unit: String,
        val category: ItemCategory,
        val price: Double
    )
}
