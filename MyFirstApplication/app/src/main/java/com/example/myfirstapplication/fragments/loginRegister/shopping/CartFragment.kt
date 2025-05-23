package com.example.myfirstapplication.fragments.loginRegister.shopping

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myfirstapplication.R
import com.example.myfirstapplication.adapters.CartAdapter
import com.example.myfirstapplication.data.CartItem
import com.example.myfirstapplication.databinding.FragmentCartBinding
import com.example.myfirstapplication.util.showBottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CartFragment : Fragment(R.layout.fragment_cart) {

    private lateinit var binding: FragmentCartBinding
    private lateinit var adapter: CartAdapter
    private var totalOrderPrice: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showBottomNavigationView()

        adapter = CartAdapter(requireContext()) {
            updateTotalPriceOnly()
        }
        binding.rvCart.layoutManager = LinearLayoutManager(context)
        binding.rvCart.adapter = adapter

        fetchCartItems()
    }

    private fun fetchCartItems() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance()
        val cartRef = database.getReference("cartItems").child(userId)

        cartRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val groupedItems = mutableListOf<Any>()
                val productMap = mutableMapOf<String, MutableList<CartItem>>()

                for (data in snapshot.children) {
                    val cartItem = data.getValue(CartItem::class.java)
                    cartItem?.let {
                        val stallName = it.productStallName ?: "Unknown Stall"
                        if (productMap.containsKey(stallName)) {
                            productMap[stallName]?.add(it)
                        } else {
                            productMap[stallName] = mutableListOf(it)
                        }
                    }
                }

                for ((productStallName, products) in productMap) {
                    groupedItems.add(productStallName)
                    groupedItems.addAll(products)
                }

                adapter.setItems(groupedItems)

                if (groupedItems.isEmpty()) {
                    binding.layoutCartEmpty.visibility = View.VISIBLE
                    binding.rvCart.visibility = View.GONE
                } else {
                    binding.layoutCartEmpty.visibility = View.GONE
                    binding.rvCart.visibility = View.VISIBLE
                }

                updateTotalPriceOnly()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    context,
                    "Failed to load cart items: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        binding.tvBtnDeleteItemCart.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val database = FirebaseDatabase.getInstance()
            database.reference.child("cartItems").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val checkedItems = mutableListOf<String>()
                            val productNames = mutableListOf<String>()

                            for (cartItemSnapshot in snapshot.children) {
                                val isChecked =
                                    cartItemSnapshot.child("checked").getValue(Boolean::class.java)
                                        ?: false
                                if (isChecked) {
                                    val cartItemId = cartItemSnapshot.key
                                    val productName = cartItemSnapshot.child("productName")
                                        .getValue(String::class.java) ?: "Unknown"
                                    if (cartItemId != null) {
                                        checkedItems.add(cartItemId)
                                        productNames.add(productName)
                                    }
                                }
                            }

                            if (checkedItems.isNotEmpty()) {
                                val productNamesStr = productNames.joinToString("\n")
                                AlertDialog.Builder(context)
                                    .setTitle("Confirm Deletion")
                                    .setMessage("Are you sure you want to delete the following items?\n$productNamesStr")
                                    .setPositiveButton("Yes") { _, _ ->
                                        for (itemId in checkedItems) {
                                            database.reference.child("cartItems").child(userId)
                                                .child(itemId).removeValue()
                                        }
                                        Toast.makeText(
                                            context,
                                            "Checked items deleted.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        fetchCartItems()
                                    }
                                    .setNegativeButton("No", null)
                                    .show()
                            } else {
                                Toast.makeText(context, "No items checked.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } else {
                            Toast.makeText(context, "No cart items found.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Error loading cart items.", Toast.LENGTH_SHORT)
                            .show()
                    }
                })
        }

        binding.buttonBuyNow.setOnClickListener {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val database = FirebaseDatabase.getInstance()
            database.reference.child("cartItems").child(userId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val checkedItems = mutableListOf<CartItem>()

                            for (cartItemSnapshot in snapshot.children) {
                                val isChecked =
                                    cartItemSnapshot.child("checked").getValue(Boolean::class.java)
                                        ?: false
                                if (isChecked) {
                                    val cartItem = cartItemSnapshot.getValue(CartItem::class.java)
                                    cartItem?.let { checkedItems.add(it) }
                                }
                            }

                            if (checkedItems.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "No items selected for checkout.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return
                            }

                            val firstStallName = checkedItems.firstOrNull()?.productStallName
                            val allSameStall =
                                checkedItems.all { it.productStallName == firstStallName }

                            if (!allSameStall) {
                                AlertDialog.Builder(context)
                                    .setTitle("Order Limit")
                                    .setMessage("You can only order from one stall at a time. Please select items from the same stall.")
                                    .setPositiveButton("OK", null)
                                    .show()
                                return
                            }

                            val checkedItemsArrayList = ArrayList(checkedItems)

                            val bundle = Bundle().apply {
                                putParcelableArrayList("checkedItems", checkedItemsArrayList)
                                putDouble("totalPrice", totalOrderPrice)
                            }

                            findNavController().navigate(
                                R.id.action_cartFragment_to_checkoutFragment,
                                bundle
                            )

                        } else {
                            Toast.makeText(context, "No cart items found.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Error loading cart items.", Toast.LENGTH_SHORT)
                            .show()
                    }
                })
        }
    }

    private fun updateTotalPriceOnly() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val cartRef = FirebaseDatabase.getInstance().getReference("cartItems").child(userId)
        cartRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalPrice = 0.0
                for (cartItemSnapshot in snapshot.children) {
                    val isChecked =
                        cartItemSnapshot.child("checked").getValue(Boolean::class.java) ?: false
                    if (isChecked) {
                        val cartItem = cartItemSnapshot.getValue(CartItem::class.java)
                        cartItem?.let {
                            totalPrice += it.subTotalPrice
                        }
                    }
                }
                binding.tvTotalPrice.text = String.format("₱%.2f", totalPrice)
                totalOrderPrice = totalPrice
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to update total price", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
