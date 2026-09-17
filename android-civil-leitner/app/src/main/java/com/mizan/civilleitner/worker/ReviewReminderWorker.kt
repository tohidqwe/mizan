package com.mizan.civilleitner.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
import com.mizan.civilleitner.data.AppDatabase
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class ReviewReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val due = AppDatabase.get(applicationContext)
            .articleDao()
            .dueCount(java.time.LocalDate.now().toEpochDay())

        if (due > 0) notifyDue(applicationContext, due)
        ReminderScheduler.scheduleNext(applicationContext, 21, 0)
        return Result.success()
    }

    private fun notifyDue(context: Context, due: Int) {
        val channelId = "civil_review"
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                "مرور قانون مدنی",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )

        if (Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("مرور قانون مدنی آماده است")
            .setContentText("$due ماده برای مرور شما باقی مانده است")
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1001, notification)
    }
}

object ReminderScheduler {
    private const val UNIQUE_WORK = "civil-law-daily-review"

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
