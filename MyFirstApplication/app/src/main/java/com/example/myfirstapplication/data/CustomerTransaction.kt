package com.example.myfirstapplication.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CustomerTransaction(
    var customerEmail : String = "",
    var customerID: String = "",
    var customerName: String = "",
    var customerTransactionID: String = "",
    var customerPaymentScreenShot: String = "",
    val stability: Int = 0
): Parcelable
