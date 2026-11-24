package com.hariku.feature.mood.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hariku.feature.mood.domain.model.MoodType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class MoodBurstStyle {
    JOYFUL_SPARKS,
    DOUBLE_PULSE,
    SOFT_RIPPLE
}

data class KeyframePoint(
    val fraction: Float,
    val value: Float
)

data class MoodMotionFrame(
    val scaleX: Float,
    val scaleY: Float,
    val offsetXDp: Float,
    val offsetYDp: Float,
    val rotationDeg: Float,
    val iconAlpha: Float
)

data class MoodTapMotionSpec(
    val durationMillis: Int,
    val burstStyle: MoodBurstStyle,
    val scaleXKeyframes: List<KeyframePoint>,
    val scaleYKeyframes: List<KeyframePoint> = scaleXKeyframes,
    val offsetXKeyframesDp: List<KeyframePoint> = REST_ZERO,
    val offsetYKeyframesDp: List<KeyframePoint> = REST_ZERO,
    val rotationKeyframesDeg: List<KeyframePoint> = REST_ZERO,
    val alphaKeyframes: List<KeyframePoint> = REST_ONE
) {
    fun sampleAt(progress: Float): MoodMotionFrame {
        val clamped = progress.coerceIn(0f, 1f)
        return MoodMotionFrame(
            scaleX = interpolateSmooth(scaleXKeyframes, clamped, defaultValue = 1f),
            scaleY = interpolateSmooth(scaleYKeyframes, clamped, defaultValue = 1f),
            offsetXDp = interpolateSmooth(offsetXKeyframesDp, clamped, defaultValue = 0f),
            offsetYDp = interpolateSmooth(offsetYKeyframesDp, clamped, defaultValue = 0f),
            rotationDeg = interpolateSmooth(rotationKeyframesDeg, clamped, defaultValue = 0f),
            iconAlpha = interpolateSmooth(alphaKeyframes, clamped, defaultValue = 1f).coerceIn(0f, 1f)
        )
    }

    companion object {
        val REST_ZERO = listOf(KeyframePoint(0f, 0f), KeyframePoint(1f, 0f))
        val REST_ONE = listOf(KeyframePoint(0f, 1f), KeyframePoint(1f, 1f))

        private fun interpolateSmooth(
            points: List<KeyframePoint>,
            fraction: Float,
            defaultValue: Float
        ): Float {
            if (points.isEmpty()) return defaultValue
            if (fraction <= points.first().fraction) return points.first().value
            if (fraction >= points.last().fraction) return points.last().value

            for (index in 0 until points.lastIndex) {
                val start = points[index]
                val end = points[index + 1]
                if (fraction <= end.fraction) {
                    val span = (end.fraction - start.fraction).coerceAtLeast(1e-5f)
                    val localT = ((fraction - start.fraction) / span).coerceIn(0f, 1f)
                    val smoothT = localT * localT * (3f - 2f * localT)
                    return start.value + (end.value - start.value) * smoothT
                }
            }
            return points.last().value
        }
    }
}

object MoodTapMotionCatalog {

    private val sparkAnglesRad = floatArrayOf(
        (-90.0 * PI / 180.0).toFloat(),
        (-35.0 * PI / 180.0).toFloat(),
        (22.0 * PI / 180.0).toFloat(),
        (90.0 * PI / 180.0).toFloat(),
        (158.0 * PI / 180.0).toFloat(),
        (215.0 * PI / 180.0).toFloat()
    )

