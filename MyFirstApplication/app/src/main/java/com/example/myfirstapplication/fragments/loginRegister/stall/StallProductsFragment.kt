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
import com.example.myfirstapplication.adapters.BestProductsAdapter
import com.example.myfirstapplication.data.Product
import com.example.myfirstapplication.data.Stall
import com.example.myfirstapplication.databinding.FragmentStallProductsBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class StallProductsFragment : Fragment(R.layout.fragment_stall_products) {

    private lateinit var binding: FragmentStallProductsBinding
    private lateinit var bestProductAdapter: BestProductsAdapter
    private lateinit var productsRef: DatabaseReference
    private var stallName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStallProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stallName = arguments?.getString("STALL_NAME")

        setupBestProductsRv()
        fetchBestProductsFromFirebase()
        
        bestProductAdapter.onClick = {product ->
            val bundle = Bundle().apply {
                putParcelable("product", product)
            }
            findNavController().navigate(R.id.action_stallDetailsFragment_to_productDetailsFragment, bundle)
        }
        
    }

    private fun setupBestProductsRv() {
        bestProductAdapter = BestProductsAdapter()
        binding.rvStallProduct.apply {
            layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
            adapter = bestProductAdapter
        }
    }

    private fun fetchBestProductsFromFirebase() {
        productsRef = FirebaseDatabase.getInstance().getReference("Products")
        productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bestProductsList = mutableListOf<Product>()
                for (productSnapshot in snapshot.children) {
                    try {
                        val product = productSnapshot.getValue(Product::class.java)
                        if (product != null && product.productStallName == stallName) {
                            Log.d("StallProductsFragment", "Fetched product: $product")
                            bestProductsList.add(product)
                        }
                    } catch (e: Exception) {
                        Log.e("StallProductsFragment", "Failed to parse product: ${e.message}")
                    }
                }
                bestProductAdapter.submitList(bestProductsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("StallProductsFragment", "Database error: ${error.message}")
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

