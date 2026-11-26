package com.hariku.feature.journal.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hariku.R
import com.hariku.core.ui.components.FloatingActButton
import com.hariku.core.ui.components.Routes
import com.hariku.core.ui.components.SosTopBar
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.journal.domain.model.CbtTemplateType
import com.hariku.feature.journal.domain.model.JournalCategoryTag
import com.hariku.feature.journal.domain.model.JournalEntry
import com.hariku.feature.journal.presentation.components.AdaptivePromptBannerCard
import com.hariku.feature.journal.presentation.components.JournalCategoryChipRow
import com.hariku.feature.journal.presentation.components.JournalEntryCard
import com.hariku.feature.journal.presentation.components.TemplateSelectorSheet
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun JournalScreen(
    navController: NavController,
    viewModel: JournalViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is JournalEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    JournalScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onSosClick = { navController.navigate(Routes.SosGraph.route) },
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onSelectCategory = viewModel::onSelectCategory,
        onClearFilters = viewModel::onClearFilters,
        onCyclePrompt = viewModel::onCyclePromptVariant,
        onOpenTemplateSheet = viewModel::onShowTemplateSheet,
        onDismissTemplateSheet = viewModel::onDismissTemplateSheet,
        onStartGuidedJournal = { templateOverride ->
            val route = if (templateOverride != null) {
                "${Routes.CreateNotePrompt.route}?template=${templateOverride.name}"
            } else {
                Routes.CreateNotePrompt.route
            }
            navController.navigate(route)
        },
        onStartFreeWrite = {
            navController.navigate("${Routes.CreateNotePrompt.route}?skipToEditor=true")
        },
        onEntryClick = { entryId ->
            navController.navigate(Routes.JournalDetail.createRoute(entryId.toString()))
        },
        onTogglePin = viewModel::onTogglePin,
        onRequestDelete = viewModel::onRequestDelete,
        onConfirmDelete = viewModel::onConfirmDelete,
        onDismissDeleteDialog = viewModel::onDismissDeleteDialog
    )
}

