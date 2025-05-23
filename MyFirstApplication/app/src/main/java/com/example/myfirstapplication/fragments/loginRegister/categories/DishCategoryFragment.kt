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
import com.example.myfirstapplication.R
import com.example.myfirstapplication.adapters.BestProductsAdapter
import com.example.myfirstapplication.adapters.DishCategoryAdapter
import com.example.myfirstapplication.data.Product
import com.example.myfirstapplication.databinding.FragmentDishCategoryBinding
import com.google.firebase.database.*

class DishCategoryFragment : Fragment(R.layout.fragment_dish_category) {
    private lateinit var binding: FragmentDishCategoryBinding
    private lateinit var bestProductsAdapter: BestProductsAdapter
    private lateinit var dishCategoryAdapter: DishCategoryAdapter
    private lateinit var specialDishRef: DatabaseReference
    private lateinit var bestProductsRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDishCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpecialDishCategory()
        setupBestProductsRv()
        fetchSpecialDishCategoryFromFirebase()
        fetchBestProductsFromFirebase()

        dishCategoryAdapter.onClick = { product ->
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

    private fun setupSpecialDishCategory() {
        dishCategoryAdapter = DishCategoryAdapter()
        binding.rvOffer.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = dishCategoryAdapter
        }
    }

    private fun setupBestProductsRv() {
        bestProductsAdapter = BestProductsAdapter()
        binding.rvDishCategory.apply {
            layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
            adapter = bestProductsAdapter
        }
    }

    private fun fetchSpecialDishCategoryFromFirebase() {
        specialDishRef = FirebaseDatabase.getInstance().getReference("Products")
        specialDishRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productsList = mutableListOf<Product>()
                for (productSnapshot in snapshot.children) {
                    try {
                        val product = productSnapshot.getValue(Product::class.java)
                        if (product != null && product.productCategory == "Dish" && product.productIdentity == "Special") {
                            Log.d("DishCategoryFragment", "Fetched product: $product")
                            productsList.add(product)
                        } else {
                            Log.d("DishCategoryFragment", "Product does not match criteria or is null for snapshot: $productSnapshot")
                        }
                    } catch (e: Exception) {
                        Log.e("DishCategoryFragment", "Failed to parse product: ${e.message}")
                    }
                }
                dishCategoryAdapter.submitList(productsList)
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
                        if (product != null && product.productCategory == "Dish") {
                            Log.d("DishCategoryFragment", "Fetched product: $product")
                            productsList.add(product)
                        } else {
                            Log.d("DishCategoryFragment", "Product does not match criteria or is null for snapshot: $productSnapshot")
                        }
                    } catch (e: Exception) {
                        Log.e("DishCategoryFragment", "Failed to parse product: ${e.message}")
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
        Log.e("DishCategoryFragment", "Database error: ${error.message}")
        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
    }
}
