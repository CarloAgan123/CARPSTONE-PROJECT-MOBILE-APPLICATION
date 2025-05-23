package com.example.myfirstapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginViewModelFactory(
    private val firebaseAuth: FirebaseAuth,
    private val db: FirebaseDatabase
) :ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)){
            return LoginViewModel(firebaseAuth, db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}