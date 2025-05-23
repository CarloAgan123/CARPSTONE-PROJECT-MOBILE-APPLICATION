package com.example.myfirstapplication.fragments.loginRegister.shopping

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myfirstapplication.R
import com.example.myfirstapplication.adapters.OrderAdapter
import com.example.myfirstapplication.data.CartItem
import com.example.myfirstapplication.data.CustomerTransaction
import com.example.myfirstapplication.data.OrderDetails
import com.example.myfirstapplication.data.ProductList
import com.example.myfirstapplication.data.StallPaymentMethods
import com.example.myfirstapplication.data.Users
import com.example.myfirstapplication.databinding.FragmentCheckoutBinding
import com.example.myfirstapplication.uitel.ProgressDialog
import com.example.myfirstapplication.util.hideBottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random
import java.text.SimpleDateFormat
import java.util.*

class CheckoutFragment : Fragment(R.layout.fragment_checkout) {

    private lateinit var binding: FragmentCheckoutBinding
    private lateinit var checkedItems: MutableList<CartItem>
    private lateinit var adapter: OrderAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    private var orderIdentification = generateShortId(6)
    private var customerNameTransaction: String = ""
    private var customerIDTransaction: String = ""
    private var customerEmailTransaction: String = ""
    private var currentType: String = ""
    private var selectedPayMethod: String = ""
    private var paymentStatusIndicator: String = ""
    private lateinit var progressDialog: ProgressDialog
    private var totalPrice: Double = 0.00

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        val view = binding.root

        hideBottomNavigationView()

        progressDialog = ProgressDialog(requireActivity())

        firebaseAuth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().reference

        arguments?.let {
            checkedItems = it.getParcelableArrayList<CartItem>("checkedItems")?.toMutableList() ?: mutableListOf()
        }

        val orderTotalPrice = arguments?.getDouble("totalPrice")!!

        binding.tvOverallTotalPrice.text = "₱${orderTotalPrice.toString()}"

        adapter = OrderAdapter(checkedItems) {
            displayTotalPrice()
        }
        binding.orderItemsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.orderItemsRecyclerView.adapter = adapter

        currentType = binding.tvOrderType.toString()

        setupPaymentMethodSpinner()
        displayStallNumber()

        binding.placeOrderButton.setOnClickListener {
            progressDialog.loadingStart()
            val selectedPaymentMethod = binding.paymentMethodSpinner.selectedItem.toString()
            retrieveProductOriginalPrice(selectedPaymentMethod)
        }

        binding.backArrow.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        if (checkedItems.isNotEmpty()) {
            binding.stallName.text = checkedItems[0].productStallName
            binding.tvOrderType.text = checkedItems[0].cartItemType
        }

