package com.example.tokkit.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.tokkit.MainActivity // 알림 클릭 시 열 앱 액티비티
import com.example.tokkit.R
import com.example.tokkit.ReviewTypeActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "From: ${remoteMessage.from}")

        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "Tokkit 알림"
        val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: "알림 메시지가 도착했습니다"
        val noteId = remoteMessage.data["note_id"] // ✅ UUID 형태 문자열

        sendNotification(title, body, noteId)
    }

    private fun sendNotification(title: String, messageBody: String, noteId: String?) {
        val channelId = "default_channel_id"
        val notificationId = 1001
        val largeIcon = BitmapFactory.decodeResource(resources, R.drawable.rabbit1)

        val intent = Intent(this, ReviewTypeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("NOTE_ID", noteId) // ✅ noteId 전달
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.rabbit_icon)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setLargeIcon(largeIcon)
            .setAutoCancel(true)
            //.setColor(Color.GRAY) // 디폴트는 그레이.
            .setColor(ContextCompat.getColor(this, R.color.bora200))
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "기본 채널",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "🔥 새 FCM 토큰: $token")
        // TODO: 백엔드로 전송
    }
}

