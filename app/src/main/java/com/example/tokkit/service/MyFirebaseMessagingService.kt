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
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "From: ${remoteMessage.from}")

        // 데이터 메시지 확인
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Message data payload: ${remoteMessage.data}")
            val title = remoteMessage.data["title"] ?: "Tokkit 알림"
            val body = remoteMessage.data["body"] ?: "데이터 메시지가 도착했습니다"
            sendNotification(title, body)
        }

        // 알림 메시지 확인
        remoteMessage.notification?.let {
            val title = it.title ?: "Tokkit 알림"
            val body = it.body ?: "알림 메시지가 도착했습니다"
            sendNotification(title, body)
        }

    }

    private fun sendNotification(title: String, messageBody: String) {
        val channelId = "default_channel_id"
        val notificationId = 1001
        val largeIcon = BitmapFactory.decodeResource(resources, R.drawable.rabbit1)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.rabbit_icon) // 흰색 벡터가 더 권장됨
            .setContentTitle(title)
            .setContentText(messageBody)
            .setLargeIcon(largeIcon) // ✅ 오른쪽 상단에 원형으로 표시됨
            .setAutoCancel(true)
            //.setColor(Color.GRAY)
            .setColor(ContextCompat.getColor(this, R.color.bora200))
            .setContentIntent(pendingIntent)


        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android O 이상은 알림 채널이 필요함
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

        // TODO: 이 토큰을 서버에 전송 (백엔드 저장용)
    }

}
