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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mizan.civilleitner.MainActivity
import com.mizan.civilleitner.data.AppDatabase
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class ReviewReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        ReminderScheduler.refreshNow(applicationContext)
        ReminderScheduler.scheduleNext(applicationContext, 8, 0)
        return Result.success()
    }
}

object ReviewNotification {
    const val ID = 1001
    private const val CHANNEL_ID = "exam_review_strict"

    fun show(context: Context, due: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "مرور اجباری آزمون دکتری",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "تا وقتی مرورهای سررسیدشده تمام نشده‌اند، یادآوری فعال می‌ماند."
                setShowBadge(true)
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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("مرور امروز تمام نشده")
            .setContentText("$due مورد سررسیدشده باقی مانده؛ اول مرور، بعد محتوای جدید.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "$due مورد سررسیدشده باقی مانده است. تا صفر شدن صف مرور، برنامه محتوای جدید را قفل نگه می‌دارد."
            ))
            .setContentIntent(pending)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        NotificationManagerCompat.from(context).notify(ID, notification)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID)
    }
}

object ReminderScheduler {
    private const val UNIQUE_WORK = "phd-140-daily-review"

    suspend fun refreshNow(context: Context) {
        val db = AppDatabase.get(context)
        val today = LocalDate.now().toEpochDay()
        val due = db.articleDao().dueCount(today) + db.studyCardDao().dueCount(today)
        if (due > 0) ReviewNotification.show(context, due) else ReviewNotification.cancel(context)
    }

    fun scheduleNext(context: Context, hour: Int, minute: Int) {
        val now = ZonedDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        val delay = Duration.between(now, target).toMillis()

        val request = OneTimeWorkRequestBuilder<ReviewReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
