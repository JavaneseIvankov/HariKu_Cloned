package com.hariku.feature.mood.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodTrackerLogic
import com.hariku.feature.mood.domain.model.MoodType
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val MoodInk = Color(0xFF352C28)
private val MoodMuted = Color(0xFF74665E)
private val MoodSage = Color(0xFF5D9467)
private val MoodSageSoft = Color(0xFFEAF1E8)
private val MoodWarmSoft = Color(0xFFF9EEE5)
private val MoodAccent = Color(0xFFC97D50)
private val MoodErrorBg = Color(0xFFFDECEB)
private val MoodErrorInk = Color(0xFFB33A32)

@Composable
fun QuickMoodTrackerSection(
    modifier: Modifier = Modifier,
    viewModel: QuickMoodViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    QuickMoodTrackerCard(
        state = state,
        onSelectMood = viewModel::onSelectMood,
        onNoteInputChanged = viewModel::onNoteInputChanged,
        onToggleNoteInput = viewModel::onToggleNoteInput,
        onToggleHistory = viewModel::onToggleHistory,
        onUndoLastSaved = viewModel::onUndoLastSaved,
        onDeleteEntry = viewModel::onDeleteEntry,
        onDismissFeedback = viewModel::onDismissFeedback,
        onRetryLoad = viewModel::retryLoad,
        modifier = modifier
    )
}

