package com.mizan.civilleitner.product

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mizan.civilleitner.BuildConfig
import com.mizan.civilleitner.domain.PersianDate
import com.mizan.civilleitner.worker.ExactReminderScheduler
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ProductHomeScreen(vm: ProductViewModel, navigate: (ProductScreen) -> Unit) {
    val countdown by vm.countdownDays.collectAsStateWithLifecycle()
    val due by vm.dueReminderCount.collectAsStateWithLifecycle()
    ScreenList {
        item {
            HeroCard(
                title = "دوره آموزشی دکتر توحید نجفیان",
                subtitle = vm.todayPersianLabel(),
                detail = countdown?.let { "تا آزمون: " + it.coerceAtLeast(0) + " روز" }
                    ?: "تاریخ آزمون هنوز تعیین نشده است",
            )
        }
        item {
            DashboardGrid(
                entries = listOf(
                    Triple("آموزش", "دوره‌ها و منابع آزمون", ProductScreen.EDUCATION),
                    Triple("برنامه‌ریزی تحصیلی", "طراحی برنامه تا روز آزمون", ProductScreen.STUDY_PLANNING),
                    Triple("مرورها", due.toString() + " مورد سررسیدشده", ProductScreen.REVIEWS),
                    Triple("برنامه‌ریز", "کارهای روزانه و آینده", ProductScreen.PLANNER),
                    Triple("خرید اشتراک", "ماهانه، سالیانه یا دوره‌ای", ProductScreen.SUBSCRIPTION),
                    Triple("کارتابل موکلین", "پرونده و تایم‌لاین اقدامات", ProductScreen.CLIENT_PORTAL),
                ),
                navigate = navigate,
            )
        }
    }
}

@Composable
fun ProductTodayScreen(vm: ProductViewModel, navigate: (ProductScreen) -> Unit) {
    val tasks by vm.plannerTasks.collectAsStateWithLifecycle()
    val due by vm.dueReminderCount.collectAsStateWithLifecycle()
    val countdown by vm.countdownDays.collectAsStateWithLifecycle()
    val admin by vm.isAdminBound.collectAsStateWithLifecycle()
    val zone = ZoneId.systemDefault()
    val today = java.time.LocalDate.now()
    val todayTasks = tasks.filter {
        Instant.ofEpochMilli(it.scheduledAtMillis).atZone(zone).toLocalDate() == today
    }

    ScreenList {
        item {
            HeroCard(
                title = if (admin) "برنامه امروز دکتر توحید نجفیان" else "برنامه امروز",
                subtitle = vm.todayPersianLabel(),
                detail = countdown?.let { "تا آزمون: " + it.coerceAtLeast(0) + " روز" }
                    ?: "برای شروع Countdown تاریخ آزمون را تعیین کنید.",
            )
        }
        item {
            MetricRow(
                leftTitle = "مرورهای سررسیدشده",
                leftValue = due.toString(),
                rightTitle = "کارهای امروز",
                rightValue = todayTasks.count { !it.completed }.toString(),
            )
        }
        if (countdown == null) {
            item {
                OutlinedButton(
                    onClick = { navigate(ProductScreen.SETTINGS) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("تعیین تاریخ آزمون") }
            }
        }
        if (todayTasks.isEmpty()) {
            item { InfoCard("برای امروز کار ثبت‌شده‌ای وجود ندارد.", "از بخش برنامه‌ریز کار جدید اضافه کنید.") }
        } else {
            items(todayTasks, key = { it.id }) { task ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(task.title, style = MaterialTheme.typography.titleMedium)
                        if (task.description.isNotBlank()) Text(task.description)
                        Text(formatDateTime(task.scheduledAtMillis), color = MaterialTheme.colorScheme.secondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.togglePlannerTask(task) }) {
                                Text(if (task.completed) "بازگردانی" else "انجام شد")
                            }
                            TextButton(onClick = { vm.deletePlannerTask(task) }) { Text("حذف") }
                        }
                    }
                }
            }
        }
        if (admin) {
            item {
                Button(
                    onClick = { navigate(ProductScreen.ADMIN) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("ورود به کارتابل اختصاصی دکتر") }
            }
        }
    }
}