@Composable
fun JournalScreenContent(
    state: JournalUiState,
    snackbarHostState: SnackbarHostState,
    onSosClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (JournalCategoryTag?) -> Unit,
    onClearFilters: () -> Unit,
    onCyclePrompt: () -> Unit,
    onOpenTemplateSheet: () -> Unit,
    onDismissTemplateSheet: () -> Unit,
    onStartGuidedJournal: (CbtTemplateType?) -> Unit,
    onStartFreeWrite: () -> Unit,
    onEntryClick: (Long) -> Unit,
    onTogglePin: (Long) -> Unit,
    onRequestDelete: (JournalEntry) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDeleteDialog: () -> Unit
) {
    if (state.isTemplateSheetVisible) {
        TemplateSelectorSheet(
            recommendedTemplate = state.recommendation.template,
            selectedTemplate = state.recommendation.template,
            onSelectTemplate = { chosen ->
                onDismissTemplateSheet()
                onStartGuidedJournal(chosen)
            },
            onDismiss = onDismissTemplateSheet
        )
    }

    state.pendingDeleteEntry?.let { entry ->
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
                    text = "Apakah kamu yakin ingin menghapus jurnal \"${entry.title}\"? Tindakan ini tidak dapat dibatalkan.",
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

    Scaffold(
        containerColor = JournalDesignTokens.CanvasBackground,
        topBar = {
            SosTopBar(
                title = "Jurnal",
                onSosClick = onSosClick
            )
        },
        floatingActionButton = {
            FloatingActButton(
                label = "Tambah Jurnal",
                onClick = { onStartGuidedJournal(null) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateLeftPadding(LayoutDirection.Ltr),
                    end = paddingValues.calculateRightPadding(LayoutDirection.Ltr)
                )
                .background(JournalDesignTokens.CanvasBackground)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 14.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Search Bar ("Cari Jurnal atau Halaman")
                item(key = "search_bar", contentType = "search_bar") {
                    JournalSearchBar(
                        query = state.searchQuery,
                        onQueryChange = onSearchQueryChange,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                // Filter chip kategori
                item(key = "category_chips", contentType = "category_chips") {
                    JournalCategoryChipRow(
                        selectedCategory = state.selectedCategory,
                        onSelectCategory = onSelectCategory
                    )
                }

                // Banner Adaptive CBT Prompt
                item(key = "adaptive_banner", contentType = "adaptive_banner") {
                    AdaptivePromptBannerCard(
                        recommendation = state.recommendation,
                        onStartGuidedReflection = { onStartGuidedJournal(null) },
                        onStartFreeWrite = onStartFreeWrite,
                        onCyclePrompt = onCyclePrompt,
                        onOpenTemplateSheet = onOpenTemplateSheet,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                when {
                    state.isLoading -> {
                        item(key = "loading", contentType = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = JournalDesignTokens.PrimaryCta)
                            }
                        }
                    }

                    state.filteredEntries.isEmpty() -> {
                        item(key = "empty_state", contentType = "empty_state") {
                            JournalEmptyState(
                                hasActiveFilter = state.hasActiveFilter,
                                onClearFilters = onClearFilters,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                            )
                        }
                    }

                    else -> {
                        if (state.pinnedEntries.isNotEmpty()) {
                            item(key = "pinned_header", contentType = "section_header") {
                                SectionHeader(
                                    title = "Disematkan",
                                    count = state.pinnedEntries.size,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                            items(
                                items = state.pinnedEntries,
                                key = { it.id },
                                contentType = { "journal_entry" }
                            ) { entry ->
                                JournalEntryCard(
                                    entry = entry,
                                    onClick = { onEntryClick(entry.id) },
                                    onTogglePin = { onTogglePin(entry.id) },
                                    onDeleteClick = { onRequestDelete(entry) },
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }

                        if (state.recentEntries.isNotEmpty()) {
                            item(key = "recent_header", contentType = "section_header") {
                                SectionHeader(
                                    title = if (state.pinnedEntries.isNotEmpty()) {
                                        "Jurnal Terbaru"
                                    } else {
                                        "Semua Jurnal"
                                    },
                                    count = state.recentEntries.size,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                            items(
                                items = state.recentEntries,
                                key = { it.id },
                                contentType = { "journal_entry" }
                            ) { entry ->
                                JournalEntryCard(
                                    entry = entry,
                                    onClick = { onEntryClick(entry.id) },
                                    onTogglePin = { onTogglePin(entry.id) },
                                    onDeleteClick = { onRequestDelete(entry) },
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        placeholder = {
            Text(
                text = "Cari Jurnal atau Halaman",
                fontSize = 14.sp,
                color = JournalDesignTokens.TextMuted
            )
        },
        leadingIcon = {
            Image(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = "Cari",
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Bersihkan pencarian",
                        tint = JournalDesignTokens.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = JournalDesignTokens.CardWhite,
            unfocusedContainerColor = JournalDesignTokens.CardWhite,
            focusedBorderColor = JournalDesignTokens.PrimaryCta,
            unfocusedBorderColor = Color(0xFFE5DFDC),
            focusedTextColor = JournalDesignTokens.TextPrimary,
            unfocusedTextColor = JournalDesignTokens.TextPrimary,
            cursorColor = JournalDesignTokens.PrimaryCta
        )
    )
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = JournalDesignTokens.TextPrimary
        )
        Text(
            text = "$count catatan",
            fontSize = 12.sp,
            color = JournalDesignTokens.TextMuted
        )
    }
}

@Composable
private fun JournalEmptyState(
    hasActiveFilter: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(JournalDesignTokens.CardWhite, RoundedCornerShape(20.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_cat_handing_book),
            contentDescription = null,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (hasActiveFilter) {
                "Tidak ada jurnal yang cocok"
            } else {
                "Belum ada catatan jurnal"
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = JournalDesignTokens.TextPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (hasActiveFilter) {
                "Coba ubah kata kunci pencarian atau pilih kategori lain."
            } else {
                "Mulai refleksi pertamamu hari ini dengan panduan CBT adaptif di atas atau ketuk tombol + di kanan bawah."
            },
            fontSize = 13.sp,
            color = JournalDesignTokens.TextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
        if (hasActiveFilter) {
            TextButton(onClick = onClearFilters) {
                Text(
                    text = "Reset Filter",
                    color = JournalDesignTokens.PrimaryCta,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JournalScreenPreview() {
    HariKuTheme {
        JournalScreenContent(
            state = JournalUiState(
                isLoading = false,
                filteredEntries = listOf(
                    JournalEntry(
                        id = 1L,
                        title = "Menghadapi tenggat laporan praktikum",
                        content = "Hari ini sempat cemas karena deadline menumpuk, tapi setelah membuat daftar prioritas kecil rasanya jauh lebih terkendali.",
                        dateMillis = System.currentTimeMillis(),
                        tag = JournalCategoryTag.AKADEMIK,
                        templateType = CbtTemplateType.COGNITIVE_REFRAMING,
                        isPinned = true
                    )
                ),
                pinnedEntries = listOf(
                    JournalEntry(
                        id = 1L,
                        title = "Menghadapi tenggat laporan praktikum",
                        content = "Hari ini sempat cemas karena deadline menumpuk, tapi setelah membuat daftar prioritas kecil rasanya jauh lebih terkendali.",
                        dateMillis = System.currentTimeMillis(),
                        tag = JournalCategoryTag.AKADEMIK,
                        templateType = CbtTemplateType.COGNITIVE_REFRAMING,
                        isPinned = true
                    )
                )
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onSosClick = {},
            onSearchQueryChange = {},
            onSelectCategory = {},
            onClearFilters = {},
            onCyclePrompt = {},
            onOpenTemplateSheet = {},
            onDismissTemplateSheet = {},
            onStartGuidedJournal = {},
            onStartFreeWrite = {},
            onEntryClick = {},
            onTogglePin = {},
            onRequestDelete = {},
            onConfirmDelete = {},
            onDismissDeleteDialog = {}
        )
    }
}
