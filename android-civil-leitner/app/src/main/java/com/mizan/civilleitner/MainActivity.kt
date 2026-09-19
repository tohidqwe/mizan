package com.mizan.civilleitner

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mizan.civilleitner.data.ArticleEntity
import com.mizan.civilleitner.data.PlannerTaskEntity
import com.mizan.civilleitner.data.ReminderEntity
import com.mizan.civilleitner.data.StudyCardEntity
import com.mizan.civilleitner.domain.PersianCalendar
import com.mizan.civilleitner.domain.Phd140DayPlan
import com.mizan.civilleitner.worker.StudyAlarmScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

private val FocusNavy = Color(0xFF17324D)
private val FocusBlue = Color(0xFF2D6CDF)
private val FocusGreen = Color(0xFF2E7D32)
private val FocusAmber = Color(0xFFF4B400)
private val FocusBackground = Color(0xFFF5F7F6)
private val LawTextBlack = Color(0xFF171717)
private val SoftRed = Color(0xFFB3261E)

data class LegalTextItem(
    val id: String,
    val collectionLabel: String,
    val articleKey: String,
    val articleNumber: Int,
    val officialText: String,
    val legalStatus: String,
    val statusLabel: String = "",
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DoctorTohidCourseRoot() }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CivilLawApplication
    private val articleDao = app.database.articleDao()
    private val cardDao = app.database.studyCardDao()
    private val reminderDao = app.database.reminderDao()
    private val plannerDao = app.database.plannerDao()
    private val clock = MutableStateFlow(System.currentTimeMillis())
    private val prefs = app.getSharedPreferences("doctor_tohid_settings", Context.MODE_PRIVATE)

    val articles = articleDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val cards = cardDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reminders = reminderDao.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dueReminders = clock.flatMapLatest { reminderDao.observeDue(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val plannerTasks = plannerDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val soundUri = MutableStateFlow(prefs.getString("alarm_sound_uri", "") ?: "")
    val civilSupplemental = MutableStateFlow<List<LegalTextItem>>(emptyList())
    val tradeLibrary = MutableStateFlow<List<LegalTextItem>>(emptyList())

    init {
        viewModelScope.launch {
            civilSupplemental.value = loadCivilSupplemental()
            tradeLibrary.value = loadTradeLibrary()
        }
        viewModelScope.launch {
            while (isActive) {
                clock.value = System.currentTimeMillis()
                delay(30_000)
            }
        }
    }

    private fun loadCivilSupplemental(): List<LegalTextItem> = runCatching {
        val raw = app.assets.open("civil_supplemental.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    LegalTextItem(
                        id = "CIVIL:" + o.getString("articleKey"),
                        collectionLabel = "قانون مدنی",
                        articleKey = o.getString("articleKey"),
                        articleNumber = o.getInt("articleNumber"),
                        officialText = o.getString("officialText"),
                        legalStatus = o.optString("legalStatus", "ACTIVE"),
                        statusLabel = o.optString("legalStatus"),
                    )
                )
            }
        }
    }.getOrElse { emptyList() }

    private fun loadTradeLibrary(): List<LegalTextItem> = runCatching {
        val raw = app.assets.open("trade_all_articles.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    LegalTextItem(
                        id = o.getString("id"),
                        collectionLabel = o.getString("collectionLabel"),
                        articleKey = o.getString("articleKey"),
                        articleNumber = o.getInt("articleNumber"),
                        officialText = o.getString("officialText"),
                        legalStatus = o.getString("legalStatus"),
                        statusLabel = o.optString("statusLabel"),
                    )
                )
            }
        }
    }.getOrElse { emptyList() }

    fun setSound(uri: String) {
        soundUri.value = uri
        prefs.edit().putString("alarm_sound_uri", uri).apply()
    }

    fun scheduleReminder(
        itemType: String,
        itemId: String,
        title: String,
        body: String,
        delayMillis: Long,
    ) {
        viewModelScope.launch {
            val row = ReminderEntity(
                itemType = itemType,
                itemId = itemId,
                title = title,
                body = body,
                dueAtMillis = System.currentTimeMillis() + delayMillis,
                soundUri = soundUri.value,
            )
            val id = reminderDao.insert(row)
            StudyAlarmScheduler.scheduleStudy(app, row.copy(id = id))
            clock.value = System.currentTimeMillis()
        }
    }

    fun markReminderDone(item: ReminderEntity) {
        viewModelScope.launch {
            reminderDao.update(item.copy(enabled = false))
            StudyAlarmScheduler.cancel(app, StudyAlarmScheduler.KIND_STUDY, item.id)
            clock.value = System.currentTimeMillis()
        }
    }

    fun addPlannerTask(title: String, details: String, persianDate: String, timeText: String): String? {
        val date = PersianCalendar.parseNumeric(persianDate) ?: return "تاریخ شمسی معتبر نیست."
        val time = runCatching { LocalTime.parse(timeText.trim()) }.getOrNull()
            ?: return "ساعت را به صورت HH:mm وارد کن."
        val due = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (due <= System.currentTimeMillis()) return "زمان انتخاب‌شده گذشته است."
        viewModelScope.launch {
            val row = PlannerTaskEntity(
                title = title.trim(),
                details = details.trim(),
                persianDate = persianDate.trim(),
                timeText = timeText.trim(),
                dueAtMillis = due,
                soundUri = soundUri.value,
            )
            val id = plannerDao.insert(row)
            StudyAlarmScheduler.schedulePlanner(app, row.copy(id = id))
        }
        return null
    }

    fun togglePlannerDone(task: PlannerTaskEntity) {
        viewModelScope.launch {
            val next = task.copy(completed = !task.completed)
            plannerDao.update(next)
            if (next.completed) {
                StudyAlarmScheduler.cancel(app, StudyAlarmScheduler.KIND_PLANNER, task.id)
            } else if (next.dueAtMillis > System.currentTimeMillis()) {
                StudyAlarmScheduler.schedulePlanner(app, next)
            }
        }
    }

    fun deletePlanner(task: PlannerTaskEntity) {
        viewModelScope.launch {
            StudyAlarmScheduler.cancel(app, StudyAlarmScheduler.KIND_PLANNER, task.id)
            plannerDao.delete(task.id)
        }
    }
}

