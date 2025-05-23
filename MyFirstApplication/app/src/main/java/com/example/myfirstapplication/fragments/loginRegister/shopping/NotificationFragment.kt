package com.example.myfirstapplication.fragments.loginRegister.shopping

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myfirstapplication.R
import com.example.myfirstapplication.adapters.NotificationAdapter
import com.example.myfirstapplication.data.NotificationItem
import com.example.myfirstapplication.data.Users
import com.example.myfirstapplication.databinding.FragmentNotificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NotificationFragment : Fragment(R.layout.fragment_notification) {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private val viewedNotifications = mutableSetOf<String>()
    private lateinit var notificationAdapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationItem>()
    private lateinit var binding: FragmentNotificationBinding
    private var isFirebaseInitialized = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        isFirebaseInitialized = true

        setupNotificationList()
        checkOrdersAndNotify(requireContext())
    }

    private fun setupNotificationList() {
        notificationAdapter = NotificationAdapter()
        binding.rvNotification.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
        }
    }

    fun checkOrdersAndNotify(context: Context) {
        if (!isFirebaseInitialized) {
            Toast.makeText(context, "Firebase Authentication is not initialized", Toast.LENGTH_SHORT).show()
            return
        }

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
                                                checkOrderDetails(customerTransactionID, context)
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

    private fun checkOrderDetails(customerTransactionID: String, context: Context) {
        val orderDetailsRef = database.getReference("OrderDetails")
        orderDetailsRef.orderByChild("orderId").equalTo(customerTransactionID).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                handleOrderUpdate(snapshot, context)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                handleOrderUpdate(snapshot, context)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error listening for order updates: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleOrderUpdate(snapshot: DataSnapshot, context: Context) {
        val orderId = snapshot.child("orderId").value.toString()
        val orderStatus = snapshot.child("orderStatus").value.toString()
        if (!viewedNotifications.contains(orderId)) {
            markNotificationAsViewed(orderId, orderStatus)
            viewedNotifications.add(orderId)
            val notificationItem = NotificationItem("Order Update", "Your order #$orderId is now $orderStatus", getCurrentTimestamp())
            notificationList.add(notificationItem)
            notificationAdapter.setNotifications(notificationList)
        }
    }

    private fun markNotificationAsViewed(orderId: String, orderStatus: String) {
        val userId = firebaseAuth.currentUser?.uid
        val viewedNotificationRef = database.getReference("viewedNotifications/$userId/$orderId")
        viewedNotificationRef.setValue(mapOf("viewed" to true, "previousOrderStatus" to orderStatus)).addOnSuccessListener {}.addOnFailureListener { exception ->
            Toast.makeText(requireContext(), "Failed to mark notification as viewed: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentTimestamp(): String {
        return System.currentTimeMillis().toString()
    }

    fun onNotificationViewed(orderId: String) {
        viewedNotifications.add(orderId)
        markNotificationAsViewed(orderId, "")
    }
}
