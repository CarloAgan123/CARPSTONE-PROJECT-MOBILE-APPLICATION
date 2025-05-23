package com.example.myfirstapplication.fragments.loginRegister

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myfirstapplication.R

class IntroductionFragment: Fragment(
    R.layout.fragment_introduction
) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val startBtn: AppCompatButton = view.findViewById(R.id.btnStart)

        startBtn.setOnClickListener{
            val sharedPreferences = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().putBoolean("hasStarted", true).apply()

            findNavController().navigate(R.id.action_introductionFragment_to_accountFragment)
        }
    }
}
