package com.example.myfirstapplication.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OrderDetails(
    var orderId: String = "",
    var orderDate: String = "",
    var orderStatus: String = "",
    var orderTime: String = "",
    var paid: String = "",
    var paymentMethod: String = "",
    var products: List<ProductList> = listOf(),
    var stallID: String = "",
    var totalPrice: String = "",
    var type: String = "",
    val stability: Int = 0
): Parcelable

@Parcelize
data class ProductList(
    var name: String = "",
    var price: String = "",
    var quantity: String = "",
    var subTotalPrice: String = "",
    val stability: Int = 0
): Parcelable
