package com.example.moneymap.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.moneymap.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupScreen(
    onNavigateBack: () -> Unit,
    onPinSet: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var pin by remember { mutableStateOf("") }
    var confirmedPin by remember { mutableStateOf("") }
    var isConfirming by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set up PIN") },
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text(
                    text = if (isConfirming) "Confirm your PIN" else "Create a PIN",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (isConfirming) "Re-enter your 4-digit PIN" else "Enter a 4-digit PIN to secure your app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                PinDots(length = 4, filledCount = if (isConfirming) confirmedPin.length else pin.length)

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            NumberPad(
                onNumberClick = { number ->
                    error = null
                    if (isConfirming) {
                        if (confirmedPin.length < 4) {
                            confirmedPin += number
                            if (confirmedPin.length == 4) {
                                if (confirmedPin == pin) {
                                    viewModel.setPin(pin)
                                    onPinSet()
                                } else {
                                    error = "PINs do not match. Try again."
                                    confirmedPin = ""
                                    pin = ""
                                    isConfirming = false
                                }
                            }
                        }
                    } else {
                        if (pin.length < 4) {
                            pin += number
                            if (pin.length == 4) {
                                isConfirming = true
                            }
                        }
                    }
                },
                onDeleteClick = {
                    error = null
                    if (isConfirming) {
                        if (confirmedPin.isNotEmpty()) {
                            confirmedPin = confirmedPin.dropLast(1)
                        }
                    } else {
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun PinDots(length: Int, filledCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(length) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < filledCount) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

@Composable
fun NumberPad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(bottom = 32.dp)
    ) {
        for (i in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                for (j in 1..3) {
                    val number = i * 3 + j
                    NumberKey(number.toString(), onClick = { onNumberClick(number.toString()) })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Spacer(modifier = Modifier.size(72.dp))
            NumberKey("0", onClick = { onNumberClick("0") })
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDeleteClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun NumberKey(number: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleLarge,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
