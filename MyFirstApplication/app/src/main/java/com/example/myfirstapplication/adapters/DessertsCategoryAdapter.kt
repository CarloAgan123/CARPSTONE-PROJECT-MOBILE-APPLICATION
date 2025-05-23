package com.example.myfirstapplication.adapters

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.myfirstapplication.R
import com.example.myfirstapplication.adapters.DrinksCategoryAdapter.DrinksCategoryViewHolder
import com.example.myfirstapplication.data.Product
import com.example.myfirstapplication.databinding.SpecialRvItemBinding
import decodeBase64ToBitmap

class DessertsCategoryAdapter: RecyclerView.Adapter<DessertsCategoryAdapter.DessertsCategoryViewHolder>() {

    inner class DessertsCategoryViewHolder(private val binding: SpecialRvItemBinding): RecyclerView.ViewHolder(binding.root)
    {
        fun bind(product: Product) {
            binding.apply {
                Glide.with(binding.root.context)
                    .load(product.productImage)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.default_icon)
                            .error(R.drawable.new_logo))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageSpecialRvItem)

                tvSpecialProductName.text = product.productName
                tvSpecialProductPrice.text = "₱ ${product.productPrice}"

                root.setOnClickListener {
                    onClick?.invoke(product)
                }
            }
        }
    }

    private val diffCallback = object : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.productId == newItem.productId
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DessertsCategoryViewHolder {
        val binding = SpecialRvItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DessertsCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DessertsCategoryViewHolder, position: Int) {
        val product = differ.currentList[position]
        holder.bind(product)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    fun submitList(list: List<Product>) {
        differ.submitList(list)
    }

    var onClick: ((Product) -> Unit)? = null

}