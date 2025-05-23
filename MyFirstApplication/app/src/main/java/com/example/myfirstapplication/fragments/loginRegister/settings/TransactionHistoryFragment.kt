package com.example.myfirstapplication.fragments.loginRegister.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myfirstapplication.R
import com.example.myfirstapplication.adapters.TransactionHistoryCancelledAdapter
import com.example.myfirstapplication.adapters.TransactionHistoryDeclinedAdapter
import com.example.myfirstapplication.adapters.TransactionHistoryReceiveAdapter
import com.example.myfirstapplication.data.CustomerTransaction
import com.example.myfirstapplication.data.OrderDetails
import com.example.myfirstapplication.data.Users
import com.example.myfirstapplication.databinding.FragmentTransactionHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class TransactionHistoryFragment: Fragment(R.layout.fragment_transaction_history) {

    private lateinit var binding: FragmentTransactionHistoryBinding
    private lateinit var receivedAdapter: TransactionHistoryReceiveAdapter
    private lateinit var cancelledAdapter: TransactionHistoryCancelledAdapter
    private lateinit var declinedAdapter: TransactionHistoryDeclinedAdapter
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
        binding = FragmentTransactionHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference
        receivedAdapter = TransactionHistoryReceiveAdapter()
        cancelledAdapter = TransactionHistoryCancelledAdapter()
        declinedAdapter = TransactionHistoryDeclinedAdapter()

        setupReceivedList()
        fetchReceivedList()
        setupCancelledList()
        fetchCancelledList()
        setupDeclinedList()
        fetchDeclinedList()

        receivedAdapter.onClick = { orderDetails ->
            val bundle = Bundle().apply {
                putString("ORDER_ID", orderDetails.orderId)
            }
            findNavController().navigate(
                R.id.action_transactionHistoryFragment_to_transactionFragment,
                bundle
            )
        }
        cancelledAdapter.onClick = { orderDetails ->
            val bundle = Bundle().apply {
                putString("ORDER_ID", orderDetails.orderId)
            }
            findNavController().navigate(
                R.id.action_transactionHistoryFragment_to_transactionFragment,
                bundle
            )
        }
        declinedAdapter.onClick = { orderDetails ->
            val bundle = Bundle().apply {
                putString("ORDER_ID", orderDetails.orderId)
            }
            findNavController().navigate(
                R.id.action_transactionHistoryFragment_to_transactionFragment,
                bundle
            )
        }

        binding.ivExpandReceived.setOnClickListener {
            toggleSection(binding.rvOrderListReceived, binding.ivExpandReceivedIcon)
        }

        binding.ivExpandCancelled.setOnClickListener {
            toggleSection(binding.rvOrderListCancelled, binding.ivExpandCancelledIcon)
        }

        binding.ivExpandDeclined.setOnClickListener {
            toggleSection(binding.rvOrderListDeclined, binding.ivExpandDeclinedIcon)
        }

        binding.ivClose.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun toggleSection(recyclerView: RecyclerView, arrowIcon: ImageView) {
        if (recyclerView.visibility == View.VISIBLE) {
            recyclerView.visibility = View.GONE
            arrowIcon.setImageResource(R.drawable.arrow_down) // Collapsed state
        } else {
            recyclerView.visibility = View.VISIBLE
            arrowIcon.setImageResource(R.drawable.arrow_up) // Expanded state
        }
    }

    private fun setupReceivedList() {
        receivedAdapter = TransactionHistoryReceiveAdapter()
        binding.rvOrderListReceived.apply {
            layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
            adapter = receivedAdapter
        }
    }

    private fun fetchReceivedList() {
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
                                getReceivedList()
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

    private fun getReceivedList() {
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
                getOrderReceivedList()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching customer orders", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getOrderReceivedList() {
        databaseReference.child("OrderDetails").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listOrder = mutableListOf<OrderDetails>()
                for (orderSnapshot in snapshot.children) {
                    try {
                        val order = orderSnapshot.getValue(OrderDetails::class.java)
                        if (order != null && cusOrderList.any { it.customerTransactionID == order.orderId && order.orderStatus == "Received"}) {
                            Log.d("TrackOrderFragment", "Fetched order: $order")
                            listOrder.add(order)
                        }
                    } catch (e: Exception) {
                        Log.e("TrackOrderFragment", "Failed to parse order: ${e.message}")
                    }
                }
                receivedAdapter.submitList(listOrder)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching order details", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupCancelledList() {
        cancelledAdapter = TransactionHistoryCancelledAdapter()
        binding.rvOrderListCancelled.apply {
            layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
            adapter = cancelledAdapter
        }
    }

    private fun fetchCancelledList() {
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
                                getCancelledList()
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

    private fun getCancelledList() {
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
                getOrderCancelledList()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching customer orders", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getOrderCancelledList() {
        databaseReference.child("OrderDetails").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listOrder = mutableListOf<OrderDetails>()
                for (orderSnapshot in snapshot.children) {
                    try {
                        val order = orderSnapshot.getValue(OrderDetails::class.java)
                        if (order != null && cusOrderList.any { it.customerTransactionID == order.orderId && order.orderStatus == "Cancelled"}) {
                            Log.d("TrackOrderFragment", "Fetched order: $order")
                            listOrder.add(order)
                        }
                    } catch (e: Exception) {
                        Log.e("TrackOrderFragment", "Failed to parse order: ${e.message}")
                    }
                }
                cancelledAdapter.submitList(listOrder)
            }


            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching order details", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupDeclinedList() {
        declinedAdapter = TransactionHistoryDeclinedAdapter()
        binding.rvOrderListDeclined.apply {
            layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
            adapter = declinedAdapter
        }
    }

    private fun fetchDeclinedList() {
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
                                getDeclinedList()
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

    private fun getDeclinedList() {
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
                getOrderDeclinedList()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching customer orders", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getOrderDeclinedList() {
        databaseReference.child("OrderDetails").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listOrder = mutableListOf<OrderDetails>()
                for (orderSnapshot in snapshot.children) {
                    try {
                        val order = orderSnapshot.getValue(OrderDetails::class.java)
                        if (order != null && cusOrderList.any { it.customerTransactionID == order.orderId && order.orderStatus == "Declined"}) {
                            Log.d("TrackOrderFragment", "Fetched order: $order")
                            listOrder.add(order)
                        }
                    } catch (e: Exception) {
                        Log.e("TrackOrderFragment", "Failed to parse order: ${e.message}")
                    }
                }
                declinedAdapter.submitList(listOrder)
            }


            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error fetching order details", Toast.LENGTH_SHORT).show()
            }
        })
    }

}