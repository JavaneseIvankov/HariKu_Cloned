package com.hariku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hariku.core.ui.components.Routes
import com.google.firebase.auth.FirebaseAuth
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.sos.SensesScreen
import com.hariku.feature.auth.presentation.login.LoginScreen
import com.hariku.feature.auth.presentation.register.RegisterScreen
import com.hariku.feature.chatbot.presentation.customize.CustomizeCatScreen
import com.hariku.feature.chatbot.presentation.customize.CustomizeNewCatScreen
import com.hariku.feature.chatbot.presentation.customize.CustomizePersonalCatScreen
import com.hariku.feature.chatbot.presentation.detail.ChatbotDetailScreen
import com.hariku.feature.sos.SosScreen
import com.hariku.feature.sos.SosProfessionalScreen
import com.hariku.feature.profile.presentation.ProfileScreen
import com.hariku.feature.mood.MoodMainTabs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HariKuTheme {
                HariKuNavigation(application as HariKuApp, onSosVisibilityChanged = ::updateSystemBars)
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
private fun HariKuNavigation(app: HariKuApp, onSosVisibilityChanged: (Boolean) -> Unit) {
    val navController = rememberNavController()
    val route = navController.currentBackStackEntryAsState().value?.destination?.route
    val showSos = route == Routes.SosGraph.route ||
        route == Routes.SosProfessional.route ||
        route == Routes.Senses.route
    LaunchedEffect(showSos) { onSosVisibilityChanged(showSos) }
    // With no cached session the app opens on the auth screen; a restored
    // session goes straight to the home destination.
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Routes.Home.route
    } else {
        Routes.Login.route
    }
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.Login.route) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.Register.route) }
            )
        }
        composable(Routes.Register.route) {
            RegisterScreen(
                onRegistered = {
                    navController.navigate(Routes.Home.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Routes.Home.route) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@composable
            key(userId) {
                val moodDao = remember(app, userId) { app.moodDao(userId) }
                MoodMainTabs(navController, moodDao)
            }
        }
        composable(Routes.Profile.route) {
            ProfileScreen(
                navController = navController,
                onLogout = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
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
                onSenses = { navController.navigate(Routes.Senses.route) },
                onProfessional = { navController.navigate(Routes.SosProfessional.route) }
            )
        }
        composable(Routes.Senses.route) {
            SensesScreen(onExit = { navController.popBackStack() })
        }
        composable(Routes.SosProfessional.route) {
            SosProfessionalScreen(onBack = { navController.popBackStack() })
        }
    }
}

