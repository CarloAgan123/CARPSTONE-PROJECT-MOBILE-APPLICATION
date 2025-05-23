package com.example.myfirstapplication.util

sealed class RegisterValidation(){
    object Success: RegisterValidation()
    data class Failed(val message: String): RegisterValidation()
}

data class RegisterFieldsState(
    val userID: RegisterValidation,
    val email: RegisterValidation,
    val password: RegisterValidation
)