package com.lucrasports.sdk.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.lucrasports.sdk.app.MainActivitySdk
import com.lucrasports.sdk.app.R
import com.lucrasports.sdk.ui.push_notifications.DeeplinkConstants.NOTIFICATION_DEEPLINK
import com.lucrasports.sdk.ui.push_notifications.DeeplinkConstants.NOTIFICATION_MESSAGE
import com.lucrasports.sdk.ui.push_notifications.DeeplinkConstants.NOTIFICATION_TITLE
import kotlin.random.Random

/**
 * Must match [com.lucrasports.sdk.ui.push_notifications.LucraPushNotificationService]'s channel id
 * so debug posts land in the same channel as real Lucra pushes.
 */
private const val DEFAULT_NOTIFICATION_CHANNEL_NAME = "LucraNotifications"
private const val DEFAULT_NOTIFICATION_CHANNEL_ID = "lucra_gcm_channel"

/** Matches iOS DebugPushNotificationView — banner fires after this delay. */
private const val DEBUG_NOTIFICATION_DELAY_MS = 2_000L

data class DebugPushNotification(
    val label: String,
    val title: String,
    val body: String,
    val deeplink: String,
)

val DEBUG_PUSH_NOTIFICATIONS: List<DebugPushNotification> = listOf(
    DebugPushNotification(
        label = "Tournament Details",
        title = "New Tournament!",
        body = "Don't miss it!",
        deeplink = "https://www.lucrasports.com/TournamentDetailView?matchupId=c6e8e752-7e29-4305-9519-8dbe6827e41f",
    ),
    DebugPushNotification(
        label = "Achievement Earned",
        title = "Achievement Earned!",
        body = "Tap to see your reward",
        deeplink = "https://www.lucrasports.com/Rewards?achievementId=1f7c1f4e-5a1f-4a3c-9d5e-1c2a3b4c5d6e",
    ),
    DebugPushNotification(
        label = "Funds (no achievementId)",
        title = "Deposit Complete",
        body = "Your funds are available",
        deeplink = "https://www.lucrasports.com/Rewards",
    ),
)

/**
 * Used by the "Push Debug" sample-app option to exercise the notification -> tap -> deeplink path
 * in-app, the same way a real foreground push would (see [FCMService]).
 */
fun Context.scheduleDebugNotification(notification: DebugPushNotification) {
    Toast.makeText(
        this,
        "${notification.label} queued — fires in 2s",
        Toast.LENGTH_SHORT,
    ).show()

    Handler(Looper.getMainLooper()).postDelayed({
        postDebugNotification(
            title = notification.title,
            body = notification.body,
            deeplink = notification.deeplink,
        )
    }, DEBUG_NOTIFICATION_DELAY_MS)
}

fun Context.postDebugNotification(
    title: String,
    body: String,
    deeplink: String,
) {
    ensureDebugNotificationChannel()

    val intent = Intent(this, MainActivitySdk::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        putExtra(NOTIFICATION_TITLE, title)
        putExtra(NOTIFICATION_MESSAGE, body)
        putExtra(NOTIFICATION_DEEPLINK, deeplink)
        // A unique action keeps the extras from being dropped across identical PendingIntents.
        // https://stackoverflow.com/a/3128271
        action = Random.nextInt().toString() + "_action"
    }

    val pendingIntent = PendingIntent.getActivity(
        this,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val built = NotificationCompat.Builder(this, DEFAULT_NOTIFICATION_CHANNEL_ID)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_lucra_launcher_round))
        .setSmallIcon(R.drawable.lucra_letter_landing)
        .setContentIntent(pendingIntent)
        .build()

    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .notify(Random.nextInt(), built)
}

private fun Context.ensureDebugNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = getSystemService(NotificationManager::class.java) ?: return
    if (manager.getNotificationChannel(DEFAULT_NOTIFICATION_CHANNEL_ID) != null) return
    manager.createNotificationChannel(
        NotificationChannel(
            DEFAULT_NOTIFICATION_CHANNEL_ID,
            DEFAULT_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "All Lucra Notifications"
        }
    )
}
