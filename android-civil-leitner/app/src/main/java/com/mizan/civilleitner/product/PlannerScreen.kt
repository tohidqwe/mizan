package com.mizan.civilleitner.product

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ProductPlannerScreen(vm: ProductViewModel) {
    val tasks by vm.plannerTasks.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(vm.todayPersianNumeric()) }
    var time by remember { mutableStateOf("17:30") }
    var reminderMinutes by remember { mutableStateOf("30") }
    var recurrence by remember { mutableStateOf("NONE") }
    var selectedSound by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }

    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= 33) {
                result.data?.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java,
                )
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            selectedSound = uri?.toString()
        }
    }

    ScreenList {
        item {
            SectionHeader(
                "برنامه‌ریز شخصی",
                "ثبت کار روزانه/آینده با تاریخ شمسی، ساعت، Alarm، Notification و صدای انتخابی",
            )
        }
        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان کار") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("توضیحات") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("تاریخ شمسی") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("ساعت") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = reminderMinutes,
                    onValueChange = { reminderMinutes = it.filter(Char::isDigit) },
                    label = { Text("یادآوری چند دقیقه قبل") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = recurrence,
                    onValueChange = { recurrence = it.uppercase() },
                    label = { Text("تکرار") },
                    supportingText = { Text("NONE / DAILY / WEEKLY / MONTHLY / YEARLY") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    ringtonePicker.launch(
                        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_TYPE,
                                RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_NOTIFICATION,
                            )
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (selectedSound == null) "انتخاب صدای Alarm" else "صدای Alarm انتخاب شد")
            }
        }
        item {
            Button(
                onClick = {
                    val ok = vm.addPlannerTask(
                        title = title,
                        description = description,
                        persianDate = date,
                        time = time,
                        reminderMinutesBefore = reminderMinutes.toIntOrNull() ?: 30,
                        recurrence = recurrence.ifBlank { "NONE" },
                        soundUri = selectedSound,
                    )
                    status = if (ok) "کار با موفقیت ثبت شد." else "تاریخ، ساعت یا عنوان معتبر نیست."
                    if (ok) {
                        title = ""
                        description = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("ثبت کار و زمان‌بندی یادآوری") }
        }
        if (status.isNotBlank()) item { Text(status, color = MaterialTheme.colorScheme.secondary) }

        item { Text("کارهای ثبت‌شده", style = MaterialTheme.typography.titleLarge) }
        if (tasks.isEmpty()) {
            item { InfoCard("هنوز کاری ثبت نشده", "اولین برنامه روزانه یا موعد آینده را از فرم بالا ثبت کنید.") }
        }
        items(tasks, key = { it.id }) { task ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium)
                    if (task.description.isNotBlank()) Text(task.description)
                    Text(formatDateTime(task.scheduledAtMillis), color = MaterialTheme.colorScheme.secondary)
                    Text("تکرار: " + task.recurrence, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.togglePlannerTask(task) }) {
                            Text(if (task.completed) "انجام‌شده ✓" else "انجام شد")
                        }
                        OutlinedButton(onClick = { vm.deletePlannerTask(task) }) { Text("حذف") }
                    }
                }
            }
        }
    }
}
