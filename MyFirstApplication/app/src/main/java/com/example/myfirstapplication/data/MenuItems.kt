package com.example.myfirstapplication.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MenuItems(
    var menuItemID: String = "",
    var menuItemStallName: String = "",
    var menuImageUrl: String = "",
    var menuName: String = "",
    var menuPrice: String = "",
    var menuSubtotalPrice: Int = 0,
    var menuQuantity: String ="",
    var menuType: String = "",
    var nameMenu: String = "",
    val stability: Int = 0
) : Parcelable
