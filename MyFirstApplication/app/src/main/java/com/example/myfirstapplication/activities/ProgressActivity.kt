package com.example.myfirstapplication.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.myfirstapplication.R
import com.example.myfirstapplication.uitel.LoadingDialog
import com.google.firebase.auth.FirebaseAuth

class ProgressActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress)
        val loading = LoadingDialog(this)
        loading.startLoading()
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            checkUser()
        }, 3000)

        firebaseAuth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        if (isFirstLaunch()) {
            setFirstLaunchFlag(false)
        }
    }

    private fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean("first_launch", true)
    }

    private fun setFirstLaunchFlag(isFirstLaunch: Boolean) {
        sharedPreferences.edit().putBoolean("first_launch", isFirstLaunch).apply()
    }

    private fun checkUser() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, ShoppingActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
