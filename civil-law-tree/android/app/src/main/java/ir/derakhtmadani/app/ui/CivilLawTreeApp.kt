package ir.derakhtmadani.app.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.derakhtmadani.app.BuildConfig
import ir.derakhtmadani.app.data.ContentRepository
import ir.derakhtmadani.app.data.StudyStateStore
import ir.derakhtmadani.app.model.CivilArticle
import ir.derakhtmadani.app.model.CivilCourse

private sealed interface Screen {
    data object Home : Screen
    data class Course(val id: Int) : Screen
    data object Search : Screen
    data class Article(val key: String) : Screen
    data object Premium : Screen
}

private val courses = listOf(
    CivilCourse(1, "مدنی ۱ — اشخاص و محجورین", "کلیات، شخصیت، اهلیت، اقامتگاه، غایب، حجر، ولایت و قیمومت"),
    CivilCourse(2, "مدنی ۲ — اموال و مالکیت", "اموال، مالکیت، انتفاع، ارتفاق و اسباب تملک"),
    CivilCourse(3, "مدنی ۳ — قراردادها و تعهدات", "قواعد عمومی قراردادها، شروط، آثار، سقوط تعهدات و ادله مشترک"),
    CivilCourse(4, "مدنی ۴ — مسئولیت مدنی", "ضمان قهری، ایفای ناروا، غصب، اتلاف، تسبیب و استیفا"),
    CivilCourse(5, "مدنی ۵ — خانواده", "نکاح، مهر، نفقه، انحلال نکاح، نسب و حضانت"),
    CivilCourse(6, "مدنی ۶ — عقود معین ۱", "بیع، معاوضه، اجاره، قرض، جعاله، صلح و خیارات"),
    CivilCourse(7, "مدنی ۷ — عقود معین ۲", "شرکت، مضاربه، مزارعه، مساقات، عقود اذنی و تضمینی"),
    CivilCourse(8, "مدنی ۸ — شفعه، وصیت و ارث", "شفعه، وصیت، ترکه، طبقات و موانع ارث"),
)

