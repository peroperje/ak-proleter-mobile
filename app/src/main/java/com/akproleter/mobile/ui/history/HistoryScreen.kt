package com.akproleter.mobile.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akproleter.mobile.data.local.entities.VoiceRecordEntity
import com.akproleter.mobile.data.local.entities.RecordStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val records by viewModel.records.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadRecords()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recording History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(records) { record ->
                HistoryCard(
                    record = record,
                    isOnline = viewModel.isOnline(),
                    onUpdate = { viewModel.updateRecord(it) },
                    onDelete = { viewModel.deleteRecord(it) }
                )
            }
        }
    }
}

@Composable
fun HistoryCard(
    record: VoiceRecordEntity,
    isOnline: Boolean,
    onUpdate: (VoiceRecordEntity) -> Unit,
    onDelete: (VoiceRecordEntity) -> Unit
) {
    var isEditing by remember(record.uuid) { mutableStateOf(false) }
    var editedText by remember(record.uuid) { mutableStateOf(record.voiceInput) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val dateStr = dateFormat.format(Date(record.createdAt))
            
            Text(text = dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isEditing) {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { isEditing = false }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        onUpdate(record.copy(voiceInput = editedText, status = RecordStatus.PENDING))
                        isEditing = false
                    }, modifier = Modifier.weight(1f)) {
                        Text("Save")
                    }
                }
            } else {
                Text(text = "Input Text", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(text = record.voiceInput, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (record.discipline != null) {
                    Text(text = "Discipline", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(text = record.discipline, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(text = record.status.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    
                    if (record.formattedScore != null) {
                        Column {
                            Text(text = "Score", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(text = record.formattedScore, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (record.status == RecordStatus.PENDING) {
                        Button(onClick = { isEditing = true }) {
                            Text("Edit")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp)) // Maintain alignment if button hidden
                    }
                    
                    val canDelete = when (record.status) {
                        RecordStatus.PENDING -> true
                        RecordStatus.SAVED -> isOnline
                        RecordStatus.PROCESSING -> false
                    }

                    OutlinedButton(
                        onClick = { onDelete(record) },
                        enabled = canDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}
