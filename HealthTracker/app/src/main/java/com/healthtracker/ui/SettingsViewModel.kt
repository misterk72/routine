package com.healthtracker.ui

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtracker.data.MetricType
import com.healthtracker.data.repository.MetricTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing app settings and preferences
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val metricTypeRepository: MetricTypeRepository
) : ViewModel() {

    companion object {
        const val PREF_DARK_MODE = "dark_mode"
        const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val PREF_REMINDER_TIME = "reminder_time"
        const val PREF_EXPORT_FORMAT = "export_format"
    }
    
    // Settings state
    private val _isDarkMode = MutableLiveData(false)
    val isDarkMode: LiveData<Boolean> = _isDarkMode
    

    
    private val _notificationsEnabled = MutableLiveData(false)
    val notificationsEnabled: LiveData<Boolean> = _notificationsEnabled
    
    private val _reminderTime = MutableLiveData<String>("08:00")
    val reminderTime: LiveData<String> = _reminderTime
    
    private val _exportFormat = MutableLiveData<String>("CSV")
    val exportFormat: LiveData<String> = _exportFormat
    
    // Metric types
    private val _metricTypes = MutableLiveData<List<MetricType>>(emptyList())
    val metricTypes: LiveData<List<MetricType>> = _metricTypes
    
    // Status indicators
    private val _isSaving = MutableLiveData(false)
    val isSaving: LiveData<Boolean> = _isSaving
    
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    init {
        loadSettings()
        loadMetricTypes()
    }
    
    /**
     * Load settings from SharedPreferences
     */
    private fun loadSettings() {
        _isDarkMode.value = sharedPreferences.getBoolean(PREF_DARK_MODE, false)

        _notificationsEnabled.value = sharedPreferences.getBoolean(PREF_NOTIFICATIONS_ENABLED, false)
        _reminderTime.value = sharedPreferences.getString(PREF_REMINDER_TIME, "08:00") ?: "08:00"
        _exportFormat.value = sharedPreferences.getString(PREF_EXPORT_FORMAT, "CSV") ?: "CSV"
    }
    
    /**
     * Load available metric types
     */
    private fun loadMetricTypes() {
        viewModelScope.launch {
            metricTypeRepository.getAllMetricTypes().collectLatest { types ->
                _metricTypes.value = types
            }
        }
    }
    
    /**
     * Update dark mode setting
     */
    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        sharedPreferences.edit()
            .putBoolean(PREF_DARK_MODE, enabled)
            .apply()
    }
    

    
    /**
     * Update notifications setting
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        sharedPreferences.edit()
            .putBoolean(PREF_NOTIFICATIONS_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Update reminder time setting
     */
    fun setReminderTime(time: String) {
        _reminderTime.value = time
        sharedPreferences.edit()
            .putString(PREF_REMINDER_TIME, time)
            .apply()
    }
    
    /**
     * Update export format setting
     */
    fun setExportFormat(format: String) {
        _exportFormat.value = format
        sharedPreferences.edit()
            .putString(PREF_EXPORT_FORMAT, format)
            .apply()
    }
    
    /**
     * Add a new metric type
     */
    fun addMetricType(name: String, unit: String? = null, description: String? = null,
                      minValue: Double? = null, maxValue: Double? = null, stepSize: Double? = null) {
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            
            try {
                // Check if metric type already exists
                if (metricTypeRepository.metricTypeExists(name)) {
                    _error.value = "A metric type with this name already exists"
                    return@launch
                }
                
                val metricType = MetricType(
                    name = name,
                    unit = unit,
                    description = description,
                    minValue = minValue,
                    maxValue = maxValue,
                    stepSize = stepSize
                )
                
                metricTypeRepository.insertMetricType(metricType)
                loadMetricTypes() // Refresh the list
            } catch (e: Exception) {
                _error.value = "Error adding metric type: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
    
    /**
     * Update an existing metric type
     */
    fun updateMetricType(metricType: MetricType) {
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            
            try {
                metricTypeRepository.updateMetricType(metricType)
                loadMetricTypes() // Refresh the list
            } catch (e: Exception) {
                _error.value = "Error updating metric type: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
    
    /**
     * Delete a metric type
     */
    fun deleteMetricType(metricType: MetricType) {
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            
            try {
                metricTypeRepository.deleteMetricType(metricType)
                loadMetricTypes() // Refresh the list
            } catch (e: Exception) {
                _error.value = "Error deleting metric type: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }
}
