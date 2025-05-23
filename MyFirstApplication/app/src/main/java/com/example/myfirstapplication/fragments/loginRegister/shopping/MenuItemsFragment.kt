package com.example.myfirstapplication.fragments.loginRegister.shopping

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myfirstapplication.R
import com.example.myfirstapplication.adapters.MenuItemsAdapter
import com.example.myfirstapplication.data.MenuItems
import com.example.myfirstapplication.databinding.FragmentPreOrderDetailsBinding
import com.example.myfirstapplication.databinding.FragmentStallTodayMenuBinding
import com.example.myfirstapplication.util.showBottomNavigationView
import com.github.leandroborgesferreira.loadingbutton.customViews.CircularProgressButton
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

class MenuItemsFragment : Fragment(R.layout.fragment_pre_order_details) {
    private lateinit var binding: FragmentPreOrderDetailsBinding
    private lateinit var menuItemsAdapter: MenuItemsAdapter
    private lateinit var menuItemRef: DatabaseReference
    private var stallName: String? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPreOrderDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stallName = arguments?.getString("STALL_NAME")
        setupMenuItem()
        fetchMenuItem()

        showBottomNavigationView()

        menuItemsAdapter.onClick = { menuItems ->
            val bundle = Bundle().apply {
                putParcelable("menuItems", menuItems)
            }
            findNavController().navigate(R.id.action_stallDetailsFragment_to_stallMenuItemDetailsFragment, bundle)
        }

        val nestedScrollView = view.findViewById<RecyclerView>(R.id.rvPreOrderItem)
        val imageButton = view.findViewById<ImageButton>(R.id.imageButton)

        nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val contentView = nestedScrollView.getChildAt(nestedScrollView.childCount - 1)
            val diff = contentView.bottom - (nestedScrollView.height + scrollY)

            if (diff > 0) {
                imageButton.animate().alpha(1.0f).setDuration(500).start()
            } else {
                imageButton.animate().alpha(0.0f).setDuration(500).start()
            }
        }
        val radioBtnToday = view.findViewById<RadioButton>(R.id.radioButtonToday)
        val radioBtnTomorrow = view.findViewById<RadioButton>(R.id.radioButtonTomorrow)
        val radioBtnOthers = view.findViewById<RadioButton>(R.id.radioButtonOthers)
        val radioBtnAll = view.findViewById<RadioButton>(R.id.radioButtonAll)


        radioBtnAll.setOnCheckedChangeListener{_, isChecked ->
            if (isChecked) {
                radioBtnToday.isChecked = false
                radioBtnTomorrow.isChecked = false
                radioBtnOthers.isChecked = false
                fetchMenuItem()
            }
        }

        radioBtnToday.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                radioBtnTomorrow.isChecked = false
                radioBtnAll.isChecked = false
                radioBtnOthers.isChecked = false
                fetchMenuItemsForToday()
            }
        }

        radioBtnTomorrow.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                radioBtnToday.isChecked = false
                radioBtnAll.isChecked = false
                radioBtnOthers.isChecked = false
                fetchMenuItemsForTomorrow()
            }
        }

        radioBtnOthers.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                radioBtnToday.isChecked = false
                radioBtnAll.isChecked = false
                radioBtnTomorrow.isChecked = false
                fetchMenuItemsForOthers()
            }
        }

        menuItemsAdapter.onClick = { menuItems ->
            if (menuItems.menuType == "MenuToday") {
                val bundle = Bundle().apply {
                    putParcelable("menuItems", menuItems)
                }
                findNavController().navigate(R.id.action_menuItemsFragment_to_stallMenuItemDetailsFragment, bundle)
            } else {
                val bundle = Bundle().apply {
                    putParcelable("menuItems", menuItems)
                }
                findNavController().navigate(R.id.action_menuItemsFragment_to_stallMenuItemPreOrderDetailsFragment, bundle)
            }
        }
    }

    private fun fetchMenuItemsForOthers() {

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
                        if (menuDetails != null && menuDetails.menuType != currentDate && menuDetails.menuType != tomorrowDate) {
                            menuDetailsList.add(menuDetails)
                        }
                    } catch (e: Exception) {
                        Log.e("Menus", "Failed to parse MenuItems: ${e.message}")
                    }
                }
                menuItemsAdapter.submitList(menuDetailsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Menus", "Database error: ${error.message}")
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchMenuItemsForToday() {

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
                        if (menuDetails != null && menuDetails.menuType == currentDate) {
                            menuDetailsList.add(menuDetails)
                        }
                    } catch (e: Exception) {
                        Log.e("Menus", "Failed to parse MenuItems: ${e.message}")
                    }
                }
                menuItemsAdapter.submitList(menuDetailsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Menus", "Database error: ${error.message}")
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun fetchMenuItemsForTomorrow() {

        val dateFormat = SimpleDateFormat("EEE, MMMM d, yyyy", Locale.ENGLISH)
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Manila")

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
                        if (menuDetails != null && menuDetails.menuType == tomorrowDate) {
                            menuDetailsList.add(menuDetails)
                        }
                    } catch (e: Exception) {
                        Log.e("Menus", "Failed to parse MenuItems: ${e.message}")
                    }
                }
                menuItemsAdapter.submitList(menuDetailsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Menus", "Database error: ${error.message}")
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupMenuItem() {
        menuItemsAdapter = MenuItemsAdapter()
        binding.rvPreOrderItem.apply {
            layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
            adapter = menuItemsAdapter
        }
    }

    private fun fetchMenuItem() {
        menuItemRef = FirebaseDatabase.getInstance().getReference("MenuItems")
        menuItemRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val menuDetailsList = mutableListOf<MenuItems>()
                for (menuTypeSnapshot in snapshot.children) {
                    try {
                        val menuDetails = menuTypeSnapshot.getValue(MenuItems::class.java)
                        if (menuDetails != null) {
                            menuDetailsList.add(menuDetails)
                        }
                    } catch (e: Exception) {
                        Log.e("Menus", "Failed to parse MenuItems: ${e.message}")
                    }
                }
                menuItemsAdapter.submitList(menuDetailsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Menus", "Database error: ${error.message}")
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}