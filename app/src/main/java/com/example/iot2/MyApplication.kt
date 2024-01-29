package com.example.iot2

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MyApplication : Application() {

    companion object {
        const val CHANNEL_ID = "1"
    }

    override fun onCreate() {
        super.onCreate()

        // Luo ilmoituskanava sovelluksen käynnistyksen yhteydessä
        createNotificationChannel()
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "kanava"
            val descriptionText = "Kanava kulunvalvonnalle"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
