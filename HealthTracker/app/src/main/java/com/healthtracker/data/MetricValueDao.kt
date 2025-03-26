package com.healthtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MetricValueDao {
    @Insert
    suspend fun insertMetricValue(value: MetricValue): Long

    @Insert
    suspend fun insertMetricValues(values: List<MetricValue>): List<Long>

    @Update
    suspend fun updateMetricValue(value: MetricValue)

    @Delete
    suspend fun deleteMetricValue(value: MetricValue)

    @Query("DELETE FROM metric_values WHERE entryId = :entryId")
    suspend fun deleteMetricValuesForEntry(entryId: Long)

    @Query("SELECT * FROM metric_values")
    fun getAllMetricValues(): Flow<List<MetricValue>>

    @Query("SELECT * FROM metric_values WHERE entryId = :entryId")
    fun getMetricValuesForEntry(entryId: Long): Flow<List<MetricValue>>

    @Query("SELECT * FROM metric_values WHERE metricType = :metricType")
    fun getValuesByType(metricType: String): Flow<List<MetricValue>>

    @Query("SELECT * FROM metric_values WHERE id = :id")
    fun getMetricValueById(id: Long): Flow<MetricValue?>
}
