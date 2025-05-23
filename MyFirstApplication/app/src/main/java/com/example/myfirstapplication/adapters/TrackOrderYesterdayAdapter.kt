package com.example.myfirstapplication.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.myfirstapplication.adapters.TrackOrderAdapter.TrackOrderViewHolder
import com.example.myfirstapplication.data.OrderDetails
import com.example.myfirstapplication.databinding.OrderListItemBinding

class TrackOrderYesterdayAdapter: RecyclerView.Adapter<TrackOrderYesterdayAdapter.TrackOrderYesterdayViewHolder>() {
    inner class TrackOrderYesterdayViewHolder(private val binding: OrderListItemBinding):RecyclerView.ViewHolder(binding.root) {

        fun bind(orderDetails: OrderDetails) {
            binding.tvOrderNumberTitle.text = "Order #${orderDetails.orderId}"
            binding.tvOrderDate.text = "Placed on ${orderDetails.orderDate}"
            binding.tvOrderStatus.text = "Status ${orderDetails.orderStatus}"
            binding.tvOrderTotalPrice.text = "₱${orderDetails.totalPrice}"

            binding.root.setOnClickListener{
                onClick?.invoke(orderDetails)
            }
        }

    }

    private val diffCallback = object : DiffUtil.ItemCallback<OrderDetails>() {
        override fun areItemsTheSame(oldItem: OrderDetails, newItem: OrderDetails): Boolean {
            return oldItem.orderId == newItem.orderId
        }

        override fun areContentsTheSame(oldItem: OrderDetails, newItem: OrderDetails): Boolean {
            return oldItem == newItem
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackOrderYesterdayViewHolder {
        val binding = OrderListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrackOrderYesterdayViewHolder(binding)
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: TrackOrderYesterdayViewHolder, position: Int) {
        val orderDetails = differ.currentList[position]
        holder.bind(orderDetails)
    }

    fun submitList(list: List<OrderDetails>) {
        differ.submitList(list)
    }

    var onClick: ((OrderDetails) -> Unit)? = null

}