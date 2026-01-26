package com.healthtracker.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.healthtracker.data.WorkoutEntryWithUser
import com.healthtracker.data.repository.WorkoutEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutEntryViewModel @Inject constructor(
    private val workoutEntryRepository: WorkoutEntryRepository
) : ViewModel() {

    private val _deleteComplete = MutableLiveData(false)
    val deleteComplete: LiveData<Boolean> = _deleteComplete

    fun getWorkout(id: Long): LiveData<WorkoutEntryWithUser?> {
        return workoutEntryRepository.getEntryWithUserById(id).asLiveData()
    }

    fun deleteWorkout(entry: com.healthtracker.data.WorkoutEntry) {
        viewModelScope.launch {
            _deleteComplete.value = false
            workoutEntryRepository.deleteEntry(entry)
            _deleteComplete.value = true
        }
    }
}
