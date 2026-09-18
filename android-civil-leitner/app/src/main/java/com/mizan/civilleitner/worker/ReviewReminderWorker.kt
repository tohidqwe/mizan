package com.mizan.civilleitner.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mizan.civilleitner.MainActivity
import com.mizan.civilleitner.data.AppDatabase
import com.mizan.civilleitner.domain.Phd140DayPlan
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class ReviewReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val hour = inputData.getInt(KEY_HOUR, 8)
        val minute = inputData.getInt(KEY_MINUTE, 0)
        ReminderScheduler.refreshNow(applicationContext)
        ReminderScheduler.scheduleSlot(applicationContext, hour, minute)
        return Result.success()
    }

    companion object {
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
    }
}

object ReviewNotification {
    const val ID = 1001
    private const val CHANNEL_ID = "exam_review_strict"

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
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
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
        val delay = Duration.between(now, target).toMillis()

        val request = OneTimeWorkRequestBuilder<ReviewReminderWorker>()
            .setInputData(
                Data.Builder()
                    .putInt(ReviewReminderWorker.KEY_HOUR, hour)
                    .putInt(ReviewReminderWorker.KEY_MINUTE, minute)
                    .build()
            )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "phd-140-reminder-%02d%02d".format(hour, minute),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
