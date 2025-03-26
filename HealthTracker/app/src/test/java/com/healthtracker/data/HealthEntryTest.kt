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
            timestamp = timestamp,
            weight = 75.5f,
            waistMeasurement = 85.0f,
            notes = "Test note"
        )
        
        assertEquals(1, entry.id)
        assertEquals(timestamp, entry.timestamp)
        assertEquals(75.5f, entry.weight)
        assertEquals(85.0f, entry.waistMeasurement)
        assertEquals("Test note", entry.notes)
    }
    
    @Test
    fun `test HealthEntry equality`() {
        val timestamp = LocalDateTime.now()
        val entry1 = HealthEntry(1, timestamp, 75.5f, 85.0f, "Note")
        val entry2 = HealthEntry(1, timestamp, 75.5f, 85.0f, "Note")
        val entry3 = HealthEntry(2, timestamp, 75.5f, 85.0f, "Note")
        
        assertEquals(entry1, entry2)
        assertNotEquals(entry1, entry3)
    }
    
    @Test
    fun `test HealthEntry with null values`() {
        val timestamp = LocalDateTime.now()
        val entry = HealthEntry(
            id = 1,
            timestamp = timestamp,
            weight = null,
            waistMeasurement = null,
            notes = null
        )
        
        assertEquals(1, entry.id)
        assertEquals(timestamp, entry.timestamp)
        assertNull(entry.weight)
        assertNull(entry.waistMeasurement)
        assertNull(entry.notes)
    }
    
    @Test
    fun `test HealthEntry copy`() {
        val timestamp = LocalDateTime.now()
        val entry = HealthEntry(1, timestamp, 75.5f, 85.0f, "Original note")
        val copiedEntry = entry.copy(weight = 76.0f, notes = "Updated note")
        
        assertEquals(entry.id, copiedEntry.id)
        assertEquals(entry.timestamp, copiedEntry.timestamp)
        assertEquals(76.0f, copiedEntry.weight)
        assertEquals(entry.waistMeasurement, copiedEntry.waistMeasurement)
        assertEquals("Updated note", copiedEntry.notes)
    }
}
