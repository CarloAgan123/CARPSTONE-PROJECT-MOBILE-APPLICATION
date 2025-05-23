package com.example.myfirstapplication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myfirstapplication.R
import com.example.myfirstapplication.data.Product

class ProductSearchAdapter(
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductSearchAdapter.ProductViewHolder>() {

    private var productList = listOf<Product>()

    fun updateList(newList: List<Product>) {
        productList = newList
        notifyDataSetChanged()
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productName: TextView = itemView.findViewById(R.id.tv_name)
        private val productPrice: TextView = itemView.findViewById(R.id.tv_price)
        private val productImage: ImageView = itemView.findViewById(R.id.img_product)

        fun bind(product: Product) {
            productName.text = product.productName
            productPrice.text = "₱${product.productPrice}"
            Glide.with(itemView.context)
                .load(product.productImage)
                .placeholder(R.drawable.placeholder_image)
                .into(productImage)
            itemView.setOnClickListener { onItemClick(product) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.best_product_rv_item, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(productList[position])
    }

    override fun getItemCount(): Int = productList.size
}
