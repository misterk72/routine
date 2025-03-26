package com.healthtracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MetricTypeDao {
    @Insert
    suspend fun insert(type: MetricType)

    @Update
    suspend fun update(type: MetricType)

    @Delete
    suspend fun delete(type: MetricType)

    @Query("SELECT * FROM metric_types")
    fun getAllTypes(): Flow<List<MetricType>>

    @Query("SELECT * FROM metric_types WHERE id = :id")
    suspend fun getTypeById(id: String): MetricType?

    @Query("SELECT * FROM metric_types WHERE name = :name")
    suspend fun getTypeByName(name: String): MetricType?
}
