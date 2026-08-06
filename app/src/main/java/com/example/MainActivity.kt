package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.presentation.components.FrostedBottomNavBar
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.diagnostics.DiagnosticsScreen
import com.example.presentation.servers.ServerListScreen
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.viewmodel.VpnViewModel
import com.example.ui.theme.KmthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KmthTheme {
                KmthVpnApp()
            }
        }
    }
}

@Composable
fun KmthVpnApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val viewModel: VpnViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    Scaffold(
        containerColor = Color(0xFF05060A),
        bottomBar = {
            FrostedBottomNavBar(
                currentRoute = currentRoute,
                onTabSelected = { tab ->
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo("dashboard") {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToServers = { navController.navigate("servers") },
                    onNavigateToDiagnostics = { navController.navigate("diagnostics") },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("servers") {
                ServerListScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("diagnostics") {
                DiagnosticsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

