package com.hariku.feature_statistic.presentation.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

@Composable
fun GroupedBarChart(data: List<WeeklyData>) {
    if (data.isEmpty()) return

    val density = LocalDensity.current
    val labelSize = with(density) { 11.sp.toPx() }
    val axisPaint = remember(labelSize) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.DKGRAY
            textAlign = Paint.Align.RIGHT
            textSize = labelSize
        }
    }
    val datePaint = remember(axisPaint) {
        Paint(axisPaint).apply { textAlign = Paint.Align.CENTER }
    }
    val dateAngle = 45f
    val dateHeight = datePaint.descent() - datePaint.ascent()
    val widestDate = data.maxOf { datePaint.measureText(it.dateLabel) }
    // At 45 degrees, the rotated label bounds occupy the same width and height.
    val rotatedDateSize = (widestDate + dateHeight) / sqrt(2f)
    val dateSpace = with(density) { rotatedDateSize.toDp() } + 18.dp
    val axisWidth = with(density) { axisPaint.measureText("100%").toDp() } + 8.dp
    val minWeekWidth = maxOf(48.dp, with(density) { rotatedDateSize.toDp() } + 12.dp)

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val chartWidth = maxOf(maxWidth, axisWidth + minWeekWidth * data.size + 4.dp)
        val chartHeight = (maxWidth * 0.67f).coerceIn(150.dp, 230.dp) + dateSpace
        Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Canvas(Modifier.width(chartWidth).height(chartHeight)) {
                val labelCenterToBaseline = -(axisPaint.ascent() + axisPaint.descent()) / 2
                val plotLeft = axisWidth.toPx()
                val plotRight = size.width - 4.dp.toPx()
                val plotTop = labelSize
                val plotBottom = size.height - dateSpace.toPx()
                val dateCenterY = plotBottom + 10.dp.toPx() + rotatedDateSize / 2
                val plotHeight = plotBottom - plotTop
                if (plotHeight <= 0f) return@Canvas

                val lineWidth = 1.dp.toPx()
                for (percent in 0..100 step 20) {
                    val y = plotBottom - plotHeight * (percent / 100f)
                    drawLine(
                        color = if (percent == 0) Color.Gray else ColorGridLine,
                        start = Offset(plotLeft, y),
                        end = Offset(plotRight, y),
                        strokeWidth = lineWidth
                    )
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(
                            "$percent%", plotLeft - 6.dp.toPx(), y + labelCenterToBaseline, axisPaint
                        )
                    }
                }
                drawLine(Color.Gray, Offset(plotLeft, plotTop), Offset(plotLeft, plotBottom), lineWidth)

                val weekWidth = (plotRight - plotLeft) / data.size
                val gap = 2.dp.toPx()
                val barWidth = ((weekWidth * 0.86f - 2 * gap) / 3).coerceIn(1f, 32.dp.toPx())
                data.forEachIndexed { index, week ->
                    val center = plotLeft + (index + 0.5f) * weekWidth
                    fun drawBar(value: Float, slot: Int, color: Color) {
                        val height = (value / 100f).coerceIn(0f, 1f) * plotHeight
                        if (height > 0f) {
                            drawRect(
                                color,
                                Offset(center - barWidth / 2 + slot * (barWidth + gap), plotBottom - height),
                                Size(barWidth, height)
                            )
                        }
                    }

                    drawBar(week.negative, -1, BarColorNegative)
                    drawBar(week.positive, 0, BarColorPositive)
                    drawBar(week.neutral, 1, BarColorNeutral)
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        val checkpoint = nativeCanvas.save()
                        nativeCanvas.rotate(dateAngle, center, dateCenterY)
                        nativeCanvas.drawText(
                            week.dateLabel, center, dateCenterY + labelCenterToBaseline, datePaint
                        )
                        nativeCanvas.restoreToCount(checkpoint)
                    }
                }
            }
        }
    }
}
