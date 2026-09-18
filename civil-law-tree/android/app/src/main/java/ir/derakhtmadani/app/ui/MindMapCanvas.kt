package ir.derakhtmadani.app.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.sp
import ir.derakhtmadani.app.model.MindNode
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MindMapCanvas(nodes: List<MindNode>, modifier: Modifier = Modifier) {
    val shown = nodes.take(10)
    Canvas(modifier.fillMaxWidth().aspectRatio(1.15f)) {
        if (shown.isEmpty()) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val root = shown.first()
        val others = shown.drop(1)
        val radius = size.minDimension * 0.37f
        val positions = mutableMapOf(root.id to center)

        others.forEachIndexed { index, node ->
            val angle = (Math.PI * 2.0 * index / others.size.coerceAtLeast(1)) - Math.PI / 2
            positions[node.id] = Offset(
                center.x + (radius * cos(angle)).toFloat(),
                center.y + (radius * sin(angle)).toFloat(),
            )
        }

        shown.drop(1).forEach { node ->
            val from = positions[node.parentId] ?: center
            val to = positions[node.id] ?: center
            drawLine(Color(0xFFB58B36), from, to, strokeWidth = 3f)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 12.sp.toPx()
            color = android.graphics.Color.rgb(23, 60, 51)
        }

        shown.forEach { node ->
            val p = positions[node.id] ?: center
            val isRoot = node.id == root.id
            drawCircle(
                color = if (isRoot) Color(0xFF173C33) else Color(0xFFF2E8CE),
                radius = if (isRoot) 48f else 40f,
                center = p,
            )
            paint.color = if (isRoot) android.graphics.Color.WHITE else android.graphics.Color.rgb(23, 60, 51)
            val label = if (node.label.length > 18) node.label.take(17) + "…" else node.label
            drawContext.canvas.nativeCanvas.drawText(label, p.x, p.y + 5f, paint)
        }
    }
}
