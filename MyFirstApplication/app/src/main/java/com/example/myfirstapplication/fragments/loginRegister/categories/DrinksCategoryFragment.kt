package com.example.myfirstapplication.fragments.loginRegister.categories

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.example.myfirstapplication.R
import com.example.myfirstapplication.adapters.BestProductsAdapter
import com.example.myfirstapplication.adapters.DrinksCategoryAdapter
import com.example.myfirstapplication.data.Product
import com.example.myfirstapplication.databinding.FragmentDrinksCategoryBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DrinksCategoryFragment: Fragment(R.layout.fragment_drinks_category) {
    private lateinit var binding: FragmentDrinksCategoryBinding
    private lateinit var drinksCategoryAdapter: DrinksCategoryAdapter
    private lateinit var bestProductsAdapter: BestProductsAdapter
    private lateinit var specialDrinksRef: DatabaseReference
    private lateinit var bestProductsRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDrinksCategoryBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpecialDrinksCategory()
        setupBestProductsRv()
        fetchSpecialDrinksCategoryFromFirebase()
        fetchBestProductsFromFirebase()

        drinksCategoryAdapter.onClick = { product ->
            val bundle = Bundle().apply {
                putParcelable("product", product)
            }
            findNavController().navigate(R.id.action_homeFragment_to_productDetailsFragment, bundle)
        }

        bestProductsAdapter.onClick = { product ->
            val bundle = Bundle().apply {
                putParcelable("product", product)
            }
            findNavController().navigate(R.id.action_homeFragment_to_productDetailsFragment, bundle)
        }

    }

    private fun setupSpecialDrinksCategory() {
        drinksCategoryAdapter = DrinksCategoryAdapter()
        binding.rvSpecialDrinks.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = drinksCategoryAdapter
        }
    }

    private fun setupBestProductsRv() {
        bestProductsAdapter = BestProductsAdapter()
        binding.rvDrinksCategory.apply {
            layoutManager = GridLayoutManager(requireContext(),1, GridLayoutManager.VERTICAL, false)
            adapter = bestProductsAdapter
        }
    }

    private fun fetchSpecialDrinksCategoryFromFirebase() {
        specialDrinksRef = FirebaseDatabase.getInstance().getReference("Products")
        specialDrinksRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val productList = mutableListOf<Product>()
                for (productSnapshot in snapshot.children) {
                    try {
                        val product = productSnapshot.getValue(Product::class.java)
                        if (product != null && product.productCategory == "Drinks" && product.productIdentity == "Special"){
                            Log.d("DrinksCategoryFragment", "Fetch Product: $product")
                            productList.add(product)
                        } else {
                            Log.d("DrinksCategoryFragment", "Product does not match criteria or is null for snapshot: $productSnapshot")
                        }
                    } catch (e:Exception){
                        Log.e("DrinksCategoryFragment", "Failed to parse product: ${e.message}")
                    }
                }
                drinksCategoryAdapter.submitList(productList)
            }

            override fun onCancelled(error: DatabaseError) {
                handleError(error)
            }
        })
    }

    private fun fetchBestProductsFromFirebase() {
        bestProductsRef = FirebaseDatabase.getInstance().getReference("Products")
        bestProductsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productsList = mutableListOf<Product>()
                for (productSnapshot in snapshot.children) {
                    try {
                        val product = productSnapshot.getValue(Product::class.java)
                        if (product != null && product.productCategory == "Drinks") {
                            Log.d("DrinksCategoryFragment", "Fetched product: $product")
                            productsList.add(product)
                        } else {
                            Log.d("DrinksCategoryFragment", "Product does not match criteria or is null for snapshot: $productSnapshot")
                        }
                    } catch (e: Exception) {
                        Log.e("DrinksCategoryFragment", "Failed to parse product: ${e.message}")
                    }
                }
                bestProductsAdapter.submitList(productsList)
            }

            override fun onCancelled(error: DatabaseError) {
                handleError(error)
            }
        })
    }

    private fun handleError(error: DatabaseError) {
        Log.e("DrinksCategoryFragment", "Database error: ${error.message}")
        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
    }

}