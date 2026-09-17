package com.mizan.civilleitner

import android.app.Application
import com.mizan.civilleitner.data.AppDatabase
import com.mizan.civilleitner.data.VerifiedArticleImporter
import com.mizan.civilleitner.worker.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CivilLawApplication : Application() {
    val database by lazy { AppDatabase.get(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            VerifiedArticleImporter.importBundledSeedIfEmpty(this@CivilLawApplication, database)
        }
        ReminderScheduler.scheduleNext(this, 21, 0)
    }
}
