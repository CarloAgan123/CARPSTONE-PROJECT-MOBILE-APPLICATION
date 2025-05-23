package com.example.myfirstapplication.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.myfirstapplication.R
import com.example.myfirstapplication.data.CartItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CartAdapter(
    private val context: Context,
    private val updateTotalPriceCallback: () -> Unit)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Any>()
    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference

    companion object {
        private const val VIEW_TYPE_STALL = 0
        private const val VIEW_TYPE_PRODUCT = 1
    }

    private val productViewHolders = mutableMapOf<String, MutableList<ProductViewHolder>>()
    private val stallViewHolders = mutableMapOf<String, StallViewHolder>()

    fun setItems(newItems: List<Any>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()

        productViewHolders.clear()
        stallViewHolders.clear()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is String -> VIEW_TYPE_STALL
            is CartItem -> VIEW_TYPE_PRODUCT
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_STALL -> {
                val view = LayoutInflater.from(context).inflate(R.layout.item_stall_name, parent, false)
                StallViewHolder(view)
            }
            VIEW_TYPE_PRODUCT -> {
                val view = LayoutInflater.from(context).inflate(R.layout.cart_product_item, parent, false)
                ProductViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is String -> {
                val stallHolder = holder as StallViewHolder
                stallHolder.bind(item)
                stallViewHolders[item] = stallHolder
            }
            is CartItem -> {
                val productHolder = holder as ProductViewHolder
                productHolder.bind(item)
                val stallName = item.productStallName
                if (stallName != null) {
                    productViewHolders.getOrPut(stallName) { mutableListOf() }.add(productHolder)
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    inner class StallViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvCartProductStallName: TextView = itemView.findViewById(R.id.tvCartProductStallName)
        private val cbCartStallName: CheckBox = itemView.findViewById(R.id.cbCartStallName)

        init {
            cbCartStallName.setOnCheckedChangeListener { _, isChecked ->
                val stallName = (items[adapterPosition] as? String) ?: return@setOnCheckedChangeListener
                updateProductCheckBoxes(stallName, isChecked)
            }
        }

        fun bind(stallName: String) {
            tvCartProductStallName.text = stallName
            updateCheckboxState()
        }

        private fun updateProductCheckBoxes(stallName: String, isChecked: Boolean) {
            productViewHolders[stallName]?.forEach { productViewHolder ->
                productViewHolder.setProductChecked(isChecked)
            }
        }

        fun updateCheckboxState() {
            val stallName = (items[adapterPosition] as? String) ?: return
            val allChecked = productViewHolders[stallName]?.all { it.isProductChecked() } ?: false
            cbCartStallName.isChecked = allChecked
        }
    }

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageCartProduct: ImageView = itemView.findViewById(R.id.imageCartProduct)
        private val tvProductCartName: TextView = itemView.findViewById(R.id.tvProductCartName)
        private val tvProductCartPrice: TextView = itemView.findViewById(R.id.tvProductCartPrice)
        private val tvProductQuantity: TextView = itemView.findViewById(R.id.tvProductQuantity)
        private val tvCartProductQuantity: TextView = itemView.findViewById(R.id.tvCartProductQuantity)
        private val imageMinus: ImageView = itemView.findViewById(R.id.imageMinus)
        private val imagePlus: ImageView = itemView.findViewById(R.id.imagePlus)
        private val cbStallProductItem: CheckBox = itemView.findViewById(R.id.cbStallProductItem)

        private fun updateQuantityAndPrice(cartItem: CartItem) {
            val totalPrice = cartItem.subTotalPrice  // Using subTotalPrice instead of calculating price * quantity

            tvCartProductQuantity.text = cartItem.productQuantity.toString()
            tvProductCartPrice.text = "₱$totalPrice"

            items[adapterPosition] = cartItem

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val database = FirebaseDatabase.getInstance()
                val cartItemRef = database.reference.child("cartItems").child(userId).child(cartItem.cartItemId ?: "")

                cartItemRef.child("productQuantity").setValue(cartItem.productQuantity)
                cartItemRef.child("subTotalPrice").setValue(totalPrice)
            }
        }

        fun bind(cartItem: CartItem) {
            tvProductCartName.text = cartItem.productName
            tvProductCartPrice.text = "₱${cartItem.subTotalPrice}"
            tvCartProductQuantity.text = cartItem.productQuantity.toString()

            Glide.with(itemView.context)
                .load(cartItem.productImage)
                .apply(RequestOptions().placeholder(R.drawable.default_icon).error(R.drawable.new_logo))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageCartProduct)

            if (cartItem.productQuantity == 0) cartItem.productQuantity = 1
            tvCartProductQuantity.text = cartItem.productQuantity.toString()

            databaseReference.child("Products").child(cartItem.productId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val productQuantity = dataSnapshot.child("productQuantity").getValue(String::class.java)?.toIntOrNull() ?: 0
                        tvProductQuantity.text = productQuantity.toString()
                        val currentQuantity = cartItem.productQuantity
                        imagePlus.isEnabled = currentQuantity < productQuantity
                        imageMinus.isEnabled = currentQuantity > 1
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("CartAdapter", "Error fetching product quantity", databaseError.toException())
                    }
                })

            imagePlus.setOnClickListener {
                databaseReference.child("Products").child(cartItem.productId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                val unitPrice = dataSnapshot.child("productPrice").getValue(String::class.java)?.toIntOrNull() ?: 0
                                val maxQuantity = tvProductQuantity.text.toString().toIntOrNull() ?: 0

                                if (cartItem.productQuantity < maxQuantity) {
                                    cartItem.productQuantity++
                                    cartItem.subTotalPrice = unitPrice * cartItem.productQuantity
                                    cartItem.productPrice = unitPrice

                                    updateQuantityAndPrice(cartItem)
                                    updateTotalPriceCallback()
                                }

                                imagePlus.isEnabled = cartItem.productQuantity < maxQuantity
                                imageMinus.isEnabled = cartItem.productQuantity > 1
                            } else {
                                Log.e("CartAdapter", "Product not found: ${cartItem.productId}")
                                Toast.makeText(context, "Product not found", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("CartAdapter", "Error fetching product price", databaseError.toException())
                        }
                    })
            }

            imageMinus.setOnClickListener {
                databaseReference.child("Products").child(cartItem.productId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                val unitPrice = dataSnapshot.child("productPrice").getValue(String::class.java)?.toIntOrNull() ?: 0
                                val maxQuantity = tvProductQuantity.text.toString().toIntOrNull() ?: 0

                                if (cartItem.productQuantity > 1) {
                                    cartItem.productQuantity--
                                    cartItem.subTotalPrice = unitPrice * cartItem.productQuantity
                                    cartItem.productPrice = unitPrice

                                    updateQuantityAndPrice(cartItem)
                                    updateTotalPriceCallback()
                                }

                                imagePlus.isEnabled = cartItem.productQuantity < maxQuantity
                                imageMinus.isEnabled = cartItem.productQuantity > 1
                            } else {
                                Log.e("CartAdapter", "Product not found: ${cartItem.productId}")
                                Toast.makeText(context, "Product not found", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("CartAdapter", "Error fetching product price", databaseError.toException())
                        }
                    })
            }

            cbStallProductItem.isChecked = cartItem.isChecked
            cbStallProductItem.setOnCheckedChangeListener { _, isChecked ->
                cartItem.isChecked = isChecked

                updateProductInDatabase(cartItem)
                updateTotalPriceCallback()
            }
        }

        private fun updateProductInDatabase(cartItem: CartItem) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
                return
            }

            val database = FirebaseDatabase.getInstance()
            val cartItemRef = database.reference.child("cartItems").child(userId).child(cartItem.cartItemId ?: "")

            cartItemRef.child("checked").setValue(cartItem.isChecked)
                .addOnSuccessListener {
                    Log.d("CartAdapter", "Successfully updated cart item: ${cartItem.cartItemId}")
                }
                .addOnFailureListener { exception ->
                    Log.e("CartAdapter", "Failed to update cart item: ${cartItem.cartItemId}", exception)
                }
        }

        fun setProductChecked(isChecked: Boolean) {
            cbStallProductItem.isChecked = isChecked
        }

        fun isProductChecked(): Boolean {
            return cbStallProductItem.isChecked
        }
    }

    fun getCheckedItems(): List<CartItem> {
        val checkedItems = mutableListOf<CartItem>()
        items.forEach { item ->
            if (item is CartItem && item.isChecked) {
                checkedItems.add(item)
            }
        }
        return checkedItems
    }
}
