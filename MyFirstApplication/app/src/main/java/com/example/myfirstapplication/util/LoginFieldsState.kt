package com.example.myfirstapplication.util

data class LoginFieldsState(
    val userID: LoginValidation,
    val email: LoginValidation,
    val password: LoginValidation
)
