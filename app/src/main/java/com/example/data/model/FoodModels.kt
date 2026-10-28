package com.example.data.model

import androidx.annotation.DrawableRes

data class Restaurant(
    val id: String,
    val name: String,
    val cuisine: String,
    val rating: Double,
    val ratingCount: Int,
    val deliveryTimeMinutes: Int,
    val deliveryFee: Double,
    val distanceMiles: Double,
    val priceTier: String,
    val isFeatured: Boolean = false,
    val isFavorite: Boolean = false,
    @DrawableRes val bannerDrawable: Int,
    val address: String,
    val promoBadge: String? = null,
    val popularTags: List<String> = emptyList(),
    val menuCategories: List<String> = emptyList()
)

data class CustomOption(
    val id: String,
    val name: String,
    val priceDelta: Double = 0.0
)

data class MenuItem(
    val id: String,
    val restaurantId: String,
    val name: String,
    val description: String,
    val basePrice: Double,
    val category: String,
    @DrawableRes val imageRes: Int? = null,
    val isPopular: Boolean = false,
    val isSpicy: Boolean = false,
    val isVegetarian: Boolean = false,
    val sizeOptions: List<CustomOption> = emptyList(),
    val addOns: List<CustomOption> = emptyList()
)

data class CartItem(
    val cartItemId: String,
    val menuItem: MenuItem,
    val quantity: Int,
    val selectedSize: CustomOption? = null,
    val selectedAddOns: List<CustomOption> = emptyList(),
    val specialInstructions: String = ""
) {
    val unitPrice: Double
        get() {
            val sizeDelta = selectedSize?.priceDelta ?: 0.0
            val addOnsDelta = selectedAddOns.sumOf { it.priceDelta }
            return menuItem.basePrice + sizeDelta + addOnsDelta
        }

    val itemTotal: Double
        get() = unitPrice * quantity
}

enum class OrderStatus(val title: String, val subtitle: String, val progress: Float) {
    CONFIRMED("Order Confirmed", "Restaurant accepted your order", 0.15f),
    PREPARING("Preparing Food", "Chef is cooking with fresh ingredients", 0.40f),
    ON_THE_WAY("On the Way", "Driver is heading towards your delivery address", 0.75f),
    NEARBY("Arriving Soon", "Driver is on your street", 0.90f),
    DELIVERED("Delivered", "Enjoy your delicious meal!", 1.0f)
}

data class Order(
    val id: String,
    val orderNumber: String,
    val restaurantId: String,
    val restaurantName: String,
    val items: List<CartItem>,
    val subtotal: Double,
    val deliveryFee: Double,
    val tip: Double,
    val discount: Double,
    val appliedPromo: String?,
    val total: Double,
    val status: OrderStatus,
    val placedTimestamp: Long = System.currentTimeMillis(),
    val placedTimeFormatted: String,
    val estimatedArrivalMinutes: Int,
    val deliveryAddress: DeliveryAddress,
    val driverName: String = "Marcus Vance",
    val driverPhone: String = "+1 (555) 392-8172",
    val driverRating: Double = 4.95,
    val driverVehicle: String = "Silver Honda Civic • Plate 7XYZ89"
)

data class DeliveryAddress(
    val id: String,
    val label: String,
    val street: String,
    val suiteOrApt: String = "",
    val deliveryNotes: String = "Leave at front door / ring bell",
    val isDefault: Boolean = false
)
