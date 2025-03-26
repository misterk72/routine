package com.healthtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MetricValueDao {
    @Insert
    suspend fun insert(value: MetricValue)

    @Update
    suspend fun update(value: MetricValue)

    @Delete
    suspend fun delete(value: MetricValue)

    @Query("SELECT * FROM metric_values WHERE entryId = :entryId")
    fun getValuesForEntry(entryId: Long): Flow<List<MetricValue>>

    @Query("SELECT * FROM metric_values WHERE metricType = :metricType")
    fun getValuesByType(metricType: String): Flow<List<MetricValue>>

    @Query("SELECT * FROM metric_values WHERE id = :id")
    suspend fun getValueById(id: Long): MetricValue?
}
