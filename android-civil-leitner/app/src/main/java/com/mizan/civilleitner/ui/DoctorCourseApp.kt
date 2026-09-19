package com.mizan.civilleitner.ui

import android.Manifest
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mizan.civilleitner.CivilLawApplication
import com.mizan.civilleitner.data.ArticleEntity
import com.mizan.civilleitner.data.PlannerTaskEntity
import com.mizan.civilleitner.data.ReminderEntity
import com.mizan.civilleitner.data.StudyCardEntity
import com.mizan.civilleitner.domain.PersianDate
import com.mizan.civilleitner.domain.Phd140DayPlan
import com.mizan.civilleitner.worker.LearningAlarmScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

private val FocusBlue = Color(0xFF1F5FA8)
private val MemoryGreen = Color(0xFF1B7F5A)
private val EnergyAmber = Color(0xFFD88B1F)
private val InkBlack = Color(0xFF111827)
private val QuietGray = Color(0xFF5D6876)
private val Canvas = Color(0xFFF4F7F3)
private val SurfaceWhite = Color(0xFFFFFFFF)

private val importantLegalWords = listOf(
    "باطل", "بطلان", "غیرنافذ", "نافذ", "فسخ", "اقاله", "اکراه", "اشتباه", "اهلیت",
    "تعهد", "متعهد", "ضامن", "ضمان", "مالکیت", "مالک", "خیار", "نکاح", "طلاق",
    "ارث", "وارث", "وصیت", "رهن", "وکالت", "حواله", "کفالت", "صلح", "بیع",
    "مبیع", "ثمن", "تاجر", "شرکت", "ورشکستگی", "چک", "سفته", "برات", "مسئول",
    "ممنوع", "الزام", "حق", "شرط", "قصد", "رضا", "محجور", "قیم", "ولی"
).sortedByDescending { it.length }

private enum class MainTab(val title: String, val icon: String) {
    HOME("خانه", "⌂"),
    LAW("قوانین", "$"),
    WORDS("لغات", "Aa"),
    FIQH("فقه", "ع"),
    PLAN("برنامه", "◷"),
}

class CourseViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CivilLawApplication
    private val db = app.database
    private val articleDao = db.articleDao()
    private val cardDao = db.studyCardDao()
    private val reminderDao = db.reminderDao()
    private val plannerDao = db.plannerTaskDao()
    private val now = MutableStateFlow(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            while (isActive) {
                now.value = System.currentTimeMillis()
                delay(30_000)
            }
        }
    }

    val articles = articleDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val cards = cardDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeReminders = reminderDao.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val plannerTasks = plannerDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dueReminders = combine(activeReminders, now) { all, t -> all.filter { it.dueAtMillis <= t } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayPlan = now.map {
        Phd140DayPlan.planFor(Phd140DayPlan.calendarDay(LocalDate.now()))
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        Phd140DayPlan.planFor(Phd140DayPlan.calendarDay(LocalDate.now()))
    )

    val message = MutableStateFlow("")

    fun scheduleReview(
        targetType: String,
        targetId: String,
        title: String,
        preview: String,
        hours: Int,
        soundUri: String,
    ) {
        viewModelScope.launch {
            LearningAlarmScheduler.addReview(
                app,
                targetType = targetType,
                targetId = targetId,
                title = title,
                preview = preview,
                intervalHours = hours,
                soundUri = soundUri,
            )
            message.value = "یادآوری برای ${intervalLabel(hours)} ثبت شد."
        }
    }

    fun gradeEnglish(card: StudyCardEntity, correct: Boolean, soundUri: String) {
        viewModelScope.launch {
            cardDao.update(
                card.copy(
                    firstStudiedEpochDay = card.firstStudiedEpochDay ?: LocalDate.now().toEpochDay(),
                    reviewCount = card.reviewCount + 1,
                    reviewEnabled = !correct,
                    nextReviewEpochDay = if (correct) Long.MAX_VALUE else LocalDate.now().plusDays(1).toEpochDay(),
                    lastReviewEpochDay = if (!correct) LocalDate.now().toEpochDay() else card.lastReviewEpochDay,
                )
            )
            if (!correct) {
                LearningAlarmScheduler.addReview(
                    app,
                    targetType = "ENGLISH",
                    targetId = card.id,
                    title = card.prompt,
                    preview = "معنی این واژه را دوباره از حافظه بازیابی کن.",
                    intervalHours = 24,
                    soundUri = soundUri,
                )
                message.value = "نادرست ثبت شد؛ این واژه ۲۴ ساعت بعد خودکار یادآوری می‌شود."
            } else {
                message.value = "درست ✓"
            }
        }
    }

    fun addPlannerTask(
        title: String,
        details: String,
        persianDate: String,
        hour: Int,
        minute: Int,
        soundUri: String,
    ) {
        viewModelScope.launch {
            runCatching {
                val day = PersianDate.parseToGregorian(persianDate)
                val due = LocalDateTime.of(day.year, day.monthValue, day.dayOfMonth, hour, minute)
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                require(due > System.currentTimeMillis()) { "زمان انتخاب‌شده باید در آینده باشد." }
                val id = plannerDao.upsert(
                    PlannerTaskEntity(
                        title = title.trim(),
                        details = details.trim(),
                        persianDate = PersianDate.fromGregorian(day).numeric(),
                        hour = hour,
                        minute = minute,
                        dueAtMillis = due,
                        soundUri = soundUri,
                    )
                )
                val row = plannerDao.getById(id) ?: error("Planner row missing")
                LearningAlarmScheduler.schedulePlanner(app, row)
            }.onSuccess {
                message.value = "برنامه و آلارم ثبت شد."
            }.onFailure {
                message.value = it.message ?: "تاریخ یا ساعت نامعتبر است."
            }
        }
    }

    fun togglePlanner(item: PlannerTaskEntity) {
        viewModelScope.launch {
            val next = item.copy(completed = !item.completed)
            plannerDao.update(next)
            if (next.completed) {
                LearningAlarmScheduler.cancelPlanner(app, item.id)
            } else {
                LearningAlarmScheduler.schedulePlanner(app, next)
            }
        }
    }

    fun deletePlanner(item: PlannerTaskEntity) {
        viewModelScope.launch {
            LearningAlarmScheduler.cancelPlanner(app, item.id)
            plannerDao.delete(item.id)
        }
    }

    fun clearMessage() { message.value = "" }

    private fun intervalLabel(hours: Int): String = when (hours) {
        12 -> "۱۲ ساعت بعد"
        24 -> "۲۴ ساعت بعد"
        48 -> "۴۸ ساعت بعد"
        72 -> "۳ روز بعد"
        168 -> "۷ روز بعد"
        336 -> "۱۴ روز بعد"
        480 -> "۲۰ روز بعد"
        960 -> "۴۰ روز بعد"
        else -> "$hours ساعت بعد"
    }
}

