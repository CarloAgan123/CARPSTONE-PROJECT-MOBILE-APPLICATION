package com.example.myfirstapplication.data

data class PreOrderItems(
    var preOrderItemID: String = "",
    var preOrderItemName: String = "",
    var preOrderItemPrice: String = "",
    var preOrderItemQuantity: String = "",
    var preOrderItemStallName: String = "",
    var preOrderItemImage: String = "",
    var preOrderItemMenuType: String = "",
    val stability: Int = 0
)
