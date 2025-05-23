package com.example.myfirstapplication.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.myfirstapplication.data.CartItem

class CartViewModel : ViewModel() {
    private val _cartItems = MutableLiveData<MutableList<CartItem>>(mutableListOf())
    val cartItems: LiveData<MutableList<CartItem>> get() = _cartItems

    fun addToCart(cartItem: CartItem) {
        val items = _cartItems.value ?: mutableListOf()
        val existingItem = items.find { it.productId == cartItem.productId }
        if (existingItem != null) {
            existingItem.productQuantity += cartItem.productQuantity
        } else {
            items.add(cartItem)
        }
        _cartItems.value = items
    }
}
