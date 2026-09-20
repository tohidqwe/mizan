package com.mizan.civilleitner

import android.app.Application
import com.mizan.civilleitner.data.AppDatabase
import com.mizan.civilleitner.data.SecureClientDatabase
import com.mizan.civilleitner.data.StudyCardImporter
import com.mizan.civilleitner.data.VerifiedArticleImporter
import com.mizan.civilleitner.worker.ExactReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CivilLawApplication : Application() {
    val database by lazy { AppDatabase.get(this) }
    val secureClientDatabase by lazy { SecureClientDatabase.get(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        CrashGuard.install(this)
        appScope.launch {
            VerifiedArticleImporter.importBundledSeedAndRepair(this@CivilLawApplication, database)
            StudyCardImporter.importBundledCardsIfPresent(this@CivilLawApplication, database)
            ExactReminderScheduler.rescheduleAll(this@CivilLawApplication)
        }
    }
}
