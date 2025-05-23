package com.example.myfirstapplication.fragments.loginRegister.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myfirstapplication.R
import com.example.myfirstapplication.adapters.HomeViewpagerAdapter
import com.example.myfirstapplication.databinding.FragmentHomeBinding
import com.example.myfirstapplication.fragments.loginRegister.categories.DessertsCategoryFragment
import com.example.myfirstapplication.fragments.loginRegister.categories.DishCategoryFragment
import com.example.myfirstapplication.fragments.loginRegister.categories.DrinksCategoryFragment
import com.example.myfirstapplication.fragments.loginRegister.categories.FastFoodCategoryFragment
import com.example.myfirstapplication.fragments.loginRegister.categories.MainCategoryFragment
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment: Fragment(R.layout.fragment_home) {
    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.searchBar.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }

        binding.notification.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_notificationFragment)
        }

        val categoriesFragments = arrayListOf<Fragment>(
            MainCategoryFragment(),
            DishCategoryFragment(),
            DrinksCategoryFragment(),
            DessertsCategoryFragment(),
            FastFoodCategoryFragment()
        )

        binding.viewpagerHome.isUserInputEnabled = false

        val viewPager2Adapter =
            HomeViewpagerAdapter(categoriesFragments, childFragmentManager, lifecycle)
        binding.viewpagerHome.adapter = viewPager2Adapter
        TabLayoutMediator(binding.tabLayout, binding.viewpagerHome) {tab, position ->
            when (position) {
                0 -> tab.text = "Main"
                1 -> tab.text = "Dish"
                2 -> tab.text = "Drinks"
                3 -> tab.text = "Dessert"
                4 -> tab.text = "Fast Food"
            }
        }.attach()
    }

}