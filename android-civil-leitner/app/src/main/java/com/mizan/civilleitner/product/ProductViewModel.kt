package com.mizan.civilleitner.product

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mizan.civilleitner.CivilLawApplication
import com.mizan.civilleitner.data.AppSettingEntity
import com.mizan.civilleitner.data.PlannerTaskEntity
import com.mizan.civilleitner.data.ReminderEntity
import com.mizan.civilleitner.data.StudyCardEntity
import com.mizan.civilleitner.domain.PersianDate
import com.mizan.civilleitner.domain.ProductReviewPolicy
import com.mizan.civilleitner.security.AdminProvisioningClient\nimport com.mizan.civilleitner.security.AppSecurityStore
import com.mizan.civilleitner.worker.ExactReminderScheduler
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine\nimport kotlinx.coroutines.flow.flatMapLatest\nimport kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CivilLawApplication
    private val db = app.database
    private val security = AppSecurityStore(app)

    private val nowMillis = MutableStateFlow(System.currentTimeMillis())
    val demoStartedAt = MutableStateFlow<Long?>(null)
    val examEpochDay = MutableStateFlow<Long?>(null)
    val isAdminBound = MutableStateFlow(security.isAdminBound())\n    val adminProvisionStatus = MutableStateFlow(\"\")

    val articles = db.articleDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allArticles = db.articleDao().observeAllIncludingRepealed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cards = db.studyCardDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reminders = db.reminderDao().observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val plannerTasks = db.plannerDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val entitlements = db.entitlementDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val inbox = db.inboxDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dueReminderCount = combine(reminders, nowMillis) { items, now ->
        items.count { it.scheduledAtMillis <= now && it.status == "ACTIVE" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val demoActive = combine(demoStartedAt, nowMillis) { start, now ->
        start != null && now < start + DEMO_DURATION_MS
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val phdAccess = combine(isAdminBound, demoActive, entitlements, nowMillis) { admin, demo, grants, now ->
        admin || demo || grants.any {
            it.scope == CourseCatalog.PHD_PRIVATE_LAW &&
                it.enabled &&
                it.startsAtMillis <= now &&
                (it.endsAtMillis == null || it.endsAtMillis > now)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val countdownDays = combine(examEpochDay, nowMillis) { exam, now ->
        if (exam == null) null
        else exam - java.time.Instant.ofEpochMilli(now)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toEpochDay()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            demoStartedAt.value = db.settingsDao().get(KEY_DEMO_START)?.toLongOrNull()
            examEpochDay.value = db.settingsDao().get(KEY_EXAM_EPOCH_DAY)?.toLongOrNull()
        }
        viewModelScope.launch {
            while (isActive) {
                nowMillis.value = System.currentTimeMillis()
                delay(60_000)
            }
        }
    }

    fun refreshAdminBinding() {
        isAdminBound.value = security.isAdminBound()
    }

    fun provisionAdminDevice(baseUrl: String, activationCode: String) {
        if (baseUrl.isBlank() || activationCode.isBlank()) {
            adminProvisionStatus.value = "آدرس سرور و کد فعال‌سازی لازم است."
            return
        }
        adminProvisionStatus.value = "در حال ثبت امن این دستگاه…"
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                AdminProvisioningClient.enroll(baseUrl.trim(), activationCode.trim())
            }.onSuccess { result ->
                security.adminApiBaseUrl = baseUrl.trim()
                security.adminDeviceId = result.deviceId
                isAdminBound.value = true
                adminProvisionStatus.value = "دستگاه مدیر با موفقیت ثبت شد."
            }.onFailure { error ->
                adminProvisionStatus.value = error.message ?: "ثبت دستگاه ناموفق بود."
            }
        }
    }

    fun activate72HourDemo() {
        if (demoStartedAt.value != null) return
        val start = System.currentTimeMillis()
        demoStartedAt.value = start
        viewModelScope.launch {
            db.settingsDao().upsert(AppSettingEntity(KEY_DEMO_START, start.toString()))
        }
    }

    fun demoRemainingMillis(): Long {
        val start = demoStartedAt.value ?: return 0L
        return (start + DEMO_DURATION_MS - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun setExamPersianDate(input: String): Boolean {
        val gregorian = PersianDate.parseToGregorian(input) ?: return false
        val epoch = gregorian.toEpochDay()
        examEpochDay.value = epoch
        viewModelScope.launch {
            db.settingsDao().upsert(AppSettingEntity(KEY_EXAM_EPOCH_DAY, epoch.toString()))
        }
        return true
    }

    fun addPlannerTask(
        title: String,
        description: String,
        persianDate: String,
        time: String,
        reminderMinutesBefore: Int = 30,
        recurrence: String = "NONE",
        soundUri: String? = null,
    ): Boolean {
        if (title.isBlank()) return false
        val date = PersianDate.parseToGregorian(persianDate) ?: return false
        val localTime = runCatching { LocalTime.parse(time) }.getOrNull() ?: return false
        val scheduledAt = date.atTime(localTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val remindAt = scheduledAt - reminderMinutesBefore.coerceAtLeast(0) * 60_000L
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        viewModelScope.launch {
            db.plannerDao().upsert(
                PlannerTaskEntity(
                    id = id,
                    title = title.trim(),
                    description = description.trim(),
                    scheduledAtMillis = scheduledAt,
                    remindAtMillis = remindAt,
                    soundUri = soundUri,
                    recurrence = recurrence,
                    createdAtMillis = now,
                    updatedAtMillis = now,
                )
            )
            val reminder = ReminderEntity(
                id = "PLANNER:" + id,
                contentType = "PLANNER_TASK",
                contentId = id,
                title = title.trim(),
                scheduledAtMillis = remindAt,
                intervalCode = "CUSTOM",
                createdAtMillis = now,
                soundUri = soundUri,
            )
            db.reminderDao().upsert(reminder)
            ExactReminderScheduler.schedule(app, reminder)
        }
        return true
    }

    fun togglePlannerTask(task: PlannerTaskEntity) {
        viewModelScope.launch {
            db.plannerDao().upsert(
                task.copy(
                    completed = !task.completed,
                    updatedAtMillis = System.currentTimeMillis(),
                )
            )
        }
    }

    fun deletePlannerTask(task: PlannerTaskEntity) {
        viewModelScope.launch {
            db.plannerDao().delete(task.id)
            val reminderId = "PLANNER:" + task.id
            db.reminderDao().delete(reminderId)
            ExactReminderScheduler.cancel(app, reminderId)
        }
    }

    fun scheduleManualReminder(contentType: String, contentId: String, title: String, intervalCode: String) {
        val now = System.currentTimeMillis()
        val due = ProductReviewPolicy.dueForInterval(now, intervalCode)
        val id = contentType + ":" + contentId
        viewModelScope.launch {
            val item = ReminderEntity(
                id = id,
                contentType = contentType,
                contentId = contentId,
                title = title,
                scheduledAtMillis = due,
                intervalCode = intervalCode,
                createdAtMillis = now,
            )
            db.reminderDao().upsert(item)
            ExactReminderScheduler.schedule(app, item)
        }
    }

    fun gradeVocabulary(card: StudyCardEntity, correct: Boolean, language: String) {
        val now = System.currentTimeMillis()
        val due = when (language) {
            "ARABIC" -> if (correct) ProductReviewPolicy.arabicAfterCorrect(now) else ProductReviewPolicy.arabicAfterWrong(now)
            else -> if (correct) ProductReviewPolicy.englishAfterCorrect(now) else ProductReviewPolicy.englishAfterWrong(now)
        }
        val item = ReminderEntity(
            id = language + ":" + card.id,
            contentType = language + "_WORD",
            contentId = card.id,
            title = card.title,
            scheduledAtMillis = due,
            intervalCode = if (correct) "D3" else "H24",
            createdAtMillis = now,
        )
        viewModelScope.launch {
            db.reminderDao().upsert(item)
            ExactReminderScheduler.schedule(app, item)
        }
    }

    fun todayPersianLabel(): String = PersianDate.fromGregorian(LocalDate.now()).label()
    fun todayPersianNumeric(): String = PersianDate.fromGregorian(LocalDate.now()).numeric()

    companion object {
        const val DEMO_DURATION_MS = 72L * 60L * 60L * 1000L
        const val KEY_DEMO_START = "phd_demo_started_at"
        const val KEY_EXAM_EPOCH_DAY = "exam_epoch_day"
    }
}
