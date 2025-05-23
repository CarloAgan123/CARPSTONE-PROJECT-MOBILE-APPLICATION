package com.example.myfirstapplication.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.myfirstapplication.R
import com.example.myfirstapplication.data.Users
import com.example.myfirstapplication.databinding.ActivityShoppingBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import com.google.firebase.database.ChildEventListener

class ShoppingActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: ActivityShoppingBinding
    private lateinit var navController: NavController
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var dataRef: DatabaseReference
    private val viewedNotifications = mutableSetOf<String>()
    private val previousOrderStatus = mutableMapOf<String, String>()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var checkOrdersRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        FirebaseMessaging.getInstance().isAutoInitEnabled = true

        binding = ActivityShoppingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        dataRef = database.getReference("users")

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.shoppingHostFragment) as? NavHostFragment
        navController = navHostFragment?.navController ?: throw IllegalStateException("NavHostFragment not found")

        binding.bottomNavigation.setupWithNavController(navController)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        getToken { token ->
            if (token != null) {
                storeToken(token)
            } else {
                Log.e("ShoppingActivity", "Failed to fetch token")
            }
        }

        checkOrdersRunnable = Runnable {
            checkOrdersAndNotify()
            handler.postDelayed(checkOrdersRunnable, 1000)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(checkOrdersRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(checkOrdersRunnable)
    }

    private fun getToken(callback: (String?) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("ShoppingActivity", "Fetching FCM registration token failed", task.exception)
                callback(null)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("ShoppingActivity", "Current token: $token")
            callback(token)
        }
    }

    private fun storeToken(token: String) {
        val userId = firebaseAuth.currentUser?.uid
        val userEmail = firebaseAuth.currentUser?.email

        if (userId != null && userEmail != null) {
            dataRef.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userSnapshot = snapshot.children.firstOrNull()
                        val user = userSnapshot?.getValue(Users::class.java)
                        if (user != null) {
                            val usersID = user.userID
                            val userTokenRef = database.getReference("userTokens/$usersID")
                            userTokenRef.setValue(token)
                                .addOnSuccessListener {
                                    Log.d("ShoppingActivity", "Token uploaded successfully")
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("ShoppingActivity", "Failed to upload token: ${exception.message}")
                                }
                        } else {
                            Log.e("ShoppingActivity", "User object could not be retrieved")
                        }
                    } else {
                        Log.e("ShoppingActivity", "No user found for email: $userEmail")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("storeToken", "Error fetching user data: ${error.message}")
                }
            })
        } else {
            Log.e("ShoppingActivity", "User not authenticated or email is null")
        }
    }

    private fun checkOrdersAndNotify() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            val userEmail = firebaseAuth.currentUser?.email
            if (userEmail != null) {
                val usersRef = database.getReference("users")
                usersRef.orderByChild("email").equalTo(userEmail).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val userSnapshot = snapshot.children.firstOrNull()
                            val user = userSnapshot?.getValue(Users::class.java)
                            if (user != null) {
                                val usersID = user.userID
                                val customerTransactionRef = database.getReference("customerTransaction")
                                customerTransactionRef.orderByChild("customerID").equalTo(usersID).addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(transactionSnapshot: DataSnapshot) {
                                        if (transactionSnapshot.exists()) {
                                            transactionSnapshot.children.forEach { transaction ->
                                                val customerTransactionID = transaction.child("customerTransactionID").value.toString()
                                                checkOrderDetails(customerTransactionID)
                                            }
                                        }
                                    }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }
    }

    private fun checkOrderDetails(customerTransactionID: String) {
        val orderDetailsRef = database.getReference("OrderDetails")
        orderDetailsRef.orderByChild("orderId").equalTo(customerTransactionID).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                handleOrderUpdate(snapshot)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                handleOrderUpdate(snapshot)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("checkOrderDetails", "Error listening for order updates: ${error.message}")
            }
        })
    }

    private fun handleOrderUpdate(snapshot: DataSnapshot) {
        val orderId = snapshot.child("orderId").value.toString()
        val orderStatus = snapshot.child("orderStatus").value.toString()
        val userId = firebaseAuth.currentUser?.uid

        if (userId != null) {
            val viewedNotificationRef = database.getReference("viewedNotifications/$userId/$orderId")
            viewedNotificationRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(viewedSnapshot: DataSnapshot) {
                    val previousStatus = viewedSnapshot.child("previousOrderStatus").value?.toString()
                    if (previousStatus == orderStatus) {
                        return
                    }

                    sendOrderNotification(orderId, orderStatus)

                    markNotificationAsViewed(orderId, orderStatus)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("handleOrderUpdate", "Error checking notification status: ${error.message}")
                }
            })
        }
    }


    private fun markNotificationAsViewed(orderId: String, orderStatus: String) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            val viewedNotificationRef = database.getReference("viewedNotifications/$userId/$orderId")
            viewedNotificationRef.setValue(mapOf("orderId" to true, "previousOrderStatus" to orderStatus)).addOnSuccessListener {}.addOnFailureListener { exception ->
                Log.e("markNotificationAsViewed", "Failed to mark notification as viewed: ${exception.message}")
            }
        }
    }

    private fun sendOrderNotification(orderId: String, orderStatus: String) {
        val notificationTitle = "Order Update"
        val notificationMessage = when (orderStatus) {
            "Processing" -> "Your order #$orderId is being accepted."
            "Ready" -> "Your order #$orderId is now ready, go to the stall and pick it up."
            "Finished" -> "Thank you for purchasing your order #$orderId."
            "Received" -> "Enjoy your food and order more delicious food at the stall!"
            "Declined" -> "Sorry to inform you that your order #$orderId is being declined by the stall!"
            else -> "Your order #$orderId is now $orderStatus"
        }

        val channelId = "order_channel_id"
        val intent = Intent(this, ShoppingActivity::class.java).apply {
            addFlags(FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_NEW_TASK)
            putExtra("showNotificationFragment", true)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.cdm_mobile_app_icon)
            .setContentTitle(notificationTitle)
            .setContentText(notificationMessage)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Order Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(orderId.hashCode(), builder.build())
    }



    override fun onStart() {
        super.onStart()
        Log.d("ShoppingActivity", "onStart called")
    }

    override fun onStop() {
        super.onStop()
        Log.d("ShoppingActivity", "onStop called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ShoppingActivity", "onDestroy called")
    }
}
