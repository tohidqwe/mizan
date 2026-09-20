package com.mizan.civilleitner.product

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ClientPortalScreen(vm: ProductViewModel) {
    val cases by vm.clientCases.collectAsStateWithLifecycle()
    val timeline by vm.selectedCaseTimeline.collectAsStateWithLifecycle()
    var selectedCase by remember { mutableStateOf<String?>(null) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    ScreenList {
        item {
            SectionHeader(
                "کارتابل مخصوص موکلین",
                "حساب فقط زمانی فعال می‌شود که دکتر برای موکل Username/Password تعریف کرده باشد.",
            )
        }
        if (cases.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("ورود موکل", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("نام کاربری") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("رمز عبور") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Button(
                            onClick = { },
                            enabled = username.isNotBlank() && password.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("ورود و همگام‌سازی پرونده") }
                        Text(
                            "اطلاعات پرونده پس از اتصال حساب از سرور امن دریافت و نسخه Cache برای مشاهده آفلاین نگهداری می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            item { Text("پرونده‌ها", style = MaterialTheme.typography.titleLarge) }
            items(cases, key = { it.id }) { case ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedCase = case.id
                        vm.selectClientCase(case.id)
                    }
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(case.title, style = MaterialTheme.typography.titleMedium)
                        if (case.referenceNo.isNotBlank()) Text("شماره/کلاسه: " + case.referenceNo)
                        Text("وضعیت: " + case.status, color = MaterialTheme.colorScheme.secondary)
                        if (case.summary.isNotBlank()) Text(case.summary)
                    }
                }
            }
            if (selectedCase != null) {
                item { Text("تایم‌لاین پرونده", style = MaterialTheme.typography.titleLarge) }
                if (timeline.isEmpty()) {
                    item { InfoCard("هنوز اقدامی ثبت نشده", "اقدامات قابل مشاهده‌ای که دکتر ثبت کند در این قسمت نمایش داده می‌شوند.") }
                } else {
                    items(timeline, key = { it.id }) { event ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(event.actionTitle, style = MaterialTheme.typography.titleMedium)
                                Text(formatDateTime(event.occurredAtMillis), color = MaterialTheme.colorScheme.primary)
                                if (event.details.isNotBlank()) Text(event.details)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorAdminScreen(vm: ProductViewModel) {
    val admin by vm.isAdminBound.collectAsStateWithLifecycle()
    val tasks by vm.plannerTasks.collectAsStateWithLifecycle()
    val inbox by vm.inbox.collectAsStateWithLifecycle()
    val due by vm.dueReminderCount.collectAsStateWithLifecycle()

    if (!admin) {
        ScreenList {
            item {
                InfoCard(
                    "دسترسی غیرمجاز",
                    "این بخش فقط روی دستگاهی نمایش داده می‌شود که به‌صورت رمزنگاری‌شده به حساب دکتر متصل شده باشد.",
                )
            }
        }
        return
    }

    ScreenList {
        item {
            HeroCard(
                "کارتابل دکتر توحید نجفیان",
                "دستگاه مدیر ثبت‌شده",
                "درخواست‌های مدیریتی آنلاین فقط پس از امضای سخت‌افزاری همان گوشی پذیرفته می‌شوند.",
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminMetric("کارهای باز", tasks.count { !it.completed }.toString(), Modifier.weight(1f))
                AdminMetric("مرور", due.toString(), Modifier.weight(1f))
                AdminMetric("پیام", inbox.size.toString(), Modifier.weight(1f))
            }
        }
        item { AdminActionCard(Icons.Default.People, "مدیریت کاربران و موکلین", "ساخت حساب، Username/Password، مسدودسازی و سطوح دسترسی") }
        item { AdminActionCard(Icons.Default.FolderShared, "پرونده‌ها و تایم‌لاین", "ثبت پرونده و افزودن اقدام با تاریخ، ساعت و شرح قابل مشاهده توسط موکل") }
        item { AdminActionCard(Icons.Default.Subscriptions, "اشتراک‌ها", "فعال‌سازی ماهانه، سالیانه یا دسترسی یک دوره مشخص") }
        item { AdminActionCard(Icons.Default.Notifications, "ارسال نوتیفیکیشن", "ارسال فوری به همه، یک کاربر یا گروه و مشاهده SENT/FAILED/DELIVERED با تیک وضعیت") }
        item { AdminActionCard(Icons.Default.AdminPanelSettings, "انتشار نسخه و بروزرسانی", "تعریف نسخه جدید و تعیین اختیاری یا اجباری بودن Update") }
        item {
            InfoCard(
                "امنیت کارتابل",
                "کد خصوصی Backend در Repository خصوصی جدا نگهداری می‌شود. APK هیچ Service Account یا Secret مدیریتی در خود ندارد.",
            )
        }
    }
}

@Composable
fun AdminProvisionScreen(
    vm: ProductViewModel,
    navigate: (ProductScreen) -> Unit,
) {
    val status by vm.adminProvisionStatus.collectAsStateWithLifecycle()
    val admin by vm.isAdminBound.collectAsStateWithLifecycle()
    var baseUrl by remember { mutableStateOf("") }
    var activationCode by remember { mutableStateOf("") }

    ScreenList {
        item {
            SectionHeader(
                "فعال‌سازی امن دستگاه مدیر",
                "این صفحه فقط برای ثبت اولین و تنها دستگاه Admin استفاده می‌شود.",
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null)
                    Text(
                        "کلید خصوصی داخل Android Keystore ساخته می‌شود و هرگز از گوشی خارج نمی‌شود.",
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it },
                        label = { Text("آدرس HTTPS سرور Admin") },
                        placeholder = { Text("https://admin.example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = activationCode,
                        onValueChange = { activationCode = it },
                        label = { Text("کد یک‌بارۀ فعال‌سازی") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Button(
                        onClick = { vm.provisionAdminDevice(baseUrl, activationCode) },
                        enabled = !admin && baseUrl.startsWith("https://") && activationCode.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (admin) "این دستگاه ثبت شده است" else "ثبت این گوشی به‌عنوان تنها دستگاه مدیر")
                    }
                    if (status.isNotBlank()) Text(status)
                    if (admin) {
                        TextButton(
                            onClick = { navigate(ProductScreen.ADMIN) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("ورود به کارتابل دکتر") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMetric(title: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AdminActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
