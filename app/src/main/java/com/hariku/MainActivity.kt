package com.hariku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hariku.core.ui.components.MainScaffold
import com.hariku.core.ui.components.Routes
import com.hariku.core.ui.components.SosTopBar
import com.google.firebase.auth.FirebaseAuth
import com.hariku.core.ui.theme.HariKuTheme
import com.hariku.feature.auth.presentation.login.LoginScreen
import com.hariku.feature.auth.presentation.register.RegisterScreen
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
            MainScaffold(
                parentNavController = navController,
                initialTabIndex = 0,
                homeContent = {
                    HomeScreen(
                        onOpenSos = { navController.navigate(Routes.SosGraph.route) },
                        onSignOut = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate(Routes.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
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
                onProfessional = { navController.navigate(Routes.SosProfessional.route) }
            )
        }
        composable(Routes.SosProfessional.route) {
            SosProfessionalScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun HomeScreen(
    onOpenSos: () -> Unit,
    onSignOut: () -> Unit
) {
    Scaffold(
        topBar = {
            SosTopBar(
                title = stringResource(R.string.app_name),
                onSosClick = onOpenSos
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextButton(onClick = onSignOut) {
                Text(text = "Keluar")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    HariKuTheme {
        HomeScreen(onOpenSos = {}, onSignOut = {})
    }
}
