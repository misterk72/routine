package com.healthtracker.data.repository

import com.healthtracker.data.HealthDatabase
import com.healthtracker.data.MetricValue
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class for managing MetricValue objects in the database.
 * This class serves as a single source of truth for accessing metric value data.
 */
@Singleton
class MetricRepository @Inject constructor(
    private val database: HealthDatabase
) {
    /**
     * Get all metric values
     * @return Flow of List of MetricValue objects
     */
    fun getAllMetricValues(): Flow<List<MetricValue>> {
        return database.metricValueDao().getAllMetricValues()
    }
    
    /**
     * Get all metric values for a specific entry
     * @param entryId The ID of the entry
     * @return Flow of List of MetricValue objects for the specified entry
     */
    fun getMetricValuesForEntry(entryId: Long): Flow<List<MetricValue>> {
        return database.metricValueDao().getMetricValuesForEntry(entryId)
    }
    
    /**
     * Get a specific metric value by ID
     * @param id The ID of the metric value to retrieve
     * @return The MetricValue object wrapped in a Flow
     */
    fun getMetricValueById(id: Long): Flow<MetricValue?> {
        return database.metricValueDao().getMetricValueById(id)
    }
    
    /**
     * Insert a new metric value
     * @param metricValue The MetricValue to insert
     * @return The ID of the inserted metric value
     */
    suspend fun insertMetricValue(metricValue: MetricValue): Long {
        return database.metricValueDao().insertMetricValue(metricValue)
    }
    
    /**
     * Insert multiple metric values at once
     * @param metricValues The list of MetricValue objects to insert
     * @return The list of IDs for the inserted metric values
     */
    suspend fun insertMetricValues(metricValues: List<MetricValue>): List<Long> {
        return database.metricValueDao().insertMetricValues(metricValues)
    }
    
    /**
     * Update an existing metric value
     * @param metricValue The MetricValue to update
     */
    suspend fun updateMetricValue(metricValue: MetricValue) {
        database.metricValueDao().updateMetricValue(metricValue)
    }
    
    /**
     * Delete a metric value
     * @param metricValue The MetricValue to delete
     */
    suspend fun deleteMetricValue(metricValue: MetricValue) {
        database.metricValueDao().deleteMetricValue(metricValue)
    }
    
    /**
     * Delete all metric values associated with a specific entry
     * @param entryId The ID of the entry whose metric values should be deleted
     */
    suspend fun deleteMetricValuesForEntry(entryId: Long) {
        database.metricValueDao().deleteMetricValuesForEntry(entryId)
    }
}
