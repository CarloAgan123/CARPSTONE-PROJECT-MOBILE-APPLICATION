package com.example.myfirstapplication.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class Stall(
    val ID: String? = null,
    val Img: String? = null,
    val contactNumber: String? = null,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val middlename: String? = null,
    val password: String? = null,
    val stallNumber: String? = null,
    val selectedDays: List<String>? = null,
    val stallName: String? = null,
    val timein: String? = null,
    val timeout: String? = null,
    val username: String? = null
):Parcelable

