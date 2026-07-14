package com.decibelmeter.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.decibelmeter.app.domain.model.NoiseCategory
import com.decibelmeter.app.ui.theme.*
import com.decibelmeter.app.util.FormatUtil

/**
 * 共享 UI 组件库
 */

/**
 * 分贝环形仪表盘
 * 参考 Glyph-Decibel-Meter 的视觉仪表设计
 */
@Composable
fun DecibelGauge(
    currentDb: Float,
    modifier: Modifier = Modifier,
    size: Int = 240,
    strokeWidth: Float = 24f
) {
    val category = when {
        currentDb < 30f -> NoiseCategory.QUIET
        currentDb < 50f -> NoiseCategory.NORMAL
        currentDb < 65f -> NoiseCategory.MODERATE
        currentDb < 80f -> NoiseCategory.LOUD
        currentDb < 100f -> NoiseCategory.VERY_LOUD
        else -> NoiseCategory.DANGEROUS
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (currentDb.coerceIn(0f, 120f) / 120f),
        animationSpec = tween(durationMillis = 200),
        label = "db_progress"
    )

    val gaugeColors = listOf(
        Color(category.colorArgb),
        Color(category.colorArgb).copy(alpha = 0.3f)
    )

    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = this.size.minDimension
            val topLeft = Offset(
                (this.size.width - canvasSize) / 2 + strokeWidth / 2,
                (this.size.height - canvasSize) / 2 + strokeWidth / 2
            )
            val arcSize = androidx.compose.ui.geometry.Size(
                canvasSize - strokeWidth,
                canvasSize - strokeWidth
            )

            // 背景弧线
            drawArc(
                color = Color.Gray.copy(alpha = 0.15f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // 动态进度弧线
            drawArc(
                brush = Brush.sweepGradient(gaugeColors),
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }

        // 中心 dB 读数
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = FormatUtil.formatDb(currentDb),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color(category.colorArgb)
            )
            Text(
                text = "dB",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = Color(category.colorArgb).copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = category.label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(category.colorArgb)
                )
            }
        }
    }
}

/**
 * 统计信息卡片
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 历史波形图
 * 参考 SoundMeterESP 的图表设计，使用 Compose Canvas 绘制
 */
@Composable
fun WaveformChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Secondary
) {
    if (data.isEmpty()) return

    val animatedData by remember(data) { mutableStateOf(data) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            if (animatedData.size < 2) return@Canvas

            val width = size.width
            val height = size.height
            val stepX = width / (animatedData.size - 1)

            // 填充区域
            val fillPath = Path().apply {
                moveTo(0f, height)
                animatedData.forEachIndexed { index, db ->
                    val x = index * stepX
                    val normalizedDb = (db.coerceIn(0f, 100f) - 0f) / 100f
                    val y = height - (normalizedDb * height * 0.9f)
                    lineTo(x, y)
                }
                lineTo(width, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.3f),
                        lineColor.copy(alpha = 0.0f)
                    )
                )
            )

            // 线条
            val linePath = Path().apply {
                animatedData.forEachIndexed { index, db ->
                    val x = index * stepX
                    val normalizedDb = (db.coerceIn(0f, 100f) - 0f) / 100f
                    val y = height - (normalizedDb * height * 0.9f)
                    if (index == 0) moveTo(x, y) else lineTo(x, y)
                }
            }

            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
    }
}

/**
 * 自定义滑块 (带标签)
 */
@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(value * 100).toInt()}%",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
