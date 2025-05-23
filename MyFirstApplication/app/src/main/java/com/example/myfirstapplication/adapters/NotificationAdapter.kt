package com.example.myfirstapplication.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myfirstapplication.data.NotificationItem
import com.example.myfirstapplication.databinding.NotificationItemBinding

class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    private val notificationList = mutableListOf<NotificationItem>()

    inner class NotificationViewHolder(private val binding: NotificationItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(notificationItem: NotificationItem) {
            binding.notificationTitle.text = notificationItem.title
            binding.notificationMessage.text = notificationItem.message
            binding.notificationTimestamp.text = notificationItem.timestamp
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = NotificationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return notificationList.size
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notificationList[position])
    }

    fun setNotifications(notifications: List<NotificationItem>) {
        notificationList.clear()
        notificationList.addAll(notifications)
        notifyDataSetChanged()
    }
}
