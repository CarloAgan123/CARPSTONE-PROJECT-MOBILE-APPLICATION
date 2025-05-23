package com.example.myfirstapplication.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.myfirstapplication.R
import com.example.myfirstapplication.data.Stall
import com.example.myfirstapplication.databinding.StallsRvItemBinding
import com.example.myfirstapplication.fragments.loginRegister.stall.StallProductsFragment
import java.text.SimpleDateFormat
import java.util.*

class StallsAdapter(
    private val onClick: (Stall) -> Unit
) : RecyclerView.Adapter<StallsAdapter.StallsViewHolder>() {

    inner class StallsViewHolder(private val binding: StallsRvItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(stall: Stall) {
            binding.apply {
                Glide.with(binding.root.context)
                    .load(stall.Img)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.default_stall)
                            .error(R.drawable.new_logo))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageStallRvItem)

                tvStallsName.text = stall.stallName

                val isOpen = isStallOpen(stall)
                tvOpenCloseIndicator.text = if (isOpen) "Open" else "Closed"

                btnVisitStall.setOnClickListener {
                    onClick(stall)
                }
            }
        }

        private fun isStallOpen(stall: Stall): Boolean {
            val dateFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            val timeFormat12Hour = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val timeFormat24Hour = SimpleDateFormat("HH:mm", Locale.getDefault())

            timeFormat24Hour.timeZone = TimeZone.getTimeZone("Asia/Manila")

            val currentDay = dateFormat.format(Date())
            val currentTimeString = timeFormat24Hour.format(Date())
            val currentTime = timeFormat24Hour.parse(currentTimeString)

            val selectedDays = stall.selectedDays ?: return false

            if (selectedDays.contains(currentDay)) {
                val timeIn = stall.timein?.let { timeFormat12Hour.parse(it) }?.let { timeFormat24Hour.format(it) }?.let { timeFormat24Hour.parse(it) }
                val timeOut = stall.timeout?.let { timeFormat12Hour.parse(it) }?.let { timeFormat24Hour.format(it) }?.let { timeFormat24Hour.parse(it) }

                if (currentTime != null && timeIn != null && timeOut != null) {
                    return currentTime.after(timeIn) && currentTime.before(timeOut)
                }
            }
            return false
        }
    }

    private val diffCallback = object : DiffUtil.ItemCallback<Stall>() {
        override fun areItemsTheSame(oldItem: Stall, newItem: Stall): Boolean {
            return oldItem.ID == newItem.ID
        }

        override fun areContentsTheSame(oldItem: Stall, newItem: Stall): Boolean {
            return oldItem == newItem
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StallsViewHolder {
        val binding = StallsRvItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StallsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StallsViewHolder, position: Int) {
        val stall = differ.currentList[position]
        holder.bind(stall)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    fun submitList(list: List<Stall>) {
        differ.submitList(list)
    }
}




