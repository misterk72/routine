package com.healthtracker.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.healthtracker.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import java.time.LocalDateTime
import org.junit.Assert.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HealthTrackerViewModelTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: HealthDatabase
    private lateinit var healthEntryDao: HealthEntryDao
    private lateinit var viewModel: HealthTrackerViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        
        database = mock()
        healthEntryDao = mock()
        whenever(database.healthEntryDao()).thenReturn(healthEntryDao)
        whenever(healthEntryDao.getAllEntries()).thenReturn(flowOf(emptyList()))
        
        viewModel = HealthTrackerViewModel(database)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `loadEntries emits entries`() = runTest {
        // Given
        val entries = listOf(
            HealthEntry(
                id = 1,
                timestamp = LocalDateTime.now(),
                weight = 75.5f,
                waistMeasurement = 85.0f,
                notes = "Test entry"
            )
        )
        
        // When
        var result: List<HealthEntry>? = null
        viewModel.entries.observeForever { list ->
            result = list
        }
        advanceUntilIdle() // Wait for initial empty list
        
        whenever(healthEntryDao.getAllEntries())
            .thenReturn(flowOf(entries))
        viewModel.loadEntries() // Trigger reload
        
        // Then
        advanceUntilIdle()
        assertEquals(entries, result)
        verify(healthEntryDao, atLeast(2)).getAllEntries()
    }
    
    @Test
    fun `addEntry inserts new entry`() = runTest {
        // Given
        val entry = HealthEntry(
            id = 0,
            timestamp = LocalDateTime.now(),
            weight = 75.5f,
            waistMeasurement = 85.0f,
            notes = "New entry"
        )
        
        // When
        viewModel.addEntry(entry)
        advanceUntilIdle()
        
        // Then
        verify(healthEntryDao).insert(entry)
        verify(healthEntryDao, atLeast(2)).getAllEntries()
    }
    
    @Test
    fun `updateEntry updates existing entry`() = runTest {
        // Given
        val entry = HealthEntry(
            id = 1,
            timestamp = LocalDateTime.now(),
            weight = 76.0f,
            waistMeasurement = 84.0f,
            notes = "Updated entry"
        )
        
        // When
        viewModel.updateEntry(entry)
        advanceUntilIdle()
        
        // Then
        verify(healthEntryDao).update(entry)
        verify(healthEntryDao, atLeast(2)).getAllEntries()
    }
    
    @Test
    fun `deleteEntry removes entry`() = runTest {
        // Given
        val entry = HealthEntry(
            id = 1,
            timestamp = LocalDateTime.now(),
            weight = 75.5f,
            waistMeasurement = 85.0f,
            notes = "Test entry"
        )
        
        // When
        viewModel.deleteEntry(entry)
        advanceUntilIdle()
        
        // Then
        verify(healthEntryDao).delete(entry)
        verify(healthEntryDao, atLeast(2)).getAllEntries()
    }
}
