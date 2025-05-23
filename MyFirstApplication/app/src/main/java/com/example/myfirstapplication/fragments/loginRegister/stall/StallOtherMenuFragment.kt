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
import com.example.myfirstapplication.adapters.StallOtherMenuAdapter
import com.example.myfirstapplication.data.MenuItems
import com.example.myfirstapplication.databinding.FragmentStallOthersBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class StallOtherMenuFragment : Fragment(R.layout.fragment_stall_others){

    private lateinit var binding : FragmentStallOthersBinding
    private lateinit var stallOtherMenuAdapter: StallOtherMenuAdapter
    private lateinit var menuItemRef: DatabaseReference
    private var stallName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStallOthersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stallName = arguments?.getString("STALL_NAME")
        setupOtherMenuItem()
        fetchOtherMenuItem()

        stallOtherMenuAdapter.onClick = { menuItems ->
            val bundle = Bundle().apply {
                putParcelable("menuItems", menuItems)
            }
            findNavController().navigate(R.id.action_stallDetailsFragment_to_stallMenuItemPreOrderDetailsFragment, bundle)
        }

    }

    private fun setupOtherMenuItem() {
        stallOtherMenuAdapter = StallOtherMenuAdapter()
        binding.rvStallOtherMenu.apply {
            layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
            adapter = stallOtherMenuAdapter
        }
    }

    private fun fetchOtherMenuItem() {

        val dateFormat = SimpleDateFormat("EEE, MMMM d, yyyy", Locale.ENGLISH)
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Manila")
        val currentDate = dateFormat.format(Date())

        val calendar = Calendar.getInstance()
        calendar.timeZone = TimeZone.getTimeZone("Asia/Manila")
        calendar.add(Calendar.DATE, 1)
        val tomorrowDate = dateFormat.format(calendar.time)

        menuItemRef = FirebaseDatabase.getInstance().getReference("MenuItems")
        menuItemRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val menuDetailsList = mutableListOf<MenuItems>()
                for (menuTypeSnapshot in snapshot.children) {
                    try {
                        val menuDetails = menuTypeSnapshot.getValue(MenuItems::class.java)
                        if (menuDetails != null && menuDetails.menuItemStallName == stallName
                            && menuDetails.menuType != currentDate && menuDetails.menuType != tomorrowDate) {
                            menuDetailsList.add(menuDetails)
                        }
                    } catch (e: Exception) {
                        Log.e("StallOtherMenuFragment", "Failed to parse MenuItems: ${e.message}")
                    }
                }
                stallOtherMenuAdapter.submitList(menuDetailsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("StallOtherMenuFragment", "Database error: ${error.message}")
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

}