@Composable
fun DoctorCourseApp(vm: CourseViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("doctor_tohid_settings", Context.MODE_PRIVATE) }
    var soundUri by remember { mutableStateOf(prefs.getString("alarm_sound_uri", "") ?: "") }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val colors = lightColorScheme(
        primary = FocusBlue,
        onPrimary = Color.White,
        secondary = MemoryGreen,
        onSecondary = Color.White,
        tertiary = EnergyAmber,
        background = Canvas,
        surface = SurfaceWhite,
        onSurface = InkBlack,
        error = Color(0xFFB42318),
    )

    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            var selected by remember { mutableStateOf(MainTab.HOME) }
            val message by vm.message.collectAsStateWithLifecycle()
            val today = PersianDate.fromGregorian(LocalDate.now())
            val days = Phd140DayPlan.daysUntilExam()

            Scaffold(
                topBar = {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(SurfaceWhite)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "دوره آموزشی دکتر توحید نجفیان",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = InkBlack,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(today.label(), color = FocusBlue, style = MaterialTheme.typography.bodySmall)
                            Text("$days روز تا آزمون دکتری ۱۴۰۶", color = MemoryGreen, style = MaterialTheme.typography.bodySmall)
                        }
                        if (message.isNotBlank()) {
                            Text(
                                message,
                                color = if (message.contains("نامعتبر") || message.contains("باید")) MaterialTheme.colorScheme.error else MemoryGreen,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.clearMessage() }
                                    .padding(top = 4.dp),
                            )
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(containerColor = SurfaceWhite) {
                        MainTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selected == tab,
                                onClick = { selected = tab },
                                icon = { Text(tab.icon, fontSize = 18.sp) },
                                label = { Text(tab.title) },
                            )
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when (selected) {
                        MainTab.HOME -> HomeScreen(vm, onGoPlan = { selected = MainTab.PLAN })
                        MainTab.LAW -> LawLibraryScreen(vm, soundUri)
                        MainTab.WORDS -> VocabularyScreen(vm, soundUri)
                        MainTab.FIQH -> FiqhScreen(vm, soundUri)
                        MainTab.PLAN -> PlanAndPlannerScreen(
                            vm = vm,
                            soundUri = soundUri,
                            onSoundChanged = {
                                soundUri = it
                                prefs.edit().putString("alarm_sound_uri", it).apply()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(vm: CourseViewModel, onGoPlan: () -> Unit) {
    val articles by vm.articles.collectAsStateWithLifecycle()
    val cards by vm.cards.collectAsStateWithLifecycle()
    val reminders by vm.activeReminders.collectAsStateWithLifecycle()
    val due by vm.dueReminders.collectAsStateWithLifecycle()
    val plan by vm.todayPlan.collectAsStateWithLifecycle()

    val trade = cards.count { it.domain == "TRADE" }
    val fiqh = cards.count { it.domain == "FIQH" }
    val en = cards.count { it.domain == "ENGLISH" || it.domain == "VOCAB" }
    val ar = cards.count { it.domain == "ARABIC" }
    val reviewMinutes = estimateDueMinutes(due)
    val totalMinutes = plan.baseStudyMinutes + reviewMinutes

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2FD)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("امروز • روز ${plan.dayNumber} از ۱۴۰", color = FocusBlue, fontWeight = FontWeight.Bold)
                    Text(plan.phase, style = MaterialTheme.typography.titleLarge, color = InkBlack)
                    Text("مطالعه جدید: حدود ${formatMinutes(plan.baseStudyMinutes)}", color = QuietGray)
                    Text("مرور سررسیدشده: حدود ${formatMinutes(reviewMinutes)}", color = MemoryGreen)
                    Text("زمان پیشنهادی کل: ${formatMinutes(totalMinutes)}", fontWeight = FontWeight.Bold, color = InkBlack)
                    if (due.isNotEmpty()) {
                        Text("${due.size} مورد الآن برای مرور آماده است.", color = MaterialTheme.colorScheme.error)
                    }
                    Button(onClick = onGoPlan, modifier = Modifier.fillMaxWidth()) { Text("برنامه امروز و آلارم‌ها") }
                }
            }
        }
        item {
            Text("بانک‌های مطالعه", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        item {
            StatsGrid(
                listOf(
                    "قانون مدنی" to "${articles.size} ماده",
                    "قانون تجارت" to "$trade ماده/مقرره",
                    "متون فقه" to "$fiqh کارت",
                    "انگلیسی" to "$en لغت",
                    "عربی" to "$ar لغت",
                    "یادآوری فعال" to reminders.size.toString(),
                )
            )
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8F3))) {
                Column(Modifier.padding(16.dp)) {
                    Text("اصل برنامه", fontWeight = FontWeight.Bold, color = MemoryGreen)
                    Text(
                        "متن قانون بدون تفسیر داخل برنامه؛ تحلیل فقط با ارسال همان ماده به Gemini یا DeepSeek. هر مطلب را هر زمان خواستی وارد چرخه یادآوری ۱۲ ساعت تا ۴۰ روز کن.",
                        color = InkBlack,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (title, value) ->
                    Card(
                        Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(Modifier.padding(13.dp)) {
                            Text(value, color = FocusBlue, fontWeight = FontWeight.Bold)
                            Text(title, color = QuietGray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LawLibraryScreen(vm: CourseViewModel, soundUri: String) {
    val articles by vm.articles.collectAsStateWithLifecycle()
    val cards by vm.cards.collectAsStateWithLifecycle()
    var source by remember { mutableStateOf("CIVIL") }
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (source == "CIVIL") Button(onClick = { source = "CIVIL" }, modifier = Modifier.weight(1f)) { Text("قانون مدنی") }
            else OutlinedButton(onClick = { source = "CIVIL" }, modifier = Modifier.weight(1f)) { Text("قانون مدنی") }
            if (source == "TRADE") Button(onClick = { source = "TRADE" }, modifier = Modifier.weight(1f)) { Text("قانون تجارت") }
            else OutlinedButton(onClick = { source = "TRADE" }, modifier = Modifier.weight(1f)) { Text("قانون تجارت") }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            label = { Text("جستجوی شماره ماده یا عبارت داخل متن") },
            singleLine = true,
        )

        if (source == "CIVIL") {
            val filtered = remember(articles, query) {
                val q = query.trim()
                if (q.isBlank()) articles else articles.filter {
                    it.articleNumber.toString().contains(q) || it.officialText.contains(q, ignoreCase = true)
                }
            }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                item {
                    Text(
                        "۱۳۳۵ شماره ماده • متن رسمی • بدون تحلیل داخلی",
                        color = QuietGray,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
                items(filtered, key = { it.articleNumber }) { item ->
                    CivilLawCard(item, vm, soundUri)
                }
            }
        } else {
            val trade = remember(cards, query) {
                val q = query.trim()
                cards.filter { it.domain == "TRADE" }.filter {
                    q.isBlank() || it.title.contains(q, true) || it.answer.contains(q, true) ||
                        it.id.substringAfterLast(':').trimStart('0').contains(q)
                }
            }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                item {
                    Text("قانون تجارت و مقررات شرکتی موجود در بانک رسمی", color = QuietGray, modifier = Modifier.padding(vertical = 10.dp))
                }
                items(trade, key = { it.id }) { item ->
                    TradeLawCard(item, vm, soundUri)
                }
            }
        }
    }
}

@Composable
private fun CivilLawCard(article: ArticleEntity, vm: CourseViewModel, soundUri: String) {
    var reminderOpen by remember(article.articleNumber) { mutableStateOf(false) }
    val context = LocalContext.current
    Card(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LegalHeading(article.articleNumber.toString())
            HighlightedLegalText(article.officialText)
            if (article.topic == "ماده منسوخ") {
                Text("وضعیت: منسوخ/حذف‌شده در منبع تنقیحی", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { reminderOpen = true }, modifier = Modifier.weight(1f)) { Text("⏰ یادآوری") }
                OutlinedButton(
                    onClick = { shareArticleToAi(context, "com.google.android.apps.bard", "Gemini", "ماده ${article.articleNumber}", article.officialText) },
                    modifier = Modifier.weight(1f),
                ) { Text("Gemini") }
                OutlinedButton(
                    onClick = { shareArticleToAi(context, "com.deepseek.chat", "DeepSeek", "ماده ${article.articleNumber}", article.officialText) },
                    modifier = Modifier.weight(1f),
                ) { Text("DeepSeek") }
            }
        }
    }
    if (reminderOpen) {
        ReminderIntervalDialog(
            title = "ماده ${article.articleNumber}",
            onDismiss = { reminderOpen = false },
            onSelect = {
                vm.scheduleReview("CIVIL", article.articleNumber.toString(), "ماده ${article.articleNumber}", article.officialText, it, soundUri)
                reminderOpen = false
            }
        )
    }
}

@Composable
private fun TradeLawCard(card: StudyCardEntity, vm: CourseViewModel, soundUri: String) {
    var reminderOpen by remember(card.id) { mutableStateOf(false) }
    val context = LocalContext.current
    val number = card.id.substringAfterLast(':').toIntOrNull()?.toString() ?: card.ordinal.toString()
    Card(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LegalHeading(number)
            Text(card.title.substringBefore(" — ماده").ifBlank { "قانون تجارت" }, color = QuietGray, style = MaterialTheme.typography.bodySmall)
            HighlightedLegalText(card.answer)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { reminderOpen = true }, modifier = Modifier.weight(1f)) { Text("⏰ یادآوری") }
                OutlinedButton(
                    onClick = { shareArticleToAi(context, "com.google.android.apps.bard", "Gemini", "ماده $number", card.answer) },
                    modifier = Modifier.weight(1f),
                ) { Text("Gemini") }
                OutlinedButton(
                    onClick = { shareArticleToAi(context, "com.deepseek.chat", "DeepSeek", "ماده $number", card.answer) },
                    modifier = Modifier.weight(1f),
                ) { Text("DeepSeek") }
            }
        }
    }
    if (reminderOpen) {
        ReminderIntervalDialog(
            title = "ماده $number",
            onDismiss = { reminderOpen = false },
            onSelect = {
                vm.scheduleReview("TRADE", card.id, "ماده $number", card.answer, it, soundUri)
                reminderOpen = false
            }
        )
    }
}

@Composable
private fun LegalHeading(number: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = InkBlack, fontWeight = FontWeight.Bold, fontSize = 18.sp)) { append("ماده ") }
            withStyle(SpanStyle(color = FocusBlue, fontWeight = FontWeight.Bold, fontSize = 20.sp)) { append(number) }
        }
    )
}