@Composable
fun EducationScreen(vm: ProductViewModel, navigate: (ProductScreen) -> Unit) {
    val access by vm.phdAccess.collectAsStateWithLifecycle()
    val demo by vm.demoActive.collectAsStateWithLifecycle()
    var lockedCourse by remember { mutableStateOf<CourseItem?>(null) }

    ScreenList {
        item {
            SectionHeader("آموزش حقوق", "ساختار دوره‌ها برای توسعه بلندمدت از همین نسخه ایجاد شده است.")
        }
        items(CourseCatalog.courses, key = { it.id }) { course ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    if (!course.available) return@clickable
                    if (course.id == CourseCatalog.PHD_PRIVATE_LAW && access) {
                        navigate(ProductScreen.PHD)
                    } else {
                        lockedCourse = course
                    }
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (course.available) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        if (course.available) Icons.Default.School else Icons.Default.Lock,
                        contentDescription = null,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(course.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            when {
                                course.id == CourseCatalog.PHD_PRIVATE_LAW && access ->
                                    if (demo) "نسخه آزمایشی ۷۲ ساعته فعال" else "فعال"
                                course.available -> "نیازمند اشتراک — امکان آزمایش ۷۲ ساعته"
                                else -> "به‌زودی",
                            },
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
    }

    lockedCourse?.let { course ->
        AlertDialog(
            onDismissRequest = { lockedCourse = null },
            icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = null) },
            title = { Text("دسترسی اشتراکی") },
            text = {
                Text(
                    "برای استفاده کامل از «" + course.title +
                        "» اشتراک لازم است. می‌توانید یک نسخه آزمایشی ۷۲ ساعته همراه با برنامه مطالعاتی ۷۲ ساعته فعال کنید."
                )
            },
            confirmButton = {
                Button(onClick = {
                    vm.activate72HourDemo()
                    lockedCourse = null
                    navigate(ProductScreen.PHD)
                }) { Text("فعال‌سازی ۷۲ ساعت") }
            },
            dismissButton = {
                TextButton(onClick = {
                    lockedCourse = null
                    navigate(ProductScreen.SUBSCRIPTION)
                }) { Text("خرید اشتراک") }
            },
        )
    }
}

