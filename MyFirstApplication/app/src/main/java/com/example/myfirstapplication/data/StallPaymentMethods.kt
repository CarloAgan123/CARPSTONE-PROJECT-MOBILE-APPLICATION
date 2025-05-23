package com.example.myfirstapplication.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StallPaymentMethods(
    val stallUID: String = "",
    val stallGCashQRCode: String = ""
): Parcelable