@Composable
private fun HighlightedLegalText(text: String) {
    Text(
        highlightedText(text),
        color = InkBlack,
        style = MaterialTheme.typography.bodyLarge,
        lineHeight = 28.sp,
    )
}

private fun highlightedText(text: String): AnnotatedString = buildAnnotatedString {
    append(text)
    importantLegalWords.forEach { word ->
        var start = text.indexOf(word)
        while (start >= 0) {
            addStyle(
                SpanStyle(color = MemoryGreen, fontWeight = FontWeight.Bold),
                start,
                start + word.length,
            )
            start = text.indexOf(word, start + word.length)
        }
    }
}

@Composable
private fun VocabularyScreen(vm: CourseViewModel, soundUri: String) {
    val cards by vm.cards.collectAsStateWithLifecycle()
    var domain by remember { mutableStateOf("ENGLISH") }
    val list = remember(cards, domain) {
        cards.filter { if (domain == "ENGLISH") it.domain in setOf("ENGLISH", "VOCAB") else it.domain == "ARABIC" }
            .sortedBy { it.ordinal }
    }
    var index by remember(domain, list.size) { mutableIntStateOf(0) }
    var revealed by remember(domain, index) { mutableStateOf(false) }
    var reminderOpen by remember(domain, index) { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (domain == "ENGLISH") Button(onClick = { domain = "ENGLISH" }, modifier = Modifier.weight(1f)) { Text("انگلیسی ۲۰۰۰") }
            else OutlinedButton(onClick = { domain = "ENGLISH" }, modifier = Modifier.weight(1f)) { Text("انگلیسی ۲۰۰۰") }
            if (domain == "ARABIC") Button(onClick = { domain = "ARABIC" }, modifier = Modifier.weight(1f)) { Text("عربی ۱۰۰۰") }
            else OutlinedButton(onClick = { domain = "ARABIC" }, modifier = Modifier.weight(1f)) { Text("عربی ۱۰۰۰") }
        }

        if (list.isEmpty()) {
            Text("بانک لغات هنوز بارگذاری نشده است.", color = MaterialTheme.colorScheme.error)
            return
        }

        val card = list[index.coerceIn(0, list.lastIndex)]
        Text("کارت ${index + 1} از ${list.size}", color = QuietGray)
        Card(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clickable { revealed = !revealed },
            colors = CardDefaults.cardColors(containerColor = if (revealed) Color(0xFFEAF7F0) else Color(0xFFEAF2FD)),
            shape = RoundedCornerShape(26.dp),
        ) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        if (revealed) card.answer else card.prompt,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (revealed) MemoryGreen else FocusBlue,
                    )
                    Text(if (revealed) "پشت کارت • فارسی" else if (domain == "ENGLISH") "روی کارت • انگلیسی" else "روی کارت • عربی", color = QuietGray)
                    if (!revealed) Text("برای دیدن معنی لمس کن", color = EnergyAmber)
                }
            }
        }

        if (revealed) {
            if (domain == "ENGLISH") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            vm.gradeEnglish(card, false, soundUri)
                            index = (index + 1).coerceAtMost(list.lastIndex)
                            revealed = false
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("✕ نادرست") }
                    Button(
                        onClick = {
                            vm.gradeEnglish(card, true, soundUri)
                            index = (index + 1).coerceAtMost(list.lastIndex)
                            revealed = false
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("✓ درست") }
                }
                Text("نادرست = ورود خودکار به مرور ۲۴ ساعت بعد", color = MemoryGreen, style = MaterialTheme.typography.bodySmall)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { reminderOpen = true }, modifier = Modifier.weight(1f)) { Text("⏰ یادآوری") }
                    Button(
                        onClick = {
                            index = (index + 1).coerceAtMost(list.lastIndex)
                            revealed = false
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("کارت بعد") }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { if (index > 0) { index--; revealed = false } }, modifier = Modifier.weight(1f)) { Text("قبلی") }
                OutlinedButton(onClick = { reminderOpen = true }, modifier = Modifier.weight(1f)) { Text("⏰ یادآوری") }
                OutlinedButton(onClick = { if (index < list.lastIndex) { index++; revealed = false } }, modifier = Modifier.weight(1f)) { Text("بعدی") }
            }
        }

        if (reminderOpen) {
            ReminderIntervalDialog(
                title = card.prompt,
                onDismiss = { reminderOpen = false },
                onSelect = {
                    vm.scheduleReview(domain, card.id, card.prompt, "معنی این واژه را از حافظه بازیابی کن.", it, soundUri)
                    reminderOpen = false
                }
            )
        }
    }
}

