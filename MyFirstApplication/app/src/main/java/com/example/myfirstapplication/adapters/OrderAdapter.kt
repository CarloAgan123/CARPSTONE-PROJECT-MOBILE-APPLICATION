package com.example.myfirstapplication.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myfirstapplication.R
import com.example.myfirstapplication.data.CartItem
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class OrderAdapter(private val orders: MutableList<CartItem>, private val onTotalPriceUpdated: (Double) -> Unit) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productName: TextView = view.findViewById(R.id.tvProductCheckoutName)
        val productQty: TextView = view.findViewById(R.id.tvCheckoutProductQuantity)
        val productImg: ImageView = view.findViewById(R.id.imageCheckoutProduct)
        val price: TextView = view.findViewById(R.id.tvProductCheckoutPrice)
        val imagePlus: ImageView = view.findViewById(R.id.imagePlus)
        val imageMinus: ImageView = view.findViewById(R.id.imageMinus)
        val stocks: TextView = view.findViewById(R.id.tvCheckoutProductStocks)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.checkout_product_item, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.productName.text = order.productName
        holder.productQty.text = order.productQuantity.toString()

        Glide.with(holder.itemView.context)
            .load(order.productImage)
            .into(holder.productImg)

        holder.price.text = "₱${order.productPrice * order.productQuantity}"

        if (order.productQuantity == 0) order.productQuantity = 1
        holder.stocks.text = "stocks: ${order.productQuantity.toString()}"

        val databaseReference = FirebaseDatabase.getInstance().reference
        databaseReference.child("Products").child(order.productId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val productQuantity = dataSnapshot.child("productQuantity").getValue(String::class.java)?.toIntOrNull() ?: 0
                        holder.stocks.text = "stocks:${productQuantity.toString()}"
                        val currentQuantity = order.productQuantity
                        holder.imagePlus.isEnabled = currentQuantity < productQuantity
                        holder.imageMinus.isEnabled = currentQuantity > 1
                    } else {
                        databaseReference.child("MenuItems").child(order.productId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(menuSnapshot: DataSnapshot) {
                                    if (menuSnapshot.exists()) {
                                        val menuQuantity = menuSnapshot.child("menuQuantity").getValue(String::class.java)?.toIntOrNull() ?: 0
                                        holder.stocks.text = "stocks:${menuQuantity.toString()}"
                                        val currentQuantity = order.productQuantity
                                        holder.imagePlus.isEnabled = currentQuantity < menuQuantity
                                        holder.imageMinus.isEnabled = currentQuantity > 1
                                    } else {
                                        Log.e("CartAdapter", "Item not found in Products or MenuItems")
                                    }
                                }

                                override fun onCancelled(menuError: DatabaseError) {
                                    Log.e("CartAdapter", "Error checking MenuItems", menuError.toException())
                                }
                            })
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("CartAdapter", "Error fetching product quantity", databaseError.toException())
                }
            })


        holder.imagePlus.setOnClickListener {
            updateItemQuantity(holder, order, 1)
        }

        holder.imageMinus.setOnClickListener {
            updateItemQuantity(holder, order, -1)
        }
    }

    private fun updateItemQuantity(holder: OrderViewHolder, order: CartItem, change: Int) {
        val databaseReference = FirebaseDatabase.getInstance().reference
        databaseReference.child("Products").child(order.productId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val unitPrice = dataSnapshot.child("productPrice").getValue(String::class.java)?.toIntOrNull() ?: 0
                        val maxQuantity = dataSnapshot.child("productQuantity").getValue(String::class.java)?.toIntOrNull() ?: 0

                        val newQuantity = order.productQuantity + change
                        if (newQuantity in 1..maxQuantity) {
                            order.productQuantity = newQuantity
                            order.productPrice = unitPrice
                            order.subTotalPrice = unitPrice * newQuantity

                            holder.productQty.text = newQuantity.toString()
                            holder.price.text = "₱${order.subTotalPrice}"

                            holder.imagePlus.isEnabled = newQuantity < maxQuantity
                            holder.imageMinus.isEnabled = newQuantity > 1

                            onTotalPriceUpdated(getTotalPrice())
                        }
                    } else {
                        databaseReference.child("MenuItems").child(order.productId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(menuSnapshot: DataSnapshot) {
                                    if (menuSnapshot.exists()) {
                                        val unitPrice = menuSnapshot.child("menuPrice").getValue(String::class.java)?.toIntOrNull() ?: 0
                                        val maxQuantity = menuSnapshot.child("menuQuantity").getValue(String::class.java)?.toIntOrNull() ?: 0

                                        val newQuantity = order.productQuantity + change
                                        if (newQuantity in 1..maxQuantity) {
                                            order.productQuantity = newQuantity
                                            order.productPrice = unitPrice
                                            order.subTotalPrice = unitPrice * newQuantity

                                            holder.productQty.text = newQuantity.toString()
                                            holder.price.text = "₱${order.subTotalPrice}"

                                            holder.imagePlus.isEnabled = newQuantity < maxQuantity
                                            holder.imageMinus.isEnabled = newQuantity > 1

                                            onTotalPriceUpdated(getTotalPrice())
                                        }
                                    } else {
                                        Log.e("CartAdapter", "Item not found in Products or MenuItems")
                                    }
                                }

                                override fun onCancelled(menuError: DatabaseError) {
                                    Log.e("CartAdapter", "Error checking MenuItems", menuError.toException())
                                }
                            })
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("CartAdapter", "Error updating quantity", databaseError.toException())
                }
            })
    }


    override fun getItemCount(): Int = orders.size

    fun getTotalPrice(): Double {
        var total = 0.0
        for (order in orders) {
            total += order.subTotalPrice
        }
        return total
    }
}
