package com.mediakasir.apotekpos.ui

sealed class Screen(val route: String) {
    object License : Screen("license")
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object POS : Screen("pos")
    object Stok : Screen("stok")
    object History : Screen("history")
    object Settings : Screen("settings")
}