private enum class AppTab(val title: String, val glyph: String) {
    TODAY("امروز", "⌂"),
    LAWS("قوانین", "§"),
    FIQH("متون فقه", "ع"),
    VOCAB("لغات", "Aa"),
    PLANNER("برنامه", "◷"),
}

@Composable
private fun DoctorTohidCourseRoot(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        scope.launch { StudyAlarmScheduler.rescheduleAll(context.applicationContext) }
    }

    val colors = lightColorScheme(
        primary = FocusNavy,
        onPrimary = Color.White,
        secondary = FocusBlue,
        tertiary = FocusGreen,
        background = FocusBackground,
        surface = Color.White,
        error = SoftRed,
    )

    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            var selected by remember { mutableStateOf(AppTab.TODAY) }
            Scaffold(
                topBar = { HeaderBar() },
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        AppTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = selected == tab,
                                onClick = { selected = tab },
                                icon = { Text(tab.glyph, fontSize = if (tab == AppTab.VOCAB) 14.sp else 20.sp) },
                                label = { Text(tab.title, fontSize = 11.sp) },
                            )
                        }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when (selected) {
                        AppTab.TODAY -> TodayDashboard(vm)
                        AppTab.LAWS -> LawsScreen(vm)
                        AppTab.FIQH -> FiqhScreen(vm)
                        AppTab.VOCAB -> VocabularyScreen(vm)
                        AppTab.PLANNER -> PlannerScreen(vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderBar() {
    val today = LocalDate.now()
    val persian = PersianCalendar.fromGregorian(today)
    val left = ChronoUnit.DAYS.between(today, Phd140DayPlan.EXAM).coerceAtLeast(0)
    Surface(shadowElevation = 3.dp, color = Color.White) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                "دوره آموزشی دکتر توحید نجفیان",
                style = MaterialTheme.typography.titleLarge,
                color = FocusNavy,
                fontWeight = FontWeight.Bold,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("امروز: ${persian.label()}", color = Color(0xFF536471), fontSize = 13.sp)
                Text("$left روز تا تاریخ هدف آزمون", color = FocusBlue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}

data class RangeSlice(val from: Int, val to: Int) {
    val count: Int get() = if (from <= 0 || to < from) 0 else to - from + 1
}

private fun rangeFor(day: Int, total: Int, coverDays: Int): RangeSlice {
    if (day !in 1..coverDays || total <= 0) return RangeSlice(0,0)
    val from = ((day - 1) * total) / coverDays + 1
    val to = (day * total) / coverDays
    return RangeSlice(from,to)
}

@Composable
private fun TodayDashboard(vm: MainViewModel) {
    val articles by vm.articles.collectAsStateWithLifecycle()
    val cards by vm.cards.collectAsStateWithLifecycle()
    val due by vm.dueReminders.collectAsStateWithLifecycle()

    val today = LocalDate.now()
    val day = Phd140DayPlan.calendarDay(today)
    val trade = cards.filter { it.domain == "TRADE" }.sortedBy { it.ordinal }
    val english = cards.filter { it.domain == "VOCAB" }.sortedBy { it.ordinal }
    val arabic = cards.filter { it.domain == "ARABIC" }.sortedBy { it.ordinal }
    val fiqh = cards.filter { it.domain == "FIQH" }.sortedBy { it.ordinal }

    val civilRange = rangeFor(day,1335,89)
    val tradeRange = rangeFor(day,trade.size,89)
    val englishRange = rangeFor(day,english.size,89)
    val arabicRange = rangeFor(day,arabic.size,89)
    val fiqhRange = rangeFor(day,fiqh.size,60)

    val newMinutes = if (day <= 89) {
        civilRange.count * 3.0 + tradeRange.count * 3.0 +
            englishRange.count * .75 + arabicRange.count * .75 + fiqhRange.count * 6.0
    } else if (day <= 112) 180.0 else if (day <= 126) 210.0 else 150.0

    val reviewMinutes = due.sumOf {
        when (it.itemType) {
            "CIVIL", "TRADE" -> 3.0
            "FIQH" -> 5.0
            "ENGLISH", "ARABIC" -> .75
            else -> 2.0
        }
    }
    val totalMinutes = ceil(newMinutes + reviewMinutes).toInt()

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val progress = (day.coerceIn(1,140) / 140f)
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF1FA))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("روز $day از ۱۴۰", style = MaterialTheme.typography.headlineSmall, color = FocusNavy)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text("زمان مطالعه جدید: حدود ${ceil(newMinutes).toInt()} دقیقه")
                    Text(
                        "زمان مرورهای انتخابی: حدود ${ceil(reviewMinutes).toInt()} دقیقه",
                        color = if (due.isEmpty()) FocusGreen else FocusAmber,
                    )
                    Text("کل زمان پیشنهادی امروز: حدود $totalMinutes دقیقه", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text("برنامه امروز", style = MaterialTheme.typography.titleLarge, color = FocusNavy)
        }

        if (day <= 89) {
            item { PlanLine("قانون مدنی", "مواد ${civilRange.from} تا ${civilRange.to}", civilRange.count) }
            item { PlanLine("قانون تجارت", "واحدهای ${tradeRange.from} تا ${tradeRange.to}", tradeRange.count) }
            item { PlanLine("لغات انگلیسی", "کارت‌های ${englishRange.from} تا ${englishRange.to}", englishRange.count) }
            item { PlanLine("لغات عربی", "کارت‌های ${arabicRange.from} تا ${arabicRange.to}", arabicRange.count) }
            item { PlanLine("متون فقه", "کارت‌های ${fiqhRange.from} تا ${fiqhRange.to}", fiqhRange.count) }
        } else {
            item {
                Card {
                    Text(
                        when {
                            day <= 112 -> "فاز دوم: مرور موضوعی قوانین، لغات دشوار و متون فقه + تست زمان‌دار."
                            day <= 126 -> "فاز سوم: شبیه‌سازی آزمون، ترمیم نقاط ضعف و مرور فشرده."
                            else -> "فاز نهایی: فقط تثبیت، مرورهای سررسیدشده و جمع‌بندی."
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        item {
            Text("مرورهای سررسیدشده", style = MaterialTheme.typography.titleLarge, color = FocusNavy)
            if (due.isEmpty()) Text("فعلاً مرور سررسیدشده‌ای نداری.", color = FocusGreen)
        }
        items(due, key = { it.id }) { reminder ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7DF))) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(reminder.title, fontWeight = FontWeight.Bold)
                        Text(reminder.body, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                    }
                    Button(onClick = { vm.markReminderDone(reminder) }) { Text("مرور شد") }
                }
            }
        }
        item {
            Text(
                "تاریخ هدف داخلی برنامه: ${PersianCalendar.fromGregorian(Phd140DayPlan.EXAM).label()}",
                color = Color(0xFF6E7D86),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun PlanLine(title: String, range: String, count: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, color = FocusNavy)
                Text(range, color = Color(0xFF667782), fontSize = 13.sp)
            }
            Text("$count مورد", color = FocusBlue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LawsScreen(vm: MainViewModel) {
    val articles by vm.articles.collectAsStateWithLifecycle()
    val supplements by vm.civilSupplemental.collectAsStateWithLifecycle()
    val tradeLibrary by vm.tradeLibrary.collectAsStateWithLifecycle()
    var law by remember { mutableStateOf("CIVIL") }
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (law == "CIVIL") Button({ law="CIVIL" }, Modifier.weight(1f)) { Text("قانون مدنی") }
            else OutlinedButton({ law="CIVIL" }, Modifier.weight(1f)) { Text("قانون مدنی") }
            if (law == "TRADE") Button({ law="TRADE" }, Modifier.weight(1f)) { Text("قانون تجارت") }
            else OutlinedButton({ law="TRADE" }, Modifier.weight(1f)) { Text("قانون تجارت") }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            label = { Text("جستجو در شماره یا متن ماده") },
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        if (law == "CIVIL") {
            val shown = articles.filter {
                query.isBlank() || it.articleNumber.toString().contains(query.trim()) ||
                    it.officialText.contains(query.trim(), ignoreCase = true)
            }
            val shownSupplements = supplements.filter {
                query.isBlank() || it.articleKey.contains(query.trim()) ||
                    it.officialText.contains(query.trim(), ignoreCase = true)
            }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                items(shown, key = { "main:" + it.articleNumber }) {
                    CivilLawCard(it,vm)
                }
                items(shownSupplements, key = { "supp:" + it.id }) {
                    SupplementalLawCard(it,vm)
                }
            }
        } else {
            val shown = tradeLibrary.filter {
                query.isBlank() || it.collectionLabel.contains(query.trim(),true) ||
                    it.officialText.contains(query.trim(),true) || it.articleKey.contains(query.trim(),true)
            }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                items(shown, key = { it.id }) {
                    TradeLawCard(it,vm)
                }
            }
        }
    }
}

private val legalImportantWords = listOf(
    "باطل","بطلان","غیرنافذ","عدم نفوذ","فسخ","اقاله","خیار","اکراه","اشتباه","اهلیت",
    "تعهد","ضامن","ضمان","مسئول","مالک","مالکیت","تصرف","ارث","وارث","ترکه","وصیت",
    "نکاح","طلاق","عده","حضانت","نفقه","مهر","بیع","اجاره","رهن","وکالت","حواله",
    "کفالت","صلح","شرکت","مضاربه","مزارعه","مساقات","تسلیم","ثمن","مبیع","خسارت",
    "تقصیر","اتلاف","تسبیب","غصب","شفعه","قیم","محجور","صغیر","مجنون","سفیه",
    "ممنوع","مکلف","باید","نمی‌تواند","حق دارد","استثنا","مگر","شرط","مهلت"
)

private fun highlightedLawText(text: String): AnnotatedString = buildAnnotatedString {
    append(text)
    legalImportantWords.forEach { word ->
        var start = text.indexOf(word)
        while (start >= 0) {
            addStyle(
                SpanStyle(color = FocusGreen, fontWeight = FontWeight.Bold),
                start,
                start + word.length
            )
            start = text.indexOf(word,start + word.length)
        }
    }
}

@Composable
private fun CivilLawCard(article: ArticleEntity, vm: MainViewModel) {
    val context=LocalContext.current
    var reminder by remember { mutableStateOf(false) }
    var ai by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row {
                    Text("ماده ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(article.articleNumber.toString(), color = FocusBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Row {
                    TextButton(onClick = { reminder=true }) { Text("⏰ یادآوری") }
                    TextButton(onClick = { ai=true }) { Text("↗ هوش مصنوعی") }
                }
            }
            Text(highlightedLawText(article.officialText), color = LawTextBlack, lineHeight = 26.sp)
            if (article.topic == "ماده منسوخ") {
                Text("وضعیت منبع: منسوخ/حذف‌شده", color = SoftRed, fontSize = 12.sp)
            }
        }
    }
    if(reminder) ReminderDialog(
        title="ماده ${article.articleNumber} قانون مدنی",
        onDismiss={reminder=false},
        onSelect={ delay ->
            vm.scheduleReminder("CIVIL",article.articleNumber.toString(),"مرور ماده ${article.articleNumber} قانون مدنی",article.officialText,delay)
            reminder=false
        }
    )
    if(ai) AiDialog(
        onDismiss={ai=false},
        onGemini={ shareLawToAi(context,"com.google.android.apps.bard","ماده ${article.articleNumber} قانون مدنی",article.officialText); ai=false },
        onDeepSeek={ shareLawToAi(context,"com.deepseek.chat","ماده ${article.articleNumber} قانون مدنی",article.officialText); ai=false },
    )
}

