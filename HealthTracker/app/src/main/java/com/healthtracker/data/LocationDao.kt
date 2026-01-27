package com.healthtracker.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface LocationDao {
    @Query("SELECT * FROM locations ORDER BY name ASC")
    fun getAllLocations(): LiveData<List<Location>>

    @Query("SELECT * FROM locations")
    suspend fun getAllLocationsList(): List<Location>
    
    @Query("SELECT * FROM locations WHERE id = :locationId")
    suspend fun getLocationById(locationId: Long): Location?
    
    @Query("SELECT * FROM locations WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultLocation(): Location?
    
    @Insert
    suspend fun insertLocation(location: Location): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLocations(locations: List<Location>)
    
    @Update
    suspend fun updateLocation(location: Location)
    
    @Delete
    suspend fun deleteLocation(location: Location)
    
    @Query("SELECT * FROM locations WHERE " +
           "(ABS(latitude - :lat) < :latDelta) AND " +
           "(ABS(longitude - :lng) < :lngDelta)")
    suspend fun getNearbyLocations(lat: Double, lng: Double, latDelta: Double, lngDelta: Double): List<Location>
    
    @Query("SELECT COUNT(*) FROM locations")
    suspend fun getLocationCount(): Int
}
