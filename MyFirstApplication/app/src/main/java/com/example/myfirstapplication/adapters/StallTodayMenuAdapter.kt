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
import com.example.myfirstapplication.databinding.PreOrderItemBinding

class StallTodayMenuAdapter : RecyclerView.Adapter<StallTodayMenuAdapter.StallTodayMenuHolder>() {

    inner class StallTodayMenuHolder(private val binding: PreOrderItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(menuItem: MenuItems) {
            Glide.with(binding.root.context)
                .load(menuItem.menuImageUrl)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.default_icon)
                        .error(R.drawable.new_logo)
                )
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.imagePreOrderItem)

            binding.tvPreOrderProductName.text = menuItem.menuName
            binding.tvPreOrderProductPrice.text = menuItem.menuPrice.toString()
            binding.tvPreOrderProductQuantity.text = menuItem.menuQuantity

            binding.root.setOnClickListener {
                onClick?.invoke(menuItem)
            }
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StallTodayMenuHolder {
        val binding = PreOrderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StallTodayMenuHolder(binding)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: StallTodayMenuHolder, position: Int) {
        val menuItem = differ.currentList[position]
        holder.bind(menuItem)
    }

    fun submitList(list: List<MenuItems>) {
        differ.submitList(list)
    }

    var onClick: ((MenuItems) -> Unit)? = null
}