@Composable
private fun SupplementalLawCard(item: LegalTextItem, vm: MainViewModel) {
    val context=LocalContext.current
    var reminder by remember { mutableStateOf(false) }
    var ai by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row {
                    Text("ماده ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(item.articleKey, color = FocusBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Row {
                    TextButton(onClick = { reminder=true }) { Text("⏰ یادآوری") }
                    TextButton(onClick = { ai=true }) { Text("↗ هوش مصنوعی") }
                }
            }
            Text(highlightedLawText(item.officialText), color = LawTextBlack, lineHeight = 26.sp)
            if(item.legalStatus != "ACTIVE") Text("وضعیت منبع: ${item.legalStatus}", color=SoftRed, fontSize=12.sp)
        }
    }
    if(reminder) ReminderDialog(
        title="ماده ${item.articleKey} قانون مدنی",
        onDismiss={reminder=false},
        onSelect={delay->
            vm.scheduleReminder("CIVIL",item.id,"مرور ماده ${item.articleKey} قانون مدنی",item.officialText,delay)
            reminder=false
        }
    )
    if(ai) AiDialog(
        onDismiss={ai=false},
        onGemini={shareLawToAi(context,"com.google.android.apps.bard","ماده ${item.articleKey} قانون مدنی",item.officialText);ai=false},
        onDeepSeek={shareLawToAi(context,"com.deepseek.chat","ماده ${item.articleKey} قانون مدنی",item.officialText);ai=false},
    )
}

