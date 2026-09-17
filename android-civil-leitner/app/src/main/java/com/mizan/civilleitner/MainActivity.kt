package com.mizan.civilleitner

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mizan.civilleitner.data.ArticleEntity
import com.mizan.civilleitner.data.DailyProgressEntity
import com.mizan.civilleitner.data.StudyCardEntity
import com.mizan.civilleitner.domain.Phd140DayPlan
import com.mizan.civilleitner.domain.StrictReviewScheduler
import com.mizan.civilleitner.worker.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CivilLawRoot() }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as CivilLawApplication
    private val dao = app.database.articleDao()
    private val cardDao = app.database.studyCardDao()
    private val planDao = app.database.planDao()
    private val today = LocalDate.now().toEpochDay()

    val articles = dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dueArticles = dao.observeDue(today).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val total = dao.observeTotalCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val overdue = dao.observeOverdueCount(today).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val studyCards = cardDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dueCards = cardDao.observeDue(today).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dueCount = combine(dueArticles, dueCards) { a, c -> a.size + c.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val completedDays = planDao.observeCompletedDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val calendarDay = Phd140DayPlan.calendarDay()

    val effectiveDay = completedDays.map { completed ->
        (1..calendarDay).firstOrNull { it !in completed } ?: calendarDay
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 1)

    val currentPlan = effectiveDay.map(Phd140DayPlan::planFor)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Phd140DayPlan.planFor(1))

    val dayProgress = effectiveDay.flatMapLatest { planDao.observeDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val searchQuery = MutableStateFlow("")
    val searchArticles = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) dao.observeAll() else dao.search(query.trim())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val searchCards = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) cardDao.observeAll() else cardDao.search(query.trim())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(value: String) { searchQuery.value = value }

    fun activateReview(article: ArticleEntity) {
        viewModelScope.launch {
            val d = StrictReviewScheduler.activate()
            dao.update(article.copy(
                reviewEnabled = d.enabled,
                strictReviewStage = d.stage,
                nextReviewEpochDay = d.nextReviewEpochDay,
                explicitMastered = false,
                masteryLevel = "Review cycle",
                firstStudiedEpochDay = article.firstStudiedEpochDay ?: LocalDate.now().toEpochDay(),
            ))
            ReminderScheduler.refreshNow(app)
        }
    }

    fun completeReview(article: ArticleEntity) {
        viewModelScope.launch {
            val d = StrictReviewScheduler.complete(article.strictReviewStage)
            dao.update(article.copy(
                reviewEnabled = true,
                strictReviewStage = d.stage,
                nextReviewEpochDay = d.nextReviewEpochDay,
                lastReviewEpochDay = LocalDate.now().toEpochDay(),
                reviewCount = article.reviewCount + 1,
                masteryLevel = "Review cycle",
            ))
            ReminderScheduler.refreshNow(app)
        }
    }

    fun master(article: ArticleEntity) {
        viewModelScope.launch {
            val d = StrictReviewScheduler.master()
            dao.update(article.copy(
                reviewEnabled = false,
                explicitMastered = true,
                nextReviewEpochDay = d.nextReviewEpochDay,
                masteryLevel = "Mastered",
            ))
            ReminderScheduler.refreshNow(app)
        }
    }

    fun reactivate(article: ArticleEntity) = activateReview(article)

    fun activateReview(card: StudyCardEntity) {
        viewModelScope.launch {
            val d = StrictReviewScheduler.activate()
            cardDao.update(card.copy(
                reviewEnabled = true,
                strictReviewStage = d.stage,
                nextReviewEpochDay = d.nextReviewEpochDay,
                explicitMastered = false,
                firstStudiedEpochDay = card.firstStudiedEpochDay ?: LocalDate.now().toEpochDay(),
            ))
            ReminderScheduler.refreshNow(app)
        }
    }

    fun completeReview(card: StudyCardEntity) {
        viewModelScope.launch {
            val d = StrictReviewScheduler.complete(card.strictReviewStage)
            cardDao.update(card.copy(
                strictReviewStage = d.stage,
                nextReviewEpochDay = d.nextReviewEpochDay,
                lastReviewEpochDay = LocalDate.now().toEpochDay(),
                reviewCount = card.reviewCount + 1,
            ))
            ReminderScheduler.refreshNow(app)
        }
    }

    fun master(card: StudyCardEntity) {
        viewModelScope.launch {
            val d = StrictReviewScheduler.master()
            cardDao.update(card.copy(
                reviewEnabled = false,
                explicitMastered = true,
                nextReviewEpochDay = d.nextReviewEpochDay,
            ))
            ReminderScheduler.refreshNow(app)
        }
    }

    fun toggleFavorite(article: ArticleEntity) {
        viewModelScope.launch { dao.update(article.copy(favorite = !article.favorite)) }
    }

    fun toggleTask(index: Int, taskCount: Int) {
        if (index == 0) return // Review task is completed only by actually clearing the due queue.
        if (dueCount.value > 0) return // Hard gate: no new-study completion while reviews are due.
        viewModelScope.launch {
            val day = effectiveDay.value
            val old = dayProgress.value ?: DailyProgressEntity(day, 0, false, LocalDate.now().toEpochDay())
            val bit = 1 shl index
            val newMask = if (old.completedMask and bit != 0) old.completedMask and bit.inv() else old.completedMask or bit
            var requiredMask = 0
            for (i in 1 until taskCount) requiredMask = requiredMask or (1 shl i)
            val complete = (newMask and requiredMask) == requiredMask && dueCount.value == 0
            planDao.upsert(old.copy(
                completedMask = newMask,
                dayCompleted = complete,
                updatedEpochDay = LocalDate.now().toEpochDay(),
            ))
        }
    }
}

