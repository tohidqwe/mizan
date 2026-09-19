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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mizan.civilleitner.MainActivity
import com.mizan.civilleitner.data.AppDatabase
import com.mizan.civilleitner.data.PlannerTaskEntity
import com.mizan.civilleitner.data.ReminderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class LearningAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(EXTRA_KIND) ?: return
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val sound = intent.getStringExtra(EXTRA_SOUND).orEmpty()

        LearningNotifications.show(context, kind, id, title, body, sound)

        if (kind == KIND_REVIEW && id > 0) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    AppDatabase.get(context).reminderDao().markFired(id, System.currentTimeMillis())
                } finally {
                    pending.finish()
                }
            }
        }
    }

    companion object {
        const val KIND_REVIEW = "review"
        const val KIND_PLANNER = "planner"
        const val EXTRA_KIND = "kind"
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_SOUND = "sound"
    }
}

class LearningBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                LearningAlarmScheduler.rescheduleEverything(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}

object LearningNotifications {
    private const val GROUP = "dr_tohid_learning"

    fun show(context: Context, kind: String, id: Long, title: String, body: String, soundUri: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channelId = channelId(kind, soundUri)
        val uri = soundUri.takeIf { it.isNotBlank() }?.let(Uri::parse)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                if (kind == LearningAlarmReceiver.KIND_PLANNER) "برنامه‌ریزی شخصی" else "مرور مطالب",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "یادآوری‌های دوره آموزشی دکتر توحید نجفیان"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 450, 180, 450)
                if (uri != null) {
                    val attrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setSound(uri, attrs)
                }
            }
            manager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("openKind", kind)
            putExtra("openId", id)
        }
        val pending = PendingIntent.getActivity(
            context,
            (kind.hashCode() * 31 + id.hashCode()).absoluteValue,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setGroup(GROUP)
            .build()

        NotificationManagerCompat.from(context).notify((id % Int.MAX_VALUE).toInt().coerceAtLeast(1), notification)
    }

    private fun channelId(kind: String, sound: String): String =
        "dr_tohid_${kind}_${sound.hashCode().absoluteValue}"
}

object LearningAlarmScheduler {
    suspend fun addReview(
        context: Context,
        targetType: String,
        targetId: String,
        title: String,
        preview: String,
        intervalHours: Int,
        soundUri: String,
    ): Long {
        val due = System.currentTimeMillis() + intervalHours * 60L * 60L * 1000L
        val dao = AppDatabase.get(context).reminderDao()
        val id = dao.upsert(
            ReminderEntity(
                targetType = targetType,
                targetId = targetId,
                title = title,
                preview = preview.take(300),
                dueAtMillis = due,
                intervalHours = intervalHours,
                soundUri = soundUri,
            )
        )
        scheduleReview(context, dao.getById(id)!!)
        return id
    }

    suspend fun schedulePlanner(context: Context, item: PlannerTaskEntity) {
        if (!item.alarmEnabled || item.completed || item.dueAtMillis <= System.currentTimeMillis()) return
        schedule(
            context,
            requestCode = plannerCode(item.id),
            triggerAt = item.dueAtMillis,
            kind = LearningAlarmReceiver.KIND_PLANNER,
            id = item.id,
            title = "برنامه: ${item.title}",
            body = item.details.ifBlank { "${item.persianDate} • %02d:%02d".format(item.hour, item.minute) },
            soundUri = item.soundUri,
        )
    }

    suspend fun rescheduleEverything(context: Context) {
        val db = AppDatabase.get(context)
        db.reminderDao().activeSnapshot()
            .filter { it.dueAtMillis > System.currentTimeMillis() }
            .forEach { scheduleReview(context, it) }
        db.plannerTaskDao().activeFutureSnapshot(System.currentTimeMillis())
            .forEach { schedulePlanner(context, it) }
    }

    fun cancelPlanner(context: Context, id: Long) = cancel(context, plannerCode(id))
    fun cancelReview(context: Context, id: Long) = cancel(context, reviewCode(id))

    private fun scheduleReview(context: Context, item: ReminderEntity) {
        schedule(
            context = context,
            requestCode = reviewCode(item.id),
            triggerAt = item.dueAtMillis,
            kind = LearningAlarmReceiver.KIND_REVIEW,
            id = item.id,
            title = "زمان مرور: ${item.title}",
            body = item.preview,
            soundUri = item.soundUri,
        )
    }

    private fun schedule(
        context: Context,
        requestCode: Int,
        triggerAt: Long,
        kind: String,
        id: Long,
        title: String,
        body: String,
        soundUri: String,
    ) {
        val intent = Intent(context, LearningAlarmReceiver::class.java).apply {
            putExtra(LearningAlarmReceiver.EXTRA_KIND, kind)
            putExtra(LearningAlarmReceiver.EXTRA_ID, id)
            putExtra(LearningAlarmReceiver.EXTRA_TITLE, title)
            putExtra(LearningAlarmReceiver.EXTRA_BODY, body)
            putExtra(LearningAlarmReceiver.EXTRA_SOUND, soundUri)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val am = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun cancel(context: Context, requestCode: Int) {
        val intent = Intent(context, LearningAlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pending)
        pending.cancel()
    }

    private fun reviewCode(id: Long): Int = (100_000 + id % 700_000).toInt()
    private fun plannerCode(id: Long): Int = (900_000 + id % 700_000).toInt()
}