    fun specFor(mood: MoodType): MoodTapMotionSpec = when (mood) {
        MoodType.SENANG -> MoodTapMotionSpec(
            durationMillis = 560,
            burstStyle = MoodBurstStyle.JOYFUL_SPARKS,
            scaleXKeyframes = pts(0f to 1f, 0.14f to 1.08f, 0.36f to 1.24f, 0.58f to 1.16f, 0.78f to 1.07f, 1f to 1f),
            scaleYKeyframes = pts(0f to 1f, 0.14f to 0.92f, 0.36f to 1.30f, 0.58f to 1.18f, 0.78f to 0.94f, 1f to 1f),
            offsetYKeyframesDp = pts(0f to 0f, 0.14f to 1.5f, 0.36f to -8.0f, 0.58f to -4.5f, 0.78f to 1.2f, 1f to 0f),
            rotationKeyframesDeg = pts(0f to 0f, 0.36f to -11f, 0.58f to 10f, 0.78f to -3f, 1f to 0f)
        )

        MoodType.SEMANGAT -> MoodTapMotionSpec(
            durationMillis = 620,
            burstStyle = MoodBurstStyle.JOYFUL_SPARKS,
            scaleXKeyframes = pts(0f to 1f, 0.12f to 1.10f, 0.30f to 1.26f, 0.50f to 1.08f, 0.70f to 1.18f, 0.86f to 1.04f, 1f to 1f),
            scaleYKeyframes = pts(0f to 1f, 0.12f to 0.90f, 0.30f to 1.34f, 0.50f to 0.95f, 0.70f to 1.22f, 0.86f to 0.97f, 1f to 1f),
            offsetYKeyframesDp = pts(0f to 0f, 0.12f to 1.5f, 0.30f to -9.5f, 0.50f to -1.0f, 0.70f to -5.5f, 0.86f to 0.8f, 1f to 0f),
            rotationKeyframesDeg = pts(0f to 0f, 0.30f to 13f, 0.50f to -11f, 0.70f to 8f, 0.86f to -2f, 1f to 0f)
        )

        MoodType.BIASA -> MoodTapMotionSpec(
            durationMillis = 480,
            burstStyle = MoodBurstStyle.SOFT_RIPPLE,
            scaleXKeyframes = pts(0f to 1f, 0.28f to 1.18f, 0.62f to 1.05f, 0.82f to 0.98f, 1f to 1f),
            offsetYKeyframesDp = pts(0f to 0f, 0.28f to -3.0f, 0.62f to -1.0f, 0.82f to 0.5f, 1f to 0f),
            rotationKeyframesDeg = pts(0f to 0f, 0.28f to -6f, 0.62f to 5f, 0.82f to -1.5f, 1f to 0f)
        )

        MoodType.SEDIH -> MoodTapMotionSpec(
            durationMillis = 620,
            burstStyle = MoodBurstStyle.SOFT_RIPPLE,
            scaleXKeyframes = pts(0f to 1f, 0.20f to 1.15f, 0.52f to 0.96f, 0.76f to 0.98f, 1f to 1f),
            scaleYKeyframes = pts(0f to 1f, 0.20f to 1.15f, 0.52f to 0.92f, 0.76f to 0.95f, 1f to 1f),
            offsetYKeyframesDp = pts(0f to 0f, 0.20f to -2.0f, 0.52f to 5.0f, 0.76f to 3.2f, 1f to 0f),
            rotationKeyframesDeg = pts(0f to 0f, 0.20f to -3f, 0.52f to -9f, 0.76f to -5f, 1f to 0f)
        )

        MoodType.MARAH -> MoodTapMotionSpec(
            durationMillis = 520,
            burstStyle = MoodBurstStyle.DOUBLE_PULSE,
            scaleXKeyframes = pts(0f to 1f, 0.20f to 1.28f, 0.65f to 1.16f, 0.85f to 0.97f, 1f to 1f),
            scaleYKeyframes = pts(0f to 1f, 0.20f to 1.24f, 0.65f to 1.14f, 0.85f to 0.97f, 1f to 1f),
            offsetXKeyframesDp = pts(0f to 0f, 0.15f to -5.0f, 0.30f to 5.0f, 0.45f to -4.2f, 0.60f to 4.2f, 0.75f to -2.2f, 0.88f to 1.5f, 1f to 0f),
            offsetYKeyframesDp = pts(0f to 0f, 0.22f to -2.5f, 0.70f to -1.0f, 1f to 0f),
            rotationKeyframesDeg = pts(0f to 0f, 0.15f to -9f, 0.30f to 9f, 0.45f to -7f, 0.60f to 7f, 0.75f to -3f, 1f to 0f)
        )

        MoodType.TAKUT -> MoodTapMotionSpec(
            durationMillis = 560,
            burstStyle = MoodBurstStyle.DOUBLE_PULSE,
            scaleXKeyframes = pts(0f to 1f, 0.16f to 1.18f, 0.40f to 0.88f, 0.72f to 0.94f, 0.88f to 1.03f, 1f to 1f),
            scaleYKeyframes = pts(0f to 1f, 0.16f to 1.24f, 0.40f to 0.86f, 0.72f to 0.93f, 0.88f to 1.03f, 1f to 1f),
            offsetXKeyframesDp = pts(0f to 0f, 0.24f to -3.2f, 0.34f to 3.2f, 0.44f to -2.8f, 0.54f to 2.8f, 0.64f to -2.0f, 0.74f to 2.0f, 0.86f to -1.0f, 1f to 0f),
            offsetYKeyframesDp = pts(0f to 0f, 0.16f to -4.5f, 0.42f to 2.5f, 0.72f to 1.5f, 1f to 0f)
        )

        MoodType.CEMAS -> MoodTapMotionSpec(
            durationMillis = 540,
            burstStyle = MoodBurstStyle.DOUBLE_PULSE,
            scaleXKeyframes = pts(0f to 1f, 0.20f to 1.20f, 0.45f to 0.95f, 0.70f to 1.08f, 1f to 1f),
            scaleYKeyframes = pts(0f to 1f, 0.20f to 1.20f, 0.45f to 0.97f, 0.70f to 1.08f, 1f to 1f),
            offsetXKeyframesDp = pts(0f to 0f, 0.14f to -2.8f, 0.28f to 2.8f, 0.42f to -2.4f, 0.56f to 2.4f, 0.72f to -1.4f, 0.86f to 1.0f, 1f to 0f),
            offsetYKeyframesDp = pts(0f to 0f, 0.22f to -2.5f, 0.50f to 1.0f, 0.72f to -1.5f, 1f to 0f),
            rotationKeyframesDeg = pts(0f to 0f, 0.14f to -10f, 0.28f to 10f, 0.42f to -8f, 0.56f to 8f, 0.70f to -5f, 0.84f to 4f, 1f to 0f)
        )

        MoodType.KECEWA -> MoodTapMotionSpec(
            durationMillis = 580,
            burstStyle = MoodBurstStyle.SOFT_RIPPLE,
            scaleXKeyframes = pts(0f to 1f, 0.20f to 1.16f, 0.52f to 0.94f, 0.80f to 0.98f, 1f to 1f),
            scaleYKeyframes = pts(0f to 1f, 0.20f to 1.16f, 0.52f to 0.92f, 0.80f to 0.97f, 1f to 1f),
            offsetXKeyframesDp = pts(0f to 0f, 0.32f to -3.8f, 0.56f to 3.8f, 0.78f to -2.0f, 1f to 0f),
            offsetYKeyframesDp = pts(0f to 0f, 0.20f to -2.0f, 0.55f to 3.8f, 0.80f to 2.0f, 1f to 0f),
            rotationKeyframesDeg = pts(0f to 0f, 0.32f to -7f, 0.56f to 6f, 0.78f to -3f, 1f to 0f)
        )

        MoodType.LELAH -> MoodTapMotionSpec(
            durationMillis = 650,
            burstStyle = MoodBurstStyle.SOFT_RIPPLE,
            scaleXKeyframes = pts(0f to 1f, 0.22f to 1.14f, 0.58f to 1.06f, 0.82f to 1.02f, 1f to 1f),
            scaleYKeyframes = pts(0f to 1f, 0.22f to 1.18f, 0.58f to 0.87f, 0.82f to 0.94f, 1f to 1f),
            offsetYKeyframesDp = pts(0f to 0f, 0.22f to -2.5f, 0.58f to 6.0f, 0.82f to 3.5f, 1f to 0f),
            rotationKeyframesDeg = pts(0f to 0f, 0.22f to 2f, 0.60f to 8f, 0.82f to 5f, 1f to 0f)
        )

        MoodType.HAMPA -> MoodTapMotionSpec(
            durationMillis = 600,
            burstStyle = MoodBurstStyle.SOFT_RIPPLE,
            scaleXKeyframes = pts(0f to 1f, 0.25f to 1.14f, 0.60f to 0.95f, 1f to 1f),
            offsetYKeyframesDp = pts(0f to 0f, 0.30f to -3.5f, 0.68f to 2.0f, 1f to 0f),
            rotationKeyframesDeg = pts(0f to 0f, 0.35f to -4f, 0.70f to 4f, 1f to 0f),
            alphaKeyframes = pts(0f to 1f, 0.40f to 0.62f, 0.75f to 0.82f, 1f to 1f)
        )
    }