@Composable
private fun TradeLawCard(item: LegalTextItem, vm: MainViewModel) {
    val context=LocalContext.current
    var reminder by remember { mutableStateOf(false) }
    var ai by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row {
                    Text("ماده ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(item.articleKey, color = FocusBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Row {
                    TextButton({reminder=true}) { Text("⏰ یادآوری") }
                    TextButton({ai=true}) { Text("↗ هوش مصنوعی") }
                }
            }
            Text(item.collectionLabel, color = Color(0xFF687984), fontSize = 12.sp)
            Text(highlightedLawText(item.officialText), color = LawTextBlack, lineHeight = 26.sp)
            when(item.legalStatus) {
                "REPEALED" -> Text("وضعیت منبع: منسوخ/فاقد اعتبار",color=SoftRed,fontSize=12.sp)
                "HISTORICAL_REPLACED_1347" -> Text("وضعیت منبع: متن تاریخی؛ بخش شرکت‌های سهامی با لایحه ۱۳۴۷ جایگزین شده است.",color=Color(0xFF8A5A00),fontSize=12.sp)
            }
        }
    }
    if(reminder) ReminderDialog(
        title="ماده ${item.articleKey} — ${item.collectionLabel}",
        onDismiss={reminder=false},
        onSelect={delay->
            vm.scheduleReminder("TRADE",item.id,"مرور ماده ${item.articleKey} — ${item.collectionLabel}",item.officialText,delay)
            reminder=false
        }
    )
    if(ai) AiDialog(
        onDismiss={ai=false},
        onGemini={shareLawToAi(context,"com.google.android.apps.bard","ماده ${item.articleKey} — ${item.collectionLabel}",item.officialText);ai=false},
        onDeepSeek={shareLawToAi(context,"com.deepseek.chat","ماده ${item.articleKey} — ${item.collectionLabel}",item.officialText);ai=false},
    )
}

