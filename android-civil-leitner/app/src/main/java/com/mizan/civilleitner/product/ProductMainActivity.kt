package com.mizan.civilleitner.product

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class ProductMainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProductTheme {
                ProductApp()
            }
        }
    }
}

enum class ProductScreen(val title: String) {
    HOME("خانه"),
    TODAY("برنامه‌های امروز"),
    EDUCATION("آموزش"),
    PHD("دکتری حقوق خصوصی"),
    STUDY_PLANNING("برنامه‌ریزی تحصیلی"),
    REVIEWS("مرورها"),
    PLANNER("برنامه‌ریز"),
    SUBSCRIPTION("خرید اشتراک"),
    CLIENT_PORTAL("کارتابل موکل"),
    ADMIN("کارتابل دکتر توحید نجفیان"),
    INBOX("پیام‌ها"),
    UPDATES("بروزرسانی"),
    SETTINGS("تنظیمات"),
    CIVIL("حقوق مدنی"),
    TRADE("حقوق تجارت"),
    FIQH("متون فقه"),
    ENGLISH("زبان انگلیسی"),
    ADMIN_PROVISION("فعال‌سازی امن دستگاه مدیر"),
}

private data class DrawerEntry(
    val screen: ProductScreen,
    val title: String,
    val icon: ImageVector,
)

@Composable
private fun ProductApp(vm: ProductViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as ProductMainActivity
    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                vm.refreshAdminBinding()
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !permissionGranted) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (!permissionGranted) {
            MandatoryNotificationGate(
                requestPermission = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                openSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                    )
                },
            )
        } else {
            ProductShell(vm)
        }
    }
}

@Composable
private fun MandatoryNotificationGate(
    requestPermission: () -> Unit,
    openSettings: () -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.material3.Card {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null)
                Text("فعال‌سازی اعلان‌ها الزامی است", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "این برنامه برای مرورهای دقیق، برنامه‌های روزانه، پیام‌های مدیریتی و یادآوری پرونده به اعلان Android نیاز دارد. بدون این مجوز محیط اصلی باز نمی‌شود.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = requestPermission) { Text("فعال‌سازی اعلان") }
                androidx.compose.material3.OutlinedButton(onClick = openSettings) {
                    Text("باز کردن تنظیمات اعلان")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductShell(vm: ProductViewModel) {
    val adminBound by vm.isAdminBound.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val drawerState = androidx.compose.material3.rememberDrawerState(
        initialValue = androidx.compose.material3.DrawerValue.Closed
    )
    var screen by remember(adminBound) {
        mutableStateOf(if (adminBound) ProductScreen.TODAY else ProductScreen.HOME)
    }
    var footerTaps by remember { mutableIntStateOf(0) }

    val entries = buildList {
        add(DrawerEntry(ProductScreen.HOME, "خانه", Icons.Default.Home))
        add(DrawerEntry(ProductScreen.TODAY, "امروز", Icons.Default.CalendarMonth))
        add(DrawerEntry(ProductScreen.EDUCATION, "آموزش", Icons.Default.School))
        add(DrawerEntry(ProductScreen.STUDY_PLANNING, "برنامه‌ریزی تحصیلی", Icons.Default.AutoStories))
        add(DrawerEntry(ProductScreen.REVIEWS, "مرورها", Icons.Default.NotificationsActive))
        add(DrawerEntry(ProductScreen.PLANNER, "برنامه‌ریز", Icons.Default.CalendarMonth))
        add(DrawerEntry(ProductScreen.SUBSCRIPTION, "خرید اشتراک", Icons.Default.Payment))
        add(DrawerEntry(ProductScreen.CLIENT_PORTAL, "کارتابل موکلین", Icons.Default.Person))
        add(DrawerEntry(ProductScreen.INBOX, "پیام‌ها", Icons.Default.Inbox))
        if (adminBound) {
            add(DrawerEntry(ProductScreen.ADMIN, "کارتابل دکتر توحید نجفیان", Icons.Default.AdminPanelSettings))
        }
        add(DrawerEntry(ProductScreen.UPDATES, "بروزرسانی", Icons.Default.SystemUpdate))
        add(DrawerEntry(ProductScreen.SETTINGS, "تنظیمات", Icons.Default.Settings))
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "دوره آموزشی دکتر توحید نجفیان",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                HorizontalDivider()
                entries.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.title) },
                        selected = screen == item.screen,
                        icon = { Icon(item.icon, contentDescription = null) },
                        onClick = {
                            screen = item.screen
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Text(
                    "کلیه حقوق مادی و معنوی این نرم‌افزار متعلق به توحید نجفیان، وکیل پایه یک دادگستری است.\n09144470341",
                    modifier = Modifier.padding(18.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                androidx.compose.material3.TextButton(
                    onClick = {
                        footerTaps++
                        if (footerTaps >= 7) {
                            footerTaps = 0
                            screen = ProductScreen.ADMIN_PROVISION
                            scope.launch { drawerState.close() }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 10.dp),
                ) {
                    Text("نسخه 0.3.0")
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(screen.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "منو")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
        ) { padding ->
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxSize().padding(padding)
            ) {
                ProductScreenHost(
                    screen = screen,
                    vm = vm,
                    navigate = { screen = it },
                )
            }
        }
    }
}
