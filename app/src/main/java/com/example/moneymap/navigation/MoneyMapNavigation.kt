package com.example.moneymap.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.moneymap.ui.screen.auth.BiometricLockScreen
import com.example.moneymap.ui.screen.auth.LoginScreen
import com.example.moneymap.ui.screen.auth.RegisterScreen
import com.example.moneymap.ui.screen.home.HomeScreen
import com.example.moneymap.ui.screen.transaction.AddTransactionScreen
import com.example.moneymap.ui.screen.transaction.TransactionListScreen
import com.example.moneymap.ui.screen.reports.ReportsScreen
import com.example.moneymap.ui.screen.budget.BudgetScreen
import com.example.moneymap.ui.screen.settings.SettingsScreen

@Composable
fun MoneyMapNavigation(
    navController: NavHostController,
    startDestination: String = NavRoutes.LOGIN
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(NavRoutes.REGISTER)
                }
            )
        }
        composable(NavRoutes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.REGISTER) { inclusive = true }
                    }
                }
            )
        }
        composable(NavRoutes.BIOMETRIC_LOCK) {
            BiometricLockScreen(
                onAuthenticationSuccess = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.BIOMETRIC_LOCK) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.BIOMETRIC_LOCK) { inclusive = true }
                    }
                }
            )
        }
        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToTransactions = {
                    navController.navigate(NavRoutes.TRANSACTIONS)
                },
                onNavigateToAddTransaction = {
                    navController.navigate(NavRoutes.ADD_TRANSACTION)
                },
                onNavigateToReports = {
                    navController.navigate(NavRoutes.REPORTS)
                },
                onNavigateToBudgets = {
                    navController.navigate(NavRoutes.BUDGETS)
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                }
            )
        }
        composable(NavRoutes.TRANSACTIONS) {
            TransactionListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddTransaction = {
                    navController.navigate(NavRoutes.ADD_TRANSACTION)
                },
                onNavigateToEditTransaction = { transactionId ->
                    navController.navigate("edit_transaction/$transactionId")
                }
            )
        }
        composable(NavRoutes.ADD_TRANSACTION) {
            AddTransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onTransactionSaved = { navController.popBackStack() }
            )
        }
        composable("edit_transaction/{transactionId}") { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
            AddTransactionScreen(
                transactionId = transactionId,
                onNavigateBack = { navController.popBackStack() },
                onTransactionSaved = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.REPORTS) {
            ReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.BUDGETS) {
            BudgetScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignOut = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