private fun shareLawToAi(context: Context, packageName: String, title: String, text: String) {
    val prompt = """
این ماده قانونی را با حفظ دقت حقوقی، به زبان ساده تفهیم و ساده‌سازی کن.
ابتدا مفهوم اصلی ماده را توضیح بده، سپس اجزای آن را تفکیک کن و در پایان یک مثال کوتاه بزن.
چیزی خارج از متن ماده را به قانون نسبت نده.

$title:
$text
""".trimIndent()
    val targeted = Intent(Intent.ACTION_SEND).apply {
        type="text/plain"
        putExtra(Intent.EXTRA_TEXT,prompt)
        setPackage(packageName)
    }
    val pm=context.packageManager
    if(targeted.resolveActivity(pm)!=null) context.startActivity(targeted)
    else {
        val generic=Intent(Intent.ACTION_SEND).apply {
            type="text/plain"
            putExtra(Intent.EXTRA_TEXT,prompt)
        }
        context.startActivity(Intent.createChooser(generic,"ارسال ماده به هوش مصنوعی"))
    }
}

@Composable
private fun AiDialog(onDismiss:()->Unit,onGemini:()->Unit,onDeepSeek:()->Unit) {
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("ارسال ماده برای تفهیم")},
        text={Text("پرامپت ساده‌سازی از قبل آماده است. هوش مصنوعی مقصد را انتخاب کن.")},
        confirmButton={ Button(onClick=onGemini){Text("Gemini")} },
        dismissButton={
            Row {
                TextButton(onClick=onDeepSeek){Text("DeepSeek")}
                TextButton(onClick=onDismiss){Text("انصراف")}
            }
        }
    )
}

