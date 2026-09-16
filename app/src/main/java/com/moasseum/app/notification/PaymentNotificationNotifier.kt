package com.moasseum.app.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.moasseum.app.MainActivity
import com.moasseum.app.R
import com.moasseum.app.domain.NotificationCandidate
import java.text.NumberFormat
import java.util.Locale

const val EXTRA_NOTIFICATION_CANDIDATE_ID = "com.moasseum.app.extra.NOTIFICATION_CANDIDATE_ID"

object PaymentNotificationNotifier {
    const val CHANNEL_ID = "payment_detection"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "결제 알림 감지",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "인식한 결제·입금 알림을 확인하고 가계부에 추가할 수 있어요."
            enableVibration(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun areAppNotificationsEnabled(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL_ID)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return true
    }

    @SuppressLint("MissingPermission")
    fun showCandidate(context: Context, candidate: NotificationCandidate) {
        if (!areAppNotificationsEnabled(context)) return
        createChannel(context)
        val notificationId = notificationId(candidate.id)
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.moasseum.app.action.REVIEW_NOTIFICATION_CANDIDATE"
            putExtra(EXTRA_NOTIFICATION_CANDIDATE_ID, candidate.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val direction = if (candidate.type.name == "INCOME") "입금" else "지출"
        val amount = NumberFormat.getNumberInstance(Locale.KOREA).format(candidate.amount) + "원"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("알림에서 거래를 인식했어요")
            .setContentText("${candidate.merchant} · $amount")
            .setSubText("$direction · 가계부에 추가할까요?")
            .setStyle(NotificationCompat.BigTextStyle().bigText("인식되었습니다. 추가할까요?\n${candidate.merchant} · $amount"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(notificationId, notification) }
    }

    fun cancel(context: Context, candidateId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(candidateId))
    }

    private fun notificationId(id: Long): Int = (id xor (id ushr 32)).toInt()
}
