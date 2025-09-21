@file:Suppress("DEPRECATION")

package com.example.managedefaulto_dile

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MyForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "notification1"
        const val NOTIFICATION_ID = 1
        const val EXTRA_MESSAGE = "FOREGROUND_MESSAGE"
        const val ACTION_STOP_SERVICE = "STOP_SERVICE"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == ACTION_STOP_SERVICE) {
            stopForegroundService()
            return START_NOT_STICKY
        }

        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: "Running"

        val stopIntent = Intent(this, MyForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Service running")
            .setContentText(message)
            .setSmallIcon(R.drawable.foreground_icon)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.notification_icon))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(
                R.drawable.foreground_icon, // button icon
                "Stop",                      // button text
                stopPendingIntent
            )
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Mark service as active
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("serviceStatus", true)
            .apply()

        return START_STICKY
    }

    private fun stopForegroundService() {
        // Update SharedPreferences
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("serviceStatus", false)
            .apply()

        stopForeground(true)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
