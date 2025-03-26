package com.healthtracker.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtracker.data.HealthEntry
import com.healthtracker.data.repository.HealthEntryRepository
import com.healthtracker.data.repository.MetricRepository
import com.healthtracker.data.repository.MetricTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthTrackerViewModel @Inject constructor(
    private val healthEntryRepository: HealthEntryRepository,
    private val metricRepository: MetricRepository,
    private val metricTypeRepository: MetricTypeRepository
) : ViewModel() {

    private val _entries = MutableLiveData<List<HealthEntry>>(emptyList())
    val entries: LiveData<List<HealthEntry>> = _entries
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadEntries()
    }

    fun loadEntries() {
        viewModelScope.launch {
            _isLoading.value = true
            healthEntryRepository.getAllEntries().collectLatest { entries ->
                _entries.value = entries
                _isLoading.value = false
            }
        }
    }

    fun addEntry(entry: HealthEntry) {
        viewModelScope.launch {
            _isLoading.value = true
            healthEntryRepository.insertEntry(entry)
            _isLoading.value = false
        }
    }

    fun updateEntry(entry: HealthEntry) {
        viewModelScope.launch {
            _isLoading.value = true
            healthEntryRepository.updateEntry(entry)
            _isLoading.value = false
        }
    }

    fun deleteEntry(entry: HealthEntry) {
        viewModelScope.launch {
            _isLoading.value = true
            healthEntryRepository.deleteEntry(entry)
            _isLoading.value = false
        }
    }
    
    fun getMostRecentEntry() {
        viewModelScope.launch {
            healthEntryRepository.getMostRecentEntry().collectLatest { entry ->
                // Handle most recent entry if needed
            }
        }
    }
}
