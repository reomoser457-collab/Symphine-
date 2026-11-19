package com.symphine.gamespace.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_OPERATIONS = "symphine_operations"
        const val CHANNEL_THERMAL = "symphine_thermal"
        const val CHANNEL_OVERLAY = "symphine_overlay"
    }

    fun ensureChannels() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channels = listOf(
            NotificationChannel(
                CHANNEL_OPERATIONS,
                "Profile & System Operations",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Profile applied, restored, and Shizuku operation updates"
            },
            NotificationChannel(
                CHANNEL_THERMAL,
                "Thermal & Compatibility Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thermal throttling warnings and compatibility alerts"
            },
            NotificationChannel(
                CHANNEL_OVERLAY,
                "Gaming Overlay & Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active floating gaming overlay service notification"
            }
        )
        channels.forEach { nm.createNotificationChannel(it) }
    }

    fun postOperationNotification(title: String, body: String, notificationId: Int = 2001) {
        if (!canPost()) return
        ensureChannels()
        val notif = NotificationCompat.Builder(context, CHANNEL_OPERATIONS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notif)
    }

    fun postThermalWarning(body: String) {
        if (!canPost()) return
        ensureChannels()
        val notif = NotificationCompat.Builder(context, CHANNEL_THERMAL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Thermal Warning — SYMPHINE")
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(2002, notif)
    }

    private fun canPost(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}
