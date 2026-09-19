package com.mizan.civilleitner.worker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mizan.civilleitner.MainActivity
import com.mizan.civilleitner.data.AppDatabase
import com.mizan.civilleitner.data.PlannerTaskEntity
import com.mizan.civilleitner.data.ReminderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object StudyAlarmScheduler {
    const val EXTRA_KIND = "kind"
    const val EXTRA_ID = "id"
    const val KIND_STUDY = "study"
    const val KIND_PLANNER = "planner"

    fun scheduleStudy(context: Context, reminder: ReminderEntity) {
        schedule(context, KIND_STUDY, reminder.id, reminder.dueAtMillis)
    }

    fun schedulePlanner(context: Context, task: PlannerTaskEntity) {
        schedule(context, KIND_PLANNER, task.id, task.dueAtMillis)
    }

    fun cancel(context: Context, kind: String, id: Long) {
        context.getSystemService(AlarmManager::class.java).cancel(pending(context,kind,id))
    }

    private fun schedule(context: Context, kind: String, id: Long, at: Long) {
        if (at <= System.currentTimeMillis()) return
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context,kind,id)
        if (Build.VERSION.SDK_INT >= 31 && am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi)
        }
    }

    private fun pending(context: Context, kind: String, id: Long): PendingIntent {
        val intent = Intent(context,StudyAlarmReceiver::class.java)
            .putExtra(EXTRA_KIND,kind)
            .putExtra(EXTRA_ID,id)
        val request = (31 * kind.hashCode() + id.hashCode()) and 0x7fffffff
        return PendingIntent.getBroadcast(
            context,request,intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    suspend fun rescheduleAll(context: Context) {
        val db=AppDatabase.get(context)
        val now=System.currentTimeMillis()
        db.reminderDao().activeSnapshot().filter { it.dueAtMillis > now }.forEach { scheduleStudy(context,it) }
        db.plannerDao().activeSnapshot().filter { it.dueAtMillis > now }.forEach { schedulePlanner(context,it) }
    }
}

class StudyAlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind=intent.getStringExtra(StudyAlarmScheduler.EXTRA_KIND) ?: return
        val id=intent.getLongExtra(StudyAlarmScheduler.EXTRA_ID,-1L)
        if(id<=0) return
        val pending=goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db=AppDatabase.get(context)
                when(kind) {
                    StudyAlarmScheduler.KIND_STUDY -> {
                        val item=db.reminderDao().getById(id) ?: return@launch
                        show(context,item.title,item.body,item.soundUri,id)
                        db.reminderDao().update(item.copy(enabled=false,firedCount=item.firedCount+1))
                    }
                    StudyAlarmScheduler.KIND_PLANNER -> {
                        val task=db.plannerDao().getById(id) ?: return@launch
                        if(!task.completed) show(context,task.title,task.details.ifBlank{"موعد برنامه ثبت‌شده شما رسیده است."},task.soundUri,100000L+id)
                    }
                }
            } finally { pending.finish() }
        }
    }

    private fun show(context: Context,title:String,body:String,soundUri:String,id:Long) {
        val manager=context.getSystemService(NotificationManager::class.java)
        val channelId="study_alarm_"+(soundUri.ifBlank{"default"}.hashCode().toUInt().toString())
        if(Build.VERSION.SDK_INT>=26) {
            val channel=NotificationChannel(channelId,"یادآوری مطالعه و برنامه",NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern= longArrayOf(0,450,180,450)
                if(soundUri.isNotBlank()) {
                    setSound(
                        Uri.parse(soundUri),
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                    )
                }
            }
            manager.createNotificationChannel(channel)
        }
        val open=PendingIntent.getActivity(
            context,(id and 0x7fffffff).toInt(),
            Intent(context,MainActivity::class.java).apply {
                flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification=NotificationCompat.Builder(context,channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
        NotificationManagerCompat.from(context).notify((id%2000000000).toInt(),notification)
    }
}

class StudyAlarmBootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action!=Intent.ACTION_BOOT_COMPLETED && intent.action!=Intent.ACTION_MY_PACKAGE_REPLACED) return
        val p=goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { StudyAlarmScheduler.rescheduleAll(context.applicationContext) }
            finally { p.finish() }
        }
    }
}
