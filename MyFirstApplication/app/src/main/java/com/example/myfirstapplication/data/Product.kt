package com.example.myfirstapplication.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/*data class Product(
    val productId: String,
    val productName: String,
    //val category: String,
    val productPrice: Float,
    //val description: String? = null,
    //val sizes: List<String>? = null,
    val productImage: String,
    val productQuantity: Float? = null
)*/

@Parcelize

data class Product(
    val productId: String = "",
    val productName: String = "",
    val productPrice: String = "",
    val productImage: String = "",
    val productQuantity: String = "",
    val productIdentity: String = "",
    val productCategory: String = "",
    val productStallName: String = "",
    val productDescription: String = ""
): Parcelable

