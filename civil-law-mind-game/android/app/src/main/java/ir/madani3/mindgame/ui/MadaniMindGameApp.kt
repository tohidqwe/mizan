package ir.madani3.mindgame.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.madani3.mindgame.data.GameRepository
import ir.madani3.mindgame.data.GameStateStore
import ir.madani3.mindgame.model.GameLevel
import ir.madani3.mindgame.model.WorldInfo

private val worlds = listOf(
    WorldInfo(1, "DNA قرارداد", "از تعریف عقد تا منجز و معلق", 183, 189),
    WorldInfo(2, "مونتاژ اراده", "قصد، بیان اراده و نمایندگی", 190, 198),
    WorldInfo(3, "میدان رضا و اکراه", "اشتباه، تهدید، اضطرار و تنفیذ", 199, 209),
    WorldInfo(4, "دروازه اهلیت", "چه کسی می‌تواند معامله کند؟", 210, 213),
    WorldInfo(5, "کارگاه موضوع و جهت", "موضوع، منفعت مشروع و فرار از دین", 214, 218),
    WorldInfo(6, "موتور آثار قرارداد", "لزوم، عرف، صحت و تعهدات ضمنی", 219, 225),
    WorldInfo(7, "میدان تعهد و خسارت", "موعد، سبب خارجی، خسارت و وجه التزام", 226, 230),
    WorldInfo(8, "مرز اثر و شرط", "اصل نسبی و شروط باطل", 231, 232),
)

private sealed interface Screen {
    data object Home : Screen
    data object Map : Screen
    data class Level(val article: Int) : Screen
}

private val gameColors = darkColorScheme(
    primary = Color(0xFFD2A64A),
    onPrimary = Color(0xFF07110D),
    secondary = Color(0xFF6CE6B5),
    background = Color(0xFF08120E),
    surface = Color(0xFF10241D),
    onSurface = Color(0xFFF2F2E9),
    error = Color(0xFFFF7B76),
)

