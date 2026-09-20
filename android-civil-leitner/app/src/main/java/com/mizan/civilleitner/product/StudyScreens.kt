package com.mizan.civilleitner.product

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mizan.civilleitner.data.ArticleEntity
import com.mizan.civilleitner.data.ReminderEntity
import com.mizan.civilleitner.data.StudyCardEntity
import com.mizan.civilleitner.domain.ProductReviewPolicy

private val LawIvory = Color(0xFFF8F3E8)
private val LawBlue = Color(0xFF1565C0)
private val LawGreen = Color(0xFF16855B)

@Composable
fun PhdCourseScreen(vm: ProductViewModel, navigate: (ProductScreen) -> Unit) {
    val demo by vm.demoActive.collectAsStateWithLifecycle()
    ScreenList {
        item {
            HeroCard(
                "آمادگی کنکور دکتری حقوق خصوصی",
                "مدنی • تجارت • متون فقه • زبان انگلیسی",
                if (demo) "نسخه آزمایشی ۷۲ ساعته فعال است." else "دسترسی کامل فعال است.",
            )
        }
        if (demo) {
            item {
                InfoCard(
                    "برنامه مطالعاتی ۷۲ ساعته",
                    "روز اول: ارزیابی اولیه + مدنی پایه + ۳۰ لغت انگلیسی.\n" +
                        "روز دوم: تجارت + متون فقه معاملات + مرور سررسیدها.\n" +
                        "روز سوم: مدنی/تجارت ترکیبی + فلش‌کارت زبان + جمع‌بندی و سنجش نقاط ضعف.",
                )
            }
        }
        item { CourseSectionCard("حقوق مدنی", "متن رسمی قانون مدنی، وضعیت مواد و Reminder مستقل") { navigate(ProductScreen.CIVIL) } }
        item { CourseSectionCard("حقوق تجارت", "قانون تجارت و لایحه اصلاحی با تفکیک منبع") { navigate(ProductScreen.TRADE) } }
        item { CourseSectionCard("متون فقه — معاملات", "بیع، خیارات، اجاره، ضمان، رهن، وکالت و سایر ابواب") { navigate(ProductScreen.FIQH) } }
        item { CourseSectionCard("زبان انگلیسی", "فلش‌کارت English → Persian؛ درست = ۳ روز، نادرست = ۲۴ ساعت") { navigate(ProductScreen.ENGLISH) } }
    }
}

@Composable
fun CivilLawScreen(vm: ProductViewModel) {
    val articles by vm.allArticles.collectAsStateWithLifecycle()
    var reminderFor by remember { mutableStateOf<ArticleEntity?>(null) }
    ScreenList {
        item {
            SectionHeader(
                "قانون مدنی",
                "تمام شماره‌های موجود در بانک، شامل مواد منسوخ با برچسب وضعیت. متن رسمی تغییر نمی‌کند.",
            )
        }
        items(articles, key = { it.articleNumber }) { article ->
            LawArticleCard(
                article = article,
                onReminder = { reminderFor = article },
            )
        }
    }
    reminderFor?.let { article ->
        ReminderIntervalDialog(
            title = "ماده " + article.articleNumber,
            onDismiss = { reminderFor = null },
            onChoose = { code ->
                vm.scheduleManualReminder(
                    contentType = "CIVIL_ARTICLE",
                    contentId = article.articleNumber.toString(),
                    title = "ماده " + article.articleNumber + " قانون مدنی",
                    intervalCode = code,
                )
                reminderFor = null
            },
        )
    }
}

