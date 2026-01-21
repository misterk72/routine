package com.healthtracker.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.healthtracker.data.WorkoutEntryWithUser
import com.healthtracker.data.repository.WorkoutEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WorkoutEntryViewModel @Inject constructor(
    private val workoutEntryRepository: WorkoutEntryRepository
) : ViewModel() {

    fun getWorkout(id: Long): LiveData<WorkoutEntryWithUser?> {
        return workoutEntryRepository.getEntryWithUserById(id).asLiveData()
    }
}
