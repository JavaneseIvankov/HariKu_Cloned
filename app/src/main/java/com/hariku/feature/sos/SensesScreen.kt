package com.hariku.feature.sos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hariku.R
import com.hariku.core.ui.theme.Typography

private val ExerciseBackground = Color(0xFFF1F1F1)
private val ExerciseSurface = Color(0xFFF9F9F9)
private val ExerciseInk = Color(0xFF242424)
private val ExerciseAccent = Color(0xFFC77C51)
private val ExerciseTrack = Color(0xFFECB891)
private val ExerciseBody = Color(0xFF595959)
private val ExerciseFont = Typography.bodyLarge.fontFamily

private data class SenseStep(val title: Int, val description: Int, val icon: Int)

private val senseSteps = listOf(
    SenseStep(R.string.senses_see, R.string.senses_see_description, R.drawable.see),
    SenseStep(R.string.senses_touch, R.string.senses_touch_description, R.drawable.touch),
    SenseStep(R.string.senses_hear, R.string.senses_hear_description, R.drawable.hear),
    SenseStep(R.string.senses_smell, R.string.senses_smell_description, R.drawable.nose),
    SenseStep(R.string.senses_taste, R.string.senses_taste_description, R.drawable.taste),
    SenseStep(R.string.senses_done, R.string.senses_done_description, R.drawable.complete)
)

@Composable
fun SensesScreen(onExit: () -> Unit) {
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val step = senseSteps[stepIndex]
    val completed = stepIndex == senseSteps.lastIndex
    val onBack: () -> Unit = { if (stepIndex == 0) onExit() else stepIndex-- }
    BackHandler(onBack = onBack)

    Column(Modifier.fillMaxSize().background(ExerciseBackground).navigationBarsPadding()) {
        Box(
            Modifier.fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .background(ExerciseSurface)
                .statusBarsPadding()
                .heightIn(min = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.sos_back),
                    tint = ExerciseAccent
                )
            }
            Text(
                stringResource(R.string.senses_title),
                modifier = Modifier.padding(horizontal = 52.dp, vertical = 12.dp),
                color = ExerciseInk,
                fontFamily = ExerciseFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val viewportHeight = maxHeight
            val circleSize = (maxWidth * 0.63f).coerceAtMost(268.dp)
            Box(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight).padding(horizontal = 28.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(circleSize).shadow(6.dp, CircleShape).background(ExerciseSurface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val width = 15.dp.toPx()
                            val inset = width / 2
                            val arcSize = Size(size.width - width, size.height - width)
                            val topLeft = Offset(inset, inset)
                            drawArc(ExerciseTrack, 0f, 360f, false, topLeft, arcSize, style = Stroke(width))
                            if (stepIndex > 0) {
                                drawArc(
                                    ExerciseAccent, -90f, 360f * stepIndex / senseSteps.lastIndex,
                                    false, topLeft, arcSize, style = Stroke(width, cap = StrokeCap.Round)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painterResource(step.icon),
                                contentDescription = null,
                                modifier = Modifier.size(circleSize * if (completed) 0.36f else 0.44f)
                            )
                            if (!completed) {
                                Text(
                                    stringResource(R.string.senses_progress, stepIndex + 1, senseSteps.lastIndex),
                                    fontFamily = ExerciseFont,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ExerciseBody,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    Text(
                        stringResource(step.title),
                        modifier = Modifier.semantics { heading() },
                        color = ExerciseInk,
                        fontFamily = ExerciseFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(step.description),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                        color = ExerciseBody,
                        fontFamily = ExerciseFont,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Justify
                    )
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { if (completed) onExit() else stepIndex++ },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExerciseAccent,
                            contentColor = Color(0xFF26170E)
                        )
                    ) {
                        Text(
                            stringResource(if (completed) R.string.sos_back else R.string.senses_next),
                            fontFamily = ExerciseFont,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