@Composable
fun CivilLawTreeApp() {
    val context = LocalContext.current
    var articles by remember { mutableStateOf<List<CivilArticle>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val state = remember { StudyStateStore(context) }

    LaunchedEffect(Unit) {
        runCatching { ContentRepository.load(context) }
            .onSuccess { articles = it }
            .onFailure { loadError = it.message ?: "خطای ناشناخته" }
    }

    val palette = lightColorScheme(
        primary = Color(0xFF173C33),
        onPrimary = Color.White,
        secondary = Color(0xFF9B742C),
        background = Color(0xFFF7F4EC),
        surface = Color(0xFFFFFCF5),
        error = Color(0xFF8E2F2F),
    )

    MaterialTheme(colorScheme = palette) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                when {
                    loadError != null -> ErrorScreen(loadError!!)
                    articles.isEmpty() -> LoadingScreen()
                    else -> Scaffold { padding ->
                        Box(Modifier.padding(padding).fillMaxSize()) {
                            when (val s = screen) {
                                Screen.Home -> HomeScreen(
                                    articles = articles,
                                    state = state,
                                    openCourse = { screen = Screen.Course(it) },
                                    openSearch = { screen = Screen.Search },
                                    openArticle = { screen = Screen.Article(it) },
                                    openPremium = { screen = Screen.Premium },
                                )
                                is Screen.Course -> CourseScreen(
                                    course = courses.first { it.id == s.id },
                                    articles = articles.filter { it.courseId == s.id },
                                    state = state,
                                    back = { screen = Screen.Home },
                                    openArticle = { article ->
                                        if (canOpen(article)) screen = Screen.Article(article.articleKey)
                                        else screen = Screen.Premium
                                    },
                                )
                                Screen.Search -> SearchScreen(
                                    articles = articles,
                                    back = { screen = Screen.Home },
                                    openArticle = { article ->
                                        if (canOpen(article)) screen = Screen.Article(article.articleKey)
                                        else screen = Screen.Premium
                                    },
                                )
                                is Screen.Article -> ArticleScreen(
                                    article = articles.first { it.articleKey == s.key },
                                    articleIndex = articles.associateBy { it.articleKey },
                                    state = state,
                                    back = { screen = Screen.Home },
                                    openRelated = { key -> if (articles.any { it.articleKey == key }) screen = Screen.Article(key) },
                                )
                                Screen.Premium -> PremiumScreen(back = { screen = Screen.Home })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun canOpen(article: CivilArticle): Boolean {
    if (BuildConfig.DEBUG) return true
    return article.courseId == 1 || article.articleNumber == 190
}

@Composable
private fun LoadingScreen() = Box(Modifier.fillMaxSize().padding(24.dp)) {
    Text("در حال ساخت درخت ۱۳۳۵ ماده از منبع رسمی…", style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun ErrorScreen(message: String) = Column(Modifier.fillMaxSize().padding(24.dp)) {
    Text("بارگذاری محتوا انجام نشد", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
    Spacer(Modifier.height(8.dp))
    Text(message)
}

@Composable
private fun HomeScreen(
    articles: List<CivilArticle>,
    state: StudyStateStore,
    openCourse: (Int) -> Unit,
    openSearch: () -> Unit,
    openArticle: (String) -> Unit,
    openPremium: () -> Unit,
) {
    val main = articles.count { it.suffix.isBlank() }
    val supplements = articles.size - main
    val studied = state.studiedCount(articles.map { it.articleKey })

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("درخت قانون مدنی", style = MaterialTheme.typography.headlineMedium)
            Text("نقشه ذهنی جامع حقوق مدنی ایران", color = MaterialTheme.colorScheme.secondary)
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("درخت کلان قانون", style = MaterialTheme.typography.titleLarge)
                    Text("${main} ماده اصلی${if (supplements > 0) " + $supplements مقرره مکرر" else ""}")
                    Text("${studied} مورد وارد مسیر مطالعه شده")
                    Text("ریشه ← مدنی ۱ تا ۸ ← موضوع ← زیرموضوع ← ماده", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = openSearch, modifier = Modifier.weight(1f)) { Text("جستجوی حرفه‌ای") }
                OutlinedButton(onClick = openPremium, modifier = Modifier.weight(1f)) { Text("اشتراک") }
            }
        }
        item { Text("مدنی ۱ تا ۸", style = MaterialTheme.typography.titleLarge) }
        items(courses) { course ->
            val count = articles.count { it.courseId == course.id }
            val progress = state.studiedCount(articles.filter { it.courseId == course.id }.map { it.articleKey })
            Card(Modifier.fillMaxWidth().clickable { openCourse(course.id) }) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(course.name, style = MaterialTheme.typography.titleMedium)
                    Text(course.description, style = MaterialTheme.typography.bodySmall)
                    Text("${count} ماده/مقرره • مطالعه‌شده ${progress}", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().clickable { openArticle("190") }) {
                Column(Modifier.padding(15.dp)) {
                    Text("نمونه قفل ذهنی", style = MaterialTheme.typography.titleMedium)
                    Text("ماده ۱۹۰ — شرایط اساسی صحت معامله")
                    Text("از ماده به مفهوم، عناصر و درخت ذهنی", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun CourseScreen(
    course: CivilCourse,
    articles: List<CivilArticle>,
    state: StudyStateStore,
    back: () -> Unit,
    openArticle: (CivilArticle) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            TextButton(onClick = back) { Text("بازگشت") }
            Text(course.name, style = MaterialTheme.typography.headlineSmall)
            Text(course.description, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            Text("${articles.size} ماده در این مسیر آموزشی")
        }
        items(articles, key = { it.articleKey }) { article ->
            Card(Modifier.fillMaxWidth().clickable { openArticle(article) }) {
                Row(Modifier.padding(14.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("ماده ${article.articleKey} — ${article.title}", style = MaterialTheme.typography.titleSmall)
                        Text("${article.topic}${if (article.subtopic.isNotBlank()) " ← ${article.subtopic}" else ""}", style = MaterialTheme.typography.bodySmall)
                        if (article.legalStatus != "ACTIVE") Text(article.legalStatus, color = MaterialTheme.colorScheme.error)
                    }
                    Text(
                        when (state.status(article.articleKey)) {
                            "MASTERED" -> "✓"
                            "HARD" -> "!"
                            "LEARNING" -> "◐"
                            else -> "○"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(
    articles: List<CivilArticle>,
    back: () -> Unit,
    openArticle: (CivilArticle) -> Unit,
) {
    var q by remember { mutableStateOf("") }
    val normalized = q.trim()
    val results = if (normalized.isBlank()) emptyList() else articles.filter {
        it.articleKey.contains(normalized) ||
            it.officialText.contains(normalized, ignoreCase = true) ||
            it.topic.contains(normalized, ignoreCase = true) ||
            it.subtopic.contains(normalized, ignoreCase = true) ||
            it.title.contains(normalized, ignoreCase = true) ||
            it.courseName.contains(normalized, ignoreCase = true)
    }.take(80)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = back) { Text("بازگشت") }
        Text("جستجوی قانون و مفهوم", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = q,
            onValueChange = { q = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("شماره ماده، اکراه، خیار، ارث، موضوع…") },
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(results, key = { it.articleKey }) { article ->
                Card(Modifier.fillMaxWidth().clickable { openArticle(article) }) {
                    Column(Modifier.padding(12.dp)) {
                        Text("ماده ${article.articleKey} — ${article.title}", style = MaterialTheme.typography.titleSmall)
                        Text(article.courseName, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                        Text(article.officialText, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleScreen(
    article: CivilArticle,
    articleIndex: Map<String, CivilArticle>,
    state: StudyStateStore,
    back: () -> Unit,
    openRelated: (String) -> Unit,
) {
    var starred by remember(article.articleKey) { mutableStateOf(state.isStarred(article.articleKey)) }
    var status by remember(article.articleKey) { mutableStateOf(state.status(article.articleKey)) }
    val allowDraft = BuildConfig.ALLOW_DRAFT_CONTENT || article.approvalStatus == "APPROVED"

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = back) { Text("بازگشت") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { starred = state.toggleStar(article.articleKey) }) { Text(if (starred) "★" else "☆") }
            }
            Text("ماده ${article.articleKey}", style = MaterialTheme.typography.headlineMedium)
            Text(article.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
            Text("${article.courseName} ← ${article.topic}${if (article.subtopic.isNotBlank()) " ← ${article.subtopic}" else ""}")
            if (article.legalStatus != "ACTIVE") {
                Text("وضعیت رسمی: ${article.legalStatus}", color = MaterialTheme.colorScheme.error)
            }
        }
        item {
            SectionCard("متن رسمی ماده") {
                Text(article.officialText)
                Spacer(Modifier.height(8.dp))
                Text(article.sourceLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
        item {
            SectionCard("درخت ذهنی ماده") {
                if (allowDraft) MindMapCanvas(article.mindNodes)
                else Text("این درخت تا تأیید Legal Review در نسخه تجاری نمایش داده نمی‌شود.")
            }
        }
        if (allowDraft) {
            item { ExpandableTextSection("این ماده به زبان ساده چه می‌گوید؟", article.simpleExplanation) }
            item { ExpandableTextSection("تحلیل دانشگاهی", article.academicAnalysis) }
            item { ExpandableTextSection("منطق و فلسفه حکم", article.philosophy) }
            item { ListSection("عناصر و ارکان", article.elements) }
            item { ListSection("شرایط", article.conditions) }
            item { ListSection("آثار", article.effects) }
            item { ListSection("استثناها", article.exceptions) }
            item { ExpandableTextSection("کلید حافظه", article.memoryCue) }
            item { ExpandableTextSection("دام مفهومی/امتحانی", article.examTrap) }
            item { ListSection("Active Recall", article.activeRecall) }
        }
        item {
            SectionCard("نظریات حقوق‌دانان") {
                Text("هیچ دیدگاهی بدون منبع معتبر به استادان نسبت داده نشده است.")
                Text("این بخش پس از ثبت منبع، Legal Review و وضعیت Approved منتشر می‌شود.", color = MaterialTheme.colorScheme.secondary)
            }
        }
        item {
            SectionCard("مواد مرتبط") {
                if (article.relatedArticleKeys.isEmpty()) Text("هنوز رابطه تأییدشده‌ای ثبت نشده است.")
                article.relatedArticleKeys.forEach { key ->
                    val related = articleIndex[key]
                    TextButton(onClick = { openRelated(key) }, enabled = related != null) {
                        Text(if (related != null) "ماده $key — ${related.title}" else "ماده $key")
                    }
                }
            }
        }
        item {
            SectionCard("وضعیت مطالعه") {
                val choices = listOf(
                    "UNSEEN" to "مطالعه نشده",
                    "LEARNING" to "در حال یادگیری",
                    "UNDERSTOOD" to "فهمیدم",
                    "REVIEW" to "نیاز به مرور",
                    "HARD" to "دشوار",
                    "MASTERED" to "مسلط شدم",
                )
                choices.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (key, label) ->
                            if (status == key) Button(
                                onClick = { status = key; state.setStatus(article.articleKey, key) },
                                modifier = Modifier.weight(1f)
                            ) { Text(label) }
                            else OutlinedButton(
                                onClick = { status = key; state.setStatus(article.articleKey, key) },
                                modifier = Modifier.weight(1f)
                            ) { Text(label) }
                        }
                    }
                }
            }
        }
        if (article.approvalStatus != "APPROVED") {
            item {
                Text(
                    "نسخه داخلی: تحلیل و Mind Map این ماده وضعیت ${article.approvalStatus} دارد و برای انتشار تجاری نیازمند Legal Review است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun ExpandableTextSection(title: String, body: String) {
    var expanded by remember { mutableStateOf(false) }
    SectionCard(title) {
        Text(if (expanded || body.length < 260) body else body.take(260) + "…")
        if (body.length >= 260) TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "بستن" else "ادامه")
        }
    }
}

@Composable
private fun ListSection(title: String, values: List<String>) {
    if (values.isEmpty()) return
    SectionCard(title) {
        values.forEach { Text("• $it") }
    }
}

@Composable
private fun PremiumScreen(back: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = back) { Text("بازگشت") }
        Text("درخت قانون مدنی Premium", style = MaterialTheme.typography.headlineSmall)
        Text("نسخه تجاری از سمت سرور مجوز دسترسی را بررسی می‌کند؛ اعتماد به Client ممنوع است.")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Premium شامل:", style = MaterialTheme.typography.titleMedium)
                Text("تمام مدنی ۱ تا ۸ • تمام Mind Treeها • تحلیل پیشرفته • مسیر مطالعه • مرور هوشمند")
            }
        }
        Button(onClick = { }, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("پرداخت پس از اتصال Merchant / Store فعال می‌شود")
        }
    }
}
