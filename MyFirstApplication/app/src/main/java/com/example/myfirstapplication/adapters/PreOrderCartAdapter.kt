package com.example.myfirstapplication.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.myfirstapplication.R
import com.example.myfirstapplication.data.MenuItems
import com.example.myfirstapplication.data.PreOrderItems
import com.example.myfirstapplication.databinding.PreOrderMenuItemBinding

class PreOrderCartAdapter : RecyclerView.Adapter<PreOrderCartAdapter.PreOrderCartViewHolder>() {

    inner class PreOrderCartViewHolder(private val binding: PreOrderMenuItemBinding) :RecyclerView.ViewHolder(binding.root) {
        fun bind(menuItems: MenuItems) {
            Glide.with(binding.root.context)
                .load(menuItems.menuImageUrl)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.default_icon)
                        .error(R.drawable.new_logo)
                )
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.imagePreOrderItem)
            binding.tvPreOrderProductName.text = menuItems.menuName
            binding.tvPreOrderProductPrice.text = menuItems.menuPrice.toString()
            binding.tvPreOrderProductQuantity.text = menuItems.menuQuantity
        }

    }

    private val diffCallback = object : DiffUtil.ItemCallback<MenuItems>() {
        override fun areItemsTheSame(oldItem: MenuItems, newItem: MenuItems): Boolean {
            return oldItem.menuItemID == newItem.menuItemID
        }

        override fun areContentsTheSame(oldItem: MenuItems, newItem: MenuItems): Boolean {
            return oldItem == newItem
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreOrderCartViewHolder {
        val binding = PreOrderMenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PreOrderCartViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: PreOrderCartViewHolder, position: Int) {
        val menuItems = differ.currentList[position]
        holder.bind(menuItems)
    }

    fun submitList(list: MutableList<MenuItems>) {
        differ.submitList(list)
    }
}