@Composable
private fun LawArticleCard(
    article: ArticleEntity,
    onReminder: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LawIvory),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                buildAnnotatedString {
                    pushStyle(SpanStyle(color = Color.Black, fontWeight = FontWeight.SemiBold))
                    append("ماده ")
                    pop()
                    pushStyle(SpanStyle(color = LawBlue, fontWeight = FontWeight.Bold))
                    append(article.articleNumber.toString())
                    pop()
                },
                style = MaterialTheme.typography.titleLarge,
            )
            if (article.topic == "ماده منسوخ") {
                Text(
                    "وضعیت: منسوخ",
                    color = Color(0xFFB23A3A),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(
                highlightedLawText(article.officialText, article.keywords),
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (article.source1.isNotBlank()) {
                Text(
                    "منبع: " + article.source1,
                    color = Color(0xFF5F6368),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            HorizontalDivider(color = Color(0xFFD8D1C4))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReminder) { Text("یادآوری") }
                OutlinedButton(onClick = { shareLawArticle(context, article) }) { Text("اشتراک با AI") }
            }
        }
    }
}

private fun highlightedLawText(text: String, keywordsRaw: String) = buildAnnotatedString {
    append(text)
    val keywords = keywordsRaw.split("|").map { it.trim() }.filter { it.length > 1 }.distinct()
    keywords.forEach { keyword ->
        var start = text.indexOf(keyword)
        while (start >= 0) {
            addStyle(
                SpanStyle(color = LawGreen, fontWeight = FontWeight.Bold),
                start,
                start + keyword.length,
            )
            start = text.indexOf(keyword, start + keyword.length)
        }
    }
}

private fun shareLawArticle(context: android.content.Context, article: ArticleEntity) {
    val prompt =
        "ماده قانونی زیر را با حفظ دقت حقوقی، به زبان ساده تفهیم و ساده‌سازی کن. " +
            "ابتدا مفهوم اصلی ماده را توضیح بده، سپس اجزای آن را تفکیک کن و در پایان یک مثال کوتاه بزن. " +
            "چیزی خارج از متن ماده را به قانون نسبت نده.\n\n" +
            "ماده " + article.articleNumber + " قانون مدنی:\n" + article.officialText

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, prompt)
    }
    context.startActivity(Intent.createChooser(intent, "ارسال به Gemini یا DeepSeek"))
}

@Composable
fun TradeScreen(vm: ProductViewModel) {
    val cards by vm.cards.collectAsStateWithLifecycle()
    val trade = remember(cards) { cards.filter { it.domain == "TRADE" } }
    var reminderFor by remember { mutableStateOf<StudyCardEntity?>(null) }
    ScreenList {
        item { SectionHeader("حقوق تجارت", "محتوای تجارت بر اساس منبع و شناسه مستقل نگهداری می‌شود.") }
        if (trade.isEmpty()) item { InfoCard("بانک تجارت یافت نشد", "Release Gate مانع انتشار نهایی بانک ناقص خواهد شد.") }
        items(trade, key = { it.id }) { card ->
            StudyContentCard(card, onReminder = { reminderFor = card })
        }
    }
    reminderFor?.let { card ->
        ReminderIntervalDialog(
            title = card.title,
            onDismiss = { reminderFor = null },
            onChoose = { code ->
                vm.scheduleManualReminder("TRADE_ARTICLE", card.id, card.title, code)
                reminderFor = null
            },
        )
    }
}

@Composable
fun FiqhScreen(vm: ProductViewModel) {
    val cards by vm.cards.collectAsStateWithLifecycle()
    val fiqh = remember(cards) { cards.filter { it.domain == "FIQH" } }
    ScreenList {
        item {
            SectionHeader(
                "متون فقه — معاملات",
                "بانک فعلی Foundation است و در Gate نهایی از نظر منبع و پوشش آزمون ممیزی می‌شود.",
            )
        }
        if (fiqh.isEmpty()) item { InfoCard("متون فقه یافت نشد", "بانک معتبر باید قبل از Release کامل شود.") }
        items(fiqh, key = { it.id }) { card ->
            StudyContentCard(card) {
                vm.scheduleManualReminder("FIQH", card.id, card.title, "D3")
            }
        }
        item {
            InfoCard(
                "واژگان عربی",
                "زیرساخت بانک مستقل عربی → فارسی در دیتابیس جدید ایجاد شده است. تنها داده دارای منبع معتبر وارد Release خواهد شد.",
            )
        }
    }
}

