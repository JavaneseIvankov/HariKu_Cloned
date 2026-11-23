package com.hariku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import com.hariku.feature.sos.SensesScreen
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
    val showSos = route == Routes.SosGraph.route ||
        route == Routes.SosProfessional.route ||
        route == Routes.Senses.route
    LaunchedEffect(showSos) { onSosVisibilityChanged(showSos) }
    NavHost(navController = navController, startDestination = Routes.Home.route) {
        composable(Routes.Home.route) {
            HomeScreen(onOpenSos = { navController.navigate(Routes.SosGraph.route) })
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

@Composable
private fun HomeScreen(onOpenSos: () -> Unit) {
    Scaffold(
        topBar = {
            SosTopBar(
                title = stringResource(R.string.app_name),
                onSosClick = onOpenSos
            )
        }
    ) { _ -> }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    HariKuTheme {
        HomeScreen(onOpenSos = {})
    }
}