@Composable
fun QuickMoodTrackerCard(
    state: QuickMoodUiState,
    onSelectMood: (MoodType) -> Unit,
    onNoteInputChanged: (String) -> Unit,
    onToggleNoteInput: () -> Unit,
    onToggleHistory: () -> Unit,
    onUndoLastSaved: () -> Unit,
    onDeleteEntry: (Long) -> Unit,
    onDismissFeedback: () -> Unit,
    onRetryLoad: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val latestEntry = state.latestTodayEntry

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Suasana hati",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MoodSage
                )
                if (state.todayCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MoodSageSoft)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${state.todayCount} catatan hari ini",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MoodSage
                        )
                    }
                }
            }

            // Title & latest mood subtitle
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (latestEntry == null) {
                        "Bagaimana suasana hatimu hari ini?"
                    } else {
                        "Mood terakhir: ${latestEntry.mood.label}"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (latestEntry == null) MoodInk else MoodSage
                )
                Text(
                    text = if (latestEntry == null) {
                        "Ketuk salah satu ekspresi untuk mencatat suasana hatimu dengan cepat."
                    } else {
                        val timeText = formatMoodTime(latestEntry.timestamp, timeFormatter)
                        "Tercatat pukul $timeText · Ketuk ekspresi lain kapan saja untuk memperbarui."
                    },
                    fontSize = 12.sp,
                    color = MoodMuted,
                    lineHeight = 18.sp
                )
            }

            // Body: loading / load error / mood grid
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MoodSage,
                            strokeWidth = 3.dp
                        )
                    }
                }

                state.loadError != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MoodErrorBg)
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.loadError,
                            fontSize = 13.sp,
                            color = MoodErrorInk,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onRetryLoad,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MoodAccent)
                        ) {
                            Text("Coba lagi", fontSize = 13.sp)
                        }
                    }
                }

                else -> {
                    // Optional short note toggle & field
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MoodWarmSoft)
                                .clickable(enabled = !state.isSaving, onClick = onToggleNoteInput)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EditNote,
                                contentDescription = null,
                                tint = MoodAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (state.isNoteExpanded) {
                                    "Sembunyikan catatan singkat"
                                } else {
                                    "Tambah catatan singkat (opsional)"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MoodInk
                            )
                        }

                        AnimatedVisibility(visible = state.isNoteExpanded) {
                            OutlinedTextField(
                                value = state.noteInput,
                                onValueChange = onNoteInputChanged,
                                enabled = !state.isSaving,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        text = "Apa yang sedang kamu rasakan? (opsional)",
                                        fontSize = 13.sp,
                                        color = MoodMuted
                                    )
                                },
                                supportingText = {
                                    Text(
                                        text = "${state.noteInput.length}/${MoodTrackerLogic.MAX_NOTE_LENGTH} · Ketuk ekspresi di bawah untuk menyimpan",
                                        fontSize = 11.sp,
                                        color = MoodMuted
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MoodSage,
                                    unfocusedBorderColor = Color(0xFFE0D6CE),
                                    focusedTextColor = MoodInk,
                                    unfocusedTextColor = MoodInk
                                )
                            )
                        }
                    }

                    // 2x5 mood emote grid
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MoodVisualCatalog.ordered.chunked(5).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowItems.forEach { visual ->
                                    MoodOptionItem(
                                        visual = visual,
                                        isSelected = latestEntry?.mood == visual.type,
                                        isDisabled = state.isSaving,
                                        onClick = { onSelectMood(visual.type) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Feedback / error banner
                    if (state.actionError != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MoodErrorBg)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.actionError,
                                fontSize = 12.sp,
                                color = MoodErrorInk,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onDismissFeedback,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Tutup",
                                    tint = MoodErrorInk,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else if (state.feedbackMessage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MoodSageSoft)
                                .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.feedbackMessage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MoodSage,
                                modifier = Modifier.weight(1f)
                            )
                            if (state.lastSavedEntryId != null) {
                                TextButton(onClick = onUndoLastSaved, enabled = !state.isSaving) {
                                    Text(
                                        text = "Batalkan",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MoodAccent
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismissFeedback,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Tutup",
                                    tint = MoodSage,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Collapsible today's check-in history
                    if (state.todayEntries.isNotEmpty()) {
                        HorizontalDivider(color = Color(0xFFF1EAE4))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onToggleHistory)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Riwayat hari ini (${state.todayCount})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MoodInk
                            )
                            Icon(
                                imageVector = if (state.isHistoryExpanded) {
                                    Icons.Filled.ExpandLess
                                } else {
                                    Icons.Filled.ExpandMore
                                },
                                contentDescription = if (state.isHistoryExpanded) "Tutup riwayat" else "Lihat riwayat",
                                tint = MoodMuted
                            )
                        }

                        AnimatedVisibility(visible = state.isHistoryExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.todayEntries.forEach { entry ->
                                    TodayMoodRow(
                                        entry = entry,
                                        timeText = formatMoodTime(entry.timestamp, timeFormatter),
                                        isDisabled = state.isSaving,
                                        onDelete = { onDeleteEntry(entry.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodOptionItem(
    visual: MoodVisual,
    isSelected: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .alpha(if (isDisabled && !isSelected) 0.45f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isDisabled, onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(MoodSage.copy(alpha = 0.16f), CircleShape)
                        .border(2.5.dp, MoodSage, CircleShape)
                )
            }
            Image(
                painter = painterResource(id = visual.iconRes),
                contentDescription = visual.type.label,
                modifier = Modifier.size(36.dp)
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
            color = if (isSelected) MoodSage else MoodInk
        )
    }
}

@Composable
private fun TodayMoodRow(
    entry: MoodEntry,
    timeText: String,
    isDisabled: Boolean,
    onDelete: () -> Unit
) {
    val visual = MoodVisualCatalog.of(entry.mood)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFAF6F2))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = visual.iconRes),
            contentDescription = entry.mood.label,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = entry.mood.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MoodInk
                )
                Text(
                    text = "· $timeText",
                    fontSize = 11.sp,
                    color = MoodMuted
                )
            }
            if (entry.note.isNotBlank()) {
                Text(
                    text = entry.note,
                    fontSize = 12.sp,
                    color = MoodMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(
            onClick = onDelete,
            enabled = !isDisabled,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = "Hapus catatan ${entry.mood.label}",
                tint = MoodMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun formatMoodTime(
    timestamp: Long,
    formatter: DateTimeFormatter,
    zone: ZoneId = ZoneId.systemDefault()
): String = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalTime().format(formatter)

@Preview(showBackground = true)
@Composable
private fun QuickMoodTrackerCardPreview() {
    HariKuTheme {
        QuickMoodTrackerCard(
            state = QuickMoodUiState(
                isLoading = false,
                latestTodayEntry = MoodEntry(
                    id = 1L,
                    mood = MoodType.SENANG,
                    timestamp = System.currentTimeMillis(),
                    note = "Selesai mengerjakan tugas lebih awal"
                ),
                todayEntries = listOf(
                    MoodEntry(
                        id = 1L,
                        mood = MoodType.SENANG,
                        timestamp = System.currentTimeMillis(),
                        note = "Selesai mengerjakan tugas lebih awal"
                    )
                ),
                feedbackMessage = "Mood Senang tersimpan",
                lastSavedEntryId = 1L
            ),
            onSelectMood = {},
            onNoteInputChanged = {},
            onToggleNoteInput = {},
            onToggleHistory = {},
            onUndoLastSaved = {},
            onDeleteEntry = {},
            onDismissFeedback = {},
            onRetryLoad = {}
        )
    }
}
