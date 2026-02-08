package com.healthtracker.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtracker.data.HealthEntry
import com.healthtracker.data.HealthEntryWithUser
import com.healthtracker.data.Location
import com.healthtracker.data.User
import com.healthtracker.data.WorkoutEntryWithUser
import com.healthtracker.data.repository.HealthEntryRepository
import com.healthtracker.data.repository.LocationRepository
import com.healthtracker.data.repository.MetricRepository
import com.healthtracker.data.repository.MetricTypeRepository
import com.healthtracker.data.repository.UserRepository
import com.healthtracker.data.repository.WorkoutEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthTrackerViewModel @Inject constructor(
    private val healthEntryRepository: HealthEntryRepository,
    private val workoutEntryRepository: WorkoutEntryRepository,
    private val metricRepository: MetricRepository,
    private val metricTypeRepository: MetricTypeRepository,
    private val userRepository: UserRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _entries = MutableLiveData<List<HealthEntry>>(emptyList())
    val entries: LiveData<List<HealthEntry>> = _entries
    
    private val _entriesWithUser = MutableLiveData<List<HealthEntryWithUser>>(emptyList())
    val entriesWithUser: LiveData<List<HealthEntryWithUser>> = _entriesWithUser

    private val _workoutsWithUser = MutableLiveData<List<WorkoutEntryWithUser>>(emptyList())
    val workoutsWithUser: LiveData<List<WorkoutEntryWithUser>> = _workoutsWithUser

    private val _homeItems = MutableLiveData<List<HomeFeedItem>>(emptyList())
    val homeItems: LiveData<List<HomeFeedItem>> = _homeItems
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _users = MutableLiveData<List<User>>(emptyList())
    val users: LiveData<List<User>> = _users
    
    private val _defaultUser = MutableLiveData<User?>(null)
    val defaultUser: LiveData<User?> = _defaultUser
    
    private val _locations = MutableLiveData<List<Location>>(emptyList())
    val locations: LiveData<List<Location>> = _locations

    init {
        loadEntriesWithUser()
        loadWorkoutsWithUser()
        loadHomeItems()
        loadUsers()
        loadDefaultUser()
        loadLocations()
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
    
    fun loadEntriesWithUser() {
        viewModelScope.launch {
            _isLoading.value = true
            healthEntryRepository.getAllEntriesWithUser().collectLatest { entriesWithUser ->
                _entriesWithUser.value = entriesWithUser
                _isLoading.value = false
            }
        }
    }

    fun loadWorkoutsWithUser() {
        viewModelScope.launch {
            workoutEntryRepository.getAllEntriesWithUser().collectLatest { workoutsWithUser ->
                _workoutsWithUser.value = workoutsWithUser
            }
        }
    }

    fun loadHomeItems() {
        viewModelScope.launch {
            healthEntryRepository.getAllEntriesWithUser()
                .combine(workoutEntryRepository.getAllEntriesWithUser()) { entriesWithUser, workoutsWithUser ->
                    val healthItems = entriesWithUser.map { HomeFeedItem.Health(it) }
                    val workoutItems = workoutsWithUser.map { HomeFeedItem.Workout(it) }
                    (healthItems + workoutItems).sortedByDescending { it.timestamp }
                }
                .collectLatest { items ->
                    _homeItems.value = items
                }
        }
    }

    fun addEntry(entry: HealthEntry) {
        viewModelScope.launch {
            _isLoading.value = true
            healthEntryRepository.insertEntry(entry)
            loadEntriesWithUser() // Recharger les entrées avec les utilisateurs après l'ajout
            _isLoading.value = false
        }
    }

    fun updateEntry(entry: HealthEntry) {
        viewModelScope.launch {
            _isLoading.value = true
            healthEntryRepository.updateEntry(entry)
            loadEntriesWithUser() // Recharger les entrées avec les utilisateurs après la mise à jour
            _isLoading.value = false
        }
    }

    suspend fun getEntryById(id: Long): HealthEntry? {
        return healthEntryRepository.getEntryByIdSuspend(id)
    }

    fun deleteEntry(entry: HealthEntry) {
        viewModelScope.launch {
            _isLoading.value = true
            healthEntryRepository.deleteEntry(entry)
            loadEntriesWithUser() // Recharger les entrées avec les utilisateurs après la suppression
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
    
    // Fonctions pour gérer les utilisateurs
    private fun loadUsers() {
        viewModelScope.launch {
            userRepository.getAllUsers().collectLatest { usersList ->
                _users.value = usersList
                android.util.Log.d("HealthTrackerViewModel", "Utilisateurs chargés: ${usersList.size}")
                usersList.forEach { user ->
                    android.util.Log.d("HealthTrackerViewModel", "Utilisateur: ${user.id} - ${user.name} - Défaut: ${user.isDefault}")
                }
            }
        }
    }
    
    private fun loadDefaultUser() {
        viewModelScope.launch {
            userRepository.getDefaultUser().collectLatest { user ->
                _defaultUser.value = user
                android.util.Log.d("HealthTrackerViewModel", "Utilisateur par défaut: ${user?.name ?: "Aucun"}")
            }
        }
    }
    
    fun addUser(name: String) {
        viewModelScope.launch {
            val user = User(name = name)
            userRepository.insertUser(user)
        }
    }
    
    fun setDefaultUser(userId: Long) {
        viewModelScope.launch {
            userRepository.setDefaultUser(userId)
        }
    }
    
    fun deleteUser(user: User) {
        viewModelScope.launch {
            userRepository.deleteUser(user)
        }
    }
    
    fun updateUserName(userId: Long, newName: String) {
        viewModelScope.launch {
            // Récupérer l'utilisateur actuel
            val currentUser = _users.value?.find { it.id == userId } ?: return@launch
            
            // Créer un nouvel objet User avec le nom mis à jour
            val updatedUser = currentUser.copy(name = newName)
            
            // Mettre à jour l'utilisateur dans la base de données
            userRepository.updateUser(updatedUser)
        }
    }
    
    // Fonctions pour gérer les localisations
    private fun loadLocations() {
        // Pas besoin de viewModelScope.launch car allLocations est déjà un LiveData
        locationRepository.allLocations.observeForever { locationsList ->
            _locations.value = locationsList
            android.util.Log.d("HealthTrackerViewModel", "Localisations chargées: ${locationsList.size}")
        }
    }
    
    suspend fun insertLocationAndReturnId(location: Location): Long {
        return locationRepository.insertLocation(location)
    }
    
    fun updateLocation(location: Location) {
        viewModelScope.launch {
            locationRepository.updateLocation(location)
            loadLocations() // Recharger les localisations après la mise à jour
        }
    }
    
    fun deleteLocation(location: Location) {
        viewModelScope.launch {
            locationRepository.deleteLocation(location)
            loadLocations() // Recharger les localisations après la suppression
        }
    }
}
