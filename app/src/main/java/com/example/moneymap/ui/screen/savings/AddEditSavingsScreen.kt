package com.example.moneymap.ui.screen.savings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymap.data.model.Goal
import com.example.moneymap.ui.viewmodel.SavingsViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSavingsScreen(
    onNavigateBack: () -> Unit,
    onGoalSaved: () -> Unit,
    goalId: String? = null,
    viewModel: SavingsViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var savedAmount by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    LaunchedEffect(goalId) {
        if (goalId != null) {
            isLoading = true
            val goal = viewModel.getGoalById(goalId)
            if (goal != null) {
                name = goal.name
                targetAmount = goal.targetAmount.toString()
                savedAmount = goal.savedAmount.toString()
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (goalId == null) "Add Savings Goal" else "Edit Savings Goal") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Goal Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = targetAmount,
                onValueChange = { targetAmount = it },
                label = { Text("Target Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            OutlinedTextField(
                value = savedAmount,
                onValueChange = { savedAmount = it },
                label = { Text("Current Saved Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            Button(
                onClick = {
                    val target = targetAmount.toDoubleOrNull() ?: 0.0
                    val saved = savedAmount.toDoubleOrNull() ?: 0.0
                    
                    if (name.isNotBlank() && target > 0) {
                        val goal = Goal(
                            id = goalId ?: UUID.randomUUID().toString(),
                            name = name,
                            targetAmount = target,
                            savedAmount = saved
                        )
                        if (goalId == null) {
                            viewModel.addGoal(goal)
                        } else {
                            viewModel.updateGoal(goal)
                        }
                        onGoalSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && (targetAmount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Save Goal")
            }
        }
    }
}
