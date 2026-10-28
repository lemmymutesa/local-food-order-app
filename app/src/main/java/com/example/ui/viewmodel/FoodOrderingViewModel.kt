package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.CustomOption
import com.example.data.model.DeliveryAddress
import com.example.data.model.MenuItem
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.Restaurant
import com.example.data.repository.FoodRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    data class RestaurantDetail(val restaurantId: String) : Screen()
    object Cart : Screen()
    data class OrderTracking(val orderId: String) : Screen()
    object OrderHistory : Screen()
    object Favorites : Screen()
    object Profile : Screen()
}

data class CustomizerState(
    val menuItem: MenuItem,
    val selectedSize: CustomOption?,
    val selectedAddOns: Set<CustomOption> = emptySet(),
    val quantity: Int = 1,
    val specialInstructions: String = ""
) {
    val unitPrice: Double
        get() {
            val sizeDelta = selectedSize?.priceDelta ?: 0.0
            val addOnsDelta = selectedAddOns.sumOf { it.priceDelta }
            return menuItem.basePrice + sizeDelta + addOnsDelta
        }

    val totalPrice: Double
        get() = unitPrice * quantity
}

class FoodOrderingViewModel(
    private val repository: FoodRepository = FoodRepository()
) : ViewModel() {

    private val _screenStack = MutableStateFlow<List<Screen>>(listOf(Screen.Home))
    val currentScreen: StateFlow<Screen> = MutableStateFlow<Screen>(Screen.Home).apply {
        viewModelScope.launch {
            _screenStack.collect { stack ->
                value = stack.lastOrNull() ?: Screen.Home
            }
        }
    }

    val restaurants = repository.restaurants
    val menuItems = repository.menuItems
    val cartItems = repository.cartItems
    val orders = repository.orders
    val addresses = repository.addresses
    val selectedAddress = repository.selectedAddress
    val appliedPromo = repository.appliedPromo
    val promoDiscount = repository.promoDiscount
    val tipAmount = repository.tipAmount

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCuisine = MutableStateFlow("All")
    val selectedCuisine: StateFlow<String> = _selectedCuisine.asStateFlow()

    private val _filterTopRated = MutableStateFlow(false)
    val filterTopRated: StateFlow<Boolean> = _filterTopRated.asStateFlow()

    private val _filterUnder30Min = MutableStateFlow(false)
    val filterUnder30Min: StateFlow<Boolean> = _filterUnder30Min.asStateFlow()

    private val _filterFreeDelivery = MutableStateFlow(false)
    val filterFreeDelivery: StateFlow<Boolean> = _filterFreeDelivery.asStateFlow()

    val filteredRestaurants: StateFlow<List<Restaurant>> = combine(
        restaurants,
        searchQuery,
        selectedCuisine,
        filterTopRated,
        filterUnder30Min,
        filterFreeDelivery
    ) { restList, query, cuisine, topRated, under30, freeDelivery ->
        restList.filter { rest ->
            val matchesQuery = query.isBlank() ||
                    rest.name.contains(query, ignoreCase = true) ||
                    rest.cuisine.contains(query, ignoreCase = true) ||
                    rest.popularTags.any { it.contains(query, ignoreCase = true) }

            val matchesCuisine = cuisine == "All" || rest.cuisine.equals(cuisine, ignoreCase = true)
            val matchesTopRated = !topRated || rest.rating >= 4.8
            val matchesUnder30 = !under30 || rest.deliveryTimeMinutes <= 30
            val matchesFreeDelivery = !freeDelivery || rest.deliveryFee == 0.0

            matchesQuery && matchesCuisine && matchesTopRated && matchesUnder30 && matchesFreeDelivery
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val cartSubtotal: StateFlow<Double> = cartItems.combine(appliedPromo) { items, _ ->
        items.sumOf { it.itemTotal }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val activeCartRestaurant: StateFlow<Restaurant?> = combine(restaurants, cartItems) { list, cart ->
        val restId = cart.firstOrNull()?.menuItem?.restaurantId
        list.find { it.id == restId }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _customizerState = MutableStateFlow<CustomizerState?>(null)
    val customizerState: StateFlow<CustomizerState?> = _customizerState.asStateFlow()

    private val _promoMessage = MutableStateFlow<String?>(null)
    val promoMessage: StateFlow<String?> = _promoMessage.asStateFlow()

    private var simulationJob: Job? = null

    private val _driverChatDialog = MutableStateFlow<Boolean>(false)
    val driverChatDialog: StateFlow<Boolean> = _driverChatDialog.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<Pair<String, Boolean>>>(listOf(
        "Hi! I picked up your food and I'm heading your way." to false
    ))
    val chatMessages: StateFlow<List<Pair<String, Boolean>>> = _chatMessages.asStateFlow()

    fun navigateTo(screen: Screen) {
        _screenStack.value = _screenStack.value + screen
    }

    fun navigateBack(): Boolean {
        val current = _screenStack.value
        return if (current.size > 1) {
            _screenStack.value = current.dropLast(1)
            true
        } else {
            false
        }
    }

    fun navigateToHome() {
        _screenStack.value = listOf(Screen.Home)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCuisineSelected(cuisine: String) {
        _selectedCuisine.value = cuisine
    }

    fun toggleFilterTopRated() {
        _filterTopRated.value = !_filterTopRated.value
    }

    fun toggleFilterUnder30Min() {
        _filterUnder30Min.value = !_filterUnder30Min.value
    }

    fun toggleFilterFreeDelivery() {
        _filterFreeDelivery.value = !_filterFreeDelivery.value
    }

    fun toggleFavorite(restaurantId: String) {
        repository.toggleFavorite(restaurantId)
    }

    fun openCustomizer(item: MenuItem) {
        _customizerState.value = CustomizerState(
            menuItem = item,
            selectedSize = item.sizeOptions.firstOrNull(),
            selectedAddOns = emptySet(),
            quantity = 1,
            specialInstructions = ""
        )
    }

    fun closeCustomizer() {
        _customizerState.value = null
    }

    fun selectSize(option: CustomOption) {
        _customizerState.value = _customizerState.value?.copy(selectedSize = option)
    }

    fun toggleAddOn(option: CustomOption) {
        val current = _customizerState.value ?: return
        val updated = if (current.selectedAddOns.contains(option)) {
            current.selectedAddOns - option
        } else {
            current.selectedAddOns + option
        }
        _customizerState.value = current.copy(selectedAddOns = updated)
    }

    fun setCustomizerQuantity(qty: Int) {
        if (qty in 1..20) {
            _customizerState.value = _customizerState.value?.copy(quantity = qty)
        }
    }

    fun setSpecialInstructions(instructions: String) {
        _customizerState.value = _customizerState.value?.copy(specialInstructions = instructions)
    }

    fun confirmAddToCart() {
        val state = _customizerState.value ?: return
        repository.addToCart(
            menuItem = state.menuItem,
            quantity = state.quantity,
            selectedSize = state.selectedSize,
            selectedAddOns = state.selectedAddOns.toList(),
            specialInstructions = state.specialInstructions
        )
        closeCustomizer()
    }

    fun updateQuantity(cartItemId: String, qty: Int) {
        repository.updateCartItemQuantity(cartItemId, qty)
    }

    fun removeCartItem(cartItemId: String) {
        repository.removeFromCart(cartItemId)
    }

    fun clearCart() {
        repository.clearCart()
    }

    fun setTip(amount: Double) {
        repository.setTip(amount)
    }

    fun applyPromo(code: String) {
        val res = repository.applyPromoCode(code)
        if (res.isSuccess) {
            _promoMessage.value = res.getOrNull()
        } else {
            _promoMessage.value = res.exceptionOrNull()?.message
        }
    }

    fun removePromo() {
        repository.removePromoCode()
        _promoMessage.value = null
    }

    fun selectAddress(address: DeliveryAddress) {
        repository.selectAddress(address)
    }

    fun addAddress(address: DeliveryAddress) {
        repository.addAddress(address)
    }

    fun placeOrder() {
        val order = repository.placeOrder() ?: return
        navigateTo(Screen.OrderTracking(order.id))
        startOrderSimulation(order.id)
    }

    fun reorder(order: Order) {
        repository.reorder(order)
        navigateTo(Screen.Cart)
    }

    private fun startOrderSimulation(orderId: String) {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch {
            repository.updateOrderStatus(orderId, OrderStatus.CONFIRMED)
            delay(5000)
            repository.updateOrderStatus(orderId, OrderStatus.PREPARING)
            delay(7000)
            repository.updateOrderStatus(orderId, OrderStatus.ON_THE_WAY)
            delay(9000)
            repository.updateOrderStatus(orderId, OrderStatus.NEARBY)
            delay(6000)
            repository.updateOrderStatus(orderId, OrderStatus.DELIVERED)
        }
    }

    fun openDriverChat() {
        _driverChatDialog.value = true
    }

    fun closeDriverChat() {
        _driverChatDialog.value = false
    }

    fun sendChatMessage(msg: String) {
        if (msg.isBlank()) return
        val current = _chatMessages.value.toMutableList()
        current.add(msg to true)
        _chatMessages.value = current

        viewModelScope.launch {
            delay(1500)
            val driverReply = when {
                msg.contains("gate", ignoreCase = true) || msg.contains("code", ignoreCase = true) ->
                    "Got it, thanks for the code! Approaching your building now."
                msg.contains("door", ignoreCase = true) ->
                    "Understood, I will leave the food securely right at your doorstep."
                else ->
                    "Sounds good! On my way, should be there in a few minutes."
            }
            _chatMessages.value = _chatMessages.value + (driverReply to false)
        }
    }
}
