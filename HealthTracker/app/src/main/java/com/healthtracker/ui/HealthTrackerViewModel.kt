package com.healthtracker.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtracker.data.HealthDatabase
import com.healthtracker.data.HealthEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthTrackerViewModel @Inject constructor(
    private val database: HealthDatabase
) : ViewModel() {

    private val _entries = MutableLiveData<List<HealthEntry>>()
    val entries: LiveData<List<HealthEntry>> = _entries

    init {
        loadEntries()
    }

    private fun loadEntries() {
        viewModelScope.launch {
            database.healthEntryDao().getAllEntries().collect { entries ->
                _entries.value = entries
            }
        }
    }

    fun addEntry(entry: HealthEntry) {
        viewModelScope.launch {
            database.healthEntryDao().insert(entry)
        }
    }

    fun updateEntry(entry: HealthEntry) {
        viewModelScope.launch {
            database.healthEntryDao().update(entry)
        }
    }

    fun deleteEntry(entry: HealthEntry) {
        viewModelScope.launch {
            database.healthEntryDao().delete(entry)
        }
    }
}
