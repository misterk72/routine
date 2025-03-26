package com.healthtracker.data.repository

import com.healthtracker.data.HealthDatabase
import com.healthtracker.data.MetricType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository class for managing MetricType objects in the database.
 * This class serves as a single source of truth for accessing metric type data.
 */
@Singleton
class MetricTypeRepository @Inject constructor(
    private val database: HealthDatabase
) {
    /**
     * Get all metric types
     * @return Flow of List of MetricType objects
     */
    fun getAllMetricTypes(): Flow<List<MetricType>> {
        return database.metricTypeDao().getAllMetricTypes()
    }
    
    /**
     * Get a specific metric type by ID
     * @param id The ID of the metric type to retrieve
     * @return The MetricType object wrapped in a Flow
     */
    fun getMetricTypeById(id: Long): Flow<MetricType?> {
        return database.metricTypeDao().getMetricTypeById(id)
    }
    
    /**
     * Get a specific metric type by name
     * @param name The name of the metric type to retrieve
     * @return The MetricType object wrapped in a Flow
     */
    fun getMetricTypeByName(name: String): Flow<MetricType?> {
        return database.metricTypeDao().getMetricTypeByName(name)
    }
    
    /**
     * Insert a new metric type
     * @param metricType The MetricType to insert
     * @return The ID of the inserted metric type
     */
    suspend fun insertMetricType(metricType: MetricType): Long {
        return database.metricTypeDao().insertMetricType(metricType)
    }
    
    /**
     * Insert multiple metric types at once
     * @param metricTypes The list of MetricType objects to insert
     * @return The list of IDs for the inserted metric types
     */
    suspend fun insertMetricTypes(metricTypes: List<MetricType>): List<Long> {
        return database.metricTypeDao().insertMetricTypes(metricTypes)
    }
    
    /**
     * Update an existing metric type
     * @param metricType The MetricType to update
     */
    suspend fun updateMetricType(metricType: MetricType) {
        database.metricTypeDao().updateMetricType(metricType)
    }
    
    /**
     * Delete a metric type
     * @param metricType The MetricType to delete
     */
    suspend fun deleteMetricType(metricType: MetricType) {
        database.metricTypeDao().deleteMetricType(metricType)
    }
    
    /**
     * Check if a metric type with the given name exists
     * @param name The name to check
     * @return true if a metric type with this name exists, false otherwise
     */
    suspend fun metricTypeExists(name: String): Boolean {
        return database.metricTypeDao().getMetricTypeCountByName(name) > 0
    }
}
