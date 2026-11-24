package com.hariku.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hariku.feature.chatbot.presentation.ChatbotScreen

/**
 * Shell for the four main tabs, mirroring the upstream HariKu structure:
 * the bottom navigation drives an inner NavHost while feature screens receive
 * the parent controller so they can push full-screen destinations on top.
 *
 * Journal uses a placeholder until its screen is available.
 */
@Composable
fun MainScaffold(
    parentNavController: NavHostController,
    initialTabIndex: Int = 0,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    homeContent: @Composable () -> Unit,
    statisticContent: @Composable () -> Unit
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavRoutes = listOf(
        Routes.Home.route,
        Routes.Chatbot.route,
        Routes.Journal.route,
        Routes.Statistic.route
    )

    // Set initial route based on initialTabIndex
    val startDestination = bottomNavRoutes.getOrNull(initialTabIndex) ?: Routes.Home.route

    val selectedIndex = bottomNavRoutes.indexOfFirst { it == currentRoute }
        .takeIf { it >= 0 } ?: initialTabIndex

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNavBar(
                selectedIndex = selectedIndex,
                onItemSelected = { index ->
                    if (index in bottomNavRoutes.indices) {
                        val route = bottomNavRoutes[index]
                        if (route != currentRoute) {
                            bottomNavController.navigate(route) {
                                popUpTo(Routes.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        ) {
            composable(Routes.Home.route) {
                homeContent()
            }
            composable(Routes.Chatbot.route) {
                ChatbotScreen(navController = parentNavController)
            }
            composable(Routes.Journal.route) {
                PlaceholderTab(
                    title = "Jurnal",
                    message = "Fitur jurnal belum tersedia."
                )
            }
            composable(Routes.Statistic.route) {
                statisticContent()
            }
        }
    }
}

@Composable
private fun PlaceholderTab(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title)
        Text(text = message)
    }
}
