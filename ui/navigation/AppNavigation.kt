package com.example.hismaadm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.hismaadm.ui.admin.screen.*

@Composable
fun AdminNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AdminScreen.Login.route) {
        composable(AdminScreen.Login.route) {
            AdminLoginScreen(navController)
        }
        composable(AdminScreen.Dashboard.route) {
            AdminDashboardScreen(navController)
        }
        composable(AdminScreen.LubricentrosList.route) {
            AdminLubricentrosListScreen(navController)
        }
        composable(
            route = AdminScreen.LubricentroDetail.route,
            arguments = listOf(navArgument("lubricentroId") { type = NavType.StringType })
        ) { backStackEntry ->
            val lubricentroId = backStackEntry.arguments?.getString("lubricentroId") ?: ""
            AdminLubricentroDetailScreen(lubricentroId, navController)
        }
        composable(AdminScreen.SubscriptionPlans.route) {
            AdminSubscriptionPlansScreen(navController)
        }
    }
}