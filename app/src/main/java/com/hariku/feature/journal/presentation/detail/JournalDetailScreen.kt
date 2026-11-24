package com.hariku.feature.journal.presentation.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hariku.core.ui.components.CustomizeTopBar
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.journal.domain.model.CbtTemplateType
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.journal.presentation.JournalDesignTokens
import com.hariku.feature.journal.presentation.components.JournalCategoryCatalog
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun JournalDetailScreen(
    journalId: Long,
    onNavigateBack: () -> Unit,
    onEditJournal: (Long) -> Unit,
    viewModel: JournalDetailViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadJournal(journalId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                JournalDetailEvent.DeletedAndNavigateBack -> currentOnNavigateBack()
                is JournalDetailEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    JournalDetailContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBackClick = onNavigateBack,
        onTogglePin = viewModel::onTogglePin,
        onEditClick = { state.entry?.let { onEditJournal(it.id) } },
        onRequestDelete = viewModel::onRequestDelete,
        onConfirmDelete = viewModel::onConfirmDelete,
        onDismissDeleteDialog = viewModel::onDismissDeleteDialog
    )
}

@Composable
fun JournalDetailContent(
    state: JournalDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onTogglePin: () -> Unit,
    onEditClick: () -> Unit,
    onRequestDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteDialog: () -> Unit
) {
    if (state.isDeleteDialogVisible) {
        AlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            containerColor = JournalDesignTokens.CardWhite,
            title = {
                Text(
                    text = "Hapus Jurnal?",
                    fontWeight = FontWeight.Bold,
                    color = JournalDesignTokens.TextPrimary
                )
            },
            text = {
                Text(
                    text = "Apakah kamu yakin ingin menghapus jurnal ini secara permanen? Tindakan ini tidak dapat dibatalkan.",
                    color = JournalDesignTokens.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = JournalDesignTokens.ErrorInk)
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteDialog) {
                    Text("Batal", color = JournalDesignTokens.TextSecondary)
                }
            }
        )
    }

    val entry = state.entry

    Scaffold(
        containerColor = JournalDesignTokens.CanvasBackground,
        topBar = {
            CustomizeTopBar(
                title = "Detail Jurnal",
                onBackClick = onBackClick,
                isActions = {
                    if (entry != null) {
                        IconButton(onClick = onTogglePin, enabled = !state.isWorking) {
                            Icon(
                                imageVector = if (entry.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (entry.isPinned) "Lepas sematan" else "Sematkan",
                                tint = if (entry.isPinned) {
                                    JournalDesignTokens.PrimaryCta
                                } else {
                                    JournalDesignTokens.TextPrimary
                                }
                            )
                        }
                        IconButton(onClick = onEditClick, enabled = !state.isWorking) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit jurnal",
                                tint = JournalDesignTokens.TextPrimary
                            )
                        }
                        IconButton(onClick = onRequestDelete, enabled = !state.isWorking) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Hapus jurnal",
                                tint = JournalDesignTokens.ErrorInk
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(JournalDesignTokens.CanvasBackground)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        color = JournalDesignTokens.PrimaryCta,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                entry == null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.errorMessage ?: "Jurnal tidak ditemukan.",
                            fontSize = 15.sp,
                            color = JournalDesignTokens.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = onBackClick,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JournalDesignTokens.PrimaryCta
                            )
                        ) {
                            Text("Kembali")
                        }
                    }
                }

                else -> {
                    val dateFormatter = remember {
                        DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy · HH:mm", Locale.forLanguageTag("id-ID"))
                    }
                    val formattedDate = remember(entry.dateMillis) {
                        Instant.ofEpochMilli(entry.dateMillis)
                            .atZone(ZoneId.systemDefault())
                            .format(dateFormatter)
                    }
                    val categoryVisual = JournalCategoryCatalog.of(entry.tag)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Banner kategori visual (memanfaatkan ic_kategori_*.png)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(106.dp)
                                .clip(RoundedCornerShape(20.dp))
                        ) {
                            Image(
                                painter = painterResource(id = categoryVisual.bannerRes),
                                contentDescription = entry.tag.label,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = JournalDesignTokens.CardWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(JournalDesignTokens.PeachHighlight)
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = entry.tag.label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = JournalDesignTokens.TextPrimary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFEDE9E7))
                                            .padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = JournalDesignTokens.PrimaryCta,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = entry.templateType.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = JournalDesignTokens.TextSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = entry.title,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = JournalDesignTokens.TextPrimary,
                                    lineHeight = 28.sp
                                )

                                Text(
                                    text = formattedDate,
                                    fontSize = 12.sp,
                                    color = JournalDesignTokens.TextMuted
                                )

                                if (entry.promptUsed.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(JournalDesignTokens.PromptItemSurface)
                                            .border(
                                                width = 1.dp,
                                                color = JournalDesignTokens.PromptItemBorder,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .padding(14.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Prompt Pemantik",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = JournalDesignTokens.PrimaryCta
                                            )
                                            Text(
                                                text = "\"${entry.promptUsed}\"",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = JournalDesignTokens.TextPrimary,
                                                lineHeight = 19.sp
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFEDE8E4))

                                Text(
                                    text = entry.content,
                                    fontSize = 15.sp,
                                    color = JournalDesignTokens.TextPrimary,
                                    lineHeight = 24.sp
                                )
                            }
                        }

                        Button(
                            onClick = onEditClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JournalDesignTokens.PrimaryCta,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(vertical = 13.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Jurnal", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JournalDetailPreview() {
    HariKuTheme {
        JournalDetailContent(
            state = JournalDetailUiState(
                isLoading = false,
                entry = JournalEntry(
                    id = 1L,
                    title = "Menyusun langkah kecil untuk skripsi & praktikum",
                    content = "[Perasaan awal: Cemas, Ingin Fokus]\n\n• Pikiran yang muncul:\nTakut tidak sempat menyelesaikan semua modul tepat waktu.\n\n• Bukti nyata:\nSebagian besar domain & data layer sudah selesai dan teruji.\n\n• Sudut pandang lebih seimbang:\nFokus menyelesaikan satu bagian demi satu bagian jauh lebih efektif.",
                    dateMillis = System.currentTimeMillis(),
                    tag = JournalCategoryTag.KECEMASAN,
                    templateType = CbtTemplateType.COGNITIVE_REFRAMING,
                    promptUsed = "Terkait topik akademik yang terakhir kamu catat, pikiran apa yang paling membebanimu saat ini?",
                    isPinned = true
                )
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBackClick = {},
            onTogglePin = {},
            onEditClick = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onDismissDeleteDialog = {}
        )
    }
}
