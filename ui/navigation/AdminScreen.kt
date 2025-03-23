package com.example.hismaadm.ui.navigation

// Rutas para la aplicación administrativa
sealed class AdminScreen(val route: String) {
    object Login : AdminScreen("admin_login_screen")
    object Dashboard : AdminScreen("admin_dashboard_screen")
    object LubricentrosList : AdminScreen("admin_lubricentros_list_screen")
    object LubricentroDetail : AdminScreen("admin_lubricentro_detail_screen/{lubricentroId}") {
        fun createRoute(lubricentroId: String) = "admin_lubricentro_detail_screen/$lubricentroId"
    }
    object SubscriptionPlans : AdminScreen("admin_subscription_plans_screen")
    object SubscriptionRequests : AdminScreen("subscription_requests_screen")
}