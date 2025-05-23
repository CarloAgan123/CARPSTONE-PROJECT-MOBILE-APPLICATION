package com.example.myfirstapplication.fragments.loginRegister.shopping

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.myfirstapplication.R
import com.example.myfirstapplication.adapters.BestProductsAdapter
import com.example.myfirstapplication.adapters.HomeViewpagerAdapter
import com.example.myfirstapplication.adapters.StallsAdapter
import com.example.myfirstapplication.data.Product
import com.example.myfirstapplication.data.Stall
import com.example.myfirstapplication.databinding.FragmentStallsDetailsBinding
import com.example.myfirstapplication.fragments.loginRegister.categories.MainCategoryFragment.PagingInfo
import com.example.myfirstapplication.fragments.loginRegister.stall.StallOtherMenuFragment
import com.example.myfirstapplication.fragments.loginRegister.stall.StallProductsFragment
import com.example.myfirstapplication.fragments.loginRegister.stall.StallTodayMenuFragment
import com.example.myfirstapplication.fragments.loginRegister.stall.StallTomorrowMenuFragment
import com.example.myfirstapplication.util.hideBottomNavigationView
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class StallDetailsFragment : Fragment() {

    private lateinit var binding: FragmentStallsDetailsBinding
    private var stall: Stall? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        hideBottomNavigationView()
        binding = FragmentStallsDetailsBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stall = arguments?.getParcelable("stall")

        setupViewPager()

        stall?.let { stall ->
            binding.apply {
                Glide.with(requireContext())
                    .load(stall.Img)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.sample_image)
                            .error(R.drawable.new_logo)
                    )
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.ivStallImageDetails)

                tvStallsName.text = stall.stallName
                val isOpen = isStallOpen(stall)
                tvOpenCloseIndicator2.text = if (isOpen) "Open" else "Closed"
                tvOperationHours.text = "${stall.timein} - ${stall.timeout}"

                val daysOfOperation = stall.selectedDays?.joinToString(separator = ", ") { day ->
                    day.take(3)
                }
                tvDaysOfOperation.text = daysOfOperation

                binding.imageClose.setOnClickListener {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        } ?: run {
            Log.e("StallDetailsFragment", "No stall data found")
        }
    }

    private fun setupViewPager() {
        val stallFragmentList = arrayListOf<Fragment>(
            StallProductsFragment().apply {
                arguments = Bundle().apply {
                    putString("STALL_NAME", stall?.stallName)
                }
            },
            StallTodayMenuFragment().apply {
                arguments = Bundle().apply {
                    putString("STALL_NAME", stall?.stallName)
                }
            },
            StallTomorrowMenuFragment().apply {
                arguments = Bundle().apply {
                    putString("STALL_NAME", stall?.stallName)
                }
            },
            StallOtherMenuFragment().apply {
                arguments = Bundle().apply {
                    putString("STALL_NAME", stall?.stallName)
                }
            }
        )

        val viewPager2Adapter =
            HomeViewpagerAdapter(stallFragmentList, childFragmentManager, lifecycle)
        binding.viewPagerStall.adapter = viewPager2Adapter

        TabLayoutMediator(binding.tabLayoutMenus, binding.viewPagerStall) { tab, position ->
            when (position) {
                0 -> tab.text = "Products"
                1 -> tab.text = "Today's Menu"
                2 -> tab.text = "Tomorrow's Menu"
                3 -> tab.text = "Other Menu"
            }
        }.attach()
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
