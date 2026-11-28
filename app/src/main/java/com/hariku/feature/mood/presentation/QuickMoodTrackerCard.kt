package com.hariku.feature.mood.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.School
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.mood.domain.model.MoodEntry
import com.hariku.feature.mood.domain.model.MoodTrackerLogic
import com.hariku.feature.mood.domain.model.MoodType
import com.hariku.feature.mood.domain.model.QuickContextTagId
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
private val MoodStreakBg = Color(0xFFFFF3E0)
private val MoodStreakInk = Color(0xFFB85D19)
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
        onToggleQuickTag = viewModel::onToggleQuickTag,
        onToggleHistory = viewModel::onToggleHistory,
        onPreviousHistoryPage = viewModel::onPreviousHistoryPage,
        onNextHistoryPage = viewModel::onNextHistoryPage,
        onStartEditEntry = viewModel::onStartEditEntry,
        onEditMoodSelected = viewModel::onEditMoodSelected,
        onEditNoteChanged = viewModel::onEditNoteChanged,
        onToggleEditQuickTag = viewModel::onToggleEditQuickTag,
        onDismissEditEntry = viewModel::onDismissEditEntry,
        onConfirmEditEntry = viewModel::onConfirmEditEntry,
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
    modifier: Modifier = Modifier,
    onPreviousHistoryPage: () -> Unit = {},
    onNextHistoryPage: () -> Unit = {},
    onToggleQuickTag: (String) -> Unit = {},
    onStartEditEntry: (MoodEntry) -> Unit = {},
    onEditMoodSelected: (MoodType) -> Unit = {},
    onEditNoteChanged: (String) -> Unit = {},
    onToggleEditQuickTag: (String) -> Unit = {},
    onDismissEditEntry: () -> Unit = {},
    onConfirmEditEntry: () -> Unit = {}
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val latestEntry = if (state.isAuthenticated && state.loadError == null) state.latestTodayEntry else null

    if (state.isAuthenticated && state.loadError == null) {
        state.editingEntry?.let { editingEntry ->
            EditMoodEntryDialog(
                entry = editingEntry,
                selectedMood = state.editMood ?: editingEntry.mood,
                noteText = state.editNote,
                timeText = formatMoodTime(editingEntry.timestamp, timeFormatter),
                isSaving = state.isSaving,
                errorMessage = state.actionError,
                onMoodSelected = onEditMoodSelected,
                onNoteChanged = onEditNoteChanged,
                onToggleQuickTag = onToggleEditQuickTag,
                onDismiss = onDismissEditEntry,
                onConfirm = onConfirmEditEntry
            )
        }
    }

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
            // Card title & latest mood subtitle
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Suasana hati",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    color = MoodInk
                )
                AnimatedContent(
                    targetState = latestEntry?.mood,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 3 })
                            .togetherWith(fadeOut(tween(140)))
                    },
                    label = "latest_mood_title"
                ) { latestMood ->
                    Text(
                        text = if (latestMood == null) {
                            "Bagaimana suasana hatimu hari ini?"
                        } else {
                            "Mood terakhir: ${latestMood.label}"
                        },
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (latestMood == null) MoodInk else MoodSage
                    )
                }
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

            // Unified summary strip for check-in streak and today's entry count
            if (state.isAuthenticated && !state.isLoading && state.loadError == null && (state.streakDays > 0 || state.todayCount > 0)) {
                MoodSummaryStrip(
                    streakDays = state.streakDays,
                    hasCheckedInToday = state.hasCheckedInToday,
                    todayCount = state.todayCount
                )
            }

            // Body: unauthenticated / loading / load error / mood grid
            when {
                !state.isAuthenticated -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFAF6F2))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Masuk ke akun untuk mencatat dan menyinkronkan suasana hatimu.",
                            fontSize = 13.sp,
                            color = MoodMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }

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
                    // Optional short note toggle, field & quick context chips
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFFAF6F2))
                                .border(
                                    width = 1.dp,
                                    color = if (state.isNoteExpanded) MoodSage.copy(alpha = 0.45f) else Color(0xFFEDE3DA),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable(enabled = !state.isSaving, onClick = onToggleNoteInput)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.EditNote,
                                    contentDescription = null,
                                    tint = MoodAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (state.isNoteExpanded) {
                                        "Catatan singkat"
                                    } else {
                                        "Tambah catatan singkat"
                                    },
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MoodInk
                                )
                                if (state.noteInput.isNotBlank() && !state.isNoteExpanded) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MoodSageSoft)
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Terisi",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MoodSage
                                        )
                                    }
                                } else if (!state.isNoteExpanded) {
                                    Text(
                                        text = "Opsional",
                                        fontSize = 11.sp,
                                        color = MoodMuted
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (state.isNoteExpanded) {
                                    Icons.Filled.ExpandLess
                                } else {
                                    Icons.Filled.ExpandMore
                                },
                                contentDescription = if (state.isNoteExpanded) "Tutup catatan" else "Buka catatan",
                                tint = MoodMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = state.isNoteExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = state.noteInput,
                                    onValueChange = onNoteInputChanged,
                                    enabled = !state.isSaving,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            text = "Apa yang sedang kamu rasakan?",
                                            fontSize = 13.sp,
                                            color = MoodMuted
                                        )
                                    },
                                    supportingText = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Ketuk ekspresi di bawah untuk simpan",
                                                fontSize = 11.sp,
                                                color = MoodMuted
                                            )
                                            Text(
                                                text = "${state.noteInput.length}/${MoodTrackerLogic.MAX_NOTE_LENGTH}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MoodMuted
                                            )
                                        }
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
                                QuickContextTagsRow(
                                    noteText = state.noteInput,
                                    isDisabled = state.isSaving,
                                    onToggleTag = onToggleQuickTag
                                )
                            }
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
                    if (state.actionError != null && state.editingEntry == null) {
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
                                state.pagedTodayEntries.forEach { entry ->
                                    key(entry.id) {
                                        TodayMoodRow(
                                            entry = entry,
                                            timeText = formatMoodTime(entry.timestamp, timeFormatter),
                                            isDisabled = state.isSaving,
                                            onEdit = { onStartEditEntry(entry) },
                                            onDelete = { onDeleteEntry(entry.id) }
                                        )
                                    }
                                }
                                if (state.totalHistoryPages > 1) {
                                    HistoryPaginationBar(
                                        currentPage = state.currentHistoryPage,
                                        totalPages = state.totalHistoryPages,
                                        totalCount = state.todayCount,
                                        pageSize = QuickMoodUiState.HISTORY_PAGE_SIZE,
                                        canGoPrevious = state.canGoToPreviousHistoryPage,
                                        canGoNext = state.canGoToNextHistoryPage,
                                        onPrevious = onPreviousHistoryPage,
                                        onNext = onNextHistoryPage
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
private fun MoodSummaryStrip(
    streakDays: Int,
    hasCheckedInToday: Boolean,
    todayCount: Int
) {
    val showStreak = streakDays > 0
    val showTodayCount = todayCount > 0
    if (!showStreak && !showTodayCount) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFAF6F2))
            .border(1.dp, Color(0xFFEDE3DA), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showStreak) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    6.dp,
                    if (showTodayCount) Alignment.CenterHorizontally else Alignment.Start
                ),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (hasCheckedInToday) MoodStreakBg else MoodWarmSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (hasCheckedInToday) MoodStreakInk else MoodAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "$streakDays hari beruntun",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MoodInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!showTodayCount && !hasCheckedInToday) {
                Text(
                    text = "Catat hari ini untuk lanjut",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MoodAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (showStreak && showTodayCount) {
            VerticalDivider(
                modifier = Modifier
                    .height(18.dp)
                    .padding(horizontal = 8.dp),
                thickness = 1.dp,
                color = Color(0xFFE5D9CE)
            )
        }

        if (showTodayCount) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    6.dp,
                    if (showStreak) Alignment.CenterHorizontally else Alignment.Start
                ),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MoodSageSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MoodSage,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "$todayCount catatan hari ini",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MoodInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickContextTagsRow(
    noteText: String,
    isDisabled: Boolean,
    onToggleTag: (String) -> Unit,
    columns: Int = 3
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Konteks cepat",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MoodInk
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MoodTrackerLogic.quickContextTags.chunked(columns).forEach { rowTags ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowTags.forEach { tag ->
                        val isSelected = MoodTrackerLogic.hasContextTag(noteText, tag.label)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 34.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) MoodSageSoft else Color(0xFFFAF6F2))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MoodSage else Color(0xFFE7DDD4),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .alpha(if (isDisabled) 0.5f else 1f)
                                .clickable(enabled = !isDisabled) { onToggleTag(tag.label) }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tag.id.toIcon(),
                                contentDescription = null,
                                tint = if (isSelected) MoodSage else MoodMuted,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = tag.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) MoodSage else MoodInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    repeat(columns - rowTags.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun QuickContextTagId.toIcon(): ImageVector = when (this) {
    QuickContextTagId.KULIAH -> Icons.Outlined.School
    QuickContextTagId.TUGAS -> Icons.Outlined.Description
    QuickContextTagId.KURANG_TIDUR -> Icons.Outlined.Bedtime
    QuickContextTagId.TEMAN -> Icons.Outlined.Group
    QuickContextTagId.KELUARGA -> Icons.Outlined.Home
    QuickContextTagId.ISTIRAHAT -> Icons.Outlined.LocalCafe
}

@Composable
private fun MoodOptionItem(
    visual: MoodVisual,
    isSelected: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedMoodOptionItem(
        visual = visual,
        isSelected = isSelected,
        isDisabled = isDisabled,
        selectedColor = MoodSage,
        defaultLabelColor = MoodInk,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun TodayMoodRow(
    entry: MoodEntry,
    timeText: String,
    isDisabled: Boolean,
    onEdit: () -> Unit,
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
            onClick = onEdit,
            enabled = !isDisabled,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "Edit catatan ${entry.mood.label}",
                tint = MoodMuted,
                modifier = Modifier.size(18.dp)
            )
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

@Composable
private fun EditMoodEntryDialog(
    entry: MoodEntry,
    selectedMood: MoodType,
    noteText: String,
    timeText: String,
    isSaving: Boolean,
    errorMessage: String?,
    onMoodSelected: (MoodType) -> Unit,
    onNoteChanged: (String) -> Unit,
    onToggleQuickTag: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Edit catatan mood",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoodInk
                    )
                    Text(
                        text = "Tercatat pukul $timeText · ${entry.mood.label}",
                        fontSize = 12.sp,
                        color = MoodMuted
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pilih suasana hati",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MoodInk
                    )
                    MoodVisualCatalog.ordered.chunked(5).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowItems.forEach { visual ->
                                val isCurrent = visual.type == selectedMood
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isSaving) { onMoodSelected(visual.type) }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .then(
                                                if (isCurrent) {
                                                    Modifier
                                                        .background(
                                                            visual.accentColor.copy(alpha = 0.28f),
                                                            CircleShape
                                                        )
                                                        .border(2.dp, MoodSage, CircleShape)
                                                } else {
                                                    Modifier
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = visual.iconRes),
                                            contentDescription = visual.type.label,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = visual.type.label,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) MoodSage else MoodInk
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = onNoteChanged,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Catatan singkat", fontSize = 12.sp) },
                    placeholder = {
                        Text(
                            text = "Tambahkan atau ubah catatan...",
                            fontSize = 13.sp,
                            color = MoodMuted
                        )
                    },
                    supportingText = {
                        Text(
                            text = "${noteText.length}/${MoodTrackerLogic.MAX_NOTE_LENGTH}",
                            fontSize = 11.sp,
                            color = MoodMuted
                        )
                    },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MoodSage,
                        unfocusedBorderColor = Color(0xFFE0D6CE),
                        focusedTextColor = MoodInk,
                        unfocusedTextColor = MoodInk
                    )
                )

                QuickContextTagsRow(
                    noteText = noteText,
                    isDisabled = isSaving,
                    onToggleTag = onToggleQuickTag,
                    columns = 2
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = MoodErrorInk,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MoodErrorBg)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSaving
                    ) {
                        Text("Batal", color = MoodMuted, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MoodSage)
                    ) {
                        Text(if (isSaving) "Menyimpan..." else "Simpan")
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryPaginationBar(
    currentPage: Int,
    totalPages: Int,
    totalCount: Int,
    pageSize: Int,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val startNumber = currentPage * pageSize + 1
    val endNumber = ((currentPage + 1) * pageSize).coerceAtMost(totalCount)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Menampilkan $startNumber–$endNumber dari $totalCount",
            fontSize = 11.sp,
            color = MoodMuted
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PaginationStepButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Halaman sebelumnya",
                enabled = canGoPrevious,
                onClick = onPrevious
            )
            Text(
                text = "${currentPage + 1} / $totalPages",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MoodInk,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            PaginationStepButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Halaman berikutnya",
                enabled = canGoNext,
                onClick = onNext
            )
        }
    }
}

@Composable
private fun PaginationStepButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) MoodWarmSoft else Color(0xFFF5F0EC))
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) MoodAccent else MoodMuted,
            modifier = Modifier.size(18.dp)
        )
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
                streakDays = 4,
                hasCheckedInToday = true,
                isNoteExpanded = true,
                noteInput = "Kuliah",
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