@Composable
fun EnglishScreen(vm: ProductViewModel) {
    val cards by vm.cards.collectAsStateWithLifecycle()
    val vocab = remember(cards) { cards.filter { it.domain == "VOCAB" } }
    var index by remember(vocab.size) { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }

    ScreenList {
        item {
            SectionHeader(
                "زبان انگلیسی",
                "Front = English • Back = معنی فارسی • درست = مرور ۳ روز بعد • نادرست = دقیقاً ۲۴ ساعت بعد",
            )
        }
        if (vocab.isEmpty()) {
            item { InfoCard("بانک واژگان یافت نشد", "حداقل ۲۰۰۰ واژه یکتا برای Release نهایی الزامی است.") }
        } else {
            val card = vocab[index.coerceIn(0, vocab.lastIndex)]
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { revealed = !revealed },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(30.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            if (!revealed) card.title else card.answer,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(if (!revealed) "برای دیدن معنی لمس کنید" else "معنی فارسی")
                    }
                }
            }
            if (revealed) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                vm.gradeVocabulary(card, false, "ENGLISH")
                                index = (index + 1) % vocab.size
                                revealed = false
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("نادرست") }
                        Button(
                            onClick = {
                                vm.gradeVocabulary(card, true, "ENGLISH")
                                index = (index + 1) % vocab.size
                                revealed = false
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("درست") }
                    }
                }
            }
            item { Text("کارت " + (index + 1) + " از " + vocab.size) }
        }
    }
}

@Composable
fun ProductReviewScreen(vm: ProductViewModel) {
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()
    val due = remember(reminders, now) { reminders.filter { it.scheduledAtMillis <= now } }
    var reschedule by remember { mutableStateOf<ReminderEntity?>(null) }

    ScreenList {
        item { SectionHeader("مرورها", "مرورهای سررسیدشده اول؛ محتوای جدید بعد.") }
        if (due.isEmpty()) {
            item { InfoCard("صف مرور صفر است", "در حال حاضر Reminder سررسیدشده‌ای باقی نمانده است.") }
        }
        items(due, key = { it.id }) { item ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text("نوع: " + item.contentType)
                    Text("سررسید: " + formatDateTime(item.scheduledAtMillis))
                    Button(onClick = { reschedule = item }) { Text("مرور شد — تعیین فاصله بعدی") }
                }
            }
        }
    }

    reschedule?.let { item ->
        ReminderIntervalDialog(
            title = item.title,
            onDismiss = { reschedule = null },
            onChoose = { code ->
                vm.scheduleManualReminder(item.contentType, item.contentId, item.title, code)
                reschedule = null
            },
        )
    }
}

@Composable
private fun StudyContentCard(card: StudyCardEntity, onReminder: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(card.title, style = MaterialTheme.typography.titleMedium)
            Text(card.prompt)
            if (card.answer.isNotBlank()) Text(card.answer, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (card.sourceName.isNotBlank()) Text("منبع: " + card.sourceName, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onReminder) { Text("یادآوری") }
        }
    }
}

@Composable
private fun CourseSectionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ReminderIntervalDialog(
    title: String,
    onDismiss: () -> Unit,
    onChoose: (String) -> Unit,
) {
    val labels = listOf(
        "H12" to "۱۲ ساعت",
        "H24" to "۲۴ ساعت",
        "H48" to "۴۸ ساعت",
        "D3" to "۳ روز",
        "D7" to "۷ روز",
        "D14" to "۱۴ روز",
        "D20" to "۲۰ روز",
        "D40" to "۴۰ روز",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("یادآوری — " + title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                labels.forEach { pair ->
                    OutlinedButton(
                        onClick = { onChoose(pair.first) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(pair.second) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}
