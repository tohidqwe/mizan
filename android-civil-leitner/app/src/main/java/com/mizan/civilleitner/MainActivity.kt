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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mizan.civilleitner.data.ArticleEntity
import com.mizan.civilleitner.domain.LeitnerScheduler
import com.mizan.civilleitner.domain.ReviewResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
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
    private val dao = (application as CivilLawApplication).database.articleDao()
    private val today = LocalDate.now().toEpochDay()

    val articles = dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val due = dao.observeDue(today).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val total = dao.observeTotalCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val overdue = dao.observeOverdueCount(today).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val searchQuery = MutableStateFlow("")
    val searchResults = searchQuery
        .flatMapLatest { query -> if (query.isBlank()) dao.observeAll() else dao.search(query.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(value: String) { searchQuery.value = value }

    fun answer(article: ArticleEntity, result: ReviewResult) {
        viewModelScope.launch {
            val decision = LeitnerScheduler.schedule(article.reviewBox, result)
            val correct = result == ReviewResult.KNEW
            val mastery = when {
                result == ReviewResult.DONT_KNOW -> "Learning"
                decision.newBox >= 6 && article.reviewBox == 6 -> "Mastered"
                decision.newBox >= 5 -> "Strong"
                decision.newBox >= 3 -> "Familiar"
                else -> "Learning"
            }
            dao.update(
                article.copy(
                    reviewBox = decision.newBox,
                    nextReviewEpochDay = decision.nextReviewEpochDay,
                    lastReviewEpochDay = LocalDate.now().toEpochDay(),
                    reviewCount = article.reviewCount + 1,
                    correctCount = article.correctCount + if (correct) 1 else 0,
                    incorrectCount = article.incorrectCount + if (result == ReviewResult.DONT_KNOW) 1 else 0,
                    masteryLevel = mastery,
                )
            )
        }
    }

    fun toggleFavorite(article: ArticleEntity) {
        viewModelScope.launch { dao.update(article.copy(favorite = !article.favorite)) }
    }
}

private enum class Tab(val label: String, val glyph: String) {
    TODAY("امروز", "⌂"),
    ARTICLES("مواد", "§"),
    REVIEW("مرور", "✓"),
    PROGRESS("پیشرفت", "◔"),
    SEARCH("جستجو", "⌕"),
}

@Composable
private fun CivilLawRoot(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
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
            Scaffold(
                bottomBar = {
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
                }
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when (selected) {
                        Tab.TODAY -> TodayScreen(vm) { selected = Tab.REVIEW }
                        Tab.ARTICLES -> ArticlesScreen(vm)
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
    val due by vm.due.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val overdue by vm.overdue.collectAsStateWithLifecycle()
    val articles by vm.articles.collectAsStateWithLifecycle()
    val mastered = articles.count { it.masteryLevel == "Mastered" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("حافظ قانون مدنی", style = MaterialTheme.typography.headlineMedium)
            Text("مرورهای سررسیدشده اول؛ ماده جدید بعد", color = MaterialTheme.colorScheme.secondary)
        }
        item { MetricCard("مرور سررسید", due.size.toString(), if (overdue > 0) "$overdue مورد عقب‌افتاده" else "بدون عقب‌افتادگی") }
        item { MetricCard("پوشش پایگاه", "$total / ۱۳۳۵", if (total == 1335) "کامل" else "دیتاست نهایی هنوز تکمیل نشده") }
        item { MetricCard("تسلط", "$mastered ماده", "Mastered") }
        item {
            Button(
                onClick = startReview,
                enabled = due.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text(if (due.isEmpty()) "مروری سررسید نشده" else "شروع مرور امروز (${due.size})") }
        }
        if (total == 0) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "بانک رسمی قانون هنوز وارد نشده است. موتور مرور آماده است، اما برای جلوگیری از نمایش متن حقوقی تأییدنشده، داده ساختگی در نسخه اولیه قرار نگرفته.",
                        Modifier.padding(16.dp),
                    )
                }
            }
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
private fun ArticlesScreen(vm: MainViewModel) {
    val articles by vm.articles.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Text("مواد قانون مدنی", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(vertical = 16.dp))
        }
        items(articles, key = { it.articleNumber }) { article ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("ماده ${article.articleNumber}", style = MaterialTheme.typography.titleMedium)
                    Text(article.officialText, maxLines = 3)
                }
                TextButton(onClick = { vm.toggleFavorite(article) }) { Text(if (article.favorite) "★" else "☆") }
            }
            HorizontalDivider()
        }
        if (articles.isEmpty()) item { EmptyDataMessage() }
    }
}

