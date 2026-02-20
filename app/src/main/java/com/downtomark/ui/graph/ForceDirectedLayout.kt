package com.downtomark.ui.graph

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private data class NodePosition(
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f
)

@Composable
fun ForceDirectedGraph(
    data: GraphData,
    onNodeTap: (GraphNode) -> Unit,
    modifier: Modifier = Modifier
) {
    if (data.nodes.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline
    val density = LocalDensity.current

    // Layout state
    var positions by remember(data) {
        mutableStateOf(initializePositions(data.nodes))
    }

    // Pan/zoom state
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.3f, 3f)
        panOffset += panChange
    }

    // Animate physics simulation
    val progress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        positions = initializePositions(data.nodes)
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
        )
    }

    // Run physics steps based on animation progress
    LaunchedEffect(progress.value) {
        val steps = (progress.value * 200).toInt()
        val temp = max(0.1f, 1f - progress.value)
        positions = simulateStep(data, positions, temperature = temp)
    }

    val maxCount = data.nodes.maxOfOrNull { it.count } ?: 1

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformableState)
            .pointerInput(data, positions) {
                detectTapGestures { tapOffset ->
                    val adjustedTap = Offset(
                        (tapOffset.x - panOffset.x - size.width / 2f) / scale,
                        (tapOffset.y - panOffset.y - size.height / 2f) / scale
                    )
                    val tappedNode = data.nodes.indices.find { i ->
                        val pos = positions.getOrNull(i) ?: return@find false
                        val radius = nodeRadius(data.nodes[i].count, maxCount)
                        val dx = adjustedTap.x - pos.x
                        val dy = adjustedTap.y - pos.y
                        dx * dx + dy * dy <= radius * radius * 1.5f
                    }
                    tappedNode?.let { onNodeTap(data.nodes[it]) }
                }
            }
    ) {
        val centerX = size.width / 2f + panOffset.x
        val centerY = size.height / 2f + panOffset.y

        // Draw edges
        val nodeIndexMap = data.nodes.mapIndexed { i, n -> n.tag to i }.toMap()
        for (edge in data.edges) {
            val i1 = nodeIndexMap[edge.tag1] ?: continue
            val i2 = nodeIndexMap[edge.tag2] ?: continue
            val p1 = positions.getOrNull(i1) ?: continue
            val p2 = positions.getOrNull(i2) ?: continue
            val alpha = min(1f, edge.weight * 0.3f + 0.1f)
            val strokeWidth = min(4f, edge.weight * 1.5f + 0.5f)
            drawLine(
                color = outlineColor.copy(alpha = alpha),
                start = Offset(centerX + p1.x * scale, centerY + p1.y * scale),
                end = Offset(centerX + p2.x * scale, centerY + p2.y * scale),
                strokeWidth = strokeWidth * scale
            )
        }

        // Draw nodes
        for ((i, node) in data.nodes.withIndex()) {
            val pos = positions.getOrNull(i) ?: continue
            val radius = nodeRadius(node.count, maxCount) * scale
            val screenX = centerX + pos.x * scale
            val screenY = centerY + pos.y * scale

            // Node circle
            drawCircle(
                color = primaryColor,
                radius = radius,
                center = Offset(screenX, screenY)
            )
            drawCircle(
                color = surfaceColor.copy(alpha = 0.2f),
                radius = radius - 2f,
                center = Offset(screenX, screenY)
            )

            // Label
            drawContext.canvas.nativeCanvas.apply {
                val textSize = (12f * scale).coerceIn(8f, 24f)
                val paint = android.graphics.Paint().apply {
                    color = onSurfaceColor.hashCode()
                    this.textSize = textSize * density.density
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText(
                    node.tag,
                    screenX,
                    screenY + radius + textSize * density.density + 4f,
                    paint
                )
            }
        }
    }
}

private fun initializePositions(nodes: List<GraphNode>): List<NodePosition> {
    val spread = 200f
    return nodes.mapIndexed { i, _ ->
        val angle = (i.toFloat() / nodes.size) * 2 * Math.PI.toFloat()
        val r = spread * (0.3f + 0.7f * Math.random().toFloat())
        NodePosition(
            x = r * kotlin.math.cos(angle),
            y = r * kotlin.math.sin(angle)
        )
    }
}

private fun simulateStep(
    data: GraphData,
    positions: List<NodePosition>,
    temperature: Float
): List<NodePosition> {
    val result = positions.map { it.copy() }
    val n = result.size
    if (n == 0) return result

    val repulsionStrength = 5000f
    val springStrength = 0.01f
    val springLength = 150f
    val damping = 0.9f

    // Repulsion between all pairs
    for (i in 0 until n) {
        for (j in i + 1 until n) {
            val dx = result[i].x - result[j].x
            val dy = result[i].y - result[j].y
            val distSq = max(1f, dx * dx + dy * dy)
            val dist = sqrt(distSq)
            val force = repulsionStrength / distSq * temperature
            val fx = force * dx / dist
            val fy = force * dy / dist
            result[i].vx += fx
            result[i].vy += fy
            result[j].vx -= fx
            result[j].vy -= fy
        }
    }

    // Spring attraction along edges
    val nodeIndexMap = data.nodes.mapIndexed { i, n -> n.tag to i }.toMap()
    for (edge in data.edges) {
        val i1 = nodeIndexMap[edge.tag1] ?: continue
        val i2 = nodeIndexMap[edge.tag2] ?: continue
        val dx = result[i2].x - result[i1].x
        val dy = result[i2].y - result[i1].y
        val dist = max(1f, sqrt(dx * dx + dy * dy))
        val displacement = dist - springLength
        val force = springStrength * displacement * temperature
        val fx = force * dx / dist
        val fy = force * dy / dist
        result[i1].vx += fx
        result[i1].vy += fy
        result[i2].vx -= fx
        result[i2].vy -= fy
    }

    // Apply velocities with damping
    for (pos in result) {
        pos.vx *= damping
        pos.vy *= damping
        pos.x += pos.vx
        pos.y += pos.vy
    }

    return result
}

private fun nodeRadius(count: Int, maxCount: Int): Float {
    val normalized = count.toFloat() / max(1, maxCount)
    return 15f + normalized * 25f
}