    internal fun DrawScope.drawMoodEffects(
        accentColor: Color,
        selectedColor: Color,
        selectionProgress: Float,
        tapProgress: Float,
        burstStyle: MoodBurstStyle
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val ringColor = accentColor.deepenForRing(0.22f)

        // 1. Animated selection ring & soft background tint
        if (selectionProgress > 0.005f) {
            val selAlpha = selectionProgress.coerceIn(0f, 1f)
            val selScale = 0.72f + 0.28f * selectionProgress
            val selRadius = (size.minDimension / 2f) * selScale
            val strokePx = 2.5.dp.toPx()

            drawCircle(
                color = accentColor.copy(alpha = (0.28f * selAlpha).coerceIn(0f, 1f)),
                radius = selRadius.coerceAtLeast(0f),
                center = center
            )
            drawCircle(
                color = selectedColor.copy(alpha = (0.12f * selAlpha).coerceIn(0f, 1f)),
                radius = selRadius.coerceAtLeast(0f),
                center = center
            )
            drawCircle(
                color = selectedColor.copy(alpha = selAlpha),
                radius = (selRadius - strokePx / 2f).coerceAtLeast(0f),
                center = center,
                style = Stroke(width = strokePx)
            )
        }

        // 2. Tap burst wave ring + mood-specific particles/ripples
        if (tapProgress > 0.001f && tapProgress < 0.999f) {
            val ringT = (tapProgress / 0.88f).coerceIn(0f, 1f)
            val eased = 1f - (1f - ringT) * (1f - ringT)
            val fade = ((1f - ringT) * (1f - ringT)).coerceIn(0f, 1f)

            val haloRadius = lerpFloat(16.dp.toPx(), 29.dp.toPx(), eased)
            drawCircle(
                color = accentColor.copy(alpha = (0.42f * fade).coerceIn(0f, 1f)),
                radius = haloRadius,
                center = center
            )

            val waveRadius = lerpFloat(17.dp.toPx(), 30.dp.toPx(), eased)
            val waveStroke = lerpFloat(3.2.dp.toPx(), 0.9.dp.toPx(), eased)
            drawCircle(
                color = ringColor.copy(alpha = (0.85f * fade).coerceIn(0f, 1f)),
                radius = waveRadius,
                center = center,
                style = Stroke(width = waveStroke)
            )

            when (burstStyle) {
                MoodBurstStyle.DOUBLE_PULSE -> {
                    if (tapProgress > 0.14f) {
                        val secondT = ((tapProgress - 0.14f) / 0.78f).coerceIn(0f, 1f)
                        val eased2 = 1f - (1f - secondT) * (1f - secondT)
                        val fade2 = ((1f - secondT) * (1f - secondT)).coerceIn(0f, 1f)
                        drawCircle(
                            color = ringColor.copy(alpha = (0.65f * fade2).coerceIn(0f, 1f)),
                            radius = lerpFloat(16.dp.toPx(), 27.dp.toPx(), eased2),
                            center = center,
                            style = Stroke(width = lerpFloat(2.4.dp.toPx(), 0.8.dp.toPx(), eased2))
                        )
                    }
                }

                MoodBurstStyle.JOYFUL_SPARKS -> {
                    val sparkDist = lerpFloat(18.dp.toPx(), 31.dp.toPx(), eased)
                    val sparkRadius = lerpFloat(2.6.dp.toPx(), 0.8.dp.toPx(), eased)
                    val sparkColor = ringColor.copy(alpha = (0.90f * fade).coerceIn(0f, 1f))
                    for (angle in sparkAnglesRad) {
                        val dotCenter = Offset(
                            x = center.x + cos(angle) * sparkDist,
                            y = center.y + sin(angle) * sparkDist
                        )
                        drawCircle(
                            color = sparkColor,
                            radius = sparkRadius,
                            center = dotCenter
                        )
                    }
                }

                MoodBurstStyle.SOFT_RIPPLE -> Unit
            }
        }
    }