@Composable
private fun FiqhScreen(vm: CourseViewModel, soundUri: String) {
    val cards by vm.cards.collectAsStateWithLifecycle()
    val fiqh = remember(cards) { cards.filter { it.domain == "FIQH" }.sortedBy { it.ordinal } }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        item {
            Text("متون فقه معاملات • دکتری حقوق خصوصی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
            Text("متن عربی، ترجمه و نکته آموزشی منبع؛ بدون تفسیر ساختگی مواد قانونی.", color = QuietGray, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        items(fiqh, key = { it.id }) { card ->
            FiqhCard(card, vm, soundUri)
        }
    }
}

@Composable
private fun FiqhCard(card: StudyCardEntity, vm: CourseViewModel, soundUri: String) {
    var open by remember(card.id) { mutableStateOf(false) }
    var reminder by remember(card.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(card.title, color = FocusBlue, fontWeight = FontWeight.Bold)
            Text(card.prompt, color = InkBlack, style = MaterialTheme.typography.titleMedium)
            if (open) {
                HorizontalDivider()
                Text(card.answer, color = MemoryGreen)
                if (card.explanation.isNotBlank()) Text(card.explanation, color = QuietGray)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { open = !open }, modifier = Modifier.weight(1f)) { Text(if (open) "بستن" else "نمایش ترجمه") }
                OutlinedButton(onClick = { reminder = true }, modifier = Modifier.weight(1f)) { Text("⏰ یادآوری") }
            }
        }
    }
    if (reminder) {
        ReminderIntervalDialog(
            title = card.title,
            onDismiss = { reminder = false },
            onSelect = {
                vm.scheduleReview("FIQH", card.id, card.title, card.prompt, it, soundUri)
                reminder = false
            }
        )
    }
}

