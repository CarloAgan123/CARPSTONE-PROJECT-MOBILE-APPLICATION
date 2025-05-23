package com.example.myfirstapplication.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.myfirstapplication.services.MyFirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.extras != null) {
            val remoteMessage = intent.extras?.get("message") as? RemoteMessage
            remoteMessage?.let {
                Log.d("MyFirebaseReceiver", "Received message: ${it.data}")
                val title = it.notification?.title
                val message = it.notification?.body

                val messagingService = MyFirebaseMessagingService()
                messagingService.sendNotification(title, message)
            }
        }
    }
}
