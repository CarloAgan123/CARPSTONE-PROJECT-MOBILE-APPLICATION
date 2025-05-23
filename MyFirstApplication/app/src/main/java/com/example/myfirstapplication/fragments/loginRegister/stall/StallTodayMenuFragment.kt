package com.example.myfirstapplication.fragments.loginRegister.stall

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myfirstapplication.R
import com.example.myfirstapplication.adapters.StallTodayMenuAdapter
import com.example.myfirstapplication.data.MenuItems
import com.example.myfirstapplication.databinding.FragmentStallTodayMenuBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class StallTodayMenuFragment: Fragment (R.layout.fragment_stall_today_menu) {

    private lateinit var binding: FragmentStallTodayMenuBinding
    private lateinit var stallTodayMenuAdapter: StallTodayMenuAdapter
    private lateinit var menuItemRef: DatabaseReference
    private var stallName: String? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStallTodayMenuBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stallName = arguments?.getString("STALL_NAME")
        setupTodayMenuItem()
        fetchTodayMenuItem()

        stallTodayMenuAdapter.onClick = { menuItems ->
            val bundle = Bundle().apply {
                putParcelable("menuItems", menuItems)
            }
            findNavController().navigate(R.id.action_stallDetailsFragment_to_stallMenuItemDetailsFragment, bundle)
        }

    }

    private fun setupTodayMenuItem() {
        stallTodayMenuAdapter = StallTodayMenuAdapter()
        binding.rvStallTodayMenu.apply {
            layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
            adapter = stallTodayMenuAdapter
        }
    }

    private fun fetchTodayMenuItem() {

        val dateFormat = SimpleDateFormat("EEE, MMMM d, yyyy", Locale.ENGLISH)
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Manila")
        val currentDate = dateFormat.format(Date())

        menuItemRef = FirebaseDatabase.getInstance().getReference("MenuItems")
        menuItemRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val menuDetailsList = mutableListOf<MenuItems>()
                for (menuTypeSnapshot in snapshot.children) {
                    try {
                        val menuDetails = menuTypeSnapshot.getValue(MenuItems::class.java)
                        if (menuDetails != null && menuDetails.menuItemStallName == stallName
                            && menuDetails.menuType == currentDate) {
                            menuDetailsList.add(menuDetails)
                        }
                    } catch (e: Exception) {
                        Log.e("StallTodayMenuFragment", "Failed to parse MenuItems: ${e.message}")
                    }
                }
                stallTodayMenuAdapter.submitList(menuDetailsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("StallTodayMenuFragment", "Database error: ${error.message}")
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}