@Composable
private fun PlanAndPlannerScreen(
    vm: CourseViewModel,
    soundUri: String,
    onSoundChanged: (String) -> Unit,
) {
    val plan by vm.todayPlan.collectAsStateWithLifecycle()
    val due by vm.dueReminders.collectAsStateWithLifecycle()
    val tasks by vm.plannerTasks.collectAsStateWithLifecycle()
    var page by remember { mutableStateOf("PLAN") }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (page == "PLAN") Button(onClick = { page = "PLAN" }, modifier = Modifier.weight(1f)) { Text("برنامه ۱۴۰ روزه") }
            else OutlinedButton(onClick = { page = "PLAN" }, modifier = Modifier.weight(1f)) { Text("برنامه ۱۴۰ روزه") }
            if (page == "PLANNER") Button(onClick = { page = "PLANNER" }, modifier = Modifier.weight(1f)) { Text("تقویم و آلارم من") }
            else OutlinedButton(onClick = { page = "PLANNER" }, modifier = Modifier.weight(1f)) { Text("تقویم و آلارم من") }
        }
        if (page == "PLAN") DailyPlanScreen(plan, due) else PersonalPlannerScreen(vm, tasks, soundUri, onSoundChanged)
    }
}

@Composable
private fun DailyPlanScreen(plan: com.mizan.civilleitner.domain.DailyPlan, due: List<ReminderEntity>) {
    val reviewMinutes = estimateDueMinutes(due)
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("روز ${plan.dayNumber} از ۱۴۰ • ${plan.persianLabel}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(plan.phase, color = FocusBlue)
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF7F0))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("برآورد زمان امروز", fontWeight = FontWeight.Bold, color = MemoryGreen)
                    Text("مطالعه جدید: ${formatMinutes(plan.baseStudyMinutes)}")
                    Text("مرورهای سررسیدشده: ${formatMinutes(reviewMinutes)}")
                    Text("مجموع: ${formatMinutes(plan.baseStudyMinutes + reviewMinutes)}", fontWeight = FontWeight.Bold)
                    Text("${due.size} مورد سررسیدشده", color = QuietGray)
                }
            }
        }
        item { PlanRange("قانون مدنی", plan.civilFrom, plan.civilTo) }
        item { PlanRange("قانون تجارت", plan.tradeFrom, plan.tradeTo) }
        item { PlanRange("لغات انگلیسی", plan.englishFrom, plan.englishTo) }
        item { PlanRange("لغات عربی", plan.arabicFrom, plan.arabicTo) }
        item { PlanRange("متون فقه", plan.fiqhFrom, plan.fiqhTo) }
        item {
            Text(
                "زمان مرور هر روز بر اساس تعداد واقعی یادآوری‌های سررسیدشده به زمان پایه همان روز اضافه می‌شود.",
                color = QuietGray,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun PlanRange(title: String, from: Int, to: Int) {
    if (from <= 0 || to <= 0) return
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("$from تا $to", color = FocusBlue)
        }
    }
}

