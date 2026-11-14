package com.example.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

object BattleBoxNotificationHelper {
    const val CHANNEL_ANNOUNCEMENTS = "battlebox_announcements"
    const val CHANNEL_BILLING = "battlebox_billing_reminders"
    const val CHANNEL_SELF_CHECKIN = "battlebox_self_checkin"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val announcementsChannel = NotificationChannel(
                CHANNEL_ANNOUNCEMENTS,
                "Gym Announcements & Hours",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Open, Closed, Special Hours, and Holiday notices from Battle Box Admin."
            }
            val billingChannel = NotificationChannel(
                CHANNEL_BILLING,
                "Billing & Pre-Debit Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "UPI AutoPay 24h pre-debit notices, plan expiry alerts, and payment updates."
            }
            val selfCheckInChannel = NotificationChannel(
                CHANNEL_SELF_CHECKIN,
                "Wall QR Self Check-In Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Instant alerts for Trainers and Admin when members self check-in via Battle Box Wall QR."
            }
            manager.createNotificationChannel(announcementsChannel)
            manager.createNotificationChannel(billingChannel)
            manager.createNotificationChannel(selfCheckInChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun showAnnouncementNotification(
        context: Context,
        idSeed: String,
        title: String,
        body: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ANNOUNCEMENTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(idSeed.hashCode(), notification)
    }

    fun showBillingReminderNotification(
        context: Context,
        idSeed: String,
        title: String,
        body: String
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_BILLING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(idSeed.hashCode(), notification)
    }

    fun showSelfCheckInAlertNotification(
        context: Context,
        attendanceId: String,
        memberName: String,
        distanceMeters: Double
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)
        val body = "$memberName checked in via Wall QR (${distanceMeters.toInt()}m GPS geofence verified)."
        val notification = NotificationCompat.Builder(context, CHANNEL_SELF_CHECKIN)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Battle Box Wall QR Check-In")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(("checkin_$attendanceId").hashCode(), notification)
    }
}
