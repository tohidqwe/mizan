package com.mizan.civilleitner.worker

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mizan.civilleitner.product.ProductMainActivity
import com.mizan.civilleitner.data.AppDatabase
import com.mizan.civilleitner.data.ReminderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ExactReminderScheduler {
    const val EXTRA_REMINDER_ID = "product_reminder_id"
    const val EXTRA_CONTENT_TYPE = "product_content_type"
    const val EXTRA_CONTENT_ID = "product_content_id"

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    fun exactAlarmSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:" + context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun schedule(context: Context, item: ReminderEntity) {
        if (!item.alarmEnabled || item.status != "ACTIVE") return
        val manager = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context, item)
        if (canScheduleExact(context)) {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                item.scheduledAtMillis,
                pending,
            )
        } else {
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                item.scheduledAtMillis,
                pending,
            )
        }
    }

    fun cancel(context: Context, reminderId: String) {
        val intent = Intent(context, ExactReminderReceiver::class.java)
            .putExtra(EXTRA_REMINDER_ID, reminderId)
        val pending = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pending)
        pending.cancel()
    }

    suspend fun rescheduleAll(context: Context) {
        val now = System.currentTimeMillis()
        AppDatabase.get(context).reminderDao().activeSnapshot()
            .filter { it.scheduledAtMillis > now }
            .forEach { schedule(context, it) }
    }

    private fun pendingIntent(context: Context, item: ReminderEntity): PendingIntent {
        val intent = Intent(context, ExactReminderReceiver::class.java)
            .putExtra(EXTRA_REMINDER_ID, item.id)
            .putExtra(EXTRA_CONTENT_TYPE, item.contentType)
            .putExtra(EXTRA_CONTENT_ID, item.contentId)
        return PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

class ExactReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(ExactReminderScheduler.EXTRA_REMINDER_ID) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.get(context.applicationContext)
                val item = db.reminderDao().get(reminderId) ?: return@launch
                db.reminderDao().upsert(
                    item.copy(
                        lastTriggeredAtMillis = System.currentTimeMillis(),
                        reviewCount = item.reviewCount + 1,
                    )
                )
                ProductReminderNotification.show(context.applicationContext, item)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class ProductReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
            )
        ) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ExactReminderScheduler.rescheduleAll(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

object ProductReminderNotification {
    fun show(context: Context, item: ReminderEntity) {
        if (!item.notificationEnabled) return
        if (Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val channelId = "reminder_" + (item.soundUri?.hashCode() ?: 0)
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId,
            "یادآوری‌های شخصی",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "یادآوری مواد، لغات، متون و برنامه‌ریز"
            enableVibration(item.vibrationEnabled)
            if (!item.soundUri.isNullOrBlank()) {
                setSound(
                    Uri.parse(item.soundUri),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            }
        }
        manager.createNotificationChannel(channel)

        val open = Intent(context, ProductMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ExactReminderScheduler.EXTRA_REMINDER_ID, item.id)
            putExtra(ExactReminderScheduler.EXTRA_CONTENT_TYPE, item.contentType)
            putExtra(ExactReminderScheduler.EXTRA_CONTENT_ID, item.contentId)
        }
        val pending = PendingIntent.getActivity(
            context,
            item.id.hashCode(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(item.title)
            .setContentText("زمان مرور/انجام این مورد فرا رسیده است.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("زمان مرور/انجام «" + item.title + "» فرا رسیده است. با لمس اعلان مستقیماً وارد همان مورد شوید.")
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        NotificationManagerCompat.from(context).notify(item.id.hashCode(), notification)
    }
}
