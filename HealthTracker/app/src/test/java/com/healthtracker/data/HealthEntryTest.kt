package com.healthtracker.data

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime

class HealthEntryTest {
    
    @Test
    fun `test HealthEntry creation with valid data`() {
        val timestamp = LocalDateTime.now()
        val entry = HealthEntry(
            id = 1,
            userId = 1,
            timestamp = timestamp,
            weight = 75.5f,
            waistMeasurement = 85.0f,
            bodyFat = 20.0f,
            notes = "Test note",
            synced = false,
            serverEntryId = null
        )
        
        assertEquals(1, entry.id)
        assertEquals(1, entry.userId)
        assertEquals(timestamp, entry.timestamp)
        assertEquals(75.5f, entry.weight)
        assertEquals(85.0f, entry.waistMeasurement)
        assertEquals(20.0f, entry.bodyFat)
        assertEquals("Test note", entry.notes)
        assertFalse(entry.synced)
        assertNull(entry.serverEntryId)
    }
    
    @Test
    fun `test HealthEntry equality`() {
        val timestamp = LocalDateTime.now()
        val entry1 = HealthEntry(
            id = 1,
            userId = 1,
            timestamp = timestamp,
            weight = 75.5f,
            waistMeasurement = 85.0f,
            bodyFat = 20.0f,
            notes = "Note",
            synced = false,
            serverEntryId = null
        )
        val entry2 = HealthEntry(
            id = 1,
            userId = 1,
            timestamp = timestamp,
            weight = 75.5f,
            waistMeasurement = 85.0f,
            bodyFat = 20.0f,
            notes = "Note",
            synced = false,
            serverEntryId = null
        )
        val entry3 = HealthEntry(
            id = 2,
            userId = 1,
            timestamp = timestamp,
            weight = 75.5f,
            waistMeasurement = 85.0f,
            bodyFat = 20.0f,
            notes = "Note",
            synced = false,
            serverEntryId = null
        )
        
        assertEquals(entry1, entry2)
        assertNotEquals(entry1, entry3)
    }
    
    @Test
    fun `test HealthEntry with null values`() {
        val timestamp = LocalDateTime.now()
        val entry = HealthEntry(
            id = 1,
            userId = 1,
            timestamp = timestamp,
            weight = null,
            waistMeasurement = null,
            bodyFat = null,
            notes = null,
            synced = false,
            serverEntryId = null
        )
        
        assertEquals(1, entry.id)
        assertEquals(1, entry.userId)
        assertEquals(timestamp, entry.timestamp)
        assertNull(entry.weight)
        assertNull(entry.waistMeasurement)
        assertNull(entry.bodyFat)
        assertNull(entry.notes)
        assertFalse(entry.synced)
        assertNull(entry.serverEntryId)
    }
    
    @Test
    fun `test HealthEntry copy`() {
        val timestamp = LocalDateTime.now()
        val entry = HealthEntry(
            id = 1,
            userId = 1,
            timestamp = timestamp,
            weight = 75.5f,
            waistMeasurement = 85.0f,
            bodyFat = 20.0f,
            notes = "Original note",
            synced = false,
            serverEntryId = null
        )
        val copiedEntry = entry.copy(weight = 76.0f, notes = "Updated note", synced = true, serverEntryId = 123L)
        
        assertEquals(entry.id, copiedEntry.id)
        assertEquals(entry.userId, copiedEntry.userId)
        assertEquals(entry.timestamp, copiedEntry.timestamp)
        assertEquals(76.0f, copiedEntry.weight)
        assertEquals(entry.waistMeasurement, copiedEntry.waistMeasurement)
        assertEquals(entry.bodyFat, copiedEntry.bodyFat)
        assertEquals("Updated note", copiedEntry.notes)
        assertTrue(copiedEntry.synced)
        assertEquals(123L, copiedEntry.serverEntryId)
    }
}
