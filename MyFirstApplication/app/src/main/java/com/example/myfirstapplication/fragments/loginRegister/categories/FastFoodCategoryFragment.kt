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
import com.example.myfirstapplication.adapters.FastFoodCategoryAdapter
import com.example.myfirstapplication.data.Product
import com.example.myfirstapplication.databinding.FragmentFastfoodCategoryBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FastFoodCategoryFragment: Fragment(R.layout.fragment_fastfood_category) {
    private lateinit var binding: FragmentFastfoodCategoryBinding
    private lateinit var fastFoodCategoryAdapter: FastFoodCategoryAdapter
    private lateinit var bestProductsAdapter: BestProductsAdapter
    private lateinit var fastFoodRef: DatabaseReference
    private lateinit var bestProductsRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFastfoodCategoryBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpecialFastFoodCategory()
        setupBestProductsRv()
        fetchSpecialFastFoodFromFirebase()
        fetchBestProductsFromFirebase()

        fastFoodCategoryAdapter.onClick = { product ->
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

    private fun setupSpecialFastFoodCategory() {
        fastFoodCategoryAdapter = FastFoodCategoryAdapter()
        binding.rvSpecialFastFoods.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = fastFoodCategoryAdapter
        }
    }

    private fun setupBestProductsRv() {
        bestProductsAdapter = BestProductsAdapter()
        binding.rvFastFoodsCategory.apply {
            layoutManager = GridLayoutManager(requireContext(),1, GridLayoutManager.VERTICAL, false)
            adapter = bestProductsAdapter
        }
    }

    private fun fetchSpecialFastFoodFromFirebase() {
        fastFoodRef = FirebaseDatabase.getInstance().getReference("Products")
        fastFoodRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val productList = mutableListOf<Product>()
                for (productSnapshot in snapshot.children) {
                    try {
                        val product = productSnapshot.getValue(Product::class.java)
                        if (product != null && product.productCategory == "Fast Food" && product.productIdentity == "Special"){
                            Log.d("DessertsCategoryFragment", "Fetch Product: $product")
                            productList.add(product)
                        } else {
                            Log.d("DessertsCategoryFragment", "Product does not match criteria or is null for snapshot: $productSnapshot")
                        }
                    } catch (e:Exception){
                        Log.e("DessertsCategoryFragment", "Failed to parse product: ${e.message}")
                    }
                }
                fastFoodCategoryAdapter.submitList(productList)
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
                        if (product != null && product.productCategory == "Fast Food") {
                            Log.d("DessertsCategoryFragment", "Fetched product: $product")
                            productsList.add(product)
                        } else {
                            Log.d("DessertsCategoryFragment", "Product does not match criteria or is null for snapshot: $productSnapshot")
                        }
                    } catch (e: Exception) {
                        Log.e("DessertsCategoryFragment", "Failed to parse product: ${e.message}")
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
        Log.e("FastFoodCategoryFragment", "Database error: ${error.message}")
        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
    }

}