private data class IntervalOption(val label:String,val millis:Long)
private val reminderIntervals=listOf(
    IntervalOption("۱۲ ساعت",12L*60*60*1000),
    IntervalOption("۲۴ ساعت",24L*60*60*1000),
    IntervalOption("۴۸ ساعت",48L*60*60*1000),
    IntervalOption("۳ روز",3L*24*60*60*1000),
    IntervalOption("۷ روز",7L*24*60*60*1000),
    IntervalOption("۱۴ روز",14L*24*60*60*1000),
    IntervalOption("۲۰ روز",20L*24*60*60*1000),
    IntervalOption("۴۰ روز",40L*24*60*60*1000),
)

@Composable
private fun ReminderDialog(title:String,onDismiss:()->Unit,onSelect:(Long)->Unit) {
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("یادآوری مجدد")},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(title,color=FocusNavy,fontWeight=FontWeight.Bold)
                reminderIntervals.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        row.forEach { opt ->
                            OutlinedButton(
                                onClick={onSelect(opt.millis)},
                                modifier=Modifier.weight(1f)
                            ){Text(opt.label)}
                        }
                    }
                }
            }
        },
        confirmButton={},
        dismissButton={TextButton(onClick=onDismiss){Text("بستن")}}
    )
}

@Composable
private fun FiqhScreen(vm: MainViewModel) {
    val cards by vm.cards.collectAsStateWithLifecycle()
    val fiqh=cards.filter{it.domain=="FIQH"}.sortedBy{it.ordinal}
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Text(
            "متون فقه — معاملات",
            style=MaterialTheme.typography.headlineSmall,
            color=FocusNavy,
            modifier=Modifier.padding(14.dp)
        )
        OutlinedTextField(
            value=query,onValueChange={query=it},
            modifier=Modifier.fillMaxWidth().padding(horizontal=12.dp),
            label={Text("جستجو در مبحث یا متن")}
        )
        val shown=fiqh.filter{query.isBlank()||it.title.contains(query,true)||it.prompt.contains(query,true)||it.answer.contains(query,true)}
        LazyColumn(Modifier.fillMaxSize().padding(horizontal=12.dp)) {
            items(shown,key={it.id}) { card -> FiqhCard(card,vm) }
        }
    }
}