private enum class Tab(val label: String, val glyph: String) {
    TODAY("امروز", "⌂"),
    MATERIALS("منابع", "§"),
    REVIEW("مرور", "✓"),
    PROGRESS("پیشرفت", "◔"),
    SEARCH("جستجو", "⌕"),
}

@Composable
private fun CivilLawRoot(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val colors = lightColorScheme(
        primary = Color(0xFF102A43),
        onPrimary = Color.White,
        secondary = Color(0xFF486581),
        background = Color(0xFFF7F9FC),
        surface = Color.White,
    )

    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            var selected by remember { mutableStateOf(Tab.TODAY) }
            Scaffold(bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selected == tab,
                            onClick = { selected = tab },
                            icon = { Text(tab.glyph, fontSize = 20.sp) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when (selected) {
                        Tab.TODAY -> TodayScreen(vm) { selected = Tab.REVIEW }
                        Tab.MATERIALS -> MaterialsScreen(vm)
                        Tab.REVIEW -> ReviewScreen(vm)
                        Tab.PROGRESS -> ProgressScreen(vm)
                        Tab.SEARCH -> SearchScreen(vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayScreen(vm: MainViewModel, startReview: () -> Unit) {
    val plan by vm.currentPlan.collectAsStateWithLifecycle()
    val effectiveDay by vm.effectiveDay.collectAsStateWithLifecycle()
    val progress by vm.dayProgress.collectAsStateWithLifecycle()
    val dueCount by vm.dueCount.collectAsStateWithLifecycle()
    val overdue by vm.overdue.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val calendarDay = Phd140DayPlan.calendarDay()
    val behind = (calendarDay - effectiveDay).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("دکتری حقوق خصوصی ۱۴۰۶", style = MaterialTheme.typography.headlineMedium)
            Text("روز $effectiveDay از ۱۴۰ • ${plan.persianLabel}", color = MaterialTheme.colorScheme.secondary)
            if (behind > 0) Text("$behind روز عقب‌افتادگی؛ تا جبران، برنامه جلو نمی‌رود.", color = MaterialTheme.colorScheme.error)
        }
        item { MetricCard("مرحله", plan.phase, "حداقل ${plan.mandatoryMinutes} دقیقه کار واقعی") }
        item { MetricCard("مرور اجباری", dueCount.toString(), if (overdue > 0) "$overdue مورد مدنی عقب‌افتاده" else "صف امروز") }
        item { MetricCard("بانک قانون مدنی", "$total / ۱۳۳۵", if (total == 1335) "کامل و محلی" else "در حال ترمیم") }
        item {
            Button(
                onClick = startReview,
                enabled = dueCount > 0,
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text(if (dueCount == 0) "صف مرور صفر است" else "اول مرور را تمام کن ($dueCount)") }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ماموریت‌های امروز", style = MaterialTheme.typography.titleLarge)
                    plan.tasks.forEachIndexed { index, task ->
                        val checked = if (index == 0) dueCount == 0 else (progress?.completedMask ?: 0) and (1 shl index) != 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { vm.toggleTask(index, plan.tasks.size) },
                                enabled = index > 0 && dueCount == 0,
                            )
                            Text(task, modifier = Modifier.weight(1f))
                        }
                    }
                    if (dueCount > 0) {
                        Text("قفل فعال است: تا مرورها صفر نشوند، تیک مأموریت‌های جدید باز نمی‌شود.", color = MaterialTheme.colorScheme.error)
                    } else if (progress?.dayCompleted == true) {
                        Text("روز $effectiveDay کامل شد. اگر امروز روز تقویمی فعلی است، ادامه اصلی فرداست.", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item { MotivationCard("درس", plan.motivation.study) }
        item { MotivationCard("سلامتی", plan.motivation.health) }
        item { MotivationCard("ترک سیگار", plan.motivation.smoking) }
        item { MotivationCard("خانواده", plan.motivation.family) }
        item { MotivationCard("خودسازی و پیشرفت", plan.motivation.growth) }
        item {
            Text(
                "روز آزمون: ۱۶ بهمن ۱۴۰۵. برنامه ۱۴۰ روزه در ۱۵ بهمن پایان می‌یابد.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun MotivationCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            Text(body)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, subtitle: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MaterialsScreen(vm: MainViewModel) {
    val articles by vm.articles.collectAsStateWithLifecycle()
    val cards by vm.studyCards.collectAsStateWithLifecycle()
    var domain by remember { mutableStateOf("CIVIL") }
    val domains = listOf("CIVIL" to "مدنی", "TRADE" to "تجارت", "FIQH" to "متون فقه", "VOCAB" to "زبان")

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            domains.forEach { (key, label) ->
                if (domain == key) Button(onClick = { domain = key }, modifier = Modifier.weight(1f)) { Text(label) }
                else OutlinedButton(onClick = { domain = key }, modifier = Modifier.weight(1f)) { Text(label) }
            }
        }
        if (domain == "CIVIL") {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                item { Text("قانون مدنی — ۱۳۳۵ ماده", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(8.dp)) }
                items(articles, key = { it.articleNumber }) { article ->
                    CivilMaterialCard(article, vm)
                }
            }
        } else {
            val filtered = cards.filter { it.domain == domain }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                item { Text(domains.first { it.first == domain }.second, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(8.dp)) }
                if (filtered.isEmpty()) item {
                    Text("بانک این منبع هنوز از خط لوله اعتبارسنجی وارد نشده است.", Modifier.padding(20.dp))
                }
                items(filtered, key = { it.id }) { card -> StudyMaterialCard(card, vm) }
            }
        }
    }
}

@Composable
private fun CivilMaterialCard(article: ArticleEntity, vm: MainViewModel) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ماده ${article.articleNumber}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.toggleFavorite(article) }) { Text(if (article.favorite) "★" else "☆") }
            }
            Text(article.officialText)
            if (article.simpleExplanation.isNotBlank()) {
                Text("توضیح ساده", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                Text(article.simpleExplanation)
            }
            if (article.analyticalPoint.isNotBlank()) {
                Text("نکته تحلیلی", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                Text(article.analyticalPoint)
            }
            if (article.explicitMastered) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.reactivate(article) }) { Text("بازگشت به مرور") }
                    Text("مسلط ✓", modifier = Modifier.padding(top = 12.dp))
                }
            } else if (article.reviewEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("در چرخه مرور • مرحله ${article.strictReviewStage + 1}", modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { vm.master(article) }) { Text("مسلط شدم") }
                }
            } else {
                Button(onClick = { vm.activateReview(article) }, modifier = Modifier.fillMaxWidth()) { Text("نیاز به مرور") }
            }
            Text("منبع رسمی: Qavanin.ir", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun StudyMaterialCard(card: StudyCardEntity, vm: MainViewModel) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(card.title, style = MaterialTheme.typography.titleMedium)
            Text(card.prompt)
            if (card.answer.isNotBlank()) Text(card.answer)
            if (card.explanation.isNotBlank()) Text(card.explanation, color = MaterialTheme.colorScheme.secondary)
            when {
                card.explicitMastered -> Button(onClick = { vm.activateReview(card) }) { Text("بازگشت به مرور") }
                card.reviewEnabled -> OutlinedButton(onClick = { vm.master(card) }) { Text("مسلط شدم") }
                else -> Button(onClick = { vm.activateReview(card) }) { Text("نیاز به مرور") }
            }
            if (card.sourceName.isNotBlank()) Text(card.sourceName, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReviewScreen(vm: MainViewModel) {
    val dueArticles by vm.dueArticles.collectAsStateWithLifecycle()
    val dueCards by vm.dueCards.collectAsStateWithLifecycle()
    val article = dueArticles.firstOrNull()
    val card = if (article == null) dueCards.firstOrNull() else null
    val totalDue = dueArticles.size + dueCards.size
    var revealedKey by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("مرور اجباری امروز", style = MaterialTheme.typography.headlineSmall)
        if (article == null && card == null) {
            Spacer(Modifier.height(24.dp))
            Text("صف مرور صفر شد. حالا محتوای جدید امروز باز است.")
            return@Column
        }
        Text("$totalDue مورد در صف", color = MaterialTheme.colorScheme.secondary)
        LinearProgressIndicator(progress = { 1f / totalDue.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())

        if (article != null) {
            val key = "CIVIL:${article.articleNumber}"
            val revealed = revealedKey == key
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ماده ${article.articleNumber}", style = MaterialTheme.typography.titleLarge)
                    Text(article.recallQuestion.ifBlank { "حکم و مفهوم ماده را بدون نگاه کردن بازگو کن." })
                    if (!revealed) Button(onClick = { revealedKey = key }, modifier = Modifier.fillMaxWidth()) { Text("نمایش پاسخ") }
                    else {
                        HorizontalDivider()
                        Text(article.officialText)
                        if (article.simpleExplanation.isNotBlank()) Text(article.simpleExplanation)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.completeReview(article); revealedKey = "" }, modifier = Modifier.weight(1f)) { Text("مرور انجام شد") }
                            OutlinedButton(onClick = { vm.master(article); revealedKey = "" }, modifier = Modifier.weight(1f)) { Text("مسلط شدم") }
                        }
                    }
                }
            }
        } else if (card != null) {
            val key = card.id
            val revealed = revealedKey == key
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(card.title, style = MaterialTheme.typography.titleLarge)
                    Text(card.prompt)
                    if (!revealed) Button(onClick = { revealedKey = key }, modifier = Modifier.fillMaxWidth()) { Text("نمایش پاسخ") }
                    else {
                        HorizontalDivider()
                        Text(card.answer)
                        if (card.explanation.isNotBlank()) Text(card.explanation)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.completeReview(card); revealedKey = "" }, modifier = Modifier.weight(1f)) { Text("مرور انجام شد") }
                            OutlinedButton(onClick = { vm.master(card); revealedKey = "" }, modifier = Modifier.weight(1f)) { Text("مسلط شدم") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressScreen(vm: MainViewModel) {
    val articles by vm.articles.collectAsStateWithLifecycle()
    val cards by vm.studyCards.collectAsStateWithLifecycle()
    val dueCount by vm.dueCount.collectAsStateWithLifecycle()
    val effectiveDay by vm.effectiveDay.collectAsStateWithLifecycle()
    val masteredCivil = articles.count { it.explicitMastered }
    val masteredCards = cards.count { it.explicitMastered }

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("پیشرفت", style = MaterialTheme.typography.headlineSmall) }
        item { MetricCard("روز برنامه", "$effectiveDay / ۱۴۰", "آزمون: ۱۶ بهمن ۱۴۰۵") }
        item { MetricCard("قانون مدنی", "${articles.size} / ۱۳۳۵", "$masteredCivil ماده با تسلط صریح") }
        item { MetricCard("کارت‌های تجارت/فقه/زبان", cards.size.toString(), "$masteredCards کارت مسلط") }
        item { MetricCard("مرور باقی‌مانده امروز", dueCount.toString(), "تا صفر نشود محتوای جدید قفل است") }
    }
}

@Composable
private fun SearchScreen(vm: MainViewModel) {
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    val articles by vm.searchArticles.collectAsStateWithLifecycle()
    val cards by vm.searchCards.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("جستجو در همه منابع", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = vm::setSearchQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("ماده، واژه، فقه یا تجارت") },
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            if (query.isNotBlank()) {
                items(articles.take(50), key = { "a:${it.articleNumber}" }) { article ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Text("مدنی — ماده ${article.articleNumber}", style = MaterialTheme.typography.titleMedium)
                        Text(article.officialText, maxLines = 3)
                    }
                    HorizontalDivider()
                }
                items(cards.take(50), key = { "c:${it.id}" }) { card ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Text("${card.domain} — ${card.title}", style = MaterialTheme.typography.titleMedium)
                        Text(card.prompt, maxLines = 3)
                    }
                    HorizontalDivider()
                }
            } else item { Text("عبارت جستجو را وارد کن.", Modifier.padding(20.dp)) }
        }
    }
}
