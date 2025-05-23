package com.example.myfirstapplication.uitel

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.example.myfirstapplication.R

class ProgressDialog(val mActivity: Activity) {
    private lateinit var isDialog: AlertDialog
    private lateinit var progressBar: ProgressBar
    private lateinit var statusIcon: ImageView
    private lateinit var statusMessage: TextView
    private lateinit var iStatusIcon: ImageView

    @SuppressLint("MissingInflatedId")
    fun loadingStart() {
        val inflater = mActivity.layoutInflater
        val viewDialog = inflater.inflate(R.layout.loading_progress_item, null)

        progressBar = viewDialog.findViewById(R.id.progressBar)
        statusIcon = viewDialog.findViewById(R.id.statusIcon)
        statusMessage = viewDialog.findViewById(R.id.statusMessage)
        iStatusIcon = viewDialog.findViewById(R.id.iStatusIcon)
        statusIcon.visibility = View.INVISIBLE

        val builder = AlertDialog.Builder(mActivity)
        builder.setView(viewDialog)
        builder.setCancelable(false)
        isDialog = builder.create()
        isDialog.show()
    }

    fun loadingDismiss() {
        Handler(Looper.getMainLooper()).postDelayed({
            isDialog.dismiss()
        }, 2000)
    }

    fun showSuccess(message: String) {
        iStatusIcon.visibility = View.INVISIBLE
        progressBar.visibility = View.INVISIBLE
        statusIcon.visibility = View.VISIBLE
        statusIcon.setImageResource(R.drawable.icon_success)
        statusMessage.text = message
    }

    fun showFailure(message: String) {
        iStatusIcon.visibility = View.INVISIBLE
        statusIcon.visibility = View.VISIBLE
        progressBar.visibility = View.INVISIBLE
        statusIcon.setImageResource(R.drawable.icon_failed)
        statusMessage.text = message
    }


}
