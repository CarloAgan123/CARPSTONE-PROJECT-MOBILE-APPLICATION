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
import com.example.myfirstapplication.data.CustomerTransaction
import com.example.myfirstapplication.data.OrderDetails
import com.example.myfirstapplication.data.Users
import com.example.myfirstapplication.databinding.FragmentAllOrdersBinding
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

class AllOrdersFragment: Fragment(R.layout.fragment_all_orders) {

    private lateinit var binding: FragmentAllOrdersBinding
    private lateinit var trackOrderAdapter: TrackOrderAdapter
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
        binding = FragmentAllOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
        trackOrderAdapter = TrackOrderAdapter()

        setupAllOrderList()
        fetchAllOrderList()

        trackOrderAdapter.onClick = { orderDetails ->
            val bundle = Bundle().apply {
                putString("ORDER_ID", orderDetails.orderId)
            }
            findNavController().navigate(R.id.action_allOrdersFragment_to_transactionFragment, bundle)
        }

        binding.ivClose.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

    }

    private fun setupAllOrderList() {
        trackOrderAdapter = TrackOrderAdapter()
        binding.rvAllOrderList.apply {
            layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
            adapter = trackOrderAdapter
        }
    }

    private fun fetchAllOrderList() {
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
                                getCustomerAllOrderList()
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

    private fun getCustomerAllOrderList() {
        databaseReference.child("customerTransaction").addListenerForSingleValueEvent(object :
            ValueEventListener {
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
                getAllOrderList()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching customer orders", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getAllOrderList() {
        databaseReference.child("OrderDetails").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listOrder = mutableListOf<OrderDetails>()
                for (orderSnapshot in snapshot.children) {
                    try {
                        val order = orderSnapshot.getValue(OrderDetails::class.java)
                        if (order != null && cusOrderList.any { it.customerTransactionID == order.orderId}) {
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