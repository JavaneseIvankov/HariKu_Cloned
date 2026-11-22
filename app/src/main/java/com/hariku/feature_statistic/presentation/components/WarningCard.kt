package com.hariku.feature_statistic.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hariku.R

@Composable
fun WarningCard() {
    val illustration = painterResource(R.drawable.ic_statistic_warning)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardDefaults.shape)
                .drawBehind {
                    val side = minOf(size.width, illustration.intrinsicSize.width)
                    val offset = 128.dp.toPx()
                    translate(left = size.width - side - offset, top = -offset) {
                        with(illustration) { draw(Size(side, side), alpha = 0.7f) }
                    }
                }
                .padding(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PERINGATAN!",
                color = Color.Black,
                textAlign = TextAlign.Center,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(vertical = 28.dp)
            )
            Text(
                text = "Gunakan laporan ini untuk melihat pola suasana hatimu. " +
                    "Jika kamu merasa kesulitan, bicarakan dengan orang terdekat atau tenaga profesional.",
                color = Color.Black,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.W400,
                fontSize = 16.sp
            )
        }
    }
}
