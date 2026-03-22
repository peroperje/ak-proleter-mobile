package com.akproleter.mobile.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
                    onUpdate = { viewModel.updateRecord(it) }
                )
            }
        }
    }
}

@Composable
fun HistoryCard(
    record: VoiceRecordEntity,
    onUpdate: (VoiceRecordEntity) -> Unit
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
                Text(text = record.voiceInput, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(text = "Status: ${record.status.name}", style = MaterialTheme.typography.bodyMedium)
                
                if (record.discipline != null) {
                    Text(text = "Discipline: ${record.discipline}", style = MaterialTheme.typography.bodyMedium)
                }
                if (record.formattedScore != null) {
                    Text(text = "Score: ${record.formattedScore}", style = MaterialTheme.typography.bodyMedium)
                }
                
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { isEditing = true }) {
                        Text("Edit")
                    }
                    if (record.status == RecordStatus.SAVED) {
                        OutlinedButton(onClick = {
                            onUpdate(record.copy(markedIncorrect = !record.markedIncorrect))
                        }) {
                            Text(if (record.markedIncorrect) "Marked Incorrect" else "Mark Incorrect")
                        }
                    }
                }
            }
        }
    }
}
