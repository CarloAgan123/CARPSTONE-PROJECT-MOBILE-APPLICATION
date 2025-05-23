package com.example.myfirstapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfirstapplication.data.Users
import com.example.myfirstapplication.util.Resource
import com.example.myfirstapplication.util.RegisterFieldsState
import com.example.myfirstapplication.util.RegisterValidation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await

class RegisterViewModel(
    private val firebaseAuth: FirebaseAuth,
    private val db: FirebaseDatabase
) : ViewModel() {

    private val _register = MutableStateFlow<Resource<Users>>(Resource.Unspecified())
    val register: Flow<Resource<Users>> = _register

    private val _validation = Channel<RegisterFieldsState>(Channel.BUFFERED)
    val validation = _validation.receiveAsFlow()

    fun createAccountWithEmailAndPassword(user: Users, password: String) {
        viewModelScope.launch {
            val userIDExists = checkIfUserIDExists(user.userID)
            if (userIDExists) {
                _validation.send(RegisterFieldsState(
                    userID = RegisterValidation.Failed("User ID is already registered!"),
                    email = RegisterValidation.Success,
                    password = RegisterValidation.Success
                ))
                return@launch
            }

            if (checkValidation(user, password)) {
                _register.emit(Resource.Loading())
                firebaseAuth.createUserWithEmailAndPassword(user.email, password)
                    .addOnSuccessListener { authResult ->
                        val userID = user.userID
                        authResult.user?.uid?.let { uid ->
                            saveUserInfo(uid, user, userID)
                        }
                    }
                    .addOnFailureListener {
                        _register.value = Resource.Error(it.message.toString())
                    }
            } else {
                val registerFieldsState = RegisterFieldsState(
                    validateUserID(user.userID),
                    validateEmail(user.email),
                    validatePassword(password)
                )
                _validation.send(registerFieldsState)
            }
        }
    }

    private suspend fun checkIfUserIDExists(userID: String): Boolean {
        val userRef = db.getReference("users").child(userID).get().await()
        return userRef.exists()
    }

    private fun checkValidation(user: Users, password: String): Boolean {
        val userIDValidation = validateUserID(user.userID)
        val emailValidation = validateEmail(user.email)
        val passwordValidation = validatePassword(password)
        return emailValidation is RegisterValidation.Success && passwordValidation is RegisterValidation.Success
    }

    private fun validateUserID(userID: String): RegisterValidation {
        return if (userID.isNotEmpty()) {
            RegisterValidation.Success
        } else {
            RegisterValidation.Failed("User ID must not be empty")
        }
    }

    private fun validateEmail(email: String): RegisterValidation {
        return if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            RegisterValidation.Success
        } else {
            RegisterValidation.Failed("Invalid email address")
        }
    }

    private fun validatePassword(password: String): RegisterValidation {
        return if (password.length >= 6) {
            RegisterValidation.Success
        } else {
            RegisterValidation.Failed("Password must be at least 6 characters long")
        }
    }

    private fun saveUserInfo(uid: String, user: Users, userID: String) {
        val userRef = db.getReference("users").child(userID)
        val userMap = mapOf(
            "email" to user.email,
            "userID" to userID,
            "firstName" to user.firstName,
            "lastName" to user.lastName,
            "imagePath" to user.imagePath
        )
        userRef.setValue(userMap)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    _register.value = Resource.Success(user)
                } else {
                    _register.value = Resource.Error(it.exception?.message.toString())
                }
            }
    }
}
