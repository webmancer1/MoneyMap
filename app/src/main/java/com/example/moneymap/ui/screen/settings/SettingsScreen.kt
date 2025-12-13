package com.example.moneymap.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymap.ui.viewmodel.AuthViewModel
import com.example.moneymap.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToManageAccount: () -> Unit,
    onNavigateToManageCategories: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val authUiState by authViewModel.uiState.collectAsState()
    val settingsUiState by settingsViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val currencyOptions = listOf(
        "KES", "USD", "EUR", "GBP", "UGX", "TZS", "RWF", "CAD", "AUD", "JPY",
        "CNY", "INR", "ZAR", "NGN", "GHS", "AED", "SAR", "CHF", "BRL", "MXN",
        "RUB", "SGD", "NZD"
    )

    LaunchedEffect(authUiState.isLoggedIn) {
        if (!authUiState.isLoggedIn) {
            onSignOut()
        }
    }

    LaunchedEffect(Unit) {
        settingsViewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileSection(
                displayName = authUiState.user?.displayName ?: "Anonymous",
                email = authUiState.user?.email ?: "Not available",
                onManageAccount = onNavigateToManageAccount
            )

            AppearanceSection(
                darkThemeEnabled = settingsUiState.preferences.darkTheme,
                onDarkThemeToggle = settingsViewModel::toggleDarkTheme
            )

            PreferencesSection(
                selectedCurrency = settingsUiState.preferences.currency,
                currencyOptions = currencyOptions,
                onCurrencySelected = settingsViewModel::updateCurrency,
                onNavigateToManageCategories = onNavigateToManageCategories
            )

            NotificationsSection(
                enabled = settingsUiState.preferences.notificationsEnabled,
                onToggle = settingsViewModel::toggleNotifications
            )

            SecuritySection(
                biometricEnabled = settingsUiState.preferences.biometricLockEnabled,
                onToggleBiometric = settingsViewModel::toggleBiometricLock,
                onSetupPin = { settingsViewModel.triggerPlaceholderMessage("PIN setup coming soon") }
            )

            DataAndSyncSection(
                onManualSync = {
                    coroutineScope.launch {
                        val result = settingsViewModel.triggerManualSync()
                        snackbarHostState.showSnackbar(
                            if (result is com.example.moneymap.data.sync.SyncResult.Success) {
                                "Sync completed successfully"
                            } else {
                                (result as? com.example.moneymap.data.sync.SyncResult.Error)?.message ?: "Sync failed"
                            }
                        )
                    }
                },
                onBackup = { settingsViewModel.triggerPlaceholderMessage("Backup feature coming soon") },
                onExport = { settingsViewModel.triggerPlaceholderMessage("Export feature coming soon") }
            )

            SignOutSection(onSignOut = authViewModel::signOut)
        }
    }
}

@Composable
private fun ProfileSection(
    displayName: String,
    email: String,
    onManageAccount: () -> Unit
) {
    SettingsCard(title = "Profile") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = 4.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        FilledTonalButton(onClick = onManageAccount, modifier = Modifier.fillMaxWidth()) {
            Text("Manage Account")
        }
    }
}

@Composable
private fun AppearanceSection(
    darkThemeEnabled: Boolean,
    onDarkThemeToggle: (Boolean) -> Unit
) {
    SettingsCard(title = "Appearance") {
        SettingsToggleRow(
            title = "Dark theme",
            description = "Enable dark mode for the app",
            checked = darkThemeEnabled,
            onCheckedChange = onDarkThemeToggle
        )
    }
}

@Composable
private fun PreferencesSection(
    selectedCurrency: String,
    currencyOptions: List<String>,
    onCurrencySelected: (String) -> Unit,
    onNavigateToManageCategories: () -> Unit
) {
    SettingsCard(title = "Preferences") {
        Text(text = "Currency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = selectedCurrency,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                label = { Text("Default currency") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                currencyOptions.forEach { currency ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = {
                            onCurrencySelected(currency)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationsSection(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    SettingsCard(title = "Notifications") {
        SettingsToggleRow(
            title = "Budget alerts",
            description = "Receive alerts when budgets hit thresholds",
            checked = enabled,
            onCheckedChange = onToggle
        )
        Divider(modifier = Modifier.padding(vertical = 12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Customize notification channels in system settings.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SecuritySection(
    biometricEnabled: Boolean,
    onToggleBiometric: (Boolean) -> Unit,
    onSetupPin: () -> Unit
) {
    SettingsCard(title = "Security") {
        SettingsToggleRow(
            title = "Biometric unlock",
            description = "Require fingerprint or face when launching the app",
            checked = biometricEnabled,
            onCheckedChange = onToggleBiometric
        )
        Divider(modifier = Modifier.padding(vertical = 12.dp))
        OutlinedButton(onClick = onSetupPin, modifier = Modifier.fillMaxWidth()) {
            Text("Set up PIN lock")
        }
    }
}

@Composable
private fun DataAndSyncSection(
    onManualSync: () -> Unit,
    onBackup: () -> Unit,
    onExport: () -> Unit
) {
    SettingsCard(title = "Data & Sync") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onManualSync, modifier = Modifier.fillMaxWidth()) {
                Text("Manual sync")
            }
            OutlinedButton(onClick = onBackup, modifier = Modifier.fillMaxWidth()) {
                Text("Backup data")
            }
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Text("Export transactions")
            }
        }
    }
}

@Composable
private fun SignOutSection(onSignOut: () -> Unit) {
    SettingsCard(title = "Account") {
        Text("Sign out of this device", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text("Sign Out")
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