@Composable
fun StudyPlanningScreen(vm: ProductViewModel) {
    val access by vm.phdAccess.collectAsStateWithLifecycle()
    val admin by vm.isAdminBound.collectAsStateWithLifecycle()
    val allowed = access || admin
    var target by remember { mutableStateOf("آمادگی برای کنکور دکتری حقوق خصوصی") }
    var exam by remember { mutableStateOf("") }
    var dailyHours by remember { mutableStateOf("") }
    var levels by remember { mutableStateOf("") }
    var occupation by remember { mutableStateOf("") }
    var offDays by remember { mutableStateOf("") }
    var priorStudy by remember { mutableStateOf("") }
    var priorExams by remember { mutableStateOf("") }
    var targetRank by remember { mutableStateOf("") }
    var learningStyle by remember { mutableStateOf("") }
    var constraints by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    ScreenList {
        item {
            SectionHeader(
                "برنامه‌ریزی تحصیلی",
                if (allowed) "فرم جامع طراحی برنامه شخصی تا روز آزمون"
                else "این خدمت با خرید اشتراک برنامه‌ریزی فعال می‌شود.",
            )
        }
        if (!allowed) {
            item { LockedFeatureCard("برنامه‌ریزی تحصیلی نیازمند اشتراک است.") }
        } else {
            item { FormField("دوره/آزمون هدف", target) { target = it } }
            item { FormField("تاریخ آزمون به شمسی", exam, "مثال ۱۴۰۵/۱۱/۱۶") { exam = it } }
            item { FormField("زمان آزاد مطالعه در روز", dailyHours, "مثال ۳ ساعت") { dailyHours = it } }
            item { FormField("سطح فعلی هر درس", levels, "مدنی، تجارت، فقه، زبان") { levels = it } }
            item { FormField("شغل و ساعات کاری", occupation) { occupation = it } }
            item { FormField("روزهای تعطیل و محدودیت زمانی", offDays) { offDays = it } }
            item { FormField("سابقه مطالعه و منابع قبلی", priorStudy) { priorStudy = it } }
            item { FormField("سابقه آزمون و نتیجه", priorExams) { priorExams = it } }
            item { FormField("هدف رتبه/دانشگاه", targetRank) { targetRank = it } }
            item { FormField("سبک یادگیری و عادات مطالعه", learningStyle) { learningStyle = it } }
            item { FormField("شرایط شخصی، تمرکز، خواب و سایر محدودیت‌ها", constraints) { constraints = it } }
            item {
                Button(
                    onClick = { submitted = true },
                    enabled = target.isNotBlank() && dailyHours.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("ثبت درخواست برنامه‌ریزی") }
            }
            if (submitted) {
                item {
                    InfoCard(
                        "فرم ثبت شد",
                        "در نسخه متصل، اطلاعات برای بررسی و تأیید برنامه از کارتابل دکتر ارسال می‌شود.",
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionScreen() {
    val context = LocalContext.current
    ScreenList {
        item { SectionHeader("خرید اشتراک", "سه مدل دسترسی برای دوره‌ها و خدمات آموزشی") }
        item {
            SubscriptionOption("اشتراک ماهانه", "دسترسی یک‌ماهه به خدمات انتخاب‌شده")
        }
        item {
            SubscriptionOption("اشتراک سالیانه", "دسترسی بلندمدت با مدیریت تاریخ انقضا")
        }
        item {
            SubscriptionOption("اشتراک موردی", "خرید فقط یک دوره مشخص، برای مثال دکتری حقوق خصوصی")
        }
        item {
            Button(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:09144470341")))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("تماس برای خرید/فعال‌سازی") }
        }
        item {
            Text(
                "درگاه پرداخت ایرانی در مرحله اتصال تجاری به همین صفحه متصل می‌شود و ساختار دسترسی ماهانه، سالیانه و دوره‌ای از هم‌اکنون در برنامه پیش‌بینی شده است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun InboxScreen(vm: ProductViewModel) {
    val messages by vm.inbox.collectAsStateWithLifecycle()
    ScreenList {
        item { SectionHeader("پیام‌ها", "اعلان‌های مدیریتی علاوه بر Notification در این صندوق باقی می‌مانند.") }
        if (messages.isEmpty()) {
            item { InfoCard("پیامی وجود ندارد", "پیام‌های جدید دکتر/سامانه در این بخش نمایش داده می‌شوند.") }
        } else {
            items(messages, key = { it.id }) { message ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(message.title, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(message.body)
                        Spacer(Modifier.height(6.dp))
                        Text(formatDateTime(message.receivedAtMillis), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateScreen() {
    ScreenList {
        item {
            HeroCard(
                "بروزرسانی نرم‌افزار",
                "نسخه نصب‌شده: " + BuildConfig.VERSION_NAME,
                "کانال انتشار برای Google Play، مایکت، بازار و APK مستقیم به‌صورت مستقل قابل تنظیم است.",
            )
        }
        item {
            InfoCard(
                "بروزرسانی خودکار",
                "برنامه برای دریافت Manifest نسخه از سرور طراحی شده است. هر انتشار می‌تواند اختیاری یا اجباری باشد؛ نوع انتشار توسط مدیر تعیین می‌شود.",
            )
        }
        item {
            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("بررسی بروزرسانی") }
        }
    }
}

@Composable
fun ProductSettingsScreen(vm: ProductViewModel) {
    val context = LocalContext.current
    val countdown by vm.countdownDays.collectAsStateWithLifecycle()
    var examDate by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    val exact = ExactReminderScheduler.canScheduleExact(context)

    ScreenList {
        item { SectionHeader("تنظیمات", "تنظیمات ضروری بدون پیچیدگی الگوریتمی") }
        item {
            OutlinedTextField(
                value = examDate,
                onValueChange = { examDate = it },
                label = { Text("تاریخ آزمون به شمسی") },
                placeholder = { Text("۱۴۰۵/۱۱/۱۶") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            Button(
                onClick = {
                    result = if (vm.setExamPersianDate(examDate)) "تاریخ آزمون ذخیره شد."
                    else "فرمت تاریخ معتبر نیست."
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("ذخیره تاریخ و شروع Countdown") }
        }
        if (result.isNotBlank()) item { Text(result) }
        if (countdown != null) item { InfoCard("Countdown فعال", countdown.toString() + " روز تا آزمون") }
        item {
            InfoCard(
                "یادآوری دقیق",
                if (exact) "مجوز زمان‌بندی دقیق فعال است."
                else "برای اجرای دقیق ساعت Reminder، دسترسی Exact Alarm را فعال کنید.",
            )
        }
        if (!exact) {
            item {
                Button(
                    onClick = { context.startActivity(ExactReminderScheduler.exactAlarmSettingsIntent(context)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("فعال‌سازی Exact Alarm") }
            }
        }
        item {
            Text(
                "قفل ورود برای کاربران عادی اختیاری است و در نسخه متصل می‌تواند توسط خود کاربر فعال شود. قفل Admin مستقل و مبتنی بر دستگاه ثبت‌شده است.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SubscriptionOption(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(body)
        }
    }
}

@Composable
private fun LockedFeatureCard(text: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Text(text)
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    placeholder: String = "",
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = if (placeholder.isBlank()) null else ({ Text(placeholder) }),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ScreenList(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HeroCard(title: String, subtitle: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(subtitle, style = MaterialTheme.typography.titleMedium)
            Text(detail, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun InfoCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardGrid(
    entries: List<Triple<String, String, ProductScreen>>,
    navigate: (ProductScreen) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        entries.forEach { entry ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navigate(entry.third) }
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(entry.first, style = MaterialTheme.typography.titleMedium)
                    Text(entry.second, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    leftTitle: String,
    leftValue: String,
    rightTitle: String,
    rightValue: String,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.weight(1f)) {
            Column(Modifier.padding(14.dp)) {
                Text(leftValue, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text(leftTitle, style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(Modifier.weight(1f)) {
            Column(Modifier.padding(14.dp)) {
                Text(rightValue, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
                Text(rightTitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

fun formatDateTime(epochMillis: Long): String {
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    val p = PersianDate.fromGregorian(dateTime.toLocalDate())
    return p.numeric() + " — " + dateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
}
