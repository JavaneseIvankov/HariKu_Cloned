package com.hariku.feature_statistic.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hariku.core.ui.components.SosTopBar
import com.hariku.feature.mood.MoodReportScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun StatisticScreen(
    onOpenSos: () -> Unit,
    viewModel: StatisticViewModel = koinViewModel()
) {
    val records by viewModel.records.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(Color(0xFFF9F9F9))) {
        SosTopBar(title = "Statistik", onSosClick = onOpenSos)
        Box(Modifier.weight(1f)) {
            MoodReportScreen(records)
        }
    }
}