        return view
    }

    private fun setupPaymentMethodSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.payment_methods,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.paymentMethodSpinner.adapter = adapter

        binding.paymentMethodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedPaymentMethod = parent.getItemAtPosition(position).toString()
                if (selectedPaymentMethod == "Pay on Pickup") {
                    paymentStatusIndicator = "Not Paid"
                    selectedPayMethod = "Pay on Pickup"
                } else {
                    paymentStatusIndicator = "Not Paid"
                    selectedPayMethod = "GCash"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }
    }


    private fun retrieveProductOriginalPrice(selectedPaymentMethod: String) {
        val productList = mutableListOf<ProductList>()
        var orderTotalPrice = 0.00

        for (item in checkedItems) {
            val productId = item.productId

            databaseReference.child("Products").child(productId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            val productOriginalPrice = snapshot.child("productPrice").getValue(String::class.java) ?: "0"
                            productList.add(
                                ProductList(
                                    name = item.productName,
                                    price = productOriginalPrice,
                                    quantity = item.productQuantity.toString(),
                                    subTotalPrice = item.subTotalPrice.toString()
                                )
                            )
                            orderTotalPrice += item.subTotalPrice
                        } else {
                            databaseReference.child("MenuItems").child(productId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(menuSnapshot: DataSnapshot) {
                                        val menuOriginalPrice = menuSnapshot.child("menuPrice").getValue(String::class.java) ?: "0"
                                        productList.add(
                                            ProductList(
                                                name = item.productName,
                                                price = menuOriginalPrice,
                                                quantity = item.productQuantity.toString(),
                                                subTotalPrice = item.subTotalPrice.toString()
                                            )
                                        )
                                        orderTotalPrice += item.subTotalPrice

                                        if (productList.size == checkedItems.size) {
                                            getStallID { stallID ->
                                                val orderDetails = createOrderDetails(selectedPaymentMethod, productList, stallID, orderTotalPrice)
                                                saveOrderDetails(orderDetails)
                                            }
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Toast.makeText(
                                            requireContext(),
                                            "Error retrieving menu price: ${error.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                })
                        }

                        if (productList.size == checkedItems.size) {
                            getStallID { stallID ->
                                val orderDetails = createOrderDetails(selectedPaymentMethod, productList, stallID, orderTotalPrice)
                                saveOrderDetails(orderDetails)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            requireContext(),
                            "Error retrieving product price: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
    }



    private fun createOrderDetails(
        selectedPaymentMethod: String,
        productList: List<ProductList>,
        stallID: String,
        totalPrice: Double
    ): OrderDetails {
        return OrderDetails(
            orderId = orderIdentification,
            orderDate = getCurrentDate(),
            orderStatus = "Pending",
            orderTime = getCurrentTime(),
            paid = paymentStatusIndicator,
            paymentMethod = selectedPaymentMethod,
            products = productList,
            stallID = stallID,
            totalPrice = totalPrice.toString(),
            type = binding.tvOrderType.text.toString()
        )
    }


    private fun generateShortId(length: Int = 6): String {
        return Random.nextInt(0, 10_000_000).toString().padStart(length, '0').take(length)
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("EEE, MMMM d, yyyy", Locale.ENGLISH)
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Manila")
        return dateFormat.format(Date())
    }

    private fun getCurrentTime(): String {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        timeFormat.timeZone = TimeZone.getTimeZone("Asia/Manila")
        return timeFormat.format(Date())
    }

    private fun getStallID(onStallIdFetched: (String) -> Unit) {
        val childRef = checkedItems[0].productStallName
        databaseReference.child("adminInformation")
            .orderByChild("stallName")
            .equalTo(childRef)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (stallSnapshot in dataSnapshot.children) {
                            val stallID = stallSnapshot.child("user_UID").getValue(String::class.java)?.trim()

                            if (stallID != null) {
                                onStallIdFetched(stallID)
                            } else {
                                binding.stallNumber.text = "No stall number found"
                                onStallIdFetched("")
                            }
                        }
                    } else {
                        binding.stallNumber.text = "No stall number found"
                        onStallIdFetched("")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    binding.stallNumber.text = "Error retrieving stall number"
                    onStallIdFetched("")
                }
            })
    }


    private fun displayTotalPrice() {
        val total = adapter.getTotalPrice()
        binding.tvOverallTotalPrice.text = String.format("₱%.2f", total)
    }


    private fun saveOrderDetails(orderDetails: OrderDetails) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            if (selectedPayMethod == "GCash") {
                databaseReference.child("StallPaymentMethods").orderByChild("stallUID").equalTo(orderDetails.stallID)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val payMethods = snapshot.children.firstOrNull()
                                val payments = payMethods?.getValue(StallPaymentMethods::class.java)
                                if (payments != null) {
                                    saveOrderAndUploadTransaction(orderDetails)
                                } else {
                                    progressDialog.loadingDismiss()
                                    progressDialog.showFailure("This stall has no GCash yet! Select Pay on Pickup instead.")
                                }
                            } else {
                                progressDialog.loadingDismiss()
                                progressDialog.showFailure("This stall has no GCash yet! Select Pay on Pickup instead.")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            progressDialog.loadingDismiss()
                            progressDialog.showFailure("Error fetching payment methods.")
                        }
                    })
            } else {
                saveOrderAndUploadTransaction(orderDetails)
            }
        } else {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveOrderAndUploadTransaction(orderDetails: OrderDetails) {
        databaseReference.child("OrderDetails").child(orderIdentification).setValue(orderDetails)
            .addOnSuccessListener {
                uploadTransaction(orderDetails)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error placing order: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadTransaction(orderDetails: OrderDetails) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            firebaseAuth.currentUser?.let { user ->
                val email = user.email
                if (email != null) {
                    val userQuery =
                        databaseReference.child("users").orderByChild("email").equalTo(email)

                    userQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val userSnapshot = snapshot.children.firstOrNull()
                                val user = userSnapshot?.getValue(Users::class.java)
                                if (user != null) {
                                    customerEmailTransaction = user.email
                                    customerNameTransaction =
                                        "${user.firstName} ${user.lastName}"
                                    customerIDTransaction = user.userID

                                    val customerTransaction = CustomerTransaction(
                                        customerEmail = customerEmailTransaction,
                                        customerName = customerNameTransaction,
                                        customerPaymentScreenShot = "",
                                        customerID = customerIDTransaction,
                                        customerTransactionID = orderDetails.orderId
                                    )

                                    databaseReference.child("customerTransaction")
                                        .child(orderDetails.orderId)
                                        .setValue(customerTransaction)
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                requireContext(),
                                                "Order placed successfully!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            deleteCheckedItemsFromCart()

                                            val bundle = Bundle().apply {
                                                putString("ORDER_ID", orderDetails.orderId)
                                            }

                                            if (selectedPayMethod == "GCash") {
                                                progressDialog.loadingDismiss()
                                                progressDialog.showSuccess("Proceeding to Payment")
                                                findNavController().navigate(R.id.action_checkoutFragment_to_paymentConfirmationFragment, bundle)
                                            } else {
                                                progressDialog.loadingDismiss()
                                                progressDialog.showSuccess("Success! Go to stall and Pay.")
                                                findNavController().navigate(R.id.action_checkoutFragment_to_transactionFragment, bundle)
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                requireContext(),
                                                "Error placing order: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                }
            }
        }
    }

    private fun displayStallNumber() {
        val childRef = checkedItems[0].productStallName

        databaseReference.child("adminInformation")
            .orderByChild("stallName")
            .equalTo(childRef)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (stallSnapshot in dataSnapshot.children) {
                            val stallNumber = stallSnapshot.child("stallNumber").getValue(String::class.java)

                            if (stallNumber != null) {
                                binding.stallNumber.text = "Stall Number: $stallNumber"
                            } else {
                                binding.stallNumber.text = "No stall number found"
                            }
                        }
                    } else {
                        binding.stallNumber.text = "No stall number found"
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    binding.stallNumber.text = "Error retrieving stall number"
                }
            })
    }

    private fun deleteCheckedItemsFromCart() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show()
            return
        }

        val cartRef = databaseReference.child("cartItems").child(userId)

        for (item in checkedItems) {
            cartRef.orderByChild("productName").equalTo(item.productName)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (snapshot in dataSnapshot.children) {
                            snapshot.ref.removeValue().addOnCompleteListener { deleteTask ->
                                if (deleteTask.isSuccessful) {
                                    Log.d("Success","Deleted item: ${item.productName}", )
                                } else {
                                    Toast.makeText(requireContext(), "Failed to delete item: ${item.productName}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(requireContext(), "Failed to retrieve cart items: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

}
