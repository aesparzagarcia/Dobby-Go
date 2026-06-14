package com.ares.ewe_man.realtime

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ares.ewe_man.MainActivity
import com.ares.ewe_man.R

object DeliveryOrderNotificationHelper {
    const val EXTRA_ORDER_ID = "order_id"
    const val EXTRA_PUSH_TYPE = "type"
    const val PUSH_TYPE_DELIVERY_ORDER = "delivery_order_available"

    private const val CHANNEL_ID = "dobbygo_delivery_orders"
    private const val CHANNEL_NAME = "Pedidos disponibles"
    private const val ORDER_NOTIFICATION_ID = 1001
    private const val ORDER_TAG_PREFIX = "order-"

    fun orderNotificationTag(orderId: String): String =
        "$ORDER_TAG_PREFIX${orderId.trim()}".take(64)

    fun clearOrderNotifications(context: Context, orderId: String) {
        val trimmed = orderId.trim()
        if (trimmed.isEmpty()) return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val tag = orderNotificationTag(trimmed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.activeNotifications
                .filter { it.tag == tag }
                .forEach { status ->
                    notificationManager.cancel(status.tag, status.id)
                }
        }
        notificationManager.cancel(tag, ORDER_NOTIFICATION_ID)
    }

    fun clearAllOrderNotifications(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.activeNotifications
            .filter { it.tag?.startsWith(ORDER_TAG_PREFIX) == true }
            .forEach { status ->
                notificationManager.cancel(status.tag, status.id)
            }
    }

    fun show(context: Context, title: String, body: String, orderId: String?) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            )
            notificationManager.createNotificationChannel(channel)
        }

        val trimmedOrderId = orderId?.trim()?.takeIf { it.isNotEmpty() }
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PUSH_TYPE, PUSH_TYPE_DELIVERY_ORDER)
            trimmedOrderId?.let { putExtra(EXTRA_ORDER_ID, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            trimmedOrderId?.hashCode() ?: PUSH_TYPE_DELIVERY_ORDER.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setColor(ContextCompat.getColor(context, R.color.dobby_notification_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (trimmedOrderId != null) {
            notificationManager.notify(
                orderNotificationTag(trimmedOrderId),
                ORDER_NOTIFICATION_ID,
                notification,
            )
        } else {
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}
