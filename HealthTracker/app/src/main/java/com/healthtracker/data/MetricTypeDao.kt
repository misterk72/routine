package com.healthtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MetricTypeDao {
    @Insert
    suspend fun insertMetricType(type: MetricType): Long

    @Insert
    suspend fun insertMetricTypes(types: List<MetricType>): List<Long>

    @Update
    suspend fun updateMetricType(type: MetricType)

    @Delete
    suspend fun deleteMetricType(type: MetricType)

    @Query("SELECT * FROM metric_types")
    fun getAllMetricTypes(): Flow<List<MetricType>>

    @Query("SELECT * FROM metric_types WHERE id = :id")
    fun getMetricTypeById(id: Long): Flow<MetricType?>

    @Query("SELECT * FROM metric_types WHERE name = :name")
    fun getMetricTypeByName(name: String): Flow<MetricType?>
    
    @Query("SELECT COUNT(*) FROM metric_types WHERE name = :name")
    suspend fun getMetricTypeCountByName(name: String): Int
}
