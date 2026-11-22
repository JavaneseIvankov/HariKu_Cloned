package com.hariku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hariku.core.ui.components.Routes
import com.hariku.core.ui.components.SosTopBar
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.chatbot.presentation.ChatbotScreen
import com.hariku.feature.chatbot.presentation.customize.CustomizeCatScreen
import com.hariku.feature.chatbot.presentation.customize.CustomizeNewCatScreen
import com.hariku.feature.chatbot.presentation.customize.CustomizePersonalCatScreen
import com.hariku.feature.chatbot.presentation.detail.ChatbotDetailScreen
import com.hariku.feature.sos.SosScreen
import com.hariku.feature.sos.SosProfessionalScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HariKuTheme {
                HariKuNavigation(onSosVisibilityChanged = ::updateSystemBars)
            }
        }
    }

    private fun updateSystemBars(showSos: Boolean) {
        if (showSos) {
            val transparent = android.graphics.Color.TRANSPARENT
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.light(transparent, transparent),
                navigationBarStyle = SystemBarStyle.light(transparent, transparent)
            )
        } else {
            enableEdgeToEdge()
        }
    }
}

@Composable
private fun HariKuNavigation(onSosVisibilityChanged: (Boolean) -> Unit) {
    val navController = rememberNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val showSos = route == Routes.SosGraph.route || route == Routes.SosProfessional.route
    LaunchedEffect(showSos) { onSosVisibilityChanged(showSos) }
    NavHost(navController = navController, startDestination = Routes.Home.route) {
        composable(Routes.Home.route) {
            HomeScreen(
                onOpenSos = { navController.navigate(Routes.SosGraph.route) },
                onOpenChat = { navController.navigate(Routes.Chatbot.route) }
            )
        }
        composable(Routes.Chatbot.route) { ChatbotScreen(navController) }
        composable("detail_chatbot/{chatbotId}") { backStackEntry ->
            val chatbotId = backStackEntry.arguments?.getString("chatbotId") ?: ""
            ChatbotDetailScreen(navController, chatbotId)
        }
        composable(Routes.CustomizeCat.route) { CustomizeCatScreen(navController) }
        composable(Routes.CustomizeNewCat.route) { CustomizeNewCatScreen(navController) }
        composable("customize_personal_cat/{name}/{avatarResId}") { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val avatarResId = backStackEntry.arguments?.getString("avatarResId")?.toIntOrNull() ?: 0
            CustomizePersonalCatScreen(navController, name, avatarResId)
        }
        composable(Routes.SosGraph.route) {
            SosScreen(
                onBack = { navController.popBackStack() },
                onProfessional = { navController.navigate(Routes.SosProfessional.route) }
            )
        }
        composable(Routes.SosProfessional.route) {
            SosProfessionalScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun HomeScreen(onOpenSos: () -> Unit, onOpenChat: () -> Unit) {
    Scaffold(
        topBar = {
            SosTopBar(
                title = stringResource(R.string.app_name),
                onSosClick = onOpenSos
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = onOpenChat) {
                Text(text = "Buka Chat")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    HariKuTheme {
        HomeScreen(onOpenSos = {}, onOpenChat = {})
    }
}
