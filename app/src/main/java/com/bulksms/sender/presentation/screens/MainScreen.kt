// Save at: app/src/main/java/com/bulksms/sender/presentation/screens/MainScreen.kt

package com.bulksms.sender.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bulksms.sender.data.models.BatchStatus
import com.bulksms.sender.data.models.SmsBatch
import com.bulksms.sender.presentation.viewmodel.MainViewModel
import com.bulksms.sender.domain.usecases.SmsManagerService
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importFile(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk SMS Sender") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            if (uiState.phoneNumbers.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { viewModel.startBulkSend() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Number Import Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Phone Numbers", style = MaterialTheme.typography.titleLarge)

                        Button(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLoading
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import File (CSV, Excel, TXT)")
                        }

                        if (uiState.phoneNumbers.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(uiState.phoneNumbers.take(20)) { number ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = number,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(
                                            onClick = { viewModel.removeNumber(uiState.phoneNumbers.indexOf(number)) }
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                if (uiState.phoneNumbers.size > 20) {
                                    Text("+ ${uiState.phoneNumbers.size - 20} more")
                                }
                            }
                            Text("Total: ${uiState.phoneNumbers.size} numbers")
                        } else {
                            Text(
                                text = "No numbers added. Import a file or add manually.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                // Message Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Message", style = MaterialTheme.typography.titleLarge)

                        OutlinedTextField(
                            value = uiState.message,
                            onValueChange = { viewModel.setMessage(it) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            placeholder = { Text("Enter your message here...") }
                        )

                        Text(
                            text = "Characters: ${uiState.message.length} / SMS limit: 160",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.message.length > 160)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                // Settings Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Settings", style = MaterialTheme.typography.titleLarge)

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delay between messages:")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    viewModel.setInterval(maxOf(1, uiState.intervalSeconds - 1))
                                }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                }
                                Text("${uiState.intervalSeconds} seconds")
                                IconButton(onClick = {
                                    viewModel.setInterval(minOf(10, uiState.intervalSeconds + 1))
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase")
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.batches.isNotEmpty()) {
                item {
                    // Batch History Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Recent Batches", style = MaterialTheme.typography.titleLarge)

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.batches) { batch ->
                                    BatchItem(
                                        batch = batch,
                                        onPause = { viewModel.pauseSending(batch.batchId) },
                                        onResume = { viewModel.resumeSending(batch.batchId) },
                                        onCancel = { viewModel.cancelSending(batch.batchId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Error Dialog
    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(uiState.error!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    // Country Code Dialog
    if (uiState.showCountryCodeDialog) {
        var countryCode by remember { mutableStateOf(uiState.countryCode) }

        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Select Country Code") },
            text = {
                Column {
                    Text("Enter country code for phone numbers:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = countryCode,
                        onValueChange = { countryCode = it },
                        placeholder = { Text("e.g., +1, +44, +91") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Example: $countryCode 1234567890",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.applyCountryCodeAndNormalize(countryCode) }) {
                    Text("Apply")
                }
            }
        )
    }

    // SIM Selection Dialog
    if (uiState.showSimSelector && uiState.availableSims.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Select SIM Card") },
            text = {
                Column {
                    Text("Multiple SIM cards detected. Select which SIM to use:")
                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.availableSims.forEach { sim ->
                        TextButton(
                            onClick = { viewModel.selectSimCard(sim) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${sim.displayName} (${sim.carrierName})")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.selectSimCard(uiState.availableSims.first())
                }) {
                    Text("Use Default")
                }
            }
        )
    }

    // Loading Indicator
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun BatchItem(
    batch: SmsBatch,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = batch.status.name,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = when (batch.status) {
                        BatchStatus.COMPLETED -> Color.Green
                        BatchStatus.RUNNING -> Color.Blue
                        BatchStatus.PAUSED -> Color(0xFFFF9800)
                        BatchStatus.CANCELLED -> Color.Red
                        else -> Color.Gray
                    }
                )
                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(batch.startTime))
                )
            }

            LinearProgressIndicator(
                progress = if (batch.totalMessages > 0) batch.sentMessages.toFloat() / batch.totalMessages else 0f,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("✅ Sent: ${batch.sentMessages}")
                Text("❌ Failed: ${batch.failedMessages}")
                Text("📊 Total: ${batch.totalMessages}")
            }

            if (batch.currentNumber.isNotBlank() && batch.status == BatchStatus.RUNNING) {
                Text(
                    text = "📱 Current: ${batch.currentNumber}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (batch.status == BatchStatus.RUNNING) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onPause,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause")
                    }
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel")
                    }
                }
            } else if (batch.status == BatchStatus.PAUSED) {
                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resume")
                }
            }
        }
    }
}