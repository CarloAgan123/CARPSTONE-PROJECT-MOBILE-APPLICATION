package com.example.myfirstapplication.fragments.loginRegister.shopping

import ImageSliderAdapter
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.myfirstapplication.R
import com.example.myfirstapplication.data.CustomerTransaction
import com.example.myfirstapplication.data.OrderDetails
import com.example.myfirstapplication.data.Stall
import com.example.myfirstapplication.data.StallPaymentMethods
import com.example.myfirstapplication.databinding.FragmentGcashScanQrCodeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class PaymentConfirmationFragment : Fragment(R.layout.fragment_gcash_scan_qr_code) {

    private lateinit var binding: FragmentGcashScanQrCodeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var dataRef: DatabaseReference
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ImageSliderAdapter
    private lateinit var buttonNext: Button
    private var orderID: String = ""
    private val imageList = listOf(
        R.drawable.step2,
        R.drawable.step3,
        R.drawable.step4,
        R.drawable.step5,
        R.drawable.step6,
        R.drawable.step7,
    )
    private val PICK_IMAGE_REQUEST = 2
    private var selectedScreenshotUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGcashScanQrCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        dataRef = FirebaseDatabase.getInstance().getReference()

        val orderId = arguments?.getString("ORDER_ID")
        if (orderId != null) {
            fetchImageFromDatabase(orderId)
            orderID = orderId
        }

        viewPager = view.findViewById(R.id.viewPager)
        buttonNext = view.findViewById(R.id.buttonNext)
        adapter = ImageSliderAdapter(imageList)
        viewPager.adapter = adapter

        disableSwipe(viewPager)

        buttonNext.setOnClickListener {
            val nextItem = (viewPager.currentItem + 1) % imageList.size
            viewPager.currentItem = nextItem
        }

        setupFirebaseAndButtons()
    }

    private fun fetchImageFromDatabase(orderId: String) {
        val userUID = auth.currentUser?.uid
        if (userUID != null) {
            val orderRef = dataRef.child("OrderDetails").orderByChild("orderId").equalTo(orderId)
            orderRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val orderDetails = snapshot.children.firstOrNull()
                        val order = orderDetails?.getValue(OrderDetails::class.java)
                        if (order != null) {
                            val stallUID = order.stallID
                            getStallGCashImage(stallUID)
                            getStallNameById(stallUID)
                        } else {
                            Log.e("Payment", "Failed to cast snapshot to OrderDetails")
                        }
                    } else {
                        Toast.makeText(requireContext(), "Order not found.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TransactionFragment", "Error retrieving order: ${error.message}")
                }
            })
        }
    }

    private fun getStallNameById(stallUID: String) {
        val stallRef = dataRef.child("adminInformation").orderByChild("user_UID").equalTo(stallUID)
        stallRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val stallSnapshot = snapshot.children.firstOrNull()
                    val stallInfo = stallSnapshot?.getValue(Stall::class.java)
                    if (stallInfo != null) {
                        binding.tvStallName.text = stallInfo.stallName
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Stall", "Error fetching stall name: ${error.message}")
            }
        })
    }

    private fun getStallGCashImage(stallUID: String) {
        val stallRef = dataRef.child("StallPaymentMethods").orderByChild("stallUID").equalTo(stallUID)
        stallRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val stallSnapshot = snapshot.children.firstOrNull()
                    val stallInfo = stallSnapshot?.getValue(StallPaymentMethods::class.java)
                    if (stallInfo != null && stallInfo.stallGCashQRCode.isNotEmpty()) {
                        Glide.with(requireContext())
                            .load(stallInfo.stallGCashQRCode)
                            .apply(RequestOptions().placeholder(R.drawable.default_stall).error(R.drawable.new_logo))
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(binding.ivGCashImageFromStall)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GCashImage", "Error fetching GCash image: ${error.message}")
            }
        })
    }

    private fun setupFirebaseAndButtons() {
        binding.btnSaveToGallery.setOnClickListener {
            val drawable = binding.ivGCashImageFromStall.drawable
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                saveImage(bitmap)
            } else {
                Toast.makeText(requireContext(), "Image not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveImage(bitmap: Bitmap) {
        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "GCash_Stall_QRCode_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
        }

        val uri = requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (uri != null) {
            requireContext().contentResolver.openOutputStream(uri).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    Toast.makeText(requireContext(), "Image saved to Gallery", Toast.LENGTH_SHORT).show()

                    binding.ivGCashImageFromStall.visibility = View.GONE
                    binding.tvStallName.visibility = View.GONE
                    binding.tvDescription.visibility = View.GONE
                    binding.btnSaveToGallery.visibility = View.GONE
                    binding.buttonNext.isEnabled = true
                    binding.viewPager.visibility = View.VISIBLE

                    binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                        override fun onPageSelected(position: Int) {
                            super.onPageSelected(position)

                            if (position == imageList.size - 1) {
                                binding.btnOpenGCashApp.visibility = View.VISIBLE
                                binding.buttonNext.visibility = View.GONE
                                binding.btnOpenGCashApp.setOnClickListener {
                                    uploadScreenshotToFirebase()
                                    try {
                                        val intent = requireContext().packageManager.getLaunchIntentForPackage("com.globe.gcash.android")
                                        if (intent != null) {
                                            startActivity(intent)
                                        } else {
                                            val playStoreIntent = Intent(Intent.ACTION_VIEW)
                                            playStoreIntent.data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.globe.gcash.android")
                                            startActivity(playStoreIntent)
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(requireContext(), "Unable to open GCash app", Toast.LENGTH_SHORT).show()
                                    }
                                }

                            } else {
                                binding.btnOpenGCashApp.visibility = View.GONE
                                binding.buttonNext.visibility = View.VISIBLE
                            }
                        }
                    })

                } else {
                    Toast.makeText(requireContext(), "Error saving image", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Error creating image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadScreenshotToFirebase() {
        binding.btnOpenGCashApp.visibility = View.GONE
        binding.viewPager.visibility = View.GONE
        binding.btnUploadImageFromGallery.visibility = View.VISIBLE
        binding.ivScreenshot.visibility = View.VISIBLE
        binding.btnOkay.visibility = View.VISIBLE
        binding.btnOkay.isEnabled = false

        binding.btnUploadImageFromGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedScreenshotUri = data.data
            binding.ivScreenshot.setImageURI(selectedScreenshotUri)
            binding.btnOkay.isEnabled = true
            uploadDataToFirebase()
        }
    }

    private fun uploadDataToFirebase() {
        if (selectedScreenshotUri != null) {
            val storageRef = Firebase.storage.reference.child("paymentScreenshots/${System.currentTimeMillis()}.jpg")
            val uploadTask = storageRef.putFile(selectedScreenshotUri!!)

            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val screenshotUrl = uri.toString()

                    val orderRef = dataRef.child("OrderDetails").orderByChild("orderId").equalTo(orderID)
                    orderRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val orderDetails = snapshot.children.firstOrNull()
                                orderDetails?.ref?.child("paid")?.setValue("Paid")
                                Toast.makeText(requireContext(), "Order has been set to Paid.", Toast.LENGTH_SHORT).show()

                                val paymentMethodRef = dataRef.child("customerTransaction").orderByChild("customerTransactionID").equalTo(orderID)
                                paymentMethodRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            val customerTransaction = snapshot.children.firstOrNull()
                                            customerTransaction?.ref?.child("customerPaymentScreenShot")?.setValue(screenshotUrl)
                                            Toast.makeText(requireContext(), "Screenshot uploaded successfully.", Toast.LENGTH_SHORT).show()
                                            val bundle = Bundle().apply {
                                                putString("ORDER_ID", orderID)
                                            }
                                            findNavController().navigate(R.id.action_paymentConfirmationFragment_to_transactionFragment, bundle)
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Toast.makeText(requireContext(), "Error occurred: ${error.message}", Toast.LENGTH_SHORT).show()
                                    }
                                })
                            } else {
                                Toast.makeText(requireContext(), "Order not found.", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(requireContext(), "Error occurred: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to upload screenshot: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "No screenshot selected.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun disableSwipe(viewPager: ViewPager2) {
        viewPager.getChildAt(0).setOnTouchListener { _, _ -> true }
    }
}
