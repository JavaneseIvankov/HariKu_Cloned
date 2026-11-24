package com.hariku.feature.mood

import android.database.sqlite.SQLiteException
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hariku.core.ui.components.MainScaffold
import com.hariku.core.ui.components.Routes
import com.hariku.core.ui.components.SosTopBar
import com.hariku.feature.home.presentation.HomeScreen
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun MoodMainTabs(navController: NavHostController, moodDao: MoodDao) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val entriesFlow = remember(moodDao, retry) {
        moodDao.observeEntries()
            .map { Result.success(it) }
            .catch { error ->
                if (error !is SQLiteException) throw error
                emit(Result.failure(error))
            }
    }
    val entries by entriesFlow.collectAsState(initial = null)

    MainScaffold(
        parentNavController = navController,
        snackbarHostState = snackbar,
        homeContent = {
            HomeScreen(
                navController = navController,
                onOpenProfile = { navController.navigate(Routes.Profile.route) },
                onOpenSos = { navController.navigate(Routes.SosGraph.route) },
                moodContent = {
                    MoodDataContent(entries, onRetry = { retry++ }) { records ->
                        MoodEntryScreen(records, saving, onSave = { mood ->
                            if (!saving) {
                                saving = true
                                scope.launch {
                                    val message = try {
                                        moodDao.insert(MoodEntry(moodType = mood.label))
                                        "Mood tersimpan"
                                    } catch (_: SQLiteException) {
                                        "Mood gagal disimpan. Coba lagi."
                                    } finally {
                                        saving = false
                                    }
                                    snackbar.showSnackbar(message)
                                }
                            }
                        })
                    }
                }
            )
        },
        statisticContent = {
            Column(Modifier.fillMaxSize().background(Color(0xFFF9F9F9))) {
                SosTopBar(title = "Statistik", onSosClick = {
                    navController.navigate(Routes.SosGraph.route)
                })
                MoodDataContent(entries, onRetry = { retry++ }) { records ->
                    MoodReportScreen(records)
                }
            }
        }
    )
}

@Composable
private fun MoodDataContent(
    entries: Result<List<MoodEntry>>?,
    onRetry: () -> Unit,
    content: @Composable (List<MoodEntry>?) -> Unit
) {
    if (entries?.isFailure == true) {
        Column(Modifier.padding(20.dp)) {
            Text("Catatan mood gagal dimuat.", color = Color.Black)
            Button(onClick = onRetry) { Text("Coba lagi") }
        }
    } else {
        content(entries?.getOrNull())
    }
}
