package com.example.moneymap.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.moneymap.ui.screen.auth.BiometricLockScreen
import com.example.moneymap.ui.screen.auth.LoginScreen
import com.example.moneymap.ui.screen.auth.RegisterScreen
import com.example.moneymap.ui.screen.home.HomeScreen
import com.example.moneymap.ui.screen.transaction.AddTransactionScreen
import com.example.moneymap.ui.screen.transaction.TransactionListScreen
import com.example.moneymap.ui.screen.reports.ReportsScreen
import com.example.moneymap.ui.screen.budget.BudgetScreen
import com.example.moneymap.ui.screen.debt.DebtListScreen
import com.example.moneymap.ui.screen.debt.AddEditDebtScreen
import com.example.moneymap.ui.screen.insights.InsightsScreen
import com.example.moneymap.ui.screen.savings.SavingsScreen
import com.example.moneymap.ui.screen.settings.SettingsScreen

@Composable
fun MoneyMapNavigation(
    navController: NavHostController,
    startDestination: String = NavRoutes.LOGIN
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val bottomNavItems = listOf(
        BottomNavItem(
            route = NavRoutes.HOME,
            label = "Home",
            icon = Icons.Default.Home
        ),
        BottomNavItem(
            route = NavRoutes.TRANSACTIONS,
            label = "Transactions",
            icon = Icons.Default.List
        ),
        BottomNavItem(
            route = NavRoutes.REPORTS,
            label = "Reports",
            icon = Icons.Default.Info
        ),
        BottomNavItem(
            route = NavRoutes.BUDGETS,
            label = "Budgets",
            icon = Icons.Default.AccountCircle
        )
    )
    val showBottomBar = currentDestination.isBottomBarDestination(bottomNavItems)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                MoneyMapBottomBar(
                    navController = navController,
                    currentDestination = currentDestination,
                    items = bottomNavItems
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
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
                    onNavigateToDebts = {
                        navController.navigate(NavRoutes.DEBTS)
                    },
                    onNavigateToSavings = {
                        navController.navigate(NavRoutes.SAVINGS)
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
            composable(NavRoutes.DEBTS) {
                DebtListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddDebt = {
                        navController.navigate(NavRoutes.ADD_DEBT)
                    },
                    onNavigateToEditDebt = { debtId ->
                        navController.navigate("edit_debt/$debtId")
                    }
                )
            }
            composable(NavRoutes.ADD_DEBT) {
                AddEditDebtScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onDebtSaved = { navController.popBackStack() }
                )
            }
            composable("edit_debt/{debtId}") { backStackEntry ->
                val debtId = backStackEntry.arguments?.getString("debtId") ?: ""
                AddEditDebtScreen(
                    debtId = debtId,
                    onNavigateBack = { navController.popBackStack() },
                    onDebtSaved = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.INSIGHTS) {
                InsightsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.SAVINGS) {
                SavingsScreen(
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
}

private fun NavDestination?.isBottomBarDestination(items: List<BottomNavItem>): Boolean {
    return this?.hierarchy?.any { destination ->
        items.any { it.route == destination.route }
    } == true && this?.route != NavRoutes.SETTINGS
}

@Composable
private fun MoneyMapBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    items: List<BottomNavItem>
) {
    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination
                ?.hierarchy
                ?.any { it.route == item.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = {
                    Text(
                        text = item.label,
                        maxLines = 1
                    )
                }
            )
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

