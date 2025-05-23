package com.example.myfirstapplication.fragments.loginRegister

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myfirstapplication.R
import com.google.android.material.button.MaterialButton

class AccountFragment: Fragment(
    R.layout.fragment_account
) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val regBtn: AppCompatButton = view.findViewById(R.id.registerBtn)
        val loginButton: AppCompatButton = view.findViewById(R.id.loginBtn)


        regBtn.setOnClickListener{
            findNavController().navigate(R.id.action_accountFragment_to_registerFragment)
        }
        loginButton.setOnClickListener{
            findNavController().navigate(R.id.action_accountFragment_to_loginFragment)
        }

    }

}