@Composable
private fun FiqhCard(card:StudyCardEntity,vm:MainViewModel) {
    var reveal by remember(card.id){mutableStateOf(false)}
    var reminder by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(vertical=6.dp)) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(card.title,fontWeight=FontWeight.Bold,color=FocusNavy,modifier=Modifier.weight(1f))
                TextButton({reminder=true}){Text("⏰")}
            }
            Text(card.prompt)
            if(reveal) {
                HorizontalDivider()
                Text(card.answer,color=FocusGreen,lineHeight=24.sp)
            } else {
                OutlinedButton(onClick={reveal=true},modifier=Modifier.fillMaxWidth()){Text("نمایش پاسخ / ترجمه")}
            }
        }
    }
    if(reminder) ReminderDialog(
        title=card.title,onDismiss={reminder=false},
        onSelect={delay->
            vm.scheduleReminder("FIQH",card.id,"مرور متون فقه: ${card.title}",card.prompt,delay)
            reminder=false
        }
    )
}

@Composable
private fun VocabularyScreen(vm: MainViewModel) {
    val cards by vm.cards.collectAsStateWithLifecycle()
    var domain by remember { mutableStateOf("VOCAB") }
    val list=cards.filter{it.domain==domain}.sortedBy{it.ordinal}
    var index by remember(domain,list.size){mutableStateOf(0)}
    val card=list.getOrNull(index.coerceIn(0,(list.size-1).coerceAtLeast(0)))

    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            if(domain=="VOCAB") Button({domain="VOCAB"},Modifier.weight(1f)){Text("انگلیسی ۲۰۰۰")}
            else OutlinedButton({domain="VOCAB"},Modifier.weight(1f)){Text("انگلیسی ۲۰۰۰")}
            if(domain=="ARABIC") Button({domain="ARABIC"},Modifier.weight(1f)){Text("عربی ۱۰۰۰")}
            else OutlinedButton({domain="ARABIC"},Modifier.weight(1f)){Text("عربی ۱۰۰۰")}
        }
        if(card==null) {
            Text("بانک واژگان در حال بارگذاری است.")
            return@Column
        }
        Text("کارت ${index+1} از ${list.size}",color=Color(0xFF667782))
        VocabularyFlashCard(
            card=card,
            isEnglish=domain=="VOCAB",
            onCorrect={
                index=if(index+1<list.size) index+1 else 0
            },
            onWrong={
                val type=if(domain=="VOCAB") "ENGLISH" else "ARABIC"
                vm.scheduleReminder(
                    type,card.id,
                    "مرور ۲۴ ساعته لغت: ${card.title}",
                    "${card.title} → ${card.answer}",
                    24L*60*60*1000
                )
                index=if(index+1<list.size) index+1 else 0
            },
            onCustomReminder={ delay ->
                val type=if(domain=="VOCAB") "ENGLISH" else "ARABIC"
                vm.scheduleReminder(type,card.id,"مرور لغت: ${card.title}","${card.title} → ${card.answer}",delay)
            }
        )
    }
}

@Composable
private fun VocabularyFlashCard(
    card:StudyCardEntity,
    isEnglish:Boolean,
    onCorrect:()->Unit,
    onWrong:()->Unit,
    onCustomReminder:(Long)->Unit,
) {
    var back by remember(card.id){mutableStateOf(false)}
    var reminder by remember {mutableStateOf(false)}
    Card(
        modifier=Modifier.fillMaxWidth().height(330.dp).clickable{back=!back},
        shape=RoundedCornerShape(24.dp),
        colors=CardDefaults.cardColors(containerColor=if(back) Color(0xFFEAF6EC) else Color(0xFFEAF1FA))
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement=Arrangement.Center
        ) {
            Text(if(back) "پشت کارت" else "روی کارت",color=Color(0xFF71818A),fontSize=12.sp)
            Spacer(Modifier.height(18.dp))
            Text(
                if(back) card.answer else card.title,
                style=MaterialTheme.typography.headlineMedium,
                color=if(back) FocusGreen else FocusNavy,
                fontWeight=FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))
            Text(if(back) "معنی فارسی" else if(isEnglish) "برای دیدن معنی روی کارت بزن" else "برای دیدن معنی فارسی روی کارت بزن")
        }
    }
    if(back) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick=onWrong,modifier=Modifier.weight(1f),colors=ButtonDefaults.outlinedButtonColors(contentColor=SoftRed)){
                Text("نادرست")
            }
            Button(onClick=onCorrect,modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=FocusGreen)){
                Text("درست")
            }
        }
        Text(
            "«نادرست» = این لغت خودکار برای ۲۴ ساعت بعد وارد مرور می‌شود.",
            color=Color(0xFF687984),
            fontSize=12.sp
        )
    }
    OutlinedButton(onClick={reminder=true},modifier=Modifier.fillMaxWidth()){Text("⏰ یادآوری سفارشی")}
    if(reminder) ReminderDialog(
        title="لغت ${card.title}",
        onDismiss={reminder=false},
        onSelect={delay->onCustomReminder(delay);reminder=false}
    )
}

