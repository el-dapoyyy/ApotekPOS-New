package com.mediakasir.apotekpos.ui

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object POS : Screen("pos")
    object Stok : Screen("stok")
    object History : Screen("history")
    object Laporan : Screen("laporan")
    object Settings : Screen("settings")
    object Prescriptions : Screen("prescriptions")
}
