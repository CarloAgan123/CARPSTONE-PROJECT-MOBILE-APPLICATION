package com.example.myfirstapplication.fragments.loginRegister.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myfirstapplication.R
import com.example.myfirstapplication.adapters.TrackOrderAdapter
import com.example.myfirstapplication.adapters.TrackOrderPastAdapter
import com.example.myfirstapplication.adapters.TrackOrderYesterdayAdapter
import com.example.myfirstapplication.data.CustomerTransaction
import com.example.myfirstapplication.data.OrderDetails
import com.example.myfirstapplication.data.Users
import com.example.myfirstapplication.databinding.FragmentOrderListBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TrackOrderFragment: Fragment(R.layout.fragment_order_list) {

    private lateinit var binding: FragmentOrderListBinding
    private lateinit var trackOrderAdapter: TrackOrderAdapter
    private lateinit var trackOrderYesterdayAdapter: TrackOrderYesterdayAdapter
    private lateinit var trackOrderPastAdapter: TrackOrderPastAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var userEmail: String = ""
    private var userID: String = ""
    private var cusOrderList: List<CustomerTransaction> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOrderListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
        trackOrderAdapter = TrackOrderAdapter()

        setupOrderList()
        fetchOrderList()
        setupOrderListYesterday()
        fetchOrderListYesterday()
        setupOrderListPast()
        fetchOrderListPast()

        trackOrderAdapter.onClick = { orderDetails ->
            val bundle = Bundle().apply {
                putString("ORDER_ID", orderDetails.orderId)
            }
            findNavController().navigate(R.id.action_trackOrderFragment_to_transactionFragment, bundle)
        }

        trackOrderYesterdayAdapter.onClick = { orderDetails ->
            val bundle = Bundle().apply {
                putString("ORDER_ID", orderDetails.orderId)
            }
            findNavController().navigate(R.id.action_trackOrderFragment_to_transactionFragment, bundle)
        }

        trackOrderPastAdapter.onClick = { orderDetails ->
            val bundle = Bundle().apply {
                putString("ORDER_ID", orderDetails.orderId)
            }
            findNavController().navigate(R.id.action_trackOrderFragment_to_transactionFragment, bundle)
        }

        binding.ivClose.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupOrderListPast() {
        trackOrderPastAdapter = TrackOrderPastAdapter()
        binding.rvOrderListPast.apply {
            layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
            adapter = trackOrderPastAdapter
        }
    }

    private fun fetchOrderListPast() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            auth.currentUser?.email?.let { email ->
                val ref = databaseReference.child("users").orderByChild("email").equalTo(email)
                ref.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val userSnapshot = snapshot.children.firstOrNull()
                            val user = userSnapshot?.getValue(Users::class.java)
                            if (user != null) {
                                userEmail = user.email
                                getCustomerOrderListPast()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "Error fetching user data", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }

    private fun getCustomerOrderListPast() {
        databaseReference.child("customerTransaction").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val customerTransactionList = mutableListOf<CustomerTransaction>()
                for (customerTranListSnapshot in snapshot.children) {
                    try {
                        val cTL = customerTranListSnapshot.getValue(CustomerTransaction::class.java)
                        if (cTL != null && cTL.customerEmail == userEmail) {
                            customerTransactionList.add(cTL)
                        }
                    } catch (e: Exception) {
                        Log.e("TrackOrderFragment", "Error parsing transaction: ${e.message}")
                    }
                }
                cusOrderList = customerTransactionList
                getOrderListPast()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching customer orders", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getOrderListPast() {
        val dateFormat = SimpleDateFormat("EEE, MMMM d, yyyy", Locale.ENGLISH)
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Manila")
        val currentDate = dateFormat.format(Date())

        val calendar = Calendar.getInstance()
        calendar.timeZone = TimeZone.getTimeZone("Asia/Manila")
        calendar.add(Calendar.DATE, -1)
        val yesterdayDate = dateFormat.format(calendar.time)

        databaseReference.child("OrderDetails").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listOrder = mutableListOf<OrderDetails>()
                for (orderSnapshot in snapshot.children) {
                    try {
                        val order = orderSnapshot.getValue(OrderDetails::class.java)
                        if (order != null  && order.orderDate != yesterdayDate && order.orderDate != currentDate && cusOrderList.any { it.customerTransactionID == order.orderId}) {
                            Log.d("TrackOrderFragment", "Fetched order: $order")
                            listOrder.add(order)
                        }
                    } catch (e: Exception) {
                        Log.e("TrackOrderFragment", "Failed to parse order: ${e.message}")
                    }
                }
                trackOrderPastAdapter.submitList(listOrder)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching order details", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupOrderListYesterday() {
        trackOrderYesterdayAdapter = TrackOrderYesterdayAdapter()
        binding.rvOrderListYesterday.apply {
            layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
            adapter = trackOrderYesterdayAdapter
        }
    }

    private fun fetchOrderListYesterday() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            auth.currentUser?.email?.let { email ->
                val ref = databaseReference.child("users").orderByChild("email").equalTo(email)
                ref.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val userSnapshot = snapshot.children.firstOrNull()
                            val user = userSnapshot?.getValue(Users::class.java)
                            if (user != null) {
                                userEmail = user.email
                                getCustomerOrderListYesterday()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "Error fetching user data", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }

    private fun getCustomerOrderListYesterday() {
        databaseReference.child("customerTransaction").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val customerTransactionList = mutableListOf<CustomerTransaction>()
                for (customerTranListSnapshot in snapshot.children) {
                    try {
                        val cTL = customerTranListSnapshot.getValue(CustomerTransaction::class.java)
                        if (cTL != null && cTL.customerEmail == userEmail) {
                            customerTransactionList.add(cTL)
                        }
                    } catch (e: Exception) {
                        Log.e("TrackOrderFragment", "Error parsing transaction: ${e.message}")
                    }
                }
                cusOrderList = customerTransactionList
                getOrderListYesterday()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching customer orders", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getOrderListYesterday() {

        val dateFormat = SimpleDateFormat("EEE, MMMM d, yyyy", Locale.ENGLISH)
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Manila")

        val calendar = Calendar.getInstance()
        calendar.timeZone = TimeZone.getTimeZone("Asia/Manila")
        calendar.add(Calendar.DATE, -1)
        val yesterdayDate = dateFormat.format(calendar.time)

        databaseReference.child("OrderDetails").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listOrder = mutableListOf<OrderDetails>()
                for (orderSnapshot in snapshot.children) {
                    try {
                        val order = orderSnapshot.getValue(OrderDetails::class.java)
                        if (order != null  && order.orderDate == yesterdayDate && cusOrderList.any { it.customerTransactionID == order.orderId}) {
                            Log.d("TrackOrderFragment", "Fetched order: $order")
                            listOrder.add(order)
                        }
                    } catch (e: Exception) {
                        Log.e("TrackOrderFragment", "Failed to parse order: ${e.message}")
                    }
                }
                trackOrderYesterdayAdapter.submitList(listOrder)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching order details", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupOrderList() {
        trackOrderAdapter = TrackOrderAdapter()
        binding.rvOrderList.apply {
            layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
            adapter = trackOrderAdapter
        }
    }



    private fun fetchOrderList() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            auth.currentUser?.email?.let { email ->
                val ref = databaseReference.child("users").orderByChild("email").equalTo(email)
                ref.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val userSnapshot = snapshot.children.firstOrNull()
                            val user = userSnapshot?.getValue(Users::class.java)
                            if (user != null) {
                                userEmail = user.email
                                getCustomerOrderList()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(requireContext(), "Error fetching user data", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
    }

    private fun getCustomerOrderList() {
        databaseReference.child("customerTransaction").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val customerTransactionList = mutableListOf<CustomerTransaction>()
                for (customerTranListSnapshot in snapshot.children) {
                    try {
                        val cTL = customerTranListSnapshot.getValue(CustomerTransaction::class.java)
                        if (cTL != null && cTL.customerEmail == userEmail) {
                            customerTransactionList.add(cTL)
                        }
                    } catch (e: Exception) {
                        Log.e("TrackOrderFragment", "Error parsing transaction: ${e.message}")
                    }
                }
                cusOrderList = customerTransactionList
                getOrderList()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching customer orders", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getOrderList() {

        val dateFormat = SimpleDateFormat("EEE, MMMM d, yyyy", Locale.ENGLISH)
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Manila")
        val currentDate = dateFormat.format(Date())

        databaseReference.child("OrderDetails").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listOrder = mutableListOf<OrderDetails>()
                for (orderSnapshot in snapshot.children) {
                    try {
                        val order = orderSnapshot.getValue(OrderDetails::class.java)
                        if (order != null && cusOrderList.any { it.customerTransactionID == order.orderId && order.orderDate == currentDate}) {
                            Log.d("TrackOrderFragment", "Fetched order: $order")
                            listOrder.add(order)
                        }
                    } catch (e: Exception) {
                        Log.e("TrackOrderFragment", "Failed to parse order: ${e.message}")
                    }
                }
                trackOrderAdapter.submitList(listOrder)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching order details", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
