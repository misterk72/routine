package com.healthtracker.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtracker.data.User
import com.healthtracker.data.WorkoutEntry
import com.healthtracker.data.repository.UserRepository
import com.healthtracker.data.repository.WorkoutEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutEntryRepository: WorkoutEntryRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _users = MutableLiveData<List<User>>(emptyList())
    val users: LiveData<List<User>> = _users

    private val _defaultUser = MutableLiveData<User?>(null)
    val defaultUser: LiveData<User?> = _defaultUser

    init {
        loadUsers()
        loadDefaultUser()
    }

    fun addWorkout(entry: WorkoutEntry) {
        viewModelScope.launch {
            workoutEntryRepository.insertEntry(entry)
        }
    }

    fun updateWorkout(entry: WorkoutEntry) {
        viewModelScope.launch {
            workoutEntryRepository.updateEntry(entry)
        }
    }

    suspend fun getWorkoutById(id: Long): WorkoutEntry? {
        return workoutEntryRepository.getEntryById(id)
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userRepository.getAllUsers().collectLatest { usersList ->
                _users.value = usersList
            }
        }
    }

    private fun loadDefaultUser() {
        viewModelScope.launch {
            userRepository.getDefaultUser().collectLatest { user ->
                _defaultUser.value = user
            }
        }
    }
}
