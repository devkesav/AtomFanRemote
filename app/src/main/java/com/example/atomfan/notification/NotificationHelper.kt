package com.example.atomfan.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.atomfan.MainActivity
import com.example.atomfan.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_AUTOMATION = "atomfan_automation_channel"
        const val CHANNEL_CONNECTION = "atomfan_connection_channel"
        const val CHANNEL_TIMER = "atomfan_timer_channel"

        const val NOTIF_ID_AUTOMATION = 1001
        const val NOTIF_ID_CONNECTION = 1002
        const val NOTIF_ID_TIMER = 1003
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val autoChannel = NotificationChannel(
                CHANNEL_AUTOMATION,
                "Automation Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when scheduled fan automation rules trigger"
            }

            val connChannel = NotificationChannel(
                CHANNEL_CONNECTION,
                "Connection Alerts",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for BLE & Bridge connection events"
            }

            val timerChannel = NotificationChannel(
                CHANNEL_TIMER,
                "Timer & Sleep Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for fan sleep mode and timer auto-shutdown"
            }

            manager.createNotificationChannel(autoChannel)
            manager.createNotificationChannel(connChannel)
            manager.createNotificationChannel(timerChannel)
        }
    }

    @SuppressLint("MissingPermission")
    fun showAutomationNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_AUTOMATION)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_AUTOMATION, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted on Android 13+
        }
    }

    @SuppressLint("MissingPermission")
    fun showConnectionNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_CONNECTION)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_CONNECTION, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    @SuppressLint("MissingPermission")
    fun showTimerNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_TIMER, builder.build())
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}
