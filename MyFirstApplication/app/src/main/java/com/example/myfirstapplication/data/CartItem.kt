package com.example.myfirstapplication.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CartItem(
    var productId: String = "",
    var productName: String = "",
    var productPrice: Int = 0,
    var subTotalPrice: Int = 0,
    var productQuantity: Int = 0,
    var productStallName: String = "",
    var productImage: String = "",
    var cartItemId: String? = null,
    var cartItemType: String? = null,
    var isChecked: Boolean = false
) : Parcelable