    private fun pts(vararg pairs: Pair<Float, Float>): List<KeyframePoint> =
        pairs.map { (fraction, value) -> KeyframePoint(fraction, value) }

    private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
        start + (stop - start) * fraction

    private fun Color.deepenForRing(amount: Float): Color = Color(
        red = (red * (1f - amount)).coerceIn(0f, 1f),
        green = (green * (1f - amount)).coerceIn(0f, 1f),
        blue = (blue * (1f - amount)).coerceIn(0f, 1f),
        alpha = 1f
    )
}

@Composable
internal fun AnimatedMoodOptionItem(
    visual: MoodVisual,
    isSelected: Boolean,
    isDisabled: Boolean,
    selectedColor: Color,
    defaultLabelColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    var tapToken by remember { mutableIntStateOf(0) }
    val tapProgress = remember { Animatable(0f) }
    val motionSpec = remember(visual.type) { MoodTapMotionCatalog.specFor(visual.type) }

    LaunchedEffect(tapToken) {
        if (tapToken > 0) {
            tapProgress.snapTo(0f)
            tapProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = motionSpec.durationMillis,
                    easing = LinearEasing
                )
            )
            tapProgress.snapTo(0f)
        }
    }

    val isAnimatingTap = tapProgress.isRunning
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !isDisabled) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "mood_press_scale"
    )
    val selectionProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "mood_selection_progress"
    )
    val itemAlpha by animateFloatAsState(
        targetValue = if (isDisabled && !isSelected && !isAnimatingTap) 0.45f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "mood_item_alpha"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else defaultLabelColor,
        animationSpec = tween(durationMillis = 200),
        label = "mood_label_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .semantics { selected = isSelected }
            .graphicsLayer { alpha = itemAlpha.coerceIn(0f, 1f) }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isDisabled,
                role = Role.Button,
                onClick = {
                    tapToken += 1
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .drawBehind {
                    with(MoodTapMotionCatalog) {
                        drawMoodEffects(
                            accentColor = visual.accentColor,
                            selectedColor = selectedColor,
                            selectionProgress = selectionProgress,
                            tapProgress = tapProgress.value,
                            burstStyle = motionSpec.burstStyle
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = visual.iconRes),
                contentDescription = visual.type.label,
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer {
                        val frame = motionSpec.sampleAt(tapProgress.value)
                        scaleX = pressScale * frame.scaleX
                        scaleY = pressScale * frame.scaleY
                        translationX = frame.offsetXDp.dp.toPx()
                        translationY = frame.offsetYDp.dp.toPx()
                        rotationZ = frame.rotationDeg
                        alpha = frame.iconAlpha
                    }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = visual.type.label,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = labelColor,
            modifier = Modifier.graphicsLayer {
                val labelPop = if (tapProgress.value > 0f && tapProgress.value < 1f) {
                    1f + 0.08f * sin(tapProgress.value * PI.toFloat())
                } else {
                    1f
                }
                scaleX = labelPop
                scaleY = labelPop
            }
        )
    }
}
