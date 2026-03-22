package com.akproleter.mobile.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akproleter.mobile.data.local.AkProleterDao
import com.akproleter.mobile.data.local.entities.VoiceRecordEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val dao: AkProleterDao
) : ViewModel() {

    private val _records = MutableStateFlow<List<VoiceRecordEntity>>(emptyList())
    val records: StateFlow<List<VoiceRecordEntity>> = _records.asStateFlow()

    init {
        loadRecords()
    }

    fun loadRecords() {
        viewModelScope.launch {
            _records.value = dao.getAllVoiceRecords()
        }
    }

    fun updateRecord(record: VoiceRecordEntity) {
        viewModelScope.launch {
            dao.updateVoiceRecord(record)
            loadRecords()
        }
    }
}