@Composable
private fun PlannerScreen(vm:MainViewModel) {
    val context=LocalContext.current
    val tasks by vm.plannerTasks.collectAsStateWithLifecycle()
    val sound by vm.soundUri.collectAsStateWithLifecycle()
    val todayPersian=PersianCalendar.fromGregorian(LocalDate.now()).numeric()
    var title by remember{mutableStateOf("")}
    var details by remember{mutableStateOf("")}
    var date by remember{mutableStateOf(todayPersian)}
    var time by remember{mutableStateOf("20:00")}
    var message by remember{mutableStateOf("")}

    val ringtoneLauncher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
        val uri = if(Build.VERSION.SDK_INT>=33) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        if(uri!=null) vm.setSound(uri.toString())
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item {
            Text("برنامه‌ریز و آلارم شخصی",style=MaterialTheme.typography.headlineSmall,color=FocusNavy)
            Text("کارهای امروز و آینده را با تاریخ شمسی، ساعت، آلارم و نوتیفیکیشن ثبت کن.",color=Color(0xFF667782))
        }
        item {
            Card(colors=CardDefaults.cardColors(containerColor=Color.White)) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text("عنوان کار")})
                    OutlinedTextField(details,{details=it},Modifier.fillMaxWidth(),label={Text("توضیحات")})
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(date,{date=it},Modifier.weight(1f),label={Text("تاریخ شمسی")},placeholder={Text("۱۴۰۵/۰۷/۰۱")})
                        OutlinedTextField(time,{time=it},Modifier.weight(1f),label={Text("ساعت")},placeholder={Text("20:00")})
                    }
                    OutlinedButton(
                        onClick={
                            val intent=Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,false)
                                if(sound.isNotBlank()) putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,Uri.parse(sound))
                            }
                            ringtoneLauncher.launch(intent)
                        },
                        modifier=Modifier.fillMaxWidth()
                    ){Text(if(sound.isBlank()) "انتخاب صدای آلارم" else "تغییر صدای آلارم ✓")}
                    ExactAlarmPermissionButton()
                    Button(
                        onClick={
                            if(title.isBlank()) message="عنوان کار را وارد کن."
                            else {
                                message=vm.addPlannerTask(title,details,date,time) ?: "برنامه و آلارم ثبت شد."
                                if(message.startsWith("برنامه")) { title=""; details="" }
                            }
                        },
                        modifier=Modifier.fillMaxWidth()
                    ){Text("ثبت برنامه و آلارم")}
                    if(message.isNotBlank()) Text(message,color=if(message.startsWith("برنامه"))FocusGreen else SoftRed)
                }
            }
        }
        item { Text("برنامه‌های ثبت‌شده",style=MaterialTheme.typography.titleLarge,color=FocusNavy) }
        items(tasks,key={it.id}) { task->
            Card(colors=CardDefaults.cardColors(containerColor=if(task.completed) Color(0xFFE7F3E9) else Color.White)) {
                Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Text(task.title,fontWeight=FontWeight.Bold,textDecoration=if(task.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough else null)
                    Text("${task.persianDate} • ${task.timeText}",color=FocusBlue)
                    if(task.details.isNotBlank()) Text(task.details,fontSize=13.sp)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Button(onClick={vm.togglePlannerDone(task)}){Text(if(task.completed)"فعال کن" else "انجام شد")}
                        TextButton(onClick={vm.deletePlanner(task)}){Text("حذف",color=SoftRed)}
                    }
                }
            }
        }
    }
}

@Composable
private fun ExactAlarmPermissionButton() {
    val context=LocalContext.current
    if(Build.VERSION.SDK_INT>=31) {
        val am=context.getSystemService(AlarmManager::class.java)
        if(!am.canScheduleExactAlarms()) {
            OutlinedButton(
                onClick={
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data=Uri.parse("package:${context.packageName}")
                            }
                        )
                    }
                },
                modifier=Modifier.fillMaxWidth()
            ){Text("اجازه آلارم دقیق اندروید")}
        }
    }
}
