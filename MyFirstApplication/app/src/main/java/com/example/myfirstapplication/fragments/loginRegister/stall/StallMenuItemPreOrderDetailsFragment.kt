package com.example.myfirstapplication.fragments.loginRegister.stall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.myfirstapplication.R
import com.example.myfirstapplication.data.CartItem
import com.example.myfirstapplication.data.MenuItems
import com.example.myfirstapplication.databinding.FragmentMenuItemDetailsBinding
import com.example.myfirstapplication.databinding.FragmentMenuItemPreorderDetailsBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class StallMenuItemPreOrderDetailsFragment : Fragment() {

    private lateinit var binding : FragmentMenuItemPreorderDetailsBinding
    private var menuItems : MenuItems? = null
    private lateinit var databaseReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMenuItemPreorderDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseReference = FirebaseDatabase.getInstance().reference.child("PreOrderItems")
        menuItems = arguments?.getParcelable("menuItems")


        menuItems.let { menuItems ->
            binding.apply {
                tvStallMenuItemName.text = menuItems?.menuName
                tvStallMenuItemPrice.text = "₱${menuItems?.menuPrice}"
                tvStallMenuItemQuantity.text = "Stocks: ${menuItems?.menuQuantity} available"
                tvStallNameMenuItem.text = "Stall: ${menuItems?.menuItemStallName}"

                val dateFormat = SimpleDateFormat("EEE, MMMM d, yyyy", Locale.ENGLISH)
                dateFormat.timeZone = TimeZone.getTimeZone("Asia/Manila")

                val calendar = Calendar.getInstance()
                calendar.timeZone = TimeZone.getTimeZone("Asia/Manila")
                calendar.add(Calendar.DATE, 1)
                val tomorrowDate = dateFormat.format(calendar.time)

                if (menuItems?.menuType == tomorrowDate) {
                    tvMenuItemDate.text = "Tomorrow (${menuItems?.menuType.toString()})"
                } else {
                    tvMenuItemDate.text = "Date (${menuItems?.menuType.toString()})"
                }
                if (menuItems != null) {
                    Glide.with(this@StallMenuItemPreOrderDetailsFragment)
                        .load(menuItems.menuImageUrl)
                        .apply(
                            RequestOptions()
                                .placeholder(R.drawable.default_icon)
                                .error(R.drawable.new_logo))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivStallMenuItemImage)
                }
            }
        }

        binding.ivButtonClose.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnStallMenuItemPreOrder.setOnClickListener {
            menuItems?.let { menuItems ->
                val cartItem = CartItem(
                    productId = menuItems.menuItemID,
                    productName = menuItems.menuName,
                    productStallName = menuItems.menuItemStallName,
                    productPrice = menuItems.menuPrice.toInt(),
                    subTotalPrice = menuItems.menuPrice.toInt(),
                    productImage = menuItems.menuImageUrl,
                    productQuantity = 1,
                    cartItemType = "Pre-Order",
                    isChecked = true
                )

                val checkedItemsArrayList = arrayListOf(cartItem)

                val bundle = Bundle().apply {
                    putParcelableArrayList("checkedItems", checkedItemsArrayList)
                }

                findNavController().navigate(R.id.action_stallMenuItemPreOrderDetailsFragment_to_checkoutFragment, bundle)
            }
        }

    }
}