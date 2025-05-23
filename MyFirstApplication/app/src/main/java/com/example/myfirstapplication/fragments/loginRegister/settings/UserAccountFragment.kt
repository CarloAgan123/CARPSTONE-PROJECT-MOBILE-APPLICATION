package com.example.myfirstapplication.fragments.loginRegister.settings

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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.myfirstapplication.R
import com.example.myfirstapplication.databinding.FragmentUserAccountBinding
import com.example.myfirstapplication.data.Users
import com.example.myfirstapplication.dialog.setupNewBottomSheetDialog
import com.example.myfirstapplication.viewmodel.LoginViewModel
import com.example.myfirstapplication.viewmodel.LoginViewModelFactory
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.github.leandroborgesferreira.loadingbutton.customViews.CircularProgressButton
import android.content.Intent
import android.app.Activity
import android.net.Uri

class UserAccountFragment : Fragment(R.layout.fragment_user_account) {

    private lateinit var binding: FragmentUserAccountBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var loginViewModel: LoginViewModel
    private var selectedImageUri: Uri? = null

    private var userListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUserAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val factory = LoginViewModelFactory(auth, FirebaseDatabase.getInstance())
        loginViewModel = ViewModelProvider(this, factory).get(LoginViewModel::class.java)

        binding.imageCloseUserAccount.setOnClickListener {
            findNavController().navigate(R.id.action_userAccountFragment_to_profileFragment)
        }

        database = FirebaseDatabase.getInstance().reference.child("users")

        val userId = auth.currentUser?.uid
        if (userId != null) {
            auth.currentUser?.let { user ->
                val email = user.email
                if (email != null) {
                    userListener?.let { database.removeEventListener(it) }

                    userListener = database.orderByChild("email").equalTo(email).addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (isAdded) {
                                if (snapshot.exists()) {
                                    val userSnapshot = snapshot.children.firstOrNull()
                                    val user = userSnapshot?.getValue(Users::class.java)
                                    if (user != null) {
                                        binding.etUserID.setText(user.userID)
                                        binding.edFirstName.setText(user.firstName)
                                        binding.edLastName.setText(user.lastName)
                                        binding.edEmail.setText(user.email)
                                        if (user.imagePath.isNotEmpty()) {
                                            Glide.with(this@UserAccountFragment)
                                                .load(user.imagePath)
                                                .into(binding.imageUser)
                                        } else {
                                            binding.imageUser.setImageResource(R.drawable.error_image)
                                        }
                                    } else {
                                        showToast("User data is null")
                                    }
                                } else {
                                    showToast("Failed")
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            showToast("Error fetching data: ${error.message}")
                        }
                    })
                } else {
                    showToast("Email not available")
                }
            }
        } else {
            showToast("User not authenticated")
        }

        val saveButton: CircularProgressButton = view.findViewById(R.id.buttonSave)
        saveButton.setOnClickListener {
            saveButton.startAnimation()
            val firstName = binding.edFirstName.text.toString()
            val lastName = binding.edLastName.text.toString()

            if (userId != null) {
                auth.currentUser?.let { user ->
                    val email = user.email
                    if (email != null) {
                        database.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (isAdded) {
                                    if (snapshot.exists()) {
                                        val userSnapshot = snapshot.children.firstOrNull()
                                        val userRef = userSnapshot?.ref
                                        val updates = hashMapOf<String, Any>(
                                            "firstName" to firstName,
                                            "lastName" to lastName
                                        )
                                        userRef?.updateChildren(updates)?.addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                if (selectedImageUri != null) {
                                                    uploadImageAndSaveUrl()
                                                } else {
                                                    saveButton.doneLoadingAnimation(
                                                        Color.GREEN,
                                                        getBitmapFromDrawable(R.drawable.icon_done)
                                                    )
                                                    showToast("Profile updated successfully")
                                                }
                                            } else {
                                                saveButton.revertAnimation()
                                                showToast("Failed to update profile")
                                            }
                                        }
                                    } else {
                                        saveButton.revertAnimation()
                                        showToast("User data not found")
                                    }
                                } else {
                                    saveButton.revertAnimation()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                saveButton.revertAnimation()
                                showToast("Error updating data: ${error.message}")
                            }
                        })
                    }
                }
            } else {
                saveButton.revertAnimation()
                showToast("User ID not found")
            }
        }

        binding.tvUpdatePassword.setOnClickListener {
            setupNewBottomSheetDialog { email ->
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

        binding.imageEdit.setOnClickListener {
            val imagePickerIntent = Intent(Intent.ACTION_PICK)
            imagePickerIntent.type = "image/*"
            startActivityForResult(imagePickerIntent, PICK_IMAGE_REQUEST)
        }
    }

    private fun showToast(message: String) {
        context?.let { ctx ->
            Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageAndSaveUrl() {
        val imageUri = selectedImageUri ?: return
        val storageRef = FirebaseStorage.getInstance().reference.child("images/${auth.currentUser?.uid}_profile.jpg")
        val uploadTask = storageRef.putFile(imageUri)

        uploadTask.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                saveImageUrlToDatabase(uri.toString())
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to retrieve download URL", Toast.LENGTH_SHORT).show()
                binding.buttonSave.revertAnimation()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
            binding.buttonSave.revertAnimation()
        }
    }

    private fun saveImageUrlToDatabase(imageUrl: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            auth.currentUser?.let { user ->
                val email = user.email
                if (email != null) {
                    database.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (isAdded) {
                                if (snapshot.exists()) {
                                    val userSnapshot = snapshot.children.firstOrNull()
                                    val userRef = userSnapshot?.ref
                                    val imagePathRef = userRef?.child("imagePath")
                                    imagePathRef?.setValue(imageUrl)?.addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(context, "Image updated successfully", Toast.LENGTH_SHORT).show()
                                            Glide.with(this@UserAccountFragment)
                                                .load(imageUrl)
                                                .into(binding.imageUser)
                                            binding.buttonSave.doneLoadingAnimation(
                                                Color.GREEN,
                                                getBitmapFromDrawable(R.drawable.icon_done)
                                            )
                                        } else {
                                            Toast.makeText(context, "Failed to update image", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Fragment not attached", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(context, "Error updating image: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            binding.imageUser.setImageURI(selectedImageUri)
        }
    }

    private fun getBitmapFromDrawable(drawableId: Int): Bitmap {
        val drawable: Drawable? = ContextCompat.getDrawable(requireContext(), drawableId)
        return if (drawable is BitmapDrawable) {
            drawable.bitmap
        } else {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }


    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }
}
