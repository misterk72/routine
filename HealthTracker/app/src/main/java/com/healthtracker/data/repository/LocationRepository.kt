package com.healthtracker.data.repository

import androidx.lifecycle.LiveData
import com.healthtracker.data.Location
import com.healthtracker.data.LocationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao
) {
    val allLocations: LiveData<List<Location>> = locationDao.getAllLocations()
    
    suspend fun getLocationById(locationId: Long): Location? {
        return withContext(Dispatchers.IO) {
            locationDao.getLocationById(locationId)
        }
    }
    
    suspend fun getDefaultLocation(): Location? {
        return withContext(Dispatchers.IO) {
            locationDao.getDefaultLocation()
        }
    }
    
    suspend fun insertLocation(location: Location): Long {
        return withContext(Dispatchers.IO) {
            locationDao.insertLocation(location)
        }
    }
    
    suspend fun updateLocation(location: Location) {
        withContext(Dispatchers.IO) {
            locationDao.updateLocation(location)
        }
    }
    
    suspend fun deleteLocation(location: Location) {
        withContext(Dispatchers.IO) {
            locationDao.deleteLocation(location)
        }
    }
    
    suspend fun getNearbyLocations(latitude: Double, longitude: Double, radiusDelta: Double = 0.01): List<Location> {
        return withContext(Dispatchers.IO) {
            // Conversion approximative de degrés en distance (1 degré ≈ 111 km)
            // radiusDelta est en degrés (0.01 ≈ 1.1 km)
            locationDao.getNearbyLocations(latitude, longitude, radiusDelta, radiusDelta)
        }
    }
    
    suspend fun getLocationCount(): Int {
        return withContext(Dispatchers.IO) {
            locationDao.getLocationCount()
        }
    }
}