@Composable
private fun PersonalPlannerScreen(
    vm: CourseViewModel,
    tasks: List<PlannerTaskEntity>,
    soundUri: String,
    onSoundChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    val today = PersianDate.fromGregorian(LocalDate.now()).numeric()
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today) }
    var hour by remember { mutableStateOf(java.time.LocalTime.now().plusHours(1).hour.toString()) }
    var minute by remember { mutableStateOf("00") }

    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = if (Build.VERSION.SDK_INT >= 33) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        if (uri != null) onSoundChanged(uri.toString())
    }

    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("برنامه‌ریز شخصی با تاریخ شمسی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("عنوان کار") }, singleLine = true)
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(details, { details = it }, modifier = Modifier.fillMaxWidth(), label = { Text("توضیحات") })
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(date, { date = it }, modifier = Modifier.fillMaxWidth(), label = { Text("تاریخ شمسی؛ مثال ۱۴۰۵/۰۷/۰۱") }, singleLine = true)
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(hour, { hour = it.filter(Char::isDigit).take(2) }, modifier = Modifier.weight(1f), label = { Text("ساعت") }, singleLine = true)
                OutlinedTextField(minute, { minute = it.filter(Char::isDigit).take(2) }, modifier = Modifier.weight(1f), label = { Text("دقیقه") }, singleLine = true)
            }
            Spacer(Modifier.height(7.dp))
            OutlinedButton(
                onClick = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "انتخاب صدای آلارم")
                        if (soundUri.isNotBlank()) putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(soundUri))
                    }
                    ringtonePicker.launch(intent)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (soundUri.isBlank()) "انتخاب صدای آلارم" else "تغییر صدای آلارم ✓") }
            Spacer(Modifier.height(7.dp))
            Button(
                onClick = {
                    val h = hour.toIntOrNull()
                    val m = minute.toIntOrNull()
                    if (title.isBlank() || h == null || m == null || h !in 0..23 || m !in 0..59) return@Button
                    vm.addPlannerTask(title, details, date, h, m, soundUri)
                    title = ""
                    details = ""
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank(),
            ) { Text("ثبت برنامه و آلارم") }
        }
        item { HorizontalDivider() }
        item { Text("کارهای امروز و آینده", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (tasks.isEmpty()) item { Text("هنوز برنامه شخصی ثبت نشده است.", color = QuietGray) }
        items(tasks, key = { it.id }) { item ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (item.completed) Color(0xFFF0F1F1) else SurfaceWhite),
            ) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.completed, onCheckedChange = { vm.togglePlanner(item) })
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, color = if (item.completed) QuietGray else InkBlack)
                            Text("${item.persianDate} • %02d:%02d".format(item.hour, item.minute), color = FocusBlue, style = MaterialTheme.typography.bodySmall)
                            if (item.details.isNotBlank()) Text(item.details, color = QuietGray, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { vm.deletePlanner(item) }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderIntervalDialog(
    title: String,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val intervals = listOf(
        12 to "۱۲ ساعت",
        24 to "۲۴ ساعت",
        48 to "۴۸ ساعت",
        72 to "۳ روز",
        168 to "۷ روز",
        336 to "۱۴ روز",
        480 to "۲۰ روز",
        960 to "۴۰ روز",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("یادآوری مجدد • $title") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("چه زمانی دوباره این مطلب را ببینی؟", color = QuietGray)
                intervals.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (hours, label) ->
                            OutlinedButton(onClick = { onSelect(hours) }, modifier = Modifier.weight(1f)) { Text(label) }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

private fun shareArticleToAi(
    context: Context,
    packageName: String,
    providerName: String,
    articleLabel: String,
    articleText: String,
) {
    val prompt = """
        این ماده قانونی را با حفظ دقت حقوقی، به زبان ساده تفهیم و ساده‌سازی کن.
        ابتدا مفهوم اصلی ماده را توضیح بده، سپس اجزای آن را تفکیک کن و در پایان یک مثال کوتاه بزن.
        چیزی خارج از متن ماده را به قانون نسبت نده.

        $articleLabel:
        $articleText
    """.trimIndent()

    val explicit = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, prompt)
        setPackage(packageName)
    }
    try {
        context.startActivity(explicit)
    } catch (_: ActivityNotFoundException) {
        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "تفهیم $articleLabel با $providerName")
            putExtra(Intent.EXTRA_TEXT, prompt)
        }
        context.startActivity(Intent.createChooser(fallback, "ارسال به هوش مصنوعی"))
    }
}

private fun estimateDueMinutes(due: List<ReminderEntity>): Int {
    val law = due.count { it.targetType in setOf("CIVIL", "TRADE") }
    val en = due.count { it.targetType == "ENGLISH" }
    val ar = due.count { it.targetType == "ARABIC" }
    val fiqh = due.count { it.targetType == "FIQH" }
    return Phd140DayPlan.estimateReviewMinutes(law, en, ar, fiqh)
}

private fun formatMinutes(minutes: Int): String {
    if (minutes < 60) return "$minutes دقیقه"
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "$h ساعت" else "$h ساعت و $m دقیقه"
}
