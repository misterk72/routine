package com.healthtracker.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtracker.data.HealthEntry
import com.healthtracker.data.MetricValue
import com.healthtracker.data.repository.HealthEntryRepository
import com.healthtracker.data.repository.MetricRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for adding and editing health entries
 * Manages the state and operations related to a single health entry
 */
@HiltViewModel
class EntryViewModel @Inject constructor(
    private val healthEntryRepository: HealthEntryRepository,
    private val metricRepository: MetricRepository
) : ViewModel() {

    // Current entry being edited
    private val _currentEntry = MutableLiveData<HealthEntry>()
    val currentEntry: LiveData<HealthEntry> = _currentEntry

    // Metric values associated with current entry
    private val _metricValues = MutableLiveData<List<MetricValue>>(emptyList())
    val metricValues: LiveData<List<MetricValue>> = _metricValues

    // Operation status
    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving

    private val _saveComplete = MutableLiveData(false)
    val saveComplete: LiveData<Boolean> = _saveComplete

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    /**
     * Create a new blank entry with current timestamp
     */
    fun createNewEntry() {
        // Utiliser l'ID de l'utilisateur par défaut (1) en attendant que l'utilisateur soit sélectionné
        // dans l'interface utilisateur
        val entry = HealthEntry(
            userId = 1L, // ID de l'utilisateur par défaut
            timestamp = LocalDateTime.now()
        )
        _currentEntry.value = entry
        _metricValues.value = emptyList()
    }

    /**
     * Load an existing entry for editing
     * @param entryId ID of the entry to load
     */
    fun loadEntry(entryId: Long) {
        viewModelScope.launch {
            healthEntryRepository.getEntryById(entryId).collectLatest { entry ->
                entry?.let {
                    _currentEntry.value = it
                    loadMetricValues(entryId)
                }
            }
        }
    }

    /**
     * Load metric values for the current entry
     * @param entryId ID of the entry whose metric values to load
     */
    private fun loadMetricValues(entryId: Long) {
        viewModelScope.launch {
            metricRepository.getMetricValuesForEntry(entryId).collectLatest { values ->
                _metricValues.value = values
            }
        }
    }

    /**
     * Update the current entry with new values
     * @param weight Weight value
     * @param waistMeasurement Waist measurement value
     * @param bodyFat Body fat percentage value
     * @param notes Optional notes
     */
    fun updateEntryValues(weight: Float?, waistMeasurement: Float?, bodyFat: Float?, notes: String?) {
        _currentEntry.value?.let { entry ->
            _currentEntry.value = entry.copy(
                weight = weight,
                waistMeasurement = waistMeasurement,
                bodyFat = bodyFat,
                notes = notes
            )
        }
    }
    
    /**
     * Update the current entry's timestamp
     * @param dateTime New date and time for the entry
     */
    fun updateEntryDateTime(dateTime: LocalDateTime) {
        _currentEntry.value?.let { entry ->
            _currentEntry.value = entry.copy(timestamp = dateTime)
        }
    }

    /**
     * Add a new metric value to the current entry
     * @param metricType The type of metric
     * @param value The metric value
     * @param unit Optional unit
     * @param notes Optional notes
     */
    fun addMetricValue(metricType: String, value: Double, unit: String? = null, notes: String? = null) {
        val currentList = _metricValues.value ?: emptyList()
        val newValue = MetricValue(
            entryId = _currentEntry.value?.id ?: 0,
            metricType = metricType,
            value = value,
            unit = unit,
            notes = notes
        )
        _metricValues.value = currentList + newValue
    }

    /**
     * Remove a metric value
     * @param value The metric value to remove
     */
    fun removeMetricValue(value: MetricValue) {
        val currentList = _metricValues.value ?: emptyList()
        _metricValues.value = currentList.filter { it != value }
    }

    /**
     * Save the current entry and its metric values
     */
    fun saveEntry() {
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            
            try {
                val entry = _currentEntry.value
                if (entry != null) {
                    val entryId = if (entry.id == 0L) {
                        // Insert new entry
                        healthEntryRepository.insertEntry(entry)
                    } else {
                        // Update existing entry
                        healthEntryRepository.updateEntry(entry)
                        entry.id
                    }
                    
                    // Save metric values
                    val values = _metricValues.value ?: emptyList()
                    val updatedValues = values.map { 
                        it.copy(entryId = entryId) 
                    }
                    
                    if (entry.id != 0L) {
                        // For existing entries, delete old values first
                        metricRepository.deleteMetricValuesForEntry(entryId)
                    }
                    
                    if (updatedValues.isNotEmpty()) {
                        metricRepository.insertMetricValues(updatedValues)
                    }
                    
                    _saveComplete.value = true
                }
            } catch (e: Exception) {
                _error.value = "Error saving entry: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Delete the current entry and its associated metric values
     */
    fun deleteEntry() {
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            
            try {
                val entry = _currentEntry.value
                if (entry != null && entry.id != 0L) {
                    // Repository will cascade delete related metric values
                    healthEntryRepository.deleteEntry(entry)
                    _saveComplete.value = true
                }
            } catch (e: Exception) {
                _error.value = "Error deleting entry: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
}
