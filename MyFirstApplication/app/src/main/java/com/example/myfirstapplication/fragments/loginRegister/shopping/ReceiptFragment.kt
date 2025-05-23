package com.example.myfirstapplication.fragments.loginRegister.shopping

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myfirstapplication.R

class ReceiptFragment : Fragment(R.layout.fragment_receipt) {

    private lateinit var receiptDetails: TextView
    private lateinit var backButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        receiptDetails = view?.findViewById(R.id.receiptDetails) ?: return view
        backButton = view?.findViewById(R.id.backButton) ?: return view

        displayReceiptDetails()

        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        return view
    }

    private fun displayReceiptDetails() {
        val receiptInfo = """
            Receipt for Order
            ----------------------
            Item: Chicken Rice
            Quantity: 1
            Price: $8.50

            Item: Pork Noodle
            Quantity: 2
            Price: $19.00

            ----------------------
            Total Amount: $27.50
            Payment Method: GCash
            Stall Name: Sample Stall
            Stall Number: 123
        """.trimIndent()

        receiptDetails.text = receiptInfo
    }
}
