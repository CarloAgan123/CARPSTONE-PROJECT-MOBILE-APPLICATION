package com.example.myfirstapplication.fragments.loginRegister.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.myfirstapplication.R
import com.example.myfirstapplication.data.CartItem
import com.example.myfirstapplication.data.Product
import com.example.myfirstapplication.databinding.FragmentProductDetailsBinding
import com.example.myfirstapplication.uitel.ProgressDialog
import com.example.myfirstapplication.util.hideBottomNavigationView
import com.example.myfirstapplication.viewmodel.CartViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

class ProductDetailsFragment : Fragment() {

    private lateinit var binding: FragmentProductDetailsBinding
    private var product: Product? = null
    private val cartViewModel: CartViewModel by activityViewModels()
    private lateinit var databaseReference: DatabaseReference
    private lateinit var progressDialog: ProgressDialog
    private var totalOrderPrice: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        hideBottomNavigationView()
        binding = FragmentProductDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressDialog = ProgressDialog(requireActivity())

        databaseReference = FirebaseDatabase.getInstance().reference.child("cartItems")

        product = arguments?.getParcelable("product")

        product?.let { product ->
            binding.apply {
                tvProductName.text = product.productName
                tvProductPrice.text = "₱${product.productPrice}"
                tvStallName.text = "By ${product.productStallName}"
                productDescription.text = product.productDescription

                Glide.with(this@ProductDetailsFragment)
                    .load(product.productImage)
                    .apply(RequestOptions()
                        .placeholder(R.drawable.sample_image)
                        .error(R.drawable.new_logo))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivProductImage)
            }


            binding.btnAddToCart.setOnClickListener {
                progressDialog.loadingStart()
                val cartItem = CartItem(
                    productId = product.productId,
                    productName = product.productName,
                    productStallName = product.productStallName,
                    productPrice = product.productPrice.toInt(),
                    subTotalPrice = product.productPrice.toInt(),
                    productImage = product.productImage,
                    productQuantity = 1,
                    cartItemType = "Order",
                    isChecked = false
                )
                checkIfProductInCart(cartItem)
            }
        }

        binding.imageClose.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnCheckout.setOnClickListener {
            product?.let { product ->
                val cartItem = CartItem(
                    productId = product.productId,
                    productName = product.productName,
                    productStallName = product.productStallName,
                    productPrice = product.productPrice.toInt(),
                    subTotalPrice = product.productPrice.toInt(),
                    productImage = product.productImage,
                    productQuantity = 1,
                    cartItemType = "Order",
                    isChecked = true
                )

                totalOrderPrice = product.productPrice.toDouble()

                val checkedItemsArrayList = arrayListOf(cartItem)

                val bundle = Bundle().apply {
                    putParcelableArrayList("checkedItems", checkedItemsArrayList)
                    putDouble("totalPrice", totalOrderPrice)
                }

                findNavController().navigate(R.id.action_productDetailsFragment_to_checkoutFragment, bundle)
            }
        }
    }

    private fun checkIfProductInCart(cartItem: CartItem) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val cartRef = databaseReference.child(userId)
        cartRef.orderByChild("productId").equalTo(cartItem.productId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        progressDialog.showFailure("${cartItem.productName} is already in the cart!")
                        progressDialog.loadingDismiss()
                        Toast.makeText(context, "${cartItem.productName} is already in the cart! Visit your cart to add more!", Toast.LENGTH_LONG).show()
                    } else {
                        progressDialog.loadingDismiss()
                        progressDialog.showSuccess("${cartItem.productName} added to cart!")
                        val newCartItemRef = cartRef.push()
                        cartItem.cartItemId = newCartItemRef.key
                        newCartItemRef.setValue(cartItem).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "${cartItem.productName} added to cart!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to add to cart: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(context, "Failed to check cart: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

}
