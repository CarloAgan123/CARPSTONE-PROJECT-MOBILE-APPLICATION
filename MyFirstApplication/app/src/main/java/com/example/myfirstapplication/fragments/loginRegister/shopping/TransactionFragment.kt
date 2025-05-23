package com.example.myfirstapplication.fragments.loginRegister.shopping

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myfirstapplication.R
import com.example.myfirstapplication.adapters.ProductAdapter
import com.example.myfirstapplication.data.CustomerTransaction
import com.example.myfirstapplication.data.OrderDetails
import com.example.myfirstapplication.data.ProductList
import com.example.myfirstapplication.data.Stall
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.myfirstapplication.databinding.FragmentTransactionBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TransactionFragment : Fragment(R.layout.fragment_transaction) {

    private lateinit var auth: FirebaseAuth
    private lateinit var dataRef: DatabaseReference
    private lateinit var binding: FragmentTransactionBinding
    private lateinit var productAdapter: ProductAdapter
    private var currentUserID: String = ""

    private val REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        dataRef = FirebaseDatabase.getInstance().getReference()

        val orderId = arguments?.getString("ORDER_ID")
        if (orderId != null) {
            getTransactionData(orderId)
            initializeButtons(orderId)
        }

        setupOrdersList()

        binding.backArrow.setOnClickListener {
            findNavController().navigate(R.id.action_transactionFragment_to_homeFragment)
        }

        binding.btnSaveQRCodeToGallery.setOnClickListener {
            saveQRCodeToGallery()
        }

    }

    private fun initializeButtons(orderId: String) {
        binding.cancelOrder.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Confirm Cancellation")
                .setMessage("Are you sure you want to cancel this order?")
                .setPositiveButton("Yes") { dialog, which ->
                    val orderRef = dataRef.child("OrderDetails").orderByChild("orderId").equalTo(orderId)
                    orderRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val orderDetails = snapshot.children.firstOrNull()
                                val order = orderDetails?.getValue(OrderDetails::class.java)
                                if (order != null) {
                                    orderRef.getRef().child(orderId).child("orderStatus").setValue("Cancelled")
                                    Toast.makeText(requireContext(), "Order has been cancelled.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(requireContext(), "Order not found.", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(requireContext(), "Error occurred: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
                .setNegativeButton("No") { dialog, which -> dialog.dismiss() }
                .show()
        }
    }


    private fun setupOrdersList() {
        productAdapter = ProductAdapter()
        binding.rvOrderItemList.apply {
            layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
            adapter = productAdapter
        }
    }

    private fun getTransactionData(orderId: String) {
        val userUID = auth.currentUser?.uid
        if (userUID != null) {
            val orderRef = dataRef.child("OrderDetails").orderByChild("orderId").equalTo(orderId)
            orderRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val prodList = mutableListOf<ProductList>()
                    if (snapshot.exists()) {
                        val orderDetails = snapshot.children.firstOrNull()
                        val order = orderDetails?.getValue(OrderDetails::class.java)
                        if (order != null) {
                            binding.tvOrderIdLabel.text = order.orderId
                            binding.tvOrderType.text = order.type
                            binding.tvOrderDateLabel.text = order.orderDate
                            binding.tvOrderTimeLabel.text = order.orderTime
                            binding.tvPaymentMethodLabel.text = order.paymentMethod
                            binding.tvPaymentStatusLabel.text = order.paid
                            binding.tvOrderItemCountLabel.text = order.products.count().toString()
                            binding.totalAmount.text = "₱${order.totalPrice.toDouble()}"
                            binding.orderStatusDescription.text = order.orderStatus
                            prodList.addAll(order.products)
                            getStallDetails(order.stallID, order.orderId)


                            if (order.orderStatus == "Pending") {
                                binding.orderStatusPercent.text = "20%"
                            } else if (order.orderStatus == "Processing") {
                                binding.orderStatusPercent.text = "40%"
                            }else if (order.orderStatus == "Ready") {
                                binding.orderStatusPercent.text = "60%"
                                binding.cancelOrder.isEnabled = false
                            }else if (order.orderStatus == "Finished") {
                                binding.orderStatusPercent.text = "80%"
                                binding.cancelOrder.isEnabled = false
                            }else if (order.orderStatus == "Received") {
                                binding.orderStatusPercent.text = "100%"
                                binding.cancelOrder.alpha = 0.5f
                                binding.cancelOrder.isEnabled = false
                            }else if (order.orderStatus == "Cancelled") {
                                binding.orderStatusPercent.text = "0%"
                                binding.cancelOrder.alpha = 0.5f
                                binding.cancelOrder.isEnabled = false
                            }else if (order.orderStatus == "Declined") {
                                binding.orderStatusPercent.text = "0%"
                                binding.cancelOrder.alpha = 0.5f
                                binding.cancelOrder.isEnabled = false
                            } else {
                                binding.orderStatusPercent.text = "0%"
                            }
                            binding.orderStatusProgressBar.progress = getOrderStatusProgress(order.orderStatus)
                        } else {
                            Log.e("TransactionFragment", "Failed to cast snapshot to OrderDetails")
                        }
                    } else {
                        Toast.makeText(requireContext(), "Order not found.", Toast.LENGTH_SHORT).show()
                    }
                    productAdapter.submitList(prodList)
                }

                private fun getOrderStatusProgress(orderStatus: String): Int {
                    return when (orderStatus) {
                        "Pending" -> 20
                        "Processing" -> 40
                        "Ready" -> 60
                        "Finished" -> 80
                        "Received" -> 100
                        else -> 0
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TransactionFragment", "Error retrieving order: ${error.message}")
                    Toast.makeText(requireContext(), "Error retrieving order: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(requireContext(), "User is not authenticated.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getStallDetails(stallID: String, orderId: String) {
        val orderRef = dataRef.child("adminInformation").orderByChild("user_UID").equalTo(stallID)
        orderRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val orderDetails = snapshot.children.firstOrNull()
                    val order = orderDetails?.getValue(Stall::class.java)
                    if (order != null) {
                        binding.tvStallNameLabel.text = order.stallName
                        binding.tvStallNumberLabel.text = order.stallNumber
                        getUserID(orderId)
                    } else {
                        Log.e("TransactionFragment", "Failed to cast snapshot to OrderDetails")
                    }
                } else {
                    Toast.makeText(requireContext(), "Order not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("TransactionFragment", "Error retrieving order: ${error.message}")
                Toast.makeText(requireContext(), "Error retrieving order: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getUserID(orderId: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val orderRef = dataRef.child("customerTransaction").orderByChild("customerTransactionID").equalTo(orderId)
            orderRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val orderDetails = snapshot.children.firstOrNull()
                        val order = orderDetails?.getValue(CustomerTransaction::class.java)
                        if (order != null) {
                            currentUserID = order.customerID
                            generateQRCode(currentUserID, orderId)
                        } else {
                            Log.e("TransactionFragment", "Failed to cast snapshot to OrderDetails")
                        }
                    } else {
                        Toast.makeText(requireContext(), "Order not found.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TransactionFragment", "Error retrieving order: ${error.message}")
                    Toast.makeText(requireContext(), "Error retrieving order: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun generateQRCode(currentUserID: String, orderId: String) {
        val qrData = "OrderID: $orderId\nUserID: $currentUserID"
        val barcodeEncoder = BarcodeEncoder()
        try {
            val bitmap: Bitmap = barcodeEncoder.encodeBitmap(qrData, BarcodeFormat.QR_CODE, 400, 400)
            binding.ivQRCode.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            Log.e("TransactionFragment", "Error generating QR code: ${e.message}")
            Toast.makeText(requireContext(), "Error generating QR code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveQRCodeToGallery() {
        val bitmap = (binding.ivQRCode.drawable as BitmapDrawable).bitmap
        saveImage(bitmap)
    }


    private fun saveImage(bitmap: Bitmap) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "QRCode_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri = requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            requireContext().contentResolver.openOutputStream(uri).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    Toast.makeText(requireContext(), "Image saved to Gallery", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error saving image", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Error creating image", Toast.LENGTH_SHORT).show()
        }
    }
}
