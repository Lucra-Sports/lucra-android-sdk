package com.lucrasports.sdk.app.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lucrasports.sdk.app.MainActivitySdk
import com.lucrasports.sdk.app.R
import com.lucrasports.sdk.ui.push_notifications.LucraPushNotificationService
import kotlin.random.Random

class FCMService : FirebaseMessagingService() {

    companion object {
        //TODO might need more channels per category of notifications
        const val DEFAULT_NOTIFICATION_CHANNEL_NAME = "LucraNotifications"
        const val DEFAULT_NOTIFICATION_CHANNEL_ID = "lucra_gcm_channel"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        LucraPushNotificationService.refreshFirebaseToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        if (!LucraPushNotificationService.handlePushNotification(
                context = this,
                remoteMessage = message,
                activityClass = MainActivitySdk::class.java,
                smallIcon = R.drawable.lucra_letter_landing
            )
        ) {
            showNotification(message)
        }
    }

    private fun showNotification(message: RemoteMessage) {
        //Show a notification
    }
}