@Composable
fun MadaniMindGameApp() {
    val context = LocalContext.current
    val store = remember { GameStateStore(context) }
    var levels by remember { mutableStateOf<List<GameLevel>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    LaunchedEffect(Unit) {
        runCatching { GameRepository.load(context) }
            .onSuccess { levels = it }
            .onFailure { error = it.message ?: "خطای ناشناخته" }
    }

    MaterialTheme(colorScheme = gameColors) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                when {
                    error != null -> Box(Modifier.padding(24.dp)) { Text("بارگذاری بازی ناموفق بود: $error") }
                    levels.isEmpty() -> Loading()
                    else -> when (val s = screen) {
                        Screen.Home -> Home(
                            levels = levels,
                            store = store,
                            onPlay = {
                                val next = levels.firstOrNull { !store.isCompleted(it.articleNumber) && store.unlocked(it.articleNumber) }
                                    ?: levels.last()
                                screen = Screen.Level(next.articleNumber)
                            },
                            onMap = { screen = Screen.Map },
                        )
                        Screen.Map -> WorldMap(
                            levels = levels,
                            store = store,
                            back = { screen = Screen.Home },
                            open = { screen = Screen.Level(it) },
                        )
                        is Screen.Level -> {
                            val level = levels.first { it.articleNumber == s.article }
                            LevelScreen(
                                level = level,
                                store = store,
                                back = { screen = Screen.Map },
                                next = {
                                    val n = s.article + 1
                                    screen = if (n <= 232) Screen.Level(n) else Screen.Home
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Loading() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("در حال بیدار کردن شبکه قراردادها…", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("۵۰ ماده • ۸ جهان • بدون تست چهارگزینه‌ای", color = Color(0xFF83A79A))
    }
}

@Composable
private fun Home(
    levels: List<GameLevel>,
    store: GameStateStore,
    onPlay: () -> Unit,
    onMap: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("مدنی ۳", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Text("بازی ذهنی قراردادها", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text("قانون را نخوان؛ با دست بسازش.", color = Color(0xFF91AFA4))
        }
        item { ContractTreeHero(store.completedCount()) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Metric("مرحله", "${store.completedCount()}/50", Modifier.weight(1f))
                Metric("ستاره", "${store.totalStars()}/150", Modifier.weight(1f))
                Metric("XP", store.xp().toString(), Modifier.weight(1f))
            }
        }
        item {
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(if (store.completedCount() == 0) "شروع بازی" else "ادامه بازی", color = Color(0xFF08120E))
            }
        }
        item {
            OutlinedButton(onClick = onMap, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(18.dp)) {
                Text("نقشه ۸ جهان")
            }
        }
        item {
            Text("چطور بازی می‌کنی؟", style = MaterialTheme.typography.titleLarge)
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF10241D)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("⠿ قطعه را بکش و در جای حقوقی درست رها کن.")
                    Text("⌁ بین مفاهیم کابل بکش و رابطه را بساز.")
                    Text("◉ اجزای معامله را در آزمایشگاه مونتاژ کن.")
                    Text("⇢ شرط، حکم و اثر را به ترتیب واقعی بچین.")
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF10241D)), shape = RoundedCornerShape(17.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleLarge)
            Text(label, color = Color(0xFF84A197), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ContractTreeHero(done: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1C17)),
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Canvas(Modifier.fillMaxWidth().height(230.dp)) {
                val cx = size.width / 2f
                val bottom = size.height * .9f
                val progress = done / 50f
                drawLine(Color(0xFFD2A64A), Offset(cx, bottom), Offset(cx, size.height * .48f), strokeWidth = 14f)
                val branchYs = listOf(.52f, .44f, .36f, .28f)
                branchYs.forEachIndexed { i, y ->
                    val active = progress >= (i + 1) / 8f
                    val c = if (active) Color(0xFF6CE6B5) else Color(0xFF29463B)
                    drawLine(c, Offset(cx, size.height * y), Offset(size.width * .18f, size.height * (y - .12f)), strokeWidth = 8f)
                    drawLine(c, Offset(cx, size.height * y), Offset(size.width * .82f, size.height * (y - .12f)), strokeWidth = 8f)
                    drawCircle(c, 14f, Offset(size.width * .18f, size.height * (y - .12f)))
                    drawCircle(c, 14f, Offset(size.width * .82f, size.height * (y - .12f)))
                }
                drawCircle(Color(0xFFD2A64A), 24f, Offset(cx, size.height * .14f))
            }
            Text("درخت قراردادها با هر مرحله زنده می‌شود.", color = Color(0xFFB9C8C2))
        }
    }
}

@Composable
private fun WorldMap(
    levels: List<GameLevel>,
    store: GameStateStore,
    back: () -> Unit,
    open: (Int) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = back) { Text("بازگشت") }
            Text("نقشه ذهنی مدنی ۳", style = MaterialTheme.typography.headlineMedium)
            Text("هر جهان یک قفل مفهومی از قراردادها را باز می‌کند.", color = Color(0xFF8EABA0))
        }
        items(worlds) { world ->
            val worldLevels = levels.filter { it.worldIndex == world.index }
            val done = worldLevels.count { store.isCompleted(it.articleNumber) }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10241D)),
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("جهان ${world.index} • ${world.title}", style = MaterialTheme.typography.titleMedium)
                            Text(world.subtitle, color = Color(0xFF87A399), style = MaterialTheme.typography.bodySmall)
                        }
                        Text("$done/${worldLevels.size}", color = MaterialTheme.colorScheme.secondary)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        worldLevels.forEach { level ->
                            val unlocked = store.unlocked(level.articleNumber)
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .background(
                                        when {
                                            store.isCompleted(level.articleNumber) -> Color(0xFF195540)
                                            unlocked -> Color(0xFFD2A64A)
                                            else -> Color(0xFF23372F)
                                        },
                                        CircleShape
                                    )
                                    .clickable(enabled = unlocked) { open(level.articleNumber) }
                            ) {
                                Text(
                                    text = level.articleNumber.toString().takeLast(2),
                                    modifier = Modifier.padding(7.dp),
                                    color = if (unlocked && !store.isCompleted(level.articleNumber)) Color(0xFF08120E) else Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelScreen(
    level: GameLevel,
    store: GameStateStore,
    back: () -> Unit,
    next: () -> Unit,
) {
    var mistakes by remember(level.articleNumber) { mutableIntStateOf(0) }
    var solved by remember(level.articleNumber) { mutableStateOf(store.isCompleted(level.articleNumber)) }
    var showLaw by remember(level.articleNumber) { mutableStateOf(store.isCompleted(level.articleNumber)) }

    fun solve() {
        if (!solved) store.complete(level.articleNumber, mistakes)
        solved = true
        showLaw = true
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = back) { Text("نقشه") }
                Spacer(Modifier.weight(1f))
                Text("✦ ${store.xp()} XP", color = MaterialTheme.colorScheme.secondary)
            }
            Text("ماده ${level.articleNumber}", style = MaterialTheme.typography.headlineLarge)
            Text(level.title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(level.world, color = Color(0xFF8FA99F))
        }

        if (!solved) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF132A22)), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("ماموریت", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
                        Text(level.instruction)
                        if (mistakes > 0) Text("خطا: $mistakes", color = Color(0xFFFF8E87), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                when (level.mechanic) {
                    "LINK" -> LinkPuzzle(level, onMistake = { mistakes++ }, onSolved = ::solve)
                    "LAB" -> DropPuzzle(level, onMistake = { mistakes++ }, onSolved = ::solve, labStyle = true)
                    "CHAIN" -> DropPuzzle(level, onMistake = { mistakes++ }, onSolved = ::solve, labStyle = false)
                    else -> DropPuzzle(level, onMistake = { mistakes++ }, onSolved = ::solve, labStyle = false)
                }
            }
        }

        if (showLaw) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF123C2D)),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("قفل ذهنی باز شد", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleLarge)
                        Text(level.memoryLock, style = MaterialTheme.typography.titleMedium)
                        Text(level.concept, color = Color(0xFFC4D2CD))
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111D19)), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("حالا متن رسمی را ببین", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        Text(level.officialText, color = Color.White)
                        Text("منبع متن: Qavanin.ir", color = Color(0xFF6F9184), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Button(onClick = next, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) {
                    Text(if (level.articleNumber < 232) "مرحله بعد" else "بازگشت به درخت")
                }
            }
        }
    }
}
