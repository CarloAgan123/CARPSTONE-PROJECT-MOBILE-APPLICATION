package com.example.myfirstapplication.data

data class Users(
    val firstName: String = "",
    val lastName: String = "",
    val userID: String = "",
    val email: String = "",
    val imagePath: String = ""
) {
    constructor() : this("", "", "", "", "")
}
