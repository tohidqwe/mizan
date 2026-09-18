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
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mizan.civilleitner.MainActivity
import com.mizan.civilleitner.data.AppDatabase
import com.mizan.civilleitner.domain.Phd140DayPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val hour = intent.getIntExtra(EXTRA_HOUR, 8)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.refreshNow(context.applicationContext)
                ReminderScheduler.scheduleSlot(context.applicationContext, hour, minute)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            ReminderScheduler.scheduleAll(context.applicationContext)
        }
    }
}

object ReviewNotification {
    const val ID = 1001
    private const val CHANNEL_ID = "exam_review_strict_v2"

    fun show(context: Context, due: Int, effectiveDay: Int, dayIncomplete: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "یادآوری اجباری آزمون دکتری",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "مرورهای سررسیدشده و مأموریت روز تا اتمام کامل یادآوری می‌شوند."
                setShowBadge(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 450, 220, 450)
            }
        )

        if (Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = when {
            due > 0 -> "مرور اجباری عقب افتاده است"
            dayIncomplete -> "روز $effectiveDay هنوز تمام نشده"
            else -> "برنامه امروز"
        }
        val body = when {
            due > 0 -> "$due مرور سررسیدشده باقی مانده؛ محتوای جدید قفل است."
            dayIncomplete -> "ماموریت‌های روز $effectiveDay را کامل کن؛ برنامه تا اتمام روز جلو نمی‌رود."
            else -> "برنامه امروز کامل است."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setOngoing(due > 0 || dayIncomplete)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 450, 220, 450))
            .build()

        NotificationManagerCompat.from(context).notify(ID, notification)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID)
    }
}

object ReminderScheduler {
    private val slots = listOf(8 to 0, 14 to 0, 20 to 0)

    suspend fun refreshNow(context: Context) {
        val db = AppDatabase.get(context)
        val today = LocalDate.now().toEpochDay()
        val due = db.articleDao().dueCount(today) + db.studyCardDao().dueCount(today)

        val calendarDay = Phd140DayPlan.calendarDay()
        val completed = db.planDao().completedDaysSnapshot().toSet()
        val effectiveDay = (1..calendarDay).firstOrNull { it !in completed } ?: calendarDay
        val dayIncomplete = db.planDao().getDay(effectiveDay)?.dayCompleted != true

        if (due > 0 || dayIncomplete) {
            ReviewNotification.show(context, due, effectiveDay, dayIncomplete)
        } else {
            ReviewNotification.cancel(context)
        }
    }

    fun scheduleAll(context: Context) {
        slots.forEach { (hour, minute) -> scheduleSlot(context, hour, minute) }
    }

    fun scheduleNext(context: Context, hour: Int, minute: Int) = scheduleSlot(context, hour, minute)

    fun scheduleSlot(context: Context, hour: Int, minute: Int) {
        val now = ZonedDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)

        val alarmIntent = Intent(context, ReminderAlarmReceiver::class.java)
            .putExtra(ReminderAlarmReceiver.EXTRA_HOUR, hour)
            .putExtra(ReminderAlarmReceiver.EXTRA_MINUTE, minute)
        val requestCode = hour * 100 + minute
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            target.toInstant().toEpochMilli(),
            pending,
        )
    }
}
