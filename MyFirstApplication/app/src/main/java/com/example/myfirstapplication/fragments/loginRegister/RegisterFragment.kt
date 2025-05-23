package com.example.myfirstapplication.fragments.loginRegister

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.myfirstapplication.R
import com.example.myfirstapplication.activities.ShoppingActivity
import com.example.myfirstapplication.data.Users
import com.example.myfirstapplication.databinding.FragmentRegisterBinding
import com.example.myfirstapplication.util.Resource
import com.example.myfirstapplication.util.RegisterFieldsState
import com.example.myfirstapplication.util.RegisterValidation
import com.example.myfirstapplication.viewmodel.RegisterViewModel
import com.example.myfirstapplication.viewmodel.RegisterViewModelFactory
import com.google.android.material.textview.MaterialTextView
import com.github.leandroborgesferreira.loadingbutton.customViews.CircularProgressButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private lateinit var binding: FragmentRegisterBinding
    private lateinit var registerViewModel: RegisterViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val firebaseAuth = FirebaseAuth.getInstance()
        val db = FirebaseDatabase.getInstance()

        val factory = RegisterViewModelFactory(firebaseAuth, db)
        registerViewModel = ViewModelProvider(this, factory).get(RegisterViewModel::class.java)

        val loginBtn: MaterialTextView = view.findViewById(R.id.tvLogin2)
        val regSucs: CircularProgressButton = view.findViewById(R.id.registerBtn)

        loginBtn.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment2)
        }

        binding.apply {
            regSucs.setOnClickListener {
                val firstName = etFirstName.text.toString().trim()
                val lastName = etLastName.text.toString().trim()
                val userID = etStudentID.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString()

                var hasErrors = false

                if (userID.isEmpty()) {
                    etStudentID.apply {
                        requestFocus()
                        error = "Student/Staff ID is empty"
                    }
                    hasErrors = true
                } else if (!userID.contains('-')) {
                    etStudentID.apply {
                        requestFocus()
                        error = "Student ID must contain a hyphen (-)"
                    }
                    hasErrors = true
                }

                if (firstName.isEmpty()) {
                    etFirstName.apply {
                        requestFocus()
                        error = "Firstname is empty"
                    }
                    hasErrors = true
                }

                if (lastName.isEmpty()) {
                    etLastName.apply {
                        requestFocus()
                        error = "Lastname is empty"
                    }
                    hasErrors = true
                }

                if (email.isEmpty()) {
                    etEmail.apply {
                        requestFocus()
                        error = "Email is empty"
                    }
                    hasErrors = true
                }

                if (password.isEmpty()) {
                    etPassword.apply {
                        requestFocus()
                        error = "Password is empty"
                    }
                    hasErrors = true
                }

                if (hasErrors) {
                    Log.e(TAG, "All fields are required")
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    try {
                        val result = firebaseAuth.fetchSignInMethodsForEmail(email).await()
                        if (result.signInMethods?.isNotEmpty() == true) {
                            binding.etEmail.apply {
                                requestFocus()
                                error = "Email is already registered"
                            }
                        } else {
                            val user = Users(
                                firstName = firstName,
                                lastName = lastName,
                                userID = userID,
                                email = email,
                                imagePath = ""
                            )
                            registerViewModel.createAccountWithEmailAndPassword(user, password)

                            viewLifecycleOwner.lifecycleScope.launch {
                                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                                    registerViewModel.validation.collect { validation ->
                                        if (validation.userID is RegisterValidation.Failed) {
                                            withContext(Dispatchers.Main) {
                                                binding.etStudentID.apply {
                                                    requestFocus()
                                                    error = validation.userID.message
                                                }
                                            }
                                        }

                                        if (validation.email is RegisterValidation.Failed) {
                                            withContext(Dispatchers.Main) {
                                                binding.etEmail.apply {
                                                    requestFocus()
                                                    error = validation.email.message
                                                }
                                            }
                                        }

                                        if (validation.password is RegisterValidation.Failed) {
                                            withContext(Dispatchers.Main) {
                                                binding.etPassword.apply {
                                                    requestFocus()
                                                    error = validation.password.message
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            viewLifecycleOwner.lifecycleScope.launch {
                                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                                    registerViewModel.register.collect { resource ->
                                        when (resource) {
                                            is Resource.Loading -> {
                                                regSucs.startAnimation()
                                            }
                                            is Resource.Success -> {
                                                regSucs.revertAnimation()
                                                val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                                sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()
                                                startActivity(Intent(activity, ShoppingActivity::class.java))
                                            }
                                            is Resource.Error -> {
                                                Log.e(TAG, resource.message.toString())
                                                regSucs.revertAnimation()
                                                binding.etEmail.apply {
                                                    requestFocus()
                                                    error = resource.message.toString()
                                                }
                                            }
                                            else -> Unit
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking email: ${e.message}")
                        binding.etEmail.apply {
                            requestFocus()
                            error = "Error checking email availability"
                        }
                    }
                }
            }
        }
    }
}

