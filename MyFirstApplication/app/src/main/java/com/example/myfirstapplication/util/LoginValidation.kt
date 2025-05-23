package com.example.myfirstapplication.util

sealed class LoginValidation {
    object Success : LoginValidation()
    data class Failed(val message: String) : LoginValidation()
}