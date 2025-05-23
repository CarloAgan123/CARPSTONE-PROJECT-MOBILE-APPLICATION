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
import com.example.myfirstapplication.adapters.SpecialProductsAdapter
import com.example.myfirstapplication.adapters.StallsAdapter
import com.example.myfirstapplication.data.Product
import com.example.myfirstapplication.data.Stall
import com.example.myfirstapplication.databinding.FragmentMainCategoryBinding
import com.example.myfirstapplication.util.showBottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainCategoryFragment : Fragment(R.layout.fragment_main_category) {

    private lateinit var binding: FragmentMainCategoryBinding
    private lateinit var specialProductsAdapter: SpecialProductsAdapter
    private lateinit var stallsAdapter: StallsAdapter
    private lateinit var bestProductAdapter: BestProductsAdapter
    private lateinit var productsRef: DatabaseReference
    private lateinit var stallsRef: DatabaseReference
    private val pagingInfo = PagingInfo()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSpecialProductsRv()
        setupStallsRv()
        setupBestProductsRv()
        fetchSpecialProductsFromFirebase()
        fetchStallsFromFirebase()
        fetchBestProductsFromFirebase()

        specialProductsAdapter.onClick = { product ->
            val bundle = Bundle().apply {
                putParcelable("product", product)
            }
            findNavController().navigate(R.id.action_homeFragment_to_productDetailsFragment, bundle)
        }

        bestProductAdapter.onClick = { product ->
            val bundle = Bundle().apply {
                putParcelable("product", product)
            }
            findNavController().navigate(R.id.action_homeFragment_to_productDetailsFragment, bundle)
        }
    }

    private fun setupSpecialProductsRv() {
        specialProductsAdapter = SpecialProductsAdapter()
        binding.rvSpecialProducts.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = specialProductsAdapter
        }
    }

    private fun setupStallsRv() {
        stallsAdapter = StallsAdapter { stall ->
            val bundle = Bundle().apply {
                putParcelable("stall", stall)
            }
            findNavController().navigate(R.id.action_homeFragment_to_stallDetailsFragment, bundle)
        }

        binding.rvStalls.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = stallsAdapter
        }
    }

    private fun setupBestProductsRv() {
        bestProductAdapter = BestProductsAdapter()
        binding.rvBestProducts.apply {
            layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
            adapter = bestProductAdapter
        }
    }

    private fun fetchSpecialProductsFromFirebase() {
        productsRef = FirebaseDatabase.getInstance().getReference("Products")
        showLoading()

        productsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val specialProductsList = mutableListOf<Product>()
                for (productSnapshot in snapshot.children) {
                    try {
                        val product = productSnapshot.getValue(Product::class.java)
                        if (product != null && product.productIdentity == "Special") {
                            Log.d("MainCategoryFragment", "Fetched special product: $product")
                            specialProductsList.add(product)
                        } else if (product == null) {
                            Log.d("MainCategoryFragment", "Product is null for snapshot: $productSnapshot")
                        }
                    } catch (e: Exception) {
                        Log.e("MainCategoryFragment", "Failed to parse product: ${e.message}")
                    }
                }
                specialProductsAdapter.submitList(specialProductsList)
                hideLoading()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainCategoryFragment", "Database error: ${error.message}")
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                hideLoading()
            }
        })
    }

    private fun fetchStallsFromFirebase() {
        stallsRef = FirebaseDatabase.getInstance().getReference("adminInformation")
        showLoading()

        stallsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val stallsList = mutableListOf<Stall>()
                for (stallSnapshot in snapshot.children) {
                    try {
                        val stall = stallSnapshot.getValue(Stall::class.java)
                        if (stall != null) {
                            Log.d("MainCategoryFragment", "Fetched stall: $stall")
                            stallsList.add(stall)
                        } else if (stall == null) {
                            Log.d("MainCategoryFragment", "Stall is null for snapshot: $stallSnapshot")
                        }
                    } catch (e: Exception) {
                        Log.e("MainCategoryFragment", "Failed to parse stall: ${e.message}")
                    }
                }
                Log.d("MainCategoryFragment", "Stalls list size: ${stallsList.size}")
                stallsAdapter.submitList(stallsList)
                hideLoading()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainCategoryFragment", "Database error: ${error.message}")
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                hideLoading()
            }
        })
    }

    private fun fetchBestProductsFromFirebase() {
        productsRef = FirebaseDatabase.getInstance().getReference("Products")
        showLoading()

        productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bestProductsList = mutableListOf<Product>()
                for (productSnapshot in snapshot.children) {
                    try {
                        val product = productSnapshot.getValue(Product::class.java)
                        if (product != null) {
                            Log.d("MainCategoryFragment", "Fetched product: $product")
                            bestProductsList.add(product)
                        } else {
                            Log.d("MainCategoryFragment", "Product is null for snapshot: $productSnapshot")
                        }
                    } catch (e: Exception) {
                        Log.e("MainCategoryFragment", "Failed to parse product: ${e.message}")
                    }
                }
                pagingInfo.isPagingEnd = bestProductsList == pagingInfo.oldBestProducts
                pagingInfo.oldBestProducts = bestProductsList
                if (!pagingInfo.isPagingEnd) {
                    pagingInfo.bestProductsPage++
                }
                bestProductAdapter.submitList(bestProductsList)
                hideLoading()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainCategoryFragment", "Database error: ${error.message}")
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                hideLoading()
            }
        })
    }


    private fun showLoading() {
        binding.mainCategoryProgressbar.visibility = View.VISIBLE
        binding.bestProductsProgressbar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.mainCategoryProgressbar.visibility = View.GONE
        binding.bestProductsProgressbar.visibility = View.GONE
    }

    internal data class PagingInfo(
        var bestProductsPage: Long = 1,
        var oldBestProducts: List<Product> = emptyList(),
        var isPagingEnd: Boolean = false
    )

    override fun onResume() {
        super.onResume()
        showBottomNavigationView()
    }
}
