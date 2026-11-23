package com.hariku.feature.sos

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hariku.R

private val Background = Color(0xFFF9F9F9)
private val Ink = Color(0xFF242424)
private val Rose = Color(0xFFBB5366)
private val RoseText = Color(0xFFA14255)
private val CardShape = RoundedCornerShape(15.dp)
private val Rubik = FontFamily(
    Font(R.font.rubik_regular),
    Font(R.font.rubik_semibold, FontWeight.SemiBold),
    Font(R.font.rubik_bold, FontWeight.Bold)
)

@Composable
fun SosScreen(onBack: () -> Unit, onSenses: () -> Unit, onProfessional: () -> Unit) {
    SosLayout(onBack) {
        Text(stringResource(R.string.sos_question))
        Spacer(Modifier.height(26.dp))
        Text(
            stringResource(R.string.sos_headline),
            modifier = Modifier.semantics { heading() },
            fontSize = 24.sp,
            lineHeight = 29.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(26.dp))
        Text(stringResource(R.string.sos_support))
        Spacer(Modifier.height(42.dp))
        OutlinedButton(
            onClick = onSenses,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = CardShape,
            border = BorderStroke(2.5.dp, Rose)
        ) {
            Text(
                stringResource(R.string.sos_senses),
                color = RoseText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onProfessional,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = CardShape,
            colors = ButtonDefaults.buttonColors(containerColor = RoseText, contentColor = Color.White)
        ) {
            Text(
                stringResource(R.string.sos_professional),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SosProfessionalScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dialUnavailable = stringResource(R.string.sos_dial_unavailable)
    val openDialer: (String) -> Unit = { number ->
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, dialUnavailable, Toast.LENGTH_SHORT).show()
        }
    }

    SosLayout(onBack) {
        Text(stringResource(R.string.sos_professional_intro))
        Spacer(Modifier.height(26.dp))
        Text(stringResource(R.string.sos_professional_prompt))
        Spacer(Modifier.height(26.dp))
        HotlineCard(
            title = stringResource(R.string.sos_government),
            label = stringResource(R.string.sos_medical_emergency),
            number = "119",
            onDial = openDialer
        )
        Spacer(Modifier.height(26.dp))
        HotlineCard(
            title = stringResource(R.string.sos_ministry),
            label = stringResource(R.string.sos_halo_kemkes),
            number = "1500-567",
            onDial = openDialer
        )
        Spacer(Modifier.height(26.dp))
        HotlineCard(
            title = stringResource(R.string.sos_save_yourselves),
            label = stringResource(R.string.sos_contact_unverified),
            number = null,
            onDial = openDialer
        )
    }
}

@Composable
private fun HotlineCard(title: String, label: String, number: String?, onDial: (String) -> Unit) {
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
            Text(
                buildAnnotatedString {
                    append(label)
                    if (number != null) {
                        append(" ")
                        withStyle(SpanStyle(color = RoseText, textDecoration = TextDecoration.Underline)) {
                            append(number)
                        }
                    }
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
    val border = BorderStroke(2.5.dp, Rose)
    if (number == null) {
        Surface(shape = CardShape, color = Background, contentColor = Ink, border = border, content = content)
    } else {
        Surface(
            onClick = { onDial(number) },
            shape = CardShape,
            color = Background,
            contentColor = Ink,
            border = border,
            content = content
        )
    }
}

@Composable
private fun SosLayout(onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(Background).safeDrawingPadding()) {
        Box(Modifier.fillMaxWidth().height(48.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.sos_back), tint = Ink)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val verticalPadding = 16.dp
            val remainingHeight = maxHeight
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .heightIn(min = remainingHeight)
                    .padding(top = verticalPadding, bottom = 48.dp + verticalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    Modifier.widthIn(max = 430.dp).fillMaxWidth(0.77f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CompositionLocalProvider(
                        LocalTextStyle provides TextStyle(
                            color = Ink,
                            fontFamily = Rubik,
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Center
                        ),
                        content = content
                    )
                }
            }
        }
    }
}
