package com.mizan.civilleitner

import android.app.Application
import com.mizan.civilleitner.data.AppDatabase
import com.mizan.civilleitner.data.StudyCardImporter
import com.mizan.civilleitner.data.VerifiedArticleImporter
import com.mizan.civilleitner.worker.StudyAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CivilLawApplication : Application() {
    val database by lazy { AppDatabase.get(this) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        CrashGuard.install(this)
        appScope.launch {
            // Non-destructive startup integrity check: missing official rows are repaired, progress is preserved.
            VerifiedArticleImporter.importBundledSeedAndRepair(this@CivilLawApplication, database)
            StudyCardImporter.importBundledCardsIfPresent(this@CivilLawApplication, database)
            StudyAlarmScheduler.rescheduleAll(this@CivilLawApplication)
        }

    }
}
