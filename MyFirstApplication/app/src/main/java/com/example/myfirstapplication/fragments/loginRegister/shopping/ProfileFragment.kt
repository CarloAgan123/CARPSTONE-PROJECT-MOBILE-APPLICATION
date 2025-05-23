package com.example.myfirstapplication.fragments.loginRegister.shopping

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.myfirstapplication.R
import com.example.myfirstapplication.activities.LoginActivity
import com.example.myfirstapplication.activities.ProgressActivity
import com.example.myfirstapplication.activities.ShoppingActivity
import com.example.myfirstapplication.data.Users
import com.example.myfirstapplication.databinding.FragmentProfileBinding
import com.example.myfirstapplication.util.showBottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showBottomNavigationView()

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference.child("users")

        showFirstName()

        binding.constraintProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_userAccountFragment)
        }

        binding.linearTrackOrder.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_trackOrderFragment)
        }

        binding.switchNotification.isEnabled = false

        binding.linearLogOut.setOnClickListener {
            if (isInternetAvailable(requireContext())) {
                AlertDialog.Builder(context)
                    .setTitle("Confirm Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes") { _, _ ->

                        val userId = auth.currentUser?.uid
                        val userEmail = auth.currentUser?.email
                        if (userId != null && userEmail != null) {
                            database.orderByChild("email").equalTo(userEmail)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            val userSnapshot = snapshot.children.firstOrNull()
                                            val user = userSnapshot?.getValue(Users::class.java)
                                            if (user != null) {
                                                val usersID = user.userID
                                                val userTokenRef = database.child("userTokens").child(usersID)
                                                Log.d("ProfileFragment", "UserID: $usersID")
                                                userTokenRef.removeValue()
                                                    .addOnSuccessListener {
                                                        FirebaseAuth.getInstance().signOut()
                                                        clearPreferences()
                                                        Log.d("ProfileFragment", "Successfully logged out")
                                                        startActivity(Intent(requireActivity(), LoginActivity::class.java))
                                                        requireActivity().finish()
                                                    }
                                                    .addOnFailureListener { exception ->
                                                        Log.e("ProfileFragment", "Failed to remove token: ${exception.message}")
                                                    }
                                            }
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e("ProfileFragment", "Error fetching user data: ${error.message}")
                                    }
                                })
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
            } else {
                binding.linearLogOut.isEnabled = false
                Log.w("ProfileFragment", "No internet connection. Please try again later.")
            }
        }


        binding.linearTransaction.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_transactionHistoryFragment)
        }

        binding.linearAllOrders.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_allOrdersFragment)
        }
    }

    private fun showFirstName() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            auth.currentUser?.let { user ->
                val email = user.email

                if (email != null) {
                    database.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val userSnapshot = snapshot.children.firstOrNull()
                                val user = userSnapshot?.getValue(Users::class.java)
                                if (user != null) {
                                    binding.tvUserName.text = user.firstName
                                    if (user.imagePath.isNotEmpty()) {
                                        Glide.with(this@ProfileFragment).load(user.imagePath).into(binding.imageUser)
                                    } else {
                                        binding.imageUser.setImageResource(R.drawable.error_image)
                                    }
                                } else {
                                    Log.w("ProfileFragment", "User data is null")
                                }
                            } else {
                                Log.w("ProfileFragment", "No data found for email")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("ProfileFragment", "Error fetching data: ${error.message}")
                        }
                    })
                } else {
                    Log.w("ProfileFragment", "Email not available")
                }
            }
        } else {
            clearPreferences()
            startActivity(Intent(activity, ProgressActivity::class.java))
            requireActivity().finish()
            Log.w("ProfileFragment", "User not authenticated")
        }
    }

    private fun clearPreferences() {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private val connectivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && isInternetAvailable(context)) {
                binding.linearLogOut.isEnabled = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().registerReceiver(connectivityReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(connectivityReceiver)
    }
}
