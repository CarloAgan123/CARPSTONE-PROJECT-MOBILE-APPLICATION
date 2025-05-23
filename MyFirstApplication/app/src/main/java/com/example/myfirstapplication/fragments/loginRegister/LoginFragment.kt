package com.example.myfirstapplication.fragments.loginRegister

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myfirstapplication.R
import com.example.myfirstapplication.activities.ProgressActivity
import com.example.myfirstapplication.activities.ShoppingActivity
import com.example.myfirstapplication.databinding.FragmentLoginBinding
import com.example.myfirstapplication.dialog.setupBottomSheetDialog
import com.example.myfirstapplication.uitel.ProgressDialog
import com.example.myfirstapplication.util.Resource
import com.example.myfirstapplication.viewmodel.LoginViewModel
import com.example.myfirstapplication.viewmodel.LoginViewModelFactory
import com.github.leandroborgesferreira.loadingbutton.customViews.CircularProgressButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "LoginFragment"

class LoginFragment : Fragment(R.layout.fragment_login) {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var dbReference: DatabaseReference
    private lateinit var progressDialog : ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressDialog = ProgressDialog(requireActivity())

        firebaseAuth = FirebaseAuth.getInstance()
        dbReference = FirebaseDatabase.getInstance().reference
        val factory = LoginViewModelFactory(firebaseAuth, FirebaseDatabase.getInstance())
        loginViewModel = ViewModelProvider(this, factory).get(LoginViewModel::class.java)

        val registerBtn: MaterialTextView = view.findViewById(R.id.tvLogin2)
        val loginBtn: CircularProgressButton = view.findViewById(R.id.loginBtn)

        registerBtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment2)
        }

        binding.tvForgotPassword.setOnClickListener {
            setupBottomSheetDialog { email ->
                val sendBtn: CircularProgressButton? = view?.findViewById(R.id.buttonSendResetPassword)
                val etResetPassword: EditText? = view?.findViewById(R.id.etResetPassword)

                var hasErrors = false

                if (email.trim().isEmpty()) {
                    etResetPassword?.apply {
                        requestFocus()
                        error = "Email cannot be empty"
                    }
                    hasErrors = true
                }

                if (!hasErrors) {
                    loginViewModel.resetPassword(email)

                    lifecycleScope.launchWhenStarted {
                        loginViewModel.resetPassword.collect {
                            when (it) {
                                "Loading" -> {
                                    sendBtn?.startAnimation()
                                }
                                "Success" -> {
                                    sendBtn?.revertAnimation()
                                    Snackbar.make(requireView(), "Reset link was sent to your email", Snackbar.LENGTH_LONG).show()
                                }
                                else -> {
                                    sendBtn?.revertAnimation()
                                    Snackbar.make(requireView(), "Error: $it", Snackbar.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            }
        }



        loginBtn.setOnClickListener {
            progressDialog.loadingStart()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val userID = binding.etStudentID.text.toString().trim()

            if (validateInputs(email, password, userID)) {
                performLogin(email, password, userID, loginBtn)
            } else
            {
                progressDialog.loadingDismiss()
            }
        }
    }

    private fun validateInputs(email: String, password: String, userID: String): Boolean {
        var hasErrors = false

        if (userID.isEmpty()) {
            binding.etStudentID.apply {
                requestFocus()
                error = "Student/Staff ID is empty"
            }
            hasErrors = true
        } else if (!userID.contains('-')) {
            binding.etStudentID.apply {
                requestFocus()
                error = "Student/Staff ID must contain hyphen (-)"
            }
            hasErrors = true
        }

        if (email.isEmpty()) {
            binding.etEmail.apply {
                requestFocus()
                error = "Email is empty"
            }
            hasErrors = true
        }

        if (password.isEmpty()) {
            binding.etPassword.apply {
                error = "Password is empty"
                requestFocus()
            }
            hasErrors = true
        }

        return !hasErrors
    }

    private fun performLogin(email: String, password: String, userID: String, loginBtn: CircularProgressButton) {
        //loginBtn.startAnimation()
        lifecycleScope.launch {
            try {
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
                val user: FirebaseUser? = authResult.user
                if (user != null) {
                    checkEmailAndUserIDMatch(email, userID, loginBtn)
                    val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()
                } else {
                    withContext(Dispatchers.Main) {
                        //loginBtn.revertAnimation()
                        progressDialog.loadingDismiss()
                        Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    //loginBtn.revertAnimation()
                    progressDialog.loadingDismiss()
                    Toast.makeText(context, "Email and password do not match", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, e.message.toString())
                }
            }
        }
    }

    private suspend fun checkEmailAndUserIDMatch(email: String, userID: String, loginBtn: CircularProgressButton) {
        try {
            val snapshot = dbReference.child("users").child(userID).get().await()
            val storedEmail = snapshot.child("email").value as? String
            if (storedEmail == email) {
                progressDialog.showSuccess("Loading succeeded!")
                //val bitmap = getBitmapFromDrawable(R.drawable.icon_done)
                //loginBtn.doneLoadingAnimation(Color.GREEN, bitmap)
                withContext(Dispatchers.Main) {
                    val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()
                    startActivity(Intent(activity, ProgressActivity::class.java))
                    requireActivity().finish()
                }
            } else {
                withContext(Dispatchers.Main) {
                    progressDialog.showFailure("Loading failed!")
                    //loginBtn.revertAnimation()
                    Toast.makeText(context, "User ID and email do not match", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                //loginBtn.revertAnimation()
                progressDialog.showFailure("Loading failed!")
                Toast.makeText(context, "Failed to verify User ID and email", Toast.LENGTH_SHORT).show()
                Log.e(TAG, e.message.toString())
            }
        } finally {
            //progressDialog.loadingDismiss()
        }
    }

    private fun getBitmapFromDrawable(drawableId: Int): Bitmap {
        val drawable: Drawable? = resources.getDrawable(drawableId, null)
        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }
}
