package com.example.data.repository

import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.CustomOption
import com.example.data.model.DeliveryAddress
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.Restaurant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FoodRepository {

    private val _restaurants = MutableStateFlow<List<Restaurant>>(sampleRestaurants())
    val restaurants: StateFlow<List<Restaurant>> = _restaurants.asStateFlow()

    private val _menuItems = MutableStateFlow<List<MenuItem>>(sampleMenuItems())
    val menuItems: StateFlow<List<MenuItem>> = _menuItems.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(samplePastOrders())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _addresses = MutableStateFlow<List<DeliveryAddress>>(sampleAddresses())
    val addresses: StateFlow<List<DeliveryAddress>> = _addresses.asStateFlow()

    private val _selectedAddress = MutableStateFlow<DeliveryAddress>(sampleAddresses().first())
    val selectedAddress: StateFlow<DeliveryAddress> = _selectedAddress.asStateFlow()

    private val _appliedPromo = MutableStateFlow<String?>(null)
    val appliedPromo: StateFlow<String?> = _appliedPromo.asStateFlow()

    private val _promoDiscount = MutableStateFlow(0.0)
    val promoDiscount: StateFlow<Double> = _promoDiscount.asStateFlow()

    private val _tipAmount = MutableStateFlow(3.00)
    val tipAmount: StateFlow<Double> = _tipAmount.asStateFlow()

    fun toggleFavorite(restaurantId: String) {
        _restaurants.value = _restaurants.value.map {
            if (it.id == restaurantId) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    fun selectAddress(address: DeliveryAddress) {
        _selectedAddress.value = address
    }

    fun addAddress(address: DeliveryAddress) {
        _addresses.value = _addresses.value + address
        _selectedAddress.value = address
    }

    fun setTip(amount: Double) {
        _tipAmount.value = amount
    }

    fun applyPromoCode(code: String): Result<String> {
        val cleanCode = code.trim().uppercase()
        return when (cleanCode) {
            "WELCOME10" -> {
                _appliedPromo.value = "WELCOME10 ($10 Off)"
                _promoDiscount.value = 10.0
                Result.success("Promo WELCOME10 applied! $10 saved.")
            }
            "FREESHIP", "FREEDELIVERY" -> {
                _appliedPromo.value = "FREEDELIVERY (Free Delivery)"
                _promoDiscount.value = 2.99
                Result.success("Free delivery code applied!")
            }
            "TASTY5" -> {
                _appliedPromo.value = "TASTY5 ($5 Off)"
                _promoDiscount.value = 5.0
                Result.success("Promo TASTY5 applied! $5 saved.")
            }
            else -> {
                Result.failure(IllegalArgumentException("Invalid code. Try 'WELCOME10' or 'TASTY5'."))
            }
        }
    }

    fun removePromoCode() {
        _appliedPromo.value = null
        _promoDiscount.value = 0.0
    }

    fun addToCart(
        menuItem: MenuItem,
        quantity: Int,
        selectedSize: CustomOption?,
        selectedAddOns: List<CustomOption>,
        specialInstructions: String
    ) {
        val currentList = _cartItems.value.toMutableList()
        val existingRestaurantId = currentList.firstOrNull()?.menuItem?.restaurantId
        if (existingRestaurantId != null && existingRestaurantId != menuItem.restaurantId) {
            currentList.clear()
        }

        val newItem = CartItem(
            cartItemId = UUID.randomUUID().toString(),
            menuItem = menuItem,
            quantity = quantity,
            selectedSize = selectedSize,
            selectedAddOns = selectedAddOns,
            specialInstructions = specialInstructions
        )
        currentList.add(newItem)
        _cartItems.value = currentList
    }

    fun updateCartItemQuantity(cartItemId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(cartItemId)
        } else {
            _cartItems.value = _cartItems.value.map {
                if (it.cartItemId == cartItemId) it.copy(quantity = newQuantity) else it
            }
        }
    }

    fun removeFromCart(cartItemId: String) {
        _cartItems.value = _cartItems.value.filterNot { it.cartItemId == cartItemId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _appliedPromo.value = null
        _promoDiscount.value = 0.0
    }

    fun placeOrder(): Order? {
        val currentItems = _cartItems.value
        if (currentItems.isEmpty()) return null

        val restaurantId = currentItems.first().menuItem.restaurantId
        val restaurant = _restaurants.value.find { it.id == restaurantId }
        val restName = restaurant?.name ?: "Local Bistro"
        val deliveryFee = restaurant?.deliveryFee ?: 1.99
        val subtotal = currentItems.sumOf { it.itemTotal }
        val discount = _promoDiscount.value
        val tip = _tipAmount.value
        val total = (subtotal + deliveryFee + tip - discount).coerceAtLeast(0.0)

        val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val orderNum = "#BS-${(1000..9999).random()}"

        val newOrder = Order(
            id = UUID.randomUUID().toString(),
            orderNumber = orderNum,
            restaurantId = restaurantId,
            restaurantName = restName,
            items = currentItems,
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            tip = tip,
            discount = discount,
            appliedPromo = _appliedPromo.value,
            total = total,
            status = OrderStatus.CONFIRMED,
            placedTimestamp = System.currentTimeMillis(),
            placedTimeFormatted = dateFormat.format(Date()),
            estimatedArrivalMinutes = restaurant?.deliveryTimeMinutes ?: 25,
            deliveryAddress = _selectedAddress.value
        )

        _orders.value = listOf(newOrder) + _orders.value
        clearCart()
        return newOrder
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        _orders.value = _orders.value.map {
            if (it.id == orderId) it.copy(status = newStatus) else it
        }
    }

    fun reorder(order: Order) {
        _cartItems.value = order.items.map {
            it.copy(cartItemId = UUID.randomUUID().toString())
        }
    }

    companion object {
        fun sampleRestaurants(): List<Restaurant> = listOf(
            Restaurant(
                id = "rest_burger",
                name = "The Burger Forge",
                cuisine = "Burgers",
                rating = 4.8,
                ratingCount = 428,
                deliveryTimeMinutes = 22,
                deliveryFee = 0.99,
                distanceMiles = 1.2,
                priceTier = "$$",
                isFeatured = true,
                isFavorite = true,
                bannerDrawable = R.drawable.img_food_burger,
                address = "412 Oak Street, Downtown",
                promoBadge = "$5 Off with TASTY5",
                popularTags = listOf("Double Smash", "Truffle Fries", "Craft Shakes"),
                menuCategories = listOf("Popular", "Burgers", "Loaded Sides", "Beverages")
            ),
            Restaurant(
                id = "rest_pizza",
                name = "Bella Napoli Pizzeria",
                cuisine = "Italian",
                rating = 4.9,
                ratingCount = 612,
                deliveryTimeMinutes = 28,
                deliveryFee = 0.00,
                distanceMiles = 1.8,
                priceTier = "$$",
                isFeatured = true,
                isFavorite = true,
                bannerDrawable = R.drawable.img_food_pizza,
                address = "850 Bella Vista Way, Little Italy",
                promoBadge = "Free Delivery",
                popularTags = listOf("Wood-Fired", "Margherita", "Tiramisu"),
                menuCategories = listOf("Popular", "Pizzas", "Handmade Pastas", "Desserts")
            ),
            Restaurant(
                id = "rest_sushi",
                name = "Sakura Artisan Sushi & Bowls",
                cuisine = "Japanese",
                rating = 4.9,
                ratingCount = 389,
                deliveryTimeMinutes = 32,
                deliveryFee = 1.99,
                distanceMiles = 2.4,
                priceTier = "$$$",
                isFeatured = true,
                isFavorite = false,
                bannerDrawable = R.drawable.img_food_sushi,
                address = "1200 Harbor Boulevard, Waterfront",
                promoBadge = "Top Rated Local",
                popularTags = listOf("Sashimi", "Dragon Roll", "Miso Ramen"),
                menuCategories = listOf("Popular", "Special Rolls", "Nigiri & Sashimi", "Ramen & Bowls")
            ),
            Restaurant(
                id = "rest_tacos",
                name = "Taquería Los Hermanos",
                cuisine = "Mexican",
                rating = 4.7,
                ratingCount = 514,
                deliveryTimeMinutes = 18,
                deliveryFee = 0.00,
                distanceMiles = 0.9,
                priceTier = "$",
                isFeatured = false,
                isFavorite = false,
                bannerDrawable = R.drawable.img_hero_banner,
                address = "220 Mission St, Westside",
                promoBadge = "Free Delivery",
                popularTags = listOf("Birria Tacos", "Churros", "Queso Dip"),
                menuCategories = listOf("Popular", "Street Tacos", "Burritos & Bowls", "Sides & Drinks")
            ),
            Restaurant(
                id = "rest_bowls",
                name = "Green Harvest Bowls & Juices",
                cuisine = "Healthy",
                rating = 4.8,
                ratingCount = 275,
                deliveryTimeMinutes = 20,
                deliveryFee = 1.49,
                distanceMiles = 1.4,
                priceTier = "$$",
                isFeatured = false,
                isFavorite = false,
                bannerDrawable = R.drawable.img_hero_banner,
                address = "904 Market Grove, Uptown",
                promoBadge = "Healthy Choice",
                popularTags = listOf("Açai Bowls", "Salmon Quinoa", "Fresh Juices"),
                menuCategories = listOf("Popular", "Warm Bowls", "Superfood Açai", "Cold Pressed Juices")
            ),
            Restaurant(
                id = "rest_wok",
                name = "Golden Dragon Wok",
                cuisine = "Asian",
                rating = 4.6,
                ratingCount = 330,
                deliveryTimeMinutes = 26,
                deliveryFee = 1.99,
                distanceMiles = 2.0,
                priceTier = "$$",
                isFeatured = false,
                isFavorite = false,
                bannerDrawable = R.drawable.img_food_sushi,
                address = "334 Chinatown Way",
                promoBadge = null,
                popularTags = listOf("Dumplings", "General Tso", "Dan Dan"),
                menuCategories = listOf("Popular", "Dumplings", "Wok Specials", "Noodles")
            )
        )

        fun sampleMenuItems(): List<MenuItem> = listOf(
            MenuItem(
                id = "m_burger_1",
                restaurantId = "rest_burger",
                name = "The Forge Double Smash",
                description = "Two 100% Angus beef patties, double smoked cheddar, caramelized sweet onions, house secret sauce on buttered brioche bun.",
                basePrice = 14.50,
                category = "Burgers",
                imageRes = R.drawable.img_food_burger,
                isPopular = true,
                sizeOptions = listOf(
                    CustomOption("sz_reg", "Regular Double", 0.0),
                    CustomOption("sz_triple", "Triple Tower (+patty & cheese)", 3.50)
                ),
                addOns = listOf(
                    CustomOption("ad_bacon", "Applewood Smoked Bacon", 2.25),
                    CustomOption("ad_avocado", "Fresh Sliced Avocado", 1.75),
                    CustomOption("ad_egg", "Sunny Side Up Egg", 1.50),
                    CustomOption("ad_jalapeno", "Pickled Jalapeños", 0.75)
                )
            ),
            MenuItem(
                id = "m_burger_2",
                restaurantId = "rest_burger",
                name = "Truffle Mushroom Burger",
                description = "Angus beef with sautéed wild cremini mushrooms, Swiss gruyere, white truffle garlic aioli, and baby arugula.",
                basePrice = 15.95,
                category = "Burgers",
                imageRes = R.drawable.img_food_burger,
                isPopular = true,
                sizeOptions = listOf(
                    CustomOption("sz_reg", "Standard", 0.0),
                    CustomOption("sz_double", "Make it a Double Patty", 3.00)
                ),
                addOns = listOf(
                    CustomOption("ad_bacon", "Smoked Bacon", 2.25),
                    CustomOption("ad_cheese", "Extra Gruyere Cheese", 1.50)
                )
            ),
            MenuItem(
                id = "m_burger_3",
                restaurantId = "rest_burger",
                name = "Parmesan Truffle Loaded Fries",
                description = "Crispy golden cut fries tossed with aged parmesan, chopped rosemary, truffle oil, served with garlic herb dip.",
                basePrice = 6.95,
                category = "Loaded Sides",
                imageRes = R.drawable.img_food_burger,
                isPopular = true,
                isVegetarian = true,
                addOns = listOf(
                    CustomOption("ad_cheddar", "Melted Cheddar Sauce", 1.50),
                    CustomOption("ad_bacon_bits", "Crispy Bacon Bits", 1.75)
                )
            ),
            MenuItem(
                id = "m_burger_4",
                restaurantId = "rest_burger",
                name = "Salted Caramel Vanilla Milkshake",
                description = "Hand-spun organic vanilla bean ice cream with ribbon swirls of sea salt caramel and whipped cream.",
                basePrice = 5.75,
                category = "Beverages",
                isPopular = false,
                isVegetarian = true
            ),
            MenuItem(
                id = "m_pizza_1",
                restaurantId = "rest_pizza",
                name = "Margherita Classica D.O.P.",
                description = "San Marzano tomato sauce, fresh buffalo mozzarella, fresh sweet basil, and extra virgin Italian olive oil on 48h slow-fermented crust.",
                basePrice = 16.50,
                category = "Pizzas",
                imageRes = R.drawable.img_food_pizza,
                isPopular = true,
                isVegetarian = true,
                sizeOptions = listOf(
                    CustomOption("sz_12", "12\" Personal (6 slices)", 0.0),
                    CustomOption("sz_16", "16\" Large (8 slices)", 4.50)
                ),
                addOns = listOf(
                    CustomOption("ad_burrata", "Add Whole Creamy Burrata", 4.00),
                    CustomOption("ad_prosciutto", "Prosciutto di Parma", 3.50),
                    CustomOption("ad_olives", "Kalamata Olives", 1.25)
                )
            ),
            MenuItem(
                id = "m_pizza_2",
                restaurantId = "rest_pizza",
                name = "Diavola Spicy Pepperoni",
                description = "Spicy Calabrian soppressata, pepperoni, roasted red chili peppers, fresh mozzarella, and hot honey drizzle.",
                basePrice = 18.25,
                category = "Pizzas",
                imageRes = R.drawable.img_food_pizza,
                isPopular = true,
                isSpicy = true,
                sizeOptions = listOf(
                    CustomOption("sz_12", "12\" Personal", 0.0),
                    CustomOption("sz_16", "16\" Large", 4.50)
                ),
                addOns = listOf(
                    CustomOption("ad_extra_honey", "Extra Hot Honey", 1.00),
                    CustomOption("ad_ricotta", "Whipped Ricotta Dollops", 2.50)
                )
            ),
            MenuItem(
                id = "m_pizza_3",
                restaurantId = "rest_pizza",
                name = "Truffle Tagliatelle Carbonara",
                description = "Fresh house-made tagliatelle ribbons with crispy guanciale, pecorino romano, egg yolk cream, and black pepper.",
                basePrice = 17.50,
                category = "Handmade Pastas",
                imageRes = R.drawable.img_food_pizza,
                isPopular = true
            ),
            MenuItem(
                id = "m_sushi_1",
                restaurantId = "rest_sushi",
                name = "Imperial Dragon Specialty Roll",
                description = "Crispy shrimp tempura, cucumber inside, topped with fresh avocado, barbecue eel, spicy mayo, and tobiko roe.",
                basePrice = 16.95,
                category = "Special Rolls",
                imageRes = R.drawable.img_food_sushi,
                isPopular = true,
                addOns = listOf(
                    CustomOption("ad_spicy_tuna", "Topped with Spicy Tuna", 3.00),
                    CustomOption("ad_soy_paper", "Substitute Soy Paper", 1.00)
                )
            ),
            MenuItem(
                id = "m_sushi_2",
                restaurantId = "rest_sushi",
                name = "Chef's Nigiri Platter (8 pcs)",
                description = "Freshly sliced Atlantic salmon, bluefin tuna, yellowtail hamachi, and unagi over seasoned sushi rice with wasabi.",
                basePrice = 22.00,
                category = "Nigiri & Sashimi",
                imageRes = R.drawable.img_food_sushi,
                isPopular = true
            )
        )

        fun sampleAddresses(): List<DeliveryAddress> = listOf(
            DeliveryAddress(
                id = "addr_home",
                label = "Home",
                street = "742 Evergreen Terrace",
                suiteOrApt = "Apt 4B",
                deliveryNotes = "Buzz code #1204, leave outside door",
                isDefault = true
            ),
            DeliveryAddress(
                id = "addr_work",
                label = "Work Office",
                street = "100 Innovation Way, Tech Tower",
                suiteOrApt = "Suite 800",
                deliveryNotes = "Call upon arrival, meet in lobby",
                isDefault = false
            )
        )

        fun samplePastOrders(): List<Order> = listOf(
            Order(
                id = "order_past_1",
                orderNumber = "#BS-8831",
                restaurantId = "rest_burger",
                restaurantName = "The Burger Forge",
                items = listOf(
                    CartItem(
                        cartItemId = "ci_1",
                        menuItem = sampleMenuItems()[0],
                        quantity = 1,
                        selectedSize = CustomOption("sz_reg", "Regular Double", 0.0),
                        selectedAddOns = listOf(CustomOption("ad_bacon", "Smoked Bacon", 2.25)),
                        specialInstructions = "Extra pickles please!"
                    ),
                    CartItem(
                        cartItemId = "ci_2",
                        menuItem = sampleMenuItems()[2],
                        quantity = 1,
                        specialInstructions = ""
                    )
                ),
                subtotal = 23.70,
                deliveryFee = 0.99,
                tip = 4.00,
                discount = 0.0,
                appliedPromo = null,
                total = 28.69,
                status = OrderStatus.DELIVERED,
                placedTimestamp = System.currentTimeMillis() - 86400000L * 2,
                placedTimeFormatted = "Yesterday, 7:15 PM",
                estimatedArrivalMinutes = 0,
                deliveryAddress = sampleAddresses().first()
            )
        )
    }
}
