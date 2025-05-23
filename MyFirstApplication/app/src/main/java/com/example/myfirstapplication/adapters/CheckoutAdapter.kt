package com.example.myfirstapplication.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.myfirstapplication.R
import com.example.myfirstapplication.data.CartItem
import com.example.myfirstapplication.databinding.CheckoutProductItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CheckoutAdapter(
    private val context: Context,
    private val updateTotalPriceCallback: () -> Unit
) : ListAdapter<CartItem, RecyclerView.ViewHolder>(ItemDiffCallback()) {

    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference

    companion object {
        private const val VIEW_TYPE_PRODUCT = 1
    }

    fun submitCheckedItems(checkedItems: List<CartItem>) {
        submitList(checkedItems)
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_PRODUCT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.cart_product_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        val productHolder = holder as ProductViewHolder
        productHolder.bind(item)
    }

    override fun getItemCount(): Int = currentList.size

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageCartProduct: ImageView = itemView.findViewById(R.id.imageCartProduct)
        private val tvProductCartName: TextView = itemView.findViewById(R.id.tvProductCartName)
        private val tvProductCartPrice: TextView = itemView.findViewById(R.id.tvProductCartPrice)
        private val tvCheckoutProductQuantity: TextView = itemView.findViewById(R.id.tvCheckoutProductQuantity)
        private val imageMinus: ImageView = itemView.findViewById(R.id.imageMinus)
        private val imagePlus: ImageView = itemView.findViewById(R.id.imagePlus)

        fun bind(cartItem: CartItem) {
            tvProductCartName.text = cartItem.productName
            tvCheckoutProductQuantity.text = cartItem.productQuantity.toString()

            fetchProductPriceAndDisplay(cartItem)

            imagePlus.setOnClickListener {
                updateQuantityAndPrice(cartItem, isIncrease = true)
            }

            imageMinus.setOnClickListener {
                updateQuantityAndPrice(cartItem, isIncrease = false)
            }
        }

        private fun fetchProductPriceAndDisplay(cartItem: CartItem) {
            databaseReference.child("Products").child(cartItem.productId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val unitPrice = dataSnapshot.child("productPrice").getValue(String::class.java)?.toIntOrNull() ?: 0
                        val totalPrice = unitPrice * cartItem.productQuantity
                        tvProductCartPrice.text = "₱$totalPrice"
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("CheckoutAdapter", "Error fetching product price", databaseError.toException())
                    }
                })
        }

        private fun updateQuantityAndPrice(cartItem: CartItem, isIncrease: Boolean) {
            // Change the quantity based on the button clicked, but don't update the CartItem or database
            if (isIncrease) {
                cartItem.productQuantity += 1
            } else {
                if (cartItem.productQuantity > 1) {
                    cartItem.productQuantity -= 1
                }
            }

            // Update only the UI
            tvCheckoutProductQuantity.text = cartItem.productQuantity.toString()

            // Fetch product price to calculate the updated price
            databaseReference.child("Products").child(cartItem.productId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            val unitPrice = dataSnapshot.child("productPrice").getValue(String::class.java)?.toIntOrNull() ?: 0
                            val updatedTotalPrice = unitPrice * cartItem.productQuantity
                            tvProductCartPrice.text = "₱$updatedTotalPrice"
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("CheckoutAdapter", "Error fetching product price", databaseError.toException())
                    }
                })

            // Notify the adapter to refresh the UI (if necessary)
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                notifyItemChanged(position)
            }

            // Update total price callback if necessary (optional)
            updateTotalPriceCallback()
        }

    }

    class ItemDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.cartItemId == newItem.cartItemId
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }
}


