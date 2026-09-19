package ir.madani3.mindgame.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.madani3.mindgame.model.GameLevel
import ir.madani3.mindgame.model.GamePiece
import ir.madani3.mindgame.model.LinkNode
import kotlin.math.hypot

private val Gold = Color(0xFFD2A64A)
private val Mint = Color(0xFF6CE6B5)
private val Deep = Color(0xFF10241D)

@Composable
fun DropPuzzle(
    level: GameLevel,
    onMistake: () -> Unit,
    onSolved: () -> Unit,
    labStyle: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val targetBounds = remember(level.articleNumber) { mutableStateMapOf<String, Rect>() }
    val placed = remember(level.articleNumber) { mutableStateMapOf<String, String>() }

    LaunchedEffect(placed.size) {
        if (level.pieces.isNotEmpty() && placed.size == level.pieces.size) onSolved()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (labStyle) Color(0xFF0C1C17) else Color.Transparent, RoundedCornerShape(24.dp))
            .padding(if (labStyle) 14.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (labStyle) {
            Text("هسته معامله", color = Gold, style = MaterialTheme.typography.titleMedium)
            Text(
                "قطعات را با انگشت داخل جای حقوقی صحیح بنشان.",
                color = Color(0xFFB7C9C1),
                style = MaterialTheme.typography.bodySmall,
            )
        }

        level.targets.forEach { target ->
            val found = placed.entries.firstOrNull { it.value == target.id }?.key
            val piece = level.pieces.firstOrNull { it.id == found }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { targetBounds[target.id] = it.boundsInRoot() }
                    .border(
                        width = if (piece != null) 1.6.dp else 1.dp,
                        color = if (piece != null) Mint else Color(0xFF355449),
                        shape = RoundedCornerShape(18.dp),
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (piece != null) Color(0xFF12392B) else Color(0xFF10251E)
                ),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(Modifier.padding(13.dp)) {
                    Text(target.label, color = Color(0xFF9BB3AA), style = MaterialTheme.typography.labelMedium)
                    if (piece != null) {
                        Text("✓ ${piece.label}", color = Mint, style = MaterialTheme.typography.titleSmall)
                    } else {
                        Text("اینجا رها کن", color = Color(0xFF536E64), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (placed.size < level.pieces.size) {
            Text("قطعات آزاد", color = Gold, style = MaterialTheme.typography.labelLarge)
            level.pieces.filterNot { placed.containsKey(it.id) }.forEach { piece ->
                DraggablePiece(
                    piece = piece,
                    targetBounds = targetBounds,
                    onCorrect = {
                        placed[piece.id] = piece.targetId
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onWrong = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMistake()
                    },
                )
            }
        }
    }
}

@Composable
private fun DraggablePiece(
    piece: GamePiece,
    targetBounds: Map<String, Rect>,
    onCorrect: () -> Unit,
    onWrong: () -> Unit,
) {
    var drag by remember(piece.id) { mutableStateOf(Offset.Zero) }
    var origin by remember(piece.id) { mutableStateOf(Rect.Zero) }
    var dragging by remember(piece.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (dragging) 20f else 1f)
            .onGloballyPositioned { origin = it.boundsInRoot() }
            .graphicsLayer {
                translationX = drag.x
                translationY = drag.y
                shadowElevation = if (dragging) 22f else 4f
                scaleX = if (dragging) 1.03f else 1f
                scaleY = if (dragging) 1.03f else 1f
            }
            .border(1.dp, if (dragging) Gold else Color(0xFF4A685D), RoundedCornerShape(16.dp))
            .then(
                Modifier.detectDrag(
                    onStart = { dragging = true },
                    onDrag = { delta -> drag += delta },
                    onEnd = {
                        dragging = false
                        val center = origin.center + drag
                        val hit = targetBounds.entries.firstOrNull { (_, rect) -> rect.contains(center) }?.key
                        if (hit == piece.targetId) {
                            onCorrect()
                        } else {
                            if (hit != null) onWrong()
                        }
                        drag = Offset.Zero
                    },
                    onCancel = {
                        dragging = false
                        drag = Offset.Zero
                    }
                )
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF18362C)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(piece.label, color = Color.White, modifier = Modifier.weight(1f))
            Text("⠿", color = Gold)
        }
    }
}

private fun Modifier.detectDrag(
    onStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit,
): Modifier = this.pointerInput(Unit) {
    detectDragGestures(
        onDragStart = { onStart() },
        onDragEnd = onEnd,
        onDragCancel = onCancel,
        onDrag = { change, dragAmount ->
            onDrag(dragAmount)
        }
    )
}

@Composable
fun LinkPuzzle(
    level: GameLevel,
    onMistake: () -> Unit,
    onSolved: () -> Unit,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val hitRadius = with(density) { 54.dp.toPx() }
    var boardSize by remember(level.articleNumber) { mutableStateOf(IntSize.Zero) }
    var startId by remember(level.articleNumber) { mutableStateOf<String?>(null) }
    var pointer by remember(level.articleNumber) { mutableStateOf<Offset?>(null) }
    val made = remember(level.articleNumber) { mutableStateMapOf<String, Boolean>() }

    fun key(a: String, b: String) = listOf(a, b).sorted().joinToString("|")
    val required = remember(level.articleNumber) {
        level.edges.associate { key(it.from, it.to) to true }
    }

    LaunchedEffect(made.size) {
        if (required.isNotEmpty() && required.keys.all { made[it] == true }) onSolved()
    }

    fun nodePoint(node: LinkNode): Offset =
        Offset(boardSize.width * node.x, boardSize.height * node.y)

    fun nearest(pos: Offset): String? =
        level.nodes
            .map { it.id to hypot((nodePoint(it).x - pos.x).toDouble(), (nodePoint(it).y - pos.y).toDouble()) }
            .filter { it.second <= hitRadius }
            .minByOrNull { it.second }
            ?.first

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(410.dp)
            .background(Color(0xFF0B1A15), RoundedCornerShape(24.dp))
            .onSizeChanged { boardSize = it }
            .padding(6.dp)
            .then(
                Modifier.pointerInput(level.articleNumber, boardSize) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            startId = nearest(pos)
                            pointer = pos
                        },
                        onDrag = { change, _ ->
                                            pointer = change.position
                        },
                        onDragCancel = {
                            startId = null
                            pointer = null
                        },
                        onDragEnd = {
                            val a = startId
                            val p = pointer
                            val b = p?.let { nearest(it) }
                            if (a != null && b != null && a != b) {
                                val k = key(a, b)
                                if (required.containsKey(k)) {
                                    made[k] = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else {
                                    onMistake()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                            startId = null
                            pointer = null
                        }
                    )
                }
            )
    ) {
        if (boardSize.width == 0) return@Canvas

        level.edges.forEach { edge ->
            val k = key(edge.from, edge.to)
            if (made[k] == true) {
                val a = level.nodes.first { it.id == edge.from }
                val b = level.nodes.first { it.id == edge.to }
                drawLine(Mint, nodePoint(a), nodePoint(b), strokeWidth = 8f)
            }
        }

        val active = startId
        if (active != null && pointer != null) {
            val n = level.nodes.firstOrNull { it.id == active }
            if (n != null) drawLine(Gold, nodePoint(n), pointer!!, strokeWidth = 6f)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 13.sp.toPx()
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        level.nodes.forEach { node ->
            val p = nodePoint(node)
            val connected = required.keys.filter { it.contains(node.id) }.any { made[it] == true }
            drawCircle(
                color = if (connected) Color(0xFF164D39) else Deep,
                radius = 49f,
                center = p,
            )
            drawCircle(
                color = if (connected) Mint else Gold,
                radius = 49f,
                center = p,
                style = Stroke(width = 3f),
            )
            paint.color = android.graphics.Color.WHITE
            val label = if (node.label.length > 15) node.label.take(14) + "…" else node.label
            drawContext.canvas.nativeCanvas.drawText(label, p.x, p.y + 5f, paint)
        }
    }

    Text(
        "انگشتت را از یک گره بگیر و کابل را تا گره مرتبط بکش.",
        color = Color(0xFF9CB7AD),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 8.dp),
    )
}