@Composable
private fun ReviewScreen(vm: MainViewModel) {
    val due by vm.due.collectAsStateWithLifecycle()
    val article = due.firstOrNull()
    var revealedFor by remember { mutableIntStateOf(-1) }
    val revealed = article?.articleNumber == revealedFor

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("مرور", style = MaterialTheme.typography.headlineSmall)
        if (article == null) {
            Spacer(Modifier.height(24.dp))
            Text("همه مرورهای سررسیدشده انجام شده‌اند.")
            return@Column
        }

        Text("${due.size} ماده در صف", color = MaterialTheme.colorScheme.secondary)
        LinearProgressIndicator(
            progress = { 1f / due.size.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth(),
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ماده ${article.articleNumber}", style = MaterialTheme.typography.titleLarge)
                Text(article.recallQuestion.ifBlank { "مفهوم و حکم این ماده را به یاد بیاورید." })
                if (!revealed) {
                    Button(onClick = { revealedFor = article.articleNumber }, modifier = Modifier.fillMaxWidth()) {
                        Text("نمایش پاسخ")
                    }
                } else {
                    HorizontalDivider()
                    Text("متن رسمی", style = MaterialTheme.typography.titleMedium)
                    Text(article.officialText)
                    if (article.simpleExplanation.isNotBlank()) {
                        Text("توضیح ساده", style = MaterialTheme.typography.titleMedium)
                        Text(article.simpleExplanation)
                    }
                    if (article.importantPoints.isNotBlank()) {
                        Text("نکات مهم", style = MaterialTheme.typography.titleMedium)
                        Text(article.importantPoints)
                    }
                }
            }
        }

        if (revealed) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { vm.answer(article, ReviewResult.DONT_KNOW); revealedFor = -1 },
                    modifier = Modifier.weight(1f),
                ) { Text("نمی‌دانستم") }
                OutlinedButton(
                    onClick = { vm.answer(article, ReviewResult.HARD); revealedFor = -1 },
                    modifier = Modifier.weight(1f),
                ) { Text("سخت بود") }
                Button(
                    onClick = { vm.answer(article, ReviewResult.KNEW); revealedFor = -1 },
                    modifier = Modifier.weight(1f),
                ) { Text("بلد بودم") }
            }
        }
    }
}

@Composable
private fun ProgressScreen(vm: MainViewModel) {
    val articles by vm.articles.collectAsStateWithLifecycle()
    val due by vm.due.collectAsStateWithLifecycle()
    val total = articles.size
    val mastered = articles.count { it.masteryLevel == "Mastered" }
    val strong = articles.count { it.masteryLevel == "Strong" }
    val learning = articles.count { it.masteryLevel == "Learning" }
    val coverage = if (total == 0) 0f else total / 1335f

    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("پیشرفت", style = MaterialTheme.typography.headlineSmall) }
        item { MetricCard("کل مواد موجود", "$total / ۱۳۳۵", "پوشش ${(coverage * 100).toInt()}٪") }
        item { MetricCard("در حال یادگیری", learning.toString(), "Learning") }
        item { MetricCard("قوی", strong.toString(), "Strong") }
        item { MetricCard("تثبیت‌شده", mastered.toString(), "Mastered") }
        item { MetricCard("مرور باقی‌مانده امروز", due.size.toString(), "Due + Overdue") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(vm: MainViewModel) {
    val query by vm.searchQuery.collectAsStateWithLifecycle()
    val results by vm.searchResults.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("جستجو", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = vm::setSearchQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("شماره ماده، متن یا کلیدواژه") },
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(results, key = { it.articleNumber }) { article ->
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Text("ماده ${article.articleNumber}", style = MaterialTheme.typography.titleMedium)
                    Text(article.officialText, maxLines = 3)
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun EmptyDataMessage() {
    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Text("هنوز ماده تأییدشده‌ای در بانک محلی وجود ندارد.")
    }
}
