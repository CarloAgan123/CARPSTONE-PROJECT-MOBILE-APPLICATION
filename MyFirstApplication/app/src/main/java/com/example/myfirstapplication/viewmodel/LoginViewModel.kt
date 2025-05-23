package com.example.myfirstapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfirstapplication.data.Users
import com.example.myfirstapplication.util.Resource
import com.example.myfirstapplication.util.LoginFieldsState
import com.example.myfirstapplication.util.LoginValidation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val firebaseAuth: FirebaseAuth,
    private val db: FirebaseDatabase
) : ViewModel() {

    private val _login = MutableStateFlow<Resource<Users>>(Resource.Unspecified())
    val login: Flow<Resource<Users>> = _login

    private val _validation = Channel<LoginFieldsState>(Channel.BUFFERED)
    val validation = _validation.receiveAsFlow()

    private val _resetPassword = MutableSharedFlow<String>()
    val resetPassword = _resetPassword.asSharedFlow()

    fun loginWithEmailAndPassword(email: String, password: String, userID: String) {
        if (checkValidation(email, password)) {
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid
                    uid?.let {
                        fetchUserInfo(userID)
                    }
                }
                .addOnFailureListener {
                    _login.value = Resource.Error(it.message.toString())
                }
        } else {
            val loginFieldsState = LoginFieldsState(
                validateUserID(userID),
                validateEmail(email),
                validatePassword(password)
            )
            viewModelScope.launch {
                _validation.send(loginFieldsState)
            }
        }
    }

    private fun checkValidation(email: String, password: String): Boolean {
        val emailValidation = validateEmail(email)
        val passwordValidation = validatePassword(password)
        return emailValidation is LoginValidation.Success && passwordValidation is LoginValidation.Success
    }

    private fun validateEmail(email: String): LoginValidation {
        return if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            LoginValidation.Success
        } else {
            LoginValidation.Failed("Invalid email address")
        }
    }

    private fun validatePassword(password: String): LoginValidation {
        return if (password.length >= 6) {
            LoginValidation.Success
        } else {
            LoginValidation.Failed("Password must be at least 6 characters long")
        }
    }

    private fun validateUserID(userID: String): LoginValidation {
        return if (userID.isNotEmpty()) {
            LoginValidation.Success
        } else {
            LoginValidation.Failed("User ID is required")
        }
    }

    private fun fetchUserInfo(userID: String) {
        db.getReference("users").child(userID).get()
            .addOnSuccessListener { dataSnapshot ->
                val user = dataSnapshot.getValue(Users::class.java)
                if (user != null) {
                    _login.value = Resource.Success(user)
                } else {
                    _login.value = Resource.Error("User not found")
                }
            }
            .addOnFailureListener {
                _login.value = Resource.Error(it.message.toString())
            }
    }

    fun resetPassword(email: String){
        viewModelScope.launch {
            _resetPassword.emit("Loading")
        }

        firebaseAuth
            .sendPasswordResetEmail(email)
            .addOnSuccessListener {
                viewModelScope.launch {
                    _resetPassword.emit("Success")
                }
            }
            .addOnFailureListener{
                viewModelScope.launch {
                    _resetPassword.emit(it.message.toString())
                }
            }